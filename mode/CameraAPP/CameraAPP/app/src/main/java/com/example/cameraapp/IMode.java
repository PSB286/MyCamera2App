package com.example.cameraapp;


import android.hardware.camera2.CameraAccessException;

public interface IMode {
    void openCamera(int width, int height);

    void closeCamera();

    void switchCamera();

    void captureStillPicture() throws CameraAccessException;

    void createCameraPreviewSession();

    void setUpCameraOutputs(int width, int height);

    void stopRecordingVideo();

    void cameraSwitchMode();
}
