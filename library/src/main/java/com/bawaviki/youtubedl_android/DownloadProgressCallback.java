package com.bawaviki.youtubedl_android;

public interface DownloadProgressCallback {
    void onProgressUpdate(float progress, long size, long rate, long etaInSeconds);
}
