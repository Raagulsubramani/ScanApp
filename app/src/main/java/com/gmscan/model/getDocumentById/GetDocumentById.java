package com.gmscan.model.getDocumentById;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class GetDocumentById{
	@SerializedName("file_url")
	private String fileUrl;


	public String getFileUrl() {
		return fileUrl;
	}

	public void setFileUrl(String fileUrl) {
		this.fileUrl = fileUrl;
	}


	@SerializedName("profession")
	private String profession;

	@SerializedName("author_name")
	private String authorName;

	@SerializedName("summary")
	private String summary;

	@SerializedName("website")
	private String webSite;

	@SerializedName("is_favorite")
	private Boolean isFavorite;

	@SerializedName("address")
	private String address;

	@SerializedName("number_of_pages")
	private String numberOfPages;

	@SerializedName("subject")
	private String subject;

	@SerializedName("scan_type")
	private String scanType;

	@SerializedName("created_at")
	private String createdAt;

	@SerializedName("book_name")
	private String bookName;

	@SerializedName("isbn_no")
	private String isbnNo;

	@SerializedName("document_name")
	private String documentName;

	@SerializedName("updated_at")
	private String updatedAt;

	@SerializedName("user_id")
	private String userId;

	@SerializedName("company_name")
	private String companyName;

	@SerializedName("publication")
	private String publication;

	@SerializedName("name")
	private String name;

	@SerializedName("id")
	private String id;

	@SerializedName("mobile_number")
	private String mobileNumber;

	@SerializedName("email")
	private String email;

	@SerializedName("ocr_text")
	private String ocrText;

	@SerializedName("description")
	private String description;

	@SerializedName("phone_numbers")
	private ArrayList<String> phoneNumbers;

	@SerializedName("data")
	private Object data;

	public void setProfession(String profession){
		this.profession = profession;
	}

	public String getProfession(){
		return profession;
	}

	public void setAuthorName(String authorName){
		this.authorName = authorName;
	}

	public String getAuthorName(){
		return authorName;
	}

	public void setSummary(String summary){
		this.summary = summary;
	}

	public String getSummary(){
		return summary;
	}

	public void setWebSite(String webSite){
		this.webSite = webSite;
	}

	public String getWebSite(){
		return webSite;
	}

	public void setIsFavorite(Boolean isFavorite){
		this.isFavorite = isFavorite;
	}

	public Boolean isIsFavorite(){
		return isFavorite;
	}

	public void setAddress(String address){
		this.address = address;
	}

	public String getAddress(){
		return address;
	}

	public void setNumberOfPages(String numberOfPages){
		this.numberOfPages = numberOfPages;
	}

	public String getNumberOfPages(){
		return numberOfPages;
	}

	public void setSubject(String subject){
		this.subject = subject;
	}

	public String getSubject(){
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

	public void setBookName(String bookName){
		this.bookName = bookName;
	}

	public String getBookName(){
		return bookName;
	}

	public void setIsbnNo(String isbnNo){
		this.isbnNo = isbnNo;
	}

	public String getIsbnNo(){
		return isbnNo;
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

	public void setPublication(String publication){
		this.publication = publication;
	}

	public String getPublication(){
		return publication;
	}

	public void setName(String name){
		this.name = name;
	}

	public String getName(){
		return name;
	}

	public void setId(String id){
		this.id = id;
	}

	public String getId(){
		return id;
	}

	public void setMobileNumber(String mobileNumber){
		this.mobileNumber = mobileNumber;
	}

	public String getMobileNumber(){
		return mobileNumber;
	}

	public void setEmail(String email){
		this.email = email;
	}

	public String getEmail(){
		return email;
	}

	public void setOcrText(String ocrText) {
		this.ocrText = ocrText;
	}

	public String getOcrText() {
		return ocrText != null ? ocrText : "";
	}

//	public void setPhoneNumbers(ArrayList<String> phoneNumbers) {
//		this.phoneNumbers = phoneNumbers;
//	}
//
//	public ArrayList<String> getPhoneNumbers() {
//		return phoneNumbers != null ? phoneNumbers : new ArrayList<>();
//	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description != null ? description : "";
	}

	public void setData(Object data) {
		this.data = data;
	}

	public Object getData() {
		return data;
	}

	@NonNull
	@Override
	public String toString() {
		return "GetDocumentById{" +
				"name='" + name + '\'' +
				", email='" + email + '\'' +
				", mobileNumber='" + mobileNumber + '\'' +
				", address='" + address + '\'' +
				", profession='" + profession + '\'' +
				", id='" + id + '\'' +
				'}';
	}

	public String getContent() {
        return "";
    }
}