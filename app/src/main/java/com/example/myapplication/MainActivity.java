package com.example.myapplication;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity {
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    // 定义界面上根布局管理器
    private FrameLayout rootLayout;
    // 定义自定义的AutoFitTextureView组件,用于预览摄像头照片
    private AutoFitTextureView textureView;
    // 摄像头ID（通常0代表后置摄像头，1代表前置摄像头）
    private String mCameraId = "0";
    // 定义代表摄像头的成员变量
    private CameraDevice cameraDevice;
    // 预览尺寸
    private Size previewSize;
    private CaptureRequest.Builder previewRequestBuilder;
    // 定义用于预览照片的捕获请求
    private CaptureRequest previewRequest;
    // 定义CameraCaptureSession成员变量
    private CameraCaptureSession captureSession;
    private ImageReader imageReader;
    private int maskViewflg=0;

    //////////////////////////
    private ImageButton recordBn;
    private ImageButton stopBn;
    // 系统的视频文件
    private File videoFile;
    private MediaRecorder mRecorder;
    // 显示视频预览的SurfaceView
    private SurfaceView sView;
    // 记录是否正在进行录制
    private boolean isRecording;

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    private CaptureRequest.Builder mRequest;
    private List<Surface> mSurfaceList = new ArrayList<>();

    private TextureHelper mTextureHelper;
    private RecorderHelper mRecorderHelper;
    private CameraHelper mCameraHelper;

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


    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        //  摄像头被打开时激发该方法
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            MainActivity.this.cameraDevice = cameraDevice;
            // 开始预览
            createCameraPreviewSession();  // ②
        }

        // 摄像头断开连接时激发该方法
        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraDevice.close();
            MainActivity.this.cameraDevice = null;
        }

        // 打开摄像头出现错误时激发该方法
        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            MainActivity.this.cameraDevice = null;
            MainActivity.this.finish();
        }
    };

    /**
     * 当活动被创建时，这个方法会被调用
     * 该方法主要负责活动的初始化设置，包括加载布局、请求权限等
     *
     * @param savedInstanceState 如果活动之前被终止过，那么该Bundle对象中会包含活动之前保存的状态
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rootLayout = findViewById(R.id.root);
        recordBn = findViewById(R.id.record);
        stopBn = findViewById(R.id.stop);
        // 让stopBn按钮不可用
        stopBn.setEnabled(false);
        //setUpCameraOutputs(width, height);
        // 获取程序界面中录视频的两个按钮

        requestPermissions(new String[]{Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.INTERNET
        },  0x123);


        mTextureHelper = new TextureHelper(this);
    }
    @Override
    public void onResume() {
        super.onResume();
        // 启动后台线程，用于执行回调中的代码
        startBackgroundThread();
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
        // 关闭camera，关闭后台线程
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 请求权限结果回调方法
     * 当活动请求用户授予权限时，此方法将被调用以通知请求结果
     *
     * @param requestCode 请求代码，用于标识请求权限的类型
     * @param permissions 请求结果中权限的数组
     * @param grantResults 对应于每个权限的授予权限结果数组，元素为PackageManager.PERMISSION_GRANTED或PackageManager.PERMISSION_DENIED
     *                     权限请求结果的数组元素数量与permissions参数数组元素数量相同，一一对应
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 0x123 && grantResults.length == 3
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED
                && grantResults[2] == PackageManager.PERMISSION_GRANTED ) {
            // 创建预览摄像头图片的TextureView组件
            textureView = new AutoFitTextureView(MainActivity.this, null);
            // 为TextureView组件设置监听器
            textureView.setSurfaceTextureListener(mSurfaceTextureListener);
            rootLayout.addView(textureView);

            findViewById(R.id.capture).setOnClickListener(view -> captureStillPicture());
            findViewById(R.id.switch_camera).setOnClickListener(view -> {
                switchCameraWithMaskAnimation();
            });
            recordBn.setOnClickListener(view -> {
                //recordVideo();
                startRecord();
            });
            stopBn.setOnClickListener(view -> {
                stopRecord();
            });

        }
    }



    private void addRecorderSurface() {
        // 获取MediaRecorder中的surface，添加到request中、添加到surfaceList中
        Surface recorderSurface = mRecorderHelper.getSurface();

        if (null != recorderSurface) {
            mRequest.addTarget(recorderSurface);
            mSurfaceList.add(recorderSurface);
        }
    }

    private void addTextureViewSurface() {
        // 获取TextureView中的surface，添加到request中、添加到surfaceList中
        Surface previewSurface = mTextureHelper.getSurface(textureView);

        if (null != previewSurface) {
            mRequest.addTarget(previewSurface);
            mSurfaceList.add(previewSurface);
        }
    }


    private void startRecord() {
        // 设置Recorder配置，启动录像会话
        //int sensorOrientation = mCameraHelper.getSensorOrientation(mCameraHelper.getBackCameraId());
        int sensorOrientation = 0;
        int displayRotation = getWindowManager().getDefaultDisplay().getRotation();
        mRecorderHelper.configRecorder(sensorOrientation, displayRotation);

        //startRecordSession();
    }

    private void stopRecord() {
        // 关闭录像会话，停止录像，重新进入预览
        mRecorderHelper.stop();
        startPreviewSession();
    }

    private void startPreviewSession() {
        if (null == cameraDevice || !textureView.isAvailable()) {
            return;
        }

        try {
            // 创建新的会话前，关闭以前的会话
            closePreviewSession();

            // 创建预览会话请求
            mSurfaceList.clear();
            mRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            addTextureViewSurface();

            // 启动会话
            // 参数1：camera捕捉到的画面分别输出到surfaceList的各个surface中;
            // 参数2：会话状态监听;
            // 参数3：监听器中的方法会在指定的线程里调用，通过一个handler对象来指定线程;
            cameraDevice.createCaptureSession(mSurfaceList, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    captureSession = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                }

                @Override
                public void onClosed(@NonNull CameraCaptureSession session) {
                    super.onClosed(session);
                }
            }, mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startRecordSession() {
        if (null == cameraDevice || !textureView.isAvailable()) {
            return;
        }

        try {
            // 创建新的会话前，关闭以前的会话
            closePreviewSession();

            // 创建预览会话请求
            mSurfaceList.clear();
            mRequest = createCameraPreviewSession();
            addTextureViewSurface();

            // 启动会话
            // 参数1：camera捕捉到的画面分别输出到surfaceList的各个surface中;
            // 参数2：会话状态监听;
            // 参数3：监听器中的方法会在指定的线程里调用，通过一个handler对象来指定线程;
            cameraDevice.createCaptureSession(mSurfaceList, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    captureSession = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                }

                @Override
                public void onClosed(@NonNull CameraCaptureSession session) {
                    super.onClosed(session);
                }
            }, mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if (null == cameraDevice) {
            return;
        }

        try {
            mRequest.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            // 这个接口是预览。作用是把camera捕捉到的画面输出到surfaceList中的各个surface上，每隔一定时间重复一次
            captureSession.setRepeatingRequest(mRequest.build(), null, mBackgroundHandler);

            // 这个接口是拍照。由于拍照需要获得图像数据，所以这里需要实现CaptureCallback，在回调里获得图像数据
//            mSession.capture(CaptureRequest request, CaptureCallback listener, Handler handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closePreviewSession() {
        if (null != captureSession) {
            captureSession.close();
            captureSession = null;
        }
    }


    private void stopVideo() {
        // 如果正在进行录制
        if (isRecording) {
            // 停止录制
            mRecorder.stop();
            // 释放资源
            mRecorder.release();
            mRecorder = null;
            // 让recordBn按钮可用
            recordBn.setEnabled(true);
            // 让stopBn按钮不可用
            stopBn.setEnabled(false);
        }
    }


    private void recordVideo() {
        // 获取当前时间戳
        long currentTimeMillis = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String formattedTime = sdf.format(new Date(currentTimeMillis));

        // 生成随机字符串
        Random random = new Random();
        StringBuilder randomString = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            randomString.append(random.nextInt(10)); // 生成6位数字
        }

        // 组合图片名称
        String fileName = "IMG_" + formattedTime + "_" + randomString.toString() + ".mp4";

        // 创建保存录制视频的视频文件
        videoFile = new File("/storage/emulated/0/DCIM/Camera", fileName);

        // 创建MediaRecorder对象
        mRecorder = new MediaRecorder();
        // 重置录音机状态，为下一次录音做好准备
        mRecorder.reset();
        // 设置从麦克风采集声音
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        // 设置从摄像头采集图像
        mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        // 设置视频文件的输出格式
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        // 设置声音编码格式
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        // 设置图像编码格式
        mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
        // 设置视频尺寸
        mRecorder.setVideoSize(1920, 1080);
        // 每秒 12帧
        mRecorder.setVideoFrameRate(12);
        mRecorder.setOutputFile(videoFile.getAbsolutePath());

        // 获取 TextureView 的 SurfaceTexture
        SurfaceTexture texture = textureView.getSurfaceTexture();

        // 创建 Surface 对象
        Surface surface = new Surface(texture);

        // 使用 Surface 设置预览显示
        mRecorder.setPreviewDisplay(surface);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 开始录制
        mRecorder.start();
        Toast.makeText(this, "开始录制", Toast.LENGTH_SHORT).show();

        // 让recordBn按钮不可用
        recordBn.setEnabled(false);
        // 让stopBn按钮可用
        stopBn.setEnabled(true);
        isRecording = true;
    }



    /**
     * 关闭相机设备
     * 此方法检查当前是否有相机设备处于打开状态，如果有，则关闭它并置空引用
     * 这有助于资源管理，防止内存泄漏，确保相机设备在不再需要时被正确释放
     */

    private void closeCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if(mRecorderHelper!= null) {
            mRecorderHelper.release();
        }
    }

    private void SwichCamera() {
        // 切换摄像头
        closeCamera();
        mCameraId = "1".equals(mCameraId) ? "0" : "1";
        openCamera(textureView.getWidth(), textureView.getHeight());
    }

    private void SwichCamera(View maskView ) {
        // 切换摄像头
        closeCamera();
        mCameraId = "1".equals(mCameraId) ? "0" : "1";
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



    private void switchCameraWithMaskAnimation() {
        if (cameraDevice != null) {
            // 创建一个蒙版视图
            View maskView = new View(this);
            maskView.setBackgroundColor(Color.BLACK); // 设置背景色为黑色
            maskView.setAlpha(0f); // 初始透明度为0

            // 获取根布局
            FrameLayout rootView = findViewById(R.id.root); // 假设root是你的主布局
            int width =previewSize.getWidth();
            int height =previewSize.getHeight();

            // 设置蒙版视图的布局参数
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    width,
                    height
            );
            rootView.addView(maskView, params);

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
                    maskViewflg = 1;
                    SwichCamera(maskView);
                   // Toast.makeText(MainActivity.this, "动画结束", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void restoreCamera() {
        if (cameraDevice == null && maskViewflg == 0) {
            // 恢复之前的相机设备
           openCamera(textureView.getWidth(), textureView.getHeight());
           int width =previewSize.getWidth();
           String widthStr = String.valueOf(width);
           int height =previewSize.getHeight();
           String heightStr = String.valueOf(height);

           Toast.makeText(this, widthStr, Toast.LENGTH_SHORT).show();
           Toast.makeText(this, heightStr, Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * 捕获静态图片的方法
     * 该方法用于捕获静态图片，设置相机参数以确保拍摄出清晰的照片，并在拍摄完成后处理后续的显示和界面更新
     */
    private void captureStillPicture()
    {
        try {
            if (cameraDevice == null) {
                return;
            }
            // 创建作为拍照的CaptureRequest.Builder
            CaptureRequest.Builder captureRequestBuilder = cameraDevice
                    .createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            // 将imageReader的surface作为CaptureRequest.Builder的目标
            captureRequestBuilder.addTarget(imageReader.getSurface());
            // 设置自动对焦模式
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 设置自动曝光模式
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            // 获取设备方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            // 根据设备方向计算设置照片的方向
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,
                    ORIENTATIONS.get(rotation));
            // 停止连续取景
            captureSession.stopRepeating();
            // 捕获静态图像
            captureSession.capture(captureRequestBuilder.build(),
                    new CameraCaptureSession.CaptureCallback()  // ⑤
                    {
                        // 拍照完成时激发该方法
                        @Override
                        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                                       @NonNull CaptureRequest request, @NonNull TotalCaptureResult result)
                        {
                            try {
                                // 重设自动对焦模式
                                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                                        CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                                // 设置自动曝光模式
                                previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                                // 打开连续取景模式
                                captureSession.setRepeatingRequest(previewRequest, null, null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    // 根据手机的旋转方向确定预览图像的方向
    private void configureTransform(int viewWidth, int viewHeight) {
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
    // 打开摄像头
    private void openCamera(int width, int height)
    {
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            // 如果用户没有授权使用摄像头，直接返回
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            // 打开摄像头
            manager.openCamera(mCameraId, stateCallback, null); // ①


        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * 创建相机预览会话
     * 这个方法设置相机预览的SurfaceTexture，创建用于预览的CaptureRequest.Builder，
     * 并通过CameraCaptureSession管理预览和捕获请求
     */
    private CaptureRequest.Builder createCameraPreviewSession()
    {
        try
        {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface surface = new Surface(texture);
            // 创建作为预览的CaptureRequest.Builder
            previewRequestBuilder = cameraDevice
                    .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // 将textureView的surface作为CaptureRequest.Builder的目标
            previewRequestBuilder.addTarget(new Surface(texture));
            // 创建CameraCaptureSession，该对象负责管理处理预览请求和拍照请求
            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() // ③
                    {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession)
                        {
                            // 如果摄像头为null，直接结束方法
                            if (null == cameraDevice)
                            {
                                return;
                            }
                            // 当摄像头已经准备好时，开始显示预览
                            captureSession = cameraCaptureSession;
                            // 设置自动对焦模式
                            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                            // 设置自动曝光模式
                            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                            // 开始显示相机预览
                            previewRequest = previewRequestBuilder.build();
                            try {
                                // 设置预览时连续捕获图像数据
                                captureSession.setRepeatingRequest(previewRequest, null, null);  // ④
                            }
                            catch (CameraAccessException e)
                            {
                                e.printStackTrace();
                            }
                        }
                        @Override public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession)
                        {
                            Toast.makeText(MainActivity.this, "配置失败！",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }, null);
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
        return previewRequestBuilder;
    }

    /**
     * 设置摄像头的输出参数，包括照片尺寸和预览尺寸
     * @param width 预览画面的宽度
     * @param height 预览画面的高度
     */
    private void setUpCameraOutputs(int width, int height)
    {
        // 获取当前时间戳
        long currentTimeMillis = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String formattedTime = sdf.format(new Date(currentTimeMillis));

        // 生成随机字符串
        Random random = new Random();
        StringBuilder randomString = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            randomString.append(random.nextInt(10)); // 生成6位数字
        }

        // 组合图片名称
        String fileName = "IMG_"+formattedTime + "_" + randomString.toString() + ".jpg";

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            // 获取指定摄像头的特性
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId);
            // 获取摄像头支持的配置属性
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.
                    SCALER_STREAM_CONFIGURATION_MAP);
            // 获取摄像头支持的最大尺寸
            Size largest = Collections.max(
                    Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                    new CompareSizesByArea());
            // 创建一个ImageReader对象，用于获取摄像头的图像数据
            imageReader = ImageReader.newInstance(largest.getWidth(),
                    largest.getHeight(), ImageFormat.JPEG, 2);
            imageReader.setOnImageAvailableListener(reader -> {
                // 当照片数据可用时激发该方法
                // 获取捕获的照片数据
                Image image = reader.acquireNextImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                // 使用IO流将照片写入指定文件
                File file = new File("/storage/emulated/0/DCIM/Camera", fileName);
                buffer.get(bytes);
                try (
                        FileOutputStream output = new FileOutputStream(file))
                {
                    output.write(bytes);
                    Toast.makeText(MainActivity.this, "保存: "
                            + file, Toast.LENGTH_SHORT).show();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                finally
                {
                    image.close();
                    // 发送广播通知图库扫描图片
                    sendBroadcastToMediaScanner(MainActivity.this, file);
                }
            },null);

            // 获取最佳的预览尺寸
            previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    width, height, largest);
            // 根据选中的预览尺寸来调整预览组件（TextureView的）的长宽比
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                textureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
            } else {
                textureView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
            }
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
        catch (NullPointerException e)
        {
            System.out.println("出现错误。");
        }
    }
    /**
     * 发送广播给媒体扫描器，使其扫描指定的文件
     *
     * @param mainActivity 主活动上下文，用于发送广播
     * @param file 需要扫描的文件
     */
    private void sendBroadcastToMediaScanner(MainActivity mainActivity, File file) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, null, new MediaScannerConnection.OnScanCompletedListener() {
                public void onScanCompleted(String path, Uri uri) {
                    Log.i("ExternalStorage", "Scanned " + path + ":");
                    Log.i("ExternalStorage", "-> uri=" + uri);
                }
            });
        } else {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(Uri.fromFile(file));
            this.sendBroadcast(mediaScanIntent);
        }
    }

    /**
     * 选择最合适的预览尺寸
     * 从摄像头支持的分辨率列表中选择一个最合适的预览尺寸，确保选择的尺寸满足指定的宽高和宽高比要求
     *
     * @param choices 摄像头支持的分辨率列表
     * @param width 预览Surface的宽度
     * @param height 预览Surface的高度
     * @param aspectRatio 期望的宽高比
     * @return 返回最合适的预览尺寸如果找不到合适的尺寸，返回摄像头支持的第一个分辨率
     */
    private static Size chooseOptimalSize(Size[] choices
            , int width, int height, Size aspectRatio)
    {
        // 收集摄像头支持的打过预览Surface的分辨率
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
        // 如果找到多个预览尺寸，获取其中面积最小的。
        if (bigEnough.size() > 0)
        {
            return Collections.min(bigEnough, new CompareSizesByArea());
        }
        else
        {
            System.out.println("找不到合适的预览尺寸！！！");
            return choices[0];
        }
    }
    // 为Size定义一个比较器Comparator
    static class CompareSizesByArea implements Comparator<Size>
    {
        @Override
        public int compare(Size lhs, Size rhs)
        {
            // 强转为long保证不会发生溢出
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }
}