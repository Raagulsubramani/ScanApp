package com.gmscan.model.getAllDocuments;

import com.google.gson.annotations.SerializedName;

public class GetAllDocumentsResponseItem {
    @SerializedName("profession")
    private String profession;
    @SerializedName("file_url")
    private String fileUrl;


    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    @SerializedName("author_name")
    private String authorName;

    @SerializedName("summary")
    private String summary;

    @SerializedName("website")
    private String website;

    @SerializedName("is_favorite")
    private boolean isFavorite;

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }


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

    public String getProfession() {
        return profession;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getSummary() {
        return summary;
    }

    public String getWebsite() {
        return website;
    }

    public boolean isIsFavorite() {
        return isFavorite;
    }

    public String getAddress() {
        return address;
    }

    public String getNumberOfPages() {
        return numberOfPages;
    }

    public String getSubject() {
        return subject;
    }

    public String getScanType() {
        return scanType;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getBookName() {
        return bookName;
    }

    public String getIsbnNo() {
        return isbnNo;
    }

    public String getDocumentName() {
        return documentName;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public String getUserId() {
        return userId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getPublication() {
        return publication;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setScanType(String scanType) {
        this.scanType = scanType;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

}