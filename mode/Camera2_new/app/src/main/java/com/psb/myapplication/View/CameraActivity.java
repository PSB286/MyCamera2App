package com.psb.myapplication.View;

import static com.psb.myapplication.Utils.ImageUtils.getLatestThumbBitmap;
import static com.psb.myapplication.Utils.ImageUtils.rotateBitmap;
import static java.lang.Thread.sleep;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AutomaticZenRule;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.psb.myapplication.R;
import com.psb.myapplication.Utils.Camera2Proxy;
import com.psb.myapplication.Utils.ImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

// 相机界面
public class CameraActivity extends AppCompatActivity implements View.OnTouchListener {



    private static CameraActivity myapp = null;                                     // 单例
    private static final int DISTANCE_LIMIT = 50;                                   // 距离阈值
    private static final int VELOCITY_THRESHOLD = 500;                              // 速度阈值;
    private AutoFitTextureView mTextureView = null;                                 // 用于显示相机预览
    private FrameLayout mRootLayout = null;                                         // 根布局
    public static CustomViewL mCustomViewL = null;                                 // 用与模式切换
    private GestureDetector mGestureDetector = null;                                // 手势监听
    private DisplayMetrics mDisplayMetrics = null;                                  // 屏幕宽高
    //按键定义
    public static ImageView record, switch_camera, capture, mPictureIv, falsh_switch, Load;
    // 图片按键
    public static TextView switch_frame, void_quality, timerText, option1, option2, option3, option4, option5;
    // 文字按钮
    private FocusSunView focusSunView;                                              // 焦点光圈
    private LinearLayout Choose, Choose2;                                            // 画幅选择
    public static RelativeLayout Title;                                                   // 标题
    private int screenWidth = 0;                                                    // 屏幕宽
    private int screenHeight = 0;                                                   // 屏幕高
    private String cameraId = "0";                                                  // 摄像头ID
    //相机操作变量
    List<Surface> surfaces;                                                         // 用于创建预览和拍照的surface
    private Size previewSize = null;                                                // 预览尺寸
    private CameraDevice mCameraDevice = null;                                      // 摄像头设备
    SurfaceTexture texture;                                                         // 用于显示预览的surfaceTexture
    private Handler handler;                                                        // 用于控制线程
    private Handler handler2;                                                       // 用于控制线程
    private boolean Handleding = true;                                                // 是否处理中
    private ImageReader mImageReader;                                               // 用于拍照
    private MediaRecorder mMediaRecorder;                                           // 用于录制视频
    private CameraCaptureSession captureSession;                                    // 用于控制预览和拍照
    private Camera2Proxy mCameraProxy;                                              // 相机代理
    private float mOldDistance;                                                     // 用于计算双指距离
    private CaptureRequest mPreviewRequest;                                         // 用于控制预览
    CaptureRequest.Builder previewRequestBuilder;                                   // 用于控制预览
    CaptureRequest.Builder recordvideoRequestBuilder;                               // 用于控制录制视频
    CameraCharacteristics characteristics;                                          // 摄像头参数
    private boolean isRecording = false;                                            // 是否录制中
    String Previous_recorderPath, newroidPath;                                      // 录制视频路径
    boolean isRunning = false;                                                      // 是否运行
    File previousFile = null;                                                       // 录制视频文件
    private int seconds = 0;                                                        // 录制时间
    int colorState = 0;                                                               // 颜色状态
    Surface previewSurface=null;
    Runnable runnable;                                                              // 录制视频
    private short mFlashMode = 3;                                                   // 闪光灯状态
    private short lastFlashMode = 2;
    private CameraCaptureSession.CaptureCallback mPreCaptureCallback;               // 拍照回调
    public MediaActionSound mMediaActionSound;                                      // 拍照声音
    public Animation captureAnimation,btnAnimation, LoadAnimation, bitmapAnimation, mTextureViewAnimation;                     // 动画
    static Size largest = new Size(4, 3);                                 // 默认画幅
    boolean isCapture = false;                                                     // 是否拍照中
    boolean isCaptureing = true;                                                      // 是否拍照中
    static boolean isRecord4 = false;                                               // 是否录制中
    static boolean isRecord5 = false;                                                      // 是否录制中
    boolean isRecordflag = false;                                                     // 是否录制中
    boolean isLayout = false;                                                       // 是否布局中
    boolean isClickFocus = false;                                                   // 是否点击聚焦
    boolean isSwichCamera = false;                                                    // 是否切换摄像头
    boolean isAnimator = true;                                                        // 是否动画中
    boolean isOption = false;                                                         // 是否选项中
    boolean isLayoutSwich = false;                                                    // 是否切换摄像头中
    boolean isRecordingflg = false;                                                   // 是否录制中
    boolean isStopRecord = false;                                                     // 是否停止录制
    boolean isCaptureingflg = false;                                                 // 是否拍照中
    boolean isClickBitmap = false;                                                  // 是否点击图片
    boolean isRightTransverse = false;                                                // 是否右转
    boolean isLeftTransverse = false;                                                 // 是否左转
    boolean isinversion = false;                                                      // 是否翻转
    boolean Strongflash = false;// 强光
    boolean Autoflash = false;
    boolean firstView = true;
    boolean isFlat = false;
    boolean isPanning=false;
    boolean isPanningflg = true;
    float deltaX=0;
    float startX=0;
    float SWIPE_THRESHOLD=300;
    View maskView;                                                                  // 遮罩
    View maskViewbuf;                                                               // 遮罩
    ViewGroup parentbuf;                                                            // 父容器
    CameraCaptureSession.StateCallback PreCaptureCallback;                          // 拍照回调
    private ValueAnimator currentAnimator;                                          // 动画
    // 初始化 SensorManager 和传感器监听器
    private SensorManager sensorManager;                                            // 传感器管理器
    CameraState cameraState;
    Bitmap bitmap;
    CaptureRequest.Builder focusRequestBuilder = null;


    private SensorEventListener sensorEventListener = new SensorEventListener() {
        /**
         * 当传感器数据发生变化时调用此方法
         * @param event 传感器事件，包含传感器类型和传感器值等信息
         */
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
                float azimuth = event.values[0];  // 方位角
                float pitch = event.values[1];    // 倾斜角
                float roll = event.values[2];     // 滚动角

                // 处理方向变化的逻辑
                handleOrientationChange(azimuth, pitch, roll);
            }
        }

        /**
         * 当传感器的准确性发生变化时调用此方法
         * @param sensor 发生变化的传感器
         * @param accuracy 传感器的准确度
         */
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // 准确度变化时的处理
        }
    };

    /**
     * 处理设备方向变化
     *
     * @param azimuth 设备的水平方向度数
     * @param pitch   设备的俯仰角度
     * @param roll    设备的滚动角度
     */
    private void handleOrientationChange(float azimuth, float pitch, float roll) {
        // 根据方向变化执行其他逻辑
        Log.d("--Orientation--", "Azimuth: " + azimuth + ", Pitch: " + pitch + ", Roll: " + roll);
        // 判断是否需要旋转图片
        //if (Math.abs(pitch) >= 25 || Math.abs(roll) >= 25) {
        if (roll <= 20 && pitch <= 0) {
            mPictureIv.setRotation(0);
            switch_camera.setRotation(0);
            switch_frame.setRotation(0);
            void_quality.setRotation(0);
            option1.setRotation(0);
            option2.setRotation(0);
            option3.setRotation(0);
            option4.setRotation(0);
            option5.setRotation(0);
            falsh_switch.setRotation(0);
            mCustomViewL.textViews.get(0).setRotation(0);
            mCustomViewL.textViews.get(1).setRotation(0);
            isinversion = false;
            isRightTransverse = false;
            isLeftTransverse = false;
        }
        if (pitch <= 5 && roll >= 40) {
            mPictureIv.setRotation(90);
            switch_camera.setRotation(90);
            switch_frame.setRotation(90);
            void_quality.setRotation(90);
            option1.setRotation(90);
            option2.setRotation(90);
            option3.setRotation(90);
            option4.setRotation(90);
            option5.setRotation(90);
            falsh_switch.setRotation(90);
            mCustomViewL.textViews.get(0).setRotation(90);
            mCustomViewL.textViews.get(1).setRotation(90);
            isLeftTransverse = true;
            isinversion = false;
            isRightTransverse = false;
        }
        if (pitch <= 50 && roll <= -50) {
            mPictureIv.setRotation(-90);
            switch_camera.setRotation(-90);
            switch_frame.setRotation(-90);
            void_quality.setRotation(-90);
            option1.setRotation(-90);
            option2.setRotation(-90);
            option3.setRotation(-90);
            option4.setRotation(-90);
            option5.setRotation(-90);
            falsh_switch.setRotation(-90);
            mCustomViewL.textViews.get(0).setRotation(-90);
            mCustomViewL.textViews.get(1).setRotation(-90);
            isRightTransverse = true;
            isinversion = false;
            isLeftTransverse = false;
        }
        if (roll >= -76 && roll <= 76 && pitch > 30) {
            mPictureIv.setRotation(180);
            switch_camera.setRotation(180);
            switch_frame.setRotation(180);
            void_quality.setRotation(180);
            option1.setRotation(180);
            option2.setRotation(180);
            option3.setRotation(180);
            option4.setRotation(180);
            option5.setRotation(180);
            falsh_switch.setRotation(180);
            mCustomViewL.textViews.get(0).setRotation(180);
            mCustomViewL.textViews.get(1).setRotation(180);
            isinversion = true;
            isRightTransverse = false;
            isLeftTransverse = false;
        }
        // 创建一个旋转动画
        ObjectAnimator rotationAnimator = ObjectAnimator.ofFloat(mPictureIv, "rotation", 90);
        rotationAnimator.setDuration(10); // 设置动画持续时间，单位为毫秒
        rotationAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        // 启动动画
        //          rotationAnimator.start();

    }

    /**
     * 初始化活动 (Activity)
     * 此方法在活动首次创建时调用，负责设置活动的布局、申请权限、初始化变量和视图，以及设置监听器
     *
     * @param savedInstanceState 如果活动之前被销毁过且之后又重新创建，则此参数为上次销毁前的保存状态，否则为null
     */
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initRequestPermissions();                                           // 申请摄像头权限
        initScreen();                                                       //初始化布局
        initVariable();                                                     //初始化变量
        initCustomViewL();                                                  // 初始化自定义View
        initClickListener();                                                // 初始化点击监听器
        initTouchListener();                                                //初始化触摸监听器
    }

    /**
     * 初始化触摸监听器。
     * 此方法设置了一个触摸监听器，用于处理触摸事件，包括双指缩放和平移操作以及单指点击对焦功能。
     */
    private void initTouchListener() {
        mTextureView.setOnTouchListener(new View.OnTouchListener() {
            private boolean isZooming = false; // 标记是否正在缩放
            private float startX;
            private float mOldDistance;
            private boolean isOutOfBounds = false; // 标记手指是否离开过有效范围


            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // 获取手指的数量
                int pointerCount = event.getPointerCount();

                // 检查手指是否在 mTextureView 的有效范围内
                boolean isInBounds = event.getX() >= 0 && event.getX() <= mTextureView.getWidth() &&
                        event.getY() >= 0 && event.getY() <= mTextureView.getHeight();

                if (!isInBounds) {
                    // 手指离开了 mTextureView 的有效范围，取消操作
                    isZooming = false;
                    isPanning = false;
                    isPanningflg = false;
                    isOutOfBounds = true;
                    focusSunView.setVisibility(View.INVISIBLE);
                    return true;
                }
                else if (isOutOfBounds) {
                    // 手指重新进入有效范围，恢复操作
                    isOutOfBounds = false;
                    isPanningflg = true;
                }

                if (pointerCount == 2) {
                    // 双指缩放相关逻辑
                    switch (event.getAction() & MotionEvent.ACTION_MASK) {
                        case MotionEvent.ACTION_POINTER_DOWN:
                            // 双指按下
                            isZooming = true;
                            mOldDistance = getFingerSpacing(event);
                            // 隐藏聚焦图标
                            focusSunView.setVisibility(View.INVISIBLE);
                            break;
                        case MotionEvent.ACTION_MOVE:
                            // 双指移动
                            if (isZooming) {
                                float newDistance = getFingerSpacing(event);
                                if (newDistance > mOldDistance) {
                                    if (previewRequestBuilder != null) {
                                        if (isRecordingflg) {
                                            mCameraProxy.handleZoom(true, false, mCameraDevice, characteristics, recordvideoRequestBuilder, mPreviewRequest, captureSession);
                                        } else {
                                            mCameraProxy.handleZoom(true, false, mCameraDevice, characteristics, previewRequestBuilder, mPreviewRequest, captureSession);
                                        }
                                    }
                                } else if (newDistance < mOldDistance) {
                                    if (previewRequestBuilder != null) {
                                        if (isRecordingflg) {
                                            mCameraProxy.handleZoom(false, false, mCameraDevice, characteristics, recordvideoRequestBuilder, mPreviewRequest, captureSession);
                                        } else {
                                            mCameraProxy.handleZoom(false, false, mCameraDevice, characteristics, previewRequestBuilder, mPreviewRequest, captureSession);
                                        }
                                    }
                                }
                                mOldDistance = newDistance;
                            }
                            break;
                        case MotionEvent.ACTION_POINTER_UP:
                            // 双指抬起
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    isZooming = false;
                                    isPanningflg = true;
                                }
                            }, 200); // 延迟 200 毫秒
                            // 显示聚焦图标
                         //   focusSunView.setVisibility(View.VISIBLE);
                            break;
                        default:
                            break;
                    }
                } else {
                    // 单指点击对焦
                    switch (event.getAction() & MotionEvent.ACTION_MASK) {
                        case MotionEvent.ACTION_DOWN:
                            startX = event.getX();
                            float startY = event.getY();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    isPanning = true; // 结束滑动
                                }
                            }, 200); // 延迟 200 毫秒
                            Log.d("--CameraActivity2--", "单指按下");
                            break;
                        case MotionEvent.ACTION_MOVE:
                            if (isPanning&&!isRecording) {
                                float deltaX = event.getX() - startX;
                                if (Math.abs(deltaX) > SWIPE_THRESHOLD) { // SWIPE_THRESHOLD为设定的滑动阈值
                                    isPanningflg = false;
                                    if (deltaX > 0) {
                                        // 隐藏聚焦图标
                                        focusSunView.setVisibility(View.GONE);
                                        Log.d("--CameraActivity2--", "右滑");
                                        mCustomViewL.scrollRight();
                                    } else {
                                        focusSunView.setVisibility(View.GONE);
                                        Log.d("--CameraActivity2--", "左滑");
                                        mCustomViewL.scrollLeft();
                                    }
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            isPanning = false; // 结束滑动
                                        }
                                    }, 200); // 延迟 200 毫秒
                                }
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                            // 单指抬起
                            Log.d("--CameraActivity2--", "单指抬起" + isPanning+"Zoom:"+isZooming);
                            if (!isZooming && pointerCount == 1 && Objects.equals(cameraId, "0") && isPanningflg) {
                                // 点击对焦
                                if (previewRequestBuilder != null) {
                                    if (colorState == 1 && isRecordingflg) {
                                        focusOnPoint((int) event.getX(), (int) event.getY(), mTextureView.getWidth(), mTextureView.getHeight());
                                    } else {
                                        focusOnPoint((int) event.getX(), (int) event.getY(), mTextureView.getWidth(), mTextureView.getHeight());
                                    }
                                }
                                focusSunView.setVisibility(View.VISIBLE);
                                // 设置焦点位置
                                float halfWidth = focusSunView.getWidth() / 2f;
                                float halfHeight = focusSunView.getHeight() / 4f;
                                if (switch_frame.getText().toString().equals("4:3") && isCaptureing) {
                                    focusSunView.setTranslationX(event.getX() - halfWidth);
                                    focusSunView.setTranslationY(event.getY() + halfHeight);
                                }
                                if (switch_frame.getText().toString().equals("1:1") && isCaptureing) {
                                    focusSunView.setTranslationX(event.getX() - halfWidth);
                                    focusSunView.setTranslationY(event.getY() + 3 * halfHeight);
                                }
                                if (switch_frame.getText().toString().equals("FULL") && isCaptureing) {
                                    focusSunView.setTranslationX(event.getX() - halfWidth);
                                    focusSunView.setTranslationY(event.getY() - 2 * halfHeight);
                                }
                                if (!isCaptureing) {
                                    focusSunView.setTranslationX(event.getX() - halfWidth);
                                    focusSunView.setTranslationY(event.getY() + halfHeight);
                                }
                                // 开始倒计时
                                focusSunView.startCountdown(true);
                            }
                            // 不执行任何操作
                            break;
                        default:
                            break;
                    }
                }

                // 返回true，表示自己处理触摸事件
                return true;
            }
        });
    }


    /**
     * 开始相机预览
     * <p>
     * 本方法用于启动相机的预览功能，它通过设置重复请求的方式来持续进行预览
     * 如果captureSession或previewRequestBuilder任一为null，则不进行任何操作
     * 在正常情况下，将重复发送预览请求以实现持续预览的效果
     * 若遇到CameraAccessException异常，则输出异常信息
     */
    public void startPreview() {

        if (captureSession == null || previewRequestBuilder == null) {
            return;
        }
        try {
            // 开始预览，即一直发送预览的请求
            captureSession.setRepeatingRequest(mPreviewRequest, null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止相机预览
     * <p>
     * 本方法旨在停止相机的预览功能它通过停止重复请求来实现预览的停止
     * 如果会话或预览请求构建器为空，则不执行任何操作这确保了在必要组件不可用时，
     * 不会尝试停止预览此外，本方法处理可能的相机访问异常，确保异常情况下不会影响应用的稳定性
     */
    public void stopPreview() {
        // Log.v(TAG, "stopPreview");
        if (captureSession == null || previewRequestBuilder == null) {
            // Log.w(TAG, "stopPreview: mCaptureSession or mPreviewRequestBuilder is null");
            return;
        }
        try {
            captureSession.stopRepeating();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // 重写onResume方法
    @Override
    public void onResume() {
        super.onResume();
        Log.d("--onResume--", "onResume");
        if (isClickBitmap) {
            Bitmap bitmap = getLatestThumbBitmap(this);
            myapp = this;
            if (bitmap == null) {
                mPictureIv.setImageResource(R.drawable.empty);
                mPictureIv.setEnabled(false);
            } else {
                mPictureIv.setImageBitmap(bitmap);
                mPictureIv.setEnabled(true);
            }
            isClickBitmap = false;
            mCameraProxy.mZoom=0;
            mCameraProxy.handleZoom(true,false,mCameraDevice, characteristics, previewRequestBuilder, mPreviewRequest, captureSession);
            mCameraProxy.handleZoom(true,false,mCameraDevice, characteristics, recordvideoRequestBuilder, mPreviewRequest, captureSession);
        }
        // 打开相机
        if (!mTextureView.isAvailable()) {
            // Activity创建时，添加TextureView的监听，TextureView创建完成后就可以开启camera就行了
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
        {
            closeCamera();
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        }
    }

    // 重写onPause方法
    @Override
    public void onPause() {
        // Toast.makeText(MainActivity.this, "onPause", Toast.LENGTH_SHORT);
        if (isRecordingflg) {
            try {
                stopRecordingVideo();
                //录像结束声音
                mMediaActionSound.play(MediaActionSound.STOP_VIDEO_RECORDING);
                switchCameraWithMaskAnimation();
                mPictureIv.setImageBitmap(getLatestThumbBitmap(this));
                mPictureIv.setEnabled(true);
                record.setScaleX(1f);
                record.setScaleY(1f);
            } catch (CameraAccessException e) {
                throw new RuntimeException(e);
            }
        } else {
            // 关闭camera，关闭后台线程
          //  closeCamera();
            mPictureIv.setImageBitmap(getLatestThumbBitmap(this));
        }
        //stopBackgroundThread();
        super.onPause();
        // 注销监听器
       // sensorManager.unregisterListener(sensorEventListener);
    }

    // 重写onStop方法
    @Override
    public void onStop() {
        Log.d("--onDestroy--", "onDestroy");
        File newFile = new File(Previous_recorderPath);
        if (newFile.exists()) {
            newFile.delete();
        }
        mFlashMode=2;
        SwichFlash();
        super.onStop();
    }

    // 重写onDestroy方法
    @Override
    public void onDestroy() {
        Log.d("--onDestroy--", "onDestroy");
        File newFile = new File(Previous_recorderPath);
        if (newFile.exists()) {
            newFile.delete();
        }
        super.onDestroy();
    }


    // 计算两点之间的距离
    private static float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /*
    初始化屏幕
     */
    private void initScreen() {
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        getWindow().getDecorView().setSystemUiVisibility(flags);
        getWindow().setStatusBarColor(getColor(android.R.color.transparent));
        getWindow().setNavigationBarColor(getColor(android.R.color.transparent));
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // 初始化变量
    private void initVariable() {
        cameraState = restoreCameraState();                                      // 恢复相机状态

        mTextureView = findViewById(R.id.texture);                              // 绑定TextureView
        mRootLayout = findViewById(R.id.root);                                  // 绑定根布局
        timerText = findViewById(R.id.timer_text);                              // 绑定计时器
        mCustomViewL = findViewById(R.id.mCustomView);                          // 绑定自定义View
        record = findViewById(R.id.recordvideo);                                // 绑定录制按钮
        switch_camera = findViewById(R.id.switch_camera);                       // 绑定切换摄像头按钮
        capture = findViewById(R.id.capture);                                   // 绑定拍照按钮
        switch_frame = findViewById(R.id.frame_switch);                         // 绑定切换画幅按钮
        void_quality = findViewById(R.id.void_quality);                         // 绑定清晰度切换按钮
        falsh_switch = findViewById(R.id.falsh_switch);                         // 绑定闪光灯切换按钮
        Load = findViewById(R.id.Load);                                           // 绑定加载按钮
        mPictureIv = findViewById(R.id.picture_iv);                             // 绑定图片显示控件
        focusSunView = findViewById(R.id.focus_sun_view);                        // 初始化焦点光圈
        Choose = findViewById(R.id.Choose);                                      // 绑定顶部画幅切换选择栏
        Choose2 = findViewById(R.id.Choose2);                                      // 绑定顶部部质量切换选择栏
        Title = findViewById(R.id.title);                                        // 绑定顶部标题栏

        //初始化各个画幅切换按钮
        option1 = findViewById(R.id.option1);
        option2 = findViewById(R.id.option2);
        option3 = findViewById(R.id.option3);
        option4 = findViewById(R.id.option4);
        option5 = findViewById(R.id.option5);

        // 禁用按钮
        record.setEnabled(false);
        capture.setEnabled(false);
        switch_frame.setEnabled(false);
        void_quality.setEnabled(false);
        falsh_switch.setEnabled(false);
        switch_camera.setEnabled(false);
        Load.setEnabled(false);
        option2.setTextColor(Color.YELLOW);
        option5.setTextColor(Color.YELLOW);

        mCameraProxy = new Camera2Proxy(this);                       // 创建Camera2Proxy对象

        Log.d("--largest--", "largest: " + cameraState.previewWidth);
        //默认画幅
        if (cameraState.previewHeight == 0) {
            largest = new Size(4, 3);
        } else {
            if (cameraState.previewWidth == 1) {
                mCameraProxy.mZoom = 0;
                option1.setTextColor(Color.YELLOW);
                option2.setTextColor(Color.WHITE);
                option3.setTextColor(Color.WHITE);
                switch_frame.setText(option1.getText().toString());
                Title.setVisibility(View.VISIBLE);
                Choose.setVisibility(View.GONE);
                isCapture = true;
                largest = new Size(1, 1);
                saveCameraState(1, 1);
                isOption = true;
            } else if (cameraState.previewWidth == 20) {
                option1.setTextColor(Color.WHITE);
                option2.setTextColor(Color.WHITE);
                option3.setTextColor(Color.YELLOW);
                switch_frame.setText(option3.getText().toString());
                Choose.setVisibility(View.GONE);
                Title.setVisibility(View.VISIBLE);
                largest = new Size(20, 9);
                saveCameraState(20, 9);
                isCapture = true;
                isOption = true;
            }

        }

        // 获取屏幕宽高
        mDisplayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);
        screenWidth = mDisplayMetrics.widthPixels;
        screenHeight = mDisplayMetrics.heightPixels;
        Log.d("--Screen--", "width:" + screenWidth + " height:" + screenHeight);
        int proportion = (int) ((float) screenWidth / screenHeight * 100);
        Log.d("-123-", "123:" + proportion);
        if (proportion < 50) {
            isFlat = false;
        } else {
            isFlat= true;
        }

        surfaces = new ArrayList<>();                                       // 用于存储Surface
        handler = new Handler(Looper.getMainLooper());                      // UI线程的Handler
        handler2 = new Handler(Looper.getMainLooper());                      // 后台线程的Handler
        mMediaActionSound = new MediaActionSound();                         // 拍照声音
        Previous_recorderPath = null;                                       // 用于存储上一次录制的视频路径
        newroidPath = null;                                                 // 用于存储当前录制的视频路径

        // 显示最近一次拍照的图片
        Bitmap bitmap = getLatestThumbBitmap(this);
        if (bitmap == null) {
            mPictureIv.setImageResource(R.drawable.empty);
            mPictureIv.setEnabled(false);
        } else {
            mPictureIv.setImageBitmap(bitmap);
            mPictureIv.setEnabled(true);
        }

        focusSunView.setExposureLimit(0.5f, -0.5f);         // 设置曝光范围
        // 设置焦点光圈的曝光值监听器
        focusSunView.setOnExposureChangeListener(new FocusSunView.OnExposureChangeListener() {
            @Override
            public void onExposureChangeListener(float exposure) {
                // 应用曝光值到 Camera2 API
                applyExposure(exposure);
            }
        });
        btnAnimation = AnimationUtils.loadAnimation(this, R.anim.btn_anim);          // 加载动画
        captureAnimation=AnimationUtils.loadAnimation(this, R.anim.btn_anim);
        bitmapAnimation = AnimationUtils.loadAnimation(this, R.anim.alpha_out);
        mTextureViewAnimation = AnimationUtils.loadAnimation(this, R.anim.alpha_in);
        LoadAnimation = AnimationUtils.loadAnimation(this, R.anim.dialog_loading);     // 加载加载动画
        recordTouchListener();                                                              // 录制按钮动画
       // captureTouchListener();                                                             // 拍照按钮动画
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);                   // 获取传感器管理器
        sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_NORMAL);
    }

    // 初始化自定义View
    @SuppressLint("ClickableViewAccessibility")
    private void initCustomViewL() {
        String[] name = new String[]{"拍照", "视频"};
        mCustomViewL.setOnTouchListener(this);
        mCustomViewL.addIndicator(name);
        mGestureDetector = new GestureDetector(this, new MyGestureDetectorListener());
    }

    /**
     * 初始化权限请求。
     * 申请以下权限：
     * - 摄像头权限
     * - 录音权限
     * - 写入外部存储权限
     * - 读取外部存储权限
     * - 修改音频设置权限
     */
    private void initRequestPermissions() {
        // 申请摄像头权限
        requestPermissions(new String[]{
                // 摄像头权限
                Manifest.permission.CAMERA,
                // 录音权限
                Manifest.permission.RECORD_AUDIO
        }, 0x123);
    }

    /**
     * 处理权限请求结果。
     *
     * @param requestCode  请求码，用于标识哪个权限组被用户处理。
     * @param permissions  请求的具体权限数组。
     * @param grantResults 对应于permissions数组的权限结果数组。
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0x123) {
            boolean allPermissionsGranted = true;
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("--Permission--", permissions[i] + " granted");
                } else {
                    Log.d("--Permission--", permissions[i] + " denied");
                    allPermissionsGranted = false;
                }
            }
            Log.d("--Permission--", "allPermissionsGranted: " + allPermissionsGranted);
            if (!allPermissionsGranted) {
                // 如果有任何权限未被授予，则跳转到应用详情页面
                showPermissionDialog();
            }
        }
    }
    private void showPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("权限未被授予")
                .setMessage("应用需要以下权限才能正常运行：\n\n- 摄像头权限\n- 录音权限\n- 修改音频设置权限\n\n请前往应用详情页面手动授予这些权限。")
                .setPositiveButton("前往设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showAppDetails();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finishAffinity();
                    }
                })
                .show();
    }
    private void showAppDetails() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }


    // 初始化点击监听器
    private void initClickListener() {

        // 录像按钮
        record.setOnClickListener(v -> {
            if (v.getId() == R.id.recordvideo) {
                if (!isRecording) {
                    mTextureView.setEnabled(false);
                    mCustomViewL.setEnabled(false);
                    mCustomViewL.setVisibility(View.GONE);
                    mCustomViewL.textViews.get(0).setEnabled(false);
                    mCustomViewL.textViews.get(1).setEnabled(false);
                    isRecordingflg = true;
                    //播放录像声音
                    mMediaActionSound.play(MediaActionSound.START_VIDEO_RECORDING);
                    record.setEnabled(false);
                    timerText.setVisibility(View.VISIBLE);
                    timerText.setEnabled(false);
                    switch_camera.setVisibility(View.GONE);
                    switch_camera.setEnabled(false);
                    mPictureIv.setVisibility(View.GONE);
                    mPictureIv.setEnabled(false);
                    falsh_switch.setVisibility(View.GONE);
                    falsh_switch.setEnabled(false);
                    switch_frame.setVisibility(View.GONE);
                    switch_frame.setEnabled(false);
                    void_quality.setVisibility(View.GONE);
                    void_quality.setEnabled(false);
                    capture.setEnabled(false);
                    capture.setVisibility(View.GONE);
                    option4.setEnabled(false);
                    option5.setEnabled(false);
                    Choose2.setVisibility(View.GONE);
                    Title.setVisibility(View.VISIBLE);

                    //延迟500ms执行播放录像声音
                    handler2.postDelayed(this::startRecordingVideo, 500);
                    mMediaActionSound.play(MediaActionSound.START_VIDEO_RECORDING);
                } else {
                    try {
                        mTextureView.setEnabled(false);
                        // isStopRecord=true;
                        // switchCameraWithMaskAnimation();
                        stopRecordingVideo();
                        //录像结束声音
                        mMediaActionSound.play(MediaActionSound.STOP_VIDEO_RECORDING);
                        Bitmap bitmap = getLatestThumbBitmap(this);
                        if (isLeftTransverse) {
                            // 旋转图片（如果需要）
                            bitmap = rotateBitmap(bitmap, -90, false, false);
                            isLeftTransverse = false;
                        }
                        if (isRightTransverse) {
                            bitmap = rotateBitmap(bitmap, 90, false, false);
                            isLeftTransverse = false;
                        }
                        if (isinversion) {
                            // 旋转图片（如果需要）
                            bitmap = rotateBitmap(bitmap, 180, false, false);
                            isinversion = false;
                        }

                        mPictureIv.setImageBitmap(bitmap);
                        mPictureIv.setEnabled(true);
                        // ImageUtils.setLatestThumbBitmapAsync(mPictureIv, CameraActivity.this);
                    } catch (CameraAccessException e) {
                        throw new RuntimeException(e);
                    }
                   // mTextureView.setEnabled(true);
                }
            }
        });

        // 切换摄像头按钮
        switch_camera.setOnClickListener(v -> {
            if (v.getId() == R.id.switch_camera) {
                switch_camera.setEnabled(false);
                switch_camera.startAnimation(btnAnimation);
                mCameraProxy.mZoom = 0;
                //动画结束回调
                btnAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        switch_camera.setEnabled(true);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                isSwichCamera = true;
                isLayoutSwich = false;
                isOption = false;
                isCaptureingflg = false;
                mCustomViewL.setEnabled(false);
                mCustomViewL.textViews.get(0).setEnabled(false);
                mCustomViewL.textViews.get(1).setEnabled(false);
                mPictureIv.setEnabled(false);
                capture.setEnabled(false);
                if (maskViewbuf != null && parentbuf != null) {
                    maskViewbuf.setAlpha(0f);
                    parentbuf.removeView(maskView);
                }
                // switchCameraWithMaskAnimation();
                mTextureView.startAnimation(mTextureViewAnimation);
                //动画结束回调
                mTextureViewAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        mCameraProxy.mZoom = 0;
                        if (isSwichCamera) {
                            cameraId = "1".equals(cameraId) ? "0" : "1";
                            // 切换摄像头
                            closeCamera();
                            isSwichCamera = false;
                        }
                        openCamera(mTextureView.getWidth(), mTextureView.getHeight());
                        mTextureView.setEnabled(false);
                        // 关闭对焦
                        focusSunView.setVisibility(View.INVISIBLE);
                        mCameraProxy.handleZoom(true,false,mCameraDevice, characteristics, previewRequestBuilder, mPreviewRequest, captureSession);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mTextureView.setEnabled(true);
                        mCustomViewL.setEnabled(true);
                        mCustomViewL.textViews.get(0).setEnabled(true);
                        mCustomViewL.textViews.get(1).setEnabled(true);
                        mPictureIv.setEnabled(true);
                        capture.setEnabled(true);
                        mFlashMode=lastFlashMode;
                        SwichFlash();
                        //  mTextureView.startAnimation(mTextureViewAnimation);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
            }
        });

        // 拍照按钮x
        capture.setOnClickListener(v -> {
            if (v.getId() == R.id.capture) {
                mTextureViewAnimation.setAnimationListener(null);
                capture.startAnimation(captureAnimation); //拍照按钮动画
                //动漫回调
                captureAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                                                           //  Toast.makeText(CameraActivity.this, "点击了拍照按钮", Toast.LENGTH_SHORT).show();
                        capture.setEnabled(false);
                        option1.setEnabled(false);
                        option2.setEnabled(false);
                        option3.setEnabled(false);
                        switch_camera.setEnabled(false);
                        Log.d("--1CameraActivity--", "123");
                        takePicture();
                        mMediaActionSound.play(MediaActionSound.SHUTTER_CLICK); // 播放拍照声音
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                    }
                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
            }
        });

        // 切换帧按钮
        switch_frame.setOnClickListener(v -> {
            if (v.getId() == R.id.frame_switch) {
                SwichFrame();
                switch_frame.setEnabled(false);
                falsh_switch.setEnabled(false);
            }
        });

        // 切换质量按钮
        void_quality.setOnClickListener(v -> {
            if (v.getId() == R.id.void_quality) {
                SwichQuality();
                void_quality.setEnabled(false);
            }
        });

        // 图片按钮
        mPictureIv.setOnClickListener(v -> {
            if (v.getId() == R.id.picture_iv) {
                isClickBitmap = true;
                mCameraProxy.mZoom = 0;
                getLatestThumbBitmap(this);
                // 创建意图并启动
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(ImageUtils.imageUri, "image/*");
                startActivity(intent);
            }
        });

        // 闪光灯按钮
        falsh_switch.setOnClickListener(v -> {
            if (v.getId() == R.id.falsh_switch) {
                SwichFlash();
            }
        });
        //1:1
        option1.setOnClickListener(v -> {
            if (v.getId() == R.id.option1) {
                capture.setEnabled(false);
                focusSunView.setVisibility(View.INVISIBLE);
                option1.setTextColor(Color.YELLOW);
                option2.setTextColor(Color.WHITE);
                option3.setTextColor(Color.WHITE);
                switch_frame.setText(option1.getText().toString());
                Title.setVisibility(View.VISIBLE);
                Choose.setVisibility(View.GONE);
                option1.setEnabled(false);
                option2.setEnabled(false);
                option3.setEnabled(false);
                if(largest.getWidth() != 1) {
                    mCameraProxy.mZoom = 0;
                    isCapture = true;
                    largest = new Size(1, 1);
                    saveCameraState(1, 1);
                    isOption = true;
                    switchCameraWithMaskAnimation();
                    mFlashMode = 2;
                    SwichFlash();
                }
                else {
                    switch_camera.setEnabled(true);
                    mCustomViewL.setEnabled(true);
                    mPictureIv.setEnabled(true);
                    // capture.setEnabled(true);
                    // record.setEnabled(true);
                    mCustomViewL.textViews.get(0).setEnabled(true);
                    mCustomViewL.textViews.get(1).setEnabled(true);
                    mTextureView.setEnabled(true);
                    option1.setEnabled(true);
                    option2.setEnabled(true);
                    option3.setEnabled(true);
                    option4.setEnabled(true);
                    option5.setEnabled(true);
                    switch_frame.setEnabled(true);
                    void_quality.setEnabled(true);
                    falsh_switch.setEnabled(true);
                    // 线程结束的回调
                    capture.setEnabled(true);
                    record.setEnabled(true);
                }
            }
        });

        // 4:3
        option2.setOnClickListener(v -> {
            if (v.getId() == R.id.option2) {
                capture.setEnabled(false);

                option1.setTextColor(Color.WHITE);
                option3.setTextColor(Color.WHITE);
                option2.setTextColor(Color.YELLOW);
                focusSunView.setVisibility(View.INVISIBLE);
                switch_frame.setText(option2.getText().toString());
                Title.setVisibility(View.VISIBLE);
                Choose.setVisibility(View.GONE);
                option1.setEnabled(false);
                option2.setEnabled(false);
                option3.setEnabled(false);
                if(largest.getWidth()!=4) {
                    mCameraProxy.mZoom = 0;
                    isCapture = true;
                    largest = new Size(4, 3);
                    saveCameraState(4, 3);
                    isOption = true;
                    switchCameraWithMaskAnimation();
                    mFlashMode = 2;
                    SwichFlash();
                }
                else {
                    switch_camera.setEnabled(true);
                    mCustomViewL.setEnabled(true);
                    mPictureIv.setEnabled(true);
                    // capture.setEnabled(true);
                    // record.setEnabled(true);
                    mCustomViewL.textViews.get(0).setEnabled(true);
                    mCustomViewL.textViews.get(1).setEnabled(true);
                    mTextureView.setEnabled(true);
                    option1.setEnabled(true);
                    option2.setEnabled(true);
                    option3.setEnabled(true);
                    option4.setEnabled(true);
                    option5.setEnabled(true);
                    switch_frame.setEnabled(true);
                    void_quality.setEnabled(true);
                    falsh_switch.setEnabled(true);
                    // 线程结束的回调
                    capture.setEnabled(true);
                    record.setEnabled(true);
                }
            }
        });

        // 16:9
        option3.setOnClickListener(v -> {
            if (v.getId() == R.id.option3) {
                capture.setEnabled(false);
                option1.setTextColor(Color.WHITE);
                option2.setTextColor(Color.WHITE);
                option3.setTextColor(Color.YELLOW);
                focusSunView.setVisibility(View.INVISIBLE);
                switch_frame.setText(option3.getText().toString());
                Choose.setVisibility(View.GONE);
                Title.setVisibility(View.VISIBLE);
                option1.setEnabled(false);
                option2.setEnabled(false);
                option3.setEnabled(false);
                if(largest.getWidth()!=20) {
                    mCameraProxy.mZoom = 0;
                    largest = new Size(20, 9);
                    saveCameraState(20, 9);
                    isCapture = true;
                    isOption = true;
                    switchCameraWithMaskAnimation();
                    mFlashMode = 2;
                    SwichFlash();
                }
                else {
                    switch_camera.setEnabled(true);
                    mCustomViewL.setEnabled(true);
                    mPictureIv.setEnabled(true);
                    // capture.setEnabled(true);
                    // record.setEnabled(true);
                    mCustomViewL.textViews.get(0).setEnabled(true);
                    mCustomViewL.textViews.get(1).setEnabled(true);
                    mTextureView.setEnabled(true);
                    option1.setEnabled(true);
                    option2.setEnabled(true);
                    option3.setEnabled(true);
                    option4.setEnabled(true);
                    option5.setEnabled(true);
                    switch_frame.setEnabled(true);
                    void_quality.setEnabled(true);
                    falsh_switch.setEnabled(true);
                    // 线程结束的回调
                    capture.setEnabled(true);
                    record.setEnabled(true);
                }
            }
        });
        //480p
        option4.setOnClickListener(v -> {
            if (v.getId() == R.id.option4) {
                option5.setTextColor(Color.WHITE);
                option4.setTextColor(Color.YELLOW);
                focusSunView.setVisibility(View.INVISIBLE);
                void_quality.setText(option4.getText().toString());
                Choose2.setVisibility(View.GONE);
                Title.setVisibility(View.VISIBLE);
                option4.setEnabled(false);
                option5.setEnabled(false);
                record.setEnabled(false);
                if(!isRecord4) {
                    isRecord4 = true;
                    isRecord5 = false;
                    isOption = true;
                    switchCameraWithMaskAnimation();
                }
                else {
                    mCameraProxy.mZoom = 0;
                    switch_camera.setEnabled(true);
                    mCustomViewL.setEnabled(true);
                    mPictureIv.setEnabled(true);
                    // capture.setEnabled(true);
                    // record.setEnabled(true);
                    mCustomViewL.textViews.get(0).setEnabled(true);
                    mCustomViewL.textViews.get(1).setEnabled(true);
                    mTextureView.setEnabled(true);
                    option1.setEnabled(true);
                    option2.setEnabled(true);
                    option3.setEnabled(true);
                    option4.setEnabled(true);
                    option5.setEnabled(true);
                    switch_frame.setEnabled(true);
                    void_quality.setEnabled(true);
                    falsh_switch.setEnabled(true);
                    // 线程结束的回调
                    capture.setEnabled(true);
                    record.setEnabled(true);
                }
            }
        });
        //720p
        option5.setOnClickListener(v -> {
            if (v.getId() == R.id.option5) {
                mCameraProxy.mZoom = 0;
                option4.setTextColor(Color.WHITE);
                option5.setTextColor(Color.YELLOW);
                focusSunView.setVisibility(View.INVISIBLE);
                void_quality.setText(option5.getText().toString());
                Choose2.setVisibility(View.GONE);
                Title.setVisibility(View.VISIBLE);
                option4.setEnabled(false);
                option5.setEnabled(false);
                record.setEnabled(false);
                if (!isRecord5) {
                    isRecord5 = true;
                    isRecord4 = false;
                    isOption = true;
                    switchCameraWithMaskAnimation();
                } else {
                    mCameraProxy.mZoom = 0;
                    switch_camera.setEnabled(true);
                    mCustomViewL.setEnabled(true);
                    mPictureIv.setEnabled(true);
                    // capture.setEnabled(true);
                    // record.setEnabled(true);
                    mCustomViewL.textViews.get(0).setEnabled(true);
                    mCustomViewL.textViews.get(1).setEnabled(true);
                    mTextureView.setEnabled(true);
                    option1.setEnabled(true);
                    option2.setEnabled(true);
                    option3.setEnabled(true);
                    option4.setEnabled(true);
                    option5.setEnabled(true);
                    switch_frame.setEnabled(true);
                    void_quality.setEnabled(true);
                    falsh_switch.setEnabled(true);
                    // 线程结束的回调
                    capture.setEnabled(true);
                    record.setEnabled(true);
                }
            }
        });
    }

    /**
     * 设置记录按钮的触摸监听器
     * 此方法用于处理记录按钮的触摸事件，主要包括按下时的处理逻辑
     */
    @SuppressLint("ClickableViewAccessibility")
    private void recordTouchListener() {
        record.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // 按下
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (!isRecording) {
                        record.startAnimation(btnAnimation); //拍照按钮动画
                        record.setScaleX(0.8f);
                        record.setScaleY(0.8f);
                    } else {
                        record.setScaleX(1f);
                        record.setScaleY(1f);
                    }

                }
                return false;
            }
        });
    }

    /**
     * 切换水质检测的可见性状态
     * <p>
     * 此方法用于在用户界面中切换显示状态，主要是隐藏标题（Title）并显示选择项（Choose2）
     * 它通过修改视图的可见性属性来实现这一点
     */
    private void SwichQuality() {
        Title.setVisibility(View.GONE);
        Choose2.setVisibility(View.VISIBLE);
    }


    /**
     * 切换界面布局的显示状态
     * 此方法用于在界面中隐藏标题栏并显示选择栏，通常在某种操作模式切换时调用此方法
     * 以引导用户注意力从标题栏转移到选择栏
     */
    private void SwichFrame() {
        Title.setVisibility(View.GONE);
        Choose.setVisibility(View.VISIBLE);
    }

    /**
     * 切换闪光灯模式
     * 通过改变闪光灯模式状态，控制闪光灯的行为
     */
    private void SwichFlash() {
        switch (mFlashMode) {
            case 0:
                Autoflash = true;
                Strongflash = false;
                mFlashMode = 1;
                lastFlashMode = 0;
                falsh_switch.setImageResource(R.drawable.falshlightaout);
                // 设置闪光灯模式为自动
                previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                focusRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                try {
                    captureSession.setRepeatingRequest(
                            previewRequestBuilder.build(),
                            mPreCaptureCallback, null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                    return;
                }

                break;
            case 1:
                mFlashMode = 2;
                lastFlashMode = 1;
                falsh_switch.setImageResource(R.drawable.falshlightcopen);
                //设置闪光灯为强闪
                previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                focusRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                Strongflash = true;
                Autoflash = false;
                break;
            case 2:
                mFlashMode = 3;
                lastFlashMode = 2;
                Strongflash = false;
                Autoflash = false;
                falsh_switch.setImageResource(R.drawable.falshlightclose);
                // 设置闪光灯模式为自动
                previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
                focusRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                focusRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
                try {
                    // 更新预览请求
                    captureSession.setRepeatingRequest(
                            previewRequestBuilder.build(),
                            mPreCaptureCallback, null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                    return;
                }
                break;
            case 3:
                mFlashMode = 0;
                lastFlashMode = 3;
                falsh_switch.setImageResource(R.drawable.flashlight);
                // 设置闪光灯模式为常亮
                previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
                focusRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
                try {
                    captureSession.setRepeatingRequest(
                            previewRequestBuilder.build(),
                            mPreCaptureCallback, null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
        }
    }

    // 强光
    private void StrongFlash() {
        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
        focusRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
        try {
            captureSession.setRepeatingRequest(
                    previewRequestBuilder.build(),
                    mPreCaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止视频录制
     * <p>
     * 本方法负责停止视频录制，它通过停止MediaRecorder来实现这一点
     * 如果停止操作失败，将打印出异常信息
     * 最后，它将释放与MediaRecorder相关的资源，并关闭相机预览会话
     *
     * @throws CameraAccessException 当访问相机设备时发生错误
     */
    @SuppressLint("SetTextI18n")
    private void stopRecordingVideo() throws CameraAccessException {
        try {
            mMediaRecorder.stop();
        } catch (IllegalStateException e) {
            Log.d("MediaRecorder", "stopRecordingVideo");
            e.printStackTrace();
        } finally {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        // 清空 surfaces 集合
        surfaces.clear();

        isRecording = false;
        isRecordingflg = false;

        // mCameraProxy.mZoom=0;
        timerText.setVisibility(View.GONE);
        switch_camera.setVisibility(View.VISIBLE);
        switch_camera.setEnabled(true);
        mPictureIv.setVisibility(View.VISIBLE);
        mPictureIv.setEnabled(true);
        void_quality.setVisibility(View.VISIBLE);
        void_quality.setEnabled(true);
        mCustomViewL.setVisibility(View.VISIBLE);
        mCustomViewL.setEnabled(true);
        option4.setEnabled(true);
        option5.setEnabled(true);
        mCustomViewL.textViews.get(0).setEnabled(true);
        mCustomViewL.textViews.get(1).setEnabled(true);
        // 停止计时器
        handler.removeCallbacks(runnable);
        //将时间清空
        timerText.setText("00:00");
        //将时间置零
        seconds = 0;
        Log.d("stopRecordingVideo", newroidPath);
        // 刷新媒体库
        MediaScannerConnection.scanFile(this, new String[]{newroidPath}, null, null);
        // 发送广播通知媒体库更新
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(newroidPath))));
        // 重新创建预览会话
        createCaptureSession(mCameraDevice);
        recordvideoRequestBuilder = null;
    }

    /**
     * 初始化视频录制
     * <p>
     * 本方法主要用于设置和初始化相机设备，以进行视频录制它涉及创建捕获请求，
     * 设置预览和录制的Surface，以及配置相机参数，如对焦和曝光模式
     *
     * @throws CameraAccessException 如果访问相机设备时遇到错误
     */
    private void startRecordingVideoinit() throws CameraAccessException {
        // 更新预览请求
        recordvideoRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
        // 将MediaRecorder的Surface添加到请求中
        recordvideoRequestBuilder.addTarget(mMediaRecorder.getSurface());
        // 设置预览请求的输出Surface
        Rect zoomRect = previewRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION);
        // 设置预览输出的Surface
        if (zoomRect != null) {
            // 设置裁剪区域
            recordvideoRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);
        }
        // 将预览Surface添加到预览请求中
        recordvideoRequestBuilder.addTarget(previewSurface); // 保留预览Surface
        // 设置自动白平衡
        recordvideoRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
        // 设置自动曝光模式
        recordvideoRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        // 设置闪光灯模式为关闭
        recordvideoRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
        // 更新捕获会话
        captureSession.setRepeatingRequest(recordvideoRequestBuilder.build(), null, null);
    }

    /**
     * 开始录制视频
     * <p>
     * 此方法初始化视频录制过程，启动MediaRecorder，并配置用户界面元素以反映录制状态
     * 它还启动了一个计时器线程，用于在录制开始后进行倒计时
     */
    private void startRecordingVideo() {
        try {
            startRecordingVideoinit();
            mMediaRecorder.start();
            isRecording = true;
            mTextureView.setEnabled(true);
            // 启动计时器
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // 等待一秒
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // 启动计时器
                    startTimer();
                }
            }).start();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 启动计时器
     * 当计时器启动时，会创建一个可运行对象，用于每秒更新计时器的显示，并在计时开始后启用记录功能
     */
    private void startTimer() {
        runnable = new Runnable() {
            @Override
            public void run() {
                seconds++;
                if (seconds > 1) {
                    record.setEnabled(true);
                }
                @SuppressLint("DefaultLocale") String timeString = String.format("%02d:%02d", seconds / 60, seconds % 60);
                timerText.setText(timeString);
                handler.postDelayed(this, 1000);
            }
        };
        // 开始计时
        handler.post(runnable);
    }

    /**
     * 切换摄像头时带有遮罩动画的方法
     * 该方法用于在切换摄像头或执行某些操作时，通过动画效果显示或隐藏一个黑色遮罩视图
     */
    @SuppressLint("CutPasteId")
    private void switchCameraWithMaskAnimation() {
        // 确保没有正在进行的动画
        cancelCurrentAnimator();
        mTextureViewAnimation.setAnimationListener(null);
        if (mCameraDevice != null && isLayoutSwich || isSwichCamera || isOption || isStopRecord || isCaptureingflg) {
            if (maskViewbuf != null && parentbuf != null) {
                maskViewbuf.setAlpha(0f);
                parentbuf.removeView(maskView);
            }
            // 创建一个蒙版视图
            maskView = new View(this);
            maskView.setBackgroundColor(Color.BLACK); // 设置背景色为黑色
            maskView.setAlpha(0f); // 初始透明度为0
            int width = findViewById(R.id.container).getWidth();
            int height = findViewById(R.id.container).getHeight();
            Log.d("switchCameraWithMaskAnimation", "width:" + width + " height:" + height);
            // 获取 mTextureView 的父布局
            ViewGroup parent = (ViewGroup) mTextureView.getParent();
            // 设置蒙版视图的布局参数
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);

            maskView.setLayoutParams(params);
            parent.addView(maskView, params);
            // 添加动画效果
            ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(150); // 动画持续时间
//            if (isLayoutSwich) {
//                animator.setDuration(600); // 动画持续时间
//            }
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float alpha = (float) animation.getAnimatedValue();
                    maskView.setAlpha(alpha);
                }
            });
            maskViewbuf = maskView;
            parentbuf = parent;
            if (isAnimator) {
                animator.start();
                isAnimator = false;
            }
            currentAnimator = animator;
            // 在动画结束后关闭相机
            animator.addListener(new AnimatorListenerAdapter() {
                // 动画结束后执行
                @Override
                public void onAnimationEnd(Animator animation) {
                    isAnimator = true;
                    if (isSwichCamera) {
                        SwichCamera(maskView);
                    }
                    if (isOption) {
                        SwitchFrame(maskView);
                    }
                    if (isLayoutSwich) {
                        isLayoutSwich = false;
                        Swichlayout(maskView);
                    }
                    if (isStopRecord) {
                        RecordLoading(maskView);
                    }
                    if (isCaptureingflg) {
                        isCaptureingflg = false;
                        // 恢复初始状态
                        maskView.setAlpha(0f);
                        parent.removeView(maskView); // 移除蒙版视图
                        capture.setEnabled(true);
                        //CaptureLoading(maskView);
                    }
                }
            });


        }
    }

    /**
     * 取消当前正在运行的动画器
     * 如果当前动画器存在且正在运行，该方法将取消动画器并将其设置为null
     */
    private void cancelCurrentAnimator() {
        if (currentAnimator != null && currentAnimator.isRunning()) {
            currentAnimator.cancel();
            currentAnimator = null;
        }
    }

    /**
     * 开始记录加载过程，并在延迟后移除蒙版
     *
     * @param maskView 蒙版视图，用于在加载时遮挡界面的部分区域
     */
    private void RecordLoading(View maskView) {
        isStopRecord = false;
        Load.setAlpha(1f);
        Load.setVisibility(View.VISIBLE);
        Load.startAnimation(LoadAnimation); //拍照按钮动画
        // 开启一个新的线程实现移除蒙版，并且延迟600毫秒，等待摄像头切换成功
        // 创建 Runnable
        Runnable removeMaskRunnable = new Runnable() {
            @Override
            public void run() {
                ViewGroup parent = (ViewGroup) mTextureView.getParent();
                Load.setAlpha(0f);
                Load.setVisibility(View.GONE);
                maskView.setAlpha(0f);
                parent.removeView(maskView); // 移除蒙版视图

            }
        };

        // 执行延迟任务
        handler2.postDelayed(removeMaskRunnable, 650); // 延迟 870 毫秒
    }

    /**
     * 切换摄像头布局
     * 此方法主要用于在打开相机时切换摄像头，并在界面上反映相应的状态变化
     *
     * @param maskView 用于遮罩的视图，在摄像头切换后将被移除
     */
    private void Swichlayout(final View maskView) {
        isPanningflg=false;
        openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        if (isLayout) {
            mCustomViewL.textViews.get(0).setEnabled(false);
            mCustomViewL.setEnabled(false);
            capture.setEnabled(false);

        } else {
            mCustomViewL.textViews.get(1).setEnabled(false);
            mCustomViewL.setEnabled(false);
            record.setEnabled(false);
        }
        mTextureView.setEnabled(false);
        // 开启一个新的线程实现移除蒙版，并且延迟600毫秒，等待摄像头切换成功
        // 创建 Handler
        // 创建 Runnable
        Runnable removeMaskRunnable = new Runnable() {
            @Override
            public void run() {
                ViewGroup parent = (ViewGroup) mTextureView.getParent();
                maskView.setAlpha(0f);
                parent.removeView(maskView); // 移除蒙版视图
                mCustomViewL.textViews.get(0).setEnabled(true);
                mCustomViewL.textViews.get(1).setEnabled(true);
                mCustomViewL.setEnabled(true);
                if (colorState == 1) {
                    record.setEnabled(true);
                } else {
                    capture.setEnabled(true);
                }
                capture.setEnabled(true);
                switch_camera.setEnabled(true);
                mTextureView.setEnabled(true);
                void_quality.setEnabled(true);
                switch_frame.setEnabled(true);
                record.setEnabled(true);
                isPanningflg=true;
            }
        };

        // 执行延迟任务
        handler2.postDelayed(removeMaskRunnable, 650); // 延迟 870 毫秒
    }

    /**
     * 切换摄像头并移除蒙版视图
     * 此方法首先打开相机，并禁用一些交互元素以确保摄像头切换期间的用户体验
     * 在切换完成后，通过延迟执行的线程来恢复这些交互元素的可用性
     *
     * @param maskView 蒙版视图，用于在摄像头切换期间遮挡界面的一部分
     */
    private void SwitchFrame(final View maskView) {
        openCamera(mTextureView.getWidth(), mTextureView.getHeight());
       // initAutoFitTextureView(mTextureView, mTextureView.getWidth(), mTextureView.getHeight());
       // startPreview();
        isOption = false;
        switch_camera.setEnabled(false);
        record.setEnabled(false);
        mCustomViewL.setEnabled(false);
        mPictureIv.setEnabled(false);
        capture.setEnabled(false);
        mTextureView.setEnabled(false);
        mCustomViewL.textViews.get(0).setEnabled(false);
        mCustomViewL.textViews.get(1).setEnabled(false);


        // 开启一个新的线程实现移除蒙版，并且延迟600毫秒，等待摄像头切换成功
        // 创建 Runnable
        Runnable removeMaskRunnable = new Runnable() {
            @Override
            public void run() {
                ViewGroup parent = (ViewGroup) mTextureView.getParent();
                maskView.setAlpha(0f);
                parent.removeView(maskView); // 移除蒙版视图
                switch_camera.setEnabled(true);
                mCustomViewL.setEnabled(true);
                mPictureIv.setEnabled(true);
               // capture.setEnabled(true);
               // record.setEnabled(true);
                mCustomViewL.textViews.get(0).setEnabled(true);
                mCustomViewL.textViews.get(1).setEnabled(true);
                mTextureView.setEnabled(true);
                option1.setEnabled(true);
                option2.setEnabled(true);
                option3.setEnabled(true);
                option4.setEnabled(true);
                option5.setEnabled(true);
                switch_frame.setEnabled(true);
                void_quality.setEnabled(true);
                falsh_switch.setEnabled(true);
                // 线程结束的回调
                capture.setEnabled(true);
                record.setEnabled(true);
            }
        };


        // 执行延迟任务
        handler2.postDelayed(removeMaskRunnable, 650); // 延迟 870 毫秒
    }

    /**
     * 切换摄像头操作
     * 此方法用于在后置摄像头和前置摄像头之间进行切换
     * 它会关闭当前摄像头并打开另一个摄像头，根据当前摄像头的ID来决定显示或隐藏闪光灯开关
     * 在切换摄像头后，会移除界面中的遮罩视图，并恢复部分视图的交互能力
     *
     * @param maskView 用于遮罩的视图，在切换摄像头后需要移除
     */
    private void SwichCamera(final View maskView) {
        cameraId = "1".equals(cameraId) ? "0" : "1";
        // 切换摄像头
        closeCamera();
        openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        isSwichCamera = false;

        // 创建 Runnable
        Runnable removeMaskRunnable = new Runnable() {
            @Override
            public void run() {
                ViewGroup parent = (ViewGroup) mTextureView.getParent();
                maskView.setAlpha(0f);
                parent.removeView(maskView); // 移除蒙版视图

                switch_camera.setEnabled(true);
                mCustomViewL.setEnabled(true);
                mPictureIv.setEnabled(true);
                capture.setEnabled(true);
                mCustomViewL.textViews.get(0).setEnabled(true);
                mCustomViewL.textViews.get(1).setEnabled(true);
                Handleding = true;
                mFlashMode = lastFlashMode;
                SwichFlash();
            }
        };
        if (Handleding) {
            Handleding = false;
            // 执行延迟任务
            handler2.postDelayed(removeMaskRunnable, 850); // 延迟 870 毫秒
        }
    }

    /**
     * 关闭相机设备
     * 如果当前有相机设备实例，则关闭该设备并设置实例为null
     */
    private void closeCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    // 方向
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    // 方向
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * 拍摄照片方法
     * 该方法负责调用相机硬件，设置拍照参数，并捕获照片
     */
    private void takePicture() {
        try {
            if (mFlashMode == 2) {
                StrongFlash();
            }
            // 创建捕获请求
            CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            // 设置预览输出的Surface
            captureBuilder.addTarget(mImageReader.getSurface());
            // 设置自动对焦模式
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 自动对焦
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            if (mCameraProxy.mPreviewRequestBuilder != null) {
                Rect zoomRect = mCameraProxy.mPreviewRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION);
                Log.d("--takePicture--", "zoomRect:" + zoomRect);
                if (zoomRect != null) {
                    captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);
                }
            }
            // 停止连续取景
            // captureSession.stopRepeating();
            // 获取设备方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            //int rotation=getRotationDegrees();
            if (isFrontCamera()) {
                rotation = (rotation + 180) % 360;
            }
            Log.d("--CameraActivity--", "rotation:" + rotation);
            // 根据设备方向计算设置照片的方向
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, rotation + 90);

            handler2.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mTextureView.startAnimation(bitmapAnimation);
                }
            }, 0);
            // 捕获一帧图像
            if (Strongflash) {

                handler2.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            captureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                                // 捕获开始
                                @Override
                                public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                                    mTextureView.setEnabled(false);
                                }

                                //捕获结束
                                @Override
                                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                                    super.onCaptureCompleted(session, request, result);
                                    mTextureView.setEnabled(true);
                                    capture.setEnabled(true);
                                    option1.setEnabled(true);
                                    option2.setEnabled(true);
                                    option3.setEnabled(true);
                                    switch_camera.setEnabled(true);
                                }
                            }, null);
                        } catch (CameraAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, 500);
            } else if (Autoflash) {
                //等待闪光灯亮起，执行拍照
                handler2.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            captureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                                // 捕获开始
                                @Override
                                public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                                }

                                //捕获结束
                                @Override
                                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                                    super.onCaptureCompleted(session, request, result);
                                    capture.setEnabled(true);
                                    option1.setEnabled(true);
                                    option2.setEnabled(true);
                                    option3.setEnabled(true);
                                    switch_camera.setEnabled(true);
                                }
                            }, null);
                        } catch (CameraAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, 500);
            } else //延迟1000ms后执行
            {
                captureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                    // 捕获开始
                    @Override
                    public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                        super.onCaptureStarted(session, request, timestamp, frameNumber);
                    }

                    //捕获结束
                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                        super.onCaptureCompleted(session, request, result);
                        option1.setEnabled(true);
                        option2.setEnabled(true);
                        option3.setEnabled(true);
                        capture.setEnabled(true);
                        switch_camera.setEnabled(true);
                    }
                }, null);

            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    /*
    滑动事件及回调函数
    */
    private static class MyGestureDetectorListener implements GestureDetector.OnGestureListener {

        // 按下
        @Override
        public boolean onDown(MotionEvent e) {
            Log.d("--CameraActivity--", "onDown");

            return true;
        }

        // 按下后松开
        @Override
        public void onShowPress(MotionEvent e) {
            // 不做任何操作
        }

        // 单击
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        // 水平滑动
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            final float deltaX = e2.getX() - e1.getX();
            final float deltaY = e2.getY() - e1.getY();

            if (Math.abs(deltaY) < Math.abs(deltaX)) { // 确保水平滑动
                if (deltaX > DISTANCE_LIMIT) {
                    Log.d("--CameraActivity--", "左滑");
                    // Layout_Switch(mCustomViewL.getIndex());
                    if (mCustomViewL.colorState == 1) {
                        mCustomViewL.scrollRight();
                    }
                } else if (deltaX < -DISTANCE_LIMIT) {
                    Log.d("--CameraActivity--", "右滑");
                    if (mCustomViewL.colorState == 0) {
                        mCustomViewL.scrollLeft();
                    }
                }
            }
            return true;
        }

        // 长按
        @Override
        public void onLongPress(@NonNull MotionEvent e) {
            // 不做任何操作
        }

        // 快速滑动
        @Override
        public boolean onFling(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }
    }

    // 触摸监听
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    /**
     * 切换布局状态
     * 根据传入的颜色状态值，切换相机界面的布局，以适应不同的拍摄模式
     * 此方法主要用于控制相机界面元素的可见性与启用状态，并调整相机参数
     *
     * @param mcolorState 颜色状态值，决定布局切换的模式
     */
    void Layout_Switch(int mcolorState) {
        colorState = mcolorState;
        record.setVisibility(View.GONE);
        capture.setVisibility(View.GONE);
        switch_frame.setVisibility(View.GONE);
        void_quality.setVisibility(View.GONE);
        falsh_switch.setVisibility(View.GONE);
        Choose2.setVisibility(View.GONE);
        Choose2.setEnabled(false);
        Choose.setVisibility(View.GONE);
        Choose.setEnabled(false);
        switch_camera.setEnabled(false);
        mCameraProxy.mZoom = 0;
        void_quality.setEnabled(false);
        switch_frame.setEnabled(false);
        capture.setEnabled(false);
        record.setEnabled(false);
        mTextureView.startAnimation(mTextureViewAnimation);
        mCameraProxy.handleZoom(true,false ,mCameraDevice,characteristics, previewRequestBuilder, mPreviewRequest, captureSession);

        if (colorState == 1) {
            record.setEnabled(false);
            record.setVisibility(View.VISIBLE);
            capture.setVisibility(View.GONE);
            capture.setEnabled(false);
            Title.setVisibility(View.VISIBLE);
            void_quality.setVisibility(View.VISIBLE);
            falsh_switch.setVisibility(View.GONE);
            isLayout = true;
            mCameraProxy.handleZoom(true,false,mCameraDevice, characteristics, previewRequestBuilder, mPreviewRequest, captureSession);
            if (!isRecordflag) {
                Log.d("--CameraActivity2--", "Layout_Switch: xxx");
                isRecord5 = true;
                isRecord4 = false;
            } else {
                isRecord5 = false;
                isRecord4 = true;
            }
            isCaptureing = false;
            isLayoutSwich = true;
            mFlashMode = 2;
            SwichFlash();
            if (isLayoutSwich) {
                switchCameraWithMaskAnimation();
            }

            mCameraProxy.handleZoom(true,false,mCameraDevice, characteristics, previewRequestBuilder, mPreviewRequest, captureSession);

            // startPreview();

        } else {
            capture.setEnabled(false);
            capture.setVisibility(View.VISIBLE);
            record.setEnabled(false);
            record.setVisibility(View.GONE);
            Title.setVisibility(View.VISIBLE);
            switch_frame.setVisibility(View.VISIBLE);
            falsh_switch.setVisibility(View.VISIBLE);
            Log.d("--CameraActivity2--", "Layout_Switch: " + isRecord4);
            isRecordflag = isRecord4;
            isRecord4 = false;
            isRecord5 = false;
            isCaptureing = true;
            isLayoutSwich = true;
            mFlashMode = 2;
            isLayout = false;
            falsh_switch.setEnabled(true);
            SwichFlash();
            mCameraProxy.mZoom = 0;
            mCameraProxy.handleZoom(false, false, mCameraDevice, characteristics, recordvideoRequestBuilder, mPreviewRequest, captureSession);
            if (isLayoutSwich) {
                switchCameraWithMaskAnimation();
            }
            //startPreview();
        }

    }


    /*
    TextureView监听函数
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {
        // 当SurfaceTexture可用时，打开摄像头
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture
                , int width, int height) {
            // 当TextureView可用时，打开摄像头
            openCamera(width, height);
        }

        // 当SurfaceTexture尺寸改变时，重新配置变换矩阵
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture
                , int width, int height) {
            configureTransform(width, height);
            // 每次预览更新时绘制
            //drawOnTextureView();
        }

        // 当SurfaceTexture被销毁时，释放摄像头资源
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        // 当SurfaceTexture更新时，不做任何操作
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {

        }
    };

    private void drawOnTextureView() {
        Canvas canvas = mTextureView.lockCanvas(null);
        if (canvas != null) {
            Rect rect = new Rect(275, 424, 438, 547);
            // 创建一个Paint对象用于定义矩形的绘制属性（如颜色、描边宽度等）
            Paint paint = new Paint();
            paint.setColor(Color.RED); // 设置矩形的颜色为红色
            paint.setStyle(Paint.Style.STROKE); // 设置为描边样式
            paint.setStrokeWidth(5); // 设置描边宽度为5像素

            // 绘制矩形
            canvas.drawRect(rect, paint);

            // 释放并提交绘制结果
            mTextureView.unlockCanvasAndPost(canvas);
        }
    }

    /**
     * 打开摄像头并调整纹理视图以适应屏幕大小
     * 在打开摄像头之前，初始化纹理视图并停止任何正在进行的捕获会话
     * 此方法还检查是否已授权使用摄像头权限，并在没有授权的情况下阻止摄像头的打开
     *
     * @param width  纹理视图的宽度，用于调整视图大小
     * @param height 纹理视图的高度，用于调整视图大小
     */
    private void openCamera(int width, int height) {
        initAutoFitTextureView(mTextureView, mTextureView.getWidth(), mTextureView.getHeight());
        stopCaptureSessionAsync();
        //Toast.makeText(this, "打开摄像头", Toast.LENGTH_SHORT).show();
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            // 如果用户没有授权使用摄像头，直接返回
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            // 打开摄像头
            manager.openCamera(cameraId, stateCallback, null); // ①
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // 初始化纹理视图以适应屏幕大小
    private void initAutoFitTextureView(AutoFitTextureView textureView, int width, int height) {
        // 确保 surfaces 集合已被初始化
        if (surfaces == null) {
            surfaces = new ArrayList<>();
        } else {
            surfaces.clear();
        }

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            // 获取指定摄像头的特性
            characteristics = manager.getCameraCharacteristics(cameraId);
            // 获取摄像头支持的配置属性
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            // 获取摄像头支持的最大尺寸
            assert map != null;
            // 获取支持的尺寸的最大尺寸
            List<Size> outputSizes = Arrays.asList(map.getOutputSizes(ImageFormat.JPEG));
            // 设置不同的宽高比
            double aspectRatio1x1 = 1.0; // 1:1
            double aspectRatio4x3 = 4.0 / 3.0; // 4:3
            double aspectRatio16x9 = 16.0 / 9.0;//
            double aspectRatioFull = (double) (screenHeight / (double) screenWidth);//FULL
            Log.d("--3initAutoFitTextureView--", "screenWidth:" +screenWidth+ "screenHeight:" +screenHeight);

            // 分别获取 1:1、4:3 和 FULL 的最大尺寸
            List<Size> sizes1x1 = filterSizesByAspectRatio(outputSizes, aspectRatio1x1);
            List<Size> sizes4x3 = filterSizesByAspectRatio(outputSizes, aspectRatio4x3);
            List<Size> sizes16x9 = filterSizesByAspectRatio(outputSizes, aspectRatio16x9);
            List<Size> sizesFull = filterFULLSizesByAspectRatio(outputSizes, aspectRatioFull);

            Log.d("--2initAutoFitTextureView--", "sizes1x1:" + sizes1x1);
            Log.d("--2initAutoFitTextureView--", "sizes4x3:" + sizes4x3);
            Log.d("--2initAutoFitTextureView--", "sizes16x9:" + sizes16x9);
            Log.d("--2initAutoFitTextureView--", "sizesFull:" + sizesFull);
            Size largest1x1 = sizes1x1.isEmpty() ? null : Collections.max(sizes1x1, new CompareSizesByArea());
            Size largest4x3 = sizes4x3.isEmpty() ? null : Collections.max(sizes4x3, new CompareSizesByArea());
            Size largest16x9 = sizes16x9.isEmpty() ? null : Collections.max(sizes16x9, new CompareSizesByArea());
            Size largestFull = sizesFull.isEmpty() ? null : Collections.max(sizesFull, new CompareSizesByArea());

            Log.d("--initAutoFitTextureView--", "largest1x1:" + largest1x1);
            Log.d("--initAutoFitTextureView--", "largest4x3:" + largest4x3);
            Log.d("--initAutoFitTextureView--", "largest16x9:" + largest16x9);
            Log.d("--initAutoFitTextureView--", "largestFull:" + largestFull);

            width = largest.getWidth();
            height = largest.getHeight();
            Log.d("--initAutoFitTextureView2--", "width:" + width + " height:" + height);
            previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, largest4x3, "4x3");
            mImageReader = ImageReader.newInstance(largest4x3.getWidth(), largest4x3.getHeight(), ImageFormat.JPEG, 2);
            Log.d("--initAutoFitTextureView--", "previewSize:" + previewSize.getWidth() + " height:" + previewSize.getHeight());
            // 根据选中的预览尺寸来调整预览组件（TextureView的）的长宽比
            //previewSize=new Size(1600, 720);
            if (largest.getWidth() == 20 && isCapture) {
                previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, largestFull, "FULL");
                Log.d("--initAutoFitTextureView1--", "previewSize:" + previewSize.getWidth() + " height:" + previewSize.getHeight());
                mImageReader = ImageReader.newInstance(largestFull.getWidth(), largestFull.getHeight(), ImageFormat.JPEG, 2);
                Log.d("--initAutoFitTextureView--", "isCapture");
            }
            if (largest.getWidth() == 1 && isCapture) {
                previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, largest1x1, "1:1");
                Log.d("--initAutoFitTextureView1--", "previewSize:" + previewSize.getWidth() + " height:" + previewSize.getHeight());
                mImageReader = ImageReader.newInstance(largest1x1.getWidth(), largest1x1.getHeight(), ImageFormat.JPEG, 2);
            }
            if (isRecord4) {
                previewSize = new Size(720,480);
                Log.d("--initAutoFitTextureView2--", "previewSize:" + previewSize.getWidth() + " height:" + previewSize.getHeight());
            }
            if (isRecord5) {
                if (largest16x9 != null) {
                    // previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, largest16x9,"16x9");
                    previewSize = new Size(1280, 720);
                }
                Log.d("--initAutoFitTextureView2--", "previewSize:" + previewSize.getWidth() + " height:" + previewSize.getHeight());
            }
            Position_frame(previewSize);
            // 横竖屏判断
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // 横屏s
                textureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
            } else {
                // 竖屏
                textureView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
            }
            Log.d("--initAutoFitTextureView--", "textureView.getWidth():" + textureView.getWidth() + " textureView.getHeight():" + textureView.getHeight());


        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.out.println("出现错误。");
            Log.d("--initAutoFitTextureView--", "出现错误。");
        }
        // 配置变换
        configureTransform(previewSize.getWidth(), previewSize.getHeight());
    }

    /**
     * 根据给定的宽高比，从给定的尺寸列表中筛选出所有符合该宽高比的尺寸。
     */
    private List<Size> filterSizesByAspectRatio(List<Size> outputSizes, double targetAspectRatio) {
        List<Size> filteredSizes = new ArrayList<>();

        // 定义一个容差值，允许一定的误差
        double tolerance = 0.05; // 可以根据需求调整
        boolean flg=true;
        for (Size size : outputSizes) {
            double width = size.getWidth();
            double height = size.getHeight();
            double currentAspectRatio = width / height;

            // 判断当前尺寸的宽高比是否接近目标宽高比
            if (Math.abs(currentAspectRatio - targetAspectRatio) <= tolerance) {
                filteredSizes.add(size);
                Log.d("--filterSizesByAspectRatio--", "size:" + size+" targetAspectRatio:"+targetAspectRatio);
            }
        }

        return filteredSizes;
    }

    // 选择最合适的尺寸
    private List<Size> filterFULLSizesByAspectRatio(List<Size> outputSizes, double targetAspectRatio) {
        List<Size> filteredSizes = new ArrayList<>();

        // 定义一个容差值，允许一定的误差
        double tolerance = 0.1; // 可以根据需求调整

        // 记录最接近目标宽高比的尺寸和差异
        Size closestSize = null;
        double minDifference = Double.MAX_VALUE;

        for (Size size : outputSizes) {
            double width = size.getWidth();
            double height = size.getHeight();
            double currentAspectRatio = width / height;

            // 计算当前尺寸的宽高比与目标宽高比的差异
            double difference = Math.abs(currentAspectRatio - targetAspectRatio);

            // 判断当前尺寸的宽高比是否接近目标宽高比
            if (difference <= tolerance) {
                filteredSizes.add(size);
            }

            // 更新最接近目标宽高比的尺寸
            if (difference < minDifference) {
                minDifference = difference;
                closestSize = size;
            }
        }

        // 如果没有完全匹配的尺寸，则将最接近的尺寸加入结果列表
        if (closestSize != null && filteredSizes.isEmpty()) {
            filteredSizes.add(closestSize);
        }

        return filteredSizes;
    }

    // 根据屏幕尺寸和预览尺寸，计算出最佳的预览尺寸
    private void Position_frame(Size previewSize) {
        if (largest.getWidth() == 4 || isRecord5 || isRecord4) {
            // 获取 FrameLayout.LayoutParams
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mTextureView.getLayoutParams();
            Log.d("--Position_frame--", "4/3");
            //取消居中
            layoutParams.gravity = Gravity.NO_GRAVITY;
            // 设置 topMargin 为 100dp
            layoutParams.topMargin = (int) (100 * getResources().getDisplayMetrics().density);

            // 应用新的 LayoutParams
            mTextureView.setLayoutParams(layoutParams);
        } else if ((previewSize.getHeight() / previewSize.getWidth() == 1)) {
            Log.d("--Position_frame--", "1");
            // 获取 FrameLayout.LayoutParams
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mTextureView.getLayoutParams();
            //取消居中
            layoutParams.gravity = Gravity.NO_GRAVITY;
            // 设置 topMargin 为 100dp
            layoutParams.topMargin = (int) (160 * getResources().getDisplayMetrics().density);

            // 应用新的 LayoutParams
            mTextureView.setLayoutParams(layoutParams);
        } else
        {
            Log.d("--Position_frame--", "1600/720");
            // 获取 FrameLayout.LayoutParams
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mTextureView.getLayoutParams();
            //取消居中
            layoutParams.gravity = Gravity.NO_GRAVITY;
            // 设置 topMargin 为 100dp
            layoutParams.topMargin = (int) (0 * getResources().getDisplayMetrics().density);

            // 应用新的 LayoutParams
            mTextureView.setLayoutParams(layoutParams);
        }
        if(isFlat&&(isRecord5||isRecord4))
        {
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mTextureView.getLayoutParams();

            // 设置 topMargin 为 100dp
            layoutParams.topMargin = (int) (0 * getResources().getDisplayMetrics().density);
            //居中
            layoutParams.gravity = Gravity.CENTER;

            // 应用新的 LayoutParams
            mTextureView.setLayoutParams(layoutParams);
        }
    }

    //配置变换
    private void configureTransform(int width, int height) {
        if (null == previewSize) {
            return;
        }

        // 获取手机的旋转方向
        int rotation = getWindowManager().getDefaultDisplay().getRotation();

        // 获取屏幕的长和宽
        Matrix matrix = new Matrix();
        // 获取屏幕的长和宽
        RectF viewRect = new RectF(0, 0, screenWidth, screenHeight);
        // 获取摄像头的长和宽
        RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        // 获取屏幕的长和宽
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();

        // 处理手机横屏的情况
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {

            // 调整摄像头长宽和屏幕长宽的相对位置
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            // 计算变换矩阵
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            // 计算缩放比例
            float scale = Math.max(
                    (float) screenHeight / previewSize.getHeight(),
                    (float) screenWidth / previewSize.getWidth());
            // 对变换矩阵进行缩放
            matrix.postScale(scale, scale, centerX, centerY);
            // 对变换矩阵进行旋转
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }

        // 处理手机倒置的情况
        else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }


    // 摄像头状态回调类
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        //  摄像头被打开时激发该方法
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            CameraActivity.this.mCameraDevice = cameraDevice;
            createCaptureSessionAsync();
        }

        // 摄像头断开连接时激发该方法
        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraDevice.close();
            CameraActivity.this.mCameraDevice = null;
        }

        // 打开摄像头出现错误时激发该方法
        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            CameraActivity.this.mCameraDevice = null;
            CameraActivity.this.finish();
        }
    };

    // 为Size定义一个比较器Comparator
    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // 强转为long保证不会发生溢出
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    // 选择合适的预览尺寸
    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio, String flg) {
        // 收集摄像头支持的打开预览Surface的分辨率
        List<Size> bigEnough = new ArrayList<>();

        // 宽高比
        int w = aspectRatio.getWidth();
        // 高
        int h = aspectRatio.getHeight();
        Log.d("--chooseOptimalSize--", "宽高比：" + w + ":" + h);
        // 遍历所有支持的预览尺寸
        for (Size option : choices) {
            // 宽高比必须相同，且宽高比为w:h
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
                Log.d("--chooseOptimalSize--", "支持的预览尺寸：" + option.getWidth() + ":" + option.getHeight());
            }
        }


        // 如果找到多个预览尺寸，获取其中面积第二小的。
        if (!bigEnough.isEmpty()) {
            // 使用自定义比较器按面积从小到大排序
            List<Size> sortedSizes = new ArrayList<>(bigEnough);
            Collections.sort(sortedSizes, new CompareSizesByArea());

            Log.d("--chooseOptimalSize2--", "排序后：" + sortedSizes);

            if (Objects.equals(flg, "1x1"))
            // 返回面积第二小的尺寸
            {
                Log.d("--chooseOptimalSize2--", "1/1");
                return sortedSizes.get(3);
            } else if (Objects.equals(flg, "4x3") && !isRecord4) {
                Log.d("--chooseOptimalSize2--", "4/3");
                return sortedSizes.get(7);
            } else if (Objects.equals(flg, "4x3") && isRecord4) {
                Log.d("--chooseOptimalSize2--", "4/3");
                return sortedSizes.get(4);
            } else if (Objects.equals(flg, "16x9")) {
                Log.d("--chooseOptimalSize2--", "16/9" + sortedSizes.get(3));
                return sortedSizes.get(3);
            } else if (Objects.equals(flg, "FULL")) {
                Log.d("--chooseOptimalSize2--", "FULL");
                return sortedSizes.get(2);
            } else {
                Log.d("--chooseOptimalSize2--", "1/1" + sortedSizes.get(1));
                return sortedSizes.get(1);
            }

        } else {
            System.out.println("找不到合适的预览尺寸！！！");
            Log.d("--chooseOptimalSize--", "找不到合适的预览尺寸！！！");
            // Toast.makeText(CameraActivity.this, "找不到合适的预览尺寸！！！", Toast.LENGTH_SHORT).show();
            return choices[0];
        }
    }

    // 异步创建预览会话
    private void createCaptureSessionAsync() {
        if (!isRunning) {
            isRunning = true;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (isRunning) {
                            createCaptureSession(mCameraDevice);
                        }
                    } catch (CameraAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }

    // 停止预览会话
    private void stopCaptureSessionAsync() {
        if (isRunning) {
            isRunning = false;
            handler.removeCallbacksAndMessages(null);
        }
    }

    // 创建预览会话
    private void createCaptureSession(CameraDevice cameraDevice) throws CameraAccessException {
        try {
            focusRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }

        surfaces.clear();
        // 预览Surface
        texture = mTextureView.getSurfaceTexture();
        assert texture != null;
        texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        previewSurface = new Surface(texture);
        surfaces.add(previewSurface);

        // 创建ImageReader对象(拍照)
        mImageReader.setOnImageAvailableListener(mImageReaderListener, null);
        surfaces.add(mImageReader.getSurface());

        //添加录制Surface
        initRecording();// 初始化录制
        // Toast.makeText(this, "初始化录制", Toast.LENGTH_SHORT).show();
        surfaces.add(mMediaRecorder.getSurface());

        // 创建预览请求
        previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        // 设置预览目标Surface
        previewRequestBuilder.addTarget(previewSurface);

        // 创建预览请求
        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        // 设置自动曝光
        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        // 设置自动白平衡
        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
        // 设置闪光灯
        previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
        if (!isClickFocus) {
            // 自动对焦
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
        }


        // 设置预览输出的Surface
        if (recordvideoRequestBuilder != null && colorState == 1) {
            // 设置预览请求的输出Surface
            Rect zoomRect = recordvideoRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION);
            // 设置裁剪区域
            previewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);
        }

        // 检测焦点状态
        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);
        mPreCaptureCallback = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                // 对焦状态回调
                //Log.d("onCaptureCompleted", "onCaptureCompleted: afState: " + result.get(CaptureResult.CONTROL_AF_STATE));
                //Toast.makeText(CameraActivity.this, "对焦状态回调"+result.get(CaptureResult.CONTROL_AF_STATE), Toast.LENGTH_SHORT).show();
                if (isClickFocus) {
                    try {
                        process(result);
                    } catch (CameraAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };


        try {
            cameraDevice.createCaptureSession(surfaces, PreCaptureCallback = new CameraCaptureSession.StateCallback() {
                // 预览会话已创建
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    // 预览会话已创建成功，可以开始预览
                    captureSession = session;
                    mPreviewRequest = previewRequestBuilder.build();
                    // 开启连续预览
                    // captureSession.setRepeatingRequest(previewRequestBuilder.build(), mPreCaptureCallback, null);
                    if (firstView) {
                        record.setEnabled(true);
                        capture.setEnabled(true);
                        switch_frame.setEnabled(true);
                        void_quality.setEnabled(true);
                        falsh_switch.setEnabled(true);
                        switch_camera.setEnabled(true);
                        Load.setEnabled(true);
                        Choose.setEnabled(true);
                        Choose2.setEnabled(true);
                        firstView = false;
                        focusOnPoint((double) mTextureView.getWidth() / 2, (double) mTextureView.getHeight() / 2, mTextureView.getWidth(), mTextureView.getHeight());
                    }
                    else
                    {

                        if (colorState==1) {
                            try {
                                startRecordingVideoinit();
                            } catch (CameraAccessException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        else
                        {
                            startPreview();
                        }
                    }
                    mTextureView.setEnabled(true);
                }


                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    // 预览会话创建失败
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // 处理对焦状态
    private void process(TotalCaptureResult result) throws CameraAccessException {
        if (result == null) {
            Toast.makeText(this, "Result is null", Toast.LENGTH_SHORT).show();
            return;
        }
        // 获取焦点状态
        Integer state = result.get(CaptureResult.CONTROL_AF_STATE);

        if (state == null) {
            Log.e("mAfCaptureCallback", "STATE is null");
            Toast.makeText(this, "STATE is null", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "state: " + state, Toast.LENGTH_SHORT).show();
        // 检查焦点状态
        if (Objects.equals(state, CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED)
                || Objects.equals(state, CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED)) {
            Toast.makeText(this, "对焦成功", Toast.LENGTH_SHORT).show();
            // 设置相关参数
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
            // 设置对焦模式
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 关闭自动曝光
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
            // 启动预览
            createCaptureSessionAsync();
            //openCamera(mTextureView.getWidth(), mTextureView.getHeight());
            isClickFocus = false;
        } else {
            // 对焦失败，记录日志并提示用户
            Log.w("process", "对焦失败，状态：" + state);
            Toast.makeText(this, "对焦失败，请调整相机位置或光线", Toast.LENGTH_SHORT).show();
        }
    }

    private ImageReader.OnImageAvailableListener mImageReaderListener = reader -> {
        Image image = reader.acquireLatestImage();
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        saveImageToGallery(data);
        // Start an asynchronous task to decode and display images
        new DecodeAndDisplayTask().execute(data, image);
        mPictureIv.setEnabled(true);
        image.close();
    };

    // 保存图片到相册
    private void saveImageToGallery(byte[] data) {
        String fileName = "IMG_" + System.currentTimeMillis() + ".jpg";
        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + File.separator + fileName;

        try {
            // 创建文件输出流
            FileOutputStream fos = new FileOutputStream(filePath);
            // 写入数据
            fos.write(data);
            // 关闭输出流
            fos.close();
            // 刷新媒体库
            MediaScannerConnection.scanFile(this, new String[]{filePath}, null, null);
            // 发送广播通知媒体库更新
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(filePath))));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private class DecodeAndDisplayTask extends AsyncTask<Object, Void, Bitmap> {
        private Image mImage;

        @Override
        protected Bitmap doInBackground(Object... params) {
            byte[] data = (byte[]) params[0];
            mImage = (Image) params[1];
            BitmapFactory.Options options = new BitmapFactory.Options();
            // Image compression
            options.inSampleSize = 4; // Specifies the sample rate
            if (bitmap != null) {
                bitmap.recycle();
            }
            // Get image data
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
            if (isLeftTransverse) {
                // Rotate image (if needed)
                bitmap = rotateBitmap(bitmap, -90, false, false);
                isLeftTransverse = false;
            }
            if (isRightTransverse) {
                bitmap = rotateBitmap(bitmap, 90, false, false);
                isLeftTransverse = false;
            }
            if (isinversion) {
                // Rotate image (if needed)
                bitmap = rotateBitmap(bitmap, 180, false, false);
                isinversion = false;
            }
            return bitmap;
        }


        // 旋转图片
        private Bitmap rotateBitmap(Bitmap bitmap, int angle, boolean flipHorizontal, boolean flipVertical) {
            Matrix matrix = new Matrix();
            matrix.postRotate(angle);
            if (flipHorizontal) {
                matrix.postScale(-1, 1);
            }
            if (flipVertical) {
                matrix.postScale(1, -1);
            }
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }

        //The image is decoded
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                //mPictureIv.startAnimation(btnAnimation);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        mPictureIv.startAnimation(bitmapAnimation);
                    }
                });
                mPictureIv.setImageBitmap(bitmap);

                mPictureIv.setEnabled(true);
            } else {
                Log.w("onPostExecute", "Bitmap is null.");
            }
        }
    }

    // Initialize the recording
    private void initRecording() {
        String recorderPath = null;
        // Check if the last generated file exists, and check its size
        if (Previous_recorderPath != null) {
            //  Get the last generated file
            previousFile = new File(Previous_recorderPath);
            if (previousFile.exists()) {
                Log.d("initRecording", "The old file exists, start checking the file size");
                long fileSize = previousFile.length();
                if (fileSize == 0) {
                    recorderPath = Previous_recorderPath; // 文件大小为0，继续使用旧文件
                    Log.d("initRecording", "Continue to use old files：" + Previous_recorderPath);
                } else {
                    Log.d("initRecording", "If the size of the old file is not 0, a new file will be generated");
                }
            } else {
                Log.d("initRecording", "The old file does not exist, and a new file is generated");
            }
        }

        // 生成新的文件路径
        if (recorderPath == null) {
            String fileName = "VID_" + System.currentTimeMillis() + ".mp4";
            recorderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + File.separator + fileName;
            //currentPath=recorderPath;
        }
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }

        Previous_recorderPath = recorderPath;
        // 记录日志，用于调试确认视频文件路径
        Log.e("initRecording", "视频路径：" + recorderPath);
        newroidPath = recorderPath;
        // 初始化MediaRecorder对象
        mMediaRecorder = new MediaRecorder();
        // 设置音频源为麦克风
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        // 设置视频源为相机预览画面
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        // 设置输出格式为MPEG-4
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        // 设置输出文件路径
        mMediaRecorder.setOutputFile(recorderPath);
        // 设置视频编码比特率为10 Mbps，提供较好的视频质量
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        // 设置视频帧率为30帧/秒，以保证视频流畅性
        mMediaRecorder.setVideoFrameRate(30);
        // 根据相机预览尺寸设置视频分辨率

        if (isRecord4) {
            mMediaRecorder.setVideoSize(720, 480);
        } else {
            mMediaRecorder.setVideoSize(1280, 720);
        }
        // 设置视频编码格式为H.264，提供较好的压缩效率
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        // 设置音频编码格式为AAC，提供较好的音频质量
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        // 设置录制视频的方向
        int rotation = getRotationDegrees();
        mMediaRecorder.setOrientationHint(rotation);
        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 判断是否为前置摄像头
    boolean isFrontCamera() {
        return cameraId.equals("1");
    }

    // 获取旋转角度
    private int getRotationDegrees() {
        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_90:
                rotation = 0;
                break;
            case Surface.ROTATION_180:
                rotation = 180;
                break;
            case Surface.ROTATION_270:
                rotation = 270;
                break;
            default:
                rotation = 90;
                break;
        }

        // 根据摄像头位置调整角度
        if (isFrontCamera()) {
            rotation = (rotation + 180) % 360;
        }
        return rotation;
    }

    // 设置曝光度
    private void applyExposure(float exposure) {
        exposure = 500 * (exposure / 9950) - 40;
        float finalExposure = exposure;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d("applyExposure", "applyExposure: " + finalExposure);
                if (colorState == 1 && isRecordingflg) {
                    try {
                        focusRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, (int) finalExposure);
                        captureSession.setRepeatingRequest(focusRequestBuilder.build(), null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                } else {
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, (int) finalExposure);
                    try {
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, (int) finalExposure);
                        captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 200); // 延迟 200 毫秒

    }

    public void saveCameraState(int width, int height) {
        // 获取SharedPreferences实例
        SharedPreferences preferences = getSharedPreferences("camera_state", MODE_PRIVATE);
        // 获取编辑器
        SharedPreferences.Editor editor = preferences.edit();
        // 保存画幅状态
        editor.putInt("previewWidth", width);
        editor.putInt("previewHeight", height);
        // 提交更改
        editor.apply();
    }

    public CameraState restoreCameraState() {

        // 获取SharedPreferences实例
        SharedPreferences preferences = getSharedPreferences("camera_state", MODE_PRIVATE);
        // 恢复画幅状态，默认值设为0
        int previewWidth = preferences.getInt("previewWidth", 0);
        int previewHeight = preferences.getInt("previewHeight", 0);

        // 创建并返回CameraState对象
        return new CameraState(previewWidth, previewHeight);
    }

    public static class CameraState {
        private int previewWidth;
        private int previewHeight;

        public CameraState(int previewWidth, int previewHeight) {
            this.previewWidth = previewWidth;
            this.previewHeight = previewHeight;
        }

        public int getPreviewWidth() {
            return previewWidth;
        }

        public int getPreviewHeight() {
            return previewHeight;
        }
    }

    public void focusOnPoint(double x, double y, int width, int height) {
        if (mCameraDevice == null || previewRequestBuilder == null) {
            return;
        }
        Log.d("focusOnPoint", "focusOnPoint:" + focusRequestBuilder);
        if (focusRequestBuilder == null) {
            return;
        }
        Log.d("focusOnPoint2", "surfaces:" + surfaces.toArray().length);
        focusRequestBuilder.addTarget(surfaces.get(0));
        if (isRecording) {
            focusRequestBuilder.addTarget(surfaces.get(2));
        }
        mCameraProxy.handleZoom(true, true,mCameraDevice, characteristics, focusRequestBuilder, mPreviewRequest, captureSession);
        int previewWidth = previewSize.getWidth();
        int previewHeight = previewSize.getHeight();

        Rect cropRegion = focusRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION);
        if (cropRegion == null) {
            cropRegion = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        }
        double tapAreaRatio = 0.1;
        Rect rect = new Rect();
        rect.left = clamp((int) (x - tapAreaRatio / 5 * cropRegion.width()), 0, cropRegion.width());
        rect.right = clamp((int) (x + tapAreaRatio / 5 * cropRegion.width()), 0, cropRegion.width());
        rect.top = clamp((int) (y - tapAreaRatio / 5 * cropRegion.height()), 0, cropRegion.height());
        rect.bottom = clamp((int) (y + tapAreaRatio / 5 * cropRegion.height()), 0, cropRegion.height());
        //  rect=new Rect(360-50,360+50,360-50,360+50);

        focusRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{new MeteringRectangle(rect, 1000)});
        focusRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{new MeteringRectangle(rect, 1000)});
        // previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
        focusRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
        //连续对焦
        focusRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
        //连续自动对焦
        focusRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        focusRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
        if(isRecordingflg) {
            focusRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0);
        }
        try {
            CaptureRequest.Builder finalFocusRequestBuilder = focusRequestBuilder;
            if (mCameraProxy.mPreviewRequestBuilder != null) {
                Rect zoomRect = mCameraProxy.mPreviewRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION);
                Log.d("--takePicture--", "zoomRect:" + zoomRect);
                if (zoomRect != null) {
                    finalFocusRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);
                }
            }
            captureSession.capture(focusRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    // 获取焦点状态
                    Integer state = result.get(CaptureResult.CONTROL_AF_STATE);
                    Log.d("focusOnPoint", "onCaptureCompleted: " + state);
                    // 对焦完成后的处理


                    // 恢复对焦模式
                    finalFocusRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                    mPreviewRequest = finalFocusRequestBuilder.build();
                    startPreview();
                    //获取当前曝光值
                    Log.d("--takePicture2--", "exposure:" + result.get(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION));
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, result.get(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION));

                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // 对焦回调
    private CameraCaptureSession.CaptureCallback mAfCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        // 对焦回调
        private void process(CaptureResult result) throws CameraAccessException {
            Log.d("mAfCaptureCallback", "process: ");
            if (result == null) {
                //  Toast.makeText(mActivity, "Result is null", Toast.LENGTH_SHORT).show();
                return;
            }
            // 获取焦点状态
            Integer state = result.get(CaptureResult.CONTROL_AF_STATE);
            // Log.d("mAfCaptureCallback", "process: oldstate: " + state);
            if (state == null) {
                Log.e("mAfCaptureCallback", "STATE is null");
                //  Toast.makeText(mActivity, "STATE is null", Toast.LENGTH_SHORT).show();
                return;
            }
            Log.d("mAfCaptureCallback", "oldCONTROL_AF_STATE: " + state);

            // 显示进入 Process 的提示
            //  Toast.makeText(mActivity, "进入Process", Toast.LENGTH_SHORT).show();

            // 检查焦点状态
            if (Objects.equals(state, CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED)
                    || Objects.equals(state, CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED)) {
                //Toast.makeText(this, "对焦成功", Toast.LENGTH_SHORT).show();
                // 设置相关参数

                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
                // 设置对焦模式
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                // 关闭自动曝光
                previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                // 启动预览
                startPreview();
            } else {
                // 对焦失败，记录日志并提示用户
                // Log.w(TAG, "对焦失败，状态：" + state);
                //  Toast.makeText(mActivity, "对焦失败，请调整相机位置或光线", Toast.LENGTH_SHORT).show();
                //尝试重新对焦
                retryAutoFocus();
            }
        }

        private void retryAutoFocus() throws CameraAccessException {
            // 重新触发自动对焦
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
            // 提交新的请求
            captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, null);
            captureSession.capture(previewRequestBuilder.build(), mAfCaptureCallback, null);
        }

        // ae
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            //Toast.makeText(mActivity, "--onCaptureProgressed--", Toast.LENGTH_SHORT).show();
            try {
                process(partialResult);
            } catch (CameraAccessException e) {
                throw new RuntimeException(e);
            }
        }

        // ae
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            //Toast.makeText(mActivity, "--onCaptureCompleted--", Toast.LENGTH_SHORT).show();
            try {
                process(result);
            } catch (CameraAccessException e) {
                throw new RuntimeException(e);
            }
        }
    };

    /**
     * 对焦区域坐标限制
     *
     * @param x   需要限制的坐标值
     * @param min 最小坐标值
     * @param max 最大坐标值
     * @return 限制后的坐标值
     */
    private int clamp(int x, int min, int max) {
        if (x > max) return max;
        if (x < min) return min;
        return x;
    }
}