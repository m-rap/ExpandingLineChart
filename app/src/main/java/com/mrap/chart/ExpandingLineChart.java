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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ExpandingLineChart extends View {
  public static int TYPE_NUMBER = 0;
  public static int TYPE_DATE = 1;
  public static int TYPE_NOTYPE = -1;

  public static class Params {
    public Ticks xTicks = new Ticks();
    public Ticks yTicks = new Ticks();
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
    public LabelFormatterCallback xLabelFormatterCallback = null;
    public LabelFormatterCallback yLabelFormatterCallback = null;
  }

  public interface LabelFormatterCallback {
    public String onLabelFormat(double value);
  }

  protected Canvas bmpCanvas = null;
  protected boolean clearBmp = false;

  public static class PointD {
    public double x;
    public double y;

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
      return sdf.format(new Date((long) value * 1000));
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
  protected int chartBmpX;
  protected int chartBmpY;

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
    yAxisTextPaint.setTextSize(10 * scaledDensity);

    xAxisTextPaint = new Paint();
//        xAxisTextPaint.setTextAlign(Paint.Align.CENTER);
    xAxisTextPaint.setTextAlign(Paint.Align.RIGHT);
    xAxisTextPaint.setTextSize(10 * scaledDensity);

    gridPaint = new Paint();
    gridPaint.setColor(Color.GRAY);

    axisPaint = new Paint();
    axisPaint.setStrokeWidth((int) (1 * density));

    padding = (int) (3 * density);
    axisTextPadding = padding;

    chartBmpX = (int) (50 * density);
    chartBmpY = padding;
  }

  public void setParams(Params params) {
    interval = 1000 / params.fps;
    this.drawCountPerFrame = params.drawCountPerFrame;

    if (params.legend != null) {
      setDataIntern(params.legend, params.datasets, params.colors);
    }

    Log.d(TAG, "setParams xTicks " + params.xTicks + " yTicks " + params.yTicks);

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

    if (params.xLabelFormatterCallback != null) {
      xLabelFormatterCallback = params.xLabelFormatterCallback;
    }

    if (params.yLabelFormatterCallback != null) {
      yLabelFormatterCallback = params.yLabelFormatterCallback;
    }

    Log.d(TAG, "setParams finished, postInvalidate, running: " + running);
    postInvalidate();

    xAlreadyDrawn = 0;
    clearBmp = true;
    if (!running) {
      running = true;
      drawChart(bmpCanvas);
    }
  }

  public void setFps(int fps) {
    interval = 1000 / fps;
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
    Log.d(TAG, "setDataIntern");
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
//            Log.d(TAG, "setDataIntern dataset " + i + " " + legend.get(i));
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
//            Log.d(TAG, i + " sorting");
      Collections.sort(dataset, comparator);
    }
    float density = getContext().getResources().getDisplayMetrics().density;

    paintList.clear();
    for (String colStr : colors) {
      if (colStr == null) {
        paintList.add(null);
        continue;
      }
      Paint p = new Paint();
      p.setStrokeWidth(2 * density);
      p.setColor(Color.parseColor(colStr));
      p.setAntiAlias(true);
      paintList.add(p);
    }

    calcTicks(yTicks, yMin, yMax);
    calcTicks(xTicks, xMin, xMax);

    recreateChartBmp(getMeasuredWidth(), getMeasuredHeight());

    Log.d(TAG, "setDataIntern finished");
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
    synchronized (ticks) {
//      if (ticks.overrideValueMin && ticks.overrideValueMax) {
//        return;
//      }

      int ticksCount;
      ticks.appliedInterval = ticks.interval;

      int stateOnlyCalcDiv = 0;
      int stateIgnoreCountMax = 1;
      int stateWillStop = -1;

      int currState = stateOnlyCalcDiv;

      while (true) {
        if (!ticks.overrideValueMin) {
          double mod = min % ticks.appliedInterval;
          if (mod / ticks.appliedInterval > 0.5) {
            ticks.valueMin = min - mod;
          } else {
            ticks.valueMin = min - (ticks.appliedInterval + mod);
//                Log.d(TAG, "min " + (long)min + " " + (long)ticks.appliedInterval + " " +
//                        (long)((min % ticks.appliedInterval)) + " " +
//                        (long)(ticks.appliedInterval + (min % ticks.appliedInterval)) + " " +
//                        (long)ticks.valueMin);
          }
        }
        if (!ticks.overrideValueMax) {
          ticks.valueMax = Math.floor(max / ticks.appliedInterval) * ticks.appliedInterval;
          if (ticks.valueMax <= max) {
            ticks.valueMax += ticks.appliedInterval;
          }
        }
        double range = ticks.valueMax - ticks.valueMin;

        ticksCount = (int) (range / ticks.appliedInterval);
//        Log.d(TAG, "calcTicks " + ((ticks == xTicks) ? "xTicks" : "yTicks") + " range " + range +
//          " appliedInterval " + ticks.appliedInterval + " ticks count " + ticksCount + " state " +
//          currState);
        if (ticksCount <= ticks.countMax) {
          break;
        }

        if (currState == stateOnlyCalcDiv) {
        } else if (currState == stateIgnoreCountMax) {
          break;
        }

        double div = range / ticks.countMax;
        ticks.appliedInterval = Math.ceil(div / ticks.interval) * ticks.interval;
//                Log.d(TAG, String.format("div %.3f = %.3f / %d", div, range, ticks.countMax));
//                Log.d(TAG, String.format("Math.ceil(div / ticks.interval) %.3f", Math.ceil(div / ticks.interval)));
//                Log.d(TAG, String.format("ap %.3f = %.3f * %.3f", ticks.appliedInterval, Math.ceil(div / ticks.interval), ticks.interval));

        if (!ticks.overrideValueMax) {
          ticks.valueMax = Math.floor(max / ticks.appliedInterval) * ticks.appliedInterval;
          if (ticks.valueMax <= max) {
            ticks.valueMax += ticks.appliedInterval;
          }
        } else {
          if (currState == stateOnlyCalcDiv) {
            currState = stateIgnoreCountMax;
          }
        }

//                Log.d(TAG, String.format("ticks minmax %.3f %.3f vminmax %.3f %.3f > %.3f apint %.3f %.3f c %d", min, max,
//                        ticks.valueMin, ticks.valueMax, range, ticks.appliedInterval, ticks.interval, ticks.countMax));
      }
//        Log.d(TAG, String.format("post ticks minmax %.3f %.3f vminmax %.3f %.3f > %.3f apint %.3f %.3f c %d %d", min, max,
//                ticks.valueMin, ticks.valueMax, range, ticks.appliedInterval, ticks.interval, ticks.countMax, ticksCount));
    }
  }

  boolean running = false;

  protected void recreateChartBmp(int w, int h) {
    //    float density = getContext().getResources().getDisplayMetrics().density;

//        int bottom = (int)yAxisTextPaint.getTextSize() + 2 * padding;
    int bottom = (int) yAxisTextPaint.measureText("00000") + padding + axisTextPadding;
    int top = padding;
    int left = (int) xAxisTextPaint.measureText(String.format("%.0f", yTicks.valueMax)) + padding + axisTextPadding;
    int right = padding;
    chartBmpX = left;
    chartBmpY = top;
    Log.d(TAG, String.format("w %d %d %d, h %d %d %d", w, left, right, h, top, bottom));

    int bmpW = w - left - right;
    int bmpH = h - top - bottom;

    if (bmpW <= 0 || bmpH <= 0) {
      chartBmp = null;
      bmpCanvas = null;
      return;
    }

    chartBmp = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888);
    bmpCanvas = new Canvas(chartBmp);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    onResize(getMeasuredWidth(), getMeasuredHeight());
  }

  @Override
  protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
    super.onVisibilityChanged(changedView, visibility);

    if (visibility != VISIBLE) {
      return;
    }

    onResize(getMeasuredWidth(), getMeasuredHeight());
  }

  private void onResize(int width, int height) {
    xAlreadyDrawn = 0;
    clearBmp = true;

//        int w = getMeasuredWidth(), h = getMeasuredHeight();
    int w = width, h = height;
    Log.d(TAG, "onResize w h " + w + " " + h);
    if (w == 0 || h == 0) {
      return;
    }

    recreateChartBmp(w, h);

    if (!running) {
      running = true;
      drawChart(bmpCanvas);
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    if (getVisibility() != VISIBLE || datasetList.size() == 0 || chartBmp == null) {
      super.onDraw(canvas);
      return;
    }

    float scaledDensity = getContext().getResources().getDisplayMetrics().scaledDensity;
    float density = getContext().getResources().getDisplayMetrics().density;

    if (yTicks.enabled) {
      synchronized (yTicks) {
        for (double currTicks = yTicks.valueMin; currTicks <= yTicks.valueMax; currTicks += yTicks.appliedInterval) {
//          Log.d(TAG, "drawing ytick " + currTicks + " max " + yTicks.valueMax + " itvl " + yTicks.appliedInterval);
          double y = chartBmpY + (chartBmp.getHeight() - ((currTicks - yTicks.valueMin) * chartBmp.getHeight() / (yTicks.valueMax - yTicks.valueMin)));
          //Log.v(TAG, "currTicks " + currTicks + " " + ticksValueMax + " " + y);
          canvas.drawLine(chartBmpX, (float) y, chartBmpX + chartBmp.getWidth(), (float) y, gridPaint);
//                Log.d(TAG, "formatting ticks " + (long)currTicks + String.format(" %.0f", currTicks));
          String ticksText = formatLabel(currTicks, yLabelFormatterCallback);
          canvas.drawText(ticksText, chartBmpX - axisTextPadding, (float) y + (yAxisTextPaint.getTextSize() / 2), yAxisTextPaint);
        }
      }
    }

    if (xTicks.enabled) {
      synchronized (xTicks) {
        for (double currTicks = xTicks.valueMin; currTicks <= xTicks.valueMax; currTicks += xTicks.appliedInterval) {
//          Log.d(TAG, "drawing xtick " + currTicks + " max " + xTicks.valueMax + " itvl " + xTicks.appliedInterval);
          double val = currTicks;
          double[] xy = new double[2];
          drawXLabel(canvas, val, xy);
          double x = xy[0], y = xy[1];
          canvas.drawLine((float) x, chartBmpY, (float) x, (float) y, gridPaint);
        }
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
          double[] xy = new double[2];
          drawXLabel(canvas, val, xy);
          double x = xy[0], y = xy[1];
          canvas.drawLine((float) x, chartBmpY, (float) x, (float) y, gridPaint);
        }
      }
    }

    canvas.drawLine(chartBmpX, chartBmpY, chartBmpX, chartBmpY + chartBmp.getHeight() + (axisPaint.getStrokeWidth() / 2), axisPaint);
    canvas.drawLine(chartBmpX, chartBmpY + chartBmp.getHeight(), chartBmpX + chartBmp.getWidth(), chartBmpY + chartBmp.getHeight(), axisPaint);

    Rect rect = new Rect(chartBmpX, chartBmpY, chartBmpX + chartBmp.getWidth(), chartBmpY + chartBmp.getHeight());
    canvas.drawBitmap(chartBmp, null, rect, paintList.get(0));
  }

  private void drawXLabel(Canvas canvas, double val, double[] xy) {
    double x = chartBmpX + ((val - xTicks.valueMin) * chartBmp.getWidth() / (xTicks.valueMax - xTicks.valueMin));
    double y = chartBmpY + chartBmp.getHeight();
    String ticksText = formatLabel(val, xLabelFormatterCallback);
    canvas.save();
    canvas.translate((float) x + xAxisTextPaint.getTextSize() / 2, (float) y + xAxisTextPaint.getTextSize() / 2 + axisTextPadding);
    canvas.rotate(-45);
    canvas.drawText(ticksText, 0, 0, xAxisTextPaint);
    canvas.restore();
    xy[0] = x;
    xy[1] = y;
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

    if (getVisibility() != VISIBLE) {
      running = false;
      return;
    }

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
        if (legendList.get(i) == null) {
          continue;
        }
        ArrayList<PointD> dataset = datasetList.get(i);

        if (xIdx < dataset.size()) {
          long datetime = (long) dataset.get(xIdx).x;
          float val = (float) dataset.get(xIdx).y;

//                    float scaledX = (float) ((datetime - xMin) * canvas.getWidth() / (xMax - xMin));
          float scaledX = (float) ((datetime - xTicks.valueMin) * canvas.getWidth() / (xTicks.valueMax - xTicks.valueMin));
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
      if (legendList.get(i) == null) {
        continue;
      }
      int vtxIdx = 0;
      for (int j = 0; j < toDrawSizes[i] - 1; j++) {
        int toDrawIdx = i * 200 + j * 2;
        int toDrawIdx2 = i * 200 + (j + 1) * 2;

        vtxBuff[vtxIdx++] = toDraw[toDrawIdx];
        vtxBuff[vtxIdx++] = toDraw[toDrawIdx + 1];
        vtxBuff[vtxIdx++] = toDraw[toDrawIdx2];
        vtxBuff[vtxIdx++] = toDraw[toDrawIdx2 + 1];
      }

      Paint p = paintList.get(i % paintList.size());
      canvas.drawLines(vtxBuff, 0, vtxIdx, p);
      if (vtxIdx == 4 &&
          vtxBuff[0] == vtxBuff[2] &&
          vtxBuff[1] == vtxBuff[3]) {
        canvas.drawCircle(vtxBuff[0], vtxBuff[1], p.getStrokeWidth() / 2, p);
      }
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
