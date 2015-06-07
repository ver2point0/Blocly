package io.bloc.android.blocly.api.model;


public class RssFeed extends Model {

    private String mTitle;
    private String mDescription;
    private String mSiteUrl;
    private String mFeedUrl;

    public RssFeed(long rowId, String title, String description, String siteUrl, String feedUrl) {
        super(rowId);
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
