package com.psb.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义的LinearLayout，用于实现特定的滚动和缩放效果
 */
public class CustomViewL extends LinearLayout {

    private final CameraActivity mCameraActivity= (CameraActivity) getContext();
    // 日志标签
    private static final String TAG = "CustomViewL.TAG";
    // 当前选中的项目索引
    private int mCurrentItem = 0;
    // 屏幕宽度
    private int screenWidth;
    // 存储所有的 TextView
    private List<TextView> textViews = new ArrayList<>();

    int index=0;

    int colorState=0;


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
    }

    /**
     * 更新子视图的文字颜色，当前选中项文字颜色设为黄色，其他设为白色
     */
    private int updateTextColor() {
        // 遍历所有子项
        for (int i = 0; i < textViews.size(); i++) {
            TextView textView = textViews.get(i);
            if (i == mCurrentItem) {
                textView.setTextColor(Color.YELLOW);
                colorState=i;
            } else {
                textView.setTextColor(Color.WHITE);
            }
        }
        mCameraActivity.Layout_Switch(colorState);
        return colorState;
    }

    /**
     * 向右滚动视图
     */
    public boolean scrollRight() {
        if (mCurrentItem > 0) {
            mCurrentItem--;
            // 更新文字
            updateTextColor();
            return true;
        }
       return false;
    }

    /**
     * 向左滚动视图
     */
    public boolean scrollLeft() {
        // 判断是否还可以向左滚动
        if (mCurrentItem < textViews.size() - 1) {
            // 更新当前项索引
            mCurrentItem++;
            // 更新文字
            updateTextColor();
            return true;
        }
       return false;
    }

    /**
     * 添加指示器文字视图
     * @param names 指示器的名字数组
     */
    public void addIndicator(String[] names) {
        setGravity(Gravity.CENTER); // 设置 LinearLayout 的 gravity 为 center

        for (int i = 0; i < names.length; i++) {
            // 创建一个 TextView
            TextView textView = new TextView(getContext());
            // 设置文字
            textView.setText(names[i]);
            // 设置文字颜色
            if(i==0) {
                textView.setTextColor(Color.YELLOW);
            }
            else {
                textView.setTextColor(Color.WHITE);
            }
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
            params.setMargins(20, 0, 20, 110); // 增加左侧边距 20dp
            // 添加到布局中
            addView(textView, params);

            // 添加到 textViews 列表
            textViews.add(textView);

            // 添加点击监听器
            textView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 根据点击的 TextView 调用相应的滚动方法
                    int index = textViews.indexOf(v);
                    if (index == 0) {
                        scrollRight();
                    } else {
                        scrollLeft();
                    }
                }
            });
        }
    }

    // 获取当前索引(确定当前模式位置)
    public int getIndex()
    {
        return index;
    }
}
