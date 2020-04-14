package com.bawaviki.youtubedl_android_example;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bawaviki.youtubedl_android.mapper.PlaylistEntries;
import com.bawaviki.youtubedl_android.mapper.PlaylistInfo;
import com.devbrackets.android.exomedia.ui.widget.VideoView;
import com.orhanobut.logger.Logger;
import com.bawaviki.ffmpeg.FFmpeg;
import com.bawaviki.youtubedl_android.DownloadProgressCallback;
import com.bawaviki.youtubedl_android.YoutubeDL;
import com.bawaviki.youtubedl_android.YoutubeDLException;
import com.bawaviki.youtubedl_android.YoutubeDLRequest;
import com.bawaviki.youtubedl_android.mapper.VideoFormat;
import com.bawaviki.youtubedl_android.mapper.VideoInfo;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class StreamingExampleActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnStartStream,btnmp3,btn144,btn240,btn360,btn720,btn1080,btnD;
    private EditText etUrl;
    private VideoView videoView;
    private TextView tvStreamStatus;
    private List<String> fids;
    private static String CHANNEL_ID="myid";
    private boolean downloading = false;
    private String title="savetube";
    private ProgressBar playlist_progress;
    ProgressDialog progressDialog;
    NotificationCompat.Builder notifi;
    NotificationManager notificationManager;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    LinearLayout llBottomSheet;

    // init the bottom sheet behavior
    BottomSheetBehavior bottomSheetBehavior;
    ListView listView;
    ArrayAdapter adapter;
    ArrayList<PlaylistEntries> plalist=new ArrayList <>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streaming_example);

        try {
            //YoutubeDL.getInstance().init(getApplication(),StreamingExampleActivity.this);
            FFmpeg.getInstance().init(getApplication(),StreamingExampleActivity.this);
        } catch (YoutubeDLException e) {
            Log.e("ffmpeg", "failed to initialize youtubedl-android", e);
        }
        createNotfiChannel();
        initViews();
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        initListeners();

    }

    private void initViews() {
        llBottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(llBottomSheet);
        listView=findViewById(R.id.playListItem);
        btnStartStream = findViewById(R.id.btn_start_streaming);
        btnmp3=findViewById(R.id.mp3button);
        btn144=findViewById(R.id.button144);
        btn240=findViewById(R.id.button240);
        btn360=findViewById(R.id.button360);
        btn720=findViewById(R.id.button720);
        btn1080=findViewById(R.id.button1080);
        btnD=findViewById(R.id.buttond);
        etUrl = findViewById(R.id.et_url);
        videoView = findViewById(R.id.video_view);
        tvStreamStatus = findViewById(R.id.tv_status);
        progressDialog=new ProgressDialog(StreamingExampleActivity.this);
        progressDialog.setMessage("Loading.....");
        progressDialog.setCancelable(false);
        fids=new ArrayList<>();
        notifi=new NotificationCompat.Builder(StreamingExampleActivity.this,CHANNEL_ID)
                .setContentText("Downloading")
                .setSmallIcon(R.drawable.exomedia_ic_play_arrow_white)
                .setContentTitle("Download file")
                .setOngoing(true);
        notificationManager=(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        playlist_progress=findViewById(R.id.playlist_progress);
    }

    private void initListeners() {
        btnStartStream.setOnClickListener(this);
        btnmp3.setOnClickListener(this);
        btn144.setOnClickListener(this);
        btn240.setOnClickListener(this);
        btn360.setOnClickListener(this);
        btn720.setOnClickListener(this);
        btn1080.setOnClickListener(this);
        btnD.setOnClickListener(this);
        videoView.setOnPreparedListener(() -> videoView.start());
        listView.setOnItemClickListener((adapterView, view, i, l) -> {
            startStream("https://www.youtube.com/watch?v="+plalist.get(i).id);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start_streaming: {
                if(etUrl.getText().toString().contains("/playlist?list=")) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    playlist_progress.setVisibility(View.VISIBLE);
                    playlistInfo();
                }else
                    startStream(etUrl.getText().toString());
                break;
            }
            case R.id.mp3button:{
                startDownload("--audio-format", "mp3");
                break;
            }
            case R.id.button144:{
                startDownload("-f", "160+140");
                break;
            }
            case R.id.button240:{
                startDownload("-f", "135+140");
                break;
            }
            case R.id.button360:{
                startDownload("-f", "18");
                break;
            }
            case R.id.button720:{
                startDownload("-f", "136+140");
                break;
            }
            case R.id.button1080:{
                startDownload("-f", "137+140");
                break;
            }
            case R.id.buttond:{
                if( etUrl.getText().toString().contains("https://youtube.com")|| etUrl.getText().toString().contains("https://youtu.be")) {
                    getVideoFIds();
                }else {
                    downloadVideo();
                }
                break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.dispose();
        super.onDestroy();
    }

    @SuppressLint("SetTextI18n")
    private void startStream(String url) {
        if (StringUtils.isBlank(url)) {
            etUrl.setError(getString(R.string.url_error));
            return;
        }

        tvStreamStatus.setText("Fetching Stream Info");
        Disposable disposable = Observable.fromCallable(() -> YoutubeDL.getInstance().getInfo(url))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(streamInfo -> {
                    String videoUrl = getVideoUrl(streamInfo);
                    if(StringUtils.isBlank(videoUrl)){
                        tvStreamStatus.setText("Failed to fetch Stream Info");
                        Toast.makeText(StreamingExampleActivity.this, "failed to get stream url", Toast.LENGTH_LONG).show();
                    }else{
                        tvStreamStatus.setText("Streaming Now");
                        setupVideoView(videoUrl);
                    }
                }, e -> {
                    tvStreamStatus.setText("Failed to fetch Stream Info");
                    Toast.makeText(StreamingExampleActivity.this, "streaming failed. failed to get stream info", Toast.LENGTH_LONG).show();
                    Logger.e(e, "failed to get stream info");
                });
        compositeDisposable.add(disposable);
    }

    @SuppressLint("SetTextI18n")
    private void playlistInfo() {
        String url = etUrl.getText().toString();
        if (StringUtils.isBlank(url)) {
            etUrl.setError(getString(R.string.url_error));
            return;
        }

        tvStreamStatus.setText("Fetching Playlist Info");
        Disposable disposable = Observable.fromCallable(() -> YoutubeDL.getInstance().getPlaylistInfo(url))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::getVideoIdfromPlaylist, e -> {
                    tvStreamStatus.setText("Failed to fetch Stream Info");
                    Toast.makeText(StreamingExampleActivity.this, "streaming failed. failed to get stream info", Toast.LENGTH_LONG).show();
                    Logger.e(e, "failed to get stream info");
                });
        compositeDisposable.add(disposable);
    }

    private void downloadVideo() {
        String url = etUrl.getText().toString();
        if (StringUtils.isBlank(url)) {
            etUrl.setError(getString(R.string.url_error));
            return;
        }

        Disposable disposable = Observable.fromCallable(() -> YoutubeDL.getInstance().getInfo(url))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(streamInfo -> downloadManager(getVideoUrl(streamInfo)), e -> {
                    Toast.makeText(StreamingExampleActivity.this, "Downloading failed. failed to get stream info", Toast.LENGTH_LONG).show();
                    Logger.e(e, "failed to get stream info");
                });
        compositeDisposable.add(disposable);
    }

    private void setupVideoView(String videoUrl) {
        videoView.setVideoURI(Uri.parse(videoUrl));
    }

    private void getVideoIdfromPlaylist(PlaylistInfo playlistInfo){
        plalist=playlistInfo.entries;
        playlist_progress.setVisibility(View.GONE);
        adapter = new ArrayAdapter <>(this,
                android.R.layout.simple_list_item_1, plalist);
        listView.setAdapter(adapter);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private String getVideoUrl(VideoInfo streamInfo) {
        String videoUrl = null;
        if(null == streamInfo || null == streamInfo.formats){
            Toast.makeText(StreamingExampleActivity.this, "failed to get stream url", Toast.LENGTH_LONG).show();
            return null;
        }
        title=streamInfo.title+"."+streamInfo.ext;
        //To get uploader name use
        // String author = streamInfo.uploader;
        for(VideoFormat f: streamInfo.formats){
            if( etUrl.getText().toString().contains("youtube.com")|| etUrl.getText().toString().contains("https://youtu.be")) {
                Log.e("url", ":" + f.formatId);
                if ("18".equals(f.formatId)) {
                    videoUrl = f.url;
                    break;
                }
            }else{
                if ("mp4".equals(f.ext) || "mkv".equals(f.ext)) {
                    videoUrl = f.url;
                    Log.e("url",":"+videoUrl);
                    break;
                }
            }
        }
        return videoUrl;
    }

    private DownloadProgressCallback callback = new DownloadProgressCallback() {
        @Override
        public void onProgressUpdate(float progress, long etaInSeconds) {

                notificationManager.notify(0,notifi.build());

            notifi.setProgress(100, (int) progress,false);
            runOnUiThread(() -> Toast.makeText(StreamingExampleActivity.this, progress + "% (ETA " + etaInSeconds + " seconds)", Toast.LENGTH_SHORT).show()
                );

        }
    };

    private void startDownload(String op,String vl) {
        if (downloading) {
            Toast.makeText(StreamingExampleActivity.this, "cannot start download. a download is already in progress", Toast.LENGTH_LONG).show();
            return;
        }

        if (!isStoragePermissionGranted()) {
            Toast.makeText(StreamingExampleActivity.this, "grant storage permission and retry", Toast.LENGTH_LONG).show();
            return;
        }

        String url = etUrl.getText().toString();
        if (StringUtils.isBlank(url)) {
            etUrl.setError(getString(R.string.url_error));
            return;
        }
        Log.e("vurl",":"+url);
        YoutubeDLRequest request = new YoutubeDLRequest(url);
        File youtubeDLDir = getDownloadLocation();
        request.setOption("-o", youtubeDLDir.getAbsolutePath() + "/%(title)s.%(ext)s");
        if(vl.equals("mp3"))
            request.setOption("-x");
        request.setOption(op,vl);


        downloading = true;
        Disposable disposable = Observable.fromCallable(() -> YoutubeDL.getInstance().execute(request, callback))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(youtubeDLResponse -> {
                    Toast.makeText(StreamingExampleActivity.this, "download successful", Toast.LENGTH_LONG).show();
                    downloading = false;
                    notifi.setProgress(0,0,false);
                    notifi.setOngoing(false);
                    notificationManager.notify(0,notifi.build());
                }, e -> {
                    Toast.makeText(StreamingExampleActivity.this, "download failed", Toast.LENGTH_LONG).show();
                    Logger.e(e, "failed to download");
                    downloading = false;
                });
        compositeDisposable.add(disposable);

    }

    @NonNull
    private File getDownloadLocation() {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File youtubeDLDir = new File(downloadsDir, "youtubedl-android");
        if (!youtubeDLDir.exists())
            if(youtubeDLDir.mkdir())
                Log.e("Directory", "exist");
        return youtubeDLDir;
    }


    private void getVideoFIds(){
       new FidsExtractor().execute(etUrl.getText().toString());

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

    private void downloadManager(String url){
        DownloadManager.Request req=new DownloadManager.Request(Uri.parse(url));
        req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        req.setDestinationInExternalPublicDir("/Download/",title);
        DownloadManager dm=(DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        if (dm != null) {
            dm.enqueue(req);
        }
    }

    @SuppressLint("StaticFieldLeak")
    public  class FidsExtractor extends AsyncTask<String,Void,List<String>>{

        @Override
        protected List <String> doInBackground(String... strings) {
            try {
                for(VideoFormat f: YoutubeDL.getInstance().getInfo(strings[0]).formats){
                    fids.add(f.formatId);
                }
            } catch (YoutubeDLException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return fids;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(List <String> fids) {
            super.onPostExecute(fids);
          if(fids.contains("140")){
                btnmp3.setVisibility(View.VISIBLE);
            }else
                btnmp3.setVisibility(View.INVISIBLE);
            if(fids.contains("160")){
                btn144.setVisibility(View.VISIBLE);
            }else
                btn144.setVisibility(View.INVISIBLE);
            if(fids.contains("135")){
                btn240.setVisibility(View.VISIBLE);
            }else
                btn240.setVisibility(View.INVISIBLE);
            if(fids.contains("18")){
                btn360.setVisibility(View.VISIBLE);
            }else
                btn360.setVisibility(View.INVISIBLE);
            if(fids.contains("136")) {
                btn720.setVisibility(View.VISIBLE);
            }else
                btn720.setVisibility(View.INVISIBLE);
            if(fids.contains("137")){
                btn1080.setVisibility(View.VISIBLE);
            }else
                btn1080.setVisibility(View.INVISIBLE);
            progressDialog.dismiss();
        }
    }
}
