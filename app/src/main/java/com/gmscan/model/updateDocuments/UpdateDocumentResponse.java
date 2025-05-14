package com.gmscan.model.updateDocuments;

import java.util.List;
import com.google.gson.annotations.SerializedName;

public class UpdateDocumentResponse{

	@SerializedName("author_name")
	private Object authorName;

	@SerializedName("file_path")
	private Object filePath;

	@SerializedName("file_url")
	private Object fileUrl;

	@SerializedName("is_favorite")
	private boolean isFavorite;

	@SerializedName("number_of_pages")
	private Object numberOfPages;

	@SerializedName("subject")
	private Object subject;

	@SerializedName("scan_type")
	private String scanType;

	@SerializedName("created_at")
	private String createdAt;

	@SerializedName("title")
	private Object title;

	@SerializedName("book_name")
	private Object bookName;

	@SerializedName("content")
	private Object content;

	@SerializedName("document_name")
	private String documentName;

	@SerializedName("updated_at")
	private String updatedAt;

	@SerializedName("file_type")
	private Object fileType;

	@SerializedName("publication")
	private Object publication;

	@SerializedName("id")
	private String id;

	@SerializedName("email")
	private String email;

	@SerializedName("profession")
	private String profession;

	@SerializedName("summary")
	private Object summary;

	@SerializedName("image")
	private Object image;

	@SerializedName("website")
	private String website;

	@SerializedName("address")
	private String address;

	@SerializedName("file_name")
	private Object fileName;

	@SerializedName("file_size")
	private Object fileSize;

	@SerializedName("isbn_no")
	private Object isbnNo;

	@SerializedName("tags")
	private List<Object> tags;

	@SerializedName("user_id")
	private String userId;

	@SerializedName("company_name")
	private String companyName;

	@SerializedName("name")
	private String name;

	@SerializedName("mobile_number")
	private String mobileNumber;

	public void setAuthorName(Object authorName){
		this.authorName = authorName;
	}

	public Object getAuthorName(){
		return authorName;
	}

	public void setFilePath(Object filePath){
		this.filePath = filePath;
	}

	public Object getFilePath(){
		return filePath;
	}

	public void setFileUrl(Object fileUrl){
		this.fileUrl = fileUrl;
	}

	public Object getFileUrl(){
		return fileUrl;
	}

	public void setIsFavorite(boolean isFavorite){
		this.isFavorite = isFavorite;
	}

	public boolean isIsFavorite(){
		return isFavorite;
	}

	public void setNumberOfPages(Object numberOfPages){
		this.numberOfPages = numberOfPages;
	}

	public Object getNumberOfPages(){
		return numberOfPages;
	}

	public void setSubject(Object subject){
		this.subject = subject;
	}

	public Object getSubject(){
		return subject;
	}

	public void setScanType(String scanType){
		this.scanType = scanType;
	}

	public String getScanType(){
		return scanType;
	}

	public void setCreatedAt(String createdAt){
		this.createdAt = createdAt;
	}

	public String getCreatedAt(){
		return createdAt;
	}

	public void setTitle(Object title){
		this.title = title;
	}

	public Object getTitle(){
		return title;
	}

	public void setBookName(Object bookName){
		this.bookName = bookName;
	}

	public Object getBookName(){
		return bookName;
	}

	public void setContent(Object content){
		this.content = content;
	}

	public Object getContent(){
		return content;
	}

	public void setDocumentName(String documentName){
		this.documentName = documentName;
	}

	public String getDocumentName(){
		return documentName;
	}

	public void setUpdatedAt(String updatedAt){
		this.updatedAt = updatedAt;
	}

	public String getUpdatedAt(){
		return updatedAt;
	}

	public void setFileType(Object fileType){
		this.fileType = fileType;
	}

	public Object getFileType(){
		return fileType;
	}

	public void setPublication(Object publication){
		this.publication = publication;
	}

	public Object getPublication(){
		return publication;
	}

	public void setId(String id){
		this.id = id;
	}

	public String getId(){
		return id;
	}

	public void setEmail(String email){
		this.email = email;
	}

	public String getEmail(){
		return email;
	}

	public void setProfession(String profession){
		this.profession = profession;
	}

	public String getProfession(){
		return profession;
	}

	public void setSummary(Object summary){
		this.summary = summary;
	}

	public Object getSummary(){
		return summary;
	}

	public void setImage(Object image){
		this.image = image;
	}

	public Object getImage(){
		return image;
	}

	public void setWebsite(String website){
		this.website = website;
	}

	public String getWebsite(){
		return website;
	}

	public void setAddress(String address){
		this.address = address;
	}

	public String getAddress(){
		return address;
	}

	public void setFileName(Object fileName){
		this.fileName = fileName;
	}

	public Object getFileName(){
		return fileName;
	}

	public void setFileSize(Object fileSize){
		this.fileSize = fileSize;
	}

	public Object getFileSize(){
		return fileSize;
	}

	public void setIsbnNo(Object isbnNo){
		this.isbnNo = isbnNo;
	}

	public Object getIsbnNo(){
		return isbnNo;
	}

	public void setTags(List<Object> tags){
		this.tags = tags;
	}

	public List<Object> getTags(){
		return tags;
	}

	public void setUserId(String userId){
		this.userId = userId;
	}

	public String getUserId(){
		return userId;
	}

	public void setCompanyName(String companyName){
		this.companyName = companyName;
	}

	public String getCompanyName(){
		return companyName;
	}

	public void setName(String name){
		this.name = name;
	}

	public String getName(){
		return name;
	}

	public void setMobileNumber(String mobileNumber){
		this.mobileNumber = mobileNumber;
	}

	public String getMobileNumber(){
		return mobileNumber;
	}
}