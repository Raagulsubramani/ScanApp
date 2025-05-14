package com.gmscan.model;

import com.google.gson.annotations.SerializedName;

public class UpdateDocumentResponse{

	@SerializedName("profession")
	private String profession;

	@SerializedName("author_name")
	private Object authorName;

	@SerializedName("summary")
	private Object summary;

	@SerializedName("website")
	private String website;

	@SerializedName("is_favorite")
	private boolean isFavorite;

	@SerializedName("address")
	private String address;

	@SerializedName("number_of_pages")
	private Object numberOfPages;

	@SerializedName("subject")
	private Object subject;

	@SerializedName("scan_type")
	private String scanType;

	@SerializedName("created_at")
	private String createdAt;

	@SerializedName("book_name")
	private Object bookName;

	@SerializedName("isbn_no")
	private Object isbnNo;

	@SerializedName("document_name")
	private String documentName;

	@SerializedName("updated_at")
	private String updatedAt;

	@SerializedName("user_id")
	private String userId;

	@SerializedName("company_name")
	private String companyName;

	@SerializedName("publication")
	private Object publication;

	@SerializedName("name")
	private String name;

	@SerializedName("id")
	private String id;

	@SerializedName("mobile_number")
	private String mobileNumber;

	@SerializedName("email")
	private String email;

	public String getProfession(){
		return profession;
	}

	public Object getAuthorName(){
		return authorName;
	}

	public Object getSummary(){
		return summary;
	}

	public String getWebsite(){
		return website;
	}

	public boolean isIsFavorite(){
		return isFavorite;
	}

	public String getAddress(){
		return address;
	}

	public Object getNumberOfPages(){
		return numberOfPages;
	}

	public Object getSubject(){
		return subject;
	}

	public String getScanType(){
		return scanType;
	}

	public String getCreatedAt(){
		return createdAt;
	}

	public Object getBookName(){
		return bookName;
	}

	public Object getIsbnNo(){
		return isbnNo;
	}

	public String getDocumentName(){
		return documentName;
	}

	public String getUpdatedAt(){
		return updatedAt;
	}

	public String getUserId(){
		return userId;
	}

	public String getCompanyName(){
		return companyName;
	}

	public Object getPublication(){
		return publication;
	}

	public String getName(){
		return name;
	}

	public String getId(){
		return id;
	}

	public String getMobileNumber(){
		return mobileNumber;
	}

	public String getEmail(){
		return email;
	}
}