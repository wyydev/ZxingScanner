package com.ywy.zxinglib.view;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.ywy.util.DisplayUtils;

/**
 * @author ywy
 * @date 2019/7/11
 */
public class ViewFinderView extends View implements IViewFinder {
    private static final String TAG = "ViewFinderView";


    private Rect mFramingRect;
    private static final int MIN_DIMENSION_DIFF = 50;
    private Paint mFinderMaskPaint;
    @ColorInt
    private int mMaskColor;
    private Paint mScanLinePaint;
    @ColorInt
    private int mScanLineColor;
    private int mScanLineHeight;
    private int mScanLineMargin;
    private Drawable mScanLineDrawable;
    private Bitmap mScanLineBitmap;
    private int mScanLineMoveDistance;
    private float mScanLineTop;
    private long mAnimTime;
    private float mRectWidthRatio;
    private float mRectWidthHeightRatio;
    private boolean mRectSquare;
    private float mSquareDimensionRatio;
    private int mRectTopOffset;
    private Paint mBorderPaint;
    @ColorInt
    private int mBorderColor;
    private int mBorderStrokeWidth;
    private Paint mCornerPaint;
    @ColorInt
    private int mCornerColor;
    private int mCornerStrokeWidth;
    private int mCornerLineLength;
    private boolean mCornerRounded;
    private int mCornerRadius;
    private boolean mCornerInRect;

    private TextPaint mTextPaint;
    private String mTipText;
    private int mTextColor;
    private int mTextSize;
    private int mTextMargin;
    @TextGravity
    int mTextGravity;
    private boolean mTextSingleLine;
    private StaticLayout mStaticLayout;


    public ViewFinderView(Context context) {
        super(context);
        init(context);
    }

    public ViewFinderView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }


    private void init(Context context) {
        //扫描线
        mScanLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mScanLineColor = Color.WHITE;
        mScanLineHeight = DisplayUtils.dp2px(context, 2);
        mScanLineMargin = 0;
        mScanLineMoveDistance = mScanLineHeight;
        mAnimTime = 1000L;
        mScanLinePaint.setColor(mScanLineColor);
        mScanLinePaint.setStyle(Paint.Style.FILL);

        //遮罩
        mFinderMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMaskColor = Color.parseColor("#33FFFFFF");
        mFinderMaskPaint.setColor(mMaskColor);

        //边框
        mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBorderColor = Color.WHITE;
        mBorderStrokeWidth = DisplayUtils.dp2px(context, 1);
        mBorderPaint.setColor(mBorderColor);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(mBorderStrokeWidth);

        mCornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCornerColor = Color.WHITE;
        mCornerPaint.setColor(mCornerColor);
        mCornerStrokeWidth = DisplayUtils.dp2px(context, 3);
        mCornerPaint.setStrokeWidth(mCornerStrokeWidth);
        mCornerLineLength = DisplayUtils.dp2px(context, 20);
        mCornerInRect = false;
        mCornerRounded = false;

        mCornerPaint.setStyle(Paint.Style.STROKE);
        mCornerRadius = DisplayUtils.dp2px(context, 2);
        if (mCornerRounded) {
            mCornerPaint.setPathEffect(new CornerPathEffect(mCornerRadius));
        }

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextColor = Color.WHITE;
        mTextSize = DisplayUtils.sp2px(context, 14);
        mTipText = "将二维码/条码放入框内，即可自动扫描";
        mTextGravity = GRAVITY_BOTTOM;
        mTextSingleLine = true;
        mTextMargin = DisplayUtils.dp2px(context, 5);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setTextSize(mTextSize);
    }


    @Override
    public void setScanLineColor(int scanLineColor) {
        mScanLineColor = scanLineColor;
        mScanLinePaint.setColor(mScanLineColor);
    }

    @Override
    public void setScanLineHeight(int scanLineHeight) {
        mScanLineHeight = scanLineHeight;
    }

    @Override
    public void setScanLineMargin(int scanLineMargin) {
        mScanLineMargin = scanLineMargin;
    }

    @Override
    public void setScanLineMoveDistance(int scanLineMoveDistance) {
        mScanLineMoveDistance = scanLineMoveDistance;
    }

    @Override
    public void setScanLineDrawable(Drawable scanLineDrawable) {
        mScanLineDrawable = scanLineDrawable;
        if (mScanLineDrawable != null) {
            mScanLineBitmap = ((BitmapDrawable) mScanLineDrawable).getBitmap();
        }
    }

    @Override
    public void setScanLineBitmap(Bitmap bitmap) {
        mScanLineBitmap = bitmap;
    }

    @Override
    public void setAnimTime(long animTime) {
        mAnimTime = animTime;
    }

    @Override
    public void setMaskColor(int maskColor) {
        mMaskColor = maskColor;
        mFinderMaskPaint.setColor(mMaskColor);
    }

    @Override
    public void setRectWidthRatio(@FloatRange(from = 0.0, to = 1.0) float rectWidthRatio) {
        mRectWidthRatio = rectWidthRatio;
    }

    @Override
    public void setRectWidthHeightRatio(float rectWidthHeightRatio) {
        mRectWidthHeightRatio = rectWidthHeightRatio;
    }

    @Override
    public void setRectSquare(boolean square) {
        mRectSquare = square;
    }

    @Override
    public void setSquareDimensionRatio(float squareDimensionRatio) {
        mSquareDimensionRatio = squareDimensionRatio;
    }


    @Override
    public void setRectTopOffset(int rectTopOffset) {
        mRectTopOffset = rectTopOffset;
    }

    @Override
    public void setBorderColor(int borderColor) {
        mBorderColor = borderColor;
        mBorderPaint.setColor(mBorderColor);
    }

    @Override
    public void setBorderStrikeWidth(int borderStrikeWidth) {
        mBorderStrokeWidth = borderStrikeWidth;
        mBorderPaint.setStrokeWidth(mBorderStrokeWidth);
    }

    @Override
    public void setCornerColor(int cornerColor) {
        mCornerColor = cornerColor;
        mCornerPaint.setColor(mCornerColor);
    }

    @Override
    public void setCornerStrokeWidth(int cornerStrokeWidth) {
        mCornerStrokeWidth = cornerStrokeWidth;
        mCornerPaint.setStrokeWidth(mCornerStrokeWidth);
    }

    @Override
    public void setCornerLineLength(int cornerLineLength) {
        mCornerLineLength = cornerLineLength;
    }

    @Override
    public void setCornerRounded(boolean cornerRounded) {
        mCornerRounded = cornerRounded;
        if (cornerRounded) {
            mCornerPaint.setStrokeJoin(Paint.Join.ROUND);
        } else {
            mCornerPaint.setStrokeJoin(Paint.Join.BEVEL);
        }
    }

    @Override
    public void setCornerRadius(int cornerRadius) {
        mCornerRadius = cornerRadius;
        mCornerPaint.setPathEffect(new CornerPathEffect(cornerRadius));
    }

    @Override
    public void setCornerInRect(boolean inRect) {
        mCornerInRect = inRect;
    }

    @Override
    public void setTipText(String tipText) {
        this.mTipText = tipText;
    }

    @Override
    public void setTextColor(int textColor) {
        this.mTextColor = textColor;
        mTextPaint.setColor(mTextColor);

    }

    @Override
    public void setTextSize(int textSize) {
        this.mTextSize = textSize;
        mTextPaint.setTextSize(mTextSize);
    }

    @Override
    public void setTextMargin(int textMargin) {
        this.mTextMargin = textMargin;
    }

    @Override
    public void setTextGravity(@TextGravity int textGravity) {
        this.mTextGravity = textGravity;
    }

    @Override
    public void setTextSingleLine(boolean textSingleLine) {
        this.mTextSingleLine = textSingleLine;
    }

    @Override
    public void setupViewFinder() {
        updateFramingRect();
        invalidate();
    }

    @Override
    public Rect getFramingRect() {
        return mFramingRect;
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (getFramingRect() == null) {
            return;
        }

        drawViewFinderMask(canvas);

        drawBorder(canvas);

        drawCorner(canvas);

        drawScanLine(canvas);

        drawText(canvas);

        moveScanLine();
    }


    RectF rectF = new RectF();

    public void drawViewFinderMask(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        int layerId = canvas.saveLayer(0, 0, width, height, null, Canvas.ALL_SAVE_FLAG);

        if (mBorderColor != Color.TRANSPARENT && mBorderStrokeWidth > 0) {
            mFinderMaskPaint.setColor(mBorderColor);
            mFinderMaskPaint.setStrokeWidth(mBorderStrokeWidth);
            mFinderMaskPaint.setStyle(Paint.Style.FILL);
            if (mCornerRounded && mCornerRadius > 0) {
                rectF.set(mFramingRect);
                float rad = Math.min(mCornerRadius,
                        Math.min(rectF.width(), rectF.height()) * 0.5f);
                canvas.drawRoundRect(rectF, rad, rad, mFinderMaskPaint);
            } else {
                canvas.drawRect(mFramingRect, mFinderMaskPaint);
            }
        }
        mFinderMaskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
        mFinderMaskPaint.setColor(mMaskColor);
        mFinderMaskPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, width, height, mFinderMaskPaint);

        mBorderPaint.setXfermode(null);
        canvas.restoreToCount(layerId);
    }

    private void drawBorder(Canvas canvas) {
        if (mBorderColor != Color.TRANSPARENT && mBorderStrokeWidth > 0) {
            mBorderPaint.setColor(mBorderColor);
            mBorderPaint.setStrokeWidth(mBorderStrokeWidth);
            mBorderPaint.setStyle(Paint.Style.STROKE);
            if (mCornerRounded && mCornerRadius > 0) {
                rectF.set(mFramingRect);
                float rad = Math.min(mCornerRadius,
                        Math.min(rectF.width(), rectF.height()) * 0.5f);
                canvas.drawRoundRect(rectF, rad, rad, mBorderPaint);
            } else {
                canvas.drawRect(mFramingRect, mBorderPaint);
            }
        }
    }


    Path path = new Path();

    public void drawCorner(Canvas canvas) {
        Rect framingRect = getFramingRect();
        path.rewind();
        int offset;
        if (mCornerInRect) {
            offset = -(mCornerStrokeWidth / 2 - 1);
        } else {
            offset = mCornerStrokeWidth / 2 - 1;
        }

        //左上角
        path.moveTo(framingRect.left - offset, framingRect.top + mCornerLineLength);
        path.lineTo(framingRect.left - offset, framingRect.top - offset);
        path.lineTo(framingRect.left + mCornerLineLength, framingRect.top - offset);
        canvas.drawPath(path, mCornerPaint);

        //右上角
        path.moveTo(framingRect.right - mCornerLineLength, framingRect.top - offset);
        path.lineTo(framingRect.right + offset, framingRect.top - offset);
        path.lineTo(framingRect.right + offset, framingRect.top + mCornerLineLength);
        canvas.drawPath(path, mCornerPaint);

        //左下角
        path.moveTo(framingRect.left - offset, framingRect.bottom - mCornerLineLength);
        path.lineTo(framingRect.left - offset, framingRect.bottom + offset);
        path.lineTo(framingRect.left + mCornerLineLength, framingRect.bottom + offset);
        canvas.drawPath(path, mCornerPaint);

        //右下角
        path.moveTo(framingRect.right - mCornerLineLength, framingRect.bottom + offset);
        path.lineTo(framingRect.right + offset, framingRect.bottom + offset);
        path.lineTo(framingRect.right + offset, framingRect.bottom - mCornerLineLength);
        canvas.drawPath(path, mCornerPaint);
    }

    RectF destRect = new RectF();
    public void drawScanLine(Canvas canvas) {
        Rect framingRect = getFramingRect();
        mScanLinePaint.setStyle(Paint.Style.FILL);
        if (mScanLineBitmap != null) {
//            RectF destRect = new RectF(framingRect.left + mScanLineMargin, mScanLineTop,
//                    framingRect.right - mScanLineMargin, mScanLineTop + mScanLineBitmap.getHeight());
            destRect.set(framingRect.left + mScanLineMargin, mScanLineTop,
                    framingRect.right - mScanLineMargin, mScanLineTop + mScanLineBitmap.getHeight());

            canvas.drawBitmap(mScanLineBitmap, null, destRect, mScanLinePaint);
        } else {
            canvas.drawRect(framingRect.left + mScanLineMargin, mScanLineTop, framingRect.right - mScanLineMargin,
                    mScanLineTop + mScanLineHeight, mScanLinePaint);
        }

    }


    private void drawText(Canvas canvas) {
        if (TextUtils.isEmpty(mTipText) || mStaticLayout == null) {
            return;
        }

        canvas.save();
        if (mTextGravity == GRAVITY_TOP) {
            if (mTextSingleLine) {
                canvas.translate(0, mFramingRect.top - mTextMargin - mStaticLayout.getHeight());
            } else {
                canvas.translate(mFramingRect.left, mFramingRect.top - mTextMargin - mStaticLayout.getHeight());
            }
        } else {
            if (mTextSingleLine) {
                canvas.translate(0, mFramingRect.bottom + mTextMargin);
            } else {
                canvas.translate(mFramingRect.left, mFramingRect.bottom + mTextMargin);
            }
        }
        mStaticLayout.draw(canvas);
        canvas.restore();

    }


    private void moveScanLine() {
        if (mScanLineTop + mScanLineHeight >= mFramingRect.bottom - mScanLineHeight - 0.5f) {
            mScanLineTop = mFramingRect.top + 0.5f;
        } else {
            mScanLineTop += mScanLineMoveDistance;
        }

        postInvalidateDelayed(mAnimTime,
                mFramingRect.left,
                mFramingRect.top,
                mFramingRect.right,
                mFramingRect.bottom);
    }

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        updateFramingRect();
    }

    public void updateFramingRect() {
        int orientation = DisplayUtils.getScreenOrientation(getContext());
        int mRectWidth = 0;
        int mRectHeight = 0;
        if (mRectSquare) {
            if (orientation != Configuration.ORIENTATION_PORTRAIT) {
                mRectHeight = (int) (getHeight() * mSquareDimensionRatio);
                mRectWidth = mRectHeight;
            } else {
                mRectWidth = (int) (getWidth() * mSquareDimensionRatio);
                mRectHeight = mRectWidth;
            }
        } else {
            if (orientation != Configuration.ORIENTATION_PORTRAIT) {
                mRectHeight = (int) (getHeight() * mRectWidthRatio);
                mRectWidth = (int) (mRectWidthHeightRatio * mRectHeight);
            } else {
                mRectWidth = (int) (getWidth() * mRectWidthRatio);
                mRectHeight = (int) (mRectWidthHeightRatio * mRectWidth);
            }
        }

        if (mRectWidth > getWidth()) {
            mRectWidth = getWidth() - MIN_DIMENSION_DIFF;
        }

        if (mRectHeight > getHeight()) {
            mRectHeight = getHeight() - MIN_DIMENSION_DIFF;
        }

        int leftOffset = (getWidth() - mRectWidth) / 2;
        int topOffset = (getHeight() - mRectHeight) / 2 + mRectTopOffset;
        mFramingRect = new Rect(leftOffset, topOffset, leftOffset + mRectWidth, topOffset + mRectHeight);
        mScanLineTop = mFramingRect.top + 0.5f;

        if (TextUtils.isEmpty(mTipText)) {
            mStaticLayout = null;
            return;
        }
        if (mTextSingleLine) {
            mStaticLayout = new StaticLayout(mTipText, mTextPaint, getWidth(), Layout.Alignment.ALIGN_CENTER, 1.0f, 0f, true);
        } else {
            mStaticLayout = new StaticLayout(mTipText, mTextPaint, mRectWidth, Layout.Alignment.ALIGN_CENTER, 1.0f, 0f, true);
        }
    }
}
