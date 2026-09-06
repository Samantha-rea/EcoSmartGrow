package com.ecosmartgrow.ui.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ecosmartgrow.R;
import com.ecosmartgrow.model.GrowthLog;

import java.util.List;
import java.util.Locale;

public class GrowthLogAdapter extends ArrayAdapter<GrowthLog> {
    private Context context;
    private List<GrowthLog> logs;

    public GrowthLogAdapter(Context context, List<GrowthLog> logs) {
        super(context, R.layout.item_growth_log, logs);
        this.context = context;
        this.logs = logs;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_growth_log, parent, false);
        }

        GrowthLog log = logs.get(position);

        TextView tvDay = view.findViewById(R.id.tv_day);
        TextView tvStage = view.findViewById(R.id.tv_stage);
        TextView tvSize = view.findViewById(R.id.tv_size);
        View tvColor = view.findViewById(R.id.tv_color);
        TextView tvHealth = view.findViewById(R.id.tv_health);
        TextView tvDate = view.findViewById(R.id.tv_date);

        // Set values
        tvDay.setText("Day " + log.getDay());
        tvStage.setText(log.getStage());
        tvSize.setText(String.format(Locale.getDefault(), "%.1f cm", log.getSize()));
        tvHealth.setText(String.format(Locale.getDefault(), "%.0f%%", log.getHealthScore()));

        // Set color indicator and text colors based on stage
        try {
            int color = Color.parseColor(log.getColor());
            tvColor.setBackgroundColor(color);

            // Determine if the color is dark or light
            boolean isDark = isColorDark(color);

            // Set text colors based on background
            if (isDark) {
                // Dark background - use white text
                tvDay.setTextColor(Color.WHITE);
                tvStage.setTextColor(Color.parseColor("#CCCCCC"));
                tvSize.setTextColor(Color.WHITE);
                tvHealth.setTextColor(Color.WHITE);
                tvDate.setTextColor(Color.parseColor("#AAAAAA"));
                // Set the whole card background to the color
                view.setBackgroundColor(color);
            } else {
                // Light background - use dark text
                tvDay.setTextColor(Color.parseColor("#333333"));
                tvStage.setTextColor(Color.parseColor("#666666"));
                tvSize.setTextColor(Color.parseColor("#333333"));
                tvHealth.setTextColor(Color.parseColor("#4CAF50"));
                tvDate.setTextColor(Color.parseColor("#999999"));
                // Set card background to white
                view.setBackgroundColor(Color.WHITE);
            }
        } catch (Exception e) {
            tvColor.setBackgroundColor(Color.parseColor("#4CAF50"));
        }

        // Show date if available
        if (log.getTimestamp() != null && !log.getTimestamp().isEmpty()) {
            tvDate.setText(log.getTimestamp());
            tvDate.setVisibility(View.VISIBLE);
        } else {
            tvDate.setVisibility(View.GONE);
        }

        return view;
    }

    /**
     * Check if a color is dark (useful for determining text color)
     */
    private boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }

    @Override
    public int getCount() {
        return logs != null ? logs.size() : 0;
    }
}