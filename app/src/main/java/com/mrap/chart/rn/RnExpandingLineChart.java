package com.mrap.chart.rn;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.mrap.chart.ExpandingLineChart;

import java.util.ArrayList;
import java.util.Collections;

import androidx.annotation.Nullable;

public class RnExpandingLineChart extends ExpandingLineChart {

    private static final String TAG = "RnExpandingLineChart";

    public RnExpandingLineChart(Context context) {
        super(context);
    }

    public RnExpandingLineChart(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    void setData(ReadableMap data) {
        ArrayList<String> legend = new ArrayList<>();
        ArrayList<ArrayList<PointD>> datasets = new ArrayList<>();
        ArrayList<String> colors = new ArrayList<>();

        ReadableArray rnLabels = data.getArray("legend");
        for (int i = 0; i < rnLabels.size(); i++) {
            String legendName = rnLabels.getString(i);
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
