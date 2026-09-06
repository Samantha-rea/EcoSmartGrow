package com.ecosmartgrow.ui.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.ecosmartgrow.R;
import com.ecosmartgrow.model.Settings;
import com.ecosmartgrow.repository.SettingsRepository;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SettingsFragment extends Fragment {
    private static final String TAG = "SettingsFragment";
    private static final String RELAY_URL = "https://camera-relay-ojjs.onrender.com";

    private EditText etPhMin, etPhMax, etEcMin, etEcMax;
    private EditText etTempMin, etTempMax, etHumidityMin, etHumidityMax;
    private EditText etStartTime, etRecordInterval;
    private Switch swAutoRecord, swNotifications;
    private Button btnSaveSettings, btnLoadDefaults;

    private SettingsRepository settingsRepository;
    private Socket mSocket;
    private boolean isConnected = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        try {
            // Initialize views
            etPhMin = view.findViewById(R.id.et_ph_min);
            etPhMax = view.findViewById(R.id.et_ph_max);
            etEcMin = view.findViewById(R.id.et_ec_min);
            etEcMax = view.findViewById(R.id.et_ec_max);
            etTempMin = view.findViewById(R.id.et_temp_min);
            etTempMax = view.findViewById(R.id.et_temp_max);
            etHumidityMin = view.findViewById(R.id.et_humidity_min);
            etHumidityMax = view.findViewById(R.id.et_humidity_max);
            etStartTime = view.findViewById(R.id.et_start_time);
            etRecordInterval = view.findViewById(R.id.et_record_interval);
            swAutoRecord = view.findViewById(R.id.sw_auto_record);
            swNotifications = view.findViewById(R.id.sw_notifications);
            btnSaveSettings = view.findViewById(R.id.btn_save_settings);
            btnLoadDefaults = view.findViewById(R.id.btn_load_defaults);

            // Initialize repository
            settingsRepository = new SettingsRepository(getContext());

            // Connect to WebSocket
            connectToWebSocket();

            // Load existing settings
            loadSettings();

            // Save button click - SAVES TO LOCAL AND SENDS TO ESP
            btnSaveSettings.setOnClickListener(v -> saveSettings());

            // Load defaults button click
            btnLoadDefaults.setOnClickListener(v -> loadDefaultSettings());

        } catch (Exception e) {
            Log.e(TAG, "Error initializing SettingsFragment: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(getContext(), "Error loading settings", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void connectToWebSocket() {
        try {
            IO.Options options = new IO.Options();
            options.secure = true;
            options.reconnection = true;
            options.reconnectionAttempts = 10;
            options.reconnectionDelay = 3000;
            options.timeout = 10000;
            options.transports = new String[]{"websocket"};

            mSocket = IO.socket(RELAY_URL, options);

            mSocket.on(Socket.EVENT_CONNECT, args -> {
                getActivity().runOnUiThread(() -> {
                    isConnected = true;
                    Log.d(TAG, "Connected to WebSocket");
                });
            });

            mSocket.on(Socket.EVENT_CONNECT_ERROR, args -> {
                getActivity().runOnUiThread(() -> {
                    Log.e(TAG, "Connection error");
                    isConnected = false;
                });
            });

            mSocket.on(Socket.EVENT_DISCONNECT, args -> {
                getActivity().runOnUiThread(() -> {
                    isConnected = false;
                    Log.w(TAG, "Disconnected from WebSocket");
                });
            });

            mSocket.connect();

        } catch (Exception e) {
            Log.e(TAG, "WebSocket connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadSettings() {
        try {
            if (settingsRepository == null) {
                settingsRepository = new SettingsRepository(getContext());
            }

            Settings settings = settingsRepository.getLatestSettings();

            if (settings != null) {
                etPhMin.setText(String.valueOf(settings.getPhMin()));
                etPhMax.setText(String.valueOf(settings.getPhMax()));
                etEcMin.setText(String.valueOf(settings.getEcMin()));
                etEcMax.setText(String.valueOf(settings.getEcMax()));
                etTempMin.setText(String.valueOf(settings.getTempMin()));
                etTempMax.setText(String.valueOf(settings.getTempMax()));
                etHumidityMin.setText(String.valueOf(settings.getHumidityMin()));
                etHumidityMax.setText(String.valueOf(settings.getHumidityMax()));
                etStartTime.setText(settings.getStartTime());
                etRecordInterval.setText(String.valueOf(settings.getRecordInterval()));
                swAutoRecord.setChecked(settings.getAutoRecord() == 1);
                swNotifications.setChecked(settings.isNotificationsEnabled());
                Log.d(TAG, "Settings loaded from database");
            } else {
                loadDefaultSettings();
                Log.d(TAG, "No settings found, loaded defaults");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading settings: " + e.getMessage());
            e.printStackTrace();
            loadDefaultSettings();
        }
    }

    private void loadDefaultSettings() {
        try {
            Settings defaultSettings = new Settings();
            etPhMin.setText(String.valueOf(defaultSettings.getPhMin()));
            etPhMax.setText(String.valueOf(defaultSettings.getPhMax()));
            etEcMin.setText(String.valueOf(defaultSettings.getEcMin()));
            etEcMax.setText(String.valueOf(defaultSettings.getEcMax()));
            etTempMin.setText(String.valueOf(defaultSettings.getTempMin()));
            etTempMax.setText(String.valueOf(defaultSettings.getTempMax()));
            etHumidityMin.setText(String.valueOf(defaultSettings.getHumidityMin()));
            etHumidityMax.setText(String.valueOf(defaultSettings.getHumidityMax()));
            etStartTime.setText(defaultSettings.getStartTime());
            etRecordInterval.setText(String.valueOf(defaultSettings.getRecordInterval()));
            swAutoRecord.setChecked(defaultSettings.getAutoRecord() == 1);
            swNotifications.setChecked(defaultSettings.isNotificationsEnabled());

            Toast.makeText(getContext(), "Default settings loaded", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Default settings loaded");
        } catch (Exception e) {
            Log.e(TAG, "Error loading default settings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveSettings() {
        try {
            // Parse all values
            double phMin = Double.parseDouble(etPhMin.getText().toString().trim());
            double phMax = Double.parseDouble(etPhMax.getText().toString().trim());
            double ecMin = Double.parseDouble(etEcMin.getText().toString().trim());
            double ecMax = Double.parseDouble(etEcMax.getText().toString().trim());
            double tempMin = Double.parseDouble(etTempMin.getText().toString().trim());
            double tempMax = Double.parseDouble(etTempMax.getText().toString().trim());
            double humidityMin = Double.parseDouble(etHumidityMin.getText().toString().trim());
            double humidityMax = Double.parseDouble(etHumidityMax.getText().toString().trim());
            String startTime = etStartTime.getText().toString().trim();
            int interval = Integer.parseInt(etRecordInterval.getText().toString().trim());
            int autoRecord = swAutoRecord.isChecked() ? 1 : 0;
            boolean notifications = swNotifications.isChecked();

            // Validate all inputs
            if (!validateInputs(phMin, phMax, ecMin, ecMax, tempMin, tempMax,
                    humidityMin, humidityMax, startTime, interval)) {
                return;
            }

            // Create settings object
            Settings settings = new Settings();
            settings.setPhMin(phMin);
            settings.setPhMax(phMax);
            settings.setEcMin(ecMin);
            settings.setEcMax(ecMax);
            settings.setTempMin(tempMin);
            settings.setTempMax(tempMax);
            settings.setHumidityMin(humidityMin);
            settings.setHumidityMax(humidityMax);
            settings.setStartTime(startTime);
            settings.setRecordInterval(interval);
            settings.setAutoRecord(autoRecord);
            settings.setNotificationsEnabled(notifications);

            // STEP 1: Save to local database
            if (settingsRepository == null) {
                settingsRepository = new SettingsRepository(getContext());
            }

            boolean saved = settingsRepository.saveSettings(settings);

            if (saved) {
                Toast.makeText(getContext(), "Settings saved locally!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Settings saved successfully");

                // STEP 2: Send to ESP32 if connected
                sendSettingsToESP(phMin, phMax, ecMin, ecMax, tempMin, tempMax);

            } else {
                Toast.makeText(getContext(), "Failed to save settings", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to save settings");
            }

        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Please enter valid numbers in all fields", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Number format error: " + e.getMessage());
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error saving settings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendSettingsToESP(double phMin, double phMax, double ecMin, double ecMax,
                                   double tempMin, double tempMax) {
        try {
            if (!isConnected) {
                Log.w(TAG, "Not connected to WebSocket, cannot send to ESP");
                Toast.makeText(getContext(), "Not connected to server. ESP not updated.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Send commands to ESP32 via Bluetooth Bridge
            // pH Setpoints
            mSocket.emit("command", "SET_PH_MIN:" + phMin);
            Thread.sleep(100);
            mSocket.emit("command", "SET_PH_MAX:" + phMax);
            Thread.sleep(100);

            // EC Setpoints
            mSocket.emit("command", "SET_EC_MIN:" + ecMin);
            Thread.sleep(100);
            mSocket.emit("command", "SET_EC_MAX:" + ecMax);
            Thread.sleep(100);

            // Temperature Setpoints
            mSocket.emit("command", "SET_TEMP_MIN:" + tempMin);
            Thread.sleep(100);
            mSocket.emit("command", "SET_TEMP_MAX:" + tempMax);
            Thread.sleep(100);

            Toast.makeText(getContext(), "Settings sent to ESP32!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Settings sent to ESP32 via WebSocket");

        } catch (Exception e) {
            Log.e(TAG, "Error sending to ESP: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(getContext(), "Settings saved locally but failed to send to ESP", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateInputs(double phMin, double phMax, double ecMin, double ecMax,
                                   double tempMin, double tempMax, double humidityMin,
                                   double humidityMax, String startTime, int interval) {

        if (phMin < 0 || phMax > 14 || phMin >= phMax) {
            Toast.makeText(getContext(), "pH must be between 0-14 and Min < Max", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (ecMin < 0 || ecMax > 5 || ecMin >= ecMax) {
            Toast.makeText(getContext(), "EC must be between 0-5 and Min < Max", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (tempMin < 0 || tempMax > 50 || tempMin >= tempMax) {
            Toast.makeText(getContext(), "Temperature must be between 0-50°C and Min < Max", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (humidityMin < 0 || humidityMax > 100 || humidityMin >= humidityMax) {
            Toast.makeText(getContext(), "Humidity must be between 0-100% and Min < Max", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!startTime.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
            Toast.makeText(getContext(), "Start time must be in HH:MM format (e.g., 06:00)", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (interval < 1 || interval > 60) {
            Toast.makeText(getContext(), "Record interval must be between 1-60 minutes", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            loadSettings();
            if (!isConnected && mSocket != null) {
                mSocket.connect();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error onResume: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mSocket != null) {
            mSocket.disconnect();
            mSocket.off();
        }
    }
}