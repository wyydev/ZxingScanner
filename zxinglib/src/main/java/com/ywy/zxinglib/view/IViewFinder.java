package com.ywy.zxinglib.view;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface IViewFinder {
    int GRAVITY_TOP = 1;
    int GRAVITY_BOTTOM = 2;

    @IntDef({GRAVITY_TOP, GRAVITY_BOTTOM})
    @Retention(RetentionPolicy.SOURCE)
    @interface TextGravity {
    }

    /**
     * 扫码线颜色
     */
    void setScanLineColor(@ColorInt int scanLineColor);

    /**
     * 扫码线高度
     */
    void setScanLineHeight(@IntRange(from = 0) int scanLineHeight);

    /**
     * 扫码线左右两边间距
     */
    void setScanLineMargin(int scanLineMargin);

    /**
     * 扫码线每次移动距离
     */
    void setScanLineMoveDistance(@IntRange(from = 0) int scanLineMoveDistance);

    /**
     * 扫码线图片资源
     */
    void setScanLineDrawable(Drawable scanLineDrawable);

    /**
     * 扫码线bitmap
     */
    void setScanLineBitmap(Bitmap bitmap);

    /**
     * 扫码线完成一次扫描的时间
     */
    void setAnimTime(long animTime);

    /**
     * 除扫码框区域外遮罩层颜色
     */
    void setMaskColor(@ColorInt int maskColor);

    /**
     * 扫码框占据控件比率 ，竖屏时根据宽计算，横屏时根据高计算
     */
    void setRectWidthRatio(@FloatRange(from = 0.0, to = 1.0) float rectWidthRatio);


    /**
     * 扫码框宽高比率
     */
    void setRectWidthHeightRatio(float rectWidthHeightRatio);

    /**
     * 扫码框是否是正方形
     */
    void setRectSquare(boolean square);

    /**
     * 扫码框为方形时宽占据屏幕比率
     */
    void setSquareDimensionRatio(float squareDimensionRatio);

    /**
     * 扫码框距离顶部距离，如考虑Toolbar高度,全屏，Toolbar覆盖在上面，需要居中显示则可以将rectTopOffset设置为Toolbar高度
     */
    void setRectTopOffset(int rectTopOffset);

    /**
     * 扫码框边框颜色
     */
    void setBorderColor(@ColorInt int borderColor);

    /**
     * 扫码框边框宽度
     */
    void setBorderStrikeWidth(int borderStrikeWidth);


    /**
     * 扫码框四个角颜色
     */
    void setCornerColor(@ColorInt int cornerColor);

    /**
     * 扫码框四个角线段宽度
     */
    void setCornerStrokeWidth(int cornerStrokeWidth);

    /**
     * 扫码框四个角线段长度
     */
    void setCornerLineLength(@IntRange(from = 0) int cornerLineLength);

    /**
     * 扫码框四个角是否是圆角
     */
    void setCornerRounded(boolean cornerRounded);

    /**
     * 扫码框四个角圆角半径
     */
    void setCornerRadius(@IntRange(from = 0) int cornerRadius);

    /**
     * 扫码框四个角在边框里面还是外面
     */
    void setCornerInRect(boolean inRect);


    /**
     * 扫码提示文字
     */
    void setTipText(String tipText);

    /**
     * 提示文字颜色
     */
    void setTextColor(int textColor);

    /**
     * 提示文字size
     */
    void setTextSize(int textSize);

    /**
     * 提示文字距离扫码框距离
     */
    void setTextMargin(int textMargin);

    /**
     * 提示文字位置，扫码框顶部和扫码框底部
     */
    void setTextGravity(@TextGravity int textGravity);

    /**
     * 提示文字是否只显示一行
     */

    void setTextSingleLine(boolean textSingleLine);

    /**
     * 更新扫码区域并刷新视图
     */
    void setupViewFinder();

    /**
     * 扫码区域
     */
    Rect getFramingRect();

    /**
     * 宽
     */
    int getWidth();

    /**
     * 高
     */
    int getHeight();
}
