package com.gmscan.model.openbooksresponse;

import com.google.gson.annotations.SerializedName;

public class Cover{

	@SerializedName("small")
	private String small;

	@SerializedName("large")
	private String large;

	@SerializedName("medium")
	private String medium;

	public String getSmall(){
		return small;
	}

	public String getLarge(){
		return large;
	}

	public String getMedium(){
		return medium;
	}
}