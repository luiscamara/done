package br.com.luiscamara.roadqualitymonitor.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import br.com.luiscamara.roadqualitymonitor.R;
import br.com.luiscamara.roadqualitymonitor.util.Constants;


public class SensorsFragment extends Fragment {
    private LineChart chart;
    private Thread thread;
    private boolean plotVAData = false;
    private BroadcastReceiver broadcastReceiver;

    public SensorsFragment() {
        // Required empty public constructor
    }

    public static SensorsFragment newInstance() {
        SensorsFragment fragment = new SensorsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.sensors_fragment, container, false);

        chart = view.findViewById(R.id.sensor_graph);
        chart.getDescription().setEnabled(true);
        chart.getDescription().setText("Valores dos sensores");

        chart.setTouchEnabled(false);
        chart.setDragEnabled(false);
        chart.setScaleEnabled(false);
        chart.setDrawGridBackground(true);
        chart.setPinchZoom(false);
        chart.setBackgroundColor(Color.WHITE);

        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);
        chart.setData(data);

        Legend l = chart.getLegend();
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.WHITE);

        XAxis xl = chart.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(true);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMaximum(10f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        chart.setDrawBorders(false);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(Constants.BROADCAST_VERTICAL_ACCELERATION_DATA)) {
                    if(plotVAData) {
                        float va = intent.getFloatExtra("va", 0);
                        addEntry("Vertical Acceleration", va, 0);
                        plotVAData = false;
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.BROADCAST_VERTICAL_ACCELERATION_DATA);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(broadcastReceiver, filter);

        startPlot();

        return view;
    }

    private void startPlot() {
        if(thread != null) {
            thread.interrupt();
        }

        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    plotVAData = true;
                    try {
                        Thread.sleep(10);
                    } catch(InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        thread.start();
    }

    private void addEntry(String label, float value, int index) {
        LineData data = chart.getData();

        if(data != null) {
            ILineDataSet set = data.getDataSetByIndex(index);

            if(set == null) {
                set = createSet(label, Color.rgb(211, 94, 96));
                data.addDataSet(set);
            }


            data.addEntry(new Entry(data.getEntryCount(), value), index);
            data.notifyDataChanged();

            chart.notifyDataSetChanged();
            chart.setVisibleXRangeMaximum(150);
            chart.moveViewToX(data.getEntryCount());
        }
    }

    private LineDataSet createSet(String label, int color) {
        LineDataSet set = new LineDataSet(null, label);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(color);
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;
    }

    @Override
    public void onPause() {
        super.onPause();

        if(thread != null) {
            thread.interrupt();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(broadcastReceiver);
        thread.interrupt();
        super.onDestroy();
    }
}
