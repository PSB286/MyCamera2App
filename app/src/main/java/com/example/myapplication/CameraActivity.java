package com.example.myapplication;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
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

public class CameraActivity extends AppCompatActivity implements View.OnTouchListener{
    AutoFitTextureView textureView;
    List<Surface> surfaces;
    private CameraDevice mCameraDevice;
    // 摄像头ID（通常0代表后置摄像头，1代表前置摄像头）
    private String cameraId = "0";
    // 预览尺寸
    private Size previewSize;
    private Size previewSize_capturebuff;
    private Size previewSize_videobuff;
    //
    FrameLayout rootLayout;
    //按键定义
    private ImageButton record, stop, switch_camera, capture,switch_frame,void_quality;
    //
    private int initPreviewSize=0;
    // 捕获会话
    private CameraCaptureSession captureSession;
    // 用于捕获照片的ImageReader
    private ImageReader mImageReader;
    // 自定义View
    private com.psb.myapplication_huan.CustomViewL mCustomViewL;
    // 自定义View的参数
    private String[] name = new String[]{"视频", "拍照"};
    // 手势监听器
    private GestureDetector mGestureDetector;
    private int layoutFlag=0;
    // 捕获标志
    private int captureFlag=0;
    private int videoFlags = 0;

    // TextureView的纹理
    SurfaceTexture texture;
    // 预览的Surface
    Surface previewSurface;
    // ImageReader的监听器
    private ImageReader.OnImageAvailableListener mImageReaderListener = reader -> {
        Image image = reader.acquireLatestImage();
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        saveImageToGallery(data);
        image.close();
    };
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    private Handler handler = new Handler(Looper.getMainLooper());

    private void createCaptureSessionAsync(final CameraDevice cameraDevice) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                createCaptureSession(cameraDevice);
            }
        });
    }
    private MediaRecorder mMediaRecorder;
    String recorderPath;

    // 摄像头状态回调
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
            //  摄像头被打开时激发该方法
            @Override
            public void onOpened(@NonNull CameraDevice cameraDevice) {
                CameraActivity.this.mCameraDevice = cameraDevice;
                Toast.makeText(CameraActivity.this, "摄像头已打开", Toast.LENGTH_SHORT).show();
                // 开始预览
                createCaptureSession(cameraDevice);
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

    // TextureView的监听器
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture
                , int width, int height) {
            // 当TextureView可用时，打开摄像头
            openCamera(width, height);
        }

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


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        // 设置全屏模式
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
        textureView = findViewById(R.id.texture);// 绑定TextureView
        rootLayout = findViewById(R.id.root);
        mCustomViewL = (com.psb.myapplication_huan.CustomViewL) findViewById(R.id.mCustomView);
        initCustomViewL();// 初始化自定义View
        initRequestPermissions();// 申请摄像头权限
        initClickListener();// 初始化点击监听器
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initCustomViewL() {
        mCustomViewL.setOnTouchListener((View.OnTouchListener) this); // 直接将 OnTouchListener 应用于 CustomViewL
        mCustomViewL.addIndicator(name);
        mGestureDetector = new GestureDetector(this, new MyGestureDetectorListener());
    }

    private class MyGestureDetectorListener implements GestureDetector.OnGestureListener {

        private static final int distanceLimit = 50; // 距离阈值
        private static final int velocityThreshold = 500; // 速度阈值

        @Override
        public boolean onDown(MotionEvent e) {
            Log.d("onDown", "MyGestureDetectorListener onDown");
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            // 不做任何操作
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            final float deltaX = e2.getX() - e1.getX();
            final float deltaY = e2.getY() - e1.getY();

            if (Math.abs(deltaY) < Math.abs(deltaX)) { // 确保水平滑动
                if (deltaX > distanceLimit) {
                        Layout_Switch(mCustomViewL.getIndex());
                        Log.d("切换", "切换到布局1"+mCustomViewL.scrollRight());
                } else if (deltaX < -distanceLimit) {
                        Layout_Switch(mCustomViewL.getIndex());
                        Log.d("切换", "切换到布局2"+mCustomViewL.scrollLeft());
                }
            }

            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            // 不做任何操作
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Toast.makeText(MainActivity.this, "onResume", Toast.LENGTH_SHORT);
        // 启动后台线程，用于执行回调中的代码
       // startBackgroundThread();
        // 如果Activity是从stop/pause回来，TextureView是OK的，只需要重新开启camera就行
        if (textureView.isAvailable()) {
            openCamera(textureView.getWidth(), textureView.getHeight());
        } else {
            // Activity创建时，添加TextureView的监听，TextureView创建完成后就可以开启camera就行了
            textureView.setSurfaceTextureListener(mSurfaceTextureListener);
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

    /**
     * 处理触摸事件的回调方法
     *
     * @param v 触发触摸事件的视图
     * @param event 触摸事件
     * @return 返回是否消费该触摸事件
     *
     * 该方法通过调用mGestureDetector的onTouchEvent方法来处理触摸事件，将事件传递给手势检测器进行处理
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    private void initRequestPermissions() {
        requestPermissions(new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, 0x123);
    }

    private void Layout_Switch(int i) {
        Log.d("Layout_Switch", "Layout_Switch："+i);
        record.setVisibility(View.GONE);
        stop.setVisibility(View.GONE);
        capture.setVisibility(View.GONE);
        findViewById(R.id.capture_top).setVisibility(View.GONE);
        findViewById(R.id.viode_top).setVisibility(View.GONE);
        if(i==1)
        {

            capture.setVisibility(View.VISIBLE);
            findViewById(R.id.capture_top).setVisibility(View.VISIBLE);
            //previewSize=previewSize_capturebuff;
           // openCamera(textureView.getWidth(),textureView.getHeight());
        }
        else {

            record.setVisibility(View.VISIBLE);
            stop.setVisibility(View.VISIBLE);
            findViewById(R.id.viode_top).setVisibility(View.VISIBLE);
                //previewSize = new Size(1280, 720);
               // openCamera(textureView.getWidth(), textureView.getHeight());
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
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            // 获取摄像头支持的配置属性
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.
                    SCALER_STREAM_CONFIGURATION_MAP);
            // 获取摄像头支持的最大尺寸
            assert map != null;
            Size largest = Collections.max(
                    Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                    new CompareSizesByArea());
            if(initPreviewSize==0) {
                // 获取最佳的预览尺寸
                Toast.makeText(CameraActivity.this, "initPreviewSize", Toast.LENGTH_SHORT).show();
                previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, largest);
                Log.d("xxx", "--initAutoFitTextureView--");
                initPreviewSize=1;
            }
            Toast.makeText(CameraActivity.this, "高"+previewSize.getHeight()+"宽"+previewSize.getWidth(), Toast.LENGTH_SHORT).show();
            previewSize=new Size(1600,720);
            //textureView.setLayoutParams(new ViewGroup.LayoutParams(previewSize.getWidth(), previewSize.getHeight()));
            Position_frame(previewSize);

            // 应用新的 LayoutParams
            Log.d("--initAutoFitTextureView--", "高" + previewSize.getHeight()+"宽"+previewSize.getWidth());

            // 根据选中的预览尺寸来调整预览组件（TextureView的）的长宽比
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                textureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());

            } else {
                textureView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
            }
            //previewSize=new Size(2944,2944);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.out.println("出现错误。");
            Log.d("--initAutoFitTextureView--", "出现错误。");
        }
        configureTransform(width, height);
    }

    private void  Position_frame(Size previewSize){
        if((previewSize.getHeight()==720)&&(previewSize.getWidth()==1600)) {
            // 获取 FrameLayout.LayoutParams
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) textureView.getLayoutParams();

            // 设置 topMargin 为 100dp
            int margin = (int) (0 * getResources().getDisplayMetrics().density);
            layoutParams.topMargin = margin;

            // 应用新的 LayoutParams
            textureView.setLayoutParams(layoutParams);
        }
        else if((previewSize.getHeight()==720)&&(previewSize.getWidth()==720))
        {
            // 获取 FrameLayout.LayoutParams
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) textureView.getLayoutParams();

            // 设置 topMargin 为 100dp
            int margin = (int) (160 * getResources().getDisplayMetrics().density);
            layoutParams.topMargin = margin;

            // 应用新的 LayoutParams
            textureView.setLayoutParams(layoutParams);
        }
        else {
            // 获取 FrameLayout.LayoutParams
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) textureView.getLayoutParams();

            // 设置 topMargin 为 100dp
            int margin = (int) (100 * getResources().getDisplayMetrics().density);
            layoutParams.topMargin = margin;

            // 应用新的 LayoutParams
            textureView.setLayoutParams(layoutParams);
        }
    }

    // 根据手机的旋转方向确定预览图像的方向
    private void configureTransform(int viewWidth, int viewHeight) {

        Log.d("--configureTransform--", "宽"+viewWidth+"高"+viewHeight);
        if (null == previewSize) {
            return;
        }
        // 获取手机的旋转方向
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        // 处理手机横屏的情况
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / previewSize.getHeight(),
                    (float) viewWidth / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        // 处理手机倒置的情况
        else if (Surface.ROTATION_180 == rotation)
        {
            matrix.postRotate(180, centerX, centerY);
        }
        textureView.setTransform(matrix);
    }

    private void initClickListener() {
        record = findViewById(R.id.record);
        stop = findViewById(R.id.stop);
        switch_camera = findViewById(R.id.switch_camera);
        capture = findViewById(R.id.capture);
        switch_frame=findViewById(R.id.frame_switch);
        void_quality=findViewById(R.id.void_quality);
        stop.setEnabled(false);

        record.setOnClickListener(v -> {
            if (v.getId() == R.id.record) {
                record.setEnabled(true);
                Toast.makeText(CameraActivity.this, "点击了录像按钮", Toast.LENGTH_SHORT).show();
                startRecordingVideo();
                stop.setEnabled(true);
                record.setEnabled(false);
            }
        });

        stop.setOnClickListener(v -> {
            if (v.getId() == R.id.stop) {
                stop.setEnabled(true);
                Toast.makeText(CameraActivity.this, "点击了停止按钮", Toast.LENGTH_SHORT).show();
                stopRecordingVideo();
                record.setEnabled(true);
                stop.setEnabled(false);
            }
        });

        switch_camera.setOnClickListener(v -> {
            if (v.getId() == R.id.switch_camera) {
                Toast.makeText(CameraActivity.this, "点击了切换摄像头按钮", Toast.LENGTH_SHORT).show();
                switchCameraWithMaskAnimation();
                //SwichCamera();
            }
        });

        capture.setOnClickListener(v -> {
            if (v.getId() == R.id.capture) {
                Toast.makeText(CameraActivity.this, "点击了拍照按钮", Toast.LENGTH_SHORT).show();
                takePicture();
            }
        });

        switch_frame.setOnClickListener(v -> {
            if (v.getId() == R.id.frame_switch) {
                Toast.makeText(CameraActivity.this, "点击了切换帧按钮", Toast.LENGTH_SHORT).show();
                SwichFrame();
            }
        });

        void_quality.setOnClickListener(v -> {
            if (v.getId() == R.id.void_quality) {
                Toast.makeText(CameraActivity.this, "点击了切换质量按钮", Toast.LENGTH_SHORT).show();
                SwichQuality();
            }
        });
    }

    private void SwichQuality() {
        if((previewSize.getWidth()==1280)&&(previewSize.getHeight()==720))
        {
            previewSize=new Size(960,720);
            openCamera(previewSize.getWidth(),previewSize.getHeight());
            //Log.d("SwichQuality", "切换成功");
        }
        else if((previewSize.getWidth()==960)&&(previewSize.getHeight()==720)) {
            previewSize=new Size(1280,720);
            openCamera(previewSize.getWidth(),previewSize.getHeight());
            //Log.d("SwichQuality", "切换成功"+"宽"+previewSize.getWidth()+"高"+previewSize.getHeight());
        }
    }

    private void SwichFrame()
    {
        if((previewSize.getWidth()==1600)&&(previewSize.getHeight()==720))
        {
            previewSize=new Size(720,720);
            //Toast.makeText(CameraActivity.this, "1高"+previewSize.getHeight()+"宽"+previewSize.getWidth(), Toast.LENGTH_SHORT).show();
            openCamera(previewSize.getWidth(),previewSize.getHeight());
        }
        else if((previewSize.getWidth()==960)&&(previewSize.getHeight()==720))
        {
            previewSize=new Size(1600,720);
            //Toast.makeText(CameraActivity.this, "2高"+previewSize.getHeight()+"宽"+previewSize.getWidth(), Toast.LENGTH_SHORT).show();
            openCamera(previewSize.getWidth(),previewSize.getHeight());
        }
        else {
            previewSize=new Size(960,720);
            //Toast.makeText(CameraActivity.this, "4高"+previewSize.getHeight()+"宽"+previewSize.getWidth(), Toast.LENGTH_SHORT).show();
            openCamera(previewSize.getWidth(),previewSize.getHeight());
        }
    }

    private void switchCameraWithMaskAnimation() {
        if (mCameraDevice != null) {
            // 创建一个蒙版视图
            View maskView = new View(this);
            maskView.setBackgroundColor(Color.BLACK); // 设置背景色为黑色
            maskView.setAlpha(0f); // 初始透明度为0

            int width =textureView.getWidth();
            int height =textureView.getHeight();

            // 设置蒙版视图的布局参数
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    width,
                    height
            );
            rootLayout.addView(maskView, params);

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

    private void SwichCamera() {
        // 切换摄像头
        closeCamera();
        cameraId = "1".equals(cameraId) ? "0" : "1";
        // 清空 surfaces 集合
        surfaces.clear();
        openCamera(textureView.getWidth(), textureView.getHeight());
    }
    private void SwichCamera(View maskView ) {
        // 切换摄像头
        closeCamera();
        cameraId = "1".equals(cameraId) ? "0" : "1";
        // 清空 surfaces 集合
        surfaces.clear();
        openCamera(textureView.getWidth(), textureView.getHeight());
        //开启一个新的线线程实现移除蒙版，并且延迟600毫秒，等待摄像头切换成功
        // 创建 Handler
        Handler handler = new Handler();
        // 创建 Runnable
        Runnable removeMaskRunnable = new Runnable() {
            @Override
            public void run() {
                rootLayout.removeView(maskView); // 移除蒙版视图
            }
        };

// 执行延迟任务
        handler.postDelayed(removeMaskRunnable, 650); // 延迟 1000 毫秒（1 秒）
    }

    private void openCamera(int width, int height) {
        //setUpCameraOutputs(width, height);
        initAutoFitTextureView(textureView,textureView.getWidth(), textureView.getHeight());
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            // 如果用户没有授权使用摄像头，直接返回
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            // 打开摄像头
            manager.openCamera(cameraId, stateCallback, null); // ①
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }


    private void closeCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }

    }

    private void takePicture() {
        try {
            CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());
            // 设置自动对焦模式
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            // 根据设备方向计算设置照片的方向
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            captureSession.capture(captureBuilder.build(), null, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startRecordingVideo() {
        try {

            mMediaRecorder.start();
            // 更新预览请求
            CaptureRequest.Builder recordRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            recordRequestBuilder.addTarget(mMediaRecorder.getSurface());

            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(texture);
            recordRequestBuilder.addTarget(previewSurface); // 保留预览Surface
            recordRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
            recordRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            captureSession.setRepeatingRequest(recordRequestBuilder.build(), null, null);
            // isRecording = true;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void stopRecordingVideo() {
        try {
            mMediaRecorder.stop();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } finally {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        // isRecording = false;

        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        addToGallery(recorderPath);

        // 清空 surfaces 集合
        surfaces.clear();
        // 重新创建预览会话
        createCaptureSession(mCameraDevice);
    }
    private void addToGallery(String videoFilePath) {
        MediaScannerConnection.scanFile(this, new String[]{videoFilePath}, null,
                (path, uri) -> {
                    Toast.makeText(this, "视频已保存至相册", Toast.LENGTH_SHORT).show();
                    Toast.makeText(this, "视频路径：" + recorderPath, Toast.LENGTH_SHORT).show();
                });
    }
    private static Size chooseOptimalSize(Size[] choices
            , int width, int height, Size aspectRatio)
    {
        // 收集摄像头支持的打开预览Surface的分辨率
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices)
        {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height)
            {
                bigEnough.add(option);
            }
        }


        for(int i=0;i<bigEnough.toArray().length;i++) {
            // System.out.println("宽度: " + size.getWidth() + ", 高度: " + size.getHeight());
            // Toast.makeText(CameraActivity.this, "宽度: " + size.getWidth() + ", 高度: " + size.getHeight(), Toast.LENGTH_SHORT)
            Log.d("--chooseOptimalSize--", "宽度: " + bigEnough.get(i).getWidth() + ", 高度: " + bigEnough.get(i).getHeight());
        }

        // 如果找到多个预览尺寸，获取其中面积最小的。
        if (!bigEnough.isEmpty())
        {
            return Collections.min(bigEnough, new CompareSizesByArea());
        }
        else
        {
            System.out.println("找不到合适的预览尺寸！！！");
            Log.d("--chooseOptimalSize--", "找不到合适的预览尺寸！！！");
            // Toast.makeText(CameraActivity.this, "找不到合适的预览尺寸！！！", Toast.LENGTH_SHORT).show();
            return choices[0];
        }
    }

    // 为Size定义一个比较器Comparator
    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // 强转为long保证不会发生溢出
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    private void createCaptureSession(CameraDevice cameraDevice) {

        // 预览Surface
        texture = textureView.getSurfaceTexture();
        assert texture != null;
        texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        Surface previewSurface = new Surface(texture);
        surfaces.add(previewSurface);

        // 创建ImageReader对象(拍照)
        mImageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.JPEG, 1);
        mImageReader.setOnImageAvailableListener(mImageReaderListener, null);
        surfaces.add(mImageReader.getSurface());

        //添加录制Surface
        initRecording();
        surfaces.add(mMediaRecorder.getSurface());

        try {
            cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        // 预览会话已创建成功，可以开始预览
                        captureSession = session;

                        // 创建预览请求
                        CaptureRequest.Builder previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        previewRequestBuilder.addTarget(previewSurface); // 设置预览目标Surface
                        // 开启连续预览
                        captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, null);
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

    private void saveImageToGallery(byte[] data) {
        String fileName = "IMG_" + System.currentTimeMillis() + ".jpg";
        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + File.separator + fileName;

        try {
            FileOutputStream fos = new FileOutputStream(filePath);
            fos.write(data);
            fos.close();

            MediaScannerConnection.scanFile(this, new String[]{filePath}, null, null);
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(filePath))));
            Toast.makeText(this, "图片保存成功", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "图片保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void initRecording() {
        File recorderFile = new File(getExternalFilesDir(null), "video");
        if (!recorderFile.exists()) {
            recorderFile.mkdirs();
        }
        // 生成记录视频的文件路径，包括时间戳和.mp4后缀
        recorderPath = recorderFile.getAbsolutePath() + "/" + System.currentTimeMillis() + ".mp4";
        // 记录日志，用于调试确认视频文件路径
        Log.e("initRecording", "视频路径：" + recorderPath);
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
        mMediaRecorder.setVideoSize(1280, 720);
        // 设置视频编码格式为H.264，提供较好的压缩效率
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        // 设置音频编码格式为AAC，提供较好的音频质量
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}