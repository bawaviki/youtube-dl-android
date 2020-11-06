package com.bawaviki.youtubedl_android_example;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bawaviki.ffmpeg.FFmpeg;
import com.bawaviki.youtubedl_android.YoutubeDLException;
import com.orhanobut.logger.Logger;
import com.bawaviki.youtubedl_android.DownloadProgressCallback;
import com.bawaviki.youtubedl_android.YoutubeDL;
import com.bawaviki.youtubedl_android.YoutubeDLRequest;

import org.apache.commons.lang3.StringUtils;

import java.io.File;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class DownloadingExampleActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnStartDownload;
    private EditText etUrl;
    private ProgressBar progressBar;
    private TextView tvDownloadStatus;
    NotificationCompat.Builder notifi;
    NotificationManager notificationManager;
    private static String CHANNEL_ID="myid";

    private boolean downloading = false;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private DownloadProgressCallback callback = new DownloadProgressCallback() {

        @Override
        public void onProgressUpdate(float progress, long size, long rate, long etaInSeconds) {
            notificationManager.notify(0,notifi.build());

            notifi.setProgress(100, (int) progress,false);
            runOnUiThread(() -> {
                        progressBar.setProgress((int) progress);
                        tvDownloadStatus.setText(String.valueOf(progress)+"% of size "+ Formatter.formatShortFileSize(DownloadingExampleActivity.this,size)+" at "+Formatter.formatShortFileSize(DownloadingExampleActivity.this,rate) + "/s (ETA " + String.valueOf(etaInSeconds) + " seconds)");
                    }
            );
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloading_example);

        try {
            //YoutubeDL.getInstance().init(getApplication(),StreamingExampleActivity.this);
            FFmpeg.getInstance().init(getApplication(),DownloadingExampleActivity.this);
        } catch (YoutubeDLException e) {
            Log.e("ffmpeg", "failed to initialize youtubedl-android", e);
        }
        initViews();
        initListeners();
        createNotfiChannel();
    }

    private void initViews() {
        btnStartDownload = findViewById(R.id.btn_start_download);
        etUrl = findViewById(R.id.et_url);
        progressBar = findViewById(R.id.progress_bar);
        tvDownloadStatus = findViewById(R.id.tv_status);
        notifi=new NotificationCompat.Builder(DownloadingExampleActivity.this,CHANNEL_ID)
                .setContentText("Downloading")
                .setSmallIcon(R.drawable.exomedia_ic_play_arrow_white)
                .setContentTitle("Download file")
                .setOngoing(true);
        notificationManager=(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    }

    private void initListeners() {
        btnStartDownload.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start_download: {
                startDownload();
                break;
            }
        }
    }

    private void startDownload() {
        if (downloading) {
            Toast.makeText(DownloadingExampleActivity.this, "cannot start download. a download is already in progress", Toast.LENGTH_LONG).show();
            return;
        }

        if (!isStoragePermissionGranted()) {
            Toast.makeText(DownloadingExampleActivity.this, "grant storage permission and retry", Toast.LENGTH_LONG).show();
            return;
        }

        String url = etUrl.getText().toString();
        if (StringUtils.isBlank(url)) {
            etUrl.setError(getString(R.string.url_error));
            return;
        }

        YoutubeDLRequest request = new YoutubeDLRequest(url);
        File youtubeDLDir = getDownloadLocation();
        request.setOption("-o", youtubeDLDir.getAbsolutePath() + "/%(title)s.%(ext)s");

        showStart();

        downloading = true;
        Disposable disposable = Observable.fromCallable(() -> YoutubeDL.getInstance().execute(request, callback))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(youtubeDLResponse -> {
                    progressBar.setProgress(100);
                    tvDownloadStatus.setText(getString(R.string.download_complete));
                    Toast.makeText(DownloadingExampleActivity.this, "download successful", Toast.LENGTH_LONG).show();
                    downloading = false;
                    notifi.setProgress(0,0,false);
                    notifi.setOngoing(false);
                    notificationManager.notify(0,notifi.build());
                }, e -> {
                    tvDownloadStatus.setText(getString(R.string.download_failed));
                    Toast.makeText(DownloadingExampleActivity.this, "download failed", Toast.LENGTH_LONG).show();
                    Logger.e(e, "failed to download");
                    downloading = false;
                    notifi.setProgress(0,0,false);
                    notifi.setOngoing(false);
                    notificationManager.notify(0,notifi.build());
                });
        compositeDisposable.add(disposable);

    }

    @Override
    protected void onDestroy() {
        compositeDisposable.dispose();
        super.onDestroy();
    }

    @NonNull
    private File getDownloadLocation() {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File youtubeDLDir = new File(downloadsDir, "youtubedl-android");
        if (!youtubeDLDir.exists()) youtubeDLDir.mkdir();
        return youtubeDLDir;
    }

    private void showStart() {
        tvDownloadStatus.setText(getString(R.string.download_start));
        progressBar.setProgress(0);
    }

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else {
            return true;
        }
    }

    private void createNotfiChannel(){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel=new NotificationChannel(CHANNEL_ID,"my_chanell", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("download notifi");
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

}
