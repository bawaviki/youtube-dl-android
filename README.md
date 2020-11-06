# youtube-dl-android  ![Android CI](https://github.com/bawaviki/youtube-dl-android/workflows/Android%20CI/badge.svg)
Android library wrapper for [youtube-dl](https://github.com/rg3/youtube-dl) executable.
Based on [yausername's youtubedl-android](https://github.com/yausername/youtubedl-android) but with ability to download binary files at 
runtime to decrease apk size.

[![](https://jitpack.io/v/bawaviki/youtube-dl-android.svg)](https://jitpack.io/#bawaviki/youtube-dl-android)        [![GitHub license](https://img.shields.io/github/license/bawaviki/youtube-dl-android?style=plastic)](https://github.com/bawaviki/youtube-dl-android/blob/ondemand/LICENSE)      [![Twitter](https://img.shields.io/twitter/url?style=social&url=https%3A%2F%2Fgithub.com%2Fbawaviki%2Fyoutube-dl-android)](https://twitter.com/intent/tweet?text=Wow:&url=https%3A%2F%2Fgithub.com%2Fbawaviki%2Fyoutube-dl-android)

[![GitHub stars](https://img.shields.io/github/stars/bawaviki/youtube-dl-android)](https://github.com/bawaviki/youtube-dl-android/stargazers)       [![GitHub forks](https://img.shields.io/github/forks/bawaviki/youtube-dl-android)](https://github.com/bawaviki/youtube-dl-android/network)


## Credits
*  [youtubedl-java](https://github.com/sapher/youtubedl-java) by [sapher](https://github.com/sapher), youtubedl-android adds android compatibility to youtubedl-java.
*  [youtubedl-android](https://github.com/yausername/youtubedl-android) by [yausername](https://github.com/yausername), youtube-dl-android adds ondemand library files download compatibility to youtubedl-android.

<br/>

## Installation

### Gradle
Step 1 : Add jitpack repository to your project build file
```gradle
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```
Step 2: Add the dependency
```gradle
dependencies {
    implementation 'com.github.bawaviki.youtube-dl-android:library:0.7.+'
}
```
Optional FFmpeg dependency can also be added
```gradle
dependencies {
    implementation 'com.github.bawaviki.youtube-dl-android:library:0.7.+'
    implementation 'com.github.bawaviki.youtube-dl-android:ffmpeg:0.7.+'
}
```

<br/>

## Usage

* youtube-dl executable and python 3.7 are bundled in the library.
* Initialize library, preferably in `onCreate`.

```java
try {
    YoutubeDL.getInstance().init(getApplication(),this);
} catch (YoutubeDLException e) {
    Log.e(TAG, "failed to initialize youtubedl-android", e);
}
```


* Downloading / custom command (A detailed example can be found in the [sample app](app/src/main/java/com/yausername/youtubedl_android_example/DownloadingExampleActivity.java))
```java
File youtubeDLDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "youtubedl-android");
YoutubeDLRequest request = new YoutubeDLRequest("https://vimeo.com/22439234");
request.setOption("-o", youtubeDLDir.getAbsolutePath() + "/%(title)s.%(ext)s");
YoutubeDL.getInstance().execute(request, (progress, etaInSeconds) -> {
    System.out.println(String.valueOf(progress) + "% (ETA " + String.valueOf(etaInSeconds) + " seconds)");
});
```


* Get stream info (equivalent to `--dump-json` of youtube-dl)
```java
VideoInfo streamInfo = YoutubeDL.getInstance().getInfo("https://vimeo.com/22439234");
System.out.println(streamInfo.getTitle());
```

* Get Playlist info (equivalent to `--dump-single-json` of youtube-dl)
```java
PlaylistInfo playlistInfo = YoutubeDL.getInstance().getPlaylistInfo("https://www.youtube.com/playlist?list=PLpuxPG4TUOR4N78vlcDHu46hM1LDrz8Lw");
System.out.println(playlistInfo.Id());
```

* youtube-dl supports myriad different options which be seen [here](https://github.com/rg3/youtube-dl)

* youtube-dl binary can be updated from within the library
```java
YoutubeDL.getInstance().updateYoutubeDL(getApplication());
```

## FFmpeg
If you wish to use ffmpeg features of youtube-dl (e.g. --extract-audio), include and initialize the ffmpeg library(you only need to Initialize FFmpeg library youtube-dl already initialize in this).
```java
try {
    FFmpeg.getInstance().init(getApplication(),this);
} catch (YoutubeDLException e) {
    Log.e(TAG, "failed to initialize ffmpeg", e);
}
```

<br/>

## Sample app

![Download Example](https://media.giphy.com/media/LpDmy1nS4JjERk39xS/giphy.gif)
![Streaming Example](https://media.giphy.com/media/1qXGlSPB3pqRQ7dLxx/giphy.gif)

<br/>

## Docs
 *  Though not required for just using this library, documentation on building python for android can be seen [here](BUILD_PYTHON.md). Same for ffmpeg [here](BUILD_FFMPEG.md).
 * youtubedl-android uses lazy extractors based build of youtube-dl ([youtubedl-lazy](https://github.com/yausername/youtubedl-lazy/))
