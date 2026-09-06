package com.ecosmartgrow.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ecosmartgrow.R;
import com.ecosmartgrow.model.SensorLog;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class SensorLogAdapter extends ArrayAdapter<SensorLog> {
    private Context context;
    private List<SensorLog> logs;

    public SensorLogAdapter(Context context, List<SensorLog> logs) {
        super(context, R.layout.item_sensor_log, logs);
        this.context = context;
        this.logs = logs;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_sensor_log, parent, false);
        }

        SensorLog log = logs.get(position);

        TextView tvTimestamp = view.findViewById(R.id.tv_timestamp);
        TextView tvMetric = view.findViewById(R.id.tv_metric);
        TextView tvValue = view.findViewById(R.id.tv_value);
        TextView tvSetpoint = view.findViewById(R.id.tv_setpoint);
        TextView tvStatus = view.findViewById(R.id.tv_status);

        // Format timestamp
        String time = "Just now";
        if (log.getTimestamp() != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                java.util.Date date = sdf.parse(log.getTimestamp());
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                time = timeFormat.format(date);
            } catch (Exception e) {
                time = log.getTimestamp();
            }
        }

        tvTimestamp.setText(time);
        tvMetric.setText("pH");
        tvValue.setText(String.format(Locale.getDefault(), "%.0f", log.getTds()));
        tvSetpoint.setText("100 - 300");

        // Set status based on TDS
        double tds = log.getTds();
        if (tds >= 100 && tds <= 300) {
            tvStatus.setText("OK");
            tvStatus.setTextColor(context.getResources().getColor(R.color.success));
        } else if (tds > 300 && tds <= 500) {
            tvStatus.setText("HIGH");
            tvStatus.setTextColor(context.getResources().getColor(R.color.warning));
        } else if (tds > 500) {
            tvStatus.setText("VERY HIGH");
            tvStatus.setTextColor(context.getResources().getColor(R.color.danger));
        } else {
            tvStatus.setText("LOW");
            tvStatus.setTextColor(context.getResources().getColor(R.color.warning));
        }

        return view;
    }

    @Override
    public int getCount() {
        return logs != null ? logs.size() : 0;
    }
}