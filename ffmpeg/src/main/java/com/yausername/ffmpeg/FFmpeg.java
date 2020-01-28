package com.yausername.ffmpeg;

import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.utils.YoutubeDLUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

public class FFmpeg {

    private static final FFmpeg INSTANCE = new FFmpeg();
    protected static final String baseName = "youtubedl-android";
    private static final String packagesRoot = "packages";
    private static final String ffmpegBin = "usr/bin/ffmpeg";

    private static final String releasesUrl = "https://github.com/bawaviki/arm_packages/raw/master/ffmpeg_arm.zip";
    private static final String sharedPrefsName = "ffmpeg-Check";
    private static final String isFfmpeg = "ffmpeg-available";

    private AlertDialog.Builder alert;
    private static ProgressDialog progressDialog;
    private static File packagesDir;


    private boolean initialized = false;
    private static File binDir;
    private static File ffmpegPath;
    private static Application app;
    private static Context cont;

    private FFmpeg(){
    }

    public static FFmpeg getInstance(){
        return INSTANCE;
    }

    synchronized public void init(Application application, Context context) throws YoutubeDLException {
        if (initialized) return;

        initLogger();

        File baseDir = new File(application.getFilesDir(), baseName);
        if(!baseDir.exists()) baseDir.mkdir();

        packagesDir = new File(baseDir, packagesRoot);
        binDir = new File(packagesDir, "usr/bin");
        ffmpegPath = new File(packagesDir, ffmpegBin);
        app=application;
        cont=context;
        initFFmpeg(application, packagesDir,context);

        initialized = true;
    }

    private void initFFmpeg(Application application, File packagesDir,Context context){


        initAlertBox(context,application);
        if(!ffmpegIsAvailable(application)){
            alert.show();
        }else {
            try {
                YoutubeDL.getInstance().init(app,cont);
            } catch (YoutubeDLException e) {
                e.printStackTrace();
            }
        }


    }

    private static void markExecutable(File binDir) {
        File[] directoryListing = binDir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                if(!child.isDirectory()) child.setExecutable(true);
            }
        }
    }

    private void initLogger() {
        Logger.addLogAdapter(new AndroidLogAdapter() {
            @Override
            public boolean isLoggable(int priority, @Nullable String tag) {
                return BuildConfig.DEBUG;
            }
        });
    }

    private Boolean ffmpegIsAvailable(Application application){
        SharedPreferences pref = application.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);
        String oldVersion = pref.getString(isFfmpeg, null);
        if("true".equals(oldVersion) && ffmpegPath.exists() ){
            return true;
        }else
        return false;
    }

    private void initAlertBox(Context context, final Application application){
        alert=new AlertDialog.Builder(context);
        alert.setTitle("Download Ffmpeg");
        alert.setMessage("FFmpeg is needed by application.");
        alert.setCancelable(false);
        initProcessDialog(context);
        alert.setPositiveButton("Download now", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                    new InstallFfmpeg().execute(application);
            }
        });
    }

    private void initProcessDialog(Context context){
        progressDialog=new ProgressDialog(context);
        progressDialog.setCancelable(false);
        progressDialog.setTitle("Downloading");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    }

    @NonNull
    private static File download(Application application, String urls) throws YoutubeDLException, IOException {
         int count;
        try {
            URL url = new URL(urls);
            URLConnection conection = url.openConnection();
            conection.connect();

            // this will be useful so that you can show a tipical 0-100%
            // progress bar
            int lenghtOfFile = conection.getContentLength();

            // download the file
            InputStream input = new BufferedInputStream(url.openStream(),
                    8192);

            // Output stream
            OutputStream output = new FileOutputStream(application.getCacheDir().toString()
                    + "/ffmpeg_arm.zip");

            byte data[] = new byte[1024];

            long total = 0;

            while ((count = input.read(data)) != -1) {
                total += count;
                // publishing the progress....
                // After this onProgressUpdate will be called
                progressDialog.setProgress((int) ((total * 100) / lenghtOfFile));

                // writing data to file
                output.write(data, 0, count);
            }

            // flushing output
            output.flush();

            // closing streams
            output.close();
            input.close();

        } catch (Exception e) {

            Log.e("Error: ", e.getMessage());
        }

        return new File(application.getCacheDir()+"/ffmpeg_arm.zip");
    }

    private static void updateSharedPrefs(Application application, String tag) throws YoutubeDLException {
        SharedPreferences pref = application.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(isFfmpeg, tag);
        editor.apply();
    }

   static class  InstallFfmpeg extends AsyncTask<Application,Integer,Boolean>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Application... applications) {
            if (!ffmpegPath.exists()) {
                if (!packagesDir.exists()) {
                    packagesDir.mkdirs();
                }
                progressDialog.setProgress(0);
                try {
                    YoutubeDLUtils.unzip(download(applications[0],releasesUrl), packagesDir);
                } catch (IOException e) {
                    // delete for recovery later
                    YoutubeDLUtils.delete(ffmpegPath);
                    try {
                        throw new YoutubeDLException("failed to initialize", e);
                    } catch (YoutubeDLException ex) {
                        ex.printStackTrace();
                        return false;
                    }
                } catch (YoutubeDLException e) {
                    e.printStackTrace();
                    return false;
                }
                markExecutable(binDir);
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            try {
                updateSharedPrefs(app,aBoolean.toString());
            } catch (YoutubeDLException e) {
                e.printStackTrace();
            }
            progressDialog.dismiss();
            try {
                YoutubeDL.getInstance().init(app,cont);
            } catch (YoutubeDLException e) {
                e.printStackTrace();
            }
        }
    }

}
