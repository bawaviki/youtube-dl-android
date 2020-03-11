package com.bawaviki.youtubedl_android;

import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;
import com.bawaviki.youtubedl_android.mapper.VideoInfo;
import com.bawaviki.youtubedl_android.utils.StreamGobbler;
import com.bawaviki.youtubedl_android.utils.StreamProcessExtractor;
import com.bawaviki.youtubedl_android.utils.YoutubeDLUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class YoutubeDL {

    private static final YoutubeDL INSTANCE = new YoutubeDL();
    protected static final String baseName = "youtubedl-android";
    private static final String packagesRoot = "packages";
    private static final String pythonBin = "usr/bin/python";
    protected static final String youtubeDLName = "youtube-dl";
    private static final String youtubeDLBin = "__main__.py";
    protected static final String youtubeDLFile = "youtube_dl.zip";

    private AlertDialog.Builder alert;
    private static ProgressDialog progressDialog;

    private static Application app;
    private static File packagesDir;
    private static final String sharedPrefsName = "python-Check";
    private static final String isPython = "python-available";
    private static final String releasesUrl = "https://github.com/bawaviki/arm_packages/raw/master/python3_7_arm.zip";

    private boolean initialized = false;
    private static File pythonPath;
    private File youtubeDLPath;
    private File binDir;
    private String ENV_LD_LIBRARY_PATH;
    private String ENV_SSL_CERT_FILE;

    protected static final ObjectMapper objectMapper = new ObjectMapper();

    private YoutubeDL(){
    }

    public static YoutubeDL getInstance() {
        return INSTANCE;
    }

    synchronized public void init(Application application,Context context) throws YoutubeDLException {
        if (initialized) return;

        initLogger();

        File baseDir = new File(application.getFilesDir(), baseName);
        if(!baseDir.exists()) baseDir.mkdir();

        packagesDir = new File(baseDir, packagesRoot);
        binDir = new File(packagesDir, "usr/bin");
        pythonPath = new File(packagesDir, pythonBin);

        File youtubeDLDir = new File(baseDir, youtubeDLName);
        youtubeDLPath = new File(youtubeDLDir, youtubeDLBin);

        ENV_LD_LIBRARY_PATH = packagesDir.getAbsolutePath() + "/usr/lib";
        ENV_SSL_CERT_FILE = packagesDir.getAbsolutePath() + "/usr/etc/tls/cert.pem";

        app=application;
        initPython(application, packagesDir,context);
        initYoutubeDL(application, youtubeDLDir);

        initialized = true;
    }

    protected void initYoutubeDL(Application application, File youtubeDLDir) throws YoutubeDLException {
        if (!youtubeDLDir.exists()) {
            youtubeDLDir.mkdirs();
//            try {
//                YoutubeDLUtils.unzip(application.getResources().openRawResource(R.raw.youtube_dl), youtubeDLDir);
//            } catch (IOException e) {
//                YoutubeDLUtils.delete(youtubeDLDir);
//                throw new YoutubeDLException("failed to initialize", e);
//            }
        }
    }

    protected void initPython(Application application, File packagesDir,Context context) throws YoutubeDLException {

        initAlertBox(context,application);

        if(!pythonIsAvailable(application)){
            alert.show();
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

    private void assertInit() {
        if (!initialized) throw new IllegalStateException("instance not initialized");
    }

    public VideoInfo getInfo(String url) throws YoutubeDLException, InterruptedException {
        YoutubeDLRequest request = new YoutubeDLRequest(url);
        request.setOption("--dump-json");
        YoutubeDLResponse response = execute(request, null);

        VideoInfo videoInfo;

        try {
            videoInfo = objectMapper.readValue(response.getOut(), VideoInfo.class);
        } catch (IOException e) {
            throw new YoutubeDLException("Unable to parse video information", e);
        }

        return videoInfo;
    }

    public YoutubeDLResponse execute(YoutubeDLRequest request) throws YoutubeDLException, InterruptedException {
        return execute(request, null);
    }

    public YoutubeDLResponse execute(YoutubeDLRequest request, @Nullable DownloadProgressCallback callback) throws YoutubeDLException, InterruptedException {
        assertInit();

        YoutubeDLResponse youtubeDLResponse;
        Process process;
        int exitCode;
        StringBuffer outBuffer = new StringBuffer(); //stdout
        StringBuffer errBuffer = new StringBuffer(); //stderr
        long startTime = System.currentTimeMillis();

        List<String> args = request.buildCommand();
        List<String> command = new ArrayList<>();
        command.addAll(Arrays.asList(pythonPath.getAbsolutePath(), youtubeDLPath.getAbsolutePath()));
        command.addAll(args);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Map<String, String> env = processBuilder.environment();
        env.put("LD_LIBRARY_PATH", ENV_LD_LIBRARY_PATH);
        env.put("SSL_CERT_FILE", ENV_SSL_CERT_FILE);
        env.put("PATH",  System.getenv("PATH") + ":" + binDir.getAbsolutePath());

        try {
            process = processBuilder.start();
        } catch (IOException e) {
            throw new YoutubeDLException(e);
        }

        InputStream outStream = process.getInputStream();
        InputStream errStream = process.getErrorStream();

        StreamProcessExtractor stdOutProcessor = new StreamProcessExtractor(outBuffer, outStream, callback);
        StreamGobbler stdErrProcessor = new StreamGobbler(errBuffer, errStream);

        try {
            stdOutProcessor.join();
            stdErrProcessor.join();
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            process.destroy();
            throw e;
        }

        String out = outBuffer.toString();
        String err = errBuffer.toString();

        if (exitCode > 0) {
            throw new YoutubeDLException(err);
        }

        long elapsedTime = System.currentTimeMillis() - startTime;

        youtubeDLResponse = new YoutubeDLResponse(command, exitCode, elapsedTime, out, err);

        return youtubeDLResponse;
    }

    synchronized public YoutubeDLUpdater.UpdateStatus updateYoutubeDL(Application application) throws YoutubeDLException {
        try {
            return YoutubeDLUpdater.update(application);
        } catch (IOException e) {
            throw new YoutubeDLException("failed to update youtube-dl", e);
        }
    }

    private Boolean pythonIsAvailable(Application application){
        SharedPreferences pref = application.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);
        String oldVersion = pref.getString(isPython, null);
        if("true".equals(oldVersion) && pythonPath.exists() ){
            return true;
        }else
            return false;
    }

    private void initAlertBox(Context context, final Application application){
        alert=new AlertDialog.Builder(context);
        alert.setTitle("Download Python");
        alert.setMessage("Python binary is needed by this application.");
        alert.setCancelable(false);
        initProcessDialog(context);
        alert.setPositiveButton("Download now", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                new InstallPython().execute(application);
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
                    + "/python_arm.zip");

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

        return new File(application.getCacheDir()+"/python_arm.zip");
    }

    private static void updateSharedPrefs(Application application, String tag) throws YoutubeDLException {
        SharedPreferences pref = application.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(isPython, tag);
        editor.apply();
    }


    static class  InstallPython extends AsyncTask <Application,Integer,Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Application... applications) {
            if (!pythonPath.exists()) {
                if (!packagesDir.exists()) {
                    packagesDir.mkdirs();
                }
                progressDialog.setProgress(2);
                try {
                    YoutubeDLUtils.unzip(download(applications[0],releasesUrl), packagesDir);
                } catch (IOException e) {
                    // delete for recovery later
                    YoutubeDLUtils.delete(pythonPath);
                    try {
                        throw new YoutubeDLException("failed to initialize", e);
                    } catch (YoutubeDLException ex) {
                        ex.printStackTrace();
                    }
                } catch (YoutubeDLException e) {
                    e.printStackTrace();
                }
                pythonPath.setExecutable(true);
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
        }
    }
}
