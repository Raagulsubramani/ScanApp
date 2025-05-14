package com.gmscan.model.openbooksresponse;

import com.google.gson.annotations.SerializedName;

public class SubjectPlacesItem{

	@SerializedName("name")
	private String name;

	@SerializedName("url")
	private String url;

	public String getName(){
		return name;
	}

	public String getUrl(){
		return url;
	}
}