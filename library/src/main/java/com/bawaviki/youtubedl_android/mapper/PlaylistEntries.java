package com.bawaviki.youtubedl_android.mapper;

import android.support.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaylistEntries {

    public String id;
    public String url;
    public String title;
    @JsonProperty("ie_key")
    public String ieKey;
    @JsonProperty("_type")
    public String type;

    @NonNull
    @Override
    public String toString() {
        return title;
    }
}
