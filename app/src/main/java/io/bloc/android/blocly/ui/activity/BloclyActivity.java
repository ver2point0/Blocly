package io.bloc.android.blocly.ui.activity;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.bloc.android.blocly.BloclyApplication;
import io.bloc.android.blocly.R;
import io.bloc.android.blocly.api.DataSource;
import io.bloc.android.blocly.api.model.RssFeed;
import io.bloc.android.blocly.api.model.RssItem;
import io.bloc.android.blocly.ui.adapter.ItemAdapter;
import io.bloc.android.blocly.ui.adapter.NavigationDrawerAdapter;

public class BloclyActivity extends ActionBarActivity
        implements
            NavigationDrawerAdapter.NavigationDrawerAdapterDelegate,
            ItemAdapter.DataSource,
            ItemAdapter.Delegate, NavigationDrawerAdapter.NavigationDrawerAdapterDataSource {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private ItemAdapter mItemAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private NavigationDrawerAdapter mNavigationDrawerAdapter;
    private Menu mMenu;
    private View mOverFlowButton;
    private List<RssFeed> mAllFeeds = new ArrayList<RssFeed>();
    private List<RssItem> mCurrentItems = new ArrayList<RssItem>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocly);

        Toolbar toolbar = (Toolbar) findViewById(R.id.tb_activity_blocly);
        setSupportActionBar(toolbar);

        mItemAdapter = new ItemAdapter();
        mItemAdapter.setDataSource(this);
        mItemAdapter.setDelegate(this);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.srl_activity_blocly);
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.primary));
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                BloclyApplication.getSharedDataSource().fetchNewFeed("http://feeds.feedburner.com/androidcentral?format=xml",
                        new DataSource.Callback<RssFeed>() {
                            @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
                            @Override
                            public void onSuccess(RssFeed rssFeed) {
                                // #15a
                                if (isFinishing() || isDestroyed()) {
                                    return;
                                }
                                mAllFeeds.add(rssFeed);
                                mNavigationDrawerAdapter.notifyDataSetChanged();
                                BloclyApplication.getSharedDataSource().fetchItemsForFeed(rssFeed,
                                        new DataSource.Callback<List<RssItem>>() {
                                            @Override
                                            public void onSuccess(List<RssItem> rssItems) {
                                                if (isFinishing() || isDestroyed()) {
                                                    return;
                                                }
                                                mCurrentItems.addAll(rssItems);
                                                mItemAdapter.notifyItemRangeInserted(0, mCurrentItems.size());
                                                mSwipeRefreshLayout.setRefreshing(false);
                                            }

                                            @Override
                                            public void onError(String errorMessage) {
                                                mSwipeRefreshLayout.setRefreshing(false);
                                            }
                                        });
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Toast.makeText(BloclyActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                mSwipeRefreshLayout.setRefreshing(false);
                            }
                        });
            }
        });



        mRecyclerView = (RecyclerView) findViewById(R.id.rv_activity_blocly);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mItemAdapter);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.dl_activity_blocly);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, 0, 0) {

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (mOverFlowButton != null) {
                    mOverFlowButton.setAlpha(1f);
                    mOverFlowButton.setEnabled(true);
                }
                if (mMenu == null) {
                    return;
                }
                for (int i = 0; i < mMenu.size(); i++) {
                    MenuItem item = mMenu.getItem(i);
                    if (item.getItemId() == R.id.action_share
                            && mItemAdapter.getExpandedItem() == null) {
                        continue;
                    }
                    item.setEnabled(true);
                    Drawable icon = item.getIcon();
                    if (icon != null) {
                        icon.setAlpha(255);
                    }
                }
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (mOverFlowButton != null) {
                    mOverFlowButton.setEnabled(false);
                }
                if (mMenu == null) {
                    return;
                }
                for (int i = 0; i < mMenu.size(); i++ ){
                    mMenu.getItem(i).setEnabled(false);
                }
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffSet) {
                super.onDrawerSlide(drawerView, slideOffSet);
                if (mOverFlowButton == null) {
                    ArrayList<View> foundViews = new ArrayList<View>();
                    getWindow().getDecorView().findViewsWithText(foundViews,
                            getString(R.string.abc_action_menu_overflow_description),
                            View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
                    if (foundViews.size() > 0) {
                        mOverFlowButton = foundViews.get(0);
                    }
                }

                if (mOverFlowButton != null) {
                    mOverFlowButton.setAlpha(1f - slideOffSet);
                }
                if (mMenu == null) {
                    return;
                }
                for (int i = 0; i   < mMenu.size(); i++) {
                    MenuItem item = mMenu.getItem(i);
                    if (item.getItemId() == R.id.action_share
                            && mItemAdapter.getExpandedItem() == null) {
                        continue;
                    }
                    Drawable icon = item.getIcon();
                    if (icon != null) {
                        icon.setAlpha((int) ((1f - slideOffSet) * 255));
                    }
                }
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);

        mNavigationDrawerAdapter = new NavigationDrawerAdapter();
        mNavigationDrawerAdapter.setDelegate(this);
        mNavigationDrawerAdapter.setDataSource(this);
        RecyclerView navigationRecyclerView = (RecyclerView) findViewById(R.id.rv_nav_activity_blocly);
        navigationRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        navigationRecyclerView.setItemAnimator(new DefaultItemAnimator());
        navigationRecyclerView.setAdapter(mNavigationDrawerAdapter);
    }

    public Cursor queryDatabase() {
        SQLiteDatabase database = openOrCreateDatabase("rss_items", MODE_PRIVATE, null);
        return database.query(false, "rss_items", null, null, null, null, null, "pub_date", "10");
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        if (item.getItemId() == R.id.action_share) {
            RssItem itemToShare = mItemAdapter.getExpandedItem();
            if (itemToShare == null) {
                return false;
            }
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    String.format("%s (%s)", itemToShare.getTitle(), itemToShare.getUrl()));
            shareIntent.setType("text/plain");
            Intent chooser = Intent.createChooser(shareIntent, getString(R.string.share_choose_title));
            startActivity(chooser);
        } else {
            Toast.makeText(this, item.getTitle(), Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.blocly, menu);
        mMenu = menu;
        animateShareItem(mItemAdapter.getExpandedItem() != null);
        return super.onCreateOptionsMenu(menu);
    }

    /*
     * NavigationDrawerAdapterDelegate
     */

    @Override
    public void didSelectNavigationOption(NavigationDrawerAdapter adapter, NavigationDrawerAdapter.NavigationOption navigationOption) {
        mDrawerLayout.closeDrawers();
        Toast.makeText(this, "Show the " + navigationOption.name(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void didSelectFeed(NavigationDrawerAdapter adapter, RssFeed rssFeed) {
        mDrawerLayout.closeDrawers();
        Toast.makeText(this, "Show RSS items from " + rssFeed.getTitle(), Toast.LENGTH_SHORT).show();
    }

    /*
     * NavigationDrawerAdapterDataSource
     */

    @Override
    public List<RssFeed> getFeeds(NavigationDrawerAdapter adapter) {
        return mAllFeeds;
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
        RssItem rssItem = mCurrentItems.get(position);
        for (RssFeed feed : mAllFeeds) {
            if (rssItem.getRssFeedId() == feed.getRowId()) {
                return feed;
            }
        }
        return null;
    }

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
            animateShareItem(true);
        } else {
            animateShareItem(false);
            return;
        }

        int lessToScroll = 0;
        if (positionToContract > -1 && positionToContract < positionToExpand) {
            lessToScroll = itemAdapter.getExpandedItemHeight() - itemAdapter.getCollapsedItemHeight();
        }

        View viewToExpand = mRecyclerView.getLayoutManager().findViewByPosition(positionToExpand);
        mRecyclerView.smoothScrollBy(0, viewToExpand.getTop() - lessToScroll);
    }

    @Override
    public void onVisitClicked(ItemAdapter itemAdapter, RssItem rssItem){
        Intent visitIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(rssItem.getUrl()));
        startActivity(visitIntent);
    }

    /*
     * Private methods
     */

    private void animateShareItem(final boolean enabled) {
        MenuItem shareItem = mMenu.findItem(R.id.action_share);
        if (shareItem.isEnabled() == enabled) {
            return;
        }
        shareItem.setEnabled(enabled);
        final Drawable shareIcon = shareItem.getIcon();
        ValueAnimator valueAnimator = ValueAnimator.ofInt(enabled ? new int[]{0, 255} : new int[]{255, 0});
        valueAnimator.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
        valueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                shareIcon.setAlpha((Integer) animation.getAnimatedValue());
            }
        });
        valueAnimator.start();
    }
}
