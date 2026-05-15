package com.xinyu.app.util;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

public class CacheManager {
    private static final String PREFS_NAME = "xinyu_cache";

    public static void saveMoods(Context ctx, String username, JSONArray moods) {
        getPrefs(ctx).edit().putString("moods_" + username, moods.toString()).apply();
    }

    public static JSONArray loadMoods(Context ctx, String username) {
        try {
            String json = getPrefs(ctx).getString("moods_" + username, "[]");
            return new JSONArray(json);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public static void savePosts(Context ctx, String sort, JSONArray posts) {
        getPrefs(ctx).edit().putString("posts_" + sort, posts.toString()).apply();
    }

    public static JSONArray loadPosts(Context ctx, String sort) {
        try {
            String json = getPrefs(ctx).getString("posts_" + sort, "[]");
            return new JSONArray(json);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public static void saveNotifications(Context ctx, JSONArray notifs) {
        getPrefs(ctx).edit().putString("notifications", notifs.toString()).apply();
    }

    public static JSONArray loadNotifications(Context ctx) {
        try {
            String json = getPrefs(ctx).getString("notifications", "[]");
            return new JSONArray(json);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private static SharedPreferences getPrefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
