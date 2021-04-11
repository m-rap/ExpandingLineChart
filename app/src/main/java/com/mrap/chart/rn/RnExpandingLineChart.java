package com.mrap.chart.rn;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.mrap.chart.ExpandingLineChart;

import java.util.ArrayList;

import androidx.annotation.Nullable;

public class RnExpandingLineChart extends ExpandingLineChart {

    private static final String TAG = "RnExpandingLineChart";

    public static class RnLabelFormatter implements LabelFormatterCallback {

        ReactContext reactContext;
        View view;
        String eventName;

        public RnLabelFormatter(ReactContext context, View view, String axis) {
            reactContext = context;
            this.view = view;
            if (axis.toLowerCase().equals("x")) {
                eventName = "topFormatXLabel";
            } else if (axis.toLowerCase().equals("y")) {
                eventName = "topFormatYLabel";
            }
        }

        @Override
        public String onLabelFormat(double value) {
            WritableMap args = Arguments.createMap();
            args.putDouble("value", value);
            args.putString("result", "");
//            Log.d(TAG, "event onLabelFormat " + eventName + " " + value);
            reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(view.getId(),
                    eventName, args);
            Log.d(TAG, "event onLabelFormat " + eventName + " value " + value + " result " + args.getString("result"));
            return args.getString("result");
        }
    }

    public RnExpandingLineChart(Context context) {
        this(context, null);
    }

    public RnExpandingLineChart(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
//        xLabelFormatterCallback = new RnLabelFormatter((ReactContext)context, this, "x");
//        yLabelFormatterCallback = new RnLabelFormatter((ReactContext)context, this, "y");
    }

    void setData(ReadableMap data) {
        ArrayList<String> legend = new ArrayList<>();
        ArrayList<ArrayList<PointD>> datasets = new ArrayList<>();
        ArrayList<String> colors = new ArrayList<>();

        ReadableArray legendRn = data.getArray("legend");
        for (int i = 0; i < legendRn.size(); i++) {
            String legendName = legendRn.getString(i);
            Log.d(TAG, "data set name " + legendName);
            legend.add(legendName);

            ReadableMap datasetContainer = data.getMap("datasets").getMap(legendName);

            String color = datasetContainer.getString("color");
            colors.add(color);

            ReadableMap dataSet = datasetContainer.getMap("data");

            ArrayList<PointD> dataset = new ArrayList<>();
            datasets.add(dataset);

            ReadableMapKeySetIterator it = dataSet.keySetIterator();
            for (; it.hasNextKey(); ) {
                String key = it.nextKey();
                long datetime = 0;
                try {
                    datetime = Long.parseLong(key.substring(1));
//                    Log.d(TAG, "parsed datetime " + datetime);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                double val = dataSet.getDouble(key);
//                if (legendName.equals("Total Saldo")) {
//                    Log.d(TAG, "java " + (long) datetime + " " + (long) val);
//                }

                dataset.add(new PointD(datetime, val));
            }
        }

        Params params = new Params();

        params.legend = legend;
        params.datasets = datasets;
        params.colors = colors;

        if (data.hasKey("xTicks")) {
            parseTicks(data, params, "xTicks");
        }
        if (data.hasKey("yTicks")) {
            parseTicks(data, params, "yTicks");
        }

        if (data.hasKey("xValueLabelEnabled")) {
            params.xValueLabelEnabled = data.getBoolean("xValueLabelEnabled");
        }
        if (data.hasKey("yValueLabelEnabled")) {
            params.yValueLabelEnabled = data.getBoolean("yValueLabelEnabled");
        }
        if (data.hasKey("fps")) {
            params.fps = data.getInt("fps");
        }
        if (data.hasKey("drawCountPerFrame")) {
            params.drawCountPerFrame = data.getInt("drawCountPerFrame");
        }
        if (data.hasKey("xType")) {
            String val = data.getString("xType");
            params.xType = val.equals("number") ? TYPE_NUMBER : val.equals("date") ? TYPE_DATE : -1;
        }
        if (data.hasKey("yType")) {
            String val = data.getString("yType");
            params.yType = val.equals("number") ? TYPE_NUMBER : val.equals("date") ? TYPE_DATE : -1;
        }
        if (data.hasKey("xFormat")) {
            String val = data.getString("xFormat");
            params.xFormat = val;
        }
        if (data.hasKey("yFormat")) {
            String val = data.getString("yFormat");
            params.yFormat = val;
        }

//        setData(legend, datasets, colors);
        setParams(params);
    }

    private void parseTicks(ReadableMap data, Params params, String key) {
        ReadableMap ticksRn = data.getMap(key);
        Ticks ticks = new Ticks();
        if (ticksRn.hasKey("enabled")) {
            ticks.enabled = ticksRn.getBoolean("enabled");
        }
        if (ticksRn.hasKey("interval")) {
            ticks.interval = ticksRn.getDouble("interval");
        }
        if (ticksRn.hasKey("countMax")) {
            ticks.countMax = ticksRn.getInt("countMax");
        }
        if (ticksRn.hasKey("valueMin")) {
            ticks.overrideValueMin = true;
            ticks.valueMin = ticksRn.getDouble("valueMin");
        }
        if (ticksRn.hasKey("valueMax")) {
            ticks.overrideValueMax = true;
            ticks.valueMax = ticksRn.getDouble("valueMax");
        }
        if (key.equals("xTicks")) {
            params.xTicks = ticks;
        } else if (key.equals("yTicks")) {
            params.yTicks = ticks;
        }
    }
}
