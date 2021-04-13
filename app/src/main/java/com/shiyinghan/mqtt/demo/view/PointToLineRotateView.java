package com.shiyinghan.mqtt.demo.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

import com.shiyinghan.mqtt.demo.R;

public class PointToLineRotateView extends View {
    private Paint strokePaint;
    private float pi = (float) Math.PI;
    private float startAngle = 0;
    private float sweepAngle = 0;
    private ValueAnimator endValue;
    private ValueAnimator startValue;
    private int strokeWidth = 10;

    public PointToLineRotateView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        //stroke paint
        strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(strokeWidth);
        strokePaint.setColor(getResources().getColor(R.color.colorPrimary));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        RectF artRectF = new RectF();
        artRectF.set(strokeWidth / 2, strokeWidth / 2, getWidth() - strokeWidth / 2, getHeight() - strokeWidth / 2);
        if (null == startValue) {
            loading();
        }
        sweepAngle = -(float) endValue.getAnimatedValue();
        startAngle = (float) startValue.getAnimatedValue();
        canvas.drawArc(artRectF, startAngle, sweepAngle, false, strokePaint);

        if (endValue.isRunning() || startValue.isRunning()) {
            invalidate();
        }
    }

    private void loading() {
        if (startValue == null) {
            startValue = makeStartValueAnimator(0, 360);
        } else {
            startValue.start();
        }
        if (endValue == null) {
            endValue = makeEndValueAnimator(30, 60, 120, 90, 60, 30);
        } else {
            endValue.start();
        }
        postDelayed(new Runnable() {
            @Override
            public void run() {
                loading();
                invalidate();
            }
        }, startValue.getDuration());
    }

    private ValueAnimator makeStartValueAnimator(float... value) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(value);
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.setDuration(1000);
        valueAnimator.start();
        return valueAnimator;
    }

    private ValueAnimator makeEndValueAnimator(float... value) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(value);
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.setDuration(1000);
        valueAnimator.start();
        return valueAnimator;
    }
}
