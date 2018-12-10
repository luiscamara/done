package br.com.luiscamara.roadqualitymonitor.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;

import br.com.luiscamara.roadqualitymonitor.R;
import br.com.luiscamara.roadqualitymonitor.util.Constants;


public class OptionsFragment extends Fragment {
    private CheckBox automaticDataAcquireCheck;
    private CheckBox debugModeCheck;
    private SharedPreferences sharedPreferences;

    public OptionsFragment() {
        // Required empty public constructor
    }

    public static OptionsFragment newInstance() {
        OptionsFragment fragment = new OptionsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getContext().getSharedPreferences(getString(R.string.sharedprefs), Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.options_fragment, container, false);
        automaticDataAcquireCheck = view.findViewById(R.id.automaticDataAcquireCheck);
        boolean automaticDataAcquire = sharedPreferences.getBoolean(getString(R.string.automaticDataAcquire), true);
        automaticDataAcquireCheck.setChecked(automaticDataAcquire);

        debugModeCheck = view.findViewById(R.id.debugModeCheck);
        boolean debugMode = sharedPreferences.getBoolean(getString(R.string.debugMode), false);
        debugModeCheck.setChecked(debugMode);

        automaticDataAcquireCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(getString(R.string.automaticDataAcquire), isChecked);
            editor.apply();

            // Send a broadcast with automatic readings status
            Intent intent = new Intent(Constants.BROADCAST_AUTOMATIC_READINGS_ENABLE);
            intent.putExtra("enabled", isChecked);
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
        });

        debugModeCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(getString(R.string.debugMode), isChecked);
            editor.apply();
        });

        // Inflate the layout for this fragment
        return view;
    }
}
