package com.ecosmartgrow.ui.fragments.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MJPEGStreamDecoder {
    private static final String TAG = "MJPEGDecoder";
    private static final int FETCH_INTERVAL_MS = 500;
    private static final int CONNECT_TIMEOUT = 3000;
    private static final int READ_TIMEOUT = 5000;

    private ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean isRunning = false;
    private OnFrameReceivedListener listener;
    private String photoUrl;
    private int consecutiveFailures = 0;

    // Store the latest frame
    private Bitmap latestFrame = null;

    public interface OnFrameReceivedListener {
        void onFrame(Bitmap bitmap);
        void onError(String error);
        void onConnectionStatus(boolean connected);
    }

    public MJPEGStreamDecoder(String streamUrl) {
        this.photoUrl = streamUrl.replace("/stream", "/photo");
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void setOnFrameReceivedListener(OnFrameReceivedListener listener) {
        this.listener = listener;
    }

    public Bitmap getLatestFrame() {
        return latestFrame;
    }

    public void start() {
        if (isRunning) {
            Log.d(TAG, "Already running");
            return;
        }

        isRunning = true;
        consecutiveFailures = 0;
        Log.d(TAG, "Starting photo fetcher");
        notifyConnectionStatus(true);

        executor.execute(() -> {
            while (isRunning) {
                try {
                    fetchImage();
                    consecutiveFailures = 0;
                    Thread.sleep(FETCH_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Log.d(TAG, "Fetch interrupted");
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Fetch error: " + e.getMessage());
                    consecutiveFailures++;
                    if (consecutiveFailures >= 3) {
                        notifyError("Connection lost");
                        consecutiveFailures = 0;
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {}
                }
            }
            Log.d(TAG, "Photo fetcher stopped");
        });
    }

    private void fetchImage() {
        HttpURLConnection connection = null;
        InputStream inputStream = null;

        try {
            URL url = new URL(photoUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setDoInput(true);
            connection.setRequestProperty("Cache-Control", "no-cache, no-store");
            connection.setRequestProperty("Pragma", "no-cache");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)");
            connection.setRequestProperty("If-Modified-Since", "0");

            long startTime = System.currentTimeMillis();
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                if (bitmap != null) {
                    // Store the latest frame
                    latestFrame = bitmap.copy(bitmap.getConfig(), true);

                    if (listener != null) {
                        long elapsed = System.currentTimeMillis() - startTime;
                        if (elapsed > 100) {
                            Log.d(TAG, "Frame loaded in " + elapsed + "ms");
                        }
                        mainHandler.post(() -> {
                            listener.onFrame(bitmap);
                            listener.onConnectionStatus(true);
                        });
                    }
                }
            } else if (responseCode == 404) {
                Log.d(TAG, "Waiting for frame...");
            } else {
                Log.w(TAG, "Server returned: " + responseCode);
            }

        } catch (java.net.SocketTimeoutException e) {
            Log.d(TAG, "Timeout, retrying...");
        } catch (java.net.ConnectException e) {
            Log.d(TAG, "Cannot connect, retrying...");
        } catch (Exception e) {
            Log.e(TAG, "Fetch error: " + e.getMessage());
        } finally {
            try {
                if (inputStream != null) inputStream.close();
            } catch (Exception ignored) {}
            try {
                if (connection != null) connection.disconnect();
            } catch (Exception ignored) {}
        }
    }

    private void notifyError(String error) {
        if (listener != null) {
            mainHandler.post(() -> {
                listener.onError(error);
                listener.onConnectionStatus(false);
            });
        }
    }

    private void notifyConnectionStatus(boolean connected) {
        if (listener != null) {
            mainHandler.post(() -> listener.onConnectionStatus(connected));
        }
    }

    public void stop() {
        Log.d(TAG, "Stopping photo fetcher");
        isRunning = false;
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException ignored) {}
        }
        if (latestFrame != null) {
            latestFrame.recycle();
            latestFrame = null;
        }
        notifyConnectionStatus(false);
    }

    public boolean isRunning() {
        return isRunning;
    }
}