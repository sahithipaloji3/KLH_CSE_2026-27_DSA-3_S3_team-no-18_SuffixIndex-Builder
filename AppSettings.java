package com.suffixindex.model;

/**
 * Global configuration settings for Suffix Index Builder.
 * Persisted in data/settings/application.conf.
 */
public class AppSettings {
    private int pageSize = 100;
    private boolean caseSensitive = true;
    private int previewContextLength = 30;
    private String theme = "LIGHT";
    private boolean autoRebuildOutdated = false;

    public AppSettings() {}

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = Math.max(10, Math.min(10000, pageSize));
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public int getPreviewContextLength() {
        return previewContextLength;
    }

    public void setPreviewContextLength(int previewContextLength) {
        this.previewContextLength = Math.max(5, Math.min(200, previewContextLength));
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        if (theme != null && (theme.equalsIgnoreCase("DARK") || theme.equalsIgnoreCase("LIGHT"))) {
            this.theme = theme.toUpperCase();
        }
    }

    public boolean isAutoRebuildOutdated() {
        return autoRebuildOutdated;
    }

    public void setAutoRebuildOutdated(boolean autoRebuildOutdated) {
        this.autoRebuildOutdated = autoRebuildOutdated;
    }
}
