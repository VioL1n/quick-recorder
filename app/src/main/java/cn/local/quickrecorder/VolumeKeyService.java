package cn.local.quickrecorder;
import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.*;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public final class VolumeKeyService extends AccessibilityService {
    private final DoublePress gesture = new DoublePress();
    static volatile boolean connected;
    @Override protected void onServiceConnected() { connected = true; getSharedPreferences("state",0).edit().remove("wakeMode").apply(); Status.note(this, "音量键服务已连接；尚需开启应用内快捷键开关"); }
    @Override public void onDestroy() { connected = false; super.onDestroy(); }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { gesture.reset(); }
    @Override protected boolean onKeyEvent(KeyEvent e) {
        if (e.getKeyCode() != KeyEvent.KEYCODE_VOLUME_DOWN) { gesture.reset(); return false; }
        if (!getSharedPreferences("state",0).getBoolean("hotkey",false)) { gesture.reset(); return false; }
        PowerManager power = (PowerManager)getSystemService(POWER_SERVICE);
        KeyguardManager keyguard = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        if (power.isInteractive() && !keyguard.isKeyguardLocked()) { gesture.reset(); return false; }
        if (e.getAction() != KeyEvent.ACTION_DOWN && e.getAction() != KeyEvent.ACTION_UP) return false;
        if (gesture.event(e.getAction() == KeyEvent.ACTION_DOWN, e.getRepeatCount(), e.getEventTime())) {
            Status.note(this, "已收到锁屏/熄屏双击");
            if (Status.recording) {
                stopService(new Intent(this, RecorderService.class));
            } else if (!Status.starting) {
                RecorderService.requestStart(this);
            }
        }
        return false; // Never consume normal volume operation.
    }
}
