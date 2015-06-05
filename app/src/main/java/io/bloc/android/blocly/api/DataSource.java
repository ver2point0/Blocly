package io.bloc.android.blocly.api;

import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import io.bloc.android.blocly.BloclyApplication;
import io.bloc.android.blocly.BuildConfig;
import io.bloc.android.blocly.R;
import io.bloc.android.blocly.api.model.RssFeed;
import io.bloc.android.blocly.api.model.RssItem;
import io.bloc.android.blocly.api.model.database.DatabaseOpenHelper;
import io.bloc.android.blocly.api.model.database.table.RssFeedTable;
import io.bloc.android.blocly.api.model.database.table.RssItemTable;
import io.bloc.android.blocly.api.network.GetFeedsNetworkRequest;

public class DataSource {
    private DatabaseOpenHelper mDatabaseOpenHelper;
    private RssFeedTable mRssFeedTable;
    private RssItemTable mRssItemTable;
    private List<RssFeed> mFeeds;
    private List<RssItem> mItems;

    public DataSource() {
        mRssFeedTable = new RssFeedTable();
        mRssItemTable = new RssItemTable();

        mDatabaseOpenHelper = new DatabaseOpenHelper(BloclyApplication.getSharedInstance(),
                mRssFeedTable, mRssItemTable);

        mFeeds = new ArrayList<RssFeed>();
        mItems = new ArrayList<RssItem>();
        createFakeData();

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (BuildConfig.DEBUG && false) {
                    BloclyApplication.getSharedInstance().deleteDatabase("blocly_db");
                }
                SQLiteDatabase writableDatabase = mDatabaseOpenHelper.getWritableDatabase();
                new GetFeedsNetworkRequest("http://feeds.feedburner.com/androidcentral?format=xml").performRequest();
            }
        }).start();
    }

    public List<RssFeed> getFeeds() {
        return mFeeds;
    }

    public List<RssItem> getItems() {
        return mItems;
    }

    void createFakeData() {
        mFeeds.add(new RssFeed("My Favorite Feed", "This feed is just incredible, I can't even begin to tell you...",
                "http://favoritefeed.net", "http://feeds/feedburner.com/favorite_feed?format=xml"));
        for (int i = 0; i < 10; i++) {
            mItems.add(new RssItem(String.valueOf(i),
                    BloclyApplication.getSharedInstance().getString(R.string.placeholder_headline) + " " + i,
                    BloclyApplication.getSharedInstance().getString(R.string.placeholder_content),
                    BloclyApplication.getSharedInstance().getString(R.string.favorite_feed_url),
                    BloclyApplication.getSharedInstance().getString(R.string.blocly_background_image),
                    0, System.currentTimeMillis(), false, false, false));
        }
    }
}
