package com.example.cameraapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.List;

public class FaceView extends View {
    public List<RectF> mFaceRects;
    public Paint mFacePaint;
    public FaceView(Context context) {
        super(context);
        mFacePaint = new Paint();
        mFacePaint.setColor(Color.RED);
        mFacePaint.setStyle(Paint.Style.STROKE);
        mFacePaint.setStrokeWidth(5);
    }
    public void setFaceRects(List<RectF> faceRects) {
        mFaceRects = faceRects;
        invalidate(); // 请求重绘View
    }
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        for (RectF rect : mFaceRects) {
            canvas.drawRect(rect, mFacePaint);
        }
    }
}
