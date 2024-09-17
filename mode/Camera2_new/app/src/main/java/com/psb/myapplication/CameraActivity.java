package com.psb.myapplication;

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
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
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

public class CameraActivity extends AppCompatActivity implements View.OnTouchListener {

    private static final int DISTANCE_LIMIT = 50; // 距离阈值
    private static final int VELOCITY_THRESHOLD = 500; // 速度阈值;

    private AutoFitTextureView mTextureView = null;
    private FrameLayout mRootLayout = null;
    private CustomViewL mCustomViewL = null;
    private GestureDetector mGestureDetector = null;
    private DisplayMetrics mDisplayMetrics = null;
    //按键定义
    private ImageView record, stop, switch_camera, capture,mPictureIv;
    private TextView switch_frame,void_quality;
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
    private static MyApp app;
    public Context mContext;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initScreen();//初始化布局
        initVariable();//初始化变量
        initCustomViewL(); // 初始化自定义View
        initRequestPermissions(); // 申请摄像头权限
        initClickListener(); // 初始化点击监听器
        initTockListener();//初始化触摸监听器
    }

    // 初始化触摸监听器
    private void initTockListener() {
        mTextureView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getPointerCount() == 1) {
                    // 点击对焦
                    mCameraProxy.triggerFocusAtPoint((int) event.getX(), (int) event.getY(), previewSize.getWidth(), previewSize.getHeight(),previewRequestBuilder,captureSession);
                    Toast.makeText(getApplicationContext(), "点击对焦", Toast.LENGTH_SHORT).show();
                    return true;
                }

                // 双指缩放
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    // 双指按下
                    case MotionEvent.ACTION_POINTER_DOWN:
                        Toast.makeText(getApplicationContext(), "双指按下", Toast.LENGTH_SHORT).show();
                        mOldDistance = getFingerSpacing(event);
                        break;
                    // 双指移动
                    case MotionEvent.ACTION_MOVE:
                         Toast.makeText(getApplicationContext(), "双指移动", Toast.LENGTH_SHORT).show();
                        float newDistance = getFingerSpacing(event);
                        if (newDistance > mOldDistance) {
                            mCameraProxy.handleZoom(true,mCameraDevice,characteristics,previewRequestBuilder);
                        } else if (newDistance < mOldDistance) {
                            mCameraProxy.handleZoom(false,mCameraDevice,characteristics,previewRequestBuilder);
                        }
                        mOldDistance = newDistance;
                        break;
                    default:
                        break;
                }

                // 返回true，表示自己处理触摸事件
                return true;
            }
        });

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
        }
    }

    @Override
    public void onPause() {
        // Toast.makeText(MainActivity.this, "onPause", Toast.LENGTH_SHORT);
        // 关闭camera，关闭后台线程
        //closeCamera();
        //stopBackgroundThread();
        super.onPause();
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

        mCustomViewL = findViewById(R.id.mCustomView);
        // 初始化点击监听器
        record = findViewById(R.id.record);
        stop = findViewById(R.id.stop);
        switch_camera = findViewById(R.id.switch_camera);
        capture = findViewById(R.id.capture);
        switch_frame = findViewById(R.id.frame_switch);
        void_quality = findViewById(R.id.void_quality);
        stop.setEnabled(false);
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
        mCameraProxy= new Camera2Proxy(this);
        // 显示最近
        mPictureIv = findViewById(R.id.picture_iv);
        // 显示最近一次拍照的图片
        mPictureIv.setImageBitmap(ImageUtils.getLatestThumbBitmap(this));
        mContext=this;

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
        record.setOnClickListener(v -> {
            if (v.getId() == R.id.record) {
                record.setEnabled(true);
                // Toast.makeText(CameraActivity.this, "点击了录像按钮", Toast.LENGTH_SHORT).show();
                //startRecordingVideo();
                stop.setEnabled(true);
                record.setEnabled(false);
            }
        });

        stop.setOnClickListener(v -> {
            if (v.getId() == R.id.stop) {
                stop.setEnabled(true);
                //Toast.makeText(CameraActivity.this, "点击了停止按钮", Toast.LENGTH_SHORT).show();
                //stopRecordingVideo();
                record.setEnabled(true);
                stop.setEnabled(false);
            }
        });

        switch_camera.setOnClickListener(v -> {
            if (v.getId() == R.id.switch_camera) {
                //Toast.makeText(CameraActivity.this, "点击了切换摄像头按钮", Toast.LENGTH_SHORT).show();
                switchCameraWithMaskAnimation();
                //SwichCamera();
            }
        });

        capture.setOnClickListener(v -> {
            if (v.getId() == R.id.capture) {
                Toast.makeText(CameraActivity.this, "点击了拍照按钮", Toast.LENGTH_SHORT).show();
                //takePicture(reader -> new ImageSaveTask().execute(reader.acquireNextImage()));
                takePicture();
            }
        });

        switch_frame.setOnClickListener(v -> {
            if (v.getId() == R.id.frame_switch) {
                //Toast.makeText(CameraActivity.this, "点击了切换帧按钮", Toast.LENGTH_SHORT).show();
                //SwichFrame();
            }
        });

        void_quality.setOnClickListener(v -> {
            if (v.getId() == R.id.void_quality) {
                //Toast.makeText(CameraActivity.this, "点击了切换质量按钮", Toast.LENGTH_SHORT).show();
                //SwichQuality();
            }
        });

        mPictureIv.setOnClickListener(v -> {
            if (v.getId() == R.id.void_quality) {
                //Toast.makeText(CameraActivity.this, "点击了切换质量按钮", Toast.LENGTH_SHORT).show();
                //SwichQuality();
            }
        });

    }


    private void switchCameraWithMaskAnimation() {
        if (mCameraDevice != null) {
            // 创建一个蒙版视图
            View maskView = new View(this);
            maskView.setBackgroundColor(Color.BLACK); // 设置背景色为黑色
            maskView.setAlpha(0f); // 初始透明度为0

            int width =previewSize.getWidth();
            int height =previewSize.getHeight();

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

    private void SwichCamera(View maskView ) {
        // 切换摄像头
        closeCamera();
        cameraId = "1".equals(cameraId) ? "0" : "1";
        // 清空 surfaces 集合
        surfaces.clear();
        openCamera(mTextureView.getWidth(), mTextureView.getHeight());
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
        handler.postDelayed(removeMaskRunnable, 650); // 延迟 1000 毫秒（1 秒）
    }


    private void closeCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }

    }


    // 异步保存图片
    private class ImageSaveTask extends AsyncTask<Image, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Image ... images) {
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
            return ImageUtils.getLatestThumbBitmap(mContext);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            mPictureIv.setImageBitmap(bitmap);
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

           // mImageReader.setOnImageAvailableListener(onImageAvailableListener, null);
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





    /*
    滑动事件及回调函数
    */
    private static class MyGestureDetectorListener implements GestureDetector.OnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            Log.d("CameraActivity", "onDown");
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
                if (deltaX > DISTANCE_LIMIT) {
                    //Log.d("CameraActivity", "切换到布局1" + mCustomViewL.scrollRight());
                } else if (deltaX < -DISTANCE_LIMIT) {
                    //Log.d("CameraActivity", "切换到布局2" + mCustomViewL.scrollLeft());
                }
            }

            return true;
        }

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

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture
                , int width, int height) {
            //configureTransform(width, height);
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
        //setUpCameraOutputs(width, height);
        initAutoFitTextureView(mTextureView, mTextureView.getWidth(), mTextureView.getHeight());
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
            Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
            // 获取最佳的预览尺寸
            previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, largest);
            // 根据选中的预览尺寸来调整预览组件（TextureView的）的长宽比
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                textureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
            } else {
                textureView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
            }


        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.out.println("出现错误。");
            Log.d("--initAutoFitTextureView--", "出现错误。");
        }
        // 配置变换
        configureTransform();
    }

    //配置变换
    private void configureTransform() {
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
            createCaptureSessionAsync(cameraDevice);
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
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // 如果找到多个预览尺寸，获取其中面积最小的。
        if (!bigEnough.isEmpty()) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            System.out.println("找不到合适的预览尺寸！！！");
            Log.d("--chooseOptimalSize--", "找不到合适的预览尺寸！！！");
            // Toast.makeText(CameraActivity.this, "找不到合适的预览尺寸！！！", Toast.LENGTH_SHORT).show();
            return choices[0];
        }
    }

    private void createCaptureSessionAsync(final CameraDevice cameraDevice) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    createCaptureSession(cameraDevice);
                } catch (CameraAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        });
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
        surfaces.add(mMediaRecorder.getSurface());


        // 创建预览请求
        previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        // 设置预览目标Surface
        previewRequestBuilder.addTarget(previewSurface);
        // 设置自动对焦模式
        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        // 设置自动曝光模式
        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

        try {
            cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                // 预览会话已创建
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        // 预览会话已创建成功，可以开始预览
                        captureSession = session;

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
            // 显示最近一次拍照的图片
            mPictureIv.setImageBitmap(ImageUtils.getLatestThumbBitmap(this));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initRecording() {
        File recorderFile = new File(getExternalFilesDir(null), "video");
        if (!recorderFile.exists()) {
            recorderFile.mkdirs();
        }
        String fileName= "VID_" + System.currentTimeMillis() + ".mp4";
        // 生成记录视频的文件路径，包括时间戳和.mp4后缀
        String recorderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + File.separator + fileName;
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
        mMediaRecorder.setVideoSize(previewSize.getWidth(), previewSize.getHeight());
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