package com.zeusis.gpstools.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class HistogramView extends View {

    private float mWidth;
    private int mColor;
    private int mMax;
    private int mLength;
    private int widthSize;
    private int heightSize;

    public HistogramView(Context context) {
        super(context);
    }

    public HistogramView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public HistogramView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setLineWidth(float width) {
        this.mWidth = width;
        invalidate();
    }

    public void setLineColor(int color) {
        this.mColor = color;
    }

    public void setLineMaxLength(int max) {
        this.mMax = max;
    }

    public void setLineLength(int length) {
        this.mLength = length;
    }
    /*public void setHeightSize(int heightSize) {
        this.heightSize = heightSize;
    }*/
    public void setWidthSize(int widthSize) {
        this.widthSize = widthSize;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //widthSize = (int)(MeasureSpec.getSize(widthMeasureSpec) * 0.7f);
        heightSize = MeasureSpec.getSize(heightMeasureSpec);
        //setMeasuredDimension(widthSize * mLength / mMax, heightSize);
        setMeasuredDimension(widthSize,heightSize * mLength / mMax);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint paint = new Paint();
        paint.setStrokeWidth(widthSize);
        paint.setColor(mColor);
        //canvas.drawLine(0, widthSize / 2, widthSize * mLength / mMax, widthSize / 2, paint);
        canvas.drawLine(widthSize/2,heightSize,widthSize/2,(heightSize-heightSize * mLength/mMax),paint);
//        paint.setColor(Color.BLACK);
//        paint.setStrokeWidth(3);
//        canvas.drawLine(0,0,0,heightSize,paint);
    }
}
