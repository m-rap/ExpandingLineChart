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

        ArrayList<String> labels = new ArrayList<>();
        ArrayList<ArrayList<ExpandingLineChart.PointD>> datasets = new ArrayList<>();

        labels.add("A");
        ArrayList<ExpandingLineChart.PointD> dataset = new ArrayList<>();
        datasets.add(dataset);
        dataset.add(new ExpandingLineChart.PointD(10, 10));
        dataset.add(new ExpandingLineChart.PointD(20, 15));
        dataset.add(new ExpandingLineChart.PointD(30, 10));
        dataset.add(new ExpandingLineChart.PointD(40, 20));
        dataset.add(new ExpandingLineChart.PointD(50, 25));

        labels.add("B");
        dataset = new ArrayList<>();
        datasets.add(dataset);
        dataset.add(new ExpandingLineChart.PointD(10, 5));
        dataset.add(new ExpandingLineChart.PointD(30, 20));
        dataset.add(new ExpandingLineChart.PointD(60, 20));

        labels.add("C");
        dataset = new ArrayList<>();
        datasets.add(dataset);
        dataset.add(new ExpandingLineChart.PointD(15, 5));
        dataset.add(new ExpandingLineChart.PointD(20, 20));
        dataset.add(new ExpandingLineChart.PointD(30, 15));
        dataset.add(new ExpandingLineChart.PointD(55, 30));

        expandingLineChart.setData(labels, datasets);
    }
}
