package org.uom.cse.message;

/**
 * Created by nazick on 2/10/16.
 */
public class SearchQuery {

    private String searchQuery;
    private Long searchTime;

    public SearchQuery(String searchQuery,Long searchTime) {
        this.searchQuery = searchQuery;
        this.searchTime = searchTime;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public Long getSearchTime() {
        return searchTime;
    }

    public void setSearchTime(Long searchTime) {
        this.searchTime = searchTime;
    }
}
