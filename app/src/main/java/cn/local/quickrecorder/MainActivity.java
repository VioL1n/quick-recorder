package cn.local.quickrecorder;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public final class MainActivity extends Activity {
    private Ui ui;
    private LinearLayout root, files;
    private TextView stateLabel, timer, detail, shortcut, count, connection, log;
    private Button recordButton;
    private MediaPlayer player;
    private String playingName="", page="home";
    private boolean observedRecording;
    private String buttonStyle="";
    private android.window.OnBackInvokedCallback backCallback;
    private boolean backRegistered;
    private LinearLayout shortcutCard;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() { public void run() { updateStatus(); handler.postDelayed(this,500); } };
    private android.content.SharedPreferences prefs() { return getSharedPreferences("state",0); }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); prefs().edit().remove("wakeMode").apply(); ui=new Ui(this);
        if(Build.VERSION.SDK_INT>=33) backCallback=this::navigateBack;
        page=state==null ? "home" : state.getString("page","home"); show(page);
    }
    private void show(String next) {
        releasePlayer(); page=next;buttonStyle="";
        stateLabel=null;timer=null;detail=null;shortcut=null;count=null;connection=null;log=null;recordButton=null;files=null;
        root=ui.column();
        if("settings".equals(page)) settings(); else if("diagnostics".equals(page)) diagnostics(); else { page="home";home(); }
        if(Build.VERSION.SDK_INT>=33) {
            if(backRegistered) {getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCallback);backRegistered=false;}
            if(!"home".equals(page)) {getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,backCallback);backRegistered=true;}
        }
        ui.mount(root); updateStatus();
    }
    private void home() {
        ui.header(root,"快捷录音","只在本机 · 随手记录","设置",() -> show("settings"));
        LinearLayout hero=ui.card(); ui.add(root,hero,28);
        View mic=ui.mic(); LinearLayout.LayoutParams icon=new LinearLayout.LayoutParams(ui.dp(64),ui.dp(64));icon.gravity=Gravity.CENTER_HORIZONTAL;hero.addView(mic,icon);
        stateLabel=ui.centered("准备就绪",14,ui.muted,true); ui.add(hero,stateLabel,18);
        timer=ui.centered("00:00",54,ui.text,false); timer.setTypeface(android.graphics.Typeface.create("sans-serif-light",0));timer.setMaxLines(1);timer.setAutoSizeTextTypeUniformWithConfiguration(24,54,1,android.util.TypedValue.COMPLEX_UNIT_SP); ui.add(hero,timer,12);
        TextView hint=ui.centered("让每一段声音，好好留下",13,ui.muted,false); ui.add(hero,hint,10);
        recordButton=ui.button("开始录音",true,() -> {
            releasePlayer();refreshFiles();
            if(Status.recording) { stopService(new Intent(this,RecorderService.class)); handler.postDelayed(this::refreshFiles,300); }
            else if(!Status.starting) {
                if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) requestPermissions();
                else if(!RecorderService.notificationsReady(this)) { show("settings"); Toast.makeText(this,"请允许录音通知后再开始",Toast.LENGTH_LONG).show(); }
                else RecorderService.requestStart(this);
            }
            updateStatus();
        }); ui.add(hero,recordButton,28);
        detail=ui.centered("",12,ui.muted,false); detail.setMaxLines(3); ui.add(hero,detail,16);
        shortcutCard=ui.card(); shortcut=ui.label("",14,ui.text,true); shortcutCard.addView(shortcut);
        ui.add(shortcutCard,ui.label("锁屏双击音量下键，开始 / 停止",12,ui.muted,false),6);
        shortcutCard.setOnClickListener(v -> show("settings")); shortcutCard.setFocusable(true); shortcutCard.setContentDescription("锁屏快捷键状态，点击进入设置"); ui.add(root,shortcutCard,14);
        LinearLayout heading=ui.row(); count=ui.label("本机录音",20,ui.text,true); heading.addView(count,new LinearLayout.LayoutParams(0,-2,1));
        heading.addView(ui.button("刷新",false,this::refreshFiles),new LinearLayout.LayoutParams(-2,-2)); ui.add(root,heading,28);
        files=ui.column(); ui.add(root,files,12); refreshFiles();
        TextView footer=ui.centered("不上传 · 无账号 · 无广告",12,ui.muted,false); ui.add(root,footer,24);
    }
    private String elapsed() {
        long s=Status.recording ? Math.max(0,(SystemClock.elapsedRealtime()-Status.startedAt)/1000) : 0;
        return s>=3600 ? String.format(Locale.US,"%d:%02d:%02d",s/3600,s/60%60,s%60) : String.format(Locale.US,"%02d:%02d",s/60,s%60);
    }
    private void updateStatus() {
        if((Status.recording || Status.starting) && player!=null) {releasePlayer();refreshFiles();}
        if(observedRecording && !Status.recording) refreshFiles();
        observedRecording=Status.recording;
        if(stateLabel!=null) {
            stateLabel.setText(Status.recording ? "●  正在录音" : Status.starting ? "正在准备…" : "准备就绪");
            stateLabel.setTextColor(Status.recording ? ui.danger : ui.muted);
            timer.setText(elapsed()); recordButton.setEnabled(!Status.starting); recordButton.setAlpha(Status.starting ? .55f : 1f);
            boolean mic=checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;
            boolean notifications=RecorderService.notificationsReady(this);
            recordButton.setText(Status.recording ? "停止并保存" : Status.starting ? "正在启动…" : !mic ? "允许麦克风" : !notifications ? "开启录音通知" : "开始录音");
            String nextStyle=Status.recording ? "recording" : "idle";
            if(!nextStyle.equals(buttonStyle)) {ui.styleButton(recordButton,Status.recording ? ui.danger : ui.primary,Status.recording ? (ui.dark ? Ui.color("#3D171B") : Ui.color("#FFFFFF")) : ui.onPrimary);buttonStyle=nextStyle;}
            detail.setText(Status.recording ? "录音仅保存在本机，停止后可试听" : prefs().getString("last","首次使用，请录一小段并试听"));
            boolean enabled=prefs().getBoolean("hotkey",false);
            shortcut.setText(!enabled ? "快捷键未开启  ›" : !VolumeKeyService.connected ? "快捷键待授权  ›" : "快捷键已就绪  ·  黑屏录音");
            shortcutCard.setContentDescription(shortcut.getText()+"。锁屏双击音量下键开始或停止。点击进入设置");
        }
        if(connection!=null) connection.setText(VolumeKeyService.connected ? "系统按键服务已连接" : "系统按键服务未连接");
    }
    private void settings() {
        ui.header(root,"设置","按你的习惯记录","返回",() -> show("home"));
        LinearLayout keys=section("锁屏快捷键","双击音量下键，无需先打开应用");
        Switch enabled=new Switch(this); enabled.setText("启用双击快捷键"); styleSwitch(enabled); enabled.setChecked(prefs().getBoolean("hotkey",false)); ui.add(keys,enabled,12);
        final boolean[] syncing={false};
        enabled.setOnCheckedChangeListener((button,on) -> {
            if(syncing[0]) return;
            if(!on) { prefs().edit().putBoolean("hotkey",false).apply();return; }
            syncing[0]=true;enabled.setChecked(false);syncing[0]=false;
            disclosure(() -> { prefs().edit().putBoolean("hotkey",true).apply(); syncing[0]=true;enabled.setChecked(true);syncing[0]=false; });
        });
        ui.add(keys,ui.label("双击直接尝试黑屏录音，不主动点亮或解锁手机。系统可能限制后台麦克风，请先测试并试听。",12,ui.muted,false),6);
        ui.divider(keys,18);connection=ui.label("",13,ui.muted,false);ui.add(keys,connection,16);
        ui.add(keys,ui.button("系统无障碍设置  ›",false,() -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))),12);
        ui.add(keys,ui.button("用途与使用说明",false,() -> disclosure(null)),8);
        LinearLayout permissions=section("录音权限","录音时保留系统提示与持续通知");
        ui.add(permissions,ui.button("允许麦克风与通知",true,this::requestPermissions),16);
        ui.add(permissions,ui.button("系统通知设置  ›",false,() -> startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()))),8);
        LinearLayout privacy=section("只在本机","无联网权限 · 无云同步 · 无账号");
        ui.add(privacy,ui.label("录音保存在应用私有目录，只有你选择导出时才交给接收应用。卸载或清除数据会删除全部录音，请先导出重要内容。",13,ui.muted,false),14);
        ui.add(privacy,ui.button("诊断与帮助  ›",false,() -> show("diagnostics")),16);
        ui.add(root,ui.centered("快捷录音  0.3 · 测试版",12,ui.muted,false),26);
    }
    private void styleSwitch(Switch s) { s.setTextSize(15);s.setTextColor(ui.text);s.setMinHeight(ui.dp(52));s.setSwitchPadding(ui.dp(12));s.setPadding(0,ui.dp(8),0,ui.dp(8)); }
    private LinearLayout section(String title,String subtitle) {
        LinearLayout card=ui.card();card.addView(ui.label(title,18,ui.text,true));ui.add(card,ui.label(subtitle,12,ui.muted,false),7);ui.add(root,card,20);return card;
    }
    private void requestPermissions() {
        if(Build.VERSION.SDK_INT>=33) requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO,Manifest.permission.POST_NOTIFICATIONS},7);
        else requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},7);
    }
    private void disclosure(Runnable accepted) {
        AlertDialog.Builder b=new AlertDialog.Builder(this).setTitle("快捷键用途与说明")
            .setMessage("锁屏或熄屏时，两次短按音量下键，按下间隔不超过450毫秒。按键仍会调节音量，可能与系统快捷键冲突。\n\n无障碍服务仅申请按键过滤，不读取屏幕文字或窗口内容。可随时关闭应用开关和系统服务。\n\n通过正常接口尝试黑屏录音，不主动点亮或解锁手机。系统可能拒绝后台麦克风，或不转发熄屏按键。\n\n短震表示录音器实际启动或成功保存；三次震动表示失败。录音保持持续通知。声音是否有效，请保存后试听确认。\n\n只录制你有权录制的内容。");
        if(accepted!=null) b.setNegativeButton("取消",null).setPositiveButton("理解并开启",(d,w) -> accepted.run());
        else b.setPositiveButton("知道了",null);
        b.show();
    }
    private void diagnostics() {
        ui.header(root,"诊断与帮助","仅本机保存，不含音频","返回",() -> show("settings"));
        LinearLayout help=section("录音没有开始？","先录一小段，保存后试听");
        ui.add(help,ui.label("1  检查麦克风和通知权限。\n2  检查快捷键开关及无障碍服务。\n3  锁屏双击后无记录，可能是系统未转发按键。\n4  黑屏录音失败，请解锁后在首页手动开始。\n\n系统隐私开关、其他应用抢占麦克风、来电或低存储都可能影响录音。进程被杀或强行停止时，未完成录音可能丢失，请勿作为重要场合的唯一备份。",14,ui.muted,false),16);
        LinearLayout journal=section("最近诊断","最近约7000字符，可选择复制");
        log=ui.label(prefs().getString("log","暂无记录"),12,ui.text,false);log.setTextIsSelectable(true);ui.add(journal,log,16);
        ui.add(journal,ui.button("刷新诊断",false,() -> log.setText(prefs().getString("log","暂无记录"))),18);
        ui.add(journal,ui.button("清空诊断",false,() -> {prefs().edit().remove("log").apply();log.setText("暂无记录");}),8);
    }
    private void refreshFiles() {
        if(files==null) return;
        files.removeAllViews();File[] list=Status.dir(this).listFiles((d,n) -> n.endsWith(".m4a"));
        int size=list==null ? 0 : list.length;count.setText("本机录音"+(size>0 ? "  ·  "+size : ""));
        if(size==0) {
            LinearLayout empty=ui.card();ui.add(empty,ui.centered("还没有录音",17,ui.text,true),12);
            ui.add(empty,ui.centered("点击开始，记录第一段声音",13,ui.muted,false),10);ui.space(empty,12);ui.add(files,empty,0);return;
        }
        Arrays.sort(list,Comparator.comparing(File::getName).reversed());
        for(File f:list) {
            LinearLayout card=ui.card();LinearLayout top=ui.row();LinearLayout words=ui.column();
            words.addView(ui.label(displayName(f),16,ui.text,true));ui.add(words,ui.label(String.format(Locale.CHINA,"M4A  ·  %.1f MB",f.length()/1048576.0),12,ui.muted,false),7);
            top.addView(words,new LinearLayout.LayoutParams(0,-2,1));Button more=ui.button("更多",false,() -> {});more.setContentDescription("录音"+displayName(f)+"，更多操作");LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-2,-2);mp.leftMargin=ui.dp(12);top.addView(more,mp);
            more.setOnClickListener(v -> {PopupMenu menu=new PopupMenu(this,more);menu.getMenu().add("导出录音").setOnMenuItemClickListener(item -> {share(f);return true;});menu.getMenu().add("删除录音").setOnMenuItemClickListener(item -> {delete(f);return true;});menu.show();});
            card.addView(top);
            Button play=ui.button(f.getName().equals(playingName) ? "停止播放" : "播放录音",false,() -> play(f));ui.add(card,play,16);ui.add(files,card,10);
        }
    }
    private String displayName(File f) {
        String n=f.getName();if(n.matches("[0-9]{8}_[0-9]{6}_[0-9]{3}\\.m4a")) return n.substring(0,4)+"."+n.substring(4,6)+"."+n.substring(6,8)+"  "+n.substring(9,11)+":"+n.substring(11,13)+":"+n.substring(13,15);
        return n;
    }
    private void play(File f) {
        if(Status.recording || Status.starting) {Toast.makeText(this,"请先停止录音，避免录入播放声音",Toast.LENGTH_LONG).show();return;}
        if(f.getName().equals(playingName)) {releasePlayer();refreshFiles();return;}
        releasePlayer();
        try {player=new MediaPlayer();player.setDataSource(f.getAbsolutePath());player.prepare();player.setOnCompletionListener(p -> {releasePlayer();refreshFiles();});player.setOnErrorListener((p,w,e) -> {releasePlayer();Status.note(this,"播放失败："+w+"/"+e);refreshFiles();return true;});player.start();playingName=f.getName();refreshFiles();}
        catch(Exception e) {releasePlayer();Status.error(this,"播放失败",e);refreshFiles();}
    }
    private void share(File f) {
        Uri uri=Uri.parse("content://cn.local.quickrecorder.files/"+f.getName());
        Intent i=new Intent(Intent.ACTION_SEND).setType("audio/mp4").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);i.setClipData(ClipData.newRawUri("录音",uri));
        try {startActivity(Intent.createChooser(i,"导出录音"));} catch(Exception e) {Status.error(this,"无法导出",e);}
    }
    private void delete(File f) {
        new AlertDialog.Builder(this).setTitle("删除这段录音？").setMessage("删除后无法恢复。\n"+displayName(f)).setNegativeButton("取消",null)
            .setPositiveButton("删除",(d,w) -> {releasePlayer();if(!f.delete()) Toast.makeText(this,"删除失败",Toast.LENGTH_SHORT).show();refreshFiles();}).show();
    }
    private void releasePlayer() {if(player!=null) {player.release();player=null;}playingName="";}
    private void navigateBack() {if("diagnostics".equals(page))show("settings");else if("settings".equals(page))show("home");else finish();}
    @Override public void onBackPressed() {navigateBack();}
    @Override protected void onSaveInstanceState(Bundle out) {super.onSaveInstanceState(out);out.putString("page",page);}
    @Override protected void onResume() {super.onResume();updateStatus();refreshFiles();handler.post(tick);}
    @Override protected void onPause() {handler.removeCallbacks(tick);releasePlayer();super.onPause();}
}
