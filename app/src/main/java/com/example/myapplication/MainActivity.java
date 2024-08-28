package com.example.myapplication;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.Manifest;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;
//import com.example.myapplication.Activity;



public class MainActivity extends AppCompatActivity {

    //成员变量
    private static final int PERMISSION_REQUEST_CODE = 123;                             //授权码
    private static final String TAG = "CameraActivity";                                 //log标志
    private static final String cameraId = "0";                                         //前后摄像标志
    private CameraDevice cameraDevice;
    private HandlerThread cameraThread = new HandlerThread("CameraThread");
    private Handler cameraHandler = new Handler(cameraThread.getLooper());
    private CameraManager cameraManager;
    private CameraCharacteristics characteristics;
    private CameraCaptureSession session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //获取权限
        if (checkPermissions()) {
            // 权限已被授予，可以执行相关操作
        } else {
            requestPermissions();
        }

//        //获取手机摄像头
//        CameraUtils viewModel=new CameraUtils(null);
//        List<String> cameraIds = Arrays.asList(viewModel.getCameraIds());
//        for (String cameraId : cameraIds) {
//            String orientation = viewModel.getCameraOrientationString(cameraId);
//            Log.i(TAG, "cameraId : " + cameraId + " - " + orientation);
//        }

//        ActivityBinding binding = ActivityCameraBinding.inflate(getLayoutInflater());
//        setContentView(binding.getRoot());
    }


////////////////////////////////////////////////////////////////////////
//类内函数
////////////////////////////////////////////////////////////////////////
    //获取相机权限
    private void requestPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO
                },
                PERMISSION_REQUEST_CODE
        );
    }

    //检查获取相机权限
    private boolean checkPermissions() {
        int cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int storagePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int audioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);

        return (cameraPermission == PackageManager.PERMISSION_GRANTED )&& (storagePermission == PackageManager.PERMISSION_GRANTED)&& (audioPermission == PackageManager.PERMISSION_GRANTED);
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    //重写回调函数
    ///////////////////////////////////////////////////////////////////////////////////////
    //获取权限的重写
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已被授予，可以执行相关操作
            } else {
                // 权限被拒绝，提示用户需要授权
            }
        }
    }


    /////////////////////////////////////////////////////////////////////////////////////////
    //内部类
    ////////////////////////////////////////////////////////////////////////////////////////
    //获取摄像头的id
    public  class CameraUtils {
        private CameraManager cameraManager;
        //构造函数
        public CameraUtils(Context context) {
            cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        }

        // 获取所有摄像头的CameraID
        public String[] getCameraIds() {
            try {
                return cameraManager.getCameraIdList();
            } catch (CameraAccessException e) {
                throw new RuntimeException(e);
            }
        }

        // 获取摄像头方向
        public String getCameraOrientationString(String cameraId) {
            CameraCharacteristics characteristics = null;
            try {
                characteristics = cameraManager.getCameraCharacteristics(cameraId);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (lensFacing == null) {
                return "Unknown";
            }
            switch (lensFacing) {
                case CameraCharacteristics.LENS_FACING_BACK:
                    return "后摄(Back)";
                case CameraCharacteristics.LENS_FACING_FRONT:
                    return "前摄(Front)";
                case CameraCharacteristics.LENS_FACING_EXTERNAL:
                    return "外置(External)";
                default:
                    return "Unknown";
            }
        }
    }


}