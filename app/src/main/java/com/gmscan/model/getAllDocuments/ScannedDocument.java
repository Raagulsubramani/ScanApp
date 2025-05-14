package com.gmscan.model.getAllDocuments;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

/**
 * Model class representing a scanned document with various details.
 */
public class ScannedDocument implements Serializable {

    private String id;
    private String filePath;
    private String fileUrl;

    private String name = "";
    private String jobTitle = "";
    private String companyTitle = "";
    private String ocrText;
    private String scanType;
    private String email = "";
    private String webSite = "";
    private boolean isLike = false;

    private final ArrayList<String> phoneNumbers = new ArrayList<>();
    private String address = "";
    private String dob = "";
    private String postalCode = "";
    private String url = "";

    private Date scanDate;
    private String createdAt;
    private String updatedAt;

    // Book-specific fields
    private String isbn = "";
    private String title = "";
    private String author = "";
    private String publishDate = "";
    private String pages = "";
    private String subject = "";

    // Constructor initializes scanDate
    public ScannedDocument() {
        this.scanDate = new Date();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getCompanyTitle() { return companyTitle; }
    public void setCompanyTitle(String companyTitle) { this.companyTitle = companyTitle; }

    public String getOcrText() { return ocrText; }
    public void setOcrText(String ocrText) { this.ocrText = ocrText; }

    public String getScanType() { return scanType; }
    public void setScanType(String scanType) { this.scanType = scanType; }

    public String getWebSite() { return webSite; }
    public void setWebSite(String webSite) { this.webSite = webSite; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public ArrayList<String> getPhoneNumbers() { return phoneNumbers; }
    public void setPhoneNumbers(ArrayList<String> phoneNumbers) {
        this.phoneNumbers.clear();
        if (phoneNumbers != null) {
            this.phoneNumbers.addAll(phoneNumbers);
        }
    }

    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Date getScanDate() { return scanDate; }
    public void setScanDate(Date scanDate) { this.scanDate = scanDate; }

    public boolean isLike() { return isLike; }
    public void setLike(boolean like) { isLike = like; }

    // Book-specific getters and setters
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getPublishDate() { return publishDate; }
    public void setPublishDate(String publishDate) { this.publishDate = publishDate; }

    public String getPages() { return pages; }
    public void setPages(String pages) { this.pages = pages; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}









//package com.gmscan.model.getAllDocuments;
//
//import java.io.Serializable;
//import java.util.ArrayList;
//import java.util.Date;
//
///**
// * Model class representing a scanned document with various details.
// */ // This annotation makes it an entity
//public class ScannedDocument implements Serializable {
//    private String id;
//    private String FilePath;
//    private String FileUrl;
//    private String documentsSummary;
//
//    // Constructor to auto-initialize scanDate
//    public ScannedDocument() {
//        this.scanDate = new Date(); // Automatically sets scanDate to current date/time
//    }
//
//    private String name = "";
//    private String jobTitle = "";
//    private String companyTitle = "";
//    private String ocrText;
//    private String scanType;
//    private String email = "";
//    private String webSite = "";
//    private boolean isLike = false;
//
//    // Change getter method to match the field name
//    public String getWebSite() {
//        return webSite;
//    }
//
//    public void setWebSite(String webSite) {
//        this.webSite = webSite;
//    }
//
//    private final ArrayList<String> phoneNumbers = new ArrayList<>();
//    private String address = "";
//    private String dob = "";
//    private String postalCode = "";
//    private String url = "";
//
//    // Getter and Setter for name
//    public String getName() { return name; }
//    public void setName(String name) { this.name = name; }
//
//    public String getId() { return id; }
//    public void setId(String id) { this.id = id; }
//
//    public String getFilePath() { return FilePath; }
//    public void setFilePath(String id) { this.FilePath = FilePath; }
//
//    public String getFileUrl() { return FileUrl; }
//    public void setFileUrl(String id) { this.FileUrl = FileUrl; }
//
//    // Getter and Setter for jobTitle
//    public String getJobTitle() { return jobTitle; }
//    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
//
//    // Getter and Setter for companyTitle
//    public String getCompanyTitle() { return companyTitle; }
//    public void setCompanyTitle(String companyTitle) { this.companyTitle = companyTitle; }
//
//    // Getter and Setter for ocrText
//    public String getOcrText() { return ocrText; }
//    public void setOcrText(String ocrText) { this.ocrText = ocrText; }
//
//    // Getter and Setter for scanType
//    public String getScanType() { return scanType; }
//    public void setScanType(String scanType) { this.scanType = scanType; }
//
//    // Getter and Setter for website
//    public String getWebsite() { return webSite; }
//    public void setWebsite(String webSite) { this.webSite = webSite; }
//
//    // Getter and Setter for email
//    public String getEmail() { return email; }
//    public void setEmail(String email) { this.email = email; }
//
//    // Getter and Setter for phoneNumbers
//    public ArrayList<String> getPhoneNumbers() { return phoneNumbers; }
//    public void setPhoneNumbers(ArrayList<String> phoneNumbers) {
//        this.phoneNumbers.clear(); // Clear existing numbers
//        if (phoneNumbers != null) {
//            this.phoneNumbers.addAll(phoneNumbers); // Add new numbers
//        }
//    }
//
//    // Getter and Setter for dob (date of birth)
//    public String getDob() { return dob; }
//    public void setDob(String dob) { this.dob = dob; }
//
//    // Getter and Setter for address
//    public String getAddress() { return address; }
//    public void setAddress(String address) { this.address = address; }
//
//    // Getter and Setter for postalCode
//    public String getPostalCode() { return postalCode; }
//    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
//
//    // Getter and Setter for url
//    public String getUrl() { return url; }
//    public void setUrl(String url) { this.url = url; }
//
//    private Date scanDate;  // New field to store the scan date
//
//    // Getter and setter for scanDate
//    public Date getScanDate() {
//        return scanDate;
//    }
//
//    public void setScanDate(Date scanDate) {
//        this.scanDate = scanDate;
//    }
//
//    public boolean isLike() {
//        return isLike;
//    }
//
//    public void setLike(boolean like) {
//        isLike = like;
//    }
//
//    // Add book-specific fields
//    private String isbn = "";
//    private String title = "";
//    private String author = "";
//    private String publishDate = "";
//    private String pages = "";
//    private String subject = "";
//
//    // Getters and Setters for book-specific fields
//    public String getIsbn() { return isbn; }
//    public void setIsbn(String isbn) { this.isbn = isbn; }
//
//    public String getTitle() { return title; }
//    public void setTitle(String title) { this.title = title; }
//
//    public String getAuthor() { return author; }
//    public void setAuthor(String author) { this.author = author; }
//
//    public String getPublishDate() { return publishDate; }
//    public void setPublishDate(String publishDate) { this.publishDate = publishDate; }
//
//    public String getPages() { return pages; }
//    public void setPages(String pages) { this.pages = pages; }
//
//    public String getSubject() { return subject; }
//    public void setSubject(String subject) { this.subject = subject; }
//
//    public void setCreatedAt(String currentTimestamp) {
//    }
//
//    public void setUpdatedAt(String currentTimestamp) {
//    }
//
//    public void setSummary(String summary) {
//    }
//
//    public void setdocumentName(String documentName) {
//    }
//
//    public String getSummary() {
//        return documentsSummary;
//    }
//}
