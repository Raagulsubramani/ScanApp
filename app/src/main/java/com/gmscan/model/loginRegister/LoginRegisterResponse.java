package com.gmscan.model.loginRegister;

import com.google.gson.annotations.SerializedName;

public class LoginRegisterResponse {

	@SerializedName("access_token")
	private String accessToken;

	@SerializedName("user")
	private User user;

	public String getAccessToken(){
		return accessToken;
	}

	public User getUser(){
		return user;
	}
}