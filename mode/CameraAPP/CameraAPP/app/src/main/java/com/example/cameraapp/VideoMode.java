package com.example.cameraapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioManager;
import android.media.MediaActionSound;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class VideoMode implements IMode {
    public String TAG = "MyCamera VideoMode:";
    public MainActivity mActivity;
    public CameraDevice mCameraDevice;
    public AutoFitTextureView textureView;
    public CaptureRequest.Builder mPreviewRequestBuilder; //预览的builder请求
    public CaptureRequest.Builder mCaptureRequestBuilder;  //录像的builder请求
    public CaptureRequest mPreviewRequest;  // 定义用于预览的捕获请求
    public CaptureRequest mRecordRequest;   // 录像的捕获请求
    public int mCurrentCameraIndex = 0;
    public MediaRecorder mMediaRecorder;
    public List<Surface> mOutputs;
    public CameraCaptureSession.StateCallback mSessionStateCallback;
    public CameraCaptureSession.CaptureCallback mSessionCaptureCallback;
    public CameraCharacteristics characteristics;
    public CameraManager manager;
    public StreamConfigurationMap map;
    public CameraCaptureSession mPreviewSession; //视频预览
    public Size mPreviewSize; //视频预览尺寸,
    public Size mVideoSize; //视频录制尺寸
    public String mCameraId;  //相机id
    public FrameLayout rootLayout;
    public ImageView imageView;
    public File mVideoFile;
    public AudioManager audioManager;
    public Button record;
    public boolean isRecording = true;
    public Handler mBackgroundHandler;
    public SurfaceTexture texture;
    public Surface mPreviewSurface;  //预览的surface
    public Surface mRecorderSurface; //录像的Surface
    public Spinner mVideoQuality;
    public MediaActionSound mMediaActionSound;
    public String mNextVideoAbsolutePath;
    public static final int REQUEST_CAMERA_PERMISSION = 1;
    public static final int REQUEST_READ_STORAGE_PERMISSION = 4;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 3;
    //定义支持的视频质量
    public static final Size VIDEO_SIZE_480P = new Size(640, 480);
    public static final Size VIDEO_SIZE_720P = new Size(1280, 720);
    //默认480p
    public Size mCurrentVideoSize = VIDEO_SIZE_480P;
    public int mSensorOrientation;
    public static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    public static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    public static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();
    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }


    public VideoMode(MainActivity mainActivity) {
        this.mActivity = mainActivity;
        this.rootLayout = mActivity.findViewById(R.id.root);
        this.imageView = mActivity.findViewById(R.id.imageView);
        this.textureView = mActivity.findViewById(R.id.textureView);
        this.record = mActivity.findViewById(R.id.record);

        //设置下拉
        mVideoQuality = mActivity.findViewById(R.id.videoQuality);
        List<Size> mVideoQualities = new ArrayList<>();
        mVideoQualities.add(VIDEO_SIZE_480P);
        mVideoQualities.add(VIDEO_SIZE_720P);
        ArrayAdapter<Size> adapter = new ArrayAdapter<>(mActivity, android.R.layout.simple_spinner_item, mVideoQualities);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); //设置下拉列表样式
        mVideoQuality.setAdapter(adapter);

        //设置下拉监听去
        mVideoQuality.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                Size mSelectSize = (Size) adapterView.getItemAtPosition(position);
                //更新视频大小
                mCurrentVideoSize = mSelectSize;
                //重新启动录制
                try {
                    restartRecording();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        //跳转到相册
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //启动相册的Activity
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*"); //选择图片类型
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); //授予临时读取权限
                try {
                    mActivity.startActivityForResult(intent, REQUEST_READ_STORAGE_PERMISSION);
                } finally {
                    Toast.makeText(mActivity, "打开图库成功", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //
        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isRecording) {
                    Log.d(TAG, "11111111111");
                    startRecordingVideo();
                    mMediaActionSound.play(MediaActionSound.START_VIDEO_RECORDING);
                    isRecording = false;
                } else {
                    if (mMediaRecorder != null) {
                        Log.d(TAG, "22222222");
                        stopRecordingVideo();
                        mMediaActionSound.play(MediaActionSound.STOP_VIDEO_RECORDING);
                        isRecording = true;
                    }
                }
            }
        });
        mMediaActionSound = new MediaActionSound(); //视频按钮音
    }

    public TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
            //surfaceTexture可用是打开相机
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
            mPreviewSurface.release();
            mPreviewSurface = null;
            closeCamera();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

        }
    };

    //视频回调方法
    public final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            //开始视频预览
            initChildThread();
            setUpCameraOutputs(textureView.getWidth(), textureView.getHeight()); //设置预览尺寸
            previewRequest();
            createCameraPreviewSession();
            createCameraVideoSession();
            Log.d(TAG, "aaaaaaaaaaaaaa");
            try {
                mCameraDevice.createCaptureSession(mOutputs, mSessionStateCallback, mBackgroundHandler);
                Log.i(TAG,"rrrrrrrrrrrr");

            } catch (CameraAccessException e) {
                throw new RuntimeException(e);
            }
            if (null != textureView) {
                configureTransform(textureView.getWidth(), textureView.getHeight());
                Log.d(TAG, "aaaaaa");
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;

        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            cameraDevice.close();
            mCameraDevice = null;
            mActivity.finish();
            Log.i(TAG, "相机打开错误");

        }
    };

    //设置视频质量的大小
    public void setVideoSize(Size videoSize) throws IOException {
        this.mCurrentVideoSize = mPreviewSize;
        //更新视频大小
        restartRecording();
    }

    //重新启动录制，更新视频质量
    public void restartRecording() throws IOException {
        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            initVideo();
            mMediaRecorder.start();
        }
    }
    @Override
    public void openCamera(int width, int height) {
        //判断是否有权限
        if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA) != PermissionChecker.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            Log.i(TAG, "相机没有授权");
            return;
        }
        if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }
        manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIdList = manager.getCameraIdList();  //获取摄像头id，存入cameraIdList
            if (cameraIdList.length == 0) {
                Toast.makeText(this.mActivity, "没有可用的摄像头", Toast.LENGTH_SHORT).show();
                return;
            }
            mCameraId = cameraIdList[mCurrentCameraIndex]; // 当前相机索引
            Log.i(TAG, "cameraId=" + mCameraId);
            characteristics = manager.getCameraCharacteristics(mCameraId);//获取摄像头的参数
            map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) {
                Log.i(TAG, "StreamConfigurationMap为空");
                throw new RuntimeException();
            }
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
            // 打开摄像头
            Log.i(TAG, "打开摄像头成功");
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.i(TAG, "打开摄像头失败");
        }
    }
    @Override
    public void switchCamera() {
        try {
            String[] cameraIdList = manager.getCameraIdList();
            if (cameraIdList.length < 2) {
                Toast.makeText(this.mActivity, "没有多的镜头可以切换", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "没有可切换的摄像头");
                return;
            }
            mCurrentCameraIndex = (mCurrentCameraIndex + 1) % cameraIdList.length;
            mCameraId = cameraIdList[mCurrentCameraIndex];
            closeCamera();
            openCamera(textureView.getWidth(), textureView.getHeight());
            Log.i(TAG, "切换摄像头成功,CameraId=" + mCameraId);
        } catch (CameraAccessException e) {
            Log.i(TAG, "切换摄像头失败，出现异常");
            e.printStackTrace();
        }
    }

    @Override
    public void setUpCameraOutputs(int width, int height) {
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);  //获取摄像头方向
//            mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, mCurrentVideoSize);
            //
            Log.i(TAG, "previewSize:" + mPreviewSize);
            textureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight()); //更新textureView的宽高比
            mNextVideoAbsolutePath = Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera/" + System.currentTimeMillis() + ".mp4";
            Log.i(TAG, "文件路径:" + mNextVideoAbsolutePath);
            int orientation = mActivity.getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                textureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            } else {
                textureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }
    }

    //预览选择尺寸
    public static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        //检查输入参数
        if (choices == null || aspectRatio == null) {
            Log.i("chooseOptimalSize", "无效输入参数");
            return null;
        }
        Log.i("VideoMode", "choices:" + choices);
        // 收集摄像头支持的打过预览Surface的分辨率
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        Log.i("Video Mode", "aspectRatio宽高：" + w + ":" + h);
        //检查宽高比是否有效
        if (w <= 0 || h <= 0) {
            Log.i("Tag", "宽高比无效");
        }
        Log.i("VideoMode", "bigEnough:" + bigEnough);
        // 如果找到多个预览尺寸，获取其中面积最小的。
        if (!bigEnough.isEmpty()) {
            return Collections.min(bigEnough, new PhotoMode.CompareSizesByArea());
        } else {
            System.out.println("录像找不到合适的预览尺寸！！！");
            return choices[0];
        }
    }

    //初始化一个子线程
    public void initChildThread() {
        HandlerThread mHandlerThread = new HandlerThread("CameraBackground");
        mHandlerThread.start();
        mBackgroundHandler = new Handler(mHandlerThread.getLooper());
        Log.i(TAG, "子线程初始化成功");
    }

    public void configureTransform(int viewWidth, int viewHeight) {

        // 获取手机的旋转方向
        int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getWidth(),mPreviewSize.getHeight());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        Log.i(TAG, "configureTransform");
        // 处理手机横屏的情况
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max((float) viewHeight / mPreviewSize.getHeight(), (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
            Log.d(TAG,"处理手机横屏的情况");
        }
        // 处理手机倒置的情况
        else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        textureView.setTransform(matrix);
    }
    public void previewRequest() {
        try {
            texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(textureView.getWidth(), textureView.getHeight());
            mPreviewSurface = new Surface(texture); //预览输出
            mOutputs.add(mPreviewSurface);
            Log.i(TAG, "mPreviewRequestBuilder");

            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(mPreviewSurface);

            mOutputs = new ArrayList<>();

            Log.i(TAG, "sssssssssss");
            mPreviewRequest = mPreviewRequestBuilder.build();
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }
    }
        //视频预览、录像-------------
    @Override
    public void createCameraPreviewSession() {
        mSessionStateCallback = new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                mPreviewSession = cameraCaptureSession;
                try {
                    mPreviewSession.setRepeatingRequest(mPreviewRequest, mSessionCaptureCallback, mBackgroundHandler);
                    Log.i(TAG,"createCameraPreviewSession ");
                } catch (CameraAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

            }
        };
    }
    public void createCameraVideoSession(){
        mSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
                Log.i(TAG,"onCaptureCompleted");
            }
        };
    }
    @Override
    public void captureStillPicture() {
        try{
            mPreviewSession.stopRepeating();
            mPreviewSession.close();
            mPreviewSession = null;
            Log.i(TAG,"pppppppppppppppp");
        } catch (CameraAccessException e){
            e.printStackTrace();
        }
        Log.d(TAG,"xxxxxxxxxxxxxxxxxxxx");
//        texture = textureView.getSurfaceTexture();
//        assert texture != null;
//        texture.setDefaultBufferSize(textureView.getWidth(), textureView.getHeight());

//        mPreviewSurface = new Surface(texture); //预览输出
        mPreviewSession.close();
        initVideo();
        Log.i(TAG, "ooooooooooooooooo: ");
        mRecorderSurface = mMediaRecorder.getSurface();
        Log.i(TAG, "dddddddddddddddd");
        mOutputs.add(mRecorderSurface);
        Log.i(TAG, "ccccccccccccccccccccc: ");
//        mPreviewRequestBuilder.addTarget(mRecorderSurface);
        createCameraPreviewSession();
        Log.i(TAG, "captureStillPicture: ");
        try {
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            mPreviewRequestBuilder.addTarget(mPreviewSurface);
            mPreviewRequestBuilder.addTarget(mRecorderSurface);
            mCameraDevice.createCaptureSession(mOutputs, mSessionStateCallback, mBackgroundHandler);
        }catch (CameraAccessException e){
            e.printStackTrace();
        }


    }
    public void startRecordingVideo(){
        if(mMediaRecorder != null){
            mMediaRecorder.start();
            Log.i(TAG,"开始录像1");
        } else {
            captureStillPicture();
            mMediaRecorder.start();
            Log.i(TAG,"开始录像");
        }

    }
    //关闭预览会话
    public void closePreviewSession(){
        if (mPreviewSession != null){
            mPreviewSession.close();
            mPreviewSession = null;
        }
    }
    //设置分辨率
    @Override
    public void closeCamera() {
        //关闭预览
        closePreviewSession();
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (null != mMediaRecorder) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    //停止预览------------stopPreview
    @Override
    public void stopRecordingVideo() {
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        mMediaRecorder = null;
        Log.i(TAG, "Video saved: " + mNextVideoAbsolutePath);
        mNextVideoAbsolutePath = null;
        createCameraPreviewSession();
    }

    @Override
    public void cameraSwitchMode() {
        openCamera(textureView.getWidth(),textureView.getHeight());
        record.setVisibility(View.VISIBLE);
        record.setEnabled(true);
        mVideoQuality.setVisibility(View.VISIBLE);
        mVideoQuality.setEnabled(true);
    }

    //初始化视频
    private void initVideo() {
        try {
            if (mMediaRecorder == null){
                mMediaRecorder = new MediaRecorder();
            } else {
                mMediaRecorder.reset();
            }
            if (mRecorderSurface == null){
                mRecorderSurface = MediaCodec.createPersistentInputSurface();
            }

            mMediaRecorder.setInputSurface(mRecorderSurface);
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC); //设置音频输入源
//            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);  //camera1的
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE); //设置视频输入源 camera2的
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); //设置音频输出格式
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); //设置音频编码器
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264); //设置视频编码器
            mMediaRecorder.setOutputFile(mNextVideoAbsolutePath);
            mMediaRecorder.setVideoEncodingBitRate(10000000);
            mMediaRecorder.setVideoFrameRate(30);
            mMediaRecorder.setVideoSize(mCurrentVideoSize.getWidth(), mCurrentVideoSize.getHeight());
            int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
            switch (mSensorOrientation) {
                case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                    mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                    break;
                case SENSOR_ORIENTATION_INVERSE_DEGREES:
                    mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                    break;
            }
            Surface surface = new Surface(textureView.getSurfaceTexture());
            mMediaRecorder.setPreviewDisplay(surface);
            mVideoFile = new File(mNextVideoAbsolutePath);
            Log.d(TAG,"1111222");
            mMediaRecorder.prepare();
            Log.d(TAG,"11133333");
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
            e.printStackTrace();
        } finally {
            sendBroadcastToMediaScanner(mActivity,mVideoFile);
        }
    }

    public void sendBroadcastToMediaScanner(Context context, File file) { //通知系统

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, null, null);
        } else {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(Uri.fromFile(file));
            context.sendBroadcast(mediaScanIntent);
        }
    }

}
