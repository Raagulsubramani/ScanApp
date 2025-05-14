package com.gmscan.model.loginRegister;

import com.google.gson.annotations.SerializedName;

/**
 * Model class representing a User used for login and registration.
 */
public class User {

	// Indicates login status (not serialized)
	public boolean isLogin = false;

	// Auth token
	private String accessToken;

	// Serialized fields
	@SerializedName("id")
	private String id;

	@SerializedName("mobile_number")
	private String mobileNumber;

	@SerializedName("file_url")
	private String fileUrl;

	@SerializedName("email")
	private String email;

	@SerializedName("password")
	private String password;

	@SerializedName("first_name")
	private String firstName;

	@SerializedName("last_name")
	private String lastName;

	@SerializedName("auth_provider")
	private String authProvider;

	// ------------------- Constructors -------------------

	// Empty constructor for Gson/Retrofit
	public User() {}

	// Constructor for Login
	public User(String email, String password) {
		this.email = email;
		this.password = password;
	}

	// Constructor for Registration
	public User(String email, String password, String firstName, String lastName, String mobileNumber) {
		this.email = email;
		this.password = password;
		this.firstName = firstName;
		this.lastName = lastName;
		this.mobileNumber = mobileNumber;
	}

	// ------------------- Getters and Setters -------------------

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getMobileNumber() {
		return mobileNumber;
	}

	public void setMobileNumber(String mobileNumber) {
		this.mobileNumber = mobileNumber;
	}

	public String getFileUrl() {
		return fileUrl;
	}

	public void setFileUrl(String fileUrl) {
		this.fileUrl = fileUrl;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public boolean isLogin() {
		return isLogin;
	}

	public void setLogin(boolean login) {
		isLogin = login;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getAuthProvider() {
		return authProvider;
	}

	public void setAuthProvider(String authProvider) {
		this.authProvider = authProvider;
	}
}
