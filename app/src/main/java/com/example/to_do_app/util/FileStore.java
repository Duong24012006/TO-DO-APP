package com.example.to_do_app.util;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * FileStore - helper để lưu/đọc/xóa các file JSON trong internal storage (private cho app).
 *
 * Usage:
 *   FileStore.saveJson(context, "home_display_user_x_day_2.json", jsonString);
 *   String json = FileStore.readJson(context, "home_display_user_x_day_2.json");
 *   FileStore.deleteFile(context, filename);
 *   FileStore.saveObjectAsJson(context, filename, someObject); // uses Gson
 */
public final class FileStore {
    private static final String TAG = "FileStore";
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private FileStore() { /* no instances */ }

    public static boolean saveJson(Context ctx, String filename, String json) {
        if (ctx == null || filename == null || filename.isEmpty() || json == null) return false;
        FileOutputStream fos = null;
        Writer writer = null;
        try {
            fos = ctx.openFileOutput(filename, Context.MODE_PRIVATE);
            writer = new OutputStreamWriter(fos, "UTF-8");
            writer.write(json);
            writer.flush();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "saveJson error", e);
            return false;
        } finally {
            try { if (writer != null) writer.close(); } catch (Exception ignored) {}
            try { if (fos != null) fos.close(); } catch (Exception ignored) {}
        }
    }

    public static String readJson(Context ctx, String filename) {
        if (ctx == null || filename == null || filename.isEmpty()) return null;
        FileInputStream fis = null;
        BufferedReader br = null;
        try {
            fis = ctx.openFileInput(filename);
            br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            Log.w(TAG, "readJson error for " + filename + " : " + e.getMessage());
            return null;
        } finally {
            try { if (br != null) br.close(); } catch (Exception ignored) {}
            try { if (fis != null) fis.close(); } catch (Exception ignored) {}
        }
    }

    public static boolean deleteFile(Context ctx, String filename) {
        if (ctx == null || filename == null || filename.isEmpty()) return false;
        try {
            return ctx.deleteFile(filename);
        } catch (Exception e) {
            Log.w(TAG, "deleteFile error", e);
            return false;
        }
    }

    public static boolean saveObjectAsJson(Context ctx, String filename, Object obj) {
        if (obj == null) return false;
        String json = GSON.toJson(obj);
        return saveJson(ctx, filename, json);
    }

    public static <T> T readJsonAsObject(Context ctx, String filename, Class<T> clazz) {
        String json = readJson(ctx, filename);
        if (json == null) return null;
        try {
            return GSON.fromJson(json, clazz);
        } catch (Exception e) {
            Log.w(TAG, "readJsonAsObject error", e);
            return null;
        }
    }

    public static String filePath(Context ctx, String filename) {
        if (ctx == null || filename == null) return null;
        File f = new File(ctx.getFilesDir(), filename);
        return f.getAbsolutePath();
    }
}