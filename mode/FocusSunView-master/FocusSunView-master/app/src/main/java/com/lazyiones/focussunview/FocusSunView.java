//package com.lazyiones.focussunview;
//
//import android.animation.Animator;
//import android.animation.AnimatorListenerAdapter;
//import android.animation.ValueAnimator;
//import android.annotation.SuppressLint;
//import android.content.Context;
//import android.graphics.*;
//import android.os.CountDownTimer;
//import android.util.AttributeSet;
//import android.view.MotionEvent;
//import android.view.View;
//import androidx.annotation.Nullable;
//
//public class FocusSunView extends View {
//    private int paintColor = Color.WHITE;
//    private Paint sunPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//    private Paint moonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//    private Paint framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//    private float borderWidth = 3;
//    private float progress = 0.5f;
//    private float realProcess = 0.5f;
//    private float angle = 360;
//    private float circleY = -1;
//    private float lastCircleY = 0;
//    private float posY = 0;
//    private float curPosY = 0;
//    private PorterDuffXfermode porterDuffDstOut = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
//    private float dp10 = 0;
//    private float dp8 = 0;
//    private float dp6 = 0;
//    private float dp5 = 0;
//    private float dp3 = 0;
//    private float dp2 = 0;
//    private float centerOfCircle = 0;
//    private float circleRadius = 0;
//    private RectF frameRectF = new RectF(0, 0, 0, 0);
//    private float frameRadius = 0;
//    private float _14 = 0;
//    private CountDownTimer countdown = null;
//    private boolean showLine = false;
//    private float upperExposureLimit = 2;
//    private float lowerExposureLimit = -2;
//    private OnExposureChangeListener onExposureChangeListener = null;
//    private float oldExposure = 0;
//    private ValueAnimator focusAnimator = null;
//
//    public FocusSunView(Context context) {
//        this(context, null);
//    }
//
//    public FocusSunView(Context context, @Nullable AttributeSet attrs) {
//        this(context, attrs, 0);
//    }
//
//    public FocusSunView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
//        super(context, attrs, defStyleAttr);
//        init();
//    }
//
//    private void init() {
//        sunPaint.setStrokeCap(Paint.Cap.ROUND);
//        sunPaint.setStyle(Paint.Style.STROKE);
//        moonPaint.setStyle(Paint.Style.FILL);
//        moonPaint.setStrokeCap(Paint.Cap.ROUND);
//        framePaint.setStrokeCap(Paint.Cap.ROUND);
//        framePaint.setStyle(Paint.Style.STROKE);
//        framePaint.setStrokeJoin(Paint.Join.ROUND);
//        dp10 = dp2px(getContext(), 10);
//        dp8 = dp2px(getContext(), 8);
//        dp6 = dp2px(getContext(), 6);
//        dp5 = dp2px(getContext(), 5);
//        dp3 = dp2px(getContext(), 3);
//        dp2 = dp2px(getContext(), 2);
//    }
//
//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        int width = MeasureSpec.getSize(widthMeasureSpec);
//        int height = MeasureSpec.getSize(heightMeasureSpec);
//        centerOfCircle = width / 2f;
//        circleRadius = width / 10f;
//        frameRadius = width / 2f - borderWidth;
//        frameRectF.set(borderWidth, borderWidth, width - borderWidth, height - borderWidth);
//    }
//
//    @Override
//    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
//        // 绘制太阳、月亮和其他元素
//        drawSun(canvas);
//        drawMoon(canvas);
//        drawFrame(canvas);
//        drawLine(canvas);
//    }
//
//    private void drawSun(Canvas canvas) {
//        float sunY = getHeight() * progress;
//        sunPaint.setColor(paintColor);
//        canvas.drawCircle(centerOfCircle, sunY, circleRadius, sunPaint);
//    }
//
//    private void drawMoon(Canvas canvas) {
//        moonPaint.setColor(Color.BLACK);
//        canvas.drawCircle(centerOfCircle, getHeight() - circleRadius - dp8, circleRadius, moonPaint);
//    }
//
//    private void drawFrame(Canvas canvas) {
//        framePaint.setStrokeWidth(borderWidth);
//        framePaint.setColor(Color.GRAY);
//        canvas.drawRoundRect(frameRectF, frameRadius, frameRadius, framePaint);
//    }
//
//    private void drawLine(Canvas canvas) {
//        if (showLine) {
//            Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//            linePaint.setStrokeWidth(dp2);
//            linePaint.setColor(Color.RED);
//            canvas.drawLine(centerOfCircle, curPosY, centerOfCircle, circleY, linePaint);
//        }
//    }
//
//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        switch (event.getAction()) {
//            case MotionEvent.ACTION_DOWN:
//                if (circleY < 0) {
//                    circleY = getHeight() * progress;
//                    lastCircleY = circleY;
//                }
//                posY = event.getY();
//                sunPaint.setColor(Color.WHITE);
//                break;
//            case MotionEvent.ACTION_MOVE:
//                curPosY = event.getY();
//                sunPaint.setColor(Color.WHITE);
//                if ((curPosY - posY > 0) || (curPosY - posY < 0)) {
//                    showLine = true;
//                    circleY = (curPosY - posY) + lastCircleY;
//                    if (circleY >= getHeight() - circleRadius - dp8) {
//                        circleY = getHeight() - circleRadius - dp8;
//                    }
//                    if (circleY < circleRadius + dp8) {
//                        circleY = circleRadius + dp8;
//                    }
//                    realProcess = (((circleY - (circleRadius + dp8)) / ((getHeight() - circleRadius - dp8) - (circleRadius + dp8))) * 100f) / 100.0f;
//                    progress = circleY / getHeight();
//                    angle = 360f * realProcess;
//                    float absolutelyProcess = (((((getHeight() - circleRadius - dp8) - (circleRadius + dp8)) - (circleY - (circleRadius + dp8))) / ((getHeight() - circleRadius - dp8) - (circleRadius + dp8))) * 100f) / 100.0f;
//                    float step = upperExposureLimit - lowerExposureLimit;
//                    float exposure = (((step * absolutelyProcess) + lowerExposureLimit) * 100f) / 100.0f;
//                    if (onExposureChangeListener != null && oldExposure != exposure) {
//                        oldExposure = exposure;
//                        onExposureChangeListener.onExposureChangeListener(exposure);
//                    }
//                    if (countdown != null) {
//                        countdown.cancel();
//                        countdown = null;
//                    }
//                    invalidate();
//                }
//                break;
//            case MotionEvent.ACTION_UP:
//            case MotionEvent.ACTION_CANCEL:
//                lastCircleY = circleY;
//                showLine = false;
//                invalidate();
//                startCountdown(false);
//                break;
//            default:
//                break;
//        }
//        return true;
//    }
//
//    private PointF calculationPoint(float angle, float radius) {
//        double radian = Math.toRadians(angle);
//        float x = (float) (centerOfCircle + radius * Math.cos(radian));
//        float y = (float) (getHeight() * progress + radius * Math.sin(radian));
//        return new PointF(x, y);
//    }
//
//    public void setOnExposureChangeListener(OnExposureChangeListener listener) {
//        this.onExposureChangeListener = listener;
//    }
//
//    public interface OnExposureChangeListener {
//        void onExposureChangeListener(float exposure);
//    }
//
//    private float dp2px(Context context, float dp) {
//        return dp * context.getResources().getDisplayMetrics().density + 0.5f;
//    }
//
//    public void setExposureLimit(float upperExposureLimit, float lowerExposureLimit) {
//        this.upperExposureLimit = upperExposureLimit;
//        this.lowerExposureLimit = lowerExposureLimit;
//    }
//
//    public void startCountdown(boolean reset) {
//        sunPaint.setColor(Color.WHITE);
//        if (reset) {
//            progress = 0.5f;
//            realProcess = 0.5f;
//            circleY = getHeight() * progress;
//            lastCircleY = circleY;
//        }
//        invalidate();
//
//        if (countdown != null) {
//            countdown.cancel();
//            countdown = null;
//        }
//
//        if (countdown == null) {
//            if (!reset) {
//                countdown = new CountDownTimer(5000, 1000) {
//                    @Override
//                    public void onTick(long millisUntilFinished) {
//                        if (millisUntilFinished >= 1000) {
//                            float deltaProgress = (float) (millisUntilFinished / 5000.0f);
//                            progress -= deltaProgress;
//                            realProcess = progress;
//                            circleY = getHeight() * progress;
//                            lastCircleY = circleY;
//                            invalidate();
//                        }
//                    }
//
//                    @Override
//                    public void onFinish() {
//                        progress = 0;
//                        realProcess = 0;
//// 继续上一个代码片段
//                        circleY = getHeight() * progress;
//                        lastCircleY = circleY;
//                        invalidate();
//                    }
//                };
//            } else {
//                countdown = new CountDownTimer(5000, 1000) {
//                    @Override
//                    public void onTick(long millisUntilFinished) {
//                        if (millisUntilFinished >= 1000) {
//                            float deltaProgress = (float) (millisUntilFinished / 5000.0f);
//                            progress += deltaProgress;
//                            realProcess = progress;
//                            circleY = getHeight() * progress;
//                            lastCircleY = circleY;
//                            invalidate();
//                        }
//                    }
//
//                    @Override
//                    public void onFinish() {
//                        progress = 1;
//                        realProcess = 1;
//                        circleY = getHeight();
//                        lastCircleY = circleY;
//                        invalidate();
//                    }
//                };
//            }
//            countdown.start();
//        }
//    }
//
//    public void animateFocus() {
//        if (focusAnimator != null) {
//            focusAnimator.cancel();
//        }
//
//        focusAnimator = ValueAnimator.ofFloat(0.5f, 1f);
//        focusAnimator.setDuration(1000);
//        focusAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//            @Override
//            public void onAnimationUpdate(ValueAnimator animation) {
//                progress = (float) animation.getAnimatedValue();
//                circleY = getHeight() * progress;
//                lastCircleY = circleY;
//                invalidate();
//            }
//        });
//
//        focusAnimator.addListener(new AnimatorListenerAdapter() {
//            @Override
//            public void onAnimationEnd(Animator animation) {
//                super.onAnimationEnd(animation);
//                startCountdown(true);
//            }
//        });
//
//        focusAnimator.start();
//    }
//}
