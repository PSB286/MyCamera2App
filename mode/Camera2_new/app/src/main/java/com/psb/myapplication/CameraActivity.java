package com.psb.myapplication;

import static com.psb.myapplication.ImageUtils.getLatestThumbBitmap;

import static java.lang.Thread.sleep;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

public class CameraActivity extends AppCompatActivity implements View.OnTouchListener {

    private static CameraActivity myapp;
    private static final int DISTANCE_LIMIT = 50; // 距离阈值
    private static final int VELOCITY_THRESHOLD = 500; // 速度阈值;

    private AutoFitTextureView mTextureView = null;
    private FrameLayout mRootLayout = null;
    private static CustomViewL mCustomViewL = null;
    private GestureDetector mGestureDetector = null;
    private DisplayMetrics mDisplayMetrics = null;
    //按键定义
    private ImageView record, switch_camera, capture, mPictureIv, falsh_switch;
    private TextView switch_frame, void_quality, timerText, option1, option2, option3, option4,option5;
    private FocusSunView focusSunView;
    private LinearLayout Choose,Choose2;
    private RelativeLayout Title;
    private int screenWidth = 0;
    private int screenHeight = 0;
    private String cameraId = "0";
    //相机操作变量
    List<Surface> surfaces;
    private Size previewSize = null;
    private CameraDevice mCameraDevice = null;
    SurfaceTexture texture;
    private Handler handler;
    private ImageReader mImageReader;
    private MediaRecorder mMediaRecorder;
    private CameraCaptureSession captureSession;
    private Camera2Proxy mCameraProxy;
    private float mOldDistance;
    CaptureRequest.Builder previewRequestBuilder;
    CameraCharacteristics characteristics;
    public Context mContext;
    private boolean isRecording = false;
    String Previous_recorderPath, newroidPath;
    String recorderPath, current;
    boolean isRunning = false;
    File previousFile = null;
    private int seconds = 0;
    Runnable runnable;
    private short mFlashMode = 3;
    private CameraCaptureSession.CaptureCallback mPreCaptureCallback;
    private PopupWindow popupWindow;
    public MediaActionSound mMediaActionSound;
    public Animation btnAnimation;
    Size largest=new Size(4,3);
    boolean isCapture =  false;
    boolean isRecord4 = false;
    boolean isRecord5 = false;
    boolean isLayout = false;
    boolean isClickFocus = false;
    boolean initPreview=true;
    CameraCaptureSession.StateCallback PreCaptureCallback;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initScreen();//初始化布局
        initVariable();//初始化变量
        initCustomViewL(); // 初始化自定义View
        initRequestPermissions(); // 申请摄像头权限
        initClickListener(); // 初始化点击监听器
        initTouchListener();//初始化触摸监听器
    }

    // 初始化触摸监听器
    private void initTouchListener() {
        mTextureView.setOnTouchListener(new View.OnTouchListener() {
            private boolean isZooming = false; // 标记是否正在缩放

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int pointerCount = event.getPointerCount();

                // 双指缩放相关逻辑
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_POINTER_DOWN:
                        // 双指按下
                        isZooming = true;
                        mOldDistance = getFingerSpacing(event);
                      // Toast.makeText(getApplicationContext(), "双指按下", Toast.LENGTH_SHORT).show();
                        // 隐藏聚焦图标
                        focusSunView.setVisibility(View.INVISIBLE);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // 双指移动
                        if (isZooming) {
                            float newDistance = getFingerSpacing(event);
                            if (newDistance > mOldDistance) {
                                mCameraProxy.handleZoom(true, mCameraDevice, characteristics, previewRequestBuilder);
                            } else if (newDistance < mOldDistance) {
                                mCameraProxy.handleZoom(false, mCameraDevice, characteristics, previewRequestBuilder);
                            }
                            mOldDistance = newDistance;
                          //  Toast.makeText(getApplicationContext(), "双指移动", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                        // 双指抬起
                        isZooming = false;
                        // 显示聚焦图标
                        break;
                    default:
                        break;
                }
                // 单指点击对焦
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        if (mFlashMode == 2) {
                            StrongFlash();
                        }
                        // 单指按下
                        if (!isZooming && pointerCount == 1) {
                            // 点击对焦
                            // Toast.makeText(getApplicationContext(), "单指按下", Toast.LENGTH_SHORT).show();
                            mCameraProxy.triggerFocusAtPoint((int) event.getX(), (int) event.getY(), mTextureView.getWidth(), mTextureView.getHeight(), previewRequestBuilder, captureSession);
                           // triggerFocusAtPoint(event.getX(), event.getY(), previewSize.getWidth(), previewSize.getHeight());
                            focusSunView.setVisibility(View.VISIBLE);
                            // 设置焦点位置
                            float halfWidth = focusSunView.getWidth() / 2f;
                            float halfHeight = focusSunView.getHeight() / 4f;
                            focusSunView.setTranslationX(event.getX() - halfWidth);
                            focusSunView.setTranslationY(event.getY() + halfHeight);
                            // 开始倒计时
                            focusSunView.startCountdown(false);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        // 单指抬起
                        // 不执行任何操作
                        break;
                    default:
                        break;
                }

                // 返回true，表示自己处理触摸事件
                return true;
            }
        });
    }

    private void triggerFocusAtPoint(float x, float y, int width, int height) {
        // 计算出在屏幕坐标系下的区域
        Rect cropRegion = previewRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION);
        // 计算出在传感器坐标系下的区域
        MeteringRectangle afRegion = mCameraProxy.getAFAERegion(x, y, width, height, 1f, cropRegion);
        // ae的区域比af的稍大一点，聚焦的效果比较好
        MeteringRectangle aeRegion = mCameraProxy.getAFAERegion(x, y, width, height, 1.5f, cropRegion);
        // 设置对焦区域
        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{afRegion});
        // 设置测光区域
        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{aeRegion});
        // 设置对焦模式
        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
        // 开始对焦
        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
        // 开始预取
        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);
        isClickFocus=true;
        createCaptureSessionAsync();
       // openCamera(mTextureView.getWidth(), mTextureView.getHeight());
    }


    @Override
    public void onResume() {
        super.onResume();
        // Toast.makeText(MainActivity.this, "onResume", Toast.LENGTH_SHORT);
        // 启动后台线程，用于执行回调中的代码
        // startBackgroundThread();
        // 如果Activity是从stop/pause回来，TextureView是OK的，只需要重新开启camera就行
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            // Activity创建时，添加TextureView的监听，TextureView创建完成后就可以开启camera就行了
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        // Toast.makeText(MainActivity.this, "onPause", Toast.LENGTH_SHORT);
        // 关闭camera，关闭后台线程
        closeCamera();
        //stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.d("--onDestroy--", "onDestroy");
        File newFile = new File(Previous_recorderPath);
        if (newFile.exists()) {
            newFile.delete();
        }
        super.onStop();
    }

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
    初始化函数
     */
    // 初始化屏幕
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
        // 绑定TextureView
        mTextureView = findViewById(R.id.texture); // 绑定TextureView
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        // 绑定根布局
        mRootLayout = findViewById(R.id.root);
        //mRootLayout.setOnTouchListener(this);
        // 绑定计时器
        timerText = findViewById(R.id.timer_text);
        // 绑定自定义View
        mCustomViewL = findViewById(R.id.mCustomView);
        // 初始化点击监听器
        record = findViewById(R.id.recordvideo);
        switch_camera = findViewById(R.id.switch_camera);
        capture = findViewById(R.id.capture);
        switch_frame = findViewById(R.id.frame_switch);
        void_quality = findViewById(R.id.void_quality);
        falsh_switch = findViewById(R.id.falsh_switch);


        // 获取屏幕宽高
        mDisplayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);
        screenWidth = mDisplayMetrics.widthPixels;
        screenHeight = mDisplayMetrics.heightPixels;
        Log.d("--Screen--", "width:" + screenWidth + " height:" + screenHeight);
        //相机操作
        surfaces = new ArrayList<>();
        // 创建Handler
        handler = new Handler(Looper.getMainLooper());
        mCameraProxy = new Camera2Proxy(this);
        // 显示最近
        mPictureIv = findViewById(R.id.picture_iv);
//       // 显示最近一次拍照的图片
      //  mPictureIv.setImageBitmap(getLatestThumbBitmap(this));
        // 初始化焦点光圈
        focusSunView = findViewById(R.id.focus_sun_view);
        Previous_recorderPath = null;
        newroidPath = null;
        Choose = findViewById(R.id.Choose);
        Choose2=findViewById(R.id.Choose2);
        Title = findViewById(R.id.title);
        option1 = findViewById(R.id.option1);
        option2 = findViewById(R.id.option2);
        option3 = findViewById(R.id.option3);
        option4= findViewById(R.id.option4);
        option5 = findViewById(R.id.option5);
        //拍照声音
        mMediaActionSound = new MediaActionSound();
        //按钮动画
        btnAnimation = AnimationUtils.loadAnimation(this, R.anim.btn_anim);

    }

    public static CameraActivity getInstance() {
        return myapp;
    }


    // 初始化自定义View
    @SuppressLint("ClickableViewAccessibility")
    private void initCustomViewL() {
        String[] name = new String[]{"拍照", "视频"};
        mCustomViewL.setOnTouchListener(this);
        mCustomViewL.addIndicator(name);
        mGestureDetector = new GestureDetector(this, new MyGestureDetectorListener());
    }

    // 初始化权限
    private void initRequestPermissions() {
        // 申请摄像头权限
        requestPermissions(new String[]{
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.MODIFY_AUDIO_SETTINGS
        }, 0x123);

    }


    // 初始化点击监听器
    private void initClickListener() {

        // 录像按钮
        record.setOnClickListener(v -> {
            if (v.getId() == R.id.recordvideo) {
                if (!isRecording) {
                   // Toast.makeText(CameraActivity.this, "点击了录像按钮", Toast.LENGTH_SHORT).show();
                    startRecordingVideo();
                    //播放录像声音
                    mMediaActionSound.play(MediaActionSound.START_VIDEO_RECORDING);
                } else {
                    try {
                      //  Toast.makeText(CameraActivity.this, "停止录像按钮", Toast.LENGTH_SHORT).show();
                        stopRecordingVideo();
                        //录像结束声音
                        mMediaActionSound.play(MediaActionSound.STOP_VIDEO_RECORDING);
                       mPictureIv.setImageBitmap(getLatestThumbBitmap(this));
                       // ImageUtils.setLatestThumbBitmapAsync(mPictureIv, CameraActivity.this);
                    } catch (CameraAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        // 切换摄像头按钮
        switch_camera.setOnClickListener(v -> {
            if (v.getId() == R.id.switch_camera) {
                //Toast.makeText(CameraActivity.this, "点击了切换摄像头按钮", Toast.LENGTH_SHORT).show();
                switchCameraWithMaskAnimation();
                //SwichCamera();
            }
        });

        // 拍照按钮
        capture.setOnClickListener(v -> {
            if (v.getId() == R.id.capture) {
              //  Toast.makeText(CameraActivity.this, "点击了拍照按钮", Toast.LENGTH_SHORT).show();
                takePicture();
               // mPictureIv.setImageBitmap(getLatestThumbBitmap(this));
                setLatestThumbBitmapAsync(mPictureIv, this);
                //设置拍照动画
                captureTouchListener();
                mMediaActionSound.play(MediaActionSound.SHUTTER_CLICK); // 播放拍照声音
            }
        });

        // 切换帧按钮
        switch_frame.setOnClickListener(v -> {
            if (v.getId() == R.id.frame_switch) {
                //Toast.makeText(CameraActivity.this, "点击了切换帧按钮", Toast.LENGTH_SHORT).show();
                SwichFrame();
            }
        });

        // 切换质量按钮
        void_quality.setOnClickListener(v -> {
            if (v.getId() == R.id.void_quality) {
                //Toast.makeText(CameraActivity.this, "点击了切换质量按钮", Toast.LENGTH_SHORT).show();
                SwichQuality();
            }
        });

        // 图片按钮
        mPictureIv.setOnClickListener(v -> {
            if (v.getId() == R.id.picture_iv) {
                // 创建意图并启动
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(ImageUtils.imageUri, "image/*");
                startActivity(intent);
            }
        });

        // 闪光灯按钮
        falsh_switch.setOnClickListener(v -> {
            if (v.getId() == R.id.falsh_switch) {
                //Toast.makeText(CameraActivity.this, "点击了切换闪光灯按钮", Toast.LENGTH_SHORT).show();
                SwichFlash();
            }
        });

        option1.setOnClickListener(v -> {
            if (v.getId() == R.id.option1) {
                switch_frame.setText(option1.getText().toString());
                Title.setVisibility(View.VISIBLE);
                Choose.setVisibility(View.GONE);
                largest = new Size(1,1);
                openCamera(mTextureView.getWidth(), mTextureView.getHeight());
            }
        });

        option2.setOnClickListener(v -> {
            if (v.getId() == R.id.option2) {
                switch_frame.setText(option2.getText().toString());
                Title.setVisibility(View.VISIBLE);
                Choose.setVisibility(View.GONE);
                largest = new Size(4,3);
                openCamera(mTextureView.getWidth(), mTextureView.getHeight());
            }
        });

        option3.setOnClickListener(v -> {
            if (v.getId() == R.id.option3) {
                Log.d("--option3--", "option3");
                switch_frame.setText(option3.getText().toString());
                Choose.setVisibility(View.GONE);
                Title.setVisibility(View.VISIBLE);
                Log.d("--option3--", largest.toString());
                largest = new Size(16,9);
                isCapture=true;
                Log.d("--option3--", largest.toString());
                openCamera(mTextureView.getWidth(), mTextureView.getHeight());
            }
        });

        option4.setOnClickListener(v -> {
           if (v.getId() == R.id.option4)
           {
               void_quality.setText(option4.getText().toString());
               Choose2.setVisibility(View.GONE);
               Title.setVisibility(View.VISIBLE);
               isRecord4=true;
               openCamera(mTextureView.getWidth(), mTextureView.getHeight());
           }
        });

        option5.setOnClickListener(v -> {
            if (v.getId() == R.id.option5)
            {
                void_quality.setText(option5.getText().toString());
                Choose2.setVisibility(View.GONE);
                Title.setVisibility(View.VISIBLE);
                isRecord5=true;
                openCamera(mTextureView.getWidth(), mTextureView.getHeight());
            }
        });


    }

    private void SwichQuality() {
        Title.setVisibility(View.GONE);
        Choose2.setVisibility(View.VISIBLE);
    }

    // 拍照动画
    @SuppressLint("ClickableViewAccessibility")
    private void captureTouchListener() {
        capture.setOnTouchListener(new View.OnTouchListener() {
            private boolean isZooming = false; // 标记是否正在缩放

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    // 按下
                    case MotionEvent.ACTION_DOWN:
                        capture.startAnimation(btnAnimation); //拍照按钮动画
                }
                return false;
            }
            }
        );
    }

    public static void setLatestThumbBitmapAsync(final ImageView imageView, final Context context) {
        // 创建一个 Handler，用于在主线程中更新 UI
        final Handler handler = new Handler(Looper.getMainLooper());

        // 创建一个新的 Runnable，用于异步加载 Bitmap 并更新 UI
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // 在子线程中执行耗时操作
                try {
                    sleep(600);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                Bitmap bitmap = getLatestThumbBitmap(context);
                // 使用 post 方法在主线程中更新 ImageView
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(bitmap);
                    }
                });
            }
        };

        // 启动新线程执行 Runnable
        new Thread(runnable).start();
    }

    // 切换帧
    private void SwichFrame() {
        Title.setVisibility(View.GONE);
        Choose.setVisibility(View.VISIBLE);
    }

    private void SwichFlash() {

        switch (mFlashMode) {
            case 0:
                mFlashMode = 1;
                falsh_switch.setImageResource(R.drawable.falshlightaout);
                // 设置闪光灯模式为自动
                previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
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
                falsh_switch.setImageResource(R.drawable.falshlightcopen);

                break;
            case 2:
                mFlashMode = 3;
                falsh_switch.setImageResource(R.drawable.falshlightclose);
                // 设置闪光灯模式为自动
                previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);

                try {
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
                falsh_switch.setImageResource(R.drawable.flashlight);
                // 设置闪光灯模式为常亮
                previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
                try {
                    captureSession.setRepeatingRequest(
                            previewRequestBuilder.build(),
                            mPreCaptureCallback, null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
        }
    }

    private void StrongFlash() {
        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
        try {
            captureSession.setRepeatingRequest(
                    previewRequestBuilder.build(),
                    mPreCaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void setFlashMode(int mode) {
        if (previewRequestBuilder == null || captureSession == null) {
            // Log.e(TAG, "Capture request builder or capture session is not initialized.");
            return;
        }

        try {
            previewRequestBuilder.set(CaptureRequest.FLASH_MODE, mode);
            // 更新 CaptureSession
            captureSession.setRepeatingRequest(previewRequestBuilder.build(), null /* Session state callback */, null /* Handler */);
        } catch (CameraAccessException e) {
            // Log.e(TAG, "Camera access exception while setting flash mode.", e);
        }
    }

    private void stopRecordingVideo() throws CameraAccessException {
        try {
            mMediaRecorder.stop();
        } catch (IllegalStateException e) {
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
        isRecording = false;
        // 清空 surfaces 集合
        surfaces.clear();

        timerText.setVisibility(View.GONE);
        switch_camera.setVisibility(View.VISIBLE);
        mPictureIv.setVisibility(View.VISIBLE);
        falsh_switch.setVisibility(View.VISIBLE);
        void_quality.setVisibility(View.VISIBLE);
        mCustomViewL.setVisibility(View.VISIBLE);

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
    }

    private void startRecordingVideo() {
        try {
            mMediaRecorder.start();
            // 更新预览请求
            CaptureRequest.Builder recordRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            recordRequestBuilder.addTarget(mMediaRecorder.getSurface());
            // 创建一个预览Surface
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            //
            Surface previewSurface = new Surface(texture);
            // 将预览Surface添加到预览请求中
            recordRequestBuilder.addTarget(previewSurface); // 保留预览Surface
            // 设置自动对焦和自动曝光模式
            recordRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
            // 设置自动曝光模式
            recordRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            // 更新捕获会话
            captureSession.setRepeatingRequest(recordRequestBuilder.build(), null, null);
            isRecording = true;

            // private ImageView record, switch_camera, capture,mPictureIv,falsh_switch;
            // private TextView switch_frame,void_quality,timerText;
            timerText.setVisibility(View.VISIBLE);
            switch_camera.setVisibility(View.GONE);
            mPictureIv.setVisibility(View.GONE);
            falsh_switch.setVisibility(View.GONE);
            switch_frame.setVisibility(View.GONE);
            void_quality.setVisibility(View.GONE);
            mCustomViewL.setVisibility(View.GONE);
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

    // 启动计时器
    private void startTimer() {
        runnable = new Runnable() {
            @Override
            public void run() {
                seconds++;
                @SuppressLint("DefaultLocale") String timeString = String.format("%02d:%02d", seconds / 60, seconds % 60);
                timerText.setText(timeString);
                handler.postDelayed(this, 1000);
            }
        };
        // 开始计时
        handler.post(runnable);
    }

    private void switchCameraWithMaskAnimation() {
        if (mCameraDevice != null) {
            // 创建一个蒙版视图
            View maskView = new View(this);
            maskView.setBackgroundColor(Color.BLACK); // 设置背景色为黑色
            maskView.setAlpha(0f); // 初始透明度为0

            int width = mTextureView.getWidth();
            int height = mTextureView.getHeight();

            // 设置蒙版视图的布局参数
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
            params.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, getResources().getDisplayMetrics());

            mRootLayout.addView(maskView, params);

            // 添加动画效果
            ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(150); // 动画持续时间
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float alpha = (float) animation.getAnimatedValue();
                    maskView.setAlpha(alpha);
                }
            });
            animator.start();
            // 在动画结束后关闭相机
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    //maskViewflg = 1;
                    SwichCamera(maskView);
                    // Toast.makeText(MainActivity.this, "动画结束", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void SwichCamera(View maskView) {
        // 切换摄像头
        closeCamera();
        cameraId = "1".equals(cameraId) ? "0" : "1";
        // 清空 surfaces 集合
        surfaces.clear();
        openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        if(Objects.equals(cameraId, "1")) {
            falsh_switch.setVisibility(View.GONE);
        }
        else {
            falsh_switch.setVisibility(View.VISIBLE);
        }
        //开启一个新的线线程实现移除蒙版，并且延迟600毫秒，等待摄像头切换成功
        // 创建 Handler
        Handler handler = new Handler();
        // 创建 Runnable
        Runnable removeMaskRunnable = new Runnable() {
            @Override
            public void run() {
                mRootLayout.removeView(maskView); // 移除蒙版视图
            }
        };
// 执行延迟任务
        handler.postDelayed(removeMaskRunnable, 870); // 延迟 1000 毫秒（1 秒）
    }

    private void closeCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }

    }


    // 异步保存图片
    private class ImageSaveTask extends AsyncTask<Image, Void, Bitmap> {
        // 执行耗时操作
        @Override
        protected Bitmap doInBackground(Image... images) {
            ByteBuffer buffer = images[0].getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            if (mCameraProxy.isFrontCamera()) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                // 前置摄像头需要左右镜像
                Bitmap rotateBitmap = ImageUtils.rotateBitmap(bitmap, 0, true, true);
                ImageUtils.saveBitmap(rotateBitmap);
                rotateBitmap.recycle();
            } else {
                ImageUtils.saveImage(bytes);
            }
            images[0].close();
            return getLatestThumbBitmap(getInstance());
        }
    }

    // 方向
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    // 拍照
    private void takePicture(ImageReader.OnImageAvailableListener onImageAvailableListener) {
        try {

            mImageReader.setOnImageAvailableListener(onImageAvailableListener, null);
            // 创建捕获请求
            CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            // 设置预览输出的Surface
            captureBuilder.addTarget(mImageReader.getSurface());
            // 设置自动对焦模式
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 自动对焦
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            // 自动曝光
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            // 设置照片的方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            // 根据设备方向计算设置照片的方向
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            // 捕获一帧图像
            captureSession.capture(captureBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

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
            // 自动曝光
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            // 获取设备方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            //int rotation=getRotationDegrees();
            if(isFrontCamera())
            {
                rotation = (rotation+180)%360;
            }
            Log.d("--CameraActivity--", "rotation:" + rotation);
            // 根据设备方向计算设置照片的方向
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, rotation+90);
            // 捕获一帧图像
            captureSession.capture(captureBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }




    /*
    滑动事件及回调函数
    */
    private static class MyGestureDetectorListener implements GestureDetector.OnGestureListener {

        //
        @Override
        public boolean onDown(MotionEvent e) {
            Log.d("--CameraActivity--", "onDown");

            return true;
        }

        //
        @Override
        public void onShowPress(MotionEvent e) {
            // 不做任何操作
        }

        //
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

        //
        @Override
        public void onLongPress(@NonNull MotionEvent e) {
            // 不做任何操作
        }

        @Override
        public boolean onFling(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    void Layout_Switch(int colorState) {
        record.setVisibility(View.GONE);
        capture.setVisibility(View.GONE);
        switch_frame.setVisibility(View.GONE);
        void_quality.setVisibility(View.GONE);
        falsh_switch.setVisibility(View.GONE);
        Choose2.setVisibility(View.GONE);
        Choose.setVisibility(View.GONE);
        if (colorState == 1) {
            record.setVisibility(View.VISIBLE);
            Title.setVisibility(View.VISIBLE);
            void_quality.setVisibility(View.VISIBLE);
            falsh_switch.setVisibility(View.GONE);
            isLayout=true;
            previewSize=new Size(1280,720);
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            capture.setVisibility(View.VISIBLE);
            Title.setVisibility(View.VISIBLE);
            switch_frame.setVisibility(View.VISIBLE);
            falsh_switch.setVisibility(View.VISIBLE);
            previewSize=new Size(1280,720);
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        }

    }


    /*
    TextureView监听函数
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {
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
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }
    };

    /*
    操作相机函数
     */
    //打开相机
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
            // 获取所有支持的尺寸
            //Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());

            width = largest.getWidth();
            height = largest.getHeight();
            Log.d("--initAutoFitTextureView--", "width:" + width+" height:"+height);
            previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, largest);
            Log.d("--initAutoFitTextureView--", "previewSize:" + previewSize.getWidth()+" height:"+previewSize.getHeight());
            // 根据选中的预览尺寸来调整预览组件（TextureView的）的长宽比
            //previewSize=new Size(1600, 720);
            if(width==16&&isCapture) {
                previewSize=new Size(1600, 720);
                Log.d("--initAutoFitTextureView--", "isCapture");
            }
            if(isRecord4)
            {
                previewSize = new Size(960,720);
                isRecord4=false;
            }
            if(isRecord5)
            {
                previewSize = new Size(1280,720);
                isRecord5=false;
            }
            if(isLayout)
            {
                previewSize = new Size(1280,720);
                isLayout=false;
            }
            Position_frame(previewSize);
            // 横竖屏判断
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // 横屏
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

    private void Position_frame(Size previewSize) {
        if((previewSize.getWidth()==1600)) {

            Log.d("--Position_frame--", "1600/720");
            // 获取 FrameLayout.LayoutParams
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mTextureView.getLayoutParams();

            // 设置 topMargin 为 100dp
            int margin = (int) (0 * getResources().getDisplayMetrics().density);
            layoutParams.topMargin = margin;

            // 应用新的 LayoutParams
            mTextureView.setLayoutParams(layoutParams);
        }
        else if((previewSize.getHeight()/previewSize.getWidth()== 1))
        {
            // 获取 FrameLayout.LayoutParams
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mTextureView.getLayoutParams();

            // 设置 topMargin 为 100dp
            int margin = (int) (160 * getResources().getDisplayMetrics().density);
            layoutParams.topMargin = margin;

            // 应用新的 LayoutParams
            mTextureView.setLayoutParams(layoutParams);
        }
        else {
            // 获取 FrameLayout.LayoutParams
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mTextureView.getLayoutParams();
            Log.d("--Position_frame--", "4/3");
            // 设置 topMargin 为 100dp
            int margin = (int) (100 * getResources().getDisplayMetrics().density);
            layoutParams.topMargin = margin;

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
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) screenHeight / previewSize.getHeight(),
                    (float) screenWidth / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
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

    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
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

            if(w==1)
            // 返回面积第二小的尺寸
            {
                Log.d("--chooseOptimalSize--", "1/1");
                return sortedSizes.get(3);
            }
            if(w==4)
            {
                Log.d("--chooseOptimalSize--", "4/3");
                return sortedSizes.get(7);
            }
            if(w==16)
            {
                Log.d("--chooseOptimalSize--", "16/9");
                return sortedSizes.get(0);
            }
            else
            {
                Log.d("--chooseOptimalSize--", "1/1");
                return sortedSizes.get(0);
            }
        } else {
            System.out.println("找不到合适的预览尺寸！！！");
            Log.d("--chooseOptimalSize--", "找不到合适的预览尺寸！！！");
            // Toast.makeText(CameraActivity.this, "找不到合适的预览尺寸！！！", Toast.LENGTH_SHORT).show();
            return choices[0];
        }
    }


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

    private void stopCaptureSessionAsync() {
        if (isRunning) {
            isRunning = false;
            handler.removeCallbacksAndMessages(null);
        }
    }

    private void createCaptureSession(CameraDevice cameraDevice) throws CameraAccessException {
        // 预览Surface
        texture = mTextureView.getSurfaceTexture();
        assert texture != null;

        texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        Surface previewSurface = new Surface(texture);
        surfaces.add(previewSurface);

        // 创建ImageReader对象(拍照)
        mImageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.JPEG, 1);
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
            // 闪光灯
            previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
            if (!isClickFocus) {
                // 自动对焦
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            }
            // 检测焦点状态
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);

        mPreCaptureCallback=new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                // 对焦状态回调
                //Log.d("onCaptureCompleted", "onCaptureCompleted: afState: " + result.get(CaptureResult.CONTROL_AF_STATE));
                //Toast.makeText(CameraActivity.this, "对焦状态回调"+result.get(CaptureResult.CONTROL_AF_STATE), Toast.LENGTH_SHORT).show();
               if(isClickFocus)
               {
                   try {
                       process(result);
                   } catch (CameraAccessException e) {
                       throw new RuntimeException(e);
                   }
               }
            }
        };


        try {
            cameraDevice.createCaptureSession(surfaces,  PreCaptureCallback=new CameraCaptureSession.StateCallback() {
                // 预览会话已创建
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        // 预览会话已创建成功，可以开始预览
                        captureSession = session;

                        // 开启连续预览
                        captureSession.setRepeatingRequest(previewRequestBuilder.build(), mPreCaptureCallback, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
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
            isClickFocus=false;
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
        image.close();
    };

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

    private void initRecording() {

        String recorderPath = null;
        // 检查上次生成的文件是否存在，并检查其大小
        if (Previous_recorderPath != null) {
            // 获取上次生成的文件
            previousFile = new File(Previous_recorderPath);
            if (previousFile.exists()) {
                Log.e("initRecording", "旧文件存在，开始检查文件大小");
                long fileSize = previousFile.length();
                if (fileSize == 0) {
                    recorderPath = Previous_recorderPath; // 文件大小为0，继续使用旧文件
                    Log.e("initRecording", "继续使用旧文件：" + Previous_recorderPath);
                } else {
                    Log.e("initRecording", "旧文件大小不为0，生成新文件");
                }
            } else {
                Log.e("initRecording", "旧文件不存在，生成新文件");
            }
        }

        // 生成新的文件路径
        if (recorderPath == null) {
            String fileName = "VID_" + System.currentTimeMillis() + ".mp4";
            recorderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + File.separator + fileName;
            //currentPath=recorderPath;
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
        mMediaRecorder.setVideoSize(previewSize.getWidth(), previewSize.getHeight());
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

    boolean isFrontCamera() {
        return cameraId.equals("1");
    }


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
}