package com.gmscan.model.openbooksresponse;

import java.util.List;
import com.google.gson.annotations.SerializedName;

public class Identifiers{

	@SerializedName("openlibrary")
	private List<String> openlibrary;

	@SerializedName("isbn_10")
	private List<String> isbn10;

	@SerializedName("isbn_13")
	private List<String> isbn13;

	public List<String> getOpenlibrary(){
		return openlibrary;
	}

	public List<String> getIsbn10(){
		return isbn10;
	}

	public List<String> getIsbn13(){
		return isbn13;
	}
}