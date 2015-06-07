package io.bloc.android.blocly.api;

import android.database.Cursor;
import android.os.Handler;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.bloc.android.blocly.BloclyApplication;
import io.bloc.android.blocly.BuildConfig;
import io.bloc.android.blocly.api.model.RssFeed;
import io.bloc.android.blocly.api.model.RssItem;
import io.bloc.android.blocly.api.model.database.DatabaseOpenHelper;
import io.bloc.android.blocly.api.model.database.table.RssFeedTable;
import io.bloc.android.blocly.api.model.database.table.RssItemTable;
import io.bloc.android.blocly.api.model.database.table.Table;
import io.bloc.android.blocly.api.network.GetFeedsNetworkRequest;
import io.bloc.android.blocly.api.network.NetworkRequest;

public class DataSource {

    public static interface  Callback<Result> {
        public void onSuccess(Result result);
        public void onError(String errorMessage);
    }

    private DatabaseOpenHelper mDatabaseOpenHelper;
    private RssFeedTable mRssFeedTable;
    private RssItemTable mRssItemTable;
    private ExecutorService mExecutorService;

    public DataSource() {
        mRssFeedTable = new RssFeedTable();
        mRssItemTable = new RssItemTable();

        mExecutorService = Executors.newSingleThreadExecutor();
        mDatabaseOpenHelper = new DatabaseOpenHelper(BloclyApplication.getSharedInstance(),
                mRssFeedTable, mRssItemTable);

        if (BuildConfig.DEBUG && true) {
            BloclyApplication.getSharedInstance().deleteDatabase("blocly_db");
        }
    }

    public void fetchNewFeed(final String feedURL, final Callback<RssFeed> callback) {
        final Handler callbackThreadHandler = new Handler();

        submitTask(new Runnable() {
            @Override
            public void run() {
                Cursor existingFeedCursor = RssFeedTable.fetchFeedWithURL(mDatabaseOpenHelper.getReadableDatabase(), feedURL);
                if (existingFeedCursor.moveToFirst()) {
                    final RssFeed fetchedFeed = feedFromCursor(existingFeedCursor);
                    existingFeedCursor.close();
                    callbackThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccess(fetchedFeed);
                        }
                    });
                    return;
                }

                GetFeedsNetworkRequest getFeedsNetworkRequest = new GetFeedsNetworkRequest(feedURL);
                List<GetFeedsNetworkRequest.FeedResponse> feedResponses = getFeedsNetworkRequest.performRequest();
                if (errorCheck(getFeedsNetworkRequest, callbackThreadHandler, callback)) {
                    return;
                }

                GetFeedsNetworkRequest.FeedResponse newFeedResponse = feedResponses.get(0);
                long newFeedId = new RssFeedTable.Builder()
                        .setFeedURL(newFeedResponse.channelFeedURL)
                        .setSiteURL(newFeedResponse.channelURL)
                        .setTitle(newFeedResponse.channelTitle)
                        .setDescription(newFeedResponse.channelDescription)
                        .insert(mDatabaseOpenHelper.getWritableDatabase());

                for (GetFeedsNetworkRequest.ItemResponse itemResponse : newFeedResponse.channelItems) {
                    long itemPubDate = System.currentTimeMillis();
                    DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss z", Locale.ENGLISH);
                    try {
                        itemPubDate = dateFormat.parse(itemResponse.itemPubDate).getTime();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    new RssItemTable.Builder()
                            .setTitle(itemResponse.itemTitle)
                            .setDescription(itemResponse.itemDescription)
                            .setEnclosure(itemResponse.itemEnclosureURL)
                            .setMIMEType(itemResponse.itemEnclosureMIMEType)
                            .setLink(itemResponse.itemURL)
                            .setGUID(itemResponse.itemGUID)
                            .setPubDate(itemPubDate)
                            .setRssFeed(newFeedId)
                            .insert(mDatabaseOpenHelper.getWritableDatabase());
                }
                Cursor newFeedCursor = mRssFeedTable.fetchRow(mDatabaseOpenHelper.getReadableDatabase(), newFeedId);
                newFeedCursor.moveToFirst();
                final RssFeed fetchedFeed = feedFromCursor(newFeedCursor);
                newFeedCursor.close();
                callbackThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSuccess(fetchedFeed);
                    }
                });
            }
        });
    }

    public void fetchItemsForFeed(final RssFeed rssFeed, final Callback<List<RssItem>> callback) {
        final Handler callbackThreadHandler = new Handler();
        submitTask(new Runnable() {
            @Override
            public void run() {
                final List<RssItem> resultList = new ArrayList<RssItem>();
                Cursor cursor = RssItemTable.fetchItemsForFeed(
                        mDatabaseOpenHelper.getReadableDatabase(),
                        rssFeed.getRowId());

                if (cursor.moveToFirst()) {
                    do {
                        resultList.add(itemFromCursor(cursor));
                    } while (cursor.moveToNext());
                    cursor.close();
                }
                callbackThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSuccess(resultList);
                    }
                });
            }
        });
    }


    public void fetchNewItemsFeed(final RssFeed rssFeed, final Callback<List<RssItem>> callback) {
        final Handler callbackThreadHandler = new Handler();
        submitTask(new Runnable() {
            @Override
            public void run() {
                GetFeedsNetworkRequest getFeedsNetworkRequest = new GetFeedsNetworkRequest(rssFeed.getFeedUrl());
                final List<RssItem> newItems = new ArrayList<RssItem>();
                List<GetFeedsNetworkRequest.FeedResponse> feedResponses = getFeedsNetworkRequest.performRequest();
                if (errorCheck(getFeedsNetworkRequest, callbackThreadHandler, callback)) {
                    return;
                }
                GetFeedsNetworkRequest.FeedResponse feedResponse = feedResponses.get(0);
                for (GetFeedsNetworkRequest.ItemResponse itemResponse : feedResponse.channelItems) {
                    if (RssItemTable.hasItem(mDatabaseOpenHelper.getReadableDatabase(), itemResponse.itemGUID)) {
                        continue;
                    }
                    long newItemRowId = insertResponseToDatabase(rssFeed.getRowId(), itemResponse);
                    Cursor newItemCursor = mRssItemTable.fetchRow(mDatabaseOpenHelper.getReadableDatabase(), newItemRowId);
                    newItemCursor.moveToFirst();
                    newItems.add(itemFromCursor(newItemCursor));
                    newItemCursor.close();
                }
                callbackThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSuccess(newItems);
                    }
                });
            }
        });
    }

    boolean errorCheck(GetFeedsNetworkRequest getFeedsNetworkRequest, Handler callbackThreadHandler, final Callback<?> callback) {
        if (getFeedsNetworkRequest.getErrorCode() != 0) {
            final String errorMessage;
            if (getFeedsNetworkRequest.getErrorCode() == NetworkRequest.ERROR_IO) {
                errorMessage = "Network error";
            } else if (getFeedsNetworkRequest.getErrorCode() == NetworkRequest.ERROR_MALFORMED_URL) {
                errorMessage = "Malformed URL error";
            } else if (getFeedsNetworkRequest.getErrorCode() == GetFeedsNetworkRequest.ERROR_PARSING) {
                errorMessage = "Error parsing feed";
            } else {
                errorMessage = "Error unknown";
            }
            callbackThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onError(errorMessage);
                }
            });
        }
        return getFeedsNetworkRequest.getErrorCode() != 0;
    }

    long insertResponseToDatabase(long feedId, GetFeedsNetworkRequest.ItemResponse itemResponse) {
        long itemPubDate = System.currentTimeMillis();
        DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss z", Locale.ENGLISH);
        try {
            itemPubDate = dateFormat.parse(itemResponse.itemPubDate).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new RssItemTable.Builder()
                .setTitle(itemResponse.itemTitle)
                .setDescription(itemResponse.itemDescription)
                .setEnclosure(itemResponse.itemEnclosureURL)
                .setMIMEType(itemResponse.itemEnclosureMIMEType)
                .setLink(itemResponse.itemURL)
                .setGUID(itemResponse.itemGUID)
                .setPubDate(itemPubDate)
                .setRssFeed(feedId)
                .insert(mDatabaseOpenHelper.getWritableDatabase());
    }

    static RssFeed feedFromCursor(Cursor cursor){
        return new RssFeed(Table.getRowId(cursor), RssFeedTable.getTitle(cursor), RssFeedTable.getDescription(cursor),
                RssFeedTable.getSiteURL(cursor), RssFeedTable.getFeedURL(cursor));
    }

    static RssItem itemFromCursor(Cursor cursor) {
        return new RssItem(Table.getRowId(cursor), RssItemTable.getGUID(cursor), RssItemTable.getTitle(cursor),
                RssItemTable.getDescription(cursor), RssItemTable.getLink(cursor),
                RssItemTable.getEnclosure(cursor), RssItemTable.getRssFeedId(cursor),
                RssItemTable.getPubDate(cursor), false, RssItemTable.getFavorite(cursor),
                RssItemTable.getArchived(cursor));
    }

    void submitTask(Runnable task) {
        if (mExecutorService.isShutdown() || mExecutorService.isTerminated()) {
            mExecutorService = Executors.newSingleThreadExecutor();
        }
        mExecutorService.submit(task);
    }
}
