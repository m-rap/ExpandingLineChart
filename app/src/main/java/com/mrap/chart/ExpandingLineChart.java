package com.mrap.chart;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import androidx.annotation.Nullable;

public class ExpandingLineChart extends View {

    protected Canvas bmpCanvas = null;
    protected boolean clearBmp = false;

    public static class PointD {
        double x;
        double y;

        public PointD(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private static final String TAG = "ExpandingLineChart";

    protected float[] toDraw = new float[4000];
    protected float[] vtxBuff = new float[1000];
    protected int[] toDrawSizes = new int[100];

    protected int xAlreadyDrawn = 0;

    protected double yMin = Double.MAX_VALUE;
    protected double yMax = Double.MIN_VALUE;
    protected double xMin = Double.MAX_VALUE;
    protected double xMax = Double.MIN_VALUE;

    protected ArrayList<ArrayList<PointD>> datasetList = new ArrayList<>();
    protected ArrayList<String> legendList = new ArrayList<>();

    protected ArrayList<Paint> paintList = new ArrayList<>();
    protected Paint yAxisTextPaint;
    protected Paint xAxisTextPaint;
    protected Paint gridPaint;
    protected Paint axisPaint;

    protected Bitmap chartBmp = null;

    protected int drawCountPerFrame = 1;
    protected long interval = 1000 / 12;

    protected int padding;
    protected int axisTextPadding;
    protected final int chartBmpX;
    protected final int chartBmpY;
    protected double ticksInterval = 10;
    protected int ticksCountMax = 10;
    protected double appliedTicksInterval = 10;
    protected double ticksValueMin = 0;
    protected double ticksValueMax = 0;

    public ExpandingLineChart(Context context) {
        this(context, null);
    }

    public ExpandingLineChart(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        float density = context.getResources().getDisplayMetrics().density;

        yAxisTextPaint = new Paint();
        yAxisTextPaint.setTextAlign(Paint.Align.RIGHT);
        yAxisTextPaint.setTextSize(12 * scaledDensity);

        xAxisTextPaint = new Paint();
        xAxisTextPaint.setTextAlign(Paint.Align.CENTER);
        xAxisTextPaint.setTextSize(12 * scaledDensity);

        gridPaint = new Paint();
        gridPaint.setColor(Color.GRAY);

        axisPaint = new Paint();
        axisPaint.setStrokeWidth((int)(3 * density));

        padding = (int)(3 * density);
        axisTextPadding = padding;

        chartBmpX = (int)(50 * density);
        chartBmpY = padding;
    }

    public void setFps(int fps) {
        interval = 1000/fps;
    }

    public void setDrawCountPerFrame(int drawCountPerFrame) {
        this.drawCountPerFrame = drawCountPerFrame;
    }

    public void setData(ArrayList<String> legend, ArrayList<ArrayList<PointD>> datasets, ArrayList<String> colors) {
        yMax = Float.MIN_VALUE;
        yMin = Float.MAX_VALUE;
        xMax = Float.MIN_VALUE;
        xMin = Float.MAX_VALUE;
        xAlreadyDrawn = 0;

        legendList = legend;
        datasetList = datasets;

        Comparator<PointD> comparator = new Comparator<PointD>() {
            @Override
            public int compare(PointD o1, PointD o2) {
                return Double.compare(o1.x, o2.x);
            }
        };

        for (int i = 0; i < legend.size(); i++) {
            ArrayList<PointD> dataset = datasets.get(i);
            for (int j = 0; j < dataset.size(); j++) {
                PointD p = dataset.get(j);
                if (p.x < xMin) {
                    xMin = p.x;
                }
                if (p.x > xMax) {
                    xMax = p.x;
                }
                if (p.y < yMin) {
                    yMin = p.y;
                }
                if (p.y > yMax) {
                    yMax = p.y;
                }
            }
            Collections.sort(dataset, comparator);
        }
        float density = getContext().getResources().getDisplayMetrics().density;

        paintList.clear();
        for (String colStr : colors) {
            Paint p = new Paint();
            p.setStrokeWidth(2 * density);
            p.setColor(Color.parseColor(colStr));
            p.setAntiAlias(true);
            paintList.add(p);
        }

        int ticksCount;
        appliedTicksInterval = ticksInterval;
        for (int i = 1; true; i++) {
            appliedTicksInterval = i * ticksInterval;
            if (yMin >= 0) {
                ticksValueMin = yMin - (yMin % appliedTicksInterval);
            } else {
                ticksValueMin = yMin + (yMin % appliedTicksInterval);
            }
            ticksValueMax = Math.floor(yMax / appliedTicksInterval) * appliedTicksInterval;
            ticksCount = (int)((ticksValueMax - ticksValueMin) / appliedTicksInterval);
            if (ticksCount <= ticksCountMax) {
                break;
            }
        }

        Log.v(TAG, "ticks " + ticksValueMin + " " + ticksValueMax + " " + appliedTicksInterval + " " + ticksCount);

        clearBmp = true;
        if (!running) {
            running = true;
            drawChart(bmpCanvas);
        }
    }

    boolean running = false;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        onResize(getMeasuredWidth(), getMeasuredHeight());
    }

    private void onResize(int width, int height) {
        xAlreadyDrawn = 0;

//        int w = getMeasuredWidth(), h = getMeasuredHeight();
        int w = width, h = height;
//        Log.d(TAG, "onMeasure w h " + w + " " + h);
        if (w == 0 || h == 0) {
            return;
        }

        float density = getContext().getResources().getDisplayMetrics().density;

        int bottom = (int)yAxisTextPaint.getTextSize() + padding;
        int top = padding;
        int left = (int)(50 * density);
        int right = padding + (int)(xAxisTextPaint.measureText(xMax + "") / 2);
        chartBmp = Bitmap.createBitmap(w - left - right, h - top - bottom, Bitmap.Config.ARGB_8888);
        bmpCanvas = new Canvas(chartBmp);
        if (!running) {
            running = true;
            drawChart(bmpCanvas);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (datasetList.size() == 0 || chartBmp == null) {
            super.onDraw(canvas);
            return;
        }

        float scaledDensity = getContext().getResources().getDisplayMetrics().scaledDensity;
        float density = getContext().getResources().getDisplayMetrics().density;

        for (double currTicks = ticksValueMin; currTicks <= ticksValueMax; currTicks += appliedTicksInterval) {
            double y = chartBmpY + (chartBmp.getHeight() - ((currTicks - ticksValueMin) * chartBmp.getHeight() / (ticksValueMax - ticksValueMin)));
            //Log.v(TAG, "currTicks " + currTicks + " " + ticksValueMax + " " + y);
            canvas.drawLine(chartBmpX, (float)y, chartBmpX + chartBmp.getWidth(), (float)y, gridPaint);
            canvas.drawText(String.format("%.0f", currTicks), chartBmpX - axisTextPadding, (float)y + (yAxisTextPaint.getTextSize() / 2), yAxisTextPaint);
        }

        Rect rect = new Rect(chartBmpX, chartBmpY, chartBmpX + chartBmp.getWidth(), chartBmpY + chartBmp.getHeight());
        canvas.drawBitmap(chartBmp, null, rect, paintList.get(0));

        canvas.drawLine(chartBmpX, chartBmpY, chartBmpX, chartBmpY + chartBmp.getHeight() + (axisPaint.getStrokeWidth() / 2), axisPaint);
        canvas.drawLine(chartBmpX, chartBmpY + chartBmp.getHeight(), chartBmpX + chartBmp.getWidth(), chartBmpY + chartBmp.getHeight(), axisPaint);

//        canvas.drawText(ticksValueMax + "", chartBmpX - axisTextPadding, chartBmpY + (yAxisTextPaint.getTextSize() / 2), yAxisTextPaint);
//        canvas.drawText(ticksValueMin + "", chartBmpX - axisTextPadding, chartBmpY + (yAxisTextPaint.getTextSize() / 2) + chartBmp.getHeight(), yAxisTextPaint);

        canvas.drawText(xMin + "", chartBmpX, chartBmpY + padding + xAxisTextPaint.getTextSize() + chartBmp.getHeight(), xAxisTextPaint);
        canvas.drawText(xMax + "", chartBmpX + chartBmp.getWidth(), chartBmpY + padding + xAxisTextPaint.getTextSize() + chartBmp.getHeight(), xAxisTextPaint);
    }

    private void drawChart(Canvas canvas) {
        Log.v(TAG, "drawChart clear " + clearBmp + " xAlreadyDrawn " + xAlreadyDrawn + " canvas " + ((canvas != null) ? "1" : "null"));

        if (canvas == null) {
            new Handler(getContext().getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    drawChart(bmpCanvas);
                }
            }, interval);

            return;
        }

        if (clearBmp) {
            clearBmp = false;
//            canvas.drawColor(Color.WHITE);
//            canvas.drawColor(Color.parseColor("#00ffffff"));
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            new Handler(getContext().getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        drawChart(bmpCanvas);
                    }
                }, interval);

            invalidate();

            return;
        }

        int maxX = 0;
        for (int i = 0; i < legendList.size(); i++) {
            toDrawSizes[i] = 0;

            int labelSize = datasetList.get(i).size();

            if (labelSize > maxX) {
                maxX = labelSize;
            }
        }
        int toDrawCount = 0;
        int xIdx = xAlreadyDrawn;
        if (xIdx > 0) {
            xIdx--;
        }

        for (int j = 0; toDrawCount < drawCountPerFrame && j < maxX; j++, xIdx++) {
            for (int i = 0; i < legendList.size(); i++) {
                ArrayList<PointD> dataset = datasetList.get(i);

                if (xIdx < dataset.size()) {
                    long datetime = (long)dataset.get(xIdx).x;
                    float val = (float)dataset.get(xIdx).y;

                    float scaledX = (float) ((datetime - xMin) * canvas.getWidth() / (xMax - xMin));
//                    float scaledY = (float) ((val - yMin) * canvas.getHeight() / (yMax - yMin));
//                    float scaledY = (float) (canvas.getHeight() - ((val - yMin) * canvas.getHeight() / (yMax - yMin)));
                    float scaledY = (float) (canvas.getHeight() - ((val - ticksValueMin) * canvas.getHeight() / (ticksValueMax - ticksValueMin)));

                    int toDrawIdx = i * 200 + j * 2;

                    toDraw[toDrawIdx] = scaledX;
                    toDraw[toDrawIdx + 1] = scaledY;

                    toDrawSizes[i]++;

                    if (j > 0) {
                        toDrawCount++;
                    }
                }
            }
        }

//        for (int i = 0; i < labelList.size(); i++) {
//            String p = "";
//            for (int j = 0; j < toDrawSizes[i]; j++) {
//                int toDrawIdx = i * 200 + j * 2;
//                p += toDrawIdx + " " + (toDrawIdx + 1) + " ";
//            }
//            Log.d(TAG, p);
//        }

        for (int i = 0; i < legendList.size(); i++) {
            int vtxIdx = 0;
            for (int j = 0; j < toDrawSizes[i] - 1; j++) {
                int toDrawIdx = i * 200 + j * 2;
                int toDrawIdx2 = i * 200 + (j + 1) * 2;

                vtxBuff[vtxIdx++] = toDraw[toDrawIdx];
                vtxBuff[vtxIdx++] = toDraw[toDrawIdx + 1];
                vtxBuff[vtxIdx++] = toDraw[toDrawIdx2];
                vtxBuff[vtxIdx++] = toDraw[toDrawIdx2 + 1];
            }

            canvas.drawLines(vtxBuff, 0, vtxIdx, paintList.get(i % paintList.size()));
        }

        xAlreadyDrawn = xIdx;

        Log.v(TAG, "onDraw end " + xAlreadyDrawn + " " + maxX);


//        if (xAlreadyDrawn >= maxX) {
//            xAlreadyDrawn = 0;
//
//            clearBmp = true;
//
//            if (toDrawCountMax == 100) {
//                toDrawCountMax = 1;
//            } else {
//                toDrawCountMax = 100;
//            }
//        }

//        if (xAlreadyDrawn < maxX) {
//            postInvalidate();
//        }

        invalidate();

        if (xAlreadyDrawn < maxX) {
            Log.v(TAG, "to next drawChart");
            new Handler(getContext().getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    drawChart(bmpCanvas);
                }
            }, interval);
        } else {
            running = false;
        }
    }
}
