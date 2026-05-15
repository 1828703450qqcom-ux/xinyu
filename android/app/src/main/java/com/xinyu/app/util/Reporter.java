package com.xinyu.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class Reporter {

    public static final String SERVER = "http://39.106.54.47";

    private static final TrustManager[] trustAllCerts = new TrustManager[]{
        new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() { return null; }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        }
    };

    static {
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception ignored) {}
    }
    private static final String PREFS = "xinyu";

    public static String getDeviceId(Context ctx) {
        return Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private static String getNickname(Context ctx) {
        return getPrefs(ctx).getString("current_nickname", "");
    }

    private static SharedPreferences getPrefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // 心跳 — App启动时调用
    public static void heartbeat(Context ctx) {
        new Thread(() -> post(ctx, "/api/heartbeat", buildPayload(ctx, "heartbeat", null))).start();
    }

    // 上报事件
    public static void report(Context ctx, String eventType, String data) {
        new Thread(() -> post(ctx, "/api/report", buildPayload(ctx, eventType, data))).start();
    }

    // 用户注册（服务器端）
    public static JSONObject serverRegister(String username, String password, String nickname, String gender) {
        try {
            JSONObject body = new JSONObject();
            body.put("username", username);
            body.put("password", password);
            body.put("nickname", nickname);
            body.put("gender", gender);
            body.put("platform", "app");
            return postSync("/api/user/register", body);
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    // 用户登录（服务器端）
    public static JSONObject serverLogin(String username, String password) {
        try {
            JSONObject body = new JSONObject();
            body.put("username", username);
            body.put("password", password);
            body.put("platform", "app");
            return postSync("/api/user/login", body);
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    // 验证session是否有效（检查是否被踢下线）
    public static JSONObject validateSession(Context ctx) {
        try {
            SharedPreferences prefs = getPrefs(ctx);
            String username = prefs.getString("current_user", "");
            String token = prefs.getString("session_token", "");
            if (username.isEmpty() || token.isEmpty()) {
                return errorJson("未登录");
            }
            JSONObject body = new JSONObject();
            body.put("username", username);
            body.put("session_token", token);
            return postSync("/api/user/validate", body);
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    // ===== 匿名广场 =====
    public static JSONObject squareGetPosts(Context ctx) {
        return squareGetPosts(ctx, "time");
    }

    public static JSONObject squareGetPosts(Context ctx, String sort) {
        try {
            JSONObject result = getSync("/api/square/posts?device_id=" + getDeviceId(ctx) + "&sort=" + sort);
            // Cache successful result
            if (!result.has("error") && result.has("posts")) {
                CacheManager.savePosts(ctx, sort, result.getJSONArray("posts"));
            }
            return result;
        } catch (Exception e) {
            // Return cached data when offline
            org.json.JSONArray cached = CacheManager.loadPosts(ctx, sort);
            if (cached.length() > 0) {
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("posts", cached);
                    return obj;
                } catch (Exception ex) {}
            }
            return errorJson("网络不可用，显示缓存内容");
        }
    }

    public static JSONObject squareToggleLike(Context ctx, int postId) {
        try {
            JSONObject body = new JSONObject();
            body.put("device_id", getDeviceId(ctx));
            return postSync("/api/square/posts/" + postId + "/like", body);
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    public static JSONObject squareCreatePost(Context ctx, String content, String mediaUrl) {
        try {
            JSONObject body = new JSONObject();
            body.put("content", content);
            body.put("device_id", getDeviceId(ctx));
            if (mediaUrl != null && !mediaUrl.isEmpty()) {
                body.put("media_url", mediaUrl);
            }
            return postSync("/api/square/posts", body);
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    public static JSONObject uploadFile(java.io.File file) {
        try {
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            URL url = new URL(SERVER + "/api/upload");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(120000);
            conn.setDoOutput(true);
            java.io.OutputStream os = conn.getOutputStream();
            os.write(("--" + boundary + "\r\n").getBytes());
            os.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n").getBytes());
            os.write(("Content-Type: application/octet-stream\r\n\r\n").getBytes());
            java.io.FileInputStream fis = new java.io.FileInputStream(file);
            byte[] buf = new byte[4096];
            int len;
            while ((len = fis.read(buf)) > 0) os.write(buf, 0, len);
            fis.close();
            os.write(("\r\n--" + boundary + "--\r\n").getBytes());
            os.close();
            int code = conn.getResponseCode();
            java.io.InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            byte[] res = is.readAllBytes();
            conn.disconnect();
            return new JSONObject(new String(res, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    public static JSONObject importCourseFile(java.io.File file) {
        try {
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            URL url = new URL(SERVER + "/api/course/import");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);
            java.io.OutputStream os = conn.getOutputStream();
            os.write(("--" + boundary + "\r\n").getBytes());
            os.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n").getBytes());
            os.write(("Content-Type: application/octet-stream\r\n\r\n").getBytes());
            java.io.FileInputStream fis = new java.io.FileInputStream(file);
            byte[] buf = new byte[4096];
            int len;
            while ((len = fis.read(buf)) > 0) os.write(buf, 0, len);
            fis.close();
            os.write(("\r\n--" + boundary + "--\r\n").getBytes());
            os.close();
            int code = conn.getResponseCode();
            java.io.InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            byte[] res = is.readAllBytes();
            conn.disconnect();
            return new JSONObject(new String(res, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    public static JSONObject squareGetReplies(int postId) {
        try {
            return getSync("/api/square/posts/" + postId + "/replies");
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    public static JSONObject squareCreateReply(Context ctx, int postId, String content) {
        try {
            JSONObject body = new JSONObject();
            body.put("content", content);
            body.put("device_id", getDeviceId(ctx));
            return postSync("/api/square/posts/" + postId + "/replies", body);
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    public static JSONObject squareDeletePost(Context ctx, int postId) {
        try {
            JSONObject body = new JSONObject();
            body.put("device_id", getDeviceId(ctx));
            return deleteSync("/api/square/posts/" + postId, body);
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    public static JSONObject squareGetNotifications(Context ctx) {
        try {
            return getSync("/api/square/notifications?device_id=" + getDeviceId(ctx));
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    public static JSONObject squareMarkNotificationsRead(Context ctx) {
        try {
            JSONObject body = new JSONObject();
            body.put("device_id", getDeviceId(ctx));
            return postSync("/api/square/notifications/read", body);
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    // ===== 情感互助 =====
    public static JSONObject supportCreateProfile(Context ctx, String moodTags, String interestTags, String bio) {
        try {
            JSONObject body = new JSONObject();
            body.put("device_id", getDeviceId(ctx));
            body.put("mood_tags", new org.json.JSONArray(moodTags));
            body.put("interest_tags", new org.json.JSONArray(interestTags));
            body.put("bio", bio);
            return postSync("/api/support/profile", body);
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    public static JSONObject supportGetProfile(Context ctx) {
        try {
            return getSync("/api/support/profile?device_id=" + getDeviceId(ctx));
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    public static JSONObject supportDiscover(Context ctx) {
        try {
            return getSync("/api/support/discover?device_id=" + getDeviceId(ctx));
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    public static JSONObject supportMatch(Context ctx, String targetDeviceId) {
        try {
            JSONObject body = new JSONObject();
            body.put("device_id", getDeviceId(ctx));
            body.put("target_device_id", targetDeviceId);
            return postSync("/api/support/match", body);
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    public static JSONObject supportGetMatches(Context ctx) {
        try {
            return getSync("/api/support/matches?device_id=" + getDeviceId(ctx));
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    public static JSONObject supportGetMessages(int matchId) {
        try {
            return getSync("/api/support/messages?match_id=" + matchId);
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    public static JSONObject supportSendMessage(Context ctx, int matchId, String content) {
        try {
            JSONObject body = new JSONObject();
            body.put("match_id", matchId);
            body.put("device_id", getDeviceId(ctx));
            body.put("content", content);
            return postSync("/api/support/messages", body);
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    public static JSONObject supportReport(Context ctx, String targetDeviceId, String reason) {
        try {
            JSONObject body = new JSONObject();
            body.put("device_id", getDeviceId(ctx));
            body.put("target_device_id", targetDeviceId);
            body.put("reason", reason);
            return postSync("/api/support/report", body);
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    public static JSONObject supportSearch(Context ctx, String keyword) {
        try {
            return getSync("/api/support/search?device_id=" + getDeviceId(ctx) + "&keyword=" + java.net.URLEncoder.encode(keyword, "UTF-8"));
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    public static JSONObject supportUnmatch(Context ctx, int matchId) {
        try {
            JSONObject body = new JSONObject();
            body.put("device_id", getDeviceId(ctx));
            body.put("match_id", matchId);
            return postSync("/api/support/unmatch", body);
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    // ===== 测评同步 =====
    public static JSONObject syncAssessment(String username, String testId, String testTitle,
                                            int score, String level, String description, long createdAt) {
        try {
            JSONObject body = new JSONObject();
            body.put("username", username);
            body.put("test_id", testId);
            body.put("test_title", testTitle);
            body.put("score", score);
            body.put("level", level);
            body.put("description", description);
            body.put("created_at", createdAt);
            return postSync("/api/assessment/sync", body);
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    public static JSONObject getServerAssessments(String username) {
        try {
            return getSync("/api/assessment/results?username=" + username);
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    // ===== 版本更新 =====
    public static JSONObject checkAppVersion() {
        try {
            return getSync("/api/app/version");
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    // ===== 密码找回 =====
    public static JSONObject getSecurityQuestion(String username) {
        try {
            return getSync("/api/user/security/question?username=" + java.net.URLEncoder.encode(username, "UTF-8"));
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    public static JSONObject verifySecurityAnswer(String username, String answer) {
        try {
            JSONObject body = new JSONObject();
            body.put("username", username);
            body.put("answer", answer);
            return postSync("/api/user/security/verify", body);
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    public static JSONObject resetPassword(String username, String resetToken, String newPassword) {
        try {
            JSONObject body = new JSONObject();
            body.put("username", username);
            body.put("reset_token", resetToken);
            body.put("new_password", newPassword);
            return postSync("/api/user/security/reset", body);
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    public static JSONObject setSecurityQuestion(String username, String question, String answer) {
        try {
            JSONObject body = new JSONObject();
            body.put("username", username);
            body.put("question", question);
            body.put("answer", answer);
            return postSync("/api/user/security/set", body);
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    // ===== 消息已读 =====
    public static JSONObject markMessagesRead(Context ctx, int matchId) {
        try {
            JSONObject body = new JSONObject();
            body.put("match_id", matchId);
            body.put("device_id", getDeviceId(ctx));
            return postSync("/api/support/messages/read", body);
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    private static JSONObject deleteSync(String path, JSONObject body) {
        try {
            URL url = new URL(SERVER + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            os.close();
            int code = conn.getResponseCode();
            java.io.InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            byte[] buf = is.readAllBytes();
            conn.disconnect();
            return new JSONObject(new String(buf, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    private static JSONObject getSync(String path) {
        try {
            URL url = new URL(SERVER + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            int code = conn.getResponseCode();
            java.io.InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            byte[] buf = is.readAllBytes();
            conn.disconnect();
            return new JSONObject(new String(buf, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    private static JSONObject buildPayload(Context ctx, String eventType, String data) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("device_id", getDeviceId(ctx));
            obj.put("nickname", getNickname(ctx));
            obj.put("event_type", eventType);
            if (data != null) obj.put("data", data);
            return obj;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private static void post(Context ctx, String path, JSONObject body) {
        try {
            URL url = new URL(SERVER + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            os.close();
            conn.getResponseCode(); // trigger request
            conn.disconnect();
        } catch (Exception ignored) {}
    }

    private static JSONObject postSync(String path, JSONObject body) {
        try {
            URL url = new URL(SERVER + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            os.close();
            int code = conn.getResponseCode();
            java.io.InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            byte[] buf = is.readAllBytes();
            conn.disconnect();
            return new JSONObject(new String(buf, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    private static JSONObject errorJson(String msg) {
        try {
            JSONObject o = new JSONObject();
            o.put("error", msg != null ? msg : "网络错误");
            return o;
        } catch (Exception e) { return new JSONObject(); }
    }
}
