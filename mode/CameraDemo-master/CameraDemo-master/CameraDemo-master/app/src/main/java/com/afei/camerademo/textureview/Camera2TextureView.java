package com.afei.camerademo.textureview;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;

import com.afei.camerademo.camera.Camera2Proxy;

public class Camera2TextureView extends TextureView {

    private static final String TAG = "CameraTextureView";
    private Camera2Proxy mCameraProxy;
    private int mRatioWidth = 0;
    private int mRatioHeight = 0;
    private float mOldDistance;

    //构造器
    public Camera2TextureView(Context context) {
        this(context, null);
    }
    //构造器
    public Camera2TextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    //构造器
    public Camera2TextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    //构造器
    public Camera2TextureView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }
    /**
     * 初始化方法
     * 在这个方法中，我们为当前对象设置了一个表面纹理监听器，并且创建了一个相机代理对象
     * 这里的目的是为了后续能够更好地管理和使用相机功能
     *
     * @param context 上下文对象，用于获取应用程序资源和启动活动
     */
    //
    private void init(Context context) {
        // 设置一个表面纹理监听器
        setSurfaceTextureListener(mSurfaceTextureListener);
        // 创建相机代理对象
        mCameraProxy = new Camera2Proxy((Activity) context);
    }

    // 纹理视图监听器
    private SurfaceTextureListener mSurfaceTextureListener = new SurfaceTextureListener() {
        // 当纹理视图可用的时候，会回调这个方法
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.v(TAG, "onSurfaceTextureAvailable. width: " + width + ", height: " + height);
           // 打开相机
            mCameraProxy.openCamera(width, height);
            // 设置相机预览的表面纹理
            mCameraProxy.setPreviewSurface(surface);
            // 设置预览尺寸
            int previewWidth = mCameraProxy.getPreviewSize().getWidth();
            int previewHeight = mCameraProxy.getPreviewSize().getHeight();
            // 根据宽高比来设置预览尺寸
            if (width > height) {
                setAspectRatio(previewWidth, previewHeight);
            } else {
                setAspectRatio(previewHeight, previewWidth);
            }
        }

        // 当纹理视图的尺寸发生变化的时候，会回调这个方法
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            Log.v(TAG, "onSurfaceTextureSizeChanged. width: " + width + ", height: " + height);
        }
        // 当纹理视图被销毁的时候，会回调这个方法
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.v(TAG, "onSurfaceTextureDestroyed");
            mCameraProxy.releaseCamera();
            return false;
        }
        // 当纹理视图被更新时，会回调这个方法
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };
    // 设置宽高比
    private void setAspectRatio(int width, int height) {
        // 宽高比不能小于0
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        // 设置宽高比
        mRatioWidth = width;
        mRatioHeight = height;
        // 重新计算布局
        requestLayout();
    }
    // 获取相机代理对象
    public Camera2Proxy getCameraProxy() {
        return mCameraProxy;
    }

    // 测量方法
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }

    // 触摸事件
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 单指触控
        if (event.getPointerCount() == 1) {
            mCameraProxy.focusOnPoint((int) event.getX(), (int) event.getY(), getWidth(), getHeight());
            return true;
        }
        // 双指触控
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            // 当有第二个手指按下的时候，记录距离
            case MotionEvent.ACTION_POINTER_DOWN:
                // 记录距离
                mOldDistance = getFingerSpacing(event);
                break;
                // 当有手指抬起的时候，判断是放大还是缩小
            case MotionEvent.ACTION_MOVE:
                // 计算距离
                float newDistance = getFingerSpacing(event);
                // 判断是放大还是缩小
                if (newDistance > mOldDistance) {
                    // 放大
                    mCameraProxy.handleZoom(true);
                    // 缩小
                } else if (newDistance < mOldDistance) {
                    // 缩小
                    mCameraProxy.handleZoom(false);
                }
                // 记录距离
                mOldDistance = newDistance;
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }
    // 计算手指之间的距离
    private static float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

}
