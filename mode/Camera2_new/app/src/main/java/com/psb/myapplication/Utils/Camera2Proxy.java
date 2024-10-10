package com.psb.myapplication.Utils;


import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.MeteringRectangle;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.Surface;

import com.psb.myapplication.View.AutoFitTextureView;

public class Camera2Proxy {

    private static final String TAG = "Camera2Proxy";                // TAG
    private Activity mActivity;                                      // Activity
    private int mCameraId = CameraCharacteristics.LENS_FACING_FRONT; // 要打开的摄像头ID
    private Size mPreviewSize;                                       // 预览大小
    private Size dPreviewSize;                                       // 对焦预览大小
    private CameraManager mCameraManager;                            // 相机管理者
    CameraCharacteristics mCameraCharacteristics;                    // 相机属性
    CameraDevice mCameraDevice;                                      // 相机对象
    private CameraDevice dCameraDevice;                              // 对焦相机对象
    private CameraCaptureSession mCaptureSession;                    // 会话
    private CameraCaptureSession dCaptureSession;                    // 对焦会话
    public CaptureRequest.Builder mPreviewRequestBuilder;                   // 相机预览请求的构造器
    CaptureRequest.Builder dPreviewRequestBuilder;                   // 对焦预览请求的构造器
    CaptureRequest mPreviewRequest;                                  // 预览请求
    private Handler mBackgroundHandler;                              // 子线程的Handler
    private HandlerThread mBackgroundThread;                         // 子线程
    private ImageReader mImageReader;                                // 图片读取器
    private Surface mPreviewSurface;                                 // 预览的Surface
    private OrientationEventListener mOrientationEventListener;      // 方向监听器
    private int mDisplayRotate = 0;                                  // 屏幕方向
    private int mDeviceOrientation = 0;                              // 设备方向，由相机传感器获取
    public int mZoom = 0;                                                   // 缩放


    // 构造函数
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

    /**
     * 开始相机预览
     *
     * 本方法用于启动相机的预览功能，它通过向相机系统发送重复的请求来实现持续的预览
     * 如果启动预览所需的对象未初始化（如mCaptureSession或mPreviewRequestBuilder为null），
     * 则记录警告并直接返回，不启动预览
     */
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

    /**
     * 开始预览相机画面
     * 该方法用于在相机捕获会话中重复发送预览请求，以实现持续的相机预览功能
     *
     * @param mCaptureSession 相机捕获会话对象，用于管理相机的捕获过程
     * @param mPreviewRequestBuilder 预览请求构建器对象，用于构建相机预览的请求
     */
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

    /**
     * 处理缩放操作
     *
     * @param isZoomIn 是否放大，true为放大，false为缩小
     * @param CameraDevice 相机设备对象
     * @param CameraCharacteristics 相机特性对象，用于获取相机相关信息
     * @param PreviewRequestBuilder 预览请求构建器，用于设置相机预览参数
     * @param PreviewRequest 预览请求对象
     * @param mCaptureSession 相机捕获会话，用于管理相机捕获操作
     */
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
        Rect rect = mCameraCharacteristics.get(android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
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
    /**
     * 设置对焦区域和测光区域，启动预览
     *
     * @param CameraDevice 相机设备对象
     * @param PreviewRequestBuilder 预览请求构建器
     * @param CaptureSession 捕获会话对象
     * @param PreviewSize 预览尺寸
     * @param x 点击点的X坐标
     * @param y 点击点的Y坐标
     * @param width 预览视图的宽度
     * @param height 预览视图的高度
     * @param CaptureSession 相机特性信息
     * @param mTextureView 用于显示相机预览的纹理视图
     */
    public void focusOnPoint(double x, double y, int width, int height,
                             CameraDevice CameraDevice,
                             CaptureRequest.Builder PreviewRequestBuilder,
                             CameraCaptureSession CaptureSession,
                             Size PreviewSize,
                             AutoFitTextureView mTextureView) {

        dCameraDevice = CameraDevice;
        dPreviewRequestBuilder = PreviewRequestBuilder;
        dCaptureSession = CaptureSession;
        dPreviewSize = PreviewSize;

        mCameraDevice = dCameraDevice;
        mPreviewRequestBuilder = dPreviewRequestBuilder;
        mPreviewSize = dPreviewSize;
        mCaptureSession = dCaptureSession;

        // 判断是否已经打开摄像头
        if (mCameraDevice == null || mPreviewRequestBuilder == null) {
            return;
        }

        Log.d("-focusOnPoint-", "focusOnPoint2: x:" + x + " y:" + y);
       // Toast.makeText(mActivity, "对焦x:" + x + " y:" + y, Toast.LENGTH_SHORT).show();

        // 1. 先取相对于view上面的坐标
        int previewWidth = mPreviewSize.getWidth();
        int previewHeight = mPreviewSize.getHeight();

        Log.d("--focusOnPoint--", "focusOnPoint: previewWidth: " + previewWidth + ", previewHeight: " + previewHeight);

        if (mDisplayRotate == 90 || mDisplayRotate == 270) {
            previewWidth = mPreviewSize.getHeight();
            previewHeight = mPreviewSize.getWidth();
        }

        // 2. 计算摄像头取出的图像相对于view放大了多少，以及有多少偏移
        double imgScale; // 图像的放大倍数
        double verticalOffset = 0; // 垂直偏移
        double horizontalOffset = 0; // 水平偏移

        if (previewHeight * width > previewWidth * height) {
            imgScale = width * 1.0 / previewWidth; // 图像放大的倍数
            verticalOffset = (previewHeight - height / imgScale) / 2; // 垂直偏移
        } else {
            imgScale = height * 1.0 / previewHeight; // 图像放大的倍数
            horizontalOffset = (previewWidth - width / imgScale) / 2; // 水平偏移
        }

        Log.d("--focusOnPoint--", "focusOnPoint: imgScale: " + imgScale + ", verticalOffset: " + verticalOffset + ", horizontalOffset: " + horizontalOffset);

        // 3. 将点击的坐标转换为图像上的坐标
     //   x = x / imgScale + horizontalOffset; // 图像x坐标
     //   y = y / imgScale + verticalOffset; // 图像y坐标

        // 图像坐标旋转
        if (mDisplayRotate == 90) {
            double tmp = x;
            x = y;
            y = mPreviewSize.getHeight() - tmp;
        } else if (mDisplayRotate == 270) {
            double tmp = x;
            x = mPreviewSize.getWidth() - y;
            y = tmp;
        }

        Log.d("--focusOnPoint--", "focusOnPoint: x: " + x + ", y: " + y);

        // 4. 计算取到的图像相对于裁剪区域的缩放系数，以及位移
        Rect cropRegion = mPreviewRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION);

        if (cropRegion == null) {
            Log.w(TAG, "can't get crop region");
            cropRegion = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        }

        int cropWidth = cropRegion.width();
        int cropHeight = cropRegion.height();

        if (mPreviewSize.getHeight() * cropWidth > mPreviewSize.getWidth() * cropHeight) {
            imgScale = cropHeight * 1.0 / mPreviewSize.getHeight(); // 图像放大的倍数
            verticalOffset = 0;
            horizontalOffset = (cropWidth - imgScale * mPreviewSize.getWidth()) / 2; // 水平偏移
        } else {
            imgScale = cropWidth * 1.0 / mPreviewSize.getWidth(); // 图像放大的倍数
            horizontalOffset = 0;
            verticalOffset = (cropHeight - imgScale * mPreviewSize.getHeight()) / 2; // 垂直偏移
        }

        Log.d("--focusOnPoint--", "focusOnPoint: cropWidth: " + cropWidth + ", cropHeight: " + cropHeight + ", imgScale: " + imgScale + ", verticalOffset: " + verticalOffset + ", horizontalOffset: " + horizontalOffset);

        // 5. 计算点击区域相对于裁剪区域的坐标
        double tapAreaRatio = 0.1; // 点击区域相对于裁剪区域的比例大小
        Rect rect = new Rect();
        rect.left = clamp((int) (x - tapAreaRatio / 5 * cropRegion.width()), 0, cropRegion.width());
        rect.right = clamp((int) (x + tapAreaRatio / 5 * cropRegion.width()), 0, cropRegion.width());
        rect.top = clamp((int) (y - tapAreaRatio / 5 * cropRegion.height()), 0, cropRegion.height());
        rect.bottom = clamp((int) (y + tapAreaRatio / 5 * cropRegion.height()), 0, cropRegion.height());
        rect=new Rect(360-50,360+50,360-50,360+50);

        // 6. 设置 AF、AE 的测光区域，即上述得到的 rect
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{new MeteringRectangle(rect, 1000)});
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{new MeteringRectangle(rect, 1000)});

        // 设置对焦模式为 自动
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
        // 设置自动对焦触发
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
        //设置连续模式
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        // 触发自动对焦
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);

        // 设置自动曝光模式为自动
       // mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
        // 设置自动曝光模式为自动
        //mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        // 开启自动曝光触发
        //mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);

        try {
            // 7. 发送上述设置的对焦请求，并监听回调
            mCaptureSession.capture(mPreviewRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        mPreviewRequest = mPreviewRequestBuilder.build();
         startPreview();
    }

    /**
     * 对焦区域坐标限制
     *
     * @param x 需要限制的坐标值
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
