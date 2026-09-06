package com.ecosmartgrow.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.ecosmartgrow.R;
import com.ecosmartgrow.model.GrowthLog;
import com.ecosmartgrow.repository.GrowthLogRepository;
import com.ecosmartgrow.ui.adapters.GrowthLogAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HarvestFragment extends Fragment {
    private static final String TAG = "HarvestFragment";
    private static final String BASE_URL = "https://camera-relay-ojjs.onrender.com";

    private ProgressBar harvestProgress;
    private TextView tvDaysRemaining, tvCurrentStage, tvStageProgress;
    private TextView tvPlantHeight, tvHealthScore, tvGrowthRate;
    private ListView lvGrowthLogs;

    private GrowthLogRepository growthLogRepository;
    private OkHttpClient httpClient = new OkHttpClient();
    private Handler handler = new Handler(Looper.getMainLooper());
    private GrowthLogAdapter adapter;
    private List<GrowthLog> currentLogs = new ArrayList<>();

    // Lettuce growth tracking
    private double currentLettuceSize = 0;
    private static final double HARVEST_SIZE_CM = 22.0;
    private static final int GROWTH_DAYS_TOTAL = 45;
    private int totalLettuce = 0;
    private int readyLettuce = 0;
    private int notReadyLettuce = 0;
    private double readinessPercentage = 0;
    private double currentHealthScore = 0;
    private double lastSavedSize = 0;
    private int lastSavedDay = 0;
    private boolean hasLoggedFirst = false;
    private int recordCounter = 0; // Track how many times we've recorded

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_harvest, container, false);

        // Initialize views
        harvestProgress = view.findViewById(R.id.harvest_progress);
        tvDaysRemaining = view.findViewById(R.id.tv_days_remaining);
        tvCurrentStage = view.findViewById(R.id.tv_current_stage);
        tvStageProgress = view.findViewById(R.id.tv_stage_progress);
        tvPlantHeight = view.findViewById(R.id.tv_plant_height);
        tvHealthScore = view.findViewById(R.id.tv_health_score);
        tvGrowthRate = view.findViewById(R.id.tv_growth_rate);
        lvGrowthLogs = view.findViewById(R.id.lv_growth_logs);

        growthLogRepository = new GrowthLogRepository(getContext());

        // Setup adapter
        adapter = new GrowthLogAdapter(getContext(), currentLogs);
        lvGrowthLogs.setAdapter(adapter);

        // Load initial data
        loadHarvestData();
        loadGrowthLogs();

        // Start periodic polling for harvest data
        startHarvestDataPolling();

        return view;
    }

    /**
     * Start periodic HTTP polling for harvest data
     */
    private void startHarvestDataPolling() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                fetchHarvestData();
                handler.postDelayed(this, 3000);
            }
        });
    }

    /**
     * Fetch harvest data via HTTP
     */
    private void fetchHarvestData() {
        Request request = new Request.Builder()
                .url(BASE_URL + "/api/harvest/status")
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch harvest data: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String json = response.body().string();
                        JSONObject root = new JSONObject(json);
                        JSONObject data = root.optJSONObject("data");
                        if (data == null) data = root;

                        // Parse harvest data
                        totalLettuce = data.optInt("total_lettuce", 0);
                        readyLettuce = data.optInt("ready_for_harvest", 0);
                        notReadyLettuce = data.optInt("not_ready", 0);
                        readinessPercentage = data.optDouble("readiness_percentage", 0);

                        // Get lettuce size from camera data
                        double lettuceSize = 0;
                        JSONArray lettuceArray = data.optJSONArray("lettuce_data");
                        if (lettuceArray != null && lettuceArray.length() > 0) {
                            JSONObject firstLettuce = lettuceArray.getJSONObject(0);
                            lettuceSize = firstLettuce.optDouble("diameter_cm", 0);
                        }
                        if (lettuceSize == 0) {
                            lettuceSize = data.optDouble("diameter_cm", 0);
                        }

                        currentLettuceSize = lettuceSize;

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateHarvestUI(currentLettuceSize);
                                    // Always record a log entry when new data arrives (with timestamp)
                                    if (currentLettuceSize > 0) {
                                        recordGrowthLog(currentLettuceSize);
                                    }
                                }
                            });
                        }

                        Log.d(TAG, "Harvest data: size=" + lettuceSize + "cm, ready=" + readyLettuce + "/" + totalLettuce);

                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing harvest data: " + e.getMessage());
                    }
                }
            }
        });
    }

    /**
     * Update the harvest UI with latest data
     */
    private void updateHarvestUI(double lettuceSizeCm) {
        if (lettuceSizeCm <= 0) {
            tvPlantHeight.setText("-- cm");
            tvCurrentStage.setText("No lettuce detected");
            tvDaysRemaining.setText("-- days until harvest");
            harvestProgress.setProgress(0);
            tvStageProgress.setText("Waiting for data...");
            tvHealthScore.setText("--%");
            tvGrowthRate.setText("-- cm/week");
            return;
        }

        currentLettuceSize = lettuceSizeCm;

        // Calculate percentage of growth toward harvest
        double maxSize = HARVEST_SIZE_CM;
        int progressPercent = (int) Math.min(100, (lettuceSizeCm / maxSize) * 100);
        harvestProgress.setProgress(progressPercent);

        // Determine growth stage
        String stageText;
        String stageDetail;
        int daysUntilHarvest;

        if (lettuceSizeCm >= HARVEST_SIZE_CM) {
            stageText = "READY TO HARVEST!";
            stageDetail = "Harvest now!";
            daysUntilHarvest = 0;
        } else if (lettuceSizeCm >= 18) {
            stageText = "Almost ready!";
            stageDetail = "Final growth stage";
            daysUntilHarvest = calculateDaysUntilHarvest(lettuceSizeCm);
        } else if (lettuceSizeCm >= 12) {
            stageText = "Vegetative";
            stageDetail = "Rapid growth phase";
            daysUntilHarvest = calculateDaysUntilHarvest(lettuceSizeCm);
        } else if (lettuceSizeCm >= 6) {
            stageText = "Young plant";
            stageDetail = "Establishing roots";
            daysUntilHarvest = calculateDaysUntilHarvest(lettuceSizeCm);
        } else if (lettuceSizeCm >= 2) {
            stageText = "Seedling";
            stageDetail = "Early development";
            daysUntilHarvest = calculateDaysUntilHarvest(lettuceSizeCm);
        } else {
            stageText = "Just sprouted";
            stageDetail = "Germination";
            daysUntilHarvest = GROWTH_DAYS_TOTAL;
        }

        tvCurrentStage.setText(stageText);
        tvStageProgress.setText(stageDetail);

        if (daysUntilHarvest == 0) {
            tvDaysRemaining.setText("Ready to harvest!");
        } else {
            tvDaysRemaining.setText(daysUntilHarvest + " days until harvest");
        }

        // Update plant height with actual size
        tvPlantHeight.setText(String.format(Locale.getDefault(), "%.1f cm", lettuceSizeCm));

        // Update health score based on readiness percentage
        int healthScore;
        if (readinessPercentage >= 80) {
            healthScore = 90 + (int)((readinessPercentage - 80) / 20 * 10);
        } else if (readinessPercentage >= 50) {
            healthScore = 70 + (int)((readinessPercentage - 50) / 30 * 20);
        } else if (totalLettuce > 0) {
            healthScore = 50 + (int)(readinessPercentage / 50 * 20);
        } else {
            healthScore = 0;
        }
        healthScore = Math.min(100, healthScore);
        currentHealthScore = healthScore;
        tvHealthScore.setText(healthScore + "%");

        // Calculate growth rate based on actual size changes
        double growthRate = calculateGrowthRate(lettuceSizeCm);
        tvGrowthRate.setText(String.format(Locale.getDefault(), "+%.1f cm/week", growthRate));
    }

    /**
     * Record growth log with current timestamp - ALWAYS records when new data arrives
     */
    private void recordGrowthLog(double lettuceSize) {
        if (lettuceSize <= 0) return;

        try {
            GrowthLog log = new GrowthLog();
            log.setDay(calculateDayNumber(lettuceSize));
            log.setStage(getStageName(lettuceSize));
            log.setSize(lettuceSize);
            log.setColor(getStageColor(lettuceSize));
            log.setHealthScore(currentHealthScore);
            log.setTimestamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(new Date()));

            // Save to database
            long result = growthLogRepository.saveGrowthLog(log);
            if (result != -1) {
                recordCounter++;
                Log.d(TAG, "Growth log #" + recordCounter + " saved: Day " + log.getDay() +
                        ", Size: " + log.getSize() + "cm, Time: " + log.getTimestamp());
                // Reload growth logs to update the list
                loadGrowthLogs();
            } else {
                Log.e(TAG, "Failed to save growth log");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving growth log: " + e.getMessage());
        }
    }

    /**
     * Calculate days until harvest based on current size
     */
    private int calculateDaysUntilHarvest(double currentSize) {
        if (currentSize >= HARVEST_SIZE_CM) {
            return 0;
        }

        double growthRate;
        if (currentSize < 5) {
            growthRate = 0.8;
        } else if (currentSize < 10) {
            growthRate = 1.2;
        } else if (currentSize < 15) {
            growthRate = 1.0;
        } else {
            growthRate = 0.7;
        }

        double remaining = HARVEST_SIZE_CM - currentSize;
        int days = (int) Math.ceil(remaining / growthRate);

        if (days < 1 && currentSize < HARVEST_SIZE_CM) {
            days = 1;
        }

        return days;
    }

    /**
     * Calculate growth rate based on actual size
     */
    private double calculateGrowthRate(double currentSize) {
        if (currentSize <= 0) return 0;

        // Try to calculate from actual logs if available
        List<GrowthLog> logs = growthLogRepository.getAllGrowthLogs();
        if (logs != null && logs.size() >= 2) {
            // Get the two most recent logs
            GrowthLog latest = logs.get(0);
            GrowthLog previous = logs.get(1);

            if (latest != null && previous != null) {
                double sizeDiff = latest.getSize() - previous.getSize();
                // Calculate time difference in days
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    Date latestDate = sdf.parse(latest.getTimestamp());
                    Date previousDate = sdf.parse(previous.getTimestamp());

                    if (latestDate != null && previousDate != null) {
                        long diffMillis = latestDate.getTime() - previousDate.getTime();
                        double diffDays = diffMillis / (1000.0 * 60 * 60 * 24);

                        if (diffDays > 0 && sizeDiff > 0) {
                            double growthPerDay = sizeDiff / diffDays;
                            return growthPerDay * 7;
                        }
                    }
                } catch (Exception e) {
                    // Fallback to day-based calculation
                    int dayDiff = latest.getDay() - previous.getDay();
                    if (dayDiff > 0 && sizeDiff > 0) {
                        double growthPerDay = sizeDiff / dayDiff;
                        return growthPerDay * 7;
                    }
                }
            }
        }

        // Fallback: estimate based on current size
        double growthPerDay;
        if (currentSize < 5) {
            growthPerDay = 0.8;
        } else if (currentSize < 10) {
            growthPerDay = 1.2;
        } else if (currentSize < 15) {
            growthPerDay = 1.0;
        } else {
            growthPerDay = 0.7;
        }

        return growthPerDay * 7;
    }

    /**
     * Calculate day number based on actual size
     */
    private int calculateDayNumber(double size) {
        if (size <= 0) return 1;
        double growthPerDay;
        if (size < 5) growthPerDay = 0.5;
        else if (size < 10) growthPerDay = 1.0;
        else if (size < 15) growthPerDay = 1.2;
        else growthPerDay = 0.8;

        int day = (int) Math.ceil(size / growthPerDay);
        return Math.min(day, GROWTH_DAYS_TOTAL);
    }

    /**
     * Get stage name based on actual size
     */
    private String getStageName(double size) {
        if (size >= HARVEST_SIZE_CM) return "Ready for Harvest";
        if (size >= 18) return "Almost Ready";
        if (size >= 12) return "Vegetative";
        if (size >= 6) return "Young Plant";
        if (size >= 2) return "Seedling";
        return "Germination";
    }

    /**
     * Get stage color based on actual size
     */
    private String getStageColor(double size) {
        if (size >= HARVEST_SIZE_CM) return "#4CAF50";
        if (size >= 18) return "#8BC34A";
        if (size >= 12) return "#FFC107";
        if (size >= 6) return "#FF9800";
        if (size >= 2) return "#FF5722";
        return "#9E9E9E";
    }

    /**
     * Load saved growth logs from database
     */
    private void loadGrowthLogs() {
        List<GrowthLog> logs = growthLogRepository.getAllGrowthLogs();

        currentLogs.clear();

        if (logs != null && !logs.isEmpty()) {
            // Sort by timestamp (newest first)
            logs.sort((a, b) -> {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    Date dateA = sdf.parse(a.getTimestamp());
                    Date dateB = sdf.parse(b.getTimestamp());
                    if (dateA != null && dateB != null) {
                        return dateB.compareTo(dateA);
                    }
                } catch (Exception e) {
                    // Fallback to day sorting
                    return Integer.compare(b.getDay(), a.getDay());
                }
                return 0;
            });
            currentLogs.addAll(logs);
            Log.d(TAG, "Loaded " + currentLogs.size() + " growth logs from database");
        }

        // Notify adapter
        adapter.notifyDataSetChanged();

        // Update last saved values from the most recent log
        if (!currentLogs.isEmpty()) {
            lastSavedSize = currentLogs.get(0).getSize();
            lastSavedDay = currentLogs.get(0).getDay();
            hasLoggedFirst = true;
        }
    }

    /**
     * Load initial harvest data
     */
    private void loadHarvestData() {
        tvPlantHeight.setText("Loading...");
        tvCurrentStage.setText("Loading data...");
        tvDaysRemaining.setText("-- days until harvest");
        harvestProgress.setProgress(0);
        tvStageProgress.setText("Connecting to server...");
        tvHealthScore.setText("--%");
        tvGrowthRate.setText("-- cm/week");

        fetchHarvestData();
    }

    @Override
    public void onResume() {
        super.onResume();
        handler.removeCallbacksAndMessages(null);
        startHarvestDataPolling();
        loadGrowthLogs();
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        if (httpClient != null) {
            httpClient.dispatcher().cancelAll();
        }
    }
}