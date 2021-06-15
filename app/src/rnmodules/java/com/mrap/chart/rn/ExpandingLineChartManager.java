package com.mrap.chart.rn;

import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

import java.util.Map;

public class ExpandingLineChartManager extends SimpleViewManager<RnExpandingLineChart> {
  private static final String TAG = "ExpLineChrtMgr";
  ReactApplicationContext reactContext;

  public ExpandingLineChartManager(ReactApplicationContext reactContext) {
    Log.d(TAG, "constructor");
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "ExpandingLineChart";
  }

  @Override
  public RnExpandingLineChart createViewInstance(ThemedReactContext context) {
    Log.d(TAG, "createViewInstance");
    return new RnExpandingLineChart(context);
  }

  @ReactProp(name="data")
  public void setData(RnExpandingLineChart v, ReadableMap data) {
    v.setData(data);
  }

//  @ReactProp(name="fps")
//  public void setFps(RnExpandingLineChart v, int fps) {
//    v.setFps(fps);
//  }


//  @Override
//  public Map getExportedCustomBubblingEventTypeConstants() {
//    return MapBuilder.builder()
//            .put(
//                    "topFormatXLabel",
//                    MapBuilder.of(
//                            "phasedRegistrationNames",
//                            MapBuilder.of("bubbled", "onFormatXLabel")))
//            .put(
//                    "topFormatYLabel",
//                    MapBuilder.of(
//                            "phasedRegistrationNames",
//                            MapBuilder.of("bubbled", "onFormatYLabel")))
//            .build();
//  }

  @Override
  public Map getExportedCustomDirectEventTypeConstants() {
    return MapBuilder.of(
            "topFormatXLabel",
            MapBuilder.of(
                    "registrationName", "onFormatXLabel"),
            "topFormatYLabel",
            MapBuilder.of(
                    "registrationName", "onFormatYLabel"));
  }
}
