package com.bawaviki.youtubedl_android.utils;

import com.orhanobut.logger.Logger;
import com.bawaviki.youtubedl_android.DownloadProgressCallback;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StreamProcessExtractor extends Thread {
    private static final int GROUP_PERCENT = 1;
    private static final int GROUP_SIZE = 2;
    private static final int GROUP_RATE = 3;
    private static final int GROUP_MINUTES = 4;
    private static final int GROUP_SECONDS = 5;
    private final static long KIB_FACTOR = 1024;
    private final static long MIB_FACTOR = 1024 * KIB_FACTOR;
    private InputStream stream;
    private StringBuffer buffer;
    private final DownloadProgressCallback callback;

    private Pattern p = Pattern.compile("\\[download\\]\\s+(\\d+\\.\\d)% of (.*) at (.*) ETA (\\d+):(\\d+)");

    public StreamProcessExtractor(StringBuffer buffer, InputStream stream, DownloadProgressCallback callback) {
        this.stream = stream;
        this.buffer = buffer;
        this.callback = callback;
        this.start();
    }

    public void run() {
        try {
            Reader in = new InputStreamReader(stream, "UTF-8");
            StringBuilder currentLine = new StringBuilder();
            int nextChar;
            while ((nextChar = in.read()) != -1) {
                buffer.append((char) nextChar);
                if (nextChar == '\r' && callback != null) {
                    processOutputLine(currentLine.toString());
                    currentLine.setLength(0);
                    continue;
                }
                currentLine.append((char) nextChar);
            }
        } catch (IOException e) {
            Logger.e(e, "failed to read stream");
        }
    }

    private void processOutputLine(String line) {
        Matcher m = p.matcher(line);
        if (m.matches()) {
            float progress = Float.parseFloat(m.group(GROUP_PERCENT));
            long eta = convertToSeconds(m.group(GROUP_MINUTES), m.group(GROUP_SECONDS));
            long size = convertToBits(m.group(GROUP_SIZE));
            long rate = convertToBits(m.group(GROUP_RATE));
            callback.onProgressUpdate(progress, size, rate, eta);
        }
    }

    private int convertToSeconds(String minutes, String seconds) {
        return Integer.parseInt(minutes) * 60 + Integer.parseInt(seconds);
    }

    private long convertToBits(String size){
        
        if(!Character.isDigit(size.charAt(0)))
            size=size.substring(1);

        float inp = 0;

        if(size.contains("KiB")){

            if(size.charAt(size.length()-1)=='s'){
                inp = Float.parseFloat(size.substring(0,size.length()-5));
                return KIB_FACTOR*Math.round(inp);
            }else {
                inp = Float.parseFloat(size.substring(0,size.length()-4));
                return KIB_FACTOR*Math.round(inp);
            }

        }else if(size.contains("MiB")) {

            if(size.charAt(size.length()-1)=='s'){
                inp = Float.parseFloat(size.substring(0,size.length()-5));
                return MIB_FACTOR*Math.round(inp);
            }else {
                inp = Float.parseFloat(size.substring(0,size.length()-4));
                return MIB_FACTOR*Math.round(inp);
            }
        }

        return 0L;

    }
}
