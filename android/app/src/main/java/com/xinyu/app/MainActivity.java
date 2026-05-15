package com.xinyu.app;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.xinyu.app.fragment.EmotionFragment;
import com.xinyu.app.fragment.HomeFragment;
import com.xinyu.app.fragment.NoteFragment;
import com.xinyu.app.fragment.ProfileFragment;
import com.xinyu.app.fragment.TreeHoleFragment;
import com.xinyu.app.util.Reporter;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private Fragment activeFragment;
    private final HomeFragment homeFragment = new HomeFragment();
    private final EmotionFragment emotionFragment = new EmotionFragment();
    private final TreeHoleFragment treeHoleFragment = new TreeHoleFragment();
    private final NoteFragment noteFragment = new NoteFragment();
    private final ProfileFragment profileFragment = new ProfileFragment();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 恢复深色模式偏好 - 只在模式不同时才切换，避免闪烁
        boolean isDark = getSharedPreferences("xinyu", MODE_PRIVATE).getBoolean("dark_mode", false);
        int targetMode = isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
            AppCompatDelegate.setDefaultNightMode(targetMode);
            // 不要return，继续初始化，因为setDefaultNightMode已经设置好了
        }
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_nav);
        setupFragments();
        setupBottomNav();
        Reporter.heartbeat(this);
        checkForUpdate();
    }

    private void setupFragments() {
        getSupportFragmentManager().beginTransaction()
            .add(R.id.fragment_container, profileFragment, "profile").hide(profileFragment)
            .add(R.id.fragment_container, noteFragment, "note").hide(noteFragment)
            .add(R.id.fragment_container, treeHoleFragment, "treehole").hide(treeHoleFragment)
            .add(R.id.fragment_container, emotionFragment, "emotion").hide(emotionFragment)
            .add(R.id.fragment_container, homeFragment, "home")
            .commit();
        activeFragment = homeFragment;
    }

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment target;
            int id = item.getItemId();
            if (id == R.id.nav_home) target = homeFragment;
            else if (id == R.id.nav_emotion) target = emotionFragment;
            else if (id == R.id.nav_treehole) target = treeHoleFragment;
            else if (id == R.id.nav_note) target = noteFragment;
            else if (id == R.id.nav_profile) target = profileFragment;
            else return false;

            getSupportFragmentManager().beginTransaction()
                .hide(activeFragment).show(target).commit();
            activeFragment = target;
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (activeFragment instanceof HomeFragment) {
            ((HomeFragment) activeFragment).refresh();
        } else if (activeFragment instanceof EmotionFragment) {
            ((EmotionFragment) activeFragment).refresh();
        }
    }

    /** 检查是否有新版本 */
    private void checkForUpdate() {
        executor.execute(() -> {
            JSONObject result = Reporter.checkAppVersion();
            mainHandler.post(() -> {
                try {
                    int serverCode = result.optInt("version_code", 0);
                    int localCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
                    if (serverCode > localCode) {
                        String versionName = result.optString("version_name", "");
                        String changelog = result.optString("changelog", "");
                        String downloadUrl = result.optString("download_url", "");
                        showUpdateDialog(versionName, changelog, downloadUrl);
                    }
                } catch (Exception e) {
                    // ignore
                }
            });
        });
    }

    /** 显示更新弹窗 */
    private void showUpdateDialog(String versionName, String changelog, String downloadUrl) {
        String msg = "新版本 v" + versionName;
        if (!changelog.isEmpty()) {
            msg += "\n\n更新内容：\n" + changelog;
        }
        new AlertDialog.Builder(this)
            .setTitle("发现新版本")
            .setMessage(msg)
            .setPositiveButton("立即更新", (d, w) -> downloadApk(downloadUrl))
            .setNegativeButton("稍后", null)
            .show();
    }

    /** 下载APK */
    private void downloadApk(String downloadUrl) {
        String fullUrl = Reporter.SERVER + downloadUrl;
        Toast.makeText(this, "开始下载...", Toast.LENGTH_SHORT).show();

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fullUrl));
        request.setTitle("心屿更新");
        request.setDescription("正在下载新版本...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "xinyu_update.apk");
        request.setAllowedOverMetered(true);
        request.setAllowedOverRoaming(true);

        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        long downloadId = dm.enqueue(request);

        // 监听下载完成
        new Thread(() -> {
            boolean downloading = true;
            while (downloading) {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);
                Cursor cursor = dm.query(query);
                if (cursor != null && cursor.moveToFirst()) {
                    int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                    if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                        downloading = false;
                    }
                }
                if (cursor != null) cursor.close();
                if (downloading) {
                    try { Thread.sleep(1000); } catch (InterruptedException e) { break; }
                }
            }
            mainHandler.post(() -> installApk(dm, downloadId));
        }).start();
    }

    /** 安装下载好的APK */
    private void installApk(DownloadManager dm, long downloadId) {
        Uri uri = dm.getUriForDownloadedFile(downloadId);
        if (uri == null) {
            Toast.makeText(this, "下载失败", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
