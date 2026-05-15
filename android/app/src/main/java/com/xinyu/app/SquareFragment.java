package com.xinyu.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.xinyu.app.util.Reporter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SquareFragment extends Fragment {

    private RecyclerView postsList;
    private TextView tvEmpty;
    private FloatingActionButton fabPost;
    private SwipeRefreshLayout swipeRefresh;
    private TextView btnSortTime, btnSortHot;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private PostsAdapter adapter;
    private int currentSort = 0; // 0=time, 1=hot
    private List<PostItem> allPosts = new ArrayList<>();
    private boolean notifShown = false; // 本次会话只弹一次通知
    private LinearLayout notifBanner;
    private TextView tvNotifText;

    // Image picker
    private Uri selectedImageUri;
    private String uploadedMediaUrl;
    // Keep a strong reference to the preview ImageView across the image picker callback
    private ImageView dialogPreviewRef;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                selectedImageUri = result.getData().getData();
                // Update the preview in the still-open dialog
                if (dialogPreviewRef != null && selectedImageUri != null) {
                    try {
                        InputStream is = getContext().getContentResolver().openInputStream(selectedImageUri);
                        Bitmap bmp = BitmapFactory.decodeStream(is);
                        dialogPreviewRef.setImageBitmap(bmp);
                        dialogPreviewRef.setVisibility(View.VISIBLE);
                        if (is != null) is.close();
                    } catch (Exception e) { /* ignore */ }
                }
            }
        });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_square, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        postsList = view.findViewById(R.id.posts_list);
        tvEmpty = view.findViewById(R.id.tv_empty);
        fabPost = view.findViewById(R.id.fab_post);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        btnSortTime = view.findViewById(R.id.btn_sort_time);
        btnSortHot = view.findViewById(R.id.btn_sort_hot);
        notifBanner = view.findViewById(R.id.notif_banner);
        tvNotifText = view.findViewById(R.id.tv_notif_text);
        View btnNotifClose = view.findViewById(R.id.btn_notif_close);
        btnNotifClose.setOnClickListener(v -> {
            notifBanner.setVisibility(View.GONE);
            Reporter.squareMarkNotificationsRead(requireContext());
        });

        adapter = new PostsAdapter();
        postsList.setLayoutManager(new LinearLayoutManager(getContext()));
        postsList.setAdapter(adapter);

        fabPost.setOnClickListener(v -> showPostDialog());

        swipeRefresh.setColorSchemeColors(0xFF7986CB);
        swipeRefresh.setOnRefreshListener(this::loadPosts);

        btnSortTime.setOnClickListener(v -> setSort(0));
        btnSortHot.setOnClickListener(v -> setSort(1));

        loadPosts();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPosts();
    }

    private void setSort(int sort) {
        currentSort = sort;
        btnSortTime.setTextColor(sort == 0 ? 0xFF7986CB : 0xFFB0A89E);
        btnSortTime.setTypeface(null, sort == 0 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        btnSortTime.setBackgroundResource(sort == 0 ? R.drawable.bg_tab_selected : 0);

        btnSortHot.setTextColor(sort == 1 ? 0xFF7986CB : 0xFFB0A89E);
        btnSortHot.setTypeface(null, sort == 1 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        btnSortHot.setBackgroundResource(sort == 1 ? R.drawable.bg_tab_selected : 0);

        loadPosts();
    }

    private void applySort() {
        adapter.setPosts(allPosts);
        tvEmpty.setVisibility(allPosts.isEmpty() ? View.VISIBLE : View.GONE);
        postsList.setVisibility(allPosts.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void loadPosts() {
        swipeRefresh.setRefreshing(true);
        String sort = currentSort == 0 ? "time" : "hot";
        executor.execute(() -> {
            JSONObject result = Reporter.squareGetPosts(requireContext(), sort);
            JSONObject notifResult = Reporter.squareGetNotifications(requireContext());
            mainHandler.post(() -> {
                swipeRefresh.setRefreshing(false);
                try {
                    JSONArray posts = result.optJSONArray("posts");
                    if (posts == null) {
                        String str = result.toString();
                        posts = new JSONArray(str);
                    }
                    allPosts.clear();
                    for (int i = 0; i < posts.length(); i++) {
                        JSONObject p = posts.getJSONObject(i);
                        allPosts.add(new PostItem(
                            p.getInt("id"),
                            p.getString("content"),
                            p.optString("anonymous_name", "匿名"),
                            p.optString("device_id", ""),
                            p.optString("created_at", ""),
                            p.optInt("reply_count", 0),
                            p.optString("media_url", ""),
                            p.optInt("like_count", 0),
                            p.optBoolean("liked", false)
                        ));
                    }
                    applySort();

                    int unread = notifResult.optInt("unread", 0);
                    if (unread > 0 && !notifShown) {
                        notifShown = true;
                        JSONArray notifs = notifResult.optJSONArray("notifications");
                        StringBuilder msg = new StringBuilder();
                        for (int i = 0; i < Math.min(unread, 3); i++) {
                            JSONObject n = notifs.getJSONObject(i);
                            if (i > 0) msg.append("、");
                            msg.append(n.optString("from_anonymous_name", "匿名"));
                        }
                        if (unread > 3) msg.append(" 等");
                        msg.append(" 回复了你的帖子（").append(unread).append("条）");
                        tvNotifText.setText(msg.toString());
                        notifBanner.setVisibility(View.VISIBLE);
                    } else {
                        notifBanner.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    postsList.setVisibility(View.GONE);
                }
            });
        });
    }

    private void showPostDialog() {
        selectedImageUri = null;
        uploadedMediaUrl = null;
        dialogPreviewRef = null;

        LinearLayout dialogLayout = new LinearLayout(getContext());
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(48, 32, 48, 16);

        EditText input = new EditText(getContext());
        input.setHint("说点什么吧...");
        input.setMinLines(3);
        dialogLayout.addView(input);

        // Image preview (placeholder style)
        ImageView preview = new ImageView(getContext());
        preview.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 400));
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        preview.setVisibility(View.GONE);
        preview.setPadding(0, 16, 0, 0);
        preview.setBackgroundColor(0xFFF0F0F0);
        dialogLayout.addView(preview);
        dialogPreviewRef = preview;

        // Pick buttons row
        LinearLayout pickRow = new LinearLayout(getContext());
        pickRow.setOrientation(LinearLayout.HORIZONTAL);
        pickRow.setPadding(0, 16, 0, 0);

        TextView btnPickImage = new TextView(getContext());
        btnPickImage.setText("📷 图片");
        btnPickImage.setTextColor(0xFF7986CB);
        btnPickImage.setTextSize(13);
        btnPickImage.setPadding(0, 0, 24, 0);
        btnPickImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });
        pickRow.addView(btnPickImage);

        TextView btnPickVideo = new TextView(getContext());
        btnPickVideo.setText("🎬 视频");
        btnPickVideo.setTextColor(0xFF7986CB);
        btnPickVideo.setTextSize(13);
        btnPickVideo.setPadding(0, 0, 24, 0);
        btnPickVideo.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            intent.setType("video/*");
            imagePickerLauncher.launch(intent);
        });
        pickRow.addView(btnPickVideo);

        TextView btnPickAudio = new TextView(getContext());
        btnPickAudio.setText("🎵 音频");
        btnPickAudio.setTextColor(0xFF7986CB);
        btnPickAudio.setTextSize(13);
        btnPickAudio.setPadding(0, 0, 0, 0);
        btnPickAudio.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
            intent.setType("audio/*");
            imagePickerLauncher.launch(intent);
        });
        pickRow.addView(btnPickAudio);

        dialogLayout.addView(pickRow);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setTitle("📝 发帖")
            .setView(dialogLayout)
            .setPositiveButton("发布", null)
            .setNegativeButton("取消", null)
            .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String content = input.getText().toString().trim();
                if (content.isEmpty() && selectedImageUri == null) {
                    Toast.makeText(getContext(), "写点内容或选张图片~", Toast.LENGTH_SHORT).show();
                    return;
                }
                String filterErr = com.xinyu.app.util.ContentFilter.check(content);
                if (filterErr != null) {
                    Toast.makeText(getContext(), filterErr, Toast.LENGTH_SHORT).show();
                    return;
                }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("发布中...");

                if (selectedImageUri != null) {
                    mainHandler.post(() -> Toast.makeText(getContext(), "正在上传...", Toast.LENGTH_SHORT).show());
                    executor.execute(() -> {
                        String mediaUrl = null;
                        try {
                            String filePath = copyUriToFile(selectedImageUri);
                            if (filePath != null) {
                                JSONObject uploadResult = Reporter.uploadFile(new File(filePath));
                                if (uploadResult.has("url")) {
                                    mediaUrl = uploadResult.getString("url");
                                } else {
                                    mainHandler.post(() -> Toast.makeText(getContext(), "上传失败: " + uploadResult.optString("error", "未知错误"), Toast.LENGTH_SHORT).show());
                                }
                            } else {
                                mainHandler.post(() -> Toast.makeText(getContext(), "无法读取图片", Toast.LENGTH_SHORT).show());
                            }
                        } catch (Exception e) {
                            mainHandler.post(() -> Toast.makeText(getContext(), "上传出错: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                        final String url = mediaUrl;
                        mainHandler.post(() -> {
                            dialog.dismiss();
                            dialogPreviewRef = null;
                            createPost(content, url);
                        });
                    });
                } else {
                    dialog.dismiss();
                    dialogPreviewRef = null;
                    createPost(content, null);
                }
            });
        });

        dialog.setOnDismissListener(d -> dialogPreviewRef = null);
        dialog.show();
    }

    private String copyUriToFile(Uri uri) {
        try {
            InputStream is = getContext().getContentResolver().openInputStream(uri);
            if (is == null) return null;
            String ext = ".jpg";
            String path = uri.getPath();
            if (path != null) {
                if (path.contains(".png")) ext = ".png";
                else if (path.contains(".gif")) ext = ".gif";
                else if (path.contains(".webp")) ext = ".webp";
            }
            File tmp = new File(getContext().getCacheDir(), "upload_" + System.currentTimeMillis() + ext);
            FileOutputStream fos = new FileOutputStream(tmp);
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) > 0) fos.write(buf, 0, len);
            fos.close();
            is.close();
            return tmp.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    private void createPost(String content, String mediaUrl) {
        executor.execute(() -> {
            JSONObject result = Reporter.squareCreatePost(requireContext(), content, mediaUrl);
            mainHandler.post(() -> {
                try {
                    if (result.has("ok")) {
                        Toast.makeText(getContext(), "发布成功", Toast.LENGTH_SHORT).show();
                        loadPosts();
                    } else {
                        Toast.makeText(getContext(), result.optString("error", "发布失败"), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(getContext(), "网络错误", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showRepliesDialog(int postId, String postContent, String anonymousName) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_replies, null);
        LinearLayout replyList = dialogView.findViewById(R.id.reply_list);
        EditText etReply = dialogView.findViewById(R.id.et_reply);
        TextView tvPostContent = dialogView.findViewById(R.id.tv_post_content);
        TextView tvPostAuthor = dialogView.findViewById(R.id.tv_post_author);

        tvPostContent.setText(postContent);
        tvPostAuthor.setText(anonymousName);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setTitle("💬 回复")
            .setView(dialogView)
            .setPositiveButton("发送", (d, w) -> {
                String replyContent = etReply.getText().toString().trim();
                if (!replyContent.isEmpty()) {
                    String filterErr = com.xinyu.app.util.ContentFilter.check(replyContent);
                    if (filterErr != null) {
                        Toast.makeText(getContext(), filterErr, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    createReply(postId, replyContent);
                }
            })
            .setNegativeButton("关闭", null)
            .create();

        dialog.show();

        executor.execute(() -> {
            JSONObject result = Reporter.squareGetReplies(postId);
            mainHandler.post(() -> {
                try {
                    JSONArray replies = result.optJSONArray("replies");
                    if (replies == null) {
                        String str = result.toString();
                        replies = new JSONArray(str);
                    }
                    replyList.removeAllViews();
                    SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
                    for (int i = 0; i < replies.length(); i++) {
                        JSONObject r = replies.getJSONObject(i);
                        View itemView = LayoutInflater.from(getContext()).inflate(R.layout.item_square_reply, replyList, false);
                        TextView tvName = itemView.findViewById(R.id.tv_reply_name);
                        TextView tvContent = itemView.findViewById(R.id.tv_reply_content);
                        TextView tvTime = itemView.findViewById(R.id.tv_reply_time);
                        tvName.setText(r.optString("anonymous_name", "匿名"));
                        tvContent.setText(r.getString("content"));
                        String time = r.optString("created_at", "");
                        try {
                            tvTime.setText(sdf.format(new Date(Long.parseLong(time) * 1000)));
                        } catch (Exception e) {
                            tvTime.setText(time.length() > 16 ? time.substring(0, 16) : time);
                        }
                        replyList.addView(itemView);
                    }
                } catch (Exception e) {
                    // ignore
                }
            });
        });
    }

    private void createReply(int postId, String content) {
        executor.execute(() -> {
            JSONObject result = Reporter.squareCreateReply(requireContext(), postId, content);
            mainHandler.post(() -> {
                try {
                    if (result.has("ok")) {
                        Toast.makeText(getContext(), "回复成功", Toast.LENGTH_SHORT).show();
                        loadPosts();
                    }
                } catch (Exception e) {
                    Toast.makeText(getContext(), "网络错误", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // Post data model
    static class PostItem {
        int id;
        String content, anonymousName, deviceId, createdAt, mediaUrl;
        int replyCount, likeCount;
        boolean liked;

        PostItem(int id, String content, String anonymousName, String deviceId, String createdAt, int replyCount, String mediaUrl, int likeCount, boolean liked) {
            this.id = id;
            this.content = content;
            this.anonymousName = anonymousName;
            this.deviceId = deviceId;
            this.createdAt = createdAt;
            this.replyCount = replyCount;
            this.mediaUrl = mediaUrl;
            this.likeCount = likeCount;
            this.liked = liked;
        }
    }

    // Posts adapter
    class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.PostViewHolder> {
        private List<PostItem> posts = new ArrayList<>();

        void setPosts(List<PostItem> newPosts) {
            this.posts = newPosts;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_square_post, parent, false);
            return new PostViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
            PostItem post = posts.get(position);
            holder.tvName.setText(post.anonymousName);
            holder.tvContent.setText(post.content);
            holder.tvReplyCount.setText("💬 " + post.replyCount + " 条回复");
            holder.tvLike.setText(post.liked ? "❤️ " + post.likeCount : "🤍 " + post.likeCount);
            holder.tvLike.setTextColor(post.liked ? 0xFFE91E63 : 0xFFB0A89E);
            holder.tvLike.setOnClickListener(v -> toggleLike(post, holder));

            // Show media with placeholder
            if (post.mediaUrl != null && !post.mediaUrl.isEmpty()) {
                holder.mediaContainer.setVisibility(View.VISIBLE);
                String fullUrl = "http://121.41.211.25" + post.mediaUrl;
                String lower = post.mediaUrl.toLowerCase();
                if (lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".avi") || lower.endsWith(".webm")) {
                    // Video - show thumbnail with play icon
                    holder.ivMedia.setImageBitmap(null);
                    holder.ivMedia.setBackgroundColor(0xFF000000);
                    holder.tvPlaceholder.setVisibility(View.VISIBLE);
                    holder.tvPlaceholder.setText("🎬 点击播放视频");
                    loadBitmap(fullUrl.replace("http://", "https://"), holder.ivMedia, holder.tvPlaceholder);
                } else if (lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".ogg") || lower.endsWith(".m4a") || lower.endsWith(".aac")) {
                    // Audio - show music icon
                    holder.ivMedia.setImageBitmap(null);
                    holder.ivMedia.setBackgroundColor(0xFFF3E5F5);
                    holder.tvPlaceholder.setVisibility(View.VISIBLE);
                    holder.tvPlaceholder.setText("🎵 点击播放音频");
                } else {
                    // Image
                    holder.ivMedia.setImageBitmap(null);
                    holder.ivMedia.setBackgroundColor(0xFFF0F0F0);
                    holder.tvPlaceholder.setVisibility(View.VISIBLE);
                    holder.tvPlaceholder.setText("图片加载中...");
                    loadBitmap(fullUrl, holder.ivMedia, holder.tvPlaceholder);
                }
            } else {
                holder.mediaContainer.setVisibility(View.GONE);
            }

            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
            try {
                holder.tvTime.setText(sdf.format(new Date(Long.parseLong(post.createdAt) * 1000)));
            } catch (Exception e) {
                String t = post.createdAt;
                holder.tvTime.setText(t.length() > 16 ? t.substring(0, 16) : t);
            }

            String myDeviceId = Reporter.getDeviceId(requireContext());
            if (myDeviceId != null && myDeviceId.equals(post.deviceId)) {
                holder.btnDelete.setVisibility(View.VISIBLE);
                holder.btnDelete.setOnClickListener(v -> {
                    new AlertDialog.Builder(requireContext())
                        .setTitle("删除帖子")
                        .setMessage("确定删除这条帖子吗？")
                        .setPositiveButton("删除", (d, w) -> deletePost(post.id))
                        .setNegativeButton("取消", null)
                        .show();
                });
            } else {
                holder.btnDelete.setVisibility(View.GONE);
            }

            holder.btnReply.setOnClickListener(v -> {
                showRepliesDialog(post.id, post.content, post.anonymousName);
            });

            // Click image to view full screen
            if (post.mediaUrl != null && !post.mediaUrl.isEmpty()) {
                holder.mediaContainer.setOnClickListener(v -> showFullImage(post.mediaUrl));
            }
        }

        @Override
        public int getItemCount() {
            return posts.size();
        }

        class PostViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvContent, tvTime, tvReplyCount, btnReply, btnDelete, tvLike, tvPlaceholder;
            ImageView ivMedia;
            ViewGroup mediaContainer;

            PostViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_anonymous_name);
                tvContent = itemView.findViewById(R.id.tv_content);
                tvTime = itemView.findViewById(R.id.tv_time);
                tvReplyCount = itemView.findViewById(R.id.tv_reply_count);
                tvLike = itemView.findViewById(R.id.tv_like);
                btnReply = itemView.findViewById(R.id.btn_reply);
                btnDelete = itemView.findViewById(R.id.btn_delete);
                ivMedia = itemView.findViewById(R.id.iv_media);
                tvPlaceholder = itemView.findViewById(R.id.tv_image_placeholder);
                mediaContainer = itemView.findViewById(R.id.media_container);
            }
        }
    }

    private void loadBitmap(String url, ImageView imageView, TextView placeholder) {
        executor.execute(() -> {
            try {
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                java.io.InputStream is = conn.getInputStream();
                Bitmap bmp = BitmapFactory.decodeStream(is);
                is.close();
                conn.disconnect();
                if (bmp != null) {
                    mainHandler.post(() -> {
                        imageView.setImageBitmap(bmp);
                        imageView.setBackgroundColor(Color.TRANSPARENT);
                        if (placeholder != null) placeholder.setVisibility(View.GONE);
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (placeholder != null) placeholder.setText("图片加载失败");
                });
            }
        });
    }

    private void showFullImage(String mediaUrl) {
        android.app.Dialog dialog = new android.app.Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.BLACK);
        layout.setGravity(android.view.Gravity.CENTER);

        TextView btnClose = new TextView(requireContext());
        btnClose.setText("✕");
        btnClose.setTextColor(Color.WHITE);
        btnClose.setTextSize(24);
        btnClose.setPadding(32, 32, 32, 16);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        layout.addView(btnClose);

        String fullUrl = "http://121.41.211.25" + mediaUrl;
        String lower = mediaUrl.toLowerCase();

        if (lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".avi") || lower.endsWith(".webm")) {
            // Video playback - open in browser
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(fullUrl));
                startActivity(intent);
            } catch (Exception e) {
                TextView tvErr = new TextView(requireContext());
                tvErr.setText("无法播放视频");
                tvErr.setTextColor(Color.WHITE);
                tvErr.setTextSize(16);
                layout.addView(tvErr);
            }
            dialog.setContentView(layout);
            dialog.show();
        } else if (lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".ogg") || lower.endsWith(".m4a") || lower.endsWith(".aac")) {
            // Audio playback
            TextView tvLabel = new TextView(requireContext());
            tvLabel.setText("🎵 音频播放");
            tvLabel.setTextColor(Color.WHITE);
            tvLabel.setTextSize(18);
            tvLabel.setGravity(android.view.Gravity.CENTER);
            tvLabel.setPadding(0, 40, 0, 20);
            layout.addView(tvLabel);

            TextView tvHint = new TextView(requireContext());
            tvHint.setText("正在加载音频...");
            tvHint.setTextColor(Color.WHITE);
            tvHint.setTextSize(14);
            tvHint.setGravity(android.view.Gravity.CENTER);
            tvHint.setPadding(0, 20, 0, 0);
            layout.addView(tvHint);

            dialog.setContentView(layout);
            dialog.show();

            // Try to open audio in browser
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(fullUrl));
                intent.setType("audio/*");
                startActivity(intent);
            } catch (Exception e) {
                tvHint.setText("无法播放音频");
            }
        } else {
            // Image display
            ImageView fullImage = new ImageView(requireContext());
            fullImage.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            fullImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
            fullImage.setAdjustViewBounds(true);
            fullImage.setBackgroundColor(Color.BLACK);
            layout.addView(fullImage);

            TextView tvLoading = new TextView(requireContext());
            tvLoading.setText("加载中...");
            tvLoading.setTextColor(Color.WHITE);
            tvLoading.setTextSize(14);
            tvLoading.setGravity(android.view.Gravity.CENTER);
            tvLoading.setPadding(0, 200, 0, 0);
            layout.addView(tvLoading);

            dialog.setContentView(layout);

            executor.execute(() -> {
                try {
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(fullUrl).openConnection();
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);
                    java.io.InputStream is = conn.getInputStream();
                    Bitmap bmp = BitmapFactory.decodeStream(is);
                    is.close();
                    conn.disconnect();
                    if (bmp != null) {
                        mainHandler.post(() -> {
                            fullImage.setImageBitmap(bmp);
                            tvLoading.setVisibility(View.GONE);
                        });
                    } else {
                        mainHandler.post(() -> tvLoading.setText("图片加载失败"));
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> tvLoading.setText("图片加载失败"));
                }
            });

            dialog.show();
        }
    }

    private void toggleLike(PostItem post, PostsAdapter.PostViewHolder holder) {
        holder.tvLike.setEnabled(false);
        executor.execute(() -> {
            JSONObject result = Reporter.squareToggleLike(requireContext(), post.id);
            mainHandler.post(() -> {
                holder.tvLike.setEnabled(true);
                try {
                    if (result.has("ok")) {
                        post.liked = result.getBoolean("liked");
                        post.likeCount = result.getInt("like_count");
                        holder.tvLike.setText(post.liked ? "❤️ " + post.likeCount : "🤍 " + post.likeCount);
                        holder.tvLike.setTextColor(post.liked ? 0xFFE91E63 : 0xFFB0A89E);
                    }
                } catch (Exception e) {
                    Toast.makeText(getContext(), "操作失败", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void deletePost(int postId) {
        executor.execute(() -> {
            JSONObject result = Reporter.squareDeletePost(requireContext(), postId);
            mainHandler.post(() -> {
                try {
                    if (result.has("ok")) {
                        Toast.makeText(getContext(), "已删除", Toast.LENGTH_SHORT).show();
                        loadPosts();
                    } else {
                        Toast.makeText(getContext(), result.optString("error", "删除失败"), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(getContext(), "网络错误", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
