package com.example.cameraapp;

import static android.app.PendingIntent.getActivity;
import static androidx.core.content.ContextCompat.getSystemService;
import static androidx.core.content.PermissionChecker.checkSelfPermission;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PhotoMode implements IMode{
    public String TAG = "MyCamera PhotoMode:";
    public final MainActivity mActivity;
    public CameraDevice mCameraDevice;
    public CameraCaptureSession mCaptureSession;
    public CameraCharacteristics mCharacteristics;
    public ImageReader imageReader;
    public String mCameraId;
    public int currentCameraIndex = 0;
    public Size previewSize;  // 定义拍照预览尺寸
    public CaptureRequest.Builder previewRequestBuilder;
    public CaptureRequest.Builder captureRequestBuilder;
    public Button capture;
    public CaptureRequest previewRequest;  // 定义用于预览照片的捕获请求
    public ImageView imageView;
    public AutoFitTextureView textureView;
    public FrameLayout rootLayout; // 定义界面上布局管理器
    public Spinner mAspectRatio;
    public final int MAX_ZOOM = 100; //放大的最大值
    public int mZoom = 0; //缩放
    public float mStepWidth; //每次改变宽的大小
    public float mStepHeight;
    private static final int REQUEST_READ_STORAGE_PERMISSION = 4;
    public int mSensorOrientation;
    //定义画幅比常量
    public static final List<Size>SUPPORTED_ASPECT_RATIOS = Arrays.asList(
            new Size(4,3),
            new Size(1,1),
            new Size(16,9)//全屏
    );
    //
    public MediaActionSound mMediaActionSound;
    public Animation btnAnimation;
    public Size mCurrentAspectRatio = SUPPORTED_ASPECT_RATIOS.get(0); //当前支持的画幅尺寸默认是4:3

    //构造函数
    public PhotoMode(MainActivity activity){
        this.mActivity = activity;
        this.rootLayout = activity.findViewById(R.id.root);
        this.imageView = activity.findViewById(R.id.imageView);
        this.textureView = activity.findViewById(R.id.textureView);
        this.capture = activity.findViewById(R.id.capture);
        //画幅选择
        mAspectRatio = mActivity.findViewById(R.id.aspectRatio);
        ArrayAdapter<Size> adapter = new ArrayAdapter<>(mActivity, android.R.layout.simple_spinner_item,SUPPORTED_ASPECT_RATIOS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mAspectRatio.setAdapter(adapter);

        mAspectRatio.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
           // 选择画幅尺寸
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                //
                mCurrentAspectRatio = (Size)adapterView.getItemAtPosition(position);
                //
                setAspectRatio(mCurrentAspectRatio);
                openCamera(textureView.getWidth(),textureView.getHeight());
            }
            // 未选择
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //缩略图跳转到相册
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
        //拍照声音
        mMediaActionSound = new MediaActionSound();
        //按钮动画
        btnAnimation = AnimationUtils.loadAnimation(mActivity, R.anim.btn_anim);
    }
    //权限标识
    public static final int REQUEST_CAMERA_PERMISSION = 1;
    public static final int REQUEST_WRITE_STORAGE_PERMISSION = 2;
    public static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    public static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    public static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    public static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();
    //屏幕旋转到jpeg方向的转换
    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    //

    public final TextureView.SurfaceTextureListener getSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
            // 当TextureView可用时，打开摄像头
            openCamera(width,height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
            configureTransform(width,height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
            closeCamera();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

        }
    };

    //创建一个拍照回调方法//  摄像头被打开时激发该方法
    public final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            // 开始预览
            setUpCameraOutputs(textureView.getWidth(),textureView.getHeight()); //设置预览尺寸
            createCameraPreviewSession();
            if (null != textureView){
                configureTransform(textureView.getWidth(),textureView.getHeight());
            }
        }

        // 摄像头断开连接时激发该方法
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close(); //
            mCameraDevice = null;
        }
        // 打开摄像头出现错误时激发该方法
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
            mActivity.finish();
        }
    };

    //调整画幅宽高比
    public void setAspectRatio(Size aspectRatio){
        this.mCurrentAspectRatio = aspectRatio;
        //重新配置相机预览和拍照的画幅设置
        setUpCameraOutputs(textureView.getWidth(),textureView.getHeight());
    }
    @Override
    public void setUpCameraOutputs(int width, int height) {
        CameraManager manager = (CameraManager)mActivity.getSystemService(Context.CAMERA_SERVICE);
        try {
            // 获取指定摄像头的特性
            mCharacteristics = manager.getCameraCharacteristics(mCameraId);
            // 获取摄像头支持的配置属性
            StreamConfigurationMap map = mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mSensorOrientation = mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            // 获取摄像头支持的最大尺寸
            if (map == null) {
                Log.e(TAG, "StreamConfigurationMap is null");
                return;
            }
            Log.e(TAG, "map:"+map);
            previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, mCurrentAspectRatio);

            Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
            if (largest.getWidth() > previewSize.getWidth() || largest.getHeight() > previewSize.getHeight()){
                largest = new Size(previewSize.getWidth(),previewSize.getHeight());
                Log.i(TAG,"largest=" + largest);
            }

            // 创建一个ImageReader对象，用于获取摄像头的图像数据
            imageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, 2);
            imageReader.setOnImageAvailableListener(reader -> {
                // 当照片数据可用时激发该方法
                // 获取捕获的照片数据
                Image image = reader.acquireNextImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                //使用IO流将照片写入指定文件
                //保存图片的路径
                String filePath = Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera";
                String picturePath = System.currentTimeMillis() + ".jpg";
                File file = new File(filePath, picturePath);
                Log.i(TAG,"filePath:"+filePath);

                try(FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                    //保存到本地相册
                    fileOutputStream.write(bytes);
                    //显示图
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 2;
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options); // 图片压缩
                    imageView.setImageBitmap(bitmap); // 显示图片
                } catch (IOException e){
                    e.printStackTrace();
                    Log.i(TAG,"保存照片失败");
                }finally {
                    image.close();
                    sendBroadcastToMediaScanner(mActivity,file);
                }},null);
            // 获取最佳的预览尺寸
            //previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, largest);
            previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, mCurrentAspectRatio);
            Log.i(TAG,"previewSize="+previewSize);
            //更新textureView的宽高比
            textureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
            //根据选中的预览尺寸来调整预览组件（TextureView的）的长宽比
            int orientation = mActivity.getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                textureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
            } else {
                textureView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.out.println("相机出图失败。");
        }
    }

    @Override
    public void cameraSwitchMode() {
        //切换到拍照模式
        openCamera(textureView.getWidth(),textureView.getHeight());
        //切换到录像模式
        capture.setVisibility(View.VISIBLE);
        //启用拍照按钮
        capture.setEnabled(true);
        //启用AspectRatio
        mAspectRatio.setVisibility(View.VISIBLE);
        //
        mAspectRatio.setEnabled(true);

    }

    @Override
    public void createCameraPreviewSession() {
        if (textureView == null || textureView.getSurfaceTexture() == null){
            Log.i(TAG,"TextureView is null");
            return;
        }
        try {
            Log.i(TAG,"createCameraPreviewSession");
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface surface = new Surface(texture);
            // 创建作为预览的CaptureRequest.Builder
            previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // 将textureView的surface作为CaptureRequest.Builder的目标
            previewRequestBuilder.addTarget(surface);
            // 创建CameraCaptureSession，该对象负责管理处理预览请求和拍照请求
            mCameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    // 如果摄像头为null，直接结束方法
                    if (null == mCameraDevice) {
                        Log.i(TAG,"摄像头为空");
                        return;
                    }
                    // 当摄像头已经准备好时，开始显示预览
                    mCaptureSession = cameraCaptureSession;
                    // 设置自动对焦模式
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                    // 设置自动曝光模式
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    //开启人脸检测功能
                    int mFaceMaxCount = mCharacteristics.get(CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT);
                    Log.d(TAG,"mFaceMaxCount: " + mFaceMaxCount);
                    int mFaceModes[] = mCharacteristics.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES);
                    Log.d(TAG,"mFaceModes: " + mFaceModes);
//                    if (characteristics.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES)[0] != CameraMetadata.STATISTICS_FACE_DETECT_MODE_OFF){
//                        previewRequestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE,CameraMetadata.STATISTICS_FACE_DETECT_MODE_FULL);
//                        Log.i(TAG,"ffffffffff");
//                    }
                    // 开始显示相机预览
                    try {
                        previewRequest = previewRequestBuilder.build();
                        // 设置预览时连续捕获图像数据
                        mCaptureSession.setRepeatingRequest(previewRequest, null, null);
                        Log.i(TAG,"预览连续捕获数据");
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(mActivity, "配置失败！", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.i(TAG,"创建相机预览session失败");
        }
    }

    @Override
    public void openCamera(int width, int height) {
        //setUpCameraOutputs(width, height);
       // configureTransform(width, height);
        // 如果用户没有授权使用摄像头，直接返回
        if (ContextCompat.checkSelfPermission(mActivity,Manifest.permission.CAMERA) != PermissionChecker.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            Log.i(TAG,"相机没有授权");
            return;
        }
        CameraManager manager = (CameraManager)mActivity.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIdList = manager.getCameraIdList();  //获取摄像头id，存入cameraIdList
            if (cameraIdList.length == 0) {
                Toast.makeText(this.mActivity, "没有可用的摄像头", Toast.LENGTH_SHORT).show();
                return;
            } else {
                mCameraId = cameraIdList[currentCameraIndex]; // 当前相机索引
                Log.i(TAG,"cameraId="+mCameraId);
                manager.openCamera(mCameraId,stateCallback,null);
                // 打开摄像头
                Log.i(TAG,"打开摄像头成功");
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.i(TAG,"打开摄像头失败");
        }
    }

    @Override
    public void closeCamera() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    @Override
    public void switchCamera() {
        CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIdList = manager.getCameraIdList();
            if (cameraIdList.length < 2) {
                Toast.makeText(this.mActivity, "没有多的镜头可以切换", Toast.LENGTH_SHORT).show();
                Log.i(TAG,"没有可切换的摄像头");
                return;
            }
            currentCameraIndex = (currentCameraIndex + 1) % cameraIdList.length;
            mCameraId = cameraIdList[currentCameraIndex];
            closeCamera();
            openCamera(textureView.getWidth(), textureView.getHeight());
            Log.i(TAG,"切换摄像头成功,CameraId=" + mCameraId);
        } catch (CameraAccessException e) {
            Log.i(TAG,"切换摄像头失败，出现异常");
            e.printStackTrace();
        }
    }

    @Override
    public void captureStillPicture() {
        try {
            if (mCameraDevice == null) {
                return;
            }
            // 创建作为拍照的CaptureRequest.Builder
            captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            // 将imageReader的surface作为CaptureRequest.Builder的目标
            captureRequestBuilder.addTarget(imageReader.getSurface());
            // 获取设备方向
            int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
            // 根据设备方向计算设置照片的方向
            switch (mSensorOrientation){
                case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                    captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, DEFAULT_ORIENTATIONS.get(rotation));
                    break;
                case SENSOR_ORIENTATION_INVERSE_DEGREES:
                    captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,INVERSE_ORIENTATIONS.get(rotation));
                    break;
                default:
                    break;
            }
            // 停止连续取景
            mCaptureSession.stopRepeating();
            // 执行拍照请求,捕获静态图像
            mCaptureSession.capture(captureRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                // 拍照完成时激发该方法
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    try {  // 打开连续取景模式
                        Log.i(TAG,"拍照完成");
                        mCaptureSession.setRepeatingRequest(previewRequest, null, null);
                        mMediaActionSound.play(MediaActionSound.SHUTTER_CLICK); // 播放拍照声音
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                        Log.i(TAG,"重复预览失败");
                    }
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.i(TAG,"拍照失败");
        }
    }

    public void sendBroadcastToMediaScanner(Context context, File file) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, null, null);
        } else {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(Uri.fromFile(file));
            context.sendBroadcast(mediaScanIntent);
        }
    }
    public void configureTransform(int viewWidth, int viewHeight) {//配置变换
        if (null == previewSize) {
            Log.i(TAG,"chooseOptimalSize:previewSize为空");
            return;
        }
        // 获取手机的旋转方向
        int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());

        float centerX = viewRect.centerX();// 获取SurfaceView中心点坐标
        float centerY = viewRect.centerY();
        // 处理手机横屏的情况
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY()); // 移动
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL); // 裁剪
            float scale = Math.max((float) viewHeight / previewSize.getHeight(), (float) viewWidth / previewSize.getWidth()); // 计算缩放比例
            matrix.postScale(scale, scale, centerX, centerY);   // 缩放
            matrix.postRotate(90 * (rotation - 2), centerX, centerY); // 旋转
        }
        // 处理手机倒置的情况
        else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        initZoomParam();
        textureView.setTransform(matrix); // 设置SurfaceTexture的变换矩阵
    }
    public static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        //检查输入参数
        if (choices == null || aspectRatio == null){
            Log.i("chooseOptimalSize","无效输入参数");
            return null;
        }
        // 收集摄像头支持的打过预览Surface的分辨率
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth(); // 宽高比
        int h = aspectRatio.getHeight();
        //检查宽高比是否有效
        if (w <= 0 || h <= 0){
            Log.i("Tag","宽高比无效");
        }
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w && option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }
        // 如果找到多个预览尺寸，获取其中面积最小的。
        if (!bigEnough.isEmpty()) {
            return Collections.min(bigEnough, new CompareSizesByArea()); // 按面积排序
        }
        else {
            System.out.println("拍照模式找不到合适的预览尺寸！！！");
            return choices.length > 0 ? choices[0] : null;
        }

    }
    // 比较器
    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // 强转为long保证不会发生溢出
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    //zoom
    public void initZoomParam(){
        Rect mRect = mCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        Log.d(TAG,"SENSOR_INFO_ACTIVE_ARRAY_SIZE:" + mRect);
        float mMaxDigitalZoom= mCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        Log.d(TAG,"SCALER_AVAILABLE_MAX_DIGITAL_ZOOM:"+mMaxDigitalZoom);
        float mMinWidth = mRect.width() / mMaxDigitalZoom;  //crop_rect最小的宽高
        float mMinHeight = mRect.height() / mMaxDigitalZoom;
        mStepWidth = (mRect.width() - mMinWidth) / MAX_ZOOM / 2;
        mStepHeight = (mRect.height() - mMinHeight) / MAX_ZOOM /2;
    }
    public void handleZoom(boolean isZoomIn){
        if (mCameraDevice == null || mCharacteristics == null || previewRequestBuilder == null){
            return;
        }
        if (isZoomIn && mZoom < MAX_ZOOM){
            //放大
            mZoom++;
            Log.d(TAG,"mZoom++");
        } else {
            mZoom--;
            Log.d(TAG,"mZoom--");
        }
        Rect mRect = mCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        int mCropW = (int) (mStepWidth * mZoom);
        int mCropH = (int) (mStepHeight * mZoom);
        Rect mZoomRect = new Rect(mRect.left + mCropW,mRect.top + mCropH,mRect.right - mCropW,mRect.bottom - mCropH);
        previewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION,mZoomRect);
        Log.d(TAG,"handleZoom");
        createCameraPreviewSession();//重新开启预览
        Log.d(TAG,"cccccccccc");
    }
    public float getFingerSpacing(MotionEvent event){
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        Log.d(TAG,"getFingerSpacing:");
        return (float) Math.sqrt(x*x + y*y);
    }

    @Override
    public void stopRecordingVideo() {}
}

