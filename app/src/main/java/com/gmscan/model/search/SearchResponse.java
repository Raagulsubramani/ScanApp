package com.gmscan.model.search;

import java.util.List;
import com.google.gson.annotations.SerializedName;

public class SearchResponse{

	@SerializedName("SearchResponse")
	private List<SearchResponseItem> searchResponse;

	public List<SearchResponseItem> getSearchResponse(){
		return searchResponse;
	}
}