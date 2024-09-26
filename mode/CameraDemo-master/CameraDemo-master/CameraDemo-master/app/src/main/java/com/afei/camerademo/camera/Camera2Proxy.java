package com.afei.camerademo.camera;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
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

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class Camera2Proxy {

    private static final String TAG = "Camera2Proxy";
    // Activity
    private Activity mActivity;

    private int mCameraId = CameraCharacteristics.LENS_FACING_FRONT; // 要打开的摄像头ID
    private Size mPreviewSize; // 预览大小
    private CameraManager mCameraManager; // 相机管理者
    private CameraCharacteristics mCameraCharacteristics; // 相机属性
    private CameraDevice mCameraDevice; // 相机对象
    private CameraCaptureSession mCaptureSession;// 会话
    private CaptureRequest.Builder mPreviewRequestBuilder; // 相机预览请求的构造器
    private CaptureRequest mPreviewRequest;// 预览请求
    private Handler mBackgroundHandler;// 子线程的Handler
    private HandlerThread mBackgroundThread;// 子线程
    private ImageReader mImageReader;// 图片读取器
    private Surface mPreviewSurface;// 预览的Surface
    private OrientationEventListener mOrientationEventListener;

    private int mDisplayRotate = 0;// 屏幕方向
    private int mDeviceOrientation = 0; // 设备方向，由相机传感器获取
    private int mZoom = 0; // 缩放

    /**
     * 打开摄像头的回调
     */
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
       // 摄像头打开成功
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(TAG, "onOpened");
            mCameraDevice = camera;
            initPreviewRequest();
        }
        // 摄像头打开失败
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.d(TAG, "onDisconnected");
            releaseCamera();
        }
        // 摄像头打开失败
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "Camera Open failed, error: " + error);
            releaseCamera();
        }
    };

    @TargetApi(Build.VERSION_CODES.M)
    public Camera2Proxy(Activity activity) {
        mActivity = activity;
        mCameraManager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        mOrientationEventListener = new OrientationEventListener(mActivity) {
            @Override
            public void onOrientationChanged(int orientation) {
                mDeviceOrientation = orientation;
            }
        };
    }

    @SuppressLint("MissingPermission")
    public void openCamera(int width, int height) {
        Log.v(TAG, "openCamera");
        startBackgroundThread(); // 对应 releaseCamera() 方法中的 stopBackgroundThread()
        // 注册方向传感器
        mOrientationEventListener.enable();
        try {
            // 获取摄像头属性
            mCameraCharacteristics = mCameraManager.getCameraCharacteristics(Integer.toString(mCameraId));
            // 获取支持的预览输出尺寸
            StreamConfigurationMap map = mCameraCharacteristics.get(CameraCharacteristics
                    .SCALER_STREAM_CONFIGURATION_MAP);
            // 拍照大小，选择能支持的一个最大的图片大小
            Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
            Log.d(TAG, "picture size: " + largest.getWidth() + "*" + largest.getHeight());
            // 图片读取器，用于获取拍照的图片
            mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, 2);
            // 预览大小，根据上面选择的拍照图片的长宽比，选择一个和控件长宽差不多的大小
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, largest);
            Log.d(TAG, "preview size: " + mPreviewSize.getWidth() + "*" + mPreviewSize.getHeight());
            // 打开摄像头
            mCameraManager.openCamera(Integer.toString(mCameraId), mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

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
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
        mOrientationEventListener.disable();
        stopBackgroundThread(); // 对应 openCamera() 方法中的 startBackgroundThread()
    }

    public void setImageAvailableListener(ImageReader.OnImageAvailableListener onImageAvailableListener) {
        if (mImageReader == null) {
            Log.w(TAG, "setImageAvailableListener: mImageReader is null");
            return;
        }
        mImageReader.setOnImageAvailableListener(onImageAvailableListener, null);
    }

    public void setPreviewSurface(SurfaceHolder holder) {
        mPreviewSurface = holder.getSurface();
    }

    // 设置预览输出的SurfaceTexture
    public void setPreviewSurface(SurfaceTexture surfaceTexture) {
        // 设置预览输出的SurfaceTexture的默认缓冲区大小
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        // 创建一个 Surface
        mPreviewSurface = new Surface(surfaceTexture);
    }

    private void initPreviewRequest() {
        try {
            // 创建预览请求的构造器
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // 添加预览输出的 Surface
            mPreviewRequestBuilder.addTarget(mPreviewSurface); // 设置预览输出的 Surface
            // 创建会话，并设置预览输出的 Surface 和拍照输出的 ImageReader 的 Surface
            mCameraDevice.createCaptureSession(Arrays.asList(mPreviewSurface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        // 会话创建成功
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            mCaptureSession = session;
                            // 设置连续自动对焦
                            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest
                                    .CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                            // 设置自动曝光
                            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest
                                    .CONTROL_AE_MODE_ON_AUTO_FLASH);
                            // 设置完后自动开始预览
                            mPreviewRequest = mPreviewRequestBuilder.build();
                            // 开始预览
                            startPreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.e(TAG, "ConfigureFailed. session: mCaptureSession");
                        }
                    }, mBackgroundHandler); // handle 传入 null 表示使用当前线程的 Looper
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void startPreview() {
        Log.v(TAG, "startPreview");
        if (mCaptureSession == null || mPreviewRequestBuilder == null) {
            Log.w(TAG, "startPreview: mCaptureSession or mPreviewRequestBuilder is null");
            return;
        }
        try {
            // 开始预览，即一直发送预览的请求
            mCaptureSession.setRepeatingRequest(mPreviewRequest, null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void stopPreview() {
        Log.v(TAG, "stopPreview");
        if (mCaptureSession == null || mPreviewRequestBuilder == null) {
            Log.w(TAG, "stopPreview: mCaptureSession or mPreviewRequestBuilder is null");
            return;
        }
        try {
            mCaptureSession.stopRepeating();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void captureStillPicture() {
        try {
            CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice
                    .TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation(mDeviceOrientation));
            // 预览如果有放大，拍照的时候也应该保存相同的缩放
            Rect zoomRect = mPreviewRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION);
            if (zoomRect != null) {
                captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);
            }
            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            final long time = System.currentTimeMillis();
            mCaptureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    Log.w(TAG, "onCaptureCompleted, time: " + (System.currentTimeMillis() - time));
                    try {
                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata
                                .CONTROL_AF_TRIGGER_CANCEL);
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

    // 判断是否为前置摄像头
    public boolean isFrontCamera() {
        return mCameraId == CameraCharacteristics.LENS_FACING_BACK;
    }

    // 获取预览尺寸
    public Size getPreviewSize() {
        return mPreviewSize;
    }

    // 切换摄像头
    public void switchCamera(int width, int height) {
        mCameraId ^= 1;
        Log.d(TAG, "switchCamera: mCameraId: " + mCameraId);
        releaseCamera();
        openCamera(width, height);
    }

    // 选择合适的预览尺寸
    private Size chooseOptimalSize(Size[] sizes, int viewWidth, int viewHeight, Size pictureSize) {
        // 1. 先找到与预览view的宽高比最接近的尺寸
        int totalRotation = getRotation();
        // 2. 遍历所有尺寸，找到最接近的尺寸
        boolean swapRotation = totalRotation == 90 || totalRotation == 270;
        // 3. 遍历所有尺寸，找到最接近的尺寸
        int width = swapRotation ? viewHeight : viewWidth;
        // 4. 遍历所有尺寸，找到最接近的尺寸
        int height = swapRotation ? viewWidth : viewHeight;
        // 5. 遍历所有尺寸，找到最接近的尺寸
        return getSuitableSize(sizes, width, height, pictureSize);
    }

    // 获取旋转角度
    private int getRotation() {
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
        // 获取屏幕旋转角度
        int sensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        // 计算出最终的旋转角度
        mDisplayRotate = (displayRotation + sensorOrientation + 270) % 360;
        // Log.d(TAG, "getRotation: displayRotation: " + displayRotation + ", sensorOrientation: " + sensorOrientation + ", mDisplayRotate: " + mDisplayRotate);
        return mDisplayRotate;
    }

    // 选择合适的尺寸
    private Size getSuitableSize(Size[] sizes, int width, int height, Size pictureSize) {
        int minDelta = Integer.MAX_VALUE; // 最小的差值，初始值应该设置大点保证之后的计算中会被重置
        int index = 0; // 最小的差值对应的索引坐标
        // 先找到与预览view的宽高比最接近的尺寸
        float aspectRatio = pictureSize.getHeight() * 1.0f / pictureSize.getWidth();
        Log.d(TAG, "getSuitableSize. aspectRatio: " + aspectRatio);
        // 遍历所有尺寸，找到最接近的尺寸
        for (int i = 0; i < sizes.length; i++) {
            Size size = sizes[i];
            // 先判断比例是否相等
            if (size.getWidth() * aspectRatio == size.getHeight()) {
                int delta = Math.abs(width - size.getWidth());
                if (delta == 0) {
                    return size;
                }
                if (minDelta > delta) {
                    minDelta = delta;
                    index = i;
                }
            }
        }
        // 返回最接近的尺寸
        return sizes[index];
    }

    // 放大缩小
    public void handleZoom(boolean isZoomIn) {
        if (mCameraDevice == null || mCameraCharacteristics == null || mPreviewRequestBuilder == null) {
            return;
        }
        // maxZoom 表示 active_rect 宽度除以 crop_rect 宽度的最大值
        float maxZoom = mCameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        Log.d(TAG, "handleZoom: maxZoom: " + maxZoom);
        int factor = 100; // 放大/缩小的一个因素，设置越大越平滑，相应放大的速度也越慢
        if (isZoomIn && mZoom < factor) {
            mZoom++;
        } else if (mZoom > 0) {
            mZoom--;
        }
        Log.d(TAG, "handleZoom: mZoom: " + mZoom);
        // 计算出 crop_rect
        Rect rect = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        // 计算出 crop_rect 的最小值
        int minW = (int) ((rect.width() - rect.width() / maxZoom) / (2 * factor));
        // 计算出 crop_rect 的最大值
        int minH = (int) ((rect.height() - rect.height() / maxZoom) / (2 * factor));
        // 计算出 crop_rect 的放大缩小值
        int cropW = minW * mZoom;
        // 计算出 crop_rect 的放大缩小值
        int cropH = minH * mZoom;
        // 计算出 crop_rect
        Log.d(TAG, "handleZoom: cropW: " + cropW + ", cropH: " + cropH);
        // 计算出 crop_rect
        Rect zoomRect = new Rect(rect.left + cropW, rect.top + cropH, rect.right - cropW, rect.bottom - cropH);
        // 设置 crop_rect
        mPreviewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);
        // 提交设置
        mPreviewRequest = mPreviewRequestBuilder.build();
        startPreview(); // 需要重新 start preview 才能生效
    }

    // 对焦
    public void focusOnPoint(double x, double y, int width, int height) {
        // 判断是否已经打开摄像头
        if (mCameraDevice == null || mPreviewRequestBuilder == null) {
            return;
        }
        // 1. 先取相对于view上面的坐标
        int previewWidth = mPreviewSize.getWidth();
        int previewHeight = mPreviewSize.getHeight();
        if (mDisplayRotate == 90 || mDisplayRotate == 270) {
            previewWidth = mPreviewSize.getHeight();
            previewHeight = mPreviewSize.getWidth();
        }
        // 2. 计算摄像头取出的图像相对于view放大了多少，以及有多少偏移
        double tmp;
        double imgScale;
        double verticalOffset = 0;
        double horizontalOffset = 0;
        if (previewHeight * width > previewWidth * height) {
            imgScale = width * 1.0 / previewWidth;
            verticalOffset = (previewHeight - height / imgScale) / 2;
        } else {
            imgScale = height * 1.0 / previewHeight;
            horizontalOffset = (previewWidth - width / imgScale) / 2;
        }
        // 3. 将点击的坐标转换为图像上的坐标
        x = x / imgScale + horizontalOffset;
        y = y / imgScale + verticalOffset;
        if (90 == mDisplayRotate) {
            tmp = x;
            x = y;
            y = mPreviewSize.getHeight() - tmp;
        } else if (270 == mDisplayRotate) {
            tmp = x;
            x = mPreviewSize.getWidth() - y;
            y = tmp;
        }
        // 4. 计算取到的图像相对于裁剪区域的缩放系数，以及位移
        Rect cropRegion = mPreviewRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION);
        if (cropRegion == null) {
            Log.w(TAG, "can't get crop region");
            cropRegion = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        }
        int cropWidth = cropRegion.width();
        int cropHeight = cropRegion.height();
        if (mPreviewSize.getHeight() * cropWidth > mPreviewSize.getWidth() * cropHeight) {
            imgScale = cropHeight * 1.0 / mPreviewSize.getHeight();
            verticalOffset = 0;
            horizontalOffset = (cropWidth - imgScale * mPreviewSize.getWidth()) / 2;
        } else {
            imgScale = cropWidth * 1.0 / mPreviewSize.getWidth();
            horizontalOffset = 0;
            verticalOffset = (cropHeight - imgScale * mPreviewSize.getHeight()) / 2;
        }
        // 5. 将点击区域相对于图像的坐标，转化为相对于成像区域的坐标
        x = x * imgScale + horizontalOffset + cropRegion.left;
        y = y * imgScale + verticalOffset + cropRegion.top;
        double tapAreaRatio = 0.1;
        Rect rect = new Rect();
        rect.left = clamp((int) (x - tapAreaRatio / 2 * cropRegion.width()), 0, cropRegion.width());
        rect.right = clamp((int) (x + tapAreaRatio / 2 * cropRegion.width()), 0, cropRegion.width());
        rect.top = clamp((int) (y - tapAreaRatio / 2 * cropRegion.height()), 0, cropRegion.height());
        rect.bottom = clamp((int) (y + tapAreaRatio / 2 * cropRegion.height()), 0, cropRegion.height());
        // 6. 设置 AF、AE 的测光区域，即上述得到的 rect
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{new MeteringRectangle
                (rect, 1000)});
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{new MeteringRectangle
                (rect, 1000)});
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata
                .CONTROL_AE_PRECAPTURE_TRIGGER_START);
        try {
            // 7. 发送上述设置的对焦请求，并监听回调
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mAfCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // 对焦回调
    private final CameraCaptureSession.CaptureCallback mAfCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        // 对焦回调
        private void process(CaptureResult result) {
            // 获取对焦状态
            Integer state = result.get(CaptureResult.CONTROL_AF_STATE);
            // 对焦状态为空，直接返回
            if (null == state) {
                return;
            }
            // 对焦状态为对焦完成，开始正常预览
            Log.d(TAG, "process: CONTROL_AF_STATE: " + state);
            if (state == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED || state == CaptureResult
                    .CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                Log.d(TAG, "process: start normal preview");
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest
                        .CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.FLASH_MODE_OFF);
                startPreview();
            }
        }
        //queueIdle
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            // 对焦回调
            process(partialResult);
        }
        // queueIdle
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
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
    // 对焦区域坐标限制
    private int clamp(int x, int min, int max) {
        if (x > max) return max;
        if (x < min) return min;
        return x;
    }

    // 获取最佳的尺寸
    static class CompareSizesByArea implements Comparator<Size> {
            // 降序
        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

}
