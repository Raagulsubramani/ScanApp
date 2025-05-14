package com.gmscan.model.openbooksresponse;

import com.google.gson.annotations.SerializedName;

public class PublishersItem{

	@SerializedName("name")
	private String name;

	public String getName(){
		return name;
	}
}