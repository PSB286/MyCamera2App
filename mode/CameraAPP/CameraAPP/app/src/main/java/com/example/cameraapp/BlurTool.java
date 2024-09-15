package com.example.cameraapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

public class BlurTool {
    private RenderScript mRenderScript;
    private ScriptIntrinsicBlur mBlurScript; // 模糊脚本
    private Allocation inputAllocation; // 输入
    private Allocation outputAllocation; // 输出

    public BlurTool(Context context) {
        mRenderScript = RenderScript.create(context); // 创建RenderScript
        mBlurScript = ScriptIntrinsicBlur.create(mRenderScript, Element.U8_4(mRenderScript)); // 创建模糊脚本
    }

    public Bitmap blur(Bitmap input, float radius) {
        inputAllocation = Allocation.createFromBitmap(mRenderScript, input);
        outputAllocation = Allocation.createFromBitmap(mRenderScript, input);

        mBlurScript.setRadius(radius); // 设置模糊半径
        mBlurScript.setInput(inputAllocation);// 设置输入
        mBlurScript.forEach(outputAllocation);// 执行模糊

        outputAllocation.copyTo(input);// 将模糊后的图片赋值给input
        return input;
    }

    public void destroy() {
        if (inputAllocation != null) {
            inputAllocation.destroy();
        }
        if (outputAllocation != null) {
            outputAllocation.destroy();
        }
        if (mBlurScript != null) {
            mBlurScript.destroy();
        }
        if (mRenderScript != null) {
            mRenderScript.destroy();
        }
    }
}