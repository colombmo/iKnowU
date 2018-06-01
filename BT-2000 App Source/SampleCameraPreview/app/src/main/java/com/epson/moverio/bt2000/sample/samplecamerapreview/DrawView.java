package com.epson.moverio.bt2000.sample.samplecamerapreview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

/**
 * Created by moreno on 19.12.2017.
 */

public class DrawView extends android.support.v7.widget.AppCompatImageView{
    int[] rects = new int[]{};
    String[] names = new String[]{};

    public DrawView(Context context) {
        super(context);
    }

    public DrawView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DrawView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    public void update(int[] rectangles, String[] namesa){
        rects=rectangles;
        names=namesa;

        this.invalidate();
    }

    @Override
    public void onDraw(Canvas canvas){
        super.onDraw(canvas);
        Paint myPaint = new Paint();
        myPaint.setStyle(Paint.Style.STROKE);
        myPaint.setStrokeWidth(2);
        for(int i=0; i< names.length; i++) {
            int left = (rects[i*4]*this.getWidth())/640/2;
            int top = (rects[i*4+1]*this.getHeight())/480/2;
            int right = (rects[i*4+2]*this.getWidth())/640/2;
            int bot = (rects[i*4+3]*this.getHeight())/480/2;

            if (names[i].length()>1){
                myPaint.setColor(Color.GREEN);
                myPaint.setTextSize(40);
                canvas.drawText("Pas", left, bot+20, myPaint);
            }else{
                myPaint.setColor(Color.RED);
            }
            this.bringToFront();
            myPaint.setStrokeWidth(5);
            canvas.drawRect(left,top,right,bot, myPaint);
        }
    }
}
