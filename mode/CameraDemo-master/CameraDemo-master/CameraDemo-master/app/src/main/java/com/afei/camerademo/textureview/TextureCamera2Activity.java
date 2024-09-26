package com.afei.camerademo.textureview;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.afei.camerademo.ImageUtils;
import com.afei.camerademo.R;
import com.afei.camerademo.camera.Camera2Proxy;

import java.nio.ByteBuffer;

public class TextureCamera2Activity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "TextureCameraActivity";
    // 关闭
    private ImageView mCloseIv;
    // 切换
    private ImageView mSwitchCameraIv;
    // 拍照
    private ImageView mTakePictureIv;
    // 相册
    private ImageView mPictureIv;
    // TextureView
    private Camera2TextureView mCameraView;
    // Camera2Proxy
    private Camera2Proxy mCameraProxy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_texture_camera2);
        // 初始化控件
        initView();
    }

    private void initView() {
        // 初始化控件
        mCloseIv = findViewById(R.id.toolbar_close_iv);
        // 关闭
        mCloseIv.setOnClickListener(this);
        // 切换
        mSwitchCameraIv = findViewById(R.id.toolbar_switch_iv);
        // 切换
        mSwitchCameraIv.setOnClickListener(this);
        // 拍照
        mTakePictureIv = findViewById(R.id.take_picture_iv);
        // 拍照
        mTakePictureIv.setOnClickListener(this);
        // 相册
        mPictureIv = findViewById(R.id.picture_iv);
        // 相册
        mPictureIv.setOnClickListener(this);
        // 默认图片
        mPictureIv.setImageBitmap(ImageUtils.getLatestThumbBitmap());
        // TextureView
        mCameraView = findViewById(R.id.camera_view);
        // Camera2Proxy
        mCameraProxy = mCameraView.getCameraProxy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            // 关闭
            case R.id.toolbar_close_iv:
                finish();
                break;
                // 切换
            case R.id.toolbar_switch_iv:
                mCameraProxy.switchCamera(mCameraView.getWidth(), mCameraView.getHeight());
                mCameraProxy.startPreview();
                break;
                // 拍照
            case R.id.take_picture_iv:
                mCameraProxy.setImageAvailableListener(mOnImageAvailableListener);
                mCameraProxy.captureStillPicture(); // 拍照
                break;
                // 相册
            case R.id.picture_iv:
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivity(intent);
                break;
        }
    }

    // 拍照
    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener
            () {
        @Override
        public void onImageAvailable(ImageReader reader) {
            new ImageSaveTask().execute(reader.acquireNextImage()); // 保存图片
        }
    };


    // 保存图片
    private class ImageSaveTask extends AsyncTask<Image, Void, Bitmap> {
        // 耗时操作
        @Override
        protected Bitmap doInBackground(Image ... images) {
            ByteBuffer buffer = images[0].getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);

            long time = System.currentTimeMillis();
            if (mCameraProxy.isFrontCamera()) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                Log.d(TAG, "BitmapFactory.decodeByteArray time: " + (System.currentTimeMillis() - time));
                time = System.currentTimeMillis();
                // 前置摄像头需要左右镜像
                Bitmap rotateBitmap = ImageUtils.rotateBitmap(bitmap, 0, true, true);
                Log.d(TAG, "rotateBitmap time: " + (System.currentTimeMillis() - time));
                time = System.currentTimeMillis();
                ImageUtils.saveBitmap(rotateBitmap);
                Log.d(TAG, "saveBitmap time: " + (System.currentTimeMillis() - time));
                rotateBitmap.recycle();
            } else {
                ImageUtils.saveImage(bytes);
                Log.d(TAG, "saveBitmap time: " + (System.currentTimeMillis() - time));
            }
            images[0].close();
            return ImageUtils.getLatestThumbBitmap();
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            mPictureIv.setImageBitmap(bitmap);
        }
    }
}
