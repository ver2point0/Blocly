package io.bloc.android.blocly.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import io.bloc.android.blocly.BloclyApplication;
import io.bloc.android.blocly.R;
import io.bloc.android.blocly.api.model.RssFeed;

public class NavigationDrawerAdapter extends RecyclerView.Adapter<NavigationDrawerAdapter.ViewHolder> {

    public enum NavigationOption {
        NAVIGATION_OPTION_INBOX,
        NAVIGATION_OPTION_FAVORITES,
        NAVIGATION_OPTION_ARCHIVED
    }

    public static interface NavigationDrawerAdapterDelegate {
        public void didSelectNavigationOption(NavigationDrawerAdapter adapter, NavigationOption navigationOption);
        public void didSelectFeed(NavigationDrawerAdapter adapter, RssFeed rssFeed);
    }

    WeakReference<NavigationDrawerAdapterDelegate> delegate;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
        View inflate = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.navigation_item, viewGroup, false);
        return new ViewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        RssFeed rssFeed = null;
        if (position >= NavigationOption.values().length) {
            int feedPosition = position - NavigationOption.values().length;
            rssFeed = BloclyApplication.getSharedDataSource().getFeeds().get(feedPosition);
        }
        viewHolder.update(position, rssFeed);
    }

    @Override
    public int getItemCount() {
        return NavigationOption.values().length
        + BloclyApplication.getSharedDataSource().getFeeds().size();
    }

    public NavigationDrawerAdapterDelegate getDelegate() {
        if (delegate == null) {
            return null;
        }
        return delegate.get();
    }

    public void setDelegate(NavigationDrawerAdapterDelegate delegate) {
        this.delegate = new WeakReference<NavigationDrawerAdapterDelegate>(delegate);
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        View mTopPadding;
        TextView mTitle;
        View mBottomPadding;
        View mDivider;
        int mPosition;
        RssFeed mRssFeed;

        public ViewHolder(View itemView) {
            super(itemView);
            mTopPadding = itemView.findViewById(R.id.v_nav_item_top_padding);
            mTitle = (TextView) itemView.findViewById(R.id.tv_nav_item_title);
            mBottomPadding = itemView.findViewById(R.id.v_nav_item_bottom_padding);
            mDivider = itemView.findViewById(R.id.v_nav_item_divider);
            itemView.setOnClickListener(this);
        }

        void update(int position, RssFeed rssFeed) {
            mPosition = position;
            mRssFeed = rssFeed;
            boolean shouldShowTopPadding = position == NavigationOption.NAVIGATION_OPTION_INBOX.ordinal()
                    || position == NavigationOption.values().length;
            mTopPadding.setVisibility(shouldShowTopPadding ? View.VISIBLE : View.GONE);

            boolean shouldShowBottomPadding = position == NavigationOption.NAVIGATION_OPTION_ARCHIVED.ordinal()
                    || position == NavigationOption.values().length;
            mBottomPadding.setVisibility(shouldShowBottomPadding ? View.VISIBLE : View.GONE);

            mDivider.setVisibility(position == NavigationOption.NAVIGATION_OPTION_ARCHIVED.ordinal()
                    ? View.VISIBLE : View.GONE);

            if (position < NavigationOption.values().length) {
                int[] titleTexts = new int[] {R.string.navigation_option_inbox,
                    R.string.navigation_option_favorites,
                    R.string.navigation_option_archived};
                mTitle.setText(titleTexts[position]);
            } else {
                mTitle.setText(rssFeed.getTitle());
            }
        }

        /*
         * OnClickListener
         */

        @Override
        public void onClick(View v) {
            if (getDelegate() == null) {
                return;
            }
            if (mPosition < NavigationOption.values().length) {
                getDelegate().didSelectNavigationOption(NavigationDrawerAdapter.this,
                        NavigationOption.values()[mPosition]);
            } else {
                getDelegate().didSelectFeed(NavigationDrawerAdapter.this, mRssFeed);
            }
        }
    }
}
