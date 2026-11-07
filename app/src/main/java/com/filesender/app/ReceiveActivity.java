package com.filesender.app;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.filesender.app.net.DiscoveryResponder;
import com.filesender.app.net.NetworkUtils;
import com.filesender.app.net.TransferServer;
import com.filesender.app.storage.DownloadStore;

import java.util.ArrayList;
import java.util.List;

public final class ReceiveActivity extends Activity {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<String> savedFiles = new ArrayList<>();

    private TransferServer server;
    private DiscoveryResponder responder;
    private DownloadStore store;
    private WifiManager.WifiLock wifiLock;
    private ObjectAnimator pulse;

    private TextView statusTitle;
    private TextView statusDetail;
    private TextView devicePill;
    private TextView ipLine;
    private LinearLayout progressCard;
    private LinearLayout errorCard;
    private LinearLayout receivedCard;
    private LinearLayout receivedFiles;
    private TextView transferTitle;
    private TextView progressText;
    private TextView errorTitle;
    private TextView errorText;
    private TextView receivedTitle;
    private TextView receivedDetail;
    private ProgressBar progress;
    private View radar;

    private boolean destroyed;
    private boolean transferring;
    private boolean serverReady;
    private boolean wifiWarning;
    private boolean fatal;
    private int totalFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        bindViews();
        devicePill.setText(getString(R.string.receive_visible_as, AppConfig.deviceName()));
        ipLine.setText(getString(R.string.receive_ip, "—", AppConfig.TCP_PORT));

        ImageButton back = findViewById(R.id.btn_back);
        back.setOnClickListener(v -> finish());

        if (Build.VERSION.SDK_INT <= 28
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            showFatal(getString(R.string.receive_permission_needed));
            return;
        }

        store = new DownloadStore(this);
        try {
            store.ensureReady();
        } catch (Exception e) {
            showFatal(getString(R.string.receive_folder_error,
                    e.getMessage() == null ? "" : e.getMessage()));
            return;
        }

        acquireWifiLock();
        refreshIpAddress();

        server = new TransferServer(store, transferListener);
        server.start();
    }

    private void bindViews() {
        statusTitle = findViewById(R.id.status_title);
        statusDetail = findViewById(R.id.status_detail);
        devicePill = findViewById(R.id.device_pill);
        ipLine = findViewById(R.id.ip_line);
        progressCard = findViewById(R.id.progress_card);
        errorCard = findViewById(R.id.error_card);
        receivedCard = findViewById(R.id.received_card);
        receivedFiles = findViewById(R.id.received_files);
        transferTitle = findViewById(R.id.transfer_title);
        progressText = findViewById(R.id.progress_text);
        errorTitle = findViewById(R.id.error_title);
        errorText = findViewById(R.id.error_text);
        receivedTitle = findViewById(R.id.received_title);
        receivedDetail = findViewById(R.id.received_detail);
        progress = findViewById(R.id.progress);
        radar = findViewById(R.id.radar);
    }

    private void refreshIpAddress() {
        String ip = NetworkUtils.localIpv4Address();
        if (ip == null) {
            ipLine.setText(getString(R.string.receive_ip, "—", AppConfig.TCP_PORT));
            wifiWarning = true;
            postUi(() -> showError(getString(R.string.receive_no_wifi_title),
                    getString(R.string.receive_no_wifi)));
        } else {
            ipLine.setText(getString(R.string.receive_ip, ip, AppConfig.TCP_PORT));
            if (wifiWarning) {
                wifiWarning = false;
                hideError();
            }
        }
    }

    private void showWaiting() {
        transferring = false;
        statusTitle.setText(R.string.receive_waiting);
        statusDetail.setText(R.string.receive_waiting_detail);
        progressCard.setVisibility(View.GONE);
        startPulse();
    }

    private void showError(String title, String message) {
        errorTitle.setText(title);
        errorText.setText(message);
        errorCard.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        errorCard.setVisibility(View.GONE);
    }

    private void showFatal(String message) {
        fatal = true;
        transferring = false;
        statusTitle.setText(R.string.receive_start_error_title);
        statusDetail.setText(message);
        progressCard.setVisibility(View.GONE);
        showError(getString(R.string.receive_start_error_title), message);
        stopPulse();
    }

    private final TransferServer.Listener transferListener = new TransferServer.Listener() {
        @Override
        public void onReady() {
            postUi(() -> {
                serverReady = true;
                startDiscoveryResponder();
                refreshIpAddress();
                showWaiting();
            });
        }

        @Override
        public void onSessionStarted(int fileCount, long totalKnownBytes) {
            postUi(() -> {
                transferring = true;
                totalFiles = fileCount;
                savedFiles.clear();
                receivedFiles.removeAllViews();
                receivedCard.setVisibility(View.GONE);
                hideError();
                stopPulse();
                progressCard.setVisibility(View.VISIBLE);
                progress.setProgress(0);
                transferTitle.setText(R.string.receive_receiving);
                progressText.setText("");
                statusTitle.setText(R.string.receive_receiving);
                statusDetail.setText(AppConfig.formatFileCount(ReceiveActivity.this, fileCount));
            });
        }

        @Override
        public void onFileStarted(int index, String name, long size) {
            postUi(() -> {
                transferTitle.setText(getString(R.string.receive_transfer_title, index, totalFiles));
                statusDetail.setText(name);
                progress.setProgress(0);
            });
        }

        @Override
        public void onProgress(int index, String name, long fileBytes, long fileSize,
                               long totalReceived, long totalKnownBytes) {
            postUi(() -> {
                int percent;
                if (totalKnownBytes > 0) {
                    percent = AppConfig.percent(totalReceived, totalKnownBytes);
                } else if (fileSize > 0) {
                    percent = AppConfig.percent(fileBytes, fileSize);
                } else {
                    percent = 0;
                }
                progress.setProgress(percent * 10);
                String amount;
                if (fileSize > 0) {
                    amount = getString(R.string.receive_progress,
                            AppConfig.formatBytes(ReceiveActivity.this, fileBytes),
                            AppConfig.formatBytes(ReceiveActivity.this, fileSize));
                } else {
                    amount = getString(R.string.receive_progress_unknown,
                            AppConfig.formatBytes(ReceiveActivity.this, fileBytes));
                }
                progressText.setText(getString(R.string.progress_percent, percent, amount));
            });
        }

        @Override
        public void onFileSaved(int index, String name, String location) {
            postUi(() -> savedFiles.add(name));
        }

        @Override
        public void onComplete(int fileCount, long totalReceived) {
            postUi(() -> {
                transferring = false;
                progress.setProgress(1000);
                progressCard.setVisibility(View.GONE);
                hideError();
                receivedTitle.setText(R.string.receive_done);
                receivedDetail.setText(getString(R.string.receive_done_detail,
                        AppConfig.formatFileCount(ReceiveActivity.this, fileCount)));
                rebuildReceivedFiles();
                receivedCard.setVisibility(View.VISIBLE);
                statusTitle.setText(R.string.receive_done);
                statusDetail.setText(R.string.receive_done_retry);
                startPulse();
            });
        }

        @Override
        public void onError(String message) {
            postUi(() -> {
                if (!serverReady) {
                    showFatal(message);
                    return;
                }
                transferring = false;
                progressCard.setVisibility(View.GONE);
                statusTitle.setText(R.string.receive_error_title);
                statusDetail.setText(R.string.receive_retry_hint);
                showError(getString(R.string.receive_error_title), message);
                if (!savedFiles.isEmpty()) {
                    receivedTitle.setText(R.string.receive_done);
                    receivedDetail.setText(getString(R.string.receive_done_detail,
                            AppConfig.formatFileCount(ReceiveActivity.this, savedFiles.size())));
                    rebuildReceivedFiles();
                    receivedCard.setVisibility(View.VISIBLE);
                }
                startPulse();
            });
        }
    };

    private void startDiscoveryResponder() {
        if (responder != null || destroyed) {
            return;
        }
        responder = new DiscoveryResponder(this, AppConfig.TCP_PORT, new DiscoveryResponder.Listener() {
            @Override
            public void onDiscoveryError(final String message) {
                postUi(() -> showError(getString(R.string.receive_discovery_error_title), message));
            }
        });
        responder.start();
    }

    private void rebuildReceivedFiles() {
        receivedFiles.removeAllViews();
        int shown = Math.min(savedFiles.size(), 30);
        for (int i = 0; i < shown; i++) {
            addReceivedFileRow("✓  " + savedFiles.get(i));
        }
        if (savedFiles.size() > shown) {
            TextView more = new TextView(this);
            more.setText(getString(R.string.more_received, savedFiles.size() - shown));
            more.setTextColor(getColor(R.color.muted));
            more.setTextSize(12f);
            more.setPadding(dp(10), dp(6), dp(10), dp(6));
            receivedFiles.addView(more);
        }
    }

    private void addReceivedFileRow(String text) {
        TextView row = new TextView(this);
        row.setText(text);
        row.setTextColor(getColor(R.color.ink_soft));
        row.setTextSize(13f);
        row.setSingleLine(true);
        row.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        row.setBackgroundResource(R.drawable.bg_file_chip);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(6);
        receivedFiles.addView(row, params);
    }

    private void startPulse() {
        if (pulse != null || radar == null) {
            return;
        }
        pulse = ObjectAnimator.ofFloat(radar, "alpha", 1f, 0.55f);
        pulse.setDuration(1500L);
        pulse.setRepeatCount(ObjectAnimator.INFINITE);
        pulse.setRepeatMode(ObjectAnimator.REVERSE);
        pulse.start();
    }

    private void stopPulse() {
        if (pulse != null) {
            pulse.cancel();
            pulse = null;
        }
        if (radar != null) {
            radar.setAlpha(1f);
        }
    }

    private void acquireWifiLock() {
        try {
            WifiManager wifi = (WifiManager) getApplicationContext()
                    .getSystemService(WIFI_SERVICE);
            if (wifi != null) {
                wifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                        "FileSender:receive");
                wifiLock.setReferenceCounted(false);
                wifiLock.acquire();
            }
        } catch (Exception ignored) {
            // The receiver still works without a WiFi lock.
        }
    }

    private void releaseWifiLock() {
        if (wifiLock != null) {
            try {
                if (wifiLock.isHeld()) {
                    wifiLock.release();
                }
            } catch (Exception ignored) {
                // Nothing to do.
            }
            wifiLock = null;
        }
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

    @Override
    protected void onResume() {
        super.onResume();
        if (store != null && !fatal) {
            refreshIpAddress();
        }
    }

    @Override
    public void onBackPressed() {
        if (transferring) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.receive_cancel_confirm)
                    .setMessage(R.string.receive_cancel_detail)
                    .setPositiveButton(R.string.receive_stop, (dialog, which) -> finish())
                    .setNegativeButton(R.string.receive_keep, null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        stopPulse();
        if (responder != null) {
            responder.stop();
            responder = null;
        }
        if (server != null) {
            server.stop();
            server = null;
        }
        releaseWifiLock();
        super.onDestroy();
    }
}
