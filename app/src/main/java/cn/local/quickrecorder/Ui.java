package cn.local.quickrecorder;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

/** Small native design system. No external dependencies or behavioral permissions. */
final class Ui {
    final Activity activity;
    final boolean dark;
    final int background, surface, soft, text, muted, accent, primary, onPrimary, line, danger;
    Ui(Activity a) {
        activity = a;
        dark = (a.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        background = color(dark ? "#111722" : "#F3F5F9");
        surface = color(dark ? "#1B2432" : "#FFFFFF");
        soft = color(dark ? "#26364D" : "#EAF0FA");
        text = color(dark ? "#EFF3FA" : "#202C3F");
        muted = color(dark ? "#B1BED0" : "#617087");
        accent = color(dark ? "#AFC8FF" : "#4564A5");
        primary = color(dark ? "#AFC8FF" : "#4564A5");
        onPrimary = color(dark ? "#15253D" : "#FFFFFF");
        line = color(dark ? "#344255" : "#E2E7EF");
        danger = color(dark ? "#FFB7B4" : "#AE393F");
        a.getWindow().setStatusBarColor(Color.TRANSPARENT);
        a.getWindow().setNavigationBarColor(Color.TRANSPARENT);
        a.getWindow().getDecorView().setSystemUiVisibility(dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }
    static int color(String s) { return Color.parseColor(s); }
    int dp(float value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }
    LinearLayout column() { LinearLayout l = new LinearLayout(activity); l.setOrientation(LinearLayout.VERTICAL); return l; }
    LinearLayout row() { LinearLayout l = new LinearLayout(activity); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    GradientDrawable shape(int fill, int radius) { GradientDrawable g = new GradientDrawable(); g.setColor(fill); g.setCornerRadius(dp(radius)); return g; }
    LinearLayout card() { LinearLayout l = column(); l.setPadding(dp(20),dp(20),dp(20),dp(20)); l.setBackground(shape(surface,24)); return l; }
    void add(LinearLayout parent, View child, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2); p.topMargin=dp(top); parent.addView(child,p);
    }
    void space(LinearLayout parent,int size) { View v = new View(activity); parent.addView(v,new LinearLayout.LayoutParams(1,dp(size))); }
    TextView label(String value,int sp,int color,boolean bold) {
        TextView t = new TextView(activity); t.setText(value); t.setTextSize(sp); t.setTextColor(color);
        t.setFontFeatureSettings("tnum"); t.setIncludeFontPadding(false); t.setLineSpacing(dp(3),1f);
        if(bold) t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        return t;
    }
    TextView centered(String value,int sp,int color,boolean bold) { TextView t=label(value,sp,color,bold); t.setGravity(Gravity.CENTER); return t; }
    Button button(String value, boolean primaryStyle, Runnable action) {
        Button b = new Button(activity); b.setText(value); b.setTextSize(15); b.setAllCaps(false); b.setMinHeight(dp(52)); b.setMinimumHeight(dp(52));
        b.setPadding(dp(16),dp(12),dp(16),dp(12)); b.setStateListAnimator(null);
        b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        styleButton(b,primaryStyle ? primary : soft,primaryStyle ? onPrimary : accent);
        b.setOnClickListener(v -> action.run()); return b;
    }
    void styleButton(Button b,int fill,int foreground) {
        b.setTextColor(foreground);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(dark ? 0x28FFFFFF : 0x18000000),shape(fill,16),null));
    }
    void mount(LinearLayout root) {
        ScrollView scroll = new ScrollView(activity); scroll.setBackgroundColor(background); scroll.setFillViewport(true); scroll.setClipToPadding(false);
        scroll.addView(root,new ScrollView.LayoutParams(-1,-2)); activity.setContentView(scroll);
        root.setPadding(dp(24),dp(20),dp(24),dp(28));
        scroll.setOnApplyWindowInsetsListener((v,insets) -> {
            int left=insets.getSystemWindowInsetLeft(),top=insets.getSystemWindowInsetTop(),right=insets.getSystemWindowInsetRight(),bottom=insets.getSystemWindowInsetBottom();
            DisplayCutout cutout=insets.getDisplayCutout();
            if(cutout!=null) {left=Math.max(left,cutout.getSafeInsetLeft());top=Math.max(top,cutout.getSafeInsetTop());right=Math.max(right,cutout.getSafeInsetRight());bottom=Math.max(bottom,cutout.getSafeInsetBottom());}
            root.setPadding(dp(24)+left,dp(20)+top,dp(24)+right,dp(28)+bottom);
            return insets;
        });
        scroll.requestApplyInsets();
    }
    void header(LinearLayout root,String title,String subtitle,String action,Runnable click) {
        LinearLayout row=row(); LinearLayout words=column(); words.addView(label(title,28,text,true));
        if(!subtitle.isEmpty()) add(words,label(subtitle,13,muted,false),7);
        row.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        Button b=button(action,false,click); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2); p.leftMargin=dp(12); row.addView(b,p); add(root,row,0);
    }
    void divider(LinearLayout parent,int top) { View v=new View(activity); v.setBackgroundColor(line); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(1)); p.topMargin=dp(top); parent.addView(v,p); }
    View mic() { return new Mic(activity,accent,soft); }
    final class Mic extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int ink,fill;
        Mic(Context c,int ink,int fill) { super(c); this.ink=ink;this.fill=fill;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c); float s=Math.min(getWidth(),getHeight())/64f; c.save();c.translate(getWidth()/2f-32*s,getHeight()/2f-32*s);c.scale(s,s);
            p.setStyle(Paint.Style.FILL);p.setColor(fill);c.drawCircle(32,32,31,p);
            p.setColor(ink);c.drawRoundRect(26,15,38,37,6,6,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.5f);p.setStrokeCap(Paint.Cap.ROUND);
            c.drawArc(21,25,43,45,0,180,false,p);c.drawLine(21,31,21,34,p);c.drawLine(43,31,43,34,p);c.drawLine(32,45,32,51,p);c.drawLine(26,51,38,51,p);c.restore();
        }
    }
}
