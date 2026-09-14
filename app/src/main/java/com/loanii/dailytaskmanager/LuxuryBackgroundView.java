package com.loanii.dailytaskmanager;

import android.content.Context;
import android.graphics.*;
import android.view.View;

public class LuxuryBackgroundView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final int gold = Color.rgb(224, 184, 78);

    public LuxuryBackgroundView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setShader(new LinearGradient(0, 0, w, h,
                new int[]{Color.rgb(1, 12, 28), Color.rgb(4, 29, 56), Color.rgb(1, 17, 35)},
                new float[]{0f, .52f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, bg);

        // Soft navy light to give the background depth.
        Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
        halo.setShader(new RadialGradient(w * .82f, h * .18f, w * .72f,
                new int[]{Color.argb(80, 12, 66, 112), Color.TRANSPARENT},
                null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, halo);

        // Upper-right luxury gold sweep.
        drawCurve(canvas,
                w * .36f, -h * .05f,
                w * .98f, h * .08f,
                w * .68f, h * .26f,
                w * 1.08f, h * .34f,
                2.2f, 205);
        drawCurve(canvas,
                w * .48f, -h * .02f,
                w * 1.02f, h * .12f,
                w * .78f, h * .28f,
                w * 1.10f, h * .38f,
                1.1f, 110);
        drawCurve(canvas,
                w * .56f, -h * .01f,
                w * 1.06f, h * .16f,
                w * .85f, h * .31f,
                w * 1.12f, h * .42f,
                .7f, 70);

        // Lower-left balancing sweep.
        drawCurve(canvas,
                -w * .18f, h * .72f,
                w * .18f, h * .58f,
                w * .38f, h * .96f,
                w * .72f, h * 1.04f,
                1.7f, 155);
        drawCurve(canvas,
                -w * .24f, h * .78f,
                w * .14f, h * .64f,
                w * .46f, h * .98f,
                w * .82f, h * 1.06f,
                .8f, 70);

        // Tiny soft gold glints along the top sweep.
        glowPaint.setStyle(Paint.Style.FILL);
        glowPaint.setShader(new RadialGradient(w * .90f, h * .10f, Math.max(22, w * .08f),
                new int[]{Color.argb(95, 255, 219, 128), Color.TRANSPARENT}, null, Shader.TileMode.CLAMP));
        canvas.drawCircle(w * .90f, h * .10f, Math.max(22, w * .08f), glowPaint);
    }

    private void drawCurve(Canvas canvas,
                           float sx, float sy, float c1x, float c1y,
                           float c2x, float c2y, float ex, float ey,
                           float strokeDp, int alpha) {
        path.reset();
        path.moveTo(sx, sy);
        path.cubicTo(c1x, c1y, c2x, c2y, ex, ey);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(dp(strokeDp));
        paint.setColor(Color.argb(alpha, Color.red(gold), Color.green(gold), Color.blue(gold)));
        paint.setShadowLayer(dp(strokeDp * 2.1f), 0, 0, Color.argb(alpha / 3, 255, 210, 93));
        canvas.drawPath(path, paint);
        paint.clearShadowLayer();
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
