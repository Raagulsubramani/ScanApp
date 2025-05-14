package com.gmscan.utility;

public enum DocumentType {
    ID("id"),
    BUSINESS("business"),
    BOOK("book"),
    DOCUMENT("document");

    private final String displayName;

    // Constructor to initialize the custom text for each enum constant
    DocumentType(String displayName) {
        this.displayName = displayName;
    }

    // Method to get the custom text for each enum
    public String getDisplayName() {
        return displayName;
    }

    // Static method to get enum constant from display name
    public static DocumentType fromDisplayName(String displayName) {
        for (DocumentType type : DocumentType.values()) {
            if (type.getDisplayName().equalsIgnoreCase(displayName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No enum constant with displayName: " + displayName);
    }
}