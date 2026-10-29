package cn.local.quickrecorder;
import android.content.*;
import android.os.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

final class Status {
    static volatile boolean recording, starting;
    static volatile long startedAt;
    static volatile String activeName = "";
    static File dir(Context c) { File d = new File(c.getFilesDir(), "recordings"); if (!d.exists() && !d.mkdirs()) throw new IllegalStateException("无法创建本地录音目录"); return d; }
    static void note(Context c, String text) {
        String stamp = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(new Date());
        android.content.SharedPreferences p = c.getSharedPreferences("state", 0);
        String history = stamp + " " + text + "\n" + p.getString("log", "");
        p.edit().putString("last", text).putString("log", history.substring(0, Math.min(7000, history.length()))).apply();
    }
    static void buzz(Context c, boolean ok) {
        Vibrator v = (Vibrator)c.getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) v.vibrate(VibrationEffect.createWaveform(ok ? new long[]{0,60} : new long[]{0,80,80,80,80,80}, -1));
    }
    static void error(Context c, String prefix, Exception e) {
        note(c, prefix + "：" + e.getClass().getSimpleName() + " " + String.valueOf(e.getMessage()));
        buzz(c, false);
    }
}
