package com.filesender.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

public final class MainActivity extends Activity {

    private static final int REQUEST_STORAGE = 4101;
    private static final int REQUEST_PICK_FILES = 4102;

    private boolean pendingReceive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView devicePill = findViewById(R.id.device_pill);
        devicePill.setText(getString(R.string.device_pill, AppConfig.deviceName()));

        View receive = findViewById(R.id.btn_receive);
        receive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestReceive();
            }
        });

        View send = findViewById(R.id.btn_send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickFiles();
            }
        });
    }

    private void requestReceive() {
        if (Build.VERSION.SDK_INT <= 28
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            pendingReceive = true;
            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.permission_title)
                        .setMessage(R.string.permission_storage_message)
                        .setPositiveButton(R.string.accept, (dialog, which) ->
                                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        REQUEST_STORAGE))
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            } else {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_STORAGE);
            }
            return;
        }
        openReceiver();
    }

    private void openReceiver() {
        startActivity(new Intent(this, ReceiveActivity.class));
    }

    private void pickFiles() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQUEST_PICK_FILES);
        } catch (Exception first) {
            Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
            fallback.addCategory(Intent.CATEGORY_OPENABLE);
            fallback.setType("*/*");
            fallback.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            fallback.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivityForResult(fallback, REQUEST_PICK_FILES);
            } catch (Exception second) {
                Toast.makeText(this, R.string.send_share_empty, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK_FILES || resultCode != RESULT_OK || data == null) {
            return;
        }
        ArrayList<Uri> uris = collectUris(data);
        if (uris.isEmpty()) {
            Toast.makeText(this, R.string.send_share_empty, Toast.LENGTH_LONG).show();
            return;
        }
        Intent sendIntent = new Intent(this, SendActivity.class);
        sendIntent.putParcelableArrayListExtra(SendActivity.EXTRA_URIS, uris);
        startActivity(sendIntent);
    }

    private ArrayList<Uri> collectUris(Intent data) {
        Set<String> seen = new LinkedHashSet<>();
        ArrayList<Uri> uris = new ArrayList<>();
        ClipData clipData = data.getClipData();
        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                Uri uri = clipData.getItemAt(i).getUri();
                if (uri != null && seen.add(uri.toString())) {
                    uris.add(uri);
                }
            }
        }
        Uri single = data.getData();
        if (single != null && seen.add(single.toString())) {
            uris.add(single);
        }
        return uris;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_STORAGE) {
            return;
        }
        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (granted) {
            if (pendingReceive) {
                pendingReceive = false;
                openReceiver();
            }
        } else {
            pendingReceive = false;
            new AlertDialog.Builder(this)
                    .setTitle(R.string.permission_title)
                    .setMessage(R.string.permission_storage_denied)
                    .setPositiveButton(R.string.accept, (dialog, which) -> {
                        try {
                            Intent settings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            settings.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(settings);
                        } catch (Exception ignored) {
                            // The user can still grant the permission from system settings.
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }
    }
}
