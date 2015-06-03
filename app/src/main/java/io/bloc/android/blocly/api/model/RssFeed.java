package io.bloc.android.blocly.api.model;


public class RssFeed {
    private String mTitle;
    private String mDescription;
    private String mSiteUrl;
    private String mFeedUrl;

    public RssFeed(String title, String description, String siteUrl, String feedUrl) {
        mTitle = title;
        mDescription = description;
        mSiteUrl = siteUrl;
        mFeedUrl = feedUrl;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getSiteUrl() {
        return mSiteUrl;
    }

    public String getFeedUrl() {
        return mFeedUrl;
    }
}
