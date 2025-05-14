package com.gmscan.model;

import com.google.gson.annotations.SerializedName;

public class DeleteDocumentResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("document_id")
    private String documentId;

    @SerializedName("deleted_at")
    private String deletedAt;

    @SerializedName("user_id")
    private String userId;

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getDeletedAt() {
        return deletedAt;
    }

    public String getUserId() {
        return userId;
    }
}