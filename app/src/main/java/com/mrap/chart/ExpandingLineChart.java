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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import androidx.annotation.Nullable;

public class ExpandingLineChart extends View {
    public static int TYPE_NUMBER = 0;
    public static int TYPE_DATE = 1;

    public static class Params {
        public Ticks xTicks = null;
        public Ticks yTicks = null;
        public boolean xValueLabelEnabled = true;
        public boolean yValueLabelEnabled = false;
        public ArrayList<String> legend = null;
        public ArrayList<ArrayList<PointD>> datasets = null;
        public ArrayList<String> colors = null;
        public int fps = 12;
        public int drawCountPerFrame = 1;
        public int xType = TYPE_NUMBER;
        public int yType = TYPE_NUMBER;
        public String xFormat = "";
        public String yFormat = "";
    }

    public interface LabelFormatterCallback {
        public String onLabelFormat(double value);
    }

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

    public static class NumberFormatter implements LabelFormatterCallback {

        String format;

        public NumberFormatter(String format) {
            this.format = format;
        }

        @Override
        public String onLabelFormat(double value) {
            if (format.isEmpty())
                return String.format("%.0f", value);
            return String.format(format, value);
        }
    }

    public static class DateFormatter implements LabelFormatterCallback {

        String format;
        SimpleDateFormat sdf;

        public DateFormatter(String format) {
            this.format = format;
            sdf = new SimpleDateFormat(format);
        }

        @Override
        public String onLabelFormat(double value) {
            if (format.isEmpty())
                return String.format("%.0f", value);
            return sdf.format(new Date((long)value));
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

    public static class Ticks {
        public boolean enabled = true;
        public double interval = 10;
        public int countMax = 10;
        private double appliedInterval = 10;
        public boolean overrideValueMin = false;
        public boolean overrideValueMax = false;
        public double valueMin = 0;
        public double valueMax = 0;
    }

    protected Ticks yTicks = new Ticks();
    protected Ticks xTicks = new Ticks();

    protected boolean yValueLabelEnabled = false;
    protected boolean xValueLabelEnabled = true;

    protected LabelFormatterCallback xLabelFormatterCallback = null;
    protected LabelFormatterCallback yLabelFormatterCallback = null;

    protected int xType = TYPE_NUMBER;
    protected int yType = TYPE_NUMBER;
    protected String xFormat = "";
    protected String yFormat = "";

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

    public void setParams(Params params) {
        interval = 1000/params.fps;
        this.drawCountPerFrame = params.drawCountPerFrame;

        if (params.legend != null) {
            setDataIntern(params.legend, params.datasets, params.colors);
        }

        if (params.xTicks != null) {
            setXTicksIntern(params.xTicks);
        }

        if (params.yTicks != null) {
            setYTicksIntern(params.yTicks);
        }

        xValueLabelEnabled = params.xValueLabelEnabled;
        yValueLabelEnabled = params.yValueLabelEnabled;

        if (params.xType == TYPE_NUMBER) {
            xLabelFormatterCallback = new NumberFormatter(params.xFormat);
        } else if (params.xType == TYPE_DATE) {
            xLabelFormatterCallback = new DateFormatter(params.xFormat);
        }

        if (params.yType == TYPE_NUMBER) {
            yLabelFormatterCallback = new NumberFormatter(params.yFormat);
        } else if (params.yType == TYPE_DATE) {
            yLabelFormatterCallback = new DateFormatter(params.yFormat);
        }

        postInvalidate();

        xAlreadyDrawn = 0;
        clearBmp = true;
        if (!running) {
            running = true;
            drawChart(bmpCanvas);
        }
    }

    public void setFps(int fps) {
        interval = 1000/fps;
    }

    public void setDrawCountPerFrame(int drawCountPerFrame) {
        this.drawCountPerFrame = drawCountPerFrame;
    }

    public void setData(ArrayList<String> legend, ArrayList<ArrayList<PointD>> datasets, ArrayList<String> colors) {
        setDataIntern(legend, datasets, colors);

        postInvalidate();

        xAlreadyDrawn = 0;
        clearBmp = true;
        if (!running) {
            running = true;
            drawChart(bmpCanvas);
        }
    }

    private void setDataIntern(ArrayList<String> legend, ArrayList<ArrayList<PointD>> datasets, ArrayList<String> colors) {
        yMax = Float.MIN_VALUE;
        yMin = Float.MAX_VALUE;
        xMax = Float.MIN_VALUE;
        xMin = Float.MAX_VALUE;

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

        calcTicks(yTicks, yMin, yMax);
        calcTicks(xTicks, xMin, xMax);
    }

    public void setXTicks(Ticks xTicks) {
        setXTicksIntern(xTicks);

        postInvalidate();
    }

    private void setXTicksIntern(Ticks xTicks) {
        this.xTicks = xTicks;

        calcTicks(xTicks, xMin, xMax);
    }

    public void setYTicks(Ticks yTicks) {
        setYTicksIntern(yTicks);

        postInvalidate();
    }

    private void setYTicksIntern(Ticks yTicks) {
        this.yTicks = yTicks;

        calcTicks(yTicks, yMin, yMax);
    }

    public void setXValueLabelEnabled(boolean v) {
        xValueLabelEnabled = v;
        postInvalidate();
    }

    public void setYValueLabelEnabled(boolean v) {
        yValueLabelEnabled = v;
        postInvalidate();
    }

    private void calcTicks(Ticks ticks, double min, double max) {
        int ticksCount;
        ticks.appliedInterval = ticks.interval;
        if (!ticks.overrideValueMin) {
            if (min >= 0) {
                ticks.valueMin = min - (min % ticks.appliedInterval);
            } else {
                ticks.valueMin = min + (min % ticks.appliedInterval);
            }
        }
        if (!ticks.overrideValueMax) {
            ticks.valueMax = Math.floor(max / ticks.appliedInterval) * ticks.appliedInterval;
            if (ticks.valueMax < max) {
                ticks.valueMax += ticks.appliedInterval;
            }
        }
        double range = ticks.valueMax - ticks.valueMin;
        while (true) {
            ticksCount = (int)(range / ticks.appliedInterval);
            if (ticksCount <= ticks.countMax) {
                break;
            } else {
                double div = range / ticks.countMax;
                ticks.appliedInterval = Math.floor(div / ticks.interval) * ticks.interval;
                ticks.valueMax = Math.floor(max / ticks.appliedInterval) * ticks.appliedInterval;
                if (ticks.valueMax < max) {
                    ticks.valueMax += ticks.appliedInterval;
                }
            }
        }

        Log.v(TAG, "ticks " + ticks.valueMin + " " + ticks.valueMax + " " + ticks.appliedInterval + " " + ticksCount);
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

        if (yTicks.enabled) {
            for (double currTicks = yTicks.valueMin; currTicks <= yTicks.valueMax; currTicks += yTicks.appliedInterval) {
                double y = chartBmpY + (chartBmp.getHeight() - ((currTicks - yTicks.valueMin) * chartBmp.getHeight() / (yTicks.valueMax - yTicks.valueMin)));
                //Log.v(TAG, "currTicks " + currTicks + " " + ticksValueMax + " " + y);
                canvas.drawLine(chartBmpX, (float) y, chartBmpX + chartBmp.getWidth(), (float) y, gridPaint);
                String ticksText = formatLabel(currTicks, yLabelFormatterCallback);
                canvas.drawText(ticksText, chartBmpX - axisTextPadding, (float) y + (yAxisTextPaint.getTextSize() / 2), yAxisTextPaint);
            }
        }

        if (xTicks.enabled) {
            for (double currTicks = xTicks.valueMin; currTicks <= xTicks.valueMax; currTicks += xTicks.appliedInterval) {
                double x = chartBmpX + ((currTicks - xTicks.valueMin) * chartBmp.getWidth() / (xTicks.valueMax - xTicks.valueMin));
                float y = chartBmpY + chartBmp.getHeight();
                //Log.v(TAG, "currTicks " + currTicks + " " + ticksValueMax + " " + y);
                canvas.drawLine((float)x, chartBmpY, (float)x, y, gridPaint);
                String ticksText = formatLabel(currTicks, xLabelFormatterCallback);
                canvas.drawText(ticksText, (float)x, y + xAxisTextPaint.getTextSize() + axisTextPadding, xAxisTextPaint);
            }
        }

        for (int i = 0; i < datasetList.size(); i++) {
            ArrayList<PointD> dataset = datasetList.get(i);
            for (int j = 0; j < dataset.size(); j++) {
                if (yValueLabelEnabled) {
                    double val = dataset.get(j).y;
                    double y = chartBmpY + (chartBmp.getHeight() - ((val - yTicks.valueMin) * chartBmp.getHeight() / (yTicks.valueMax - yTicks.valueMin)));
                    String ticksText = formatLabel(val, yLabelFormatterCallback);
                    canvas.drawText(ticksText,
                            chartBmpX - axisTextPadding, (float) y + (yAxisTextPaint.getTextSize() / 2), yAxisTextPaint);
                }
                if (xValueLabelEnabled) {
                    double val = dataset.get(j).x;
                    double x = chartBmpX + ((val - xTicks.valueMin) * chartBmp.getWidth() / (xTicks.valueMax - xTicks.valueMin));
                    float y = chartBmpY + chartBmp.getHeight();
                    String ticksText = formatLabel(val, xLabelFormatterCallback);
                    canvas.drawText(ticksText, (float)x, y + xAxisTextPaint.getTextSize() + axisTextPadding, xAxisTextPaint);
                }
            }
        }

        Rect rect = new Rect(chartBmpX, chartBmpY, chartBmpX + chartBmp.getWidth(), chartBmpY + chartBmp.getHeight());
        canvas.drawBitmap(chartBmp, null, rect, paintList.get(0));

        canvas.drawLine(chartBmpX, chartBmpY, chartBmpX, chartBmpY + chartBmp.getHeight() + (axisPaint.getStrokeWidth() / 2), axisPaint);
        canvas.drawLine(chartBmpX, chartBmpY + chartBmp.getHeight(), chartBmpX + chartBmp.getWidth(), chartBmpY + chartBmp.getHeight(), axisPaint);

//        canvas.drawText(ticksValueMax + "", chartBmpX - axisTextPadding, chartBmpY + (yAxisTextPaint.getTextSize() / 2), yAxisTextPaint);
//        canvas.drawText(ticksValueMin + "", chartBmpX - axisTextPadding, chartBmpY + (yAxisTextPaint.getTextSize() / 2) + chartBmp.getHeight(), yAxisTextPaint);

//        canvas.drawText(xMin + "", chartBmpX, chartBmpY + padding + xAxisTextPaint.getTextSize() + chartBmp.getHeight(), xAxisTextPaint);
//        canvas.drawText(xMax + "", chartBmpX + chartBmp.getWidth(), chartBmpY + padding + xAxisTextPaint.getTextSize() + chartBmp.getHeight(), xAxisTextPaint);
    }

    private String formatLabel(double val, LabelFormatterCallback labelFormatterCallback) {
//        Log.d(TAG, "formatLabel labelFormatterCallback " + (labelFormatterCallback == null ? "null" : "1"));
        String ticksText;
        if (labelFormatterCallback != null) {
            ticksText = labelFormatterCallback.onLabelFormat(val);
            if (ticksText == null) {
                ticksText = String.format("%.0f", val);
            }
        } else {
            ticksText = String.format("%.0f", val);
        }
        return ticksText;
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
                    float scaledY = (float) (canvas.getHeight() - ((val - yTicks.valueMin) * canvas.getHeight() / (yTicks.valueMax - yTicks.valueMin)));

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

        Log.v(TAG, "drawChart end " + xAlreadyDrawn + " " + maxX);


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
