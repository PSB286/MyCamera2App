package com.example.cameraapp;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;

public class MainActivity extends Activity {
    private final String TAG = "asdzxc";
    private AutoFitTextureView textureView;  // 定义自定义的AutoFitTextureView组件,用于自动预览摄像头照片
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int REQUEST_WRITE_STORAGE_PERMISSION = 2;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 3;
    private static final int REQUEST_READ_STORAGE_PERMISSION = 4;
    public ImageButton switchCameraId;
    private PhotoMode photoMode;
    private VideoMode videoMode;
    private final boolean PHOTO = true;
    //模式切换按钮
    private Button capbtn;
    private Button vidbtn;
    private View mBlurView;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        photoMode = new PhotoMode(this);
        videoMode = new VideoMode(this);
        mBlurView = new View(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // 保持屏幕常亮

        switchCameraId = findViewById(R.id.switchCameraId);
        textureView = findViewById(R.id.textureView);
        capbtn = findViewById(R.id.capbtn);
        vidbtn = findViewById(R.id.vidbtn);
        //模糊动画

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(); // 获取窗口参数
        params.copyFrom(getWindow().getAttributes());// 复制当前窗口参数
        params.width = WindowManager.LayoutParams.MATCH_PARENT;  // 设置窗口宽度为屏幕宽度
        params.height = WindowManager.LayoutParams.MATCH_PARENT;  // 设置窗口高度为屏幕高度
        // 设置窗口为不可聚焦和不可触摸
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        mBlurView.setLayoutParams(params); // 设置自定义的AutoFitTextureView参数
        mBlurView.setVisibility(View.GONE); // 隐藏自定义的AutoFitTextureView
        getWindowManager().addView(mBlurView,params); // 添加自定义的AutoFitTextureView到窗口


        //点击拍照按钮，执行photoMode
        capbtn.setOnClickListener(view -> {
            photoMode.cameraSwitchMode();
            videoMode.record.setEnabled(false);
            videoMode.record.setVisibility(View.GONE);
            videoMode.mVideoQuality.setVisibility(View.GONE);
            videoMode.mVideoQuality.setEnabled(false);
            capbtn.setTextColor(Color.YELLOW);
            vidbtn.setTextColor(Color.WHITE);

        });
        //点击录像按钮，执行videoMode
        vidbtn.setOnClickListener(view -> {
            photoMode.closeCamera();
            videoMode.cameraSwitchMode();
            photoMode.capture.setVisibility(View.GONE);
            photoMode.capture.setEnabled(false);
            photoMode.mAspectRatio.setEnabled(false);
            photoMode.mAspectRatio.setVisibility(View.GONE);
            vidbtn.setTextColor(Color.YELLOW);
            capbtn.setTextColor(Color.WHITE);
        });
    }
    //
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION || requestCode == REQUEST_WRITE_STORAGE_PERMISSION || requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
               photoMode.openCamera(textureView.getWidth(), textureView.getHeight());
            } else {
                Toast.makeText(this, "Camera and storage permissions are required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //处理缩略图跳转后的返回结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_READ_STORAGE_PERMISSION && resultCode == RESULT_OK){
            if (textureView.isAvailable()){
                photoMode.openCamera(textureView.getWidth(),textureView.getHeight());
            }
        }
    }
    private float mOldDistance;
    ///缩放
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 2){
            switch (event.getAction() & MotionEvent.ACTION_MASK){
                case MotionEvent.ACTION_POINTER_DOWN:
                    //点下时，得到两点间的距离
                    mOldDistance = photoMode.getFingerSpacing(event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    float mNewDistance = photoMode.getFingerSpacing(event);
                    if (mNewDistance > mOldDistance){
                        photoMode.handleZoom(true);
                    } else if (mNewDistance < mOldDistance) {
                        photoMode.handleZoom(false);
                    }
                    mOldDistance = mNewDistance; //更新
                    break;
                default:
                    break;
            }
        }
        return super.onTouchEvent(event);
    }
    //图标旋转

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

    }

    @Override
     protected void onResume() {
         super.onResume();
         //如果处于拍照模式，执行拍照按钮部分,否之执行录像模式
        if (PHOTO){
            if (photoMode.textureView.isAvailable()){
                photoMode.openCamera(photoMode.textureView.getWidth(), photoMode.textureView.getHeight()); //默认打开拍照模式
                photoMode.mAspectRatio.setEnabled(true);
                photoMode.mAspectRatio.setVisibility(View.VISIBLE);
            } else {
                photoMode.textureView.setSurfaceTextureListener(photoMode.getSurfaceTextureListener); // 设置监听器
            }
            switchCameraId.setOnClickListener(view -> {
                photoMode.switchCamera();
                startBlur();
            });
            photoMode.capture.setOnClickListener(view ->{
                photoMode.captureStillPicture();
                photoMode.capture.startAnimation(photoMode.btnAnimation); //拍照按钮动画
            });
        } else {
            if (videoMode.textureView.isAvailable()){
                videoMode.openCamera(videoMode.textureView.getWidth(), videoMode.textureView.getHeight());
            } else {
                videoMode.textureView.setSurfaceTextureListener(videoMode.mSurfaceTextureListener);
            }
            switchCameraId.setOnClickListener(view -> videoMode.switchCamera());
        }
     }
    @Override
    protected void onPause() {
        super.onPause();
        videoMode.closeCamera();
        if (videoMode.mMediaRecorder != null) {
            videoMode.mMediaRecorder.release();
            videoMode.mMediaRecorder = null;
        }
        if (videoMode.mRecorderSurface != null){
            videoMode.mRecorderSurface.release();
            videoMode.mRecorderSurface = null;
        }
    }
    //模糊动画
    private void startBlur(){
        //获取当前屏幕截图
        Bitmap mBitmap = getBitmapFromView(getWindow().getDecorView());
        BlurTool mBlurTool = new BlurTool(this);
        float radius = 25; //模糊半径
        Bitmap blurBitmap = mBlurTool.blur(mBitmap, radius); //模糊处理

        mBlurView.setBackground(new BitmapDrawable(getResources(),blurBitmap));
        mBlurView.setVisibility(View.VISIBLE);
        //1秒后移除模糊背景
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mBlurView.setVisibility(View.GONE);
                mBlurTool.destroy();

            }
        },1000);
    }
    private Bitmap getBitmapFromView(View view) { //截屏
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        view.draw(canvas);
        return returnedBitmap;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getWindowManager().removeView(mBlurView);
    }
    
}