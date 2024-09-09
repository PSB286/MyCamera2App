package com.psb.myapplication_huan;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 自定义的LinearLayout，用于实现特定的滚动和缩放效果
 */
public class CustomViewL extends LinearLayout {

    // 日志标签
    private static final String TAG = "CustomViewL.TAG";
    // 当前选中的项目索引
    private int mCurrentItem = 0;
    // 屏幕宽度
    private int screenWidth;

    // 子项缩放比例常量
    public static final float ItemScale = 0.1f;

    // 构造函数：仅传入context时的构造
    public CustomViewL(Context context) {
        super(context);
        init(context);
        setGravity(Gravity.CENTER); // 设置 LinearLayout 的 gravity 为 center
    }

    // 构造函数：传入context和属性集时的构造
    public CustomViewL(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
        setGravity(Gravity.CENTER); // 设置 LinearLayout 的 gravity 为 center
    }

    /**
     * 初始化方法，主要用于获取屏幕宽度
     * @param context 上下文环境
     */
    private void init(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        screenWidth = wm.getDefaultDisplay().getWidth();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Log.d(TAG, "onLayout ");
        super.onLayout(changed, l, t, r, b);
        // 调整子视图位置
        adjustChildrenPosition();
    }

    /**
     * 调整子视图的位置，使其符合特定的布局规则
     */
    private void adjustChildrenPosition() {
        // 计算中心点
        int center = 288;
        Log.d("--adjustChildrenPosition--", "onLayout center = " + center);
        // 遍历所有子项
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            int left = (i - mCurrentItem) * (int) (screenWidth * ItemScale);
            int right = left + v.getWidth();
            int top = v.getTop();
            int bottom = v.getBottom();
            Log.d("--adjustChildrenPosition--", "onLayout i = " + i + " left = " + left + " right = " + right);
            v.layout(center + left, top, center + right, bottom);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        //q
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            // 更新子项的缩放效果
            updateChildItem(canvas, i);
        }
    }

    /**
     * 更新子项的缩放效果和颜色
     * @param canvas 画布
     * @param item 子项索引
     */
    public void updateChildItem(Canvas canvas, int item) {
        // 获取子项
        View v = getChildAt(item);
        // 计算缩放比例
        float scale = 1 - Math.abs(item - mCurrentItem) * ItemScale;
        // 设置缩放比例
        ((TextView) v).setScaleX(scale);
        ((TextView) v).setScaleY(scale);
        drawChild(canvas, v, getDrawingTime());
        // 更新子项文字颜色
        updateTextColor();
    }

    /**
     * 更新子视图的文字颜色，当前选中项文字颜色设为黄色，其他设为白色
     */
    private void updateTextColor() {
        // 遍历所有子项
        for (int i = 0; i < getChildCount(); i++) {
            // 获取子项
            TextView textView = (TextView) getChildAt(i);
            if (i == mCurrentItem) {
                textView.setTextColor(Color.YELLOW);
            } else {
                textView.setTextColor(Color.WHITE);
            }
        }
    }

    /**
     * 向右滚动视图
     */
    public boolean scrollRight() {
        if (mCurrentItem > 0) {
            mCurrentItem--;
            // 开始执行滚动动画
            startTranslationAnimation(mCurrentItem, mCurrentItem + 1);
            updateTextColor();
        }
        return true;
    }

    /**
     * 向左滚动视图
     */
    public boolean scrollLeft() {
        // 判断是否还可以向左滚动
        if (mCurrentItem < getChildCount() - 1) {
            // 更新当前项索引
            mCurrentItem++;
            // 开始执行滚动动画
            startTranslationAnimation(mCurrentItem, mCurrentItem - 1);
            // 更新文字
            updateTextColor();
        }
        return false;
    }

    /**
     * 添加指示器文字视图
     * @param names 指示器的名字数组
     */
    public void addIndicator(String[] names) {
        setGravity(Gravity.CENTER); // 设置 LinearLayout 的 gravity 为 center

        for (String name : names) {
            // 创建一个 TextView
            TextView textView = new TextView(getContext());
            // 设置文字
            textView.setText(name);
            // 设置文字颜色
            textView.setTextColor(Color.WHITE);
            // 设置文字行数
            textView.setLines(1);
            // 设置 TextView 的 gravity 为 center
            textView.setGravity(Gravity.CENTER);
            // 设置参数
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    // 宽度
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    // 高度
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            // 设置边距
            params.setMargins(20, 0, 20, 0); // 增加左侧边距 20dp
            // 添加到布局中
            addView(textView, params);

            // 开始执行平移动画
            startTranslationAnimation(mCurrentItem, mCurrentItem - 1);
        }
    }

    /**
     * 开始执行平移动画
     * @param item 当前项索引
     * @param last 上一项索引
     */
    private void startTranslationAnimation(int item, int last) {
        // Log.d(TAG, "startTranslationAnimation last = " + last);
        Log.d(TAG, "startTranslationAnimation item = " + item);
        // 获取子项个数
        int childCount = getChildCount();
        // 平移距离
        int translate = (int) (screenWidth * ItemScale);
        // 遍历子项
        for (int i = 0; i < childCount; i++) {
            int delta = 0;
            if (i < item) {
                delta = -translate;
            } else if (i > item) {
                delta = translate;
            }
            // 创建平移动画
            TranslateAnimation translateAni = new TranslateAnimation(0, delta, 0, 0);
            translateAni.setDuration(300);
            translateAni.setFillAfter(true);
            // 开始动画
            getChildAt(i).startAnimation(translateAni);
        }
    }
}
