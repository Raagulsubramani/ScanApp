package com.gmscan.model.getAllDocuments;

import java.util.List;
import com.google.gson.annotations.SerializedName;

public class GetAllDocumentsResponse{

	@SerializedName("GetAllDocumentsResponse")
	private List<GetAllDocumentsResponseItem> getAllDocumentsResponse;

	public List<GetAllDocumentsResponseItem> getGetAllDocumentsResponse(){
		return getAllDocumentsResponse;
	}
}