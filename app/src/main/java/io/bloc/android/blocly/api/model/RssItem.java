package io.bloc.android.blocly.api.model;

public class RssItem extends Model {

    private String mGuid;
    private String mTitle;
    private String mDescription;
    private String mUrl;
    private String mImageUrl;
    private long mRssFeedId;
    private long mDatePublished;
    private boolean mRead;
    private boolean mFavorite;
    private boolean mArchived;

    public RssItem(long rowId, String guid, String title, String description, String url, String imageUrl, long rssFeedId, long datePublished, boolean read, boolean favorite, boolean archived) {
        super(rowId);
        mGuid = guid;
        mTitle = title;
        mDescription = description;
        mUrl = url;
        mImageUrl = imageUrl;
        mRssFeedId = rssFeedId;
        mDatePublished = datePublished;
        mRead = read;
        mFavorite = favorite;
        mArchived= archived;
    }

    public String getGuid() {
        return mGuid;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getImageUrl() {
        return mImageUrl;
    }

    public long getRssFeedId() {
        return mRssFeedId;
    }

    public long getDatePublished() {
        return mDatePublished;
    }

    public boolean isRead() {
        return mRead;
    }

    public boolean isFavorite() {
        return mFavorite;
    }

    public boolean isArchived() {
        return mArchived;
    }
}
