package com.bawaviki.youtubedl_android.mapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaylistInfo {

    public String id;
    @JsonProperty("webpage_url")
    public String webpageUrl;
    @JsonProperty("uploader_url")
    public String uploaderUrl;
    public String uploader;
    @JsonProperty("webpage_url_basename")
    public String webpageUrlBasename;
    public String extractor;
	public  String title;
    @JsonProperty("_type")
	public String type;
    @JsonProperty("uploader_id")
	public String uploaderId;
    @JsonProperty("extractor_key")
	public String extractorKey;
    public ArrayList<PlaylistEntries> entries;

    //getters

	public String getId() {
		return id;
	}

	public String getUploaderUrl() {
		return uploaderUrl;
	}

	public String getUploader() {
		return uploader;
	}

	public String getExtractor() {
		return extractor;
	}

	public String getUploaderId() {
		return uploaderId;
	}



	@Override
	public String toString() {
		return "PlaylistInfo [id=" + id + ", webpageUrl=" + webpageUrl + ", title=" + title + ", uploaderUrl=" + uploaderUrl
				+ ", uploader=" + uploader + ", webpageUrlBasename=" + webpageUrlBasename	+ ", uploaderId=" + uploaderId + ", type=" + type + ", extractorKey=" + extractorKey + ", extractor=" + extractor + "]";
	}
	
	
}
