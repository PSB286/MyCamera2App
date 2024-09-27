package com.psb.myapplication;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

//继承TextureView类，实现自动调整TextureView尺寸以适应预览尺寸
public class AutoFitTextureView extends TextureView
{
    //视图的宽高比
    private int mRatioWidth = 0;
    private int mRatioHeight = 0;

    public AutoFitTextureView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    /**
     * 设置视图的宽高比例
     *
     * @param width 视图宽度
     * @param height 视图高度
     */
    void setAspectRatio(int width, int height)
    {
        mRatioWidth = width;
        mRatioHeight = height;
        // 重新调用requestLayout方法，以重新计算视图的尺寸
        requestLayout();
        // 添加日志输出，方便调试
        Log.d("--AutoFitTextureView--", "Setting aspect ratio: " + width + ":" + height);
    }

    /**
     * 重写onMeasure方法以计算视图的尺寸
     * 此方法考虑了视图的宽高比限制，以确保视图在不同屏幕尺寸下能够保持比例显示
     *
     * @param widthMeasureSpec 用于指定视图宽度的测量规格
     * @param heightMeasureSpec 用于指定视图高度的测量规格
     */
    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        // 调用父类的onMeasure方法，确保基本的测量逻辑得到执行
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.d("--AutoFitTextureView-onMeasure-", "Setting aspect ratio: " + widthMeasureSpec + ":" + heightMeasureSpec);

        // 从测量规范中提取出宽度和高度的具体尺寸
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        Log.d("--AutoFitTextureView-onMeasure-", "Setting aspect ratio: " + width + ":" + height);
        // 如果宽高比的分子或分母为0，则直接使用传入的宽度和高度作为测量尺寸
        if (0 == mRatioWidth || 0 == mRatioHeight)
        {
            setMeasuredDimension(width, height);
        }
        else
        {
            // 根据宽高比来调整测量尺寸
            // 如果根据宽高比计算出的宽度小于给定的高度，或者高度小于给定的宽度，
            // 则相应地调整高度或宽度，以保持宽高比的要求
            if (width < height * mRatioWidth / mRatioHeight)
            {
               // setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
               // setMeasuredDimension(width, width * (1600/720));
                setMeasuredDimension(1200, 500);
                Log.d("--AutoFitTextureView-onMeasure-", "Setting1 aspect ratio: " + width + ":" + width * (1600/720));
            }
            else
            {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
                Log.d("--AutoFitTextureView-onMeasure-", "Setting2 aspect ratio: " + height * mRatioWidth / mRatioHeight + ":" + height);
            }
        }
    }
}