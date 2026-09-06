package com.ecosmartgrow.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.fragment.app.Fragment;

import com.ecosmartgrow.R;
import com.ecosmartgrow.model.SensorLog;
import com.ecosmartgrow.repository.SensorRepository;
import com.ecosmartgrow.ui.adapters.SensorLogAdapter;

import java.util.List;

public class LogsFragment extends Fragment {
    private ListView lvSensorLogs;
    private SensorRepository sensorRepository;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_logs, container, false);

        lvSensorLogs = view.findViewById(R.id.lv_sensor_logs);
        sensorRepository = new SensorRepository(getContext());

        loadLogs();

        return view;
    }

    private void loadLogs() {
        List<SensorLog> logs = sensorRepository.getAllReadings();
        
        if (logs != null && !logs.isEmpty()) {
            SensorLogAdapter adapter = new SensorLogAdapter(getContext(), logs);
            lvSensorLogs.setAdapter(adapter);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadLogs();
    }
}