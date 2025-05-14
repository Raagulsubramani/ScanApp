package com.gmscan.model.documentUpdate;

import com.google.gson.annotations.SerializedName;

public class DocumentUpdateRequest {

    @SerializedName("title")
    public String title;

    @SerializedName("description")
    public String description;

    @SerializedName("company_name")
    public String companyName;

    @SerializedName("email")
    public String email;

    @SerializedName("mobileNumber")
    public String mobileNumber;

    @SerializedName("PhoneNumbers")
    public String PhoneNumbers;

    @SerializedName("address")
    public String address;

    @SerializedName("website")
    public String website;

    @SerializedName("document_name")
    public String documentName;

    @SerializedName("name")
    public String name;

    @SerializedName("profession")
    public String profession;

    @SerializedName("is_favorite")
    public Boolean isFavorite;

    @SerializedName("author_name")
    public String authorName;

    @SerializedName("number_of_pages")
    public int numberOfPages;

    @SerializedName("subject")
    public String subject;

    @SerializedName("publication")
    public String publication;

    @SerializedName("scan_type")
    public String scanType;

    @SerializedName("book_name")
    public String bookName;

    @SerializedName("isbn_no")
    public long isbnNo;

    // Constructors
    public DocumentUpdateRequest() {}

    public DocumentUpdateRequest(String documentName, String name, String profession) {
        this.documentName = documentName;
        this.name = name;
        this.profession = profession;
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getPhoneNumbers() {
        return PhoneNumbers;
    }

    public void setPhoneNumbers(String phoneNumbers) {
        PhoneNumbers = phoneNumbers;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfession() {
        return profession;
    }

    public void setProfession(String profession) {
        this.profession = profession;
    }

    public Boolean getFavorite() {
        return isFavorite;
    }

    public void setFavorite(Boolean favorite) {
        isFavorite = favorite;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public int getNumberOfPages() {
        return numberOfPages;
    }

    public void setNumberOfPages(int numberOfPages) {
        this.numberOfPages = numberOfPages;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getPublication() {
        return publication;
    }

    public void setPublication(String publication) {
        this.publication = publication;
    }

    public String getScanType() {
        return scanType;
    }

    public void setScanType(String scanType) {
        this.scanType = scanType;
    }

    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public long getIsbnNo() {
        return isbnNo;
    }

    public void setIsbnNo(long isbnNo) {
        this.isbnNo = isbnNo;
    }
}
