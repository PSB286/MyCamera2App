package com.psb.myapplication;

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
    private Size dPreviewSize;
    private CameraManager mCameraManager; // 相机管理者
    private CameraCharacteristics mCameraCharacteristics; // 相机属性
    private CameraCharacteristics dCameraCharacteristics;
    private CameraDevice mCameraDevice; // 相机对象
    private CameraDevice dCameraDevice;
    private CameraCaptureSession mCaptureSession;// 会话
    private CameraCaptureSession dCaptureSession;
    CaptureRequest.Builder mPreviewRequestBuilder; // 相机预览请求的构造器
    CaptureRequest.Builder dPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;// 预览请求
    private CaptureRequest dPreviewRequest;
    private Handler mBackgroundHandler;// 子线程的Handler
    private HandlerThread mBackgroundThread;// 子线程
    private ImageReader mImageReader;// 图片读取器
    private Surface mPreviewSurface;// 预览的Surface
    private OrientationEventListener mOrientationEventListener;

    private int mDisplayRotate = 0;// 屏幕方向
    private int mDeviceOrientation = 0; // 设备方向，由相机传感器获取
    int mZoom = 0; // 缩放

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

    // 构造函数
    @TargetApi(Build.VERSION_CODES.M)
    public Camera2Proxy(Activity activity) {
        mActivity = activity;
        // 获取摄像头管理者
        mCameraManager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        // 方向监听器
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

    // 设置预览输出的SurfaceTextures
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
        Log.v("--startPreview--", "startPreview");
        if (mCaptureSession == null || mPreviewRequestBuilder == null) {
            Log.w("--startPreview--", "startPreview: mCaptureSession or mPreviewRequestBuilder is null");
            return;
        }
        try {
            // 开始预览，即一直发送预览的请求
            mCaptureSession.setRepeatingRequest(mPreviewRequest, null, mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void startPreview(CameraCaptureSession mCaptureSession,CaptureRequest.Builder mPreviewRequestBuilder) {
        Log.v("--startPreview--", "startPreview");
        if (mCaptureSession == null || mPreviewRequestBuilder == null) {
            Log.w("--startPreview--", "startPreview: mCaptureSession or mPreviewRequestBuilder is null");
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
            // 创建拍照请求的构造器
            CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice
                    .TEMPLATE_STILL_CAPTURE);
            // 设置预览输出的Surface
            captureBuilder.addTarget(mImageReader.getSurface());
            // 设置自动对焦模式
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 设置自动曝光
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation(mDeviceOrientation));
            // 预览如果有放大，拍照的时候也应该保存相同的缩放
            Rect zoomRect = mPreviewRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION);
            // 设置预览输出的Surface
            if (zoomRect != null) {
                captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);
            }
            // 暂定预览
            mCaptureSession.stopRepeating();
            // 拍照
            mCaptureSession.abortCaptures();
            final long time = System.currentTimeMillis();
            // 设置拍照的回调
            mCaptureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    Log.w(TAG, "onCaptureCompleted, time: " + (System.currentTimeMillis() - time));
                    try {
                        // 取消对焦
                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata
                                .CONTROL_AF_TRIGGER_CANCEL);
                        // 发送预览的请求
                        mCaptureSession.capture(mPreviewRequestBuilder.build(), null, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                    // 重新开始预览
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
    Size chooseOptimalSize(Size[] sizes, int viewWidth, int viewHeight, Size pictureSize) {

//        // 1. 先找到与预览view的宽高比最接近的尺寸
//        int totalRotation = getRotation();// 获取屏幕旋转角度
//        // 2. 遍历所有尺寸，找到最接近的尺寸
//        boolean swapRotation = totalRotation == 90 || totalRotation == 270;// 是否需要交换宽高
//        // 3. 遍历所有尺寸，找到最接近的尺寸
//        int width = swapRotation ? viewHeight : viewWidth;// 宽
//        // 4. 遍历所有尺寸，找到最接近的尺寸
//        int height = swapRotation ? viewWidth : viewHeight;// 高
        // 5. 遍历所有尺寸，找到最接近的尺寸
        return getSuitableSize(sizes, viewWidth, viewHeight, pictureSize);// 返回最接近的尺寸
    }

    // 获取旋转角度
    private int getRotation() {
        // 获取屏幕旋转角度
        int displayRotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
       // 屏幕旋转角度转成90度的倍数
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
        Log.d("--getSuitableSize--", "getSuitableSize. aspectRatio: " + aspectRatio);
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
    public void handleZoom(boolean isZoomIn,CameraDevice CameraDevice,CameraCharacteristics CameraCharacteristics,CaptureRequest.Builder PreviewRequestBuilder,CaptureRequest PreviewRequest,CameraCaptureSession mCaptureSession) {
        mCameraDevice=CameraDevice;
        mCameraCharacteristics=CameraCharacteristics;
        mPreviewRequestBuilder=PreviewRequestBuilder;
        mPreviewRequest=PreviewRequest;


        if (mCameraDevice == null || mCameraCharacteristics == null || mPreviewRequestBuilder == null) {
           Log.v("--Zoom--","mCameraDevice:"+mCameraDevice+" mCameraCharacteristics:"+mCameraCharacteristics+" mPreviewRequestBuilder:"+mPreviewRequestBuilder);
            return;
        }

        // maxZoom 表示 active_rect 宽度除以 crop_rect 宽度的最大值
        float maxZoom = mCameraCharacteristics.get(android.hardware.camera2.CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        Log.d("--Zoom--", "handleZoom: maxZoom: " + maxZoom);
        int factor = 100; // 放大/缩小的一个因素，设置越大越平滑，相应放大的速度也越慢
        if (isZoomIn && mZoom < factor) {
            mZoom++;
        } else if (mZoom > 0) {
            mZoom--;
        }
        Log.d("--Zoom--", "handleZoom: mZoom: " + mZoom);
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
        if(mCaptureSession==null||mPreviewRequestBuilder==null)
        {
            Log.d("--Zoom--","mCaptureSession:"+mCaptureSession+" mPreviewRequestBuilder:"+mPreviewRequestBuilder);
        }
        startPreview(mCaptureSession,mPreviewRequestBuilder); // 需要重新 start preview 才能生效
    }

    int i=0;
    // 对焦
    public void focusOnPoint(double x, double y, int width, int height,CameraDevice CameraDevice,CaptureRequest.Builder PreviewRequestBuilder,CameraCaptureSession CaptureSession,Size PreviewSize,AutoFitTextureView mTextureView) {

        dCameraDevice=CameraDevice;
        dPreviewRequestBuilder=PreviewRequestBuilder;
        dCaptureSession=CaptureSession;
        dPreviewSize=PreviewSize;

        mCameraDevice=dCameraDevice;
        mPreviewRequestBuilder=dPreviewRequestBuilder;
        mPreviewSize=dPreviewSize;
        mCaptureSession = dCaptureSession;

        // 判断是否已经打开摄像头
        if (mCameraDevice == null || mPreviewRequestBuilder == null) {
            return;
        }

        // 1. 先取相对于view上面的坐标
        int previewWidth = mPreviewSize.getWidth();
        int previewHeight = mPreviewSize.getHeight();
        Log.d("--focusOnPoint--", "focusOnPoint: previewWidth: " + previewWidth + ", previewHeight: " + previewHeight);
        if (mDisplayRotate == 90 || mDisplayRotate == 270) {
            previewWidth = mPreviewSize.getHeight();
            previewHeight = mPreviewSize.getWidth();
        }
        // 2. 计算摄像头取出的图像相对于view放大了多少，以及有多少偏移
        double tmp;// 临时变量
        double imgScale;// 图像的放大倍数
        double verticalOffset = 0;// 垂直偏移
        double horizontalOffset = 0;// 水平偏移

        if (previewHeight * width > previewWidth * height) {
            imgScale = width * 1.0 / previewWidth;// 图像放大的倍数
            verticalOffset = (previewHeight - height / imgScale) / 2;// 垂直偏移
        } else {
            imgScale = height * 1.0 / previewHeight;// 图像放大的倍数
            horizontalOffset = (previewWidth - width / imgScale) / 2;// 水平偏移
        }
        Log.d("--focusOnPoint--", "focusOnPoint: imgScale: " + imgScale + ", verticalOffset: " + verticalOffset + ", horizontalOffset: " + horizontalOffset);

        // 3. 将点击的坐标转换为图像上的坐标
        x = x / imgScale + horizontalOffset;// 图像x坐标
        y = y / imgScale + verticalOffset;// 图像y坐标
        // 图像坐标旋转
        if (90 == mDisplayRotate) {
            tmp = x;
            x = y;
            y = mPreviewSize.getHeight() - tmp;
        }// 旋转
        else if (270 == mDisplayRotate) {
            tmp = x;
            x = mPreviewSize.getWidth() - y;
            y = tmp;
        }
        Log.d("--focusOnPoint--", "focusOnPoint: x: " + x + ", y: " + y);


        // 4. 计算取到的图像相对于裁剪区域的缩放系数，以及位移
        Rect cropRegion = mPreviewRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION);
        // 裁剪区域
        if (cropRegion == null) {
            Log.w(TAG, "can't get crop region");
            // 默认取全部区域
            cropRegion = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        }
        //获取区域宽高
        int cropWidth = cropRegion.width();
        int cropHeight = cropRegion.height();
        // 图像坐标旋转
        if (mPreviewSize.getHeight() * cropWidth > mPreviewSize.getWidth() * cropHeight) {
            // 图像放大的倍数
            imgScale = cropHeight * 1.0 / mPreviewSize.getHeight();
            verticalOffset = 0;
            // 水平偏移
            horizontalOffset = (cropWidth - imgScale * mPreviewSize.getWidth()) / 2;
        } else {
            // 图像放大的倍数
            imgScale = cropWidth * 1.0 / mPreviewSize.getWidth();
            horizontalOffset = 0;
            // 垂直偏移
            verticalOffset = (cropHeight - imgScale * mPreviewSize.getHeight()) / 2;
        }
        Log.d("--focusOnPoint--", "focusOnPoint: cropWidth: " + cropWidth + ", cropHeight: " + cropHeight + ", imgScale: " + imgScale + ", verticalOffset: " + verticalOffset + ", horizontalOffset: " + horizontalOffset);
        // 5. 将点击区域相对于图像的坐标，转化为相对于成像区域的坐标（点击的中心坐标）
        x = x * imgScale + horizontalOffset + cropRegion.left;
        y = y * imgScale + verticalOffset + cropRegion.top;
        double tapAreaRatio = 0.1;// 点击区域相对于裁剪区域的比例大小
        //计算出点击区域相对于 crop_rect 的坐标（）
        Rect rect = new Rect();
        rect.left = clamp((int) (x - tapAreaRatio / 2 * cropRegion.width()), 0, cropRegion.width());
        rect.right = clamp((int) (x + tapAreaRatio / 2 * cropRegion.width()), 0, cropRegion.width());
        rect.top = clamp((int) (y - tapAreaRatio / 2 * cropRegion.height()), 0, cropRegion.height());
        rect.bottom = clamp((int) (y + tapAreaRatio / 2 * cropRegion.height()), 0, cropRegion.height());

        rect.set(360-50,360-50,360+50,360+50);

        // 6. 设置 AF、AE 的测光区域，即上述得到的 rect
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{new MeteringRectangle
                (rect, 1000)});
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{new MeteringRectangle
                (rect, 1000)});

        // 设置对焦模式为 自动
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
        // 设置对焦模式为 连续对焦
        //mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        // 开启自动对焦触发
       // mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
        //mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
        // 开启自动曝光触发
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata
               .CONTROL_AE_PRECAPTURE_TRIGGER_START);
        try {
            // 7. 发送上述设置的对焦请求，并监听回调
            mCaptureSession.capture(mPreviewRequestBuilder.build(), null, mBackgroundHandler);

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
            Log.d(TAG, "--onCaptureCompleted--: CONTROL_AF_STATE: " + state);
            if (state == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED || state == CaptureResult
                    .CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                Log.d("--onCaptureCompleted--", "--process--: start normal preview");

               // mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
               // mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest
               //         .CONTROL_AF_MODE_CONTINUOUS_PICTURE);
               // mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.FLASH_MODE_OFF);
                mPreviewRequest=mPreviewRequestBuilder.build();
                startPreview(mCaptureSession,mPreviewRequestBuilder);
            }
        }
        /**
         * 当相机捕获进度发生变化时进行调用
         * 这个方法主要用于处理对焦过程中的回调信息
         *
         * @param session 正在进行捕获的相机捕获会话
         * @param request 发出此捕获请求的请求对象
         * @param partialResult 包含捕获进度部分结果的对象
         */
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            Log.d("--onCaptureCompleted--", "--onCaptureProgressed--"+"捕获状态发生改变");
            // 对焦回调
            process(partialResult);
        }
        /**
         * 当相机捕获完成时进行调用
         * 这个方法主要用于处理捕获完成后的结果
         *
         * @param session 正在进行捕获的相机捕获会话
         * @param request 发出此捕获请求的请求对象
         * @param result 包含捕获结果的对象
         */
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
            Log.d("--onCaptureCompleted--", "--onCaptureCompleted--: 捕获完成");
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
