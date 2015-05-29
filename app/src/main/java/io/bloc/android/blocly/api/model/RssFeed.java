package io.bloc.android.blocly.api.model;


public class RssFeed {
    private String title;
    private String description;
    private String siteUrl;
    private String feedUrl;

    public RssFeed(String feedUrl, String title, String description, String siteUrl) {
        this.feedUrl = feedUrl;
        this.title = title;
        this.description = description;
        this.siteUrl = siteUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getSiteUrl() {
        return siteUrl;
    }

    public String getFeedUrl() {
        return feedUrl;
    }
}
