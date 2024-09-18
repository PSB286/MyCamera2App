package com.psb.myapplication;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
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
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.widget.Toast;
import android.hardware.camera2.CaptureRequest;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class Camera2Proxy {

    private static final String TAG = "Camera2Proxy";
    private final CameraActivity myapp=CameraActivity.getInstance();
    private final Activity mActivity;

    private int mCameraId = CameraCharacteristics.LENS_FACING_FRONT; // 要打开的摄像头ID
    private CameraCharacteristics mCameraCharacteristics; // 相机属性
    private CameraManager mCameraManager; // 相机管理者
    private CameraDevice mCameraDevice; // 相机对象
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest.Builder mPreviewRequestBuilder; // 相机预览请求的构造器
    private CaptureRequest mPreviewRequest;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private ImageReader mPictureImageReader;
    private Surface mPreviewSurface;
    private OrientationEventListener mOrientationEventListener;

    private Size mPreviewSize; // 预览大小
    private Size mPictureSize; // 拍照大小
    private int mDisplayRotation = 0; // 原始Sensor画面顺时针旋转该角度后，画面朝上
    private int mDeviceOrientation = 0; // 设备方向，由相机传感器获取

    /* 缩放相关 */
    private final int MAX_ZOOM = 200; // 放大的最大值，用于计算每次放大/缩小操作改变的大小
    private int mZoom = 0; // 0~mMaxZoom之间变化
    private float mStepWidth=30; // 每次改变的宽度大小
    private float mStepHeight=30; // 每次改变的高度大小

    /**
     * 打开摄像头的回调
     */
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(TAG, "onOpened");
            mCameraDevice = camera;
            // 初始化预览请求
            initPreviewRequest();
            // 创建预览会话
            createCommonSession();
        }

        // 摄像头被关闭
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.d(TAG, "onDisconnected");
            releaseCamera();
        }

        // 打开摄像头失败
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "Camera Open failed, error: " + error);
            releaseCamera();
        }
    };

    // 构造函数
    @TargetApi(Build.VERSION_CODES.M)
    public Camera2Proxy(Activity activity) {
        mActivity = activity;

        mOrientationEventListener = new OrientationEventListener(mActivity) {
            // 方向改变
            @Override
            public void onOrientationChanged(int orientation) {
                mDeviceOrientation = orientation;
            }
        };
    }

    // 设置预览输出尺寸
    public void setUpCameraOutputs(int width, int height) {
        mCameraManager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        try {
            mCameraCharacteristics = mCameraManager.getCameraCharacteristics(Integer.toString(mCameraId));
            StreamConfigurationMap map = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            Size[] supportPictureSizes = map.getOutputSizes(ImageFormat.JPEG);
            Size pictureSize = Collections.max(Arrays.asList(supportPictureSizes), new CompareSizesByArea());
            float aspectRatio = pictureSize.getHeight() * 1.0f / pictureSize.getWidth();
            Size[] supportPreviewSizes = map.getOutputSizes(SurfaceTexture.class);
            // 一般相机页面都是固定竖屏，宽是短边，所以根据view的宽度来计算需要的预览大小
            Size previewSize = chooseOptimalSize(supportPreviewSizes, width, aspectRatio);
            Log.d(TAG, "pictureSize: " + pictureSize);
            Log.d(TAG, "previewSize: " + previewSize);
            mPictureSize = pictureSize;
            mPreviewSize = previewSize;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // 打开摄像头
    @SuppressLint("MissingPermission")
    public void openCamera() {
        Log.v(TAG, "openCamera");
        // 开启后台线程
        startBackgroundThread(); // 对应 releaseCamera() 方法中的 stopBackgroundThread()
        // 注册方向监听器
        mOrientationEventListener.enable();
        try {
            // 获取相机属性
            mCameraCharacteristics = mCameraManager.getCameraCharacteristics(Integer.toString(mCameraId));
            // 每次切换摄像头计算一次就行，结果缓存到成员变量中
            // 计算预览方向
            initDisplayRotation();
            // 初始化缩放参数
            initZoomParameter();
            // 打开摄像头
            mCameraManager.openCamera(Integer.toString(mCameraId), mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // 初始化预览请求
    private void initDisplayRotation() {
        int displayRotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        switch (displayRotation) {
            case Surface.ROTATION_0:
                displayRotation = 90;
                break;
            case Surface.ROTATION_90:
                displayRotation = 0;
                break;
            case Surface.ROTATION_180:
                displayRotation = 270;
                break;
            case Surface.ROTATION_270:
                displayRotation = 180;
                break;
        }
        int sensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        mDisplayRotation = (displayRotation + sensorOrientation + 270) % 360;
        Log.d(TAG, "mDisplayRotation: " + mDisplayRotation);
    }

    // 计算最大放大倍数
    private void initZoomParameter() {
        Rect rect = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        Log.d(TAG, "sensor_info_active_array_size: " + rect);
        // max_digital_zoom 表示 active_rect 除以 crop_rect 的最大值
        float max_digital_zoom = mCameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        Log.d(TAG, "max_digital_zoom: " + max_digital_zoom);
        // crop_rect的最小宽高
        float minWidth = rect.width() / max_digital_zoom;
        float minHeight = rect.height() / max_digital_zoom;
        // 因为缩放时两边都要变化，所以要除以2
        mStepWidth = (rect.width() - minWidth) / MAX_ZOOM / 2;
        mStepHeight = (rect.height() - minHeight) / MAX_ZOOM / 2;
    }

    // 释放摄像头
    public void releaseCamera() {
        Log.v(TAG, "releaseCamera");
        if (null != mCaptureSession) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (mPictureImageReader != null) {
            mPictureImageReader.close();
            mPictureImageReader = null;
        }
        mOrientationEventListener.disable();
        stopBackgroundThread(); // 对应 openCamera() 方法中的 startBackgroundThread()
    }

    // 设置预览输出
    public void setPreviewSurface(SurfaceHolder holder) {
        mPreviewSurface = holder.getSurface();
    }
    // 设置预览输出
    public void setPreviewSurface(SurfaceTexture surfaceTexture) {
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        mPreviewSurface = new Surface(surfaceTexture);
    }
    // 设置图片输出尺寸
    private void createCommonSession() {
        List<Surface> outputs = new ArrayList<>();
        // preview output
        if (mPreviewSurface != null) {
            Log.d(TAG, "createCommonSession add target mPreviewSurface");
            outputs.add(mPreviewSurface);
        }
        // picture output
        Size pictureSize = mPictureSize;
        if (pictureSize != null) {
            Log.d(TAG, "createCommonSession add target mPictureImageReader");
            mPictureImageReader = ImageReader.newInstance(pictureSize.getWidth(), pictureSize.getHeight(), ImageFormat.JPEG, 1);
            outputs.add(mPictureImageReader.getSurface());
        }
        try {
            // 一个session中，所有CaptureRequest能够添加的target，必须是outputs的子集，所以在创建session的时候需要都添加进来
            mCameraDevice.createCaptureSession(outputs, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mCaptureSession = session;
                    startPreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "ConfigureFailed. session: " + session);
                }
            }, mBackgroundHandler); // handle 传入 null 表示使用当前线程的 Looper
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // 初始化预览请求
    private void initPreviewRequest() {
        if (mPreviewSurface == null) {
            Log.e(TAG, "initPreviewRequest failed, mPreviewSurface is null");
            return;
        }
        try {
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // 设置预览输出的 Surface
            mPreviewRequestBuilder.addTarget(mPreviewSurface);
            // 设置连续自动对焦
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 设置自动曝光
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            // 设置自动白平衡
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    // 开始预览
    public void startPreview() {
        Log.v(TAG, "startPreview");
        if (mCaptureSession == null || mPreviewRequestBuilder == null) {
            Log.w(TAG, "startPreview: mCaptureSession or mPreviewRequestBuilder is null");
            return;
        }
        try {
            // 开始预览，即一直发送预览的请求
            CaptureRequest captureRequest = mPreviewRequestBuilder.build();
            mCaptureSession.setRepeatingRequest(captureRequest, null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    // 停止预览
    public void stopPreview() {
        Log.v(TAG, "stopPreview");
        if (mCaptureSession == null) {
            Log.w(TAG, "stopPreview: mCaptureSession is null");
            return;
        }
        try {
            mCaptureSession.stopRepeating();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    // 拍照
    public void captureStillPicture(ImageReader.OnImageAvailableListener onImageAvailableListener) {
        if (mPictureImageReader == null) {
            Log.w(TAG, "captureStillPicture failed! mPictureImageReader is null");
            return;
        }
        mPictureImageReader.setOnImageAvailableListener(onImageAvailableListener, mBackgroundHandler);
        try {
            // 创建一个用于拍照的Request
            CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mPictureImageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation(mDeviceOrientation));
            // 预览如果有放大，拍照的时候也应该保存相同的缩放
            Rect zoomRect = mPreviewRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION);
            if (zoomRect != null) {
                captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);
            }
            stopPreview();
            mCaptureSession.abortCaptures();
            final long time = System.currentTimeMillis();
            mCaptureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    Log.w(TAG, "onCaptureCompleted, time: " + (System.currentTimeMillis() - time));
                    try {
                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                        mCaptureSession.capture(mPreviewRequestBuilder.build(), null, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                    startPreview();
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    // 获取图片的旋转角度
    private int getJpegOrientation(int deviceOrientation) {
        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN) return 0;
        int sensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90;
        // Reverse device orientation for front-facing cameras
        boolean facingFront = mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics
                .LENS_FACING_FRONT;
        if (facingFront) deviceOrientation = -deviceOrientation;
        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        int jpegOrientation = (sensorOrientation + deviceOrientation + 360) % 360;
        Log.d(TAG, "jpegOrientation: " + jpegOrientation);
        return jpegOrientation;
    }
    // 是否为前置摄像头
    public boolean isFrontCamera() {
        return mCameraId == CameraCharacteristics.LENS_FACING_BACK;
    }
    // 获取预览尺寸
    public Size getPreviewSize() {
        return mPreviewSize;
    }
    // 设置预览尺寸
    public void setPreviewSize(Size previewSize) {
        mPreviewSize = previewSize;
    }
    // 获取拍照尺寸
    public Size getPictureSize() {
        return mPictureSize;
    }
    // 设置拍照尺寸
    public void setPictureSize(Size pictureSize) {
        mPictureSize = pictureSize;
    }
    // 切换摄像头
    public void switchCamera() {
        mCameraId ^= 1;
        Log.d(TAG, "switchCamera: mCameraId: " + mCameraId);
        releaseCamera();
        openCamera();
    }
    // 处理缩放
    public void handleZoom(boolean isZoomIn,CameraDevice mCameraDevice,CameraCharacteristics mCameraCharacteristics,CaptureRequest.Builder mPreviewRequestBuilder) {
        if (mCameraDevice == null || mCameraCharacteristics == null || mPreviewRequestBuilder == null) {
            return;
        }

       // Toast.makeText(mActivity, "进入缩放", Toast.LENGTH_SHORT).show();
        Log.d("handleZoom", "handleZoom: " + isZoomIn);
        if (isZoomIn && (mZoom < MAX_ZOOM)) { // 放大
            //Toast.makeText(mActivity, "放大", Toast.LENGTH_SHORT).show();
            Log.d("handleZoom", "放大");
            mZoom++;
        } else if (mZoom > 0) { // 缩小
            //Toast.makeText(mActivity, "缩小", Toast.LENGTH_SHORT).show();
            Log.d("handleZoom", "缩小");
            mZoom--;
        }

        Log.d("handleZoom", "handleZoom: mZoom: " + mZoom);

        // 获取当前 crop 区域
        Rect rect = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        Log.d("handleZoom", "rect: " + rect);


        int cropW = (int) (mStepWidth * mZoom);
        int cropH = (int) (mStepHeight * mZoom);
        Log.d("handleZoom", "cropW: " + cropW + ", cropH: " + cropH);
        // 计算出裁剪区域
        Rect zoomRect = new Rect(rect.left + cropW, rect.top + cropH, rect.right - cropW, rect.bottom - cropH);

        Log.d("handleZoom", "zoomRect: " + zoomRect);
        // 设置裁剪区域
        mPreviewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);

        startPreview(); // 需要重新 start preview 才能生效
    }
    // 对焦
    public void triggerFocusAtPoint(float x, float y, int width, int height,CaptureRequest.Builder PreviewRequestBuilder,CameraCaptureSession CaptureSession) {
        Log.d(TAG, "triggerFocusAtPoint (" + x + ", " + y + ")");
        mPreviewRequestBuilder=PreviewRequestBuilder;
        mCaptureSession=CaptureSession;
        // 计算出在屏幕坐标系下的区域
        Rect cropRegion = mPreviewRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION);
        // 计算出在传感器坐标系下的区域
        MeteringRectangle afRegion = getAFAERegion(x, y, width, height, 1f, cropRegion);
        // ae的区域比af的稍大一点，聚焦的效果比较好
        MeteringRectangle aeRegion = getAFAERegion(x, y, width, height, 1.5f, cropRegion);
        // 设置对焦区域
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{afRegion});
        // 设置测光区域
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{aeRegion});
        // 设置对焦模式
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
        // 开始对焦
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
        // 开始预取
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);

        try {
            // 发送对焦请求
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mAfCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    // 获取对焦区域
    private MeteringRectangle getAFAERegion(float x, float y, int viewWidth, int viewHeight, float multiple, Rect cropRegion) {
        Log.v(TAG, "getAFAERegion enter");
        Log.d(TAG, "point: [" + x + ", " + y + "], viewWidth: " + viewWidth + ", viewHeight: " + viewHeight);
        Log.d(TAG, "multiple: " + multiple);
        // do rotate and mirror
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        Matrix matrix1 = new Matrix();
        matrix1.setRotate(mDisplayRotation);
        matrix1.postScale(isFrontCamera() ? -1 : 1, 1);
        matrix1.invert(matrix1);
        matrix1.mapRect(viewRect);
        // get scale and translate matrix
        Matrix matrix2 = new Matrix();
        RectF cropRect = new RectF(cropRegion);
        matrix2.setRectToRect(viewRect, cropRect, Matrix.ScaleToFit.CENTER);
        Log.d(TAG, "viewRect: " + viewRect);
        Log.d(TAG, "cropRect: " + cropRect);
        // get out region
        int side = (int) (Math.max(viewWidth, viewHeight) / 8 * multiple);
        RectF outRect = new RectF(x - side / 2, y - side / 2, x + side / 2, y + side / 2);
        Log.d(TAG, "outRect before: " + outRect);
        matrix1.mapRect(outRect);
        matrix2.mapRect(outRect);
        Log.d(TAG, "outRect after: " + outRect);
        // 做一个clamp，测光区域不能超出cropRegion的区域
        Rect meteringRect = new Rect((int) outRect.left, (int) outRect.top, (int) outRect.right, (int) outRect.bottom);
        meteringRect.left = clamp(meteringRect.left, cropRegion.left, cropRegion.right);
        meteringRect.top = clamp(meteringRect.top, cropRegion.top, cropRegion.bottom);
        meteringRect.right = clamp(meteringRect.right, cropRegion.left, cropRegion.right);
        meteringRect.bottom = clamp(meteringRect.bottom, cropRegion.top, cropRegion.bottom);
        Log.d(TAG, "meteringRegion: " + meteringRect);
        return new MeteringRectangle(meteringRect, 1000);
    }
    // 对焦回调
    private final CameraCaptureSession.CaptureCallback mAfCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) throws CameraAccessException {
            // 确保 result 不为 null
//            if (result == null) {
//                Log.e(TAG, "CaptureResult is null");
//                Toast.makeText(mActivity, "Result is null", Toast.LENGTH_SHORT).show();
//                return;
//            }
//            // 获取焦点状态
//            Integer state = result.get(CaptureResult.CONTROL_AF_STATE);
//            if (state == null) {
//                Log.e(TAG, "STATE is null");
//                Toast.makeText(mActivity, "STATE is null", Toast.LENGTH_SHORT).show();
//                return;
//            }
//            Log.d(TAG, "CONTROL_AF_STATE: " + state);
//
//            // 显示进入 Process 的提示
//            Toast.makeText(mActivity, "进入Process", Toast.LENGTH_SHORT).show();

            // 检查焦点状态
            //if (Objects.equals(state, CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED)
                    //|| Objects.equals(state, CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED)) {
               // Toast.makeText(mActivity, "对焦成功", Toast.LENGTH_SHORT).show();
                // 设置相关参数
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
                // 设置对焦模式
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                // 关闭自动曝光
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                // 启动预览
                startPreview();
          // } else {
                // 对焦失败，记录日志并提示用户
                //Log.w(TAG, "对焦失败，状态：" + state);
             //   Toast.makeText(mActivity, "对焦失败，请调整相机位置或光线", Toast.LENGTH_SHORT).show();
                // 尝试重新对焦
                //retryAutoFocus();
          //  }
        }

        private void retryAutoFocus() throws CameraAccessException {
            // 重新触发自动对焦
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
            // 提交新的请求
            //mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), this, null);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mAfCaptureCallback, mBackgroundHandler);
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

    // 开启后台线程
    private void startBackgroundThread() {
        if (mBackgroundThread == null || mBackgroundHandler == null) {
            Log.v(TAG, "startBackgroundThread");
            mBackgroundThread = new HandlerThread("CameraBackground");
            mBackgroundThread.start();
            mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        }
    }
    // 停止后台线程
    private void stopBackgroundThread() {
        Log.v(TAG, "stopBackgroundThread");
        if (mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    // 选择合适的尺寸
    public Size chooseOptimalSize(Size[] sizes, int dstSize, float aspectRatio) {
        if (sizes == null || sizes.length <= 0) {
            Log.e(TAG, "chooseOptimalSize failed, input sizes is empty");
            return null;
        }
        int minDelta = Integer.MAX_VALUE; // 最小的差值，初始值应该设置大点保证之后的计算中会被重置
        int index = 0; // 最小的差值对应的索引坐标
        for (int i = 0; i < sizes.length; i++) {
            Size size = sizes[i];
            // 先判断比例是否相等
            if (size.getWidth() * aspectRatio == size.getHeight()) {
                int delta = Math.abs(dstSize - size.getHeight());
                if (delta == 0) {
                    return size;
                }
                if (minDelta > delta) {
                    minDelta = delta;
                    index = i;
                }
            }
        }
        return sizes[index];
    }
    // 取值范围
    private int clamp(int x, int min, int max) {
        if (x > max) return max;
        if (x < min) return min;
        return x;
    }

    public void setFlashMode(int flashModeTorch) {
        mPreviewRequestBuilder= myapp.previewRequestBuilder;
        mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, flashModeTorch);
    }

    public int getFlashMode() {
        return mPreviewRequestBuilder.get(CaptureRequest.FLASH_MODE);
    }

    // 比较器
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // 我们在这里投放，以确保乘法不会溢出
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

}
