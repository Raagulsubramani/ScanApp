package com.gmscan.model.openbooksresponse;

import com.google.gson.annotations.SerializedName;

public class LinksItem{

	@SerializedName("title")
	private String title;

	@SerializedName("url")
	private String url;

	public String getTitle(){
		return title;
	}

	public String getUrl(){
		return url;
	}
}