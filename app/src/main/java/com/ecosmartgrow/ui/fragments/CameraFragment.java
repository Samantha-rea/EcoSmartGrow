package com.ecosmartgrow.ui.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.ecosmartgrow.R;
import com.ecosmartgrow.ui.fragments.utils.MJPEGStreamDecoder;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CameraFragment extends Fragment {
    private static final String TAG = "CameraFragment";
    private static final String BASE_URL = "https://camera-relay-ojjs.onrender.com";
    private static final String STREAM_URL = BASE_URL + "/camera/stream";

    // Views
    private ImageView ivCameraFeed;
    private TextView tvConnectionStatus, tvNoSignal;
    private TextView tvLettuceStatus, tvLettuceCount;
    private ProgressBar progressBar;
    private Button btnTakePhoto, btnStartRecording, btnStopRecording;

    // MJPEG Stream Decoder
    private MJPEGStreamDecoder mjpegDecoder;

    // HTTP Client for API calls
    private OkHttpClient httpClient = new OkHttpClient();

    // Detection data
    private int totalLettuce = 0;
    private int readyLettuce = 0;
    private int notReadyLettuce = 0;

    // Recording
    private boolean isRecording = false;
    private StringBuilder recordingData = new StringBuilder();
    private Handler handler = new Handler();

    // Permissions
    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);

        // Initialize views
        ivCameraFeed = view.findViewById(R.id.iv_camera_feed);
        tvConnectionStatus = view.findViewById(R.id.tv_connection_status);
        tvNoSignal = view.findViewById(R.id.tv_no_signal);
        progressBar = view.findViewById(R.id.progress_bar);
        btnTakePhoto = view.findViewById(R.id.btn_take_photo);
        btnStartRecording = view.findViewById(R.id.btn_start_recording);
        btnStopRecording = view.findViewById(R.id.btn_stop_recording);
        tvLettuceStatus = view.findViewById(R.id.tv_lettuce_status);
        tvLettuceCount = view.findViewById(R.id.tv_lettuce_count);

        // Initial state
        btnTakePhoto.setEnabled(false);
        btnStartRecording.setEnabled(false);
        btnStopRecording.setEnabled(false);
        tvConnectionStatus.setText("Connecting...");
        tvNoSignal.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        tvLettuceStatus.setText("Detection: Waiting...");
        tvLettuceCount.setText("Loading...");

        // Check permissions
        checkStoragePermissions();

        // Start stream
        setupMJPEGStream();

        // Button listeners
        btnTakePhoto.setOnClickListener(v -> capturePhoto());
        btnStartRecording.setOnClickListener(v -> startRecording());
        btnStopRecording.setOnClickListener(v -> stopRecording());

        // Start periodic data fetch
        startDataFetch();

        return view;
    }

    private void setupMJPEGStream() {
        mjpegDecoder = new MJPEGStreamDecoder(STREAM_URL);
        mjpegDecoder.setOnFrameReceivedListener(new MJPEGStreamDecoder.OnFrameReceivedListener() {
            @Override
            public void onFrame(Bitmap bitmap) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        ivCameraFeed.setImageBitmap(bitmap);
                        tvNoSignal.setVisibility(View.GONE);
                        progressBar.setVisibility(View.GONE);
                        btnTakePhoto.setEnabled(true);
                        btnStartRecording.setEnabled(true);
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        btnTakePhoto.setEnabled(false);
                        btnStartRecording.setEnabled(false);
                    });
                }
            }

            @Override
            public void onConnectionStatus(boolean connected) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        tvConnectionStatus.setText(connected ? "Connected" : "Disconnected");
                    });
                }
            }
        });
        mjpegDecoder.start();
    }

    private void startDataFetch() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                fetchDetectionData();
                handler.postDelayed(this, 3000);
            }
        });
    }

    private void fetchDetectionData() {
        Request request = new Request.Builder()
                .url(BASE_URL + "/api/harvest/status")
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch data: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String json = response.body().string();
                        JSONObject root = new JSONObject(json);
                        JSONObject data = root.optJSONObject("data");
                        if (data == null) data = root;

                        final int total = data.optInt("total_lettuce", 0);
                        final int ready = data.optInt("ready_for_harvest", 0);
                        final int notReady = data.optInt("not_ready", 0);
                        final double percentage = data.optDouble("readiness_percentage", 0);

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                totalLettuce = total;
                                readyLettuce = ready;
                                notReadyLettuce = notReady;

                                String status;
                                int color;
                                if (total == 0) {
                                    status = "No lettuce detected";
                                    color = getResources().getColor(R.color.text_secondary);
                                } else if (percentage >= 80) {
                                    status = "READY TO HARVEST!";
                                    color = getResources().getColor(R.color.success);
                                } else if (percentage >= 50) {
                                    status = "Partially ready";
                                    color = getResources().getColor(R.color.warning);
                                } else {
                                    status = "Still growing";
                                    color = getResources().getColor(R.color.text_secondary);
                                }

                                tvLettuceStatus.setText("Detection: " + status);
                                tvLettuceStatus.setTextColor(color);
                                tvLettuceCount.setText("Total " + total + " | Ready " + ready + " | Not Ready " + notReady);

                                if (isRecording) {
                                    String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                                    recordingData.append(ts)
                                            .append(" | Lettuce: ").append(total)
                                            .append(" | Ready: ").append(ready)
                                            .append(" | Growing: ").append(notReady)
                                            .append(" | Status: ").append(status)
                                            .append("\n");
                                }
                            });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing: " + e.getMessage());
                    }
                }
            }
        });
    }

    /**
     * Get the storage directory for images (SAME for ALL Android versions)
     * Saves to: Pictures/EcoSmartGrow/
     */
    private File getImageStorageDir() {
        File storageDir;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        } else {
            storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        }

        if (storageDir != null) {
            storageDir = new File(storageDir, "EcoSmartGrow");
        }
        return storageDir;
    }

    /**
     * Get the storage directory for documents (SAME for ALL Android versions)
     * Saves to: Documents/EcoSmartGrow/
     */
    private File getDocumentStorageDir() {
        File storageDir;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        } else {
            storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        }

        if (storageDir != null) {
            storageDir = new File(storageDir, "EcoSmartGrow");
        }
        return storageDir;
    }

    /**
     * CAPTURE - Save the current image to Pictures/EcoSmartGrow/
     */
    private void capturePhoto() {
        Bitmap frame = mjpegDecoder.getLatestFrame();

        if (frame == null) {
            Toast.makeText(getContext(), "No frame to capture. Waiting for stream...", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String filename = "lettuce_capture_" + timestamp + ".jpg";

            File storageDir = getImageStorageDir();
            if (storageDir == null) {
                storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            }
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }

            File imageFile = new File(storageDir, filename);
            FileOutputStream fos = new FileOutputStream(imageFile);
            frame.compress(Bitmap.CompressFormat.JPEG, 95, fos);
            fos.close();

            String path = imageFile.getAbsolutePath();
            Toast.makeText(getContext(), "Image saved to:\n" + path, Toast.LENGTH_LONG).show();
            Log.d(TAG, "Image saved to: " + path);

        } catch (IOException e) {
            Log.e(TAG, "Error saving: " + e.getMessage());
            Toast.makeText(getContext(), "Error saving image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Start recording - saves to Documents/EcoSmartGrow/ when stopped
     */
    private void startRecording() {
        isRecording = true;
        recordingData = new StringBuilder();
        recordingData.append("=== Lettuce Detection Recording ===\n");
        recordingData.append("Started: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n\n");

        btnStartRecording.setEnabled(false);
        btnStopRecording.setEnabled(true);
        Toast.makeText(getContext(), "Recording started", Toast.LENGTH_SHORT).show();

        fetchDetectionData();
    }

    /**
     * Stop recording and save as PDF to Documents/EcoSmartGrow/
     */
    private void stopRecording() {
        isRecording = false;
        btnStartRecording.setEnabled(true);
        btnStopRecording.setEnabled(false);

        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String filename = "lettuce_recording_" + timestamp + ".pdf";

            File storageDir = getDocumentStorageDir();
            if (storageDir == null) {
                storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            }
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }

            File pdfFile = new File(storageDir, filename);

            // Create PDF document
            PdfDocument pdfDocument = new PdfDocument();
            Paint paint = new Paint();
            Paint titlePaint = new Paint();
            Paint headerPaint = new Paint();

            // Setup paints
            titlePaint.setTextSize(24);
            titlePaint.setColor(Color.BLACK);
            titlePaint.setFakeBoldText(true);

            headerPaint.setTextSize(16);
            headerPaint.setColor(Color.BLACK);
            headerPaint.setFakeBoldText(true);

            paint.setTextSize(14);
            paint.setColor(Color.parseColor("#444444")); // Dark gray

            // Page dimensions
            int pageWidth = 595; // A4 width in points
            int pageHeight = 842; // A4 height in points
            int margin = 50;
            int yPosition = margin + 20;

            // Create first page
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            // Draw header
            canvas.drawText("Lettuce Detection Report", margin + 20, yPosition, titlePaint);
            yPosition += 30;

            // Draw date
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String currentDate = dateFormat.format(new Date());
            canvas.drawText("Generated: " + currentDate, margin + 20, yPosition, paint);
            yPosition += 40;

            // Draw summary
            canvas.drawText("Summary", margin + 20, yPosition, headerPaint);
            yPosition += 25;
            canvas.drawText("  Total Lettuce Detected: " + totalLettuce, margin + 20, yPosition, paint);
            yPosition += 20;
            canvas.drawText("  Ready for Harvest: " + readyLettuce, margin + 20, yPosition, paint);
            yPosition += 20;
            canvas.drawText("  Still Growing: " + notReadyLettuce, margin + 20, yPosition, paint);
            yPosition += 30;

            // Draw detection log header
            canvas.drawText("Detection Log", margin + 20, yPosition, headerPaint);
            yPosition += 25;

            // Draw column headers
            canvas.drawText("Time", margin + 20, yPosition, headerPaint);
            canvas.drawText("Lettuce", margin + 150, yPosition, headerPaint);
            canvas.drawText("Ready", margin + 250, yPosition, headerPaint);
            canvas.drawText("Growing", margin + 350, yPosition, headerPaint);
            canvas.drawText("Status", margin + 450, yPosition, headerPaint);
            yPosition += 20;

            // Draw horizontal line
            canvas.drawLine(margin, yPosition, pageWidth - margin, yPosition, paint);
            yPosition += 15;

            // Split recording data into lines
            String[] lines = recordingData.toString().split("\n");

            for (String line : lines) {
                // Check if we need a new page
                if (yPosition > pageHeight - 50) {
                    pdfDocument.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdfDocument.getPages().size() + 1).create();
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    yPosition = margin + 20;
                }

                // Skip empty lines or headers
                if (line.trim().isEmpty() || line.startsWith("===") || line.startsWith("Start") || line.startsWith("End")) {
                    if (line.trim().isEmpty()) {
                        yPosition += 5;
                    }
                    continue;
                }

                // Parse the line format: "2024-08-24 14:30:00 | Lettuce: 5 | Ready: 3 | Growing: 2 | Status: READY"
                String[] parts = line.split("\\|");
                if (parts.length >= 5) {
                    String time = parts[0].trim();
                    String lettuce = parts[1].trim().replace("Lettuce:", "").trim();
                    String ready = parts[2].trim().replace("Ready:", "").trim();
                    String growing = parts[3].trim().replace("Growing:", "").trim();
                    String status = parts[4].trim().replace("Status:", "").trim();

                    // Draw data
                    canvas.drawText(time, margin + 20, yPosition, paint);
                    canvas.drawText(lettuce, margin + 150, yPosition, paint);
                    canvas.drawText(ready, margin + 250, yPosition, paint);
                    canvas.drawText(growing, margin + 350, yPosition, paint);

                    // Color code status
                    Paint statusPaint = new Paint();
                    statusPaint.setTextSize(14);
                    if (status.contains("READY")) {
                        statusPaint.setColor(Color.parseColor("#4CAF50")); // Green
                    } else if (status.contains("Partially")) {
                        statusPaint.setColor(Color.parseColor("#FF9800")); // Orange
                    } else if (status.contains("No lettuce")) {
                        statusPaint.setColor(Color.parseColor("#999999")); // Gray
                    } else {
                        statusPaint.setColor(Color.parseColor("#F44336")); // Red
                    }
                    canvas.drawText(status, margin + 450, yPosition, statusPaint);

                    yPosition += 20;
                } else if (line.contains("Total lettuce detected:") ||
                        line.contains("Ready for harvest:") ||
                        line.contains("Still growing:") ||
                        line.contains("Status:")) {
                    // Skip summary lines as they're already at the top
                    continue;
                } else {
                    // Draw any other lines
                    canvas.drawText(line, margin + 20, yPosition, paint);
                    yPosition += 20;
                }
            }

            pdfDocument.finishPage(page);

            // Save the PDF
            FileOutputStream fos = new FileOutputStream(pdfFile);
            pdfDocument.writeTo(fos);
            pdfDocument.close();
            fos.close();

            String path = pdfFile.getAbsolutePath();
            Toast.makeText(getContext(), "PDF saved to:\n" + path, Toast.LENGTH_LONG).show();
            Log.d(TAG, "PDF saved to: " + path);

        } catch (IOException e) {
            Log.e(TAG, "Error saving PDF: " + e.getMessage());
            Toast.makeText(getContext(), "Error saving PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void checkStoragePermissions() {
        if (getActivity() == null) return;

        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
            };
        } else {
            permissions = new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }

        boolean allGranted = true;
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(requireActivity(), permissions, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                Toast.makeText(getContext(), "Storage permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Storage permissions denied. Files won't be saved.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mjpegDecoder != null) {
            mjpegDecoder.stop();
            mjpegDecoder = null;
        }
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mjpegDecoder == null || !mjpegDecoder.isRunning()) {
            setupMJPEGStream();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mjpegDecoder != null) {
            mjpegDecoder.stop();
            mjpegDecoder = null;
        }
        isRecording = false;
    }
}