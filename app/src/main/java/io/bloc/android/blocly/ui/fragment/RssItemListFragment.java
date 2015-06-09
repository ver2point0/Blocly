package io.bloc.android.blocly.ui.fragment;


import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.bloc.android.blocly.BloclyApplication;
import io.bloc.android.blocly.R;
import io.bloc.android.blocly.api.DataSource;
import io.bloc.android.blocly.api.model.RssFeed;
import io.bloc.android.blocly.api.model.RssItem;
import io.bloc.android.blocly.ui.adapter.ItemAdapter;

public class RssItemListFragment extends Fragment implements ItemAdapter.DataSource, ItemAdapter.Delegate {

    private static final String BUNDLE_EXTRA_RSS_FEED = RssItemListFragment.class.getCanonicalName().concat(".EXTRA_RSS_FEED");

    public static RssItemListFragment fragmentForRssFeed(RssFeed rssFeed) {
        Bundle arguments = new Bundle();
        arguments.putLong(BUNDLE_EXTRA_RSS_FEED, rssFeed.getRowId());
        RssItemListFragment rssItemListFragment = new RssItemListFragment();
        rssItemListFragment.setArguments(arguments);
        return rssItemListFragment;
    }

    public static interface Delegate {
        public void onItemExpanded(RssItemListFragment rssItemListFragment, RssItem rssItem);
        public void onItemContracted(RssItemListFragment rssItemListFragment, RssItem rssItem);
        public void onItemVisitClicked(RssItemListFragment rssItemListFragment, RssItem rssItem);
    }

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private ItemAdapter mItemAdapter;
    private RssFeed mCurrentFeed;
    private List<RssItem> mCurrentItems = new ArrayList<RssItem>();
    private WeakReference<Delegate> mDelegate;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mDelegate = new WeakReference<Delegate>((Delegate) activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mItemAdapter = new ItemAdapter();
        mItemAdapter.setDataSource(this);
        mItemAdapter.setDelegate(this);

        Bundle arguments = getArguments();
        if (arguments == null) {
            return;
        }
        long feedRowId = arguments.getLong(BUNDLE_EXTRA_RSS_FEED);
        BloclyApplication.getSharedDataSource().fetchFeedWithId(feedRowId, new DataSource.Callback<RssFeed>() {
            @Override
            public void onSuccess(RssFeed rssFeed) {
                mCurrentFeed = rssFeed;
            }

            @Override
            public void onError(String errorMessage) {}
        });
    }






    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View inflate = inflater.inflate(R.layout.fragment_rss_list, container, false);
        mSwipeRefreshLayout = (SwipeRefreshLayout) inflate.findViewById(R.id.srl_fragment_rss_list);
        mRecyclerView = (RecyclerView) inflate.findViewById(R.id.rv_fragment_rss_list);
        return inflate;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.primary));
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // #7
                BloclyApplication.getSharedDataSource().fetchNewItemsForFeed(mCurrentFeed,
                        new DataSource.Callback<List<RssItem>>() {
                            @Override
                            public void onSuccess(List<RssItem> rssItems) {
                                if (getActivity() == null) {
                                    return;
                                }
                                if (!rssItems.isEmpty()) {
                                    mCurrentItems.addAll(0, rssItems);
                                    mItemAdapter.notifyItemRangeInserted(0, rssItems.size());
                                }
                                mSwipeRefreshLayout.setRefreshing(false);
                            }

                            @Override
                            public void onError(String errorMessage) {
                                mSwipeRefreshLayout.setRefreshing(false);
                            }
                        });
            }
        });

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mItemAdapter);
    }

    /*
     * ItemAdapter.DataSource
     */

    @Override
    public RssItem getRssItem(ItemAdapter itemAdapter, int position) {
        return mCurrentItems.get(position);
    }

    @Override
    public RssFeed getRssFeed(ItemAdapter itemAdapter, int position) {
        return mCurrentFeed;
    }

    @Override
    public int getItemCount(ItemAdapter itemAdapter) {
        return mCurrentItems.size();
    }

    /*
      * ItemAdapter.Delegate
      */

    @Override
    public void onItemClicked(ItemAdapter itemAdapter, RssItem rssItem) {
        int positionToExpand = -1;
        int positionToContract = -1;
        if (itemAdapter.getExpandedItem() != null) {
            positionToContract = mCurrentItems.indexOf(itemAdapter.getExpandedItem());
            View viewToContract = mRecyclerView.getLayoutManager().findViewByPosition(positionToContract);
            if (viewToContract == null) {
                positionToContract = -1;
            }
        }
        if (itemAdapter.getExpandedItem() != rssItem) {
            positionToExpand = mCurrentItems.indexOf(rssItem);
            itemAdapter.setExpandedItem(rssItem);
        } else {
            itemAdapter.setExpandedItem(null);
        }
        if (positionToContract > -1) {
            itemAdapter.notifyItemChanged(positionToContract);
        }
        if (positionToExpand > -1) {
            itemAdapter.notifyItemChanged(positionToExpand);
            mDelegate.get().onItemExpanded(this, itemAdapter.getExpandedItem());
        } else {
            mDelegate.get().onItemContracted(this, rssItem);
            return;
        }
        View viewToExpand = mRecyclerView.getLayoutManager().findViewByPosition(positionToExpand);
        int lessToScroll = 0;
        if (positionToContract > -1 && positionToContract < positionToExpand) {
            lessToScroll = itemAdapter.getExpandedItemHeight() - itemAdapter.getCollapsedItemHeight();
        }
        mRecyclerView.smoothScrollBy(0, viewToExpand.getTop() - lessToScroll);
    }

    @Override
    public void onVisitClicked(ItemAdapter itemAdapter, RssItem rssItem) {
        mDelegate.get().onItemVisitClicked(this, rssItem);
    }


}

