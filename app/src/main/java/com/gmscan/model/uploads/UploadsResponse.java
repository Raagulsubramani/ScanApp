package com.gmscan.model.uploads;

import com.google.gson.annotations.SerializedName;

public class UploadsResponse{

	@SerializedName("file_url")
	private String fileUrl;

	@SerializedName("file_path")
	private String filePath;

	@SerializedName("success")
	private boolean success;

	@SerializedName("file_name")
	private String fileName;

	@SerializedName("file_type")
	private String fileType;

	@SerializedName("file_size")
	private int fileSize;

	public String getFileUrl(){
		return fileUrl;
	}

	public String getFilePath(){
		return filePath;
	}

	public boolean isSuccess(){
		return success;
	}

	public String getFileName(){
		return fileName;
	}

	public String getFileType(){
		return fileType;
	}

	public int getFileSize(){
		return fileSize;
	}
}