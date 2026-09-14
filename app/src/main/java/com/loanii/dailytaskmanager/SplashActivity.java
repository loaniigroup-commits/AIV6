package com.loanii.dailytaskmanager;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

public class SplashActivity extends Activity {
    private final int NAVY = Color.rgb(3, 20, 40);
    private final int GOLD = Color.rgb(224, 184, 78);

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(1, 13, 28));
        getWindow().setNavigationBarColor(Color.rgb(1, 13, 28));
        setContentView(build());
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, MainActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 1550);
    }

    private View build() {
        FrameLayout frame = new FrameLayout(this);
        frame.addView(new LuxuryBackgroundView(this), new FrameLayout.LayoutParams(-1,-1));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(28), dp(54), dp(28), dp(38));
        frame.addView(root, new FrameLayout.LayoutParams(-1,-1));

        Space top = new Space(this);
        root.addView(top, new LinearLayout.LayoutParams(1,0,1f));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ec_logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        root.addView(logo, new LinearLayout.LayoutParams(dp(240), dp(240)));

        TextView title = text("Daily Task Manager", 28, GOLD, true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1,-2);
        tp.setMargins(0,dp(12),0,dp(7));
        root.addView(title,tp);

        TextView tag = text("ORGANIZE TODAY", 11, Color.WHITE, true);
        tag.setLetterSpacing(.24f); tag.setGravity(Gravity.CENTER); root.addView(tag);
        TextView tag2 = text("BUILD A BETTER TOMORROW", 11, GOLD, true);
        tag2.setLetterSpacing(.12f); tag2.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tagp=new LinearLayout.LayoutParams(-1,-2);tagp.setMargins(0,dp(5),0,0);root.addView(tag2,tagp);

        Space mid = new Space(this);
        root.addView(mid, new LinearLayout.LayoutParams(1,0,.70f));

        View separator=new View(this);
        GradientDrawable sg=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{Color.TRANSPARENT,GOLD,Color.TRANSPARENT});
        separator.setBackground(sg); root.addView(separator,new LinearLayout.LayoutParams(dp(220),dp(1)));

        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setIndeterminate(true); bar.getIndeterminateDrawable().setTint(GOLD);
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(dp(190),dp(4));bp.setMargins(0,dp(18),0,0);root.addView(bar,bp);

        TextView load = text("Loading your tasks...", 11, Color.WHITE, false);
        load.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,dp(12),0,0); root.addView(load,lp);

        TextView foot = text("PLAN  •  FOCUS  •  ACHIEVE", 9, GOLD, true);
        foot.setLetterSpacing(.18f); foot.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,-2);fp.setMargins(0,dp(24),0,0);root.addView(foot,fp);
        return frame;
    }

    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD); return t;
    }
    private int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}
}
