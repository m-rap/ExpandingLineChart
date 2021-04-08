package com.mrap.chart.sandbox;

import android.app.Activity;
import android.os.Bundle;

import com.mrap.chart.ExpandingLineChart;
import com.mrap.chart.R;

import java.util.ArrayList;

import androidx.annotation.Nullable;

public class HomeActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout);

        ExpandingLineChart expandingLineChart = findViewById(R.id.expandingLineChart);

        ArrayList<String> legend = new ArrayList<>();
        ArrayList<ArrayList<ExpandingLineChart.PointD>> datasets = new ArrayList<>();
        ArrayList<String> colors = new ArrayList<>();

        legend.add("A");
        ArrayList<ExpandingLineChart.PointD> dataset = new ArrayList<>();
        datasets.add(dataset);
        dataset.add(new ExpandingLineChart.PointD(10, 10));
        dataset.add(new ExpandingLineChart.PointD(20, 15));
        dataset.add(new ExpandingLineChart.PointD(30, 12345));
        dataset.add(new ExpandingLineChart.PointD(40, 20));
        dataset.add(new ExpandingLineChart.PointD(50, 25));
        colors.add("#FF0000");

        legend.add("B");
        dataset = new ArrayList<>();
        datasets.add(dataset);
        dataset.add(new ExpandingLineChart.PointD(10, 5));
        dataset.add(new ExpandingLineChart.PointD(30, 20));
        dataset.add(new ExpandingLineChart.PointD(60, 20));
        colors.add("#00FF00");

        legend.add("C");
        dataset = new ArrayList<>();
        datasets.add(dataset);
        dataset.add(new ExpandingLineChart.PointD(15, 5));
        dataset.add(new ExpandingLineChart.PointD(20, 20));
        dataset.add(new ExpandingLineChart.PointD(30, 8878));
        dataset.add(new ExpandingLineChart.PointD(55, 30));
        colors.add("#0000FF");

        ExpandingLineChart.Ticks xTicks = new ExpandingLineChart.Ticks();
        xTicks.enabled = false;

//        expandingLineChart.setXTicks(xTicks);
//        expandingLineChart.setYValueLabelEnabled(false);
//        expandingLineChart.setXValueLabelEnabled(true);
//        expandingLineChart.setData(legend, datasets, colors);

        ExpandingLineChart.Params params = new ExpandingLineChart.Params();
        params.xTicks = xTicks;
        params.yValueLabelEnabled = false;
        params.xValueLabelEnabled = true;
        params.legend = legend;
        params.datasets = datasets;
        params.colors = colors;

        expandingLineChart.setParams(params);
    }
}
