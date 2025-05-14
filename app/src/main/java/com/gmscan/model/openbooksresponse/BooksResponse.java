package com.gmscan.model.openbooksresponse;

import java.util.List;
import com.google.gson.annotations.SerializedName;

public class BooksResponse{

	@SerializedName("subject_places")
	private List<SubjectPlacesItem> subjectPlaces;

	@SerializedName("subject_people")
	private List<SubjectPeopleItem> subjectPeople;

	@SerializedName("notes")
	private String notes;

	@SerializedName("number_of_pages")
	private int numberOfPages;

	@SerializedName("excerpts")
	private List<ExcerptsItem> excerpts;

	@SerializedName("identifiers")
	private Identifiers identifiers;

	@SerializedName("subjects")
	private List<SubjectsItem> subjects;

	@SerializedName("title")
	private String title;

	@SerializedName("url")
	private String url;

	@SerializedName("cover")
	private Cover cover;

	@SerializedName("publishers")
	private List<PublishersItem> publishers;

	@SerializedName("links")
	private List<LinksItem> links;

	@SerializedName("publish_date")
	private String publishDate;

	@SerializedName("key")
	private String key;

	@SerializedName("authors")
	private List<AuthorsItem> authors;

	public List<SubjectPlacesItem> getSubjectPlaces(){
		return subjectPlaces;
	}

	public List<SubjectPeopleItem> getSubjectPeople(){
		return subjectPeople;
	}

	public String getNotes(){
		return notes;
	}

	public int getNumberOfPages(){
		return numberOfPages;
	}

	public List<ExcerptsItem> getExcerpts(){
		return excerpts;
	}

	public Identifiers getIdentifiers(){
		return identifiers;
	}

	public List<SubjectsItem> getSubjects(){
		return subjects;
	}

	public String getTitle(){
		return title;
	}

	public String getUrl(){
		return url;
	}

	public Cover getCover(){
		return cover;
	}

	public List<PublishersItem> getPublishers(){
		return publishers;
	}

	public List<LinksItem> getLinks(){
		return links;
	}

	public String getPublishDate(){
		return publishDate;
	}

	public String getKey(){
		return key;
	}

	public List<AuthorsItem> getAuthors(){
		return authors;
	}
}