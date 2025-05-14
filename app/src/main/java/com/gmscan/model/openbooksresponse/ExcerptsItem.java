package com.gmscan.model.openbooksresponse;

import com.google.gson.annotations.SerializedName;

public class ExcerptsItem{

	@SerializedName("comment")
	private String comment;

	@SerializedName("text")
	private String text;

	@SerializedName("first_sentence")
	private boolean firstSentence;

	public String getComment(){
		return comment;
	}

	public String getText(){
		return text;
	}

	public boolean isFirstSentence(){
		return firstSentence;
	}
}