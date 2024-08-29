package com.example.myapplication;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

//继承TextureView类，实现自动调整TextureView尺寸以适应预览尺寸
public class AutoFitTextureView extends TextureView
{
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
        requestLayout();
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
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight)
        {
            setMeasuredDimension(width, height);
        }
        else
        {
            if (width < height * mRatioWidth / mRatioHeight)
            {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            }
            else
            {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }
}