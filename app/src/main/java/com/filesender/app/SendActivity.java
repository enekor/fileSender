package com.filesender.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.filesender.app.net.DeviceInfo;
import com.filesender.app.net.DiscoveryClient;
import com.filesender.app.net.TransferClient;
import com.filesender.app.net.TransferProtocol;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SendActivity extends Activity {

    public static final String EXTRA_URIS = "com.filesender.app.extra.URIS";

    private static final int ACTION_NONE = 0;
    private static final int ACTION_CANCEL = 1;
    private static final int ACTION_DONE = 2;
    private static final int ACTION_RESCAN = 3;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<SelectedFile> files = new ArrayList<>();
    private final Set<String> deviceKeys = new LinkedHashSet<>();

    private DiscoveryClient discovery;
    private TransferClient transfer;
    private DeviceInfo targetDevice;
    private boolean transferring;
    private boolean destroyed;
    private int actionMode = ACTION_NONE;

    private TextView fileCount;
    private LinearLayout selectedFiles;
    private LinearLayout scanCard;
    private ProgressBar scanProgress;
    private TextView scanTitle;
    private TextView scanHint;
    private TextView devicesTitle;
    private LinearLayout devicesContainer;
    private Button btnScan;
    private LinearLayout transferCard;
    private ProgressBar transferSpinner;
    private TextView transferTitle;
    private ProgressBar progress;
    private TextView progressText;
    private TextView transferDetail;
    private Button btnTransferAction;
    private LinearLayout errorCard;
    private TextView errorTitle;
    private TextView errorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        bindViews();

        ImageButton back = findViewById(R.id.btn_back);
        back.setOnClickListener(v -> handleBack());

        btnScan.setOnClickListener(v -> startScan());
        btnTransferAction.setOnClickListener(v -> handleTransferAction());

        loadFiles(getIntent());
        if (files.isEmpty()) {
            Toast.makeText(this, R.string.send_share_empty, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (files.size() > TransferProtocol.MAX_FILES) {
            Toast.makeText(this, R.string.send_too_many_files, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        showSelectedFiles();
        mainHandler.postDelayed(this::startScan, 250L);
    }

    private void bindViews() {
        fileCount = findViewById(R.id.file_count);
        selectedFiles = findViewById(R.id.selected_files);
        scanCard = findViewById(R.id.scan_card);
        scanProgress = findViewById(R.id.scan_progress);
        scanTitle = findViewById(R.id.scan_title);
        scanHint = findViewById(R.id.scan_hint);
        devicesTitle = findViewById(R.id.devices_title);
        devicesContainer = findViewById(R.id.devices_container);
        btnScan = findViewById(R.id.btn_scan);
        transferCard = findViewById(R.id.transfer_card);
        transferSpinner = findViewById(R.id.transfer_spinner);
        transferTitle = findViewById(R.id.transfer_title);
        progress = findViewById(R.id.progress);
        progressText = findViewById(R.id.progress_text);
        transferDetail = findViewById(R.id.transfer_detail);
        btnTransferAction = findViewById(R.id.btn_transfer_action);
        errorCard = findViewById(R.id.error_card);
        errorTitle = findViewById(R.id.error_title);
        errorText = findViewById(R.id.error_text);
    }

    private void loadFiles(Intent intent) {
        files.clear();
        Set<String> seen = new LinkedHashSet<>();
        for (Uri uri : collectUris(intent)) {
            if (uri == null || !seen.add(uri.toString())) {
                continue;
            }
            SelectedFile selected = SelectedFile.from(this, uri);
            if (selected != null) {
                files.add(selected);
            }
        }
    }

    private ArrayList<Uri> collectUris(Intent intent) {
        ArrayList<Uri> result = new ArrayList<>();
        if (intent == null) {
            return result;
        }

        try {
            ArrayList<Uri> extras = intent.getParcelableArrayListExtra(EXTRA_URIS);
            if (extras != null) {
                result.addAll(extras);
            }
        } catch (RuntimeException ignored) {
            // Ignore a malformed extra; ClipData or EXTRA_STREAM may still work.
        }

        ClipData clipData = intent.getClipData();
        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                Uri uri = clipData.getItemAt(i).getUri();
                if (isFileUri(uri)) {
                    result.add(uri);
                }
            }
        }

        ArrayList<Uri> streamList = null;
        try {
            streamList = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        } catch (RuntimeException ignored) {
            // The extra is not an ArrayList; try it as a single Uri below.
        }
        if (streamList != null) {
            for (Uri uri : streamList) {
                if (isFileUri(uri)) {
                    result.add(uri);
                }
            }
        } else {
            try {
                Uri stream = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (isFileUri(stream)) {
                    result.add(stream);
                }
            } catch (RuntimeException ignored) {
                // A non-Uri EXTRA_STREAM is ignored.
            }
        }

        Uri data = intent.getData();
        if (isFileUri(data)) {
            result.add(data);
        }
        return result;
    }

    private boolean isFileUri(Uri uri) {
        if (uri == null) {
            return false;
        }
        String scheme = uri.getScheme();
        return "content".equals(scheme) || "file".equals(scheme);
    }

    private void showSelectedFiles() {
        if (files.size() == 1) {
            fileCount.setText(R.string.send_files_selected_one);
        } else {
            fileCount.setText(getString(R.string.send_files_selected_many, files.size()));
        }
        selectedFiles.removeAllViews();
        int shown = Math.min(files.size(), 6);
        for (int i = 0; i < shown; i++) {
            addFileChip(files.get(i).getDisplayName());
        }
        if (files.size() > shown) {
            addFileChip(getString(R.string.more_files, files.size() - shown));
        }
    }

    private void addFileChip(String name) {
        TextView chip = new TextView(this);
        chip.setText(name);
        chip.setTextSize(13f);
        chip.setTextColor(getColor(R.color.ink_soft));
        chip.setSingleLine(true);
        chip.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        chip.setBackgroundResource(R.drawable.bg_file_chip);
        chip.setPadding(dp(12), dp(9), dp(12), dp(9));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(6);
        selectedFiles.addView(chip, params);
    }

    private void startScan() {
        if (transferring || destroyed) {
            return;
        }
        stopDiscovery();
        errorCard.setVisibility(View.GONE);
        transferCard.setVisibility(View.GONE);
        devicesContainer.removeAllViews();
        devicesContainer.setVisibility(View.VISIBLE);
        devicesTitle.setVisibility(View.GONE);
        btnScan.setVisibility(View.GONE);
        scanCard.setVisibility(View.VISIBLE);
        scanProgress.setVisibility(View.VISIBLE);
        scanTitle.setText(R.string.send_scanning);
        scanHint.setText(R.string.send_scan_hint);
        deviceKeys.clear();
        targetDevice = null;

        discovery = new DiscoveryClient(this, new DiscoveryClient.Listener() {
            @Override
            public void onDevice(DeviceInfo device) {
                postUi(() -> addDevice(device));
            }

            @Override
            public void onFinished(String errorMessage) {
                postUi(() -> finishScan(errorMessage));
            }
        });
        discovery.start();
    }

    private void addDevice(DeviceInfo device) {
        if (destroyed || transferring) {
            return;
        }
        if (!deviceKeys.add(device.key())) {
            return;
        }
        View row = LayoutInflater.from(this).inflate(R.layout.item_device, devicesContainer, false);
        TextView name = row.findViewById(R.id.device_name);
        TextView address = row.findViewById(R.id.device_address);
        name.setText(device.name);
        address.setText(getString(R.string.device_network, device.address, device.port));
        row.setOnClickListener(v -> startTransfer(device));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(10);
        devicesContainer.addView(row, params);
        devicesTitle.setVisibility(View.VISIBLE);
    }

    private void finishScan(String errorMessage) {
        if (destroyed || transferring) {
            return;
        }
        discovery = null;
        if (errorMessage != null) {
            scanCard.setVisibility(View.GONE);
            showError(getString(R.string.send_discovery_error_title), errorMessage);
            btnScan.setVisibility(View.VISIBLE);
            return;
        }
        if (deviceKeys.isEmpty()) {
            scanProgress.setVisibility(View.GONE);
            scanTitle.setText(R.string.send_no_devices);
            scanHint.setText(R.string.send_no_devices_hint);
            btnScan.setVisibility(View.VISIBLE);
        } else {
            scanCard.setVisibility(View.GONE);
            devicesTitle.setVisibility(View.VISIBLE);
            btnScan.setVisibility(View.VISIBLE);
        }
    }

    private void stopDiscovery() {
        if (discovery != null) {
            discovery.stop();
            discovery = null;
        }
    }

    private void startTransfer(DeviceInfo device) {
        if (transferring || destroyed) {
            return;
        }
        stopDiscovery();
        targetDevice = device;
        transferCard.setVisibility(View.VISIBLE);
        scanCard.setVisibility(View.GONE);
        devicesTitle.setVisibility(View.GONE);
        devicesContainer.setVisibility(View.GONE);
        btnScan.setVisibility(View.GONE);
        errorCard.setVisibility(View.GONE);
        transferSpinner.setVisibility(View.VISIBLE);
        transferTitle.setText(R.string.send_transfer_title);
        transferTitle.setTextColor(getColor(R.color.ink));
        progress.setVisibility(View.VISIBLE);
        progress.setProgress(0);
        progressText.setText("0 %");
        transferDetail.setText(getString(R.string.send_connecting, device.name));
        setActionMode(ACTION_CANCEL);
        transferring = true;

        transfer = new TransferClient(this, device, files, new TransferClient.Listener() {
            @Override
            public void onStarted(int fileCount, long totalKnownBytes) {
                postUi(() -> {
                    if (transferring) {
                        transferDetail.setText(AppConfig.formatFileCount(SendActivity.this, fileCount));
                    }
                });
            }

            @Override
            public void onFileStarted(int index, String name, long size,
                                      long totalSent, long totalKnownBytes) {
                postUi(() -> {
                    if (!transferring) {
                        return;
                    }
                    transferDetail.setText(getString(R.string.send_file_progress,
                            index, files.size(), name));
                });
            }

            @Override
            public void onProgress(int index, String name, long fileBytes, long fileSize,
                                   long totalSent, long totalKnownBytes) {
                postUi(() -> {
                    if (!transferring) {
                        return;
                    }
                    int percent;
                    if (totalKnownBytes > 0) {
                        percent = AppConfig.percent(totalSent, totalKnownBytes);
                    } else if (fileSize > 0) {
                        percent = AppConfig.percent(fileBytes, fileSize);
                    } else {
                        percent = 0;
                    }
                    progress.setProgress(percent * 10);
                    String amount;
                    if (fileSize > 0) {
                        amount = getString(R.string.send_progress,
                                AppConfig.formatBytes(SendActivity.this, fileBytes),
                                AppConfig.formatBytes(SendActivity.this, fileSize));
                    } else {
                        amount = getString(R.string.send_progress_unknown,
                                AppConfig.formatBytes(SendActivity.this, fileBytes));
                    }
                    progressText.setText(getString(R.string.progress_percent, percent, amount));
                    transferDetail.setText(getString(R.string.send_file_progress,
                            index, files.size(), name));
                });
            }

            @Override
            public void onFileSent(int index, String name) {
                // Progress is driven by onProgress; no extra UI change is needed here.
            }

            @Override
            public void onComplete(int fileCount, long totalSent) {
                postUi(() -> {
                    transferring = false;
                    transfer = null;
                    transferSpinner.setVisibility(View.GONE);
                    transferTitle.setText(R.string.send_done);
                    transferTitle.setTextColor(getColor(R.color.receive));
                    progress.setVisibility(View.VISIBLE);
                    progress.setProgress(1000);
                    progressText.setText(getString(R.string.progress_percent, 100,
                            AppConfig.formatFileCount(SendActivity.this, fileCount)));
                    transferDetail.setText(getString(R.string.send_done_detail,
                            AppConfig.formatFileCount(SendActivity.this, fileCount),
                            targetDevice == null ? "" : targetDevice.name));
                    setActionMode(ACTION_DONE);
                });
            }

            @Override
            public void onError(String message) {
                postUi(() -> showTransferError(message));
            }
        });
        transfer.start();
    }

    private void showTransferError(String message) {
        transferring = false;
        transfer = null;
        transferSpinner.setVisibility(View.GONE);
        transferTitle.setText(R.string.send_error);
        transferTitle.setTextColor(getColor(R.color.error));
        progress.setVisibility(View.GONE);
        progressText.setText("");
        transferDetail.setText(message);
        setActionMode(ACTION_RESCAN);
    }

    private void handleTransferAction() {
        if (actionMode == ACTION_CANCEL) {
            confirmCancelTransfer(false);
        } else if (actionMode == ACTION_DONE) {
            finish();
        } else if (actionMode == ACTION_RESCAN) {
            scanCard.setVisibility(View.VISIBLE);
            scanProgress.setVisibility(View.VISIBLE);
            transferCard.setVisibility(View.GONE);
            devicesContainer.setVisibility(View.VISIBLE);
            startScan();
        }
    }

    private void confirmCancelTransfer(boolean closeActivity) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.send_cancel_confirm)
                .setMessage(R.string.send_cancel_confirm_detail)
                .setPositiveButton(R.string.send_cancel, (dialog, which) -> {
                    cancelTransfer();
                    if (closeActivity) {
                        finish();
                    } else {
                        transferCard.setVisibility(View.GONE);
                        devicesContainer.setVisibility(View.VISIBLE);
                        startScan();
                    }
                })
                .setNegativeButton(R.string.send_keep, null)
                .show();
    }

    private void cancelTransfer() {
        transferring = false;
        if (transfer != null) {
            transfer.cancel();
            transfer = null;
        }
    }

    private void setActionMode(int mode) {
        actionMode = mode;
        if (mode == ACTION_CANCEL) {
            btnTransferAction.setText(R.string.send_cancel);
        } else if (mode == ACTION_DONE) {
            btnTransferAction.setText(R.string.send_ready);
        } else {
            btnTransferAction.setText(R.string.send_retry);
        }
    }

    private void showError(String title, String message) {
        errorTitle.setText(title);
        errorText.setText(message);
        errorCard.setVisibility(View.VISIBLE);
    }

    private void handleBack() {
        if (transferring) {
            confirmCancelTransfer(true);
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        handleBack();
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (transferring) {
            return;
        }
        setIntent(intent);
        stopDiscovery();
        loadFiles(intent);
        if (files.isEmpty()) {
            Toast.makeText(this, R.string.send_share_empty, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (files.size() > TransferProtocol.MAX_FILES) {
            Toast.makeText(this, R.string.send_too_many_files, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        showSelectedFiles();
        startScan();
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        mainHandler.removeCallbacksAndMessages(null);
        stopDiscovery();
        cancelTransfer();
        super.onDestroy();
    }

    private void postUi(Runnable runnable) {
        if (destroyed) {
            return;
        }
        mainHandler.post(() -> {
            if (!destroyed) {
                runnable.run();
            }
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
