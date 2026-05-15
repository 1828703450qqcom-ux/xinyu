package com.xinyu.app.util;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class WeatherHelper {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final String SERVER = "http://39.106.54.47";

    static {
        try {
            TrustManager[] trustAll = new TrustManager[]{new X509TrustManager(){
                public X509Certificate[] getAcceptedIssuers(){return null;}
                public void checkClientTrusted(X509Certificate[] c,String a){}
                public void checkServerTrusted(X509Certificate[] c,String a){}
            }};
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAll, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((h,s)->true);
        }catch(Exception ignored){}
    }

    public interface WeatherCallback {
        void onWeatherLoaded(String city, String temp, String desc, String detail, String icon);
        void onError(String error);
    }

    public static void fetchWeather(WeatherCallback callback) {
        fetchWeather(null, callback);
    }

    public static void fetchWeather(String city, WeatherCallback callback) {
        executor.execute(() -> {
            try {
                String path = "/api/weather";
                if (city != null && !city.isEmpty()) {
                    path += "?city=" + URLEncoder.encode(city, "UTF-8");
                }
                URL url = new URL(SERVER + path);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                int code = conn.getResponseCode();
                java.io.InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
                byte[] buf = is.readAllBytes();
                conn.disconnect();

                JSONObject data = new JSONObject(new String(buf, "UTF-8"));

                if (data.has("error")) {
                    postError(callback, data.getString("error"));
                    return;
                }

                final String fCity = data.getString("city");
                final String fTemp = data.getString("temp");
                final String fDesc = data.getString("desc");
                final String fDetail = data.getString("detail");
                final String fIcon = data.getString("icon");

                mainHandler.post(() -> callback.onWeatherLoaded(fCity, fTemp, fDesc, fDetail, fIcon));

            } catch (Exception e) {
                e.printStackTrace();
                postError(callback, "天气获取失败");
            }
        });
    }

    private static void postError(WeatherCallback callback, String msg) {
        mainHandler.post(() -> callback.onError(msg));
    }
}
