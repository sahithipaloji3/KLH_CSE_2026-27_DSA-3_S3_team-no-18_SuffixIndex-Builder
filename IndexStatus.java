package com.suffixindex.model;

/**
 * Represents the indexing status of a document.
 */
public enum IndexStatus {
    NOT_INDEXED("Not Indexed"),
    INDEXED("Indexed"),
    OUTDATED("Outdated");

    private final String displayName;

    IndexStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static IndexStatus fromString(String text) {
        if (text == null) return NOT_INDEXED;
        for (IndexStatus s : values()) {
            if (s.name().equalsIgnoreCase(text) || s.displayName.equalsIgnoreCase(text)) {
                return s;
            }
        }
        return NOT_INDEXED;
    }
}
