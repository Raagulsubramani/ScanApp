package com.gmscan.model;

import com.google.gson.annotations.SerializedName;

public class BaseResponse{

	@SerializedName("message")
	private String message;

	public String getMessage(){
		return message;
	}
}