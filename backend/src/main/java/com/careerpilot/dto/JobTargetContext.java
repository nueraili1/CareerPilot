package com.careerpilot.dto;

import java.util.ArrayList;
import java.util.List;

public class JobTargetContext {

    private String companyName;
    private String roleTitle;
    private String description;
    private String imageFileName;
    private String imageExtractedText;
    private String searchQuery;
    private String enrichmentSummary;
    private String searchHint;
    private boolean searchPerformed;
    private List<WebSearchResult> webSearchResults = new ArrayList<>();

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getRoleTitle() {
        return roleTitle;
    }

    public void setRoleTitle(String roleTitle) {
        this.roleTitle = roleTitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageFileName() {
        return imageFileName;
    }

    public void setImageFileName(String imageFileName) {
        this.imageFileName = imageFileName;
    }

    public String getImageExtractedText() {
        return imageExtractedText;
    }

    public void setImageExtractedText(String imageExtractedText) {
        this.imageExtractedText = imageExtractedText;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public String getEnrichmentSummary() {
        return enrichmentSummary;
    }

    public void setEnrichmentSummary(String enrichmentSummary) {
        this.enrichmentSummary = enrichmentSummary;
    }

    public String getSearchHint() {
        return searchHint;
    }

    public void setSearchHint(String searchHint) {
        this.searchHint = searchHint;
    }

    public boolean isSearchPerformed() {
        return searchPerformed;
    }

    public void setSearchPerformed(boolean searchPerformed) {
        this.searchPerformed = searchPerformed;
    }

    public List<WebSearchResult> getWebSearchResults() {
        return webSearchResults;
    }

    public void setWebSearchResults(List<WebSearchResult> webSearchResults) {
        this.webSearchResults = webSearchResults == null ? new ArrayList<>() : webSearchResults;
    }
}
