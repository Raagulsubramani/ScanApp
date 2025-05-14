package com.gmscan.model.getAllDocuments;

import io.opencensus.common.ServerStatsFieldEnums;

public class DocumentScanRequest {
    private String documentName;
    private String summary;
    private String scanType;
    private String name;
    private String profession;
    private String email;
    private String mobileNumber;
    private String address;
    private String companyName;
    private String website;
    private String id;
    private String createdAt;

    public DocumentScanRequest(String documentName, String scanType, String name, String profession,
                               String email, String mobileNumber, String address,
                               String companyName, String website,String summary) {
        this.documentName = documentName;
        this.scanType = scanType;
        this.name = name;
        this.profession = profession;
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.address = address;
        this.companyName = companyName;
        this.website = website;
        this.summary = summary;
    }

    public DocumentScanRequest(String documentName, String scanType,String summary) {
        this.documentName = documentName;
        this.scanType = scanType;
        this.summary = summary;
    }


    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getScanType() {
        return scanType;
    }

    public void setScanType(String scanType) {
        this.scanType = scanType;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getId() {
        return id;
    }
    public String getCreatedAt() {
        return createdAt;
    }
    public boolean isFavorite() {
        return false;
    }
}
