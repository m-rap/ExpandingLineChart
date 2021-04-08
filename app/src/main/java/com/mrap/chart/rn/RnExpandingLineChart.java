package com.mrap.chart.rn;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.mrap.chart.ExpandingLineChart;
import com.mrap.savingstrackermobile_v1.R;

import java.util.ArrayList;
import java.util.Collections;

import androidx.annotation.Nullable;

public class RnExpandingLineChart extends ExpandingLineChart {

    private static final String TAG = "RnExpandingLineChart";

    public static class RnLabelFormatter implements LabelFormatterCallback {

        ReactContext reactContext;
        View view;

        public RnLabelFormatter(ReactContext context, View view) {
            reactContext = context;
            this.view = view;
        }

        @Override
        public String onLabelFormat(double value) {
            WritableMap args = Arguments.createMap();
            args.putDouble("value", value);
            args.putString("result", "");
            reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(view.getId(),
                    "formatXLabel", args);
            return args.getString("result");
        }
    }

    public RnExpandingLineChart(Context context) {
        super(context);
    }

    public RnExpandingLineChart(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        xLabelFormatterCallback = new RnLabelFormatter((ReactContext)context, this);
        yLabelFormatterCallback = new RnLabelFormatter((ReactContext)context, this);
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
                float val = (float)dataSet.getDouble(key);

                dataset.add(new PointD(datetime, val));
            }
        }

        setData(legend, datasets, colors);
    }
}
