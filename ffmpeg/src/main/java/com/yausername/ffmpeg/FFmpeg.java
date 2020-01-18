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
    private static File download(Application application, String url) throws YoutubeDLException, IOException {
        File file = null;
        InputStream in = null;
        FileOutputStream out = null;
        ReadableByteChannel inChannel = null;
        FileChannel outChannel = null;
        progressDialog.setProgress(10);
        try {
            URL downloadUrl = new URL(url);
            in = downloadUrl.openStream();
            inChannel = Channels.newChannel(in);
            file = File.createTempFile("ffmpeg_arm", "zip", application.getCacheDir());
            out = new FileOutputStream(file);
            outChannel = out.getChannel();
            long bytesRead=0;
            long transferPosition=0;
            while ((bytesRead=outChannel.transferFrom(inChannel,transferPosition,1 << 24)) > 0) {
                transferPosition+=bytesRead;
            }
            progressDialog.setProgress(50);
        } catch (Exception e) {
            // delete temp file if something went wrong
            if (null != file && file.exists()) {
                file.delete();
            }
            throw e;
        } finally {
            if (null != in) in.close();
            if (null != inChannel) inChannel.close();
            if (null != out) out.close();
            if (null != outChannel) outChannel.close();
        }
        progressDialog.setProgress(70);
        return file;
    }

    private static void updateSharedPrefs(Application application, String tag) throws YoutubeDLException {
        progressDialog.setProgress(90);
        SharedPreferences pref = application.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(isFfmpeg, tag);
        editor.apply();
        progressDialog.setProgress(100);
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
                progressDialog.setProgress(2);
                try {
                    YoutubeDLUtils.unzip(download(applications[0],releasesUrl), packagesDir);
                    progressDialog.setProgress(80);
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
