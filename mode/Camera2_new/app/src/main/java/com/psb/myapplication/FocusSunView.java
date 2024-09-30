package com.psb.myapplication;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class FocusSunView extends View {
    private int paintColor = Color.WHITE;
    private Paint sunPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint moonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float borderWidth = 3f;
    private float progress = 0.5f;
    private float realProcess = 0.5f;
    private float angle = 360f;
    private float circleY = -1f;
    private float lastCircleY = 0f;
    private float posY = 0f;
    private float curPosY = 0f;
    private PorterDuffXfermode porterDuffDstOut = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
    private float dp10 = 0f;
    private float dp8 = 0f;
    private float dp6 = 0f;
    private float dp5 = 0f;
    private float dp3 = 0f;
    private float dp2 = 0f;
    private float centerOfCircle = 0f;
    private float circleRadius = 0f;
    private RectF frameRectF = new RectF(0f, 0f, 0f, 0f);
    private float frameRadius = 0f;
    private float _14 = 0f;
    private CountDownTimer countdown = null;
    private boolean showLine = false;
    private float upperExposureLimit = 2f;
    private float lowerExposureLimit = -2f;
    private OnExposureChangeListener onExposureChangeListener = null;
    private float oldExposure = 0f;
    private ValueAnimator focusAnimator = null;

    public FocusSunView(Context context) {
        this(context, null);
    }

    public FocusSunView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FocusSunView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 初始化画笔
        sunPaint.setStrokeCap(Paint.Cap.ROUND);
        // 设置画笔颜色
        sunPaint.setStyle(Paint.Style.STROKE);
        // 设置画笔宽度
        moonPaint.setStyle(Paint.Style.FILL);
        // 设置画
        moonPaint.setStrokeCap(Paint.Cap.ROUND);
        // 设置画笔宽度
        framePaint.setStrokeCap(Paint.Cap.ROUND);
        // 设置画笔样式
        framePaint.setStyle(Paint.Style.STROKE);
        // 设置笔触和连接处样式
        framePaint.setStrokeJoin(Paint.Join.ROUND);
        // 设置画笔颜色
        dp10 = dp2px(getContext(), 10f);
        dp8 = dp2px(getContext(), 8f);
        dp6 = dp2px(getContext(), 6f);
        dp5 = dp2px(getContext(), 5f);
        dp3 = dp2px(getContext(), 3f);
        dp2 = dp2px(getContext(), 2f);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        centerOfCircle = (width / 10f) * 9f;
        circleRadius = width / 30f;
        frameRadius = width / 5f;
        frameRectF.set((width / 2f) - frameRadius, (height / 2f) - frameRadius, (width / 2f) + frameRadius, (height / 2f) + frameRadius);
        _14 = frameRectF.height() / 4f;
    }

    public void setExposureLimit(Float upperExposureLimit, Float lowerExposureLimit) {
        if (upperExposureLimit != null && lowerExposureLimit != null) {
            this.upperExposureLimit = upperExposureLimit;
            this.lowerExposureLimit = lowerExposureLimit;
        }
    }


    // 设置曝光值

    public void startCountdown(boolean reset) {
        paintColor = Color.WHITE;

        // 重置
        if (reset) {
            progress = 0.5f;
            realProcess = 0.5f;
            circleY = getHeight() * progress;
            lastCircleY = circleY;
        }

        postInvalidate();

        // 取消
        if (countdown != null) {
            countdown.cancel();
            countdown = null;
        }

        // 开启
        if (focusAnimator != null) {
            focusAnimator.cancel();
            focusAnimator = null;
        }

        // 开启倒计时
        if (countdown == null) {
            if (!reset) {
                countdown = new CountDownTimer(2000, 1000) { // 将倒计时总时长改为2000毫秒
                    @Override
                    public void onTick(long millisUntilFinished) {
                        if (millisUntilFinished >= 1000 && millisUntilFinished <= 1500) {
                            paintColor = Color.parseColor("#FFAAAAAA");
                            postInvalidate();
                        }
                    }

                    @Override
                    public void onFinish() {
                        countdown = null;
                        setVisibility(GONE);
                    }
                }.start();
            } else {
                focusAnimator = ValueAnimator.ofFloat(0f, 1.3f, 1f).setDuration(200); // 将动画时长改为200毫秒
                if (focusAnimator != null) {
                    focusAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            float value = (float) animation.getAnimatedValue();
                            float left = (getWidth() / 2f) - frameRadius;
                            float right = (getWidth() / 2f) + frameRadius;
                            float top = (getHeight() / 2f) - frameRadius;
                            float bottom = (getHeight() / 2f) + frameRadius;
                            frameRectF.left = left - ((right - left) / 5f - ((right - left) / 5f) * value);
                            frameRectF.right = right + ((right - left) / 5f - ((right - left) / 5f) * value);
                            frameRectF.top = top - ((bottom - top) / 5f - ((bottom - top) / 5f) * value);
                            frameRectF.bottom = bottom + ((bottom - top) / 5f - ((bottom - top) / 5f) * value);
                            postInvalidate();
                        }
                    });
                    focusAnimator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            focusAnimator = null;
                            countdown = new CountDownTimer(2000, 1000) { // 将倒计时总时长改为2000毫秒
                                @Override
                                public void onTick(long millisUntilFinished) {
                                    // 倒计时中
                                    if (millisUntilFinished >= 1000 && millisUntilFinished <= 1500) {
                                        paintColor = Color.parseColor("#FFAAAAAA");
                                        postInvalidate();
                                    }
                                }

                                @Override
                                public void onFinish() {
                                    countdown = null;
                                    setVisibility(GONE);
                                }
                            }.start();
                        }
                    });
                    // 开始动画
                    focusAnimator.start();
                }
            }
        }
    }


    // 销毁
    @Override
    protected void onDetachedFromWindow() {
        if (focusAnimator != null) {
            focusAnimator.cancel();
            focusAnimator = null;
        }
        if (countdown != null) {
            countdown.cancel();
            countdown = null;
        }
        super.onDetachedFromWindow();
    }

    // 绘制
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        framePaint.setColor(paintColor);
        borderWidth = 4f;
        framePaint.setStrokeWidth(borderWidth);
        float[] points = {
                frameRectF.left, frameRectF.top, frameRectF.left, frameRectF.top + _14,
                frameRectF.left, frameRectF.top, frameRectF.left + _14, frameRectF.top,
                frameRectF.left, frameRectF.bottom, frameRectF.left, frameRectF.bottom - _14,
                frameRectF.left, frameRectF.bottom, frameRectF.left + _14, frameRectF.bottom,
                frameRectF.right, frameRectF.top, frameRectF.right, frameRectF.top + _14,
                frameRectF.right, frameRectF.top, frameRectF.right - _14, frameRectF.top,
                frameRectF.right, frameRectF.bottom, frameRectF.right, frameRectF.bottom - _14,
                frameRectF.right, frameRectF.bottom, frameRectF.right - _14, frameRectF.bottom
        };
        canvas.drawLines(points, framePaint);
        borderWidth = 4f;
        sunPaint.setColor(paintColor);
        moonPaint.setColor(paintColor);
        sunPaint.setStrokeWidth(borderWidth);
        if (showLine) {
            if (circleY != circleRadius + dp8) {
                canvas.drawLine(centerOfCircle, 0f, centerOfCircle, (getHeight() * progress) - (circleRadius) - dp10, sunPaint);
            }
            if (circleY != getHeight() - (circleRadius) - dp8) {
                canvas.drawLine(centerOfCircle, (getHeight() * progress) + (circleRadius) + dp10, centerOfCircle, getHeight() * 1f, sunPaint);
            }
        }
        borderWidth = 3f;
        sunPaint.setStrokeWidth(borderWidth);
        canvas.drawCircle(centerOfCircle, getHeight() * progress, circleRadius, sunPaint);
        for (int i = 0; i < 8; i++) {
            PointF startPointF = calculationPoint(angle - (i * 45f), circleRadius + dp3);
            PointF endPointF = calculationPoint(angle - (i * 45f), circleRadius + dp5);
            borderWidth = 5f;
            sunPaint.setStrokeWidth(borderWidth);
            canvas.drawLine(startPointF.x, startPointF.y, endPointF.x, endPointF.y, sunPaint);
            borderWidth = 3f;
        }
        if (realProcess < .5f) {
            float left = centerOfCircle - (((circleRadius - dp2) * 2f) * Math.abs(realProcess - 0.5f));
            float top = (getHeight() * progress) - (circleRadius - dp2);
            float right = centerOfCircle + (((circleRadius - dp2) * 2f) * Math.abs(realProcess - 0.5f));
            float bottom = (getHeight() * progress) + (circleRadius - dp2);
            canvas.drawOval(left, top, right, bottom, moonPaint);
            canvas.drawArc(centerOfCircle - (circleRadius - dp2), (getHeight() * progress) - (circleRadius - dp2),
                    centerOfCircle + (circleRadius - dp2), (getHeight() * progress) + (circleRadius - dp2),
                    90f, 180f, false, moonPaint);
        } else if (realProcess == .5f) {
            canvas.drawArc(centerOfCircle - (circleRadius - dp2), (getHeight() * progress) - (circleRadius - dp2),
                    centerOfCircle + (circleRadius - dp2), (getHeight() * progress) + (circleRadius - dp2),
                    90f, 180f, false, moonPaint);
        } else {
            canvas.saveLayer(null, null);
            float left = centerOfCircle - (((circleRadius - dp2) * 2f) * Math.abs(realProcess - 0.5f));
            float top = (getHeight() * progress) - (circleRadius - dp2);
            float right = centerOfCircle + (((circleRadius - dp2) * 2f) * Math.abs(realProcess - 0.5f));
            float bottom = (getHeight() * progress) + (circleRadius - dp2);
            canvas.drawArc(centerOfCircle - (circleRadius - dp2 - 1), (getHeight() * progress) - (circleRadius - dp2 - 1),
                    centerOfCircle + (circleRadius - dp2 - 1), (getHeight() * progress) + (circleRadius - dp2 - 1),
                    90f, 180f, false, moonPaint);
            moonPaint.setXfermode(porterDuffDstOut);
            canvas.drawOval(left, top, right, bottom, moonPaint);
            moonPaint.setXfermode(null);
            canvas.restoreToCount(canvas.save());
        }
        canvas.restore();
    }
    // 测量
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event != null) {
            switch (event.getAction()) {
                // 按下
                case MotionEvent.ACTION_DOWN:
                    if (circleY < 0f) {
                        circleY = getHeight() * progress;
                        lastCircleY = circleY;
                    }
                    posY = event.getY();
                    paintColor = Color.WHITE;
                    break;
                    // 移动
                case MotionEvent.ACTION_MOVE:
                    curPosY = event.getY();
                    paintColor = Color.WHITE;
                    if ((curPosY - posY != 0)) {
                        showLine = true;
                        circleY = (curPosY - posY) + lastCircleY;
                        if (circleY >= getHeight() - circleRadius - dp8) {
                            circleY = getHeight() - circleRadius - dp8;
                        }
                        if (circleY < circleRadius + dp8) {
                            circleY = circleRadius + dp8;
                        }
                        realProcess = (((circleY - (circleRadius + dp8)) / ((getHeight() - circleRadius - dp8) - (circleRadius + dp8))) * 100f) < 0 ? 0 : (((circleY - (circleRadius + dp8)) / ((getHeight() - circleRadius - dp8) - (circleRadius + dp8))) * 100f);
                        progress = circleY / getHeight();
                        angle = 360f * realProcess;
                        // 定义中间变量简化表达式
                        float numerator = ((getHeight() - circleRadius - dp8) - (circleRadius + dp8)) - (circleY - (circleRadius + dp8));
                        float denominator = ((getHeight() - circleRadius - dp8) - (circleRadius + dp8));
                        float result = numerator / denominator * 100f;
                        float absolutelyProcess = result < 0 ? 0 : result;
                        float step = upperExposureLimit - lowerExposureLimit;
                        float exposure = (((step * absolutelyProcess) + lowerExposureLimit) * 100f) < 0 ? 0 : (((step * absolutelyProcess) + lowerExposureLimit) * 100f);
                        if (onExposureChangeListener != null && oldExposure != exposure) {
                            oldExposure = exposure;
                            onExposureChangeListener.onExposureChangeListener(exposure);
                        }
                        if (countdown != null) {
                            countdown.cancel();
                            countdown = null;
                        }
                        invalidate();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    lastCircleY = circleY;
                    showLine = false;
                    invalidate();
                    startCountdown(false);
                    break;
            }
        }
        return true;
    }

    // 计算坐标
    private PointF calculationPoint(float angle, float radius) {
        float x = (centerOfCircle) + (radius) * (float) Math.cos(angle * Math.PI / 180f);
        float y = (getHeight() * progress) + (radius) * (float) Math.sin(angle * Math.PI / 180f);
        return new PointF(x, y);
    }

    // 设置监听器
    public void setOnExposureChangeListener(OnExposureChangeListener onExposureChangeListener) {
        this.onExposureChangeListener = onExposureChangeListener;
    }

    // 监听器
    public interface OnExposureChangeListener {
        void onExposureChangeListener(float exposure);
    }

    // dp转px
    private float dp2px(Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density + .5f;
    }
}