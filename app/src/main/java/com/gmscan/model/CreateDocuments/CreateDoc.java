package com.gmscan.model.CreateDocuments;

import java.util.List;
import com.google.gson.annotations.SerializedName;

public class CreateDoc{

	@SerializedName("author_name")
	private String authorName;

	@SerializedName("file_path")
	private String filePath;

	@SerializedName("file_url")
	private String fileUrl;

	@SerializedName("is_favorite")
	private boolean isFavorite;

	@SerializedName("number_of_pages")
	private String numberOfPages;

	@SerializedName("subject")
	private String subject;

	@SerializedName("scan_type")
	private String scanType;

	@SerializedName("created_at")
	private String createdAt;

	@SerializedName("title")
	private String title;

	@SerializedName("book_name")
	private String bookName;

	@SerializedName("content")
	private String content;

	@SerializedName("document_name")
	private String documentName;

	@SerializedName("updated_at")
	private String updatedAt;

	@SerializedName("file_type")
	private String fileType;

	@SerializedName("publication")
	private String publication;

	@SerializedName("id")
	private String id;

	@SerializedName("email")
	private String email;

	@SerializedName("profession")
	private String profession;

	@SerializedName("summary")
	private String summary;

	@SerializedName("image")
	private String image;

	@SerializedName("website")
	private String website;

	@SerializedName("address")
	private String address;

	@SerializedName("file_name")
	private String fileName;

	@SerializedName("file_size")
	private int fileSize;

	@SerializedName("isbn_no")
	private String isbnNo;

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

	public String getAuthorName(){
		return authorName;
	}

	public String getFilePath(){
		return filePath;
	}

	public String getFileUrl(){
		return fileUrl;
	}

	public boolean isIsFavorite(){
		return isFavorite;
	}

	public String getNumberOfPages(){
		return numberOfPages;
	}

	public String getSubject(){
		return subject;
	}

	public String getScanType(){
		return scanType;
	}

	public String getCreatedAt(){
		return createdAt;
	}

	public String getTitle(){
		return title;
	}

	public String getBookName(){
		return bookName;
	}

	public String getContent(){
		return content;
	}

	public String getDocumentName() {
		return documentName;
	}

	public String getUpdatedAt(){
		return updatedAt;
	}

	public String getFileType(){
		return fileType;
	}

	public String getPublication(){
		return publication;
	}

	public String getId(){
		return id;
	}

	public String getEmail(){
		return email;
	}

	public String getProfession(){
		return profession;
	}

	public String getSummary(){
		return summary;
	}

	public String getImage(){
		return image;
	}

	public String getWebsite(){
		return website;
	}

	public String getAddress(){
		return address;
	}

	public String getFileName(){
		return fileName;
	}

	public int getFileSize(){
		return fileSize;
	}

	public String getIsbnNo(){
		return isbnNo;
	}

	public List<Object> getTags(){
		return tags;
	}

	public String getUserId(){
		return userId;
	}

	public String getCompanyName(){
		return companyName;
	}

	public String getName(){
		return name;
	}

	public String getMobileNumber(){
		return mobileNumber;
	}

	public void setAuthorName(String authorName) {
		this.authorName = authorName;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public void setFileUrl(String fileUrl) {
		this.fileUrl = fileUrl;
	}

	public void setIsFavorite(boolean isFavorite) {
		this.isFavorite = isFavorite;
	}

	public void setNumberOfPages(String numberOfPages) {
		this.numberOfPages = numberOfPages;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public void setScanType(String scanType) {
		this.scanType = scanType;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setBookName(String bookName) {
		this.bookName = bookName;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public void setDocumentName(String documentName) {
		this.documentName = documentName;
	}

	public void setUpdatedAt(String updatedAt) {
		this.updatedAt = updatedAt;
	}

	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	public void setPublication(String publication) {
		this.publication = publication;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setProfession(String profession) {
		this.profession = profession;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public void setWebsite(String website) {
		this.website = website;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public void setFileSize(int fileSize) {
		this.fileSize = fileSize;
	}

	public void setIsbnNo(String isbnNo) {
		this.isbnNo = isbnNo;
	}

	public void setTags(List<Object> tags) {
		this.tags = tags;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setMobileNumber(String mobileNumber) {
		this.mobileNumber = mobileNumber;
	}
}