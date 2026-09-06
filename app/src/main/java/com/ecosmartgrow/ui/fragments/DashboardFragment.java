package com.ecosmartgrow.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.ecosmartgrow.R;
import com.ecosmartgrow.model.SensorLog;
import com.ecosmartgrow.model.Settings;
import com.ecosmartgrow.repository.SensorRepository;
import com.ecosmartgrow.repository.SettingsRepository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class DashboardFragment extends Fragment {
    private static final String TAG = "DashboardFragment";
    private static final String BASE_URL = "https://camera-relay-ojjs.onrender.com";
    private static final String RELAY_URL = BASE_URL;

    // Sensor display views
    private TextView tvPhValue, tvPhStatus;
    private TextView tvTdsValue, tvTdsStatus;
    private TextView tvTempValue, tvTempStatus;
    private TextView tvEcValue, tvEcStatus;
    private TextView tvHumidityValue, tvHumidityStatus;
    private TextView tvGreeting;
    private TextView tvDaysUntilHarvest, tvLettuceStatus;
    private ProgressBar growthProgress;
    private Button btnCamera;
    private LinearLayout alertsContainer;

    private SensorRepository sensorRepository;
    private SettingsRepository settingsRepository;
    private Socket mSocket;
    private boolean isConnected = false;
    private OkHttpClient httpClient = new OkHttpClient();

    // Auto-record variables
    private Timer autoRecordTimer;
    private boolean isAutoRecording = false;
    private long lastRecordTime = 0;
    private int autoRecordCounter = 0;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    // Track latest values
    private double latestPh = -1;
    private double latestTds = -1;
    private double latestTemp = -1;
    private double latestEc = -1;
    private double latestHumidity = -1;

    // Lettuce growth tracking
    private double currentLettuceSize = 0;
    private static final double HARVEST_SIZE_CM = 22.0;
    private static final int GROWTH_DAYS_TOTAL = 45;

    // Handler for periodic HTTP polling
    private Handler pollHandler = new Handler();
    private Runnable pollRunnable;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Initialize views
        tvPhValue = view.findViewById(R.id.tv_ph_value);
        tvPhStatus = view.findViewById(R.id.tv_ph_status);
        tvTdsValue = view.findViewById(R.id.tv_tds_value);
        tvTdsStatus = view.findViewById(R.id.tv_tds_status);
        tvTempValue = view.findViewById(R.id.tv_temp_value);
        tvTempStatus = view.findViewById(R.id.tv_temp_status);
        tvEcValue = view.findViewById(R.id.tv_ec_value);
        tvEcStatus = view.findViewById(R.id.tv_ec_status);
        tvHumidityValue = view.findViewById(R.id.tv_humidity_value);
        tvHumidityStatus = view.findViewById(R.id.tv_humidity_status);
        tvGreeting = view.findViewById(R.id.tv_greeting);
        tvDaysUntilHarvest = view.findViewById(R.id.tv_days_until_harvest);
        tvLettuceStatus = view.findViewById(R.id.tv_lettuce_status);
        growthProgress = view.findViewById(R.id.growth_progress);
        btnCamera = view.findViewById(R.id.btn_camera);
        alertsContainer = view.findViewById(R.id.alerts_container);

        // Initialize repositories
        sensorRepository = new SensorRepository(getContext());
        settingsRepository = new SettingsRepository(getContext());

        // Set greeting
        setGreeting();

        // Load data from database (fallback)
        loadLatestReadings();
        loadGrowthProgress();

        // Connect to WebSocket for sensor data
        connectToWebSocket();

        // Start HTTP polling for harvest data (more reliable)
        startHarvestDataPolling();

        // Setup camera button click listener
        btnCamera.setOnClickListener(v -> openCameraFragment());

        return view;
    }

    private void openCameraFragment() {
        try {
            CameraFragment cameraFragment = new CameraFragment();
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, cameraFragment);
            transaction.addToBackStack(null);
            transaction.commit();
            Log.d(TAG, "Navigated to CameraFragment");
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to CameraFragment: " + e.getMessage());
            Toast.makeText(getContext(), "Error opening camera", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Start periodic HTTP polling for harvest data
     */
    private void startHarvestDataPolling() {
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                fetchHarvestData();
                pollHandler.postDelayed(this, 3000);
            }
        };
        pollHandler.post(pollRunnable);
    }

    /**
     * Fetch harvest data via HTTP (more reliable than WebSocket)
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
                        int totalLettuce = data.optInt("total_lettuce", 0);
                        int readyLettuce = data.optInt("ready_for_harvest", 0);
                        int notReady = data.optInt("not_ready", 0);
                        double percentage = data.optDouble("readiness_percentage", 0);

                        // Try to get lettuce size
                        double lettuceSize = 0;
                        JSONArray lettuceArray = data.optJSONArray("lettuce_data");
                        if (lettuceArray != null && lettuceArray.length() > 0) {
                            JSONObject firstLettuce = lettuceArray.getJSONObject(0);
                            lettuceSize = firstLettuce.optDouble("diameter_cm", 0);
                        }
                        if (lettuceSize == 0) {
                            lettuceSize = data.optDouble("diameter_cm", 0);
                        }

                        final double finalLettuceSize = lettuceSize;

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                // Update growth progress
                                updateGrowthProgress(finalLettuceSize);
                            });
                        }

                        Log.d(TAG, "Harvest data: total=" + totalLettuce +
                                ", ready=" + readyLettuce +
                                ", size=" + lettuceSize + "cm");

                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing harvest data: " + e.getMessage());
                    }
                }
            }
        });
    }

    /**
     * Update growth progress based on lettuce size from detection
     */
    private void updateGrowthProgress(double lettuceSizeCm) {
        if (tvLettuceStatus == null || tvDaysUntilHarvest == null || growthProgress == null) {
            return;
        }

        if (lettuceSizeCm <= 0) {
            tvLettuceStatus.setText("No lettuce detected");
            tvLettuceStatus.setTextColor(getResources().getColor(R.color.text_secondary));
            growthProgress.setProgress(0);
            tvDaysUntilHarvest.setText("-- days until harvest");
            return;
        }

        currentLettuceSize = lettuceSizeCm;

        // Calculate percentage of growth toward harvest
        double maxSize = HARVEST_SIZE_CM;
        double progressPercent = Math.min(100, (lettuceSizeCm / maxSize) * 100);
        growthProgress.setProgress((int) progressPercent);

        // Determine growth stage
        String stageText;
        int stageColor;
        int daysUntilHarvest;
        String harvestText;

        if (lettuceSizeCm >= HARVEST_SIZE_CM) {
            stageText = "READY TO HARVEST!";
            stageColor = getResources().getColor(R.color.success);
            daysUntilHarvest = 0;
            harvestText = "Ready to harvest!";
        } else if (lettuceSizeCm >= 18) {
            stageText = "Almost ready!";
            stageColor = getResources().getColor(R.color.success);
            daysUntilHarvest = calculateDaysUntilHarvest(lettuceSizeCm);
            harvestText = daysUntilHarvest + " days until harvest";
        } else if (lettuceSizeCm >= 12) {
            stageText = "Growing well";
            stageColor = getResources().getColor(R.color.warning);
            daysUntilHarvest = calculateDaysUntilHarvest(lettuceSizeCm);
            harvestText = daysUntilHarvest + " days until harvest";
        } else if (lettuceSizeCm >= 6) {
            stageText = "Young plant";
            stageColor = getResources().getColor(R.color.warning);
            daysUntilHarvest = calculateDaysUntilHarvest(lettuceSizeCm);
            harvestText = daysUntilHarvest + " days until harvest";
        } else if (lettuceSizeCm >= 2) {
            stageText = "Seedling";
            stageColor = getResources().getColor(R.color.text_secondary);
            daysUntilHarvest = calculateDaysUntilHarvest(lettuceSizeCm);
            harvestText = daysUntilHarvest + " days until harvest";
        } else {
            stageText = "Just sprouted";
            stageColor = getResources().getColor(R.color.text_secondary);
            daysUntilHarvest = GROWTH_DAYS_TOTAL;
            harvestText = daysUntilHarvest + " days until harvest";
        }

        tvLettuceStatus.setText(stageText);
        tvLettuceStatus.setTextColor(stageColor);

        if (lettuceSizeCm >= HARVEST_SIZE_CM) {
            tvDaysUntilHarvest.setText("Ready to harvest!");
        } else {
            tvDaysUntilHarvest.setText(harvestText);
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

    private void connectToWebSocket() {
        try {
            Log.d(TAG, "Connecting to WebSocket...");

            IO.Options options = new IO.Options();
            options.secure = true;
            options.reconnection = true;
            options.reconnectionAttempts = 10;
            options.reconnectionDelay = 3000;
            options.timeout = 10000;
            options.transports = new String[]{"websocket"};

            mSocket = IO.socket(RELAY_URL, options);

            mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            isConnected = true;
                            Log.d(TAG, "Connected to WebSocket");
                            Toast.makeText(getContext(), "Connected to server", Toast.LENGTH_SHORT).show();
                            startAutoRecord();
                        });
                    }
                }
            });

            mSocket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Log.e(TAG, "Connection error");
                            isConnected = false;
                        });
                    }
                }
            });

            mSocket.on("sensor_data", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    try {
                        if (args.length > 0) {
                            Object dataObj = args[0];
                            String jsonData = dataObj.toString();
                            JSONObject sensorData = new JSONObject(jsonData);

                            double ph = sensorData.optDouble("ph", -1);
                            double tds = sensorData.optDouble("tds", -1);
                            double temperature = sensorData.optDouble("temperature", -1);
                            double ec = sensorData.optDouble("ec", -1);
                            double humidity = sensorData.optDouble("humidity", -1);

                            if (ph >= 0 || tds >= 0 || temperature >= 0 || ec >= 0 || humidity >= 0) {
                                if (ph >= 0) latestPh = ph;
                                if (tds >= 0) latestTds = tds;
                                if (temperature >= 0) latestTemp = temperature;
                                if (ec >= 0) latestEc = ec;
                                if (humidity >= 0) latestHumidity = humidity;

                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        if (ph >= 0) {
                                            tvPhValue.setText(String.format(Locale.getDefault(), "%.2f", ph));
                                            updatePhStatus(ph);
                                        }
                                        if (tds >= 0) {
                                            tvTdsValue.setText(String.format(Locale.getDefault(), "%.0f", tds));
                                            updateTdsStatus(tds);
                                        }
                                        if (temperature >= 0) {
                                            tvTempValue.setText(String.format(Locale.getDefault(), "%.1f", temperature));
                                            updateTempStatus(temperature);
                                        }
                                        if (ec >= 0) {
                                            tvEcValue.setText(String.format(Locale.getDefault(), "%.3f", ec));
                                            updateEcStatus(ec);
                                        }
                                        if (humidity >= 0) {
                                            tvHumidityValue.setText(String.format(Locale.getDefault(), "%.1f", humidity));
                                            updateHumidityStatus(humidity);
                                        }

                                        updateAlerts(ph, tds, temperature, ec, humidity);
                                        Log.d(TAG, "UI updated");
                                    });
                                }

                                saveToDatabase(ph, tds, temperature, ec, humidity);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing sensor data", e);
                    }
                }
            });

            mSocket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            isConnected = false;
                            Log.w(TAG, "Disconnected from WebSocket");
                            stopAutoRecord();
                        });
                    }
                }
            });

            mSocket.connect();
            Log.d(TAG, "Connection initiated");

        } catch (Exception e) {
            Log.e(TAG, "WebSocket connection error", e);
        }
    }

    private void saveToDatabase(double ph, double tds, double temperature, double ec, double humidity) {
        try {
            SensorLog log = new SensorLog();
            log.setPh(ph >= 0 ? ph : 0);
            log.setTds(tds >= 0 ? tds : 0);
            log.setTemperature(temperature >= 0 ? temperature : 0);
            log.setEc(ec >= 0 ? ec : 0);
            log.setHumidity(humidity >= 0 ? humidity : 0);
            log.setTimestamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(new Date()));

            long result = sensorRepository.saveReading(log);

            if (result != -1) {
                Log.d(TAG, "Saved to database - ID: " + result);
                checkAndRecordHistory(ph, tds, temperature, ec, humidity);
            } else {
                Log.e(TAG, "Failed to save to database");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error saving to database", e);
        }
    }

    // ============================================================
    // AUTO-RECORD METHODS
    // ============================================================

    private void checkAndRecordHistory(double ph, double tds, double temperature, double ec, double humidity) {
        try {
            Settings settings = settingsRepository.getLatestSettings();
            if (settings == null || settings.getAutoRecord() != 1) {
                Log.d(TAG, "Auto-record is disabled");
                return;
            }

            long currentTime = System.currentTimeMillis();
            long intervalMillis = settings.getRecordInterval() * 60L * 60L * 1000L;

            if (currentTime - lastRecordTime >= intervalMillis || lastRecordTime == 0) {
                SensorLog historyLog = new SensorLog();
                historyLog.setPh(ph >= 0 ? ph : 0);
                historyLog.setTds(tds >= 0 ? tds : 0);
                historyLog.setTemperature(temperature >= 0 ? temperature : 0);
                historyLog.setEc(ec >= 0 ? ec : 0);
                historyLog.setHumidity(humidity >= 0 ? humidity : 0);
                historyLog.setTimestamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(new Date()));
                historyLog.setStatus("Auto-Recorded");

                long result = sensorRepository.saveReading(historyLog);

                if (result != -1) {
                    lastRecordTime = currentTime;
                    autoRecordCounter++;
                    Log.d(TAG, "Auto-record saved (#" + autoRecordCounter + ")");

                    if (autoRecordCounter % 5 == 0 && getActivity() != null) {
                        mainHandler.post(() -> {
                            Toast.makeText(getContext(),
                                    "Auto-record #" + autoRecordCounter + " saved",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in auto-record: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startAutoRecord() {
        if (autoRecordTimer != null) {
            return;
        }

        try {
            Settings settings = settingsRepository.getLatestSettings();
            if (settings == null || settings.getAutoRecord() != 1) {
                Log.d(TAG, "Auto-record not enabled in settings");
                return;
            }

            int intervalHours = settings.getRecordInterval();
            if (intervalHours < 1) intervalHours = 6;

            autoRecordTimer = new Timer(true);
            autoRecordTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (latestPh >= 0 || latestTds >= 0 || latestTemp >= 0 || latestEc >= 0 || latestHumidity >= 0) {
                        checkAndRecordHistory(latestPh, latestTds, latestTemp, latestEc, latestHumidity);
                    } else {
                        Log.d(TAG, "No sensor data yet, skipping auto-record");
                    }
                }
            }, 0, intervalHours * 60L * 60L * 1000L);

            isAutoRecording = true;
            Log.d(TAG, "Auto-record started (interval: " + intervalHours + " hours)");

        } catch (Exception e) {
            Log.e(TAG, "Error starting auto-record: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void stopAutoRecord() {
        if (autoRecordTimer != null) {
            autoRecordTimer.cancel();
            autoRecordTimer = null;
            isAutoRecording = false;
            Log.d(TAG, "Auto-record stopped");
        }
    }

    // ============================================================
    // ALERT SYSTEM
    // ============================================================

    private void updateAlerts(double ph, double tds, double temperature, double ec, double humidity) {
        List<String> alerts = new ArrayList<>();

        if (ph >= 0) {
            if (ph < 5.5) {
                alerts.add("pH is TOO LOW (" + String.format("%.2f", ph) + ") - Add pH Up");
            } else if (ph > 6.5) {
                alerts.add("pH is TOO HIGH (" + String.format("%.2f", ph) + ") - Add pH Down");
            } else if (ph >= 5.5 && ph < 5.8) {
                alerts.add("pH is slightly low (" + String.format("%.2f", ph) + ") - Monitor");
            } else if (ph > 6.2 && ph <= 6.5) {
                alerts.add("pH is slightly high (" + String.format("%.2f", ph) + ") - Monitor");
            }
        }

        if (tds >= 0) {
            if (tds < 420) {
                alerts.add("TDS is TOO LOW (" + String.format("%.0f", tds) + " ppm) - Add nutrients");
            } else if (tds > 1120) {
                alerts.add("TDS is TOO HIGH (" + String.format("%.0f", tds) + " ppm) - Dilute");
            } else if (tds >= 420 && tds < 560) {
                alerts.add("TDS is low (" + String.format("%.0f", tds) + " ppm) - Consider adding nutrients");
            } else if (tds > 840 && tds <= 1120) {
                alerts.add("TDS is high (" + String.format("%.0f", tds) + " ppm) - Consider diluting");
            }
        }

        if (temperature >= 0) {
            if (temperature < 15) {
                alerts.add("Temperature is TOO COLD (" + String.format("%.1f", temperature) + "C) - Heat needed");
            } else if (temperature > 28) {
                alerts.add("Temperature is TOO HOT (" + String.format("%.1f", temperature) + "C) - Ventilate");
            } else if (temperature >= 15 && temperature < 18) {
                alerts.add("Temperature is cool (" + String.format("%.1f", temperature) + "C) - Consider heating");
            } else if (temperature > 24 && temperature <= 28) {
                alerts.add("Temperature is warm (" + String.format("%.1f", temperature) + "C) - Consider cooling");
            }
        }

        if (ec >= 0) {
            if (ec < 0.8) {
                alerts.add("EC is TOO LOW (" + String.format("%.3f", ec) + " dS/m) - Add nutrients");
            } else if (ec > 2.4) {
                alerts.add("EC is TOO HIGH (" + String.format("%.3f", ec) + " dS/m) - Dilute");
            } else if (ec >= 0.8 && ec < 1.2) {
                alerts.add("EC is low (" + String.format("%.3f", ec) + " dS/m) - Consider adding nutrients");
            } else if (ec > 1.8 && ec <= 2.4) {
                alerts.add("EC is high (" + String.format("%.3f", ec) + " dS/m) - Consider diluting");
            }
        }

        if (humidity >= 0) {
            if (humidity < 30) {
                alerts.add("Humidity is TOO LOW (" + String.format("%.1f", humidity) + "%) - Increase humidity");
            } else if (humidity > 70) {
                alerts.add("Humidity is TOO HIGH (" + String.format("%.1f", humidity) + "%) - Ventilate");
            } else if (humidity >= 30 && humidity < 40) {
                alerts.add("Humidity is low (" + String.format("%.1f", humidity) + "%) - Consider increasing");
            } else if (humidity > 60 && humidity <= 70) {
                alerts.add("Humidity is high (" + String.format("%.1f", humidity) + "%) - Consider ventilating");
            }
        }

        updateAlertsUI(alerts);
    }

    private void updateAlertsUI(List<String> alerts) {
        if (alertsContainer == null) return;

        alertsContainer.removeAllViews();

        if (alerts.isEmpty()) {
            TextView noAlertView = new TextView(getContext());
            noAlertView.setText("All systems optimal - No alerts");
            noAlertView.setTextSize(14);
            noAlertView.setTextColor(getResources().getColor(R.color.success));
            noAlertView.setPadding(0, 8, 0, 8);
            alertsContainer.addView(noAlertView);
        } else {
            for (String alert : alerts) {
                TextView alertView = new TextView(getContext());
                alertView.setText(alert);
                alertView.setTextSize(14);
                if (alert.contains("TOO LOW") || alert.contains("TOO HIGH") || alert.contains("TOO COLD") || alert.contains("TOO HOT")) {
                    alertView.setTextColor(getResources().getColor(R.color.danger));
                } else {
                    alertView.setTextColor(getResources().getColor(R.color.warning));
                }
                alertView.setPadding(0, 4, 0, 4);
                alertsContainer.addView(alertView);
            }
        }
    }

    // ============================================================
    // STATUS UPDATE METHODS
    // ============================================================

    private void updatePhStatus(double ph) {
        if (ph >= 5.8 && ph <= 6.2) {
            tvPhStatus.setText("Optimal");
            tvPhStatus.setTextColor(getResources().getColor(R.color.success));
        } else if (ph >= 5.5 && ph < 5.8) {
            tvPhStatus.setText("Slightly Low");
            tvPhStatus.setTextColor(getResources().getColor(R.color.warning));
        } else if (ph > 6.2 && ph <= 6.5) {
            tvPhStatus.setText("Slightly High");
            tvPhStatus.setTextColor(getResources().getColor(R.color.warning));
        } else if (ph < 5.5) {
            tvPhStatus.setText("Too Low");
            tvPhStatus.setTextColor(getResources().getColor(R.color.danger));
        } else if (ph > 6.5) {
            tvPhStatus.setText("Too High");
            tvPhStatus.setTextColor(getResources().getColor(R.color.danger));
        } else {
            tvPhStatus.setText("--");
            tvPhStatus.setTextColor(getResources().getColor(R.color.text_secondary));
        }
    }

    private void updateTdsStatus(double tds) {
        if (tds >= 560 && tds <= 840) {
            tvTdsStatus.setText("Optimal");
            tvTdsStatus.setTextColor(getResources().getColor(R.color.success));
        } else if (tds >= 420 && tds < 560) {
            tvTdsStatus.setText("Low");
            tvTdsStatus.setTextColor(getResources().getColor(R.color.warning));
        } else if (tds > 840 && tds <= 1120) {
            tvTdsStatus.setText("High");
            tvTdsStatus.setTextColor(getResources().getColor(R.color.warning));
        } else if (tds < 420) {
            tvTdsStatus.setText("Too Low");
            tvTdsStatus.setTextColor(getResources().getColor(R.color.danger));
        } else if (tds > 1120) {
            tvTdsStatus.setText("Too High");
            tvTdsStatus.setTextColor(getResources().getColor(R.color.danger));
        } else {
            tvTdsStatus.setText("--");
            tvTdsStatus.setTextColor(getResources().getColor(R.color.text_secondary));
        }
    }

    private void updateTempStatus(double temperature) {
        if (temperature >= 18 && temperature <= 24) {
            tvTempStatus.setText("Optimal");
            tvTempStatus.setTextColor(getResources().getColor(R.color.success));
        } else if (temperature >= 15 && temperature < 18) {
            tvTempStatus.setText("Slightly Cool");
            tvTempStatus.setTextColor(getResources().getColor(R.color.warning));
        } else if (temperature > 24 && temperature <= 28) {
            tvTempStatus.setText("Slightly Warm");
            tvTempStatus.setTextColor(getResources().getColor(R.color.warning));
        } else if (temperature < 15) {
            tvTempStatus.setText("Too Cold");
            tvTempStatus.setTextColor(getResources().getColor(R.color.danger));
        } else if (temperature > 28) {
            tvTempStatus.setText("Too Hot");
            tvTempStatus.setTextColor(getResources().getColor(R.color.danger));
        } else {
            tvTempStatus.setText("--");
            tvTempStatus.setTextColor(getResources().getColor(R.color.text_secondary));
        }
    }

    private void updateEcStatus(double ec) {
        if (ec >= 1.2 && ec <= 1.8) {
            tvEcStatus.setText("Optimal");
            tvEcStatus.setTextColor(getResources().getColor(R.color.success));
        } else if (ec >= 0.8 && ec < 1.2) {
            tvEcStatus.setText("Low");
            tvEcStatus.setTextColor(getResources().getColor(R.color.warning));
        } else if (ec > 1.8 && ec <= 2.4) {
            tvEcStatus.setText("High");
            tvEcStatus.setTextColor(getResources().getColor(R.color.warning));
        } else if (ec < 0.8) {
            tvEcStatus.setText("Too Low");
            tvEcStatus.setTextColor(getResources().getColor(R.color.danger));
        } else if (ec > 2.4) {
            tvEcStatus.setText("Too High");
            tvEcStatus.setTextColor(getResources().getColor(R.color.danger));
        } else {
            tvEcStatus.setText("--");
            tvEcStatus.setTextColor(getResources().getColor(R.color.text_secondary));
        }
    }

    private void updateHumidityStatus(double humidity) {
        if (humidity >= 40 && humidity <= 60) {
            tvHumidityStatus.setText("Optimal");
            tvHumidityStatus.setTextColor(getResources().getColor(R.color.success));
        } else if (humidity >= 30 && humidity < 40) {
            tvHumidityStatus.setText("Low");
            tvHumidityStatus.setTextColor(getResources().getColor(R.color.warning));
        } else if (humidity > 60 && humidity <= 70) {
            tvHumidityStatus.setText("High");
            tvHumidityStatus.setTextColor(getResources().getColor(R.color.warning));
        } else if (humidity < 30) {
            tvHumidityStatus.setText("Too Low");
            tvHumidityStatus.setTextColor(getResources().getColor(R.color.danger));
        } else if (humidity > 70) {
            tvHumidityStatus.setText("Too High");
            tvHumidityStatus.setTextColor(getResources().getColor(R.color.danger));
        } else {
            tvHumidityStatus.setText("--");
            tvHumidityStatus.setTextColor(getResources().getColor(R.color.text_secondary));
        }
    }

    private void setGreeting() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH", Locale.getDefault());
        String hourStr = sdf.format(new Date());
        int hour = Integer.parseInt(hourStr);

        String greeting;
        if (hour >= 5 && hour < 12) {
            greeting = "Good Morning, Grower!";
        } else if (hour >= 12 && hour < 17) {
            greeting = "Good Afternoon, Grower!";
        } else if (hour >= 17 && hour < 21) {
            greeting = "Good Evening, Grower!";
        } else {
            greeting = "Good Night, Grower!";
        }
        tvGreeting.setText(greeting);
    }

    private void loadLatestReadings() {
        SensorLog log = sensorRepository.getLatestReading();

        if (log != null) {
            tvPhValue.setText(String.format(Locale.getDefault(), "%.2f", log.getPh()));
            tvTdsValue.setText(String.format(Locale.getDefault(), "%.0f", log.getTds()));
            tvTempValue.setText(String.format(Locale.getDefault(), "%.1f", log.getTemperature()));
            tvEcValue.setText(String.format(Locale.getDefault(), "%.3f", log.getEc()));
            tvHumidityValue.setText(String.format(Locale.getDefault(), "%.1f", log.getHumidity()));

            updatePhStatus(log.getPh());
            updateTdsStatus(log.getTds());
            updateTempStatus(log.getTemperature());
            updateEcStatus(log.getEc());
            updateHumidityStatus(log.getHumidity());

            updateAlerts(log.getPh(), log.getTds(), log.getTemperature(), log.getEc(), log.getHumidity());

            Log.d(TAG, "Loaded from DB");
        } else {
            tvPhValue.setText("--");
            tvPhStatus.setText("No Data");
            tvTdsValue.setText("--");
            tvTdsStatus.setText("No Data");
            tvTempValue.setText("--");
            tvTempStatus.setText("No Data");
            tvEcValue.setText("--");
            tvEcStatus.setText("No Data");
            tvHumidityValue.setText("--");
            tvHumidityStatus.setText("No Data");

            List<String> waitingAlerts = new ArrayList<>();
            waitingAlerts.add("Waiting for sensor data...");
            updateAlertsUI(waitingAlerts);

            Log.d(TAG, "No data in database");
        }
    }

    private void loadGrowthProgress() {
        if (tvLettuceStatus != null) {
            tvLettuceStatus.setText("No lettuce data");
            tvLettuceStatus.setTextColor(getResources().getColor(R.color.text_secondary));
        }
        if (growthProgress != null) {
            growthProgress.setProgress(0);
        }
        if (tvDaysUntilHarvest != null) {
            tvDaysUntilHarvest.setText("-- days until harvest");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadLatestReadings();
        if (!isConnected && mSocket != null) {
            Log.d(TAG, "Reconnecting WebSocket...");
            mSocket.connect();
        } else if (isConnected) {
            stopAutoRecord();
            startAutoRecord();
        }
        // Restart polling
        if (pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
            pollHandler.post(pollRunnable);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mSocket != null) {
            mSocket.disconnect();
            mSocket.off();
        }
        stopAutoRecord();
        if (pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
        }
        Log.d(TAG, "WebSocket disconnected");
    }
}