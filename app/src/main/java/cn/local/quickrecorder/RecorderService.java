package cn.local.quickrecorder;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.media.MediaRecorder;
import android.os.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public final class RecorderService extends Service {
    static final String CHANNEL = "recording";
    private MediaRecorder recorder;
    private File pending;
    private boolean recorderFailed;
    static void channel(Context c) {
        NotificationManager n = c.getSystemService(NotificationManager.class);
        n.createNotificationChannel(new NotificationChannel(CHANNEL, "正在录音（请保持开启）", NotificationManager.IMPORTANCE_LOW));
    }
    static boolean notificationsReady(Context c) {
        channel(c);
        NotificationManager n = c.getSystemService(NotificationManager.class);
        return (Build.VERSION.SDK_INT < 33 || c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                && n.areNotificationsEnabled() && n.getNotificationChannel(CHANNEL).getImportance() != NotificationManager.IMPORTANCE_NONE;
    }
    static void requestStart(Context c) {
        if (Status.recording || Status.starting) return;
        if (c.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED || !notificationsReady(c)) {
            Status.note(c, "未开始：请先允许麦克风和录音通知"); Status.buzz(c,false); return;
        }
        Status.starting = true;
        try { c.startForegroundService(new Intent(c,RecorderService.class)); }
        catch (Exception e) { Status.starting = false; Status.error(c,"录音启动被系统拒绝",e); }
    }
    private Notification notification(String text) {
        PendingIntent open = PendingIntent.getActivity(this, 1, new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop = PendingIntent.getService(this, 2, new Intent(this,RecorderService.class).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = new Notification.Builder(this,CHANNEL).setSmallIcon(cn.local.quickrecorder.R.drawable.ic_mic)
            .setContentTitle("快捷录音").setContentText(text).setContentIntent(open)
            .setOngoing(true).setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(new Notification.Action.Builder(null,"停止并保存",stop).build());
        if (Build.VERSION.SDK_INT >= 31) builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        return builder.build();
    }
    @Override public int onStartCommand(Intent intent,int flags,int id) {
        if (intent != null && "STOP".equals(intent.getAction())) { stopSelf(); return START_NOT_STICKY; }
        if (Status.recording) return START_NOT_STICKY;
        try {
            if (!notificationsReady(this)) throw new SecurityException("录音通知未开启");
            startForeground(10,notification("正在准备录音…"),ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            pending = new File(Status.dir(this),new SimpleDateFormat("yyyyMMdd_HHmmss_SSS",Locale.US).format(new Date()) + ".pending");
            recorder = Build.VERSION.SDK_INT >= 31 ? new MediaRecorder(this) : new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioSamplingRate(44100); recorder.setAudioEncodingBitRate(128000);
            recorder.setOutputFile(pending.getAbsolutePath());
            recorder.setOnErrorListener((r,what,extra) -> {
                recorderFailed = true; Status.note(this,"录音器错误：" + what + "/" + extra); Status.buzz(this,false); stopSelf();
            });
            recorder.prepare(); recorder.start();
            Status.recording = true; Status.starting = false; Status.startedAt = SystemClock.elapsedRealtime(); Status.activeName = pending.getName();
            getSystemService(NotificationManager.class).notify(10,notification("正在录音 · 点击停止并保存"));
            Status.note(this,"录音已启动；是否有声音请保存后试听"); Status.buzz(this,true);
        } catch (Exception e) {
            Status.error(this,"未能开始录音，请解锁后在首页重试",e);
            cleanup(false); stopSelf();
        }
        return START_NOT_STICKY;
    }
    private void cleanup(boolean save) {
        boolean valid = false;
        if (recorder != null) {
            try { if (Status.recording && save) { recorder.stop(); valid = true; } }
            catch (RuntimeException e) { Status.error(this,"停止失败，录音过短或音频不可用，未保存",e); }
            finally { try { recorder.release(); } catch (RuntimeException e) { Status.error(this,"释放录音器失败",e); } recorder = null; }
        }
        if (pending != null) {
            if (valid) {
                File saved = new File(pending.getParentFile(),pending.getName().replace(".pending",".m4a"));
                if (pending.length()>0 && pending.renameTo(saved)) { Status.note(this,"已保存：" + saved.getName()); Status.buzz(this,true); }
                else { Status.note(this,"保存失败，请检查存储空间"); Status.buzz(this,false); pending.delete(); }
            } else pending.delete();
            pending = null;
        }
        Status.recording = false; Status.starting = false; Status.activeName = "";
        stopForeground(STOP_FOREGROUND_REMOVE);
    }
    @Override public void onDestroy() { cleanup(!recorderFailed); super.onDestroy(); }
    @Override public IBinder onBind(Intent i) { return null; }
}
