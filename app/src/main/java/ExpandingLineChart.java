package com.mrap.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import androidx.annotation.Nullable;

public class ExpandingLineChart extends View {

    public static class PointD {
        double x;
        double y;

        public PointD(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private static final String TAG = "ExpandingLineChart";
    //ReadableMap data = null;
    float[] toDraw = new float[4000];
    float[] toDraw2 = new float[4000];
    float[] vtxBuff = new float[1000];
    float[] vtxBuff2 = new float[1000];
    int[] toDrawSizes = new int[100];

    Path path = new Path();

    int xAlreadyDrawn = 0;

    double yMin = Double.MAX_VALUE;
    double yMax = Double.MIN_VALUE;
    double xMin = Double.MAX_VALUE;
    double xMax = Double.MIN_VALUE;

    ArrayList<ArrayList<PointD>> datasetList = new ArrayList<>();
    ArrayList<String> labelList = new ArrayList<>();

    Paint[] paint = new Paint[] {
            new Paint(), new Paint(), new Paint()
    };

    public ExpandingLineChart(Context context) {
        this(context, null);
    }

    public ExpandingLineChart(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint[0].setColor(Color.RED);
        paint[0].setStrokeWidth(8);

        paint[1].setColor(Color.GREEN);
        paint[1].setStrokeWidth(8);

        paint[2].setColor(Color.BLUE);
        paint[2].setStrokeWidth(8);
    }

    public void setData(ArrayList<String> labels, ArrayList<ArrayList<PointD>> datasets) {
        yMax = Float.MIN_VALUE;
        yMin = Float.MAX_VALUE;
        xMax = Float.MIN_VALUE;
        xMin = Float.MAX_VALUE;
        xAlreadyDrawn = 0;

        labelList = labels;
        datasetList = datasets;

        Log.d(TAG, "setData");

        Comparator<PointD> comparator = new Comparator<PointD>() {
            @Override
            public int compare(PointD o1, PointD o2) {
                return Double.compare(o1.x, o2.x);
            }
        };

        for (int i = 0; i < labels.size(); i++) {
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

        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        Log.d(TAG, "onMeasure");

        xAlreadyDrawn = 0;
        
        //invalidate();
    }

    ArrayList<Float> toDrawList = new ArrayList<>();

    int toDrawCountMax = 100;

    @Override
    protected void onDraw(Canvas canvas) {
        //if (data == null) {
        if (datasetList.size() == 0) {
            super.onDraw(canvas);
            return;
        }

        Log.d(TAG, "onDraw " + xAlreadyDrawn);
//        ReadableMap datasets = data.getMap("datasets");
//        ReadableArray legend = data.getArray("legend");
        int maxX = 0;
//        for (int i = 0; i < legend.size(); i++) {
        for (int i = 0; i < labelList.size(); i++) {
            toDrawSizes[i] = 0;

            //int labelSize = datasets.getArray(legend.getString(i)).size();

//            int labelSize = 0;
//            ReadableMap dataset = datasets.getMap(legend.getString(i)).getMap("data");
//            ReadableMapKeySetIterator it = dataset.keySetIterator();
//            for (; it.hasNextKey(); it.nextKey(), labelSize++);

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

        toDrawList.clear();

        for (int j = 0; toDrawCount < toDrawCountMax && j < maxX; j++, xIdx++) {
//            s = new StringBuilder();
            //for (int i = 0; i < legend.size(); i++) {
            for (int i = 0; i < labelList.size(); i++) {
                //ReadableMap dataset = datasets.getMap(legend.getString(i)).getMap("data");
                //ArrayList<Long> legendDatetimeList = datasetList.get(i);

                ArrayList<PointD> dataset = datasetList.get(i);

                //if (j < legendDatetimeList.size()) {
                if (xIdx < dataset.size()) {
                    //long datetime = legendDatetimeList.get(xIdx);
                    //float val = (float)dataset.getDouble("d" + datetime);

                    long datetime = (long)dataset.get(xIdx).x;
                    float val = (float)dataset.get(xIdx).y;

                    float scaledX = (float) (((double)datetime - xMin) * canvas.getWidth() / (xMax - xMin));
                    float scaledY = (float) (((double)val - yMin) * canvas.getHeight() / (yMax - yMin));

                    int toDrawIdx = i * 200 + j * 2;

//                    s.append(labelList.get(i)).append(" ").append(datetime).append(" ").append(val).
//                            append(" ").append(scaledX).append(" ").append(scaledY).append(" ");
//                    Log.d(TAG, labelList.get(i) + " " + "(" + i + "," + j + ") " + datetime + " " + val +
//                            " " + scaledX + " " + scaledY + " ");

                    toDraw[toDrawIdx] = scaledX;
                    toDraw[toDrawIdx + 1] = scaledY;

                    toDraw2[toDrawIdx] = datetime;
                    toDraw2[toDrawIdx + 1] = val;

                    toDrawSizes[i]++;
                    toDrawCount++;
                }
            }

//            Log.d(TAG, j + " " + s.toString());
        }

        for (int i = 0; i < labelList.size(); i++) {
            String p = "";
            for (int j = 0; j < toDrawSizes[i]; j++) {
                int toDrawIdx = i * 200 + j * 2;
                p += toDrawIdx + " " + (toDrawIdx + 1) + " ";
            }
            Log.d(TAG, p);
        }

        for (int i = 0; i < labelList.size(); i++) {
//            StringBuilder s = new StringBuilder();
//            StringBuilder s2 = new StringBuilder();
            int vtxIdx = 0;
            for (int j = 0; j < toDrawSizes[i] - 1; j++) {
                int toDrawIdx = i * 200 + j * 2;
                int toDrawIdx2 = i * 200 + (j + 1) * 2;

                vtxBuff[vtxIdx++] = toDraw[toDrawIdx];
                vtxBuff[vtxIdx++] = toDraw[toDrawIdx + 1];
                vtxBuff[vtxIdx++] = toDraw[toDrawIdx2];
                vtxBuff[vtxIdx++] = toDraw[toDrawIdx2 + 1];
            }

            canvas.drawLines(vtxBuff, 0, vtxIdx, paint[i % paint.length]);

//            for (int j = 0; j < toDrawSizes[i]; j++) {
//                int toDrawIdx = i * 200 + j * 2;
//                vtxBuff[vtxIdx] = toDraw[toDrawIdx];
//                vtxBuff2[vtxIdx] = toDraw2[toDrawIdx];
//                vtxIdx++;
//                vtxBuff[vtxIdx] = toDraw[toDrawIdx + 1];
//                vtxBuff2[vtxIdx] = toDraw2[toDrawIdx + 1];
//                vtxIdx++;
//                if (j == 0) {
//                    path.moveTo(toDraw[toDrawIdx], toDraw[toDrawIdx + 1]);
//                } else {
//                    path.lineTo(toDraw[toDrawIdx], toDraw[toDrawIdx + 1]);
//                }
//                s.append(vtxBuff[vtxIdx - 2]).append(",").append(vtxBuff[vtxIdx - 1]).append(" ");
//                s2.append(vtxBuff2[vtxIdx - 2]).append(",").append(vtxBuff2[vtxIdx - 1]).append(" ");
//
//            }
//            Log.d(TAG, "vtxBuff " + s.toString() + " " + toDrawSizes[i] + " " + vtxIdx);
//            Log.d(TAG, "vtxBuff2 " + s2.toString() + " " + toDrawSizes[i] + " " + vtxIdx);
//
//            canvas.drawPath(path, paint[i % paint.length]);

//            canvas.drawLines(vtxBuff, 0, vtxIdx, paint[i % paint.length]);

//            for (int j = 0; j < toDrawSizes[i] - 1; j++) {
//                int toDrawIdx = i * 200 + j * 2;
//                int toDrawIdx2 = i * 200 + (j + 1) * 2;
//
//                canvas.drawLine(toDraw[toDrawIdx], toDraw[toDrawIdx + 1], toDraw[toDrawIdx2], toDraw[toDrawIdx2 + 1], paint[(i + 1) % paint.length]);
//            }
        }

        xAlreadyDrawn = xIdx;
        Log.d(TAG, "onDraw end " + xAlreadyDrawn + " " + maxX);


        if (xAlreadyDrawn >= maxX) {
            xAlreadyDrawn = 0;

            if (toDrawCountMax == 100) {
                toDrawCountMax = 6;
            } else {
                toDrawCountMax = 100;
            }
        }

//        if (xAlreadyDrawn < maxX) {
//            postInvalidate();
//        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        }, 1000);
    }
}
