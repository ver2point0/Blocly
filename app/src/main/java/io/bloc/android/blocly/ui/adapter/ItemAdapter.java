package io.bloc.android.blocly.ui.adapter;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Outline;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import io.bloc.android.blocly.R;
import io.bloc.android.blocly.api.model.RssFeed;
import io.bloc.android.blocly.api.model.RssItem;
import io.bloc.android.blocly.ui.UIUtils;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemAdapterViewHolder> {

    public static interface DataSource {
        public RssItem getRssItem(ItemAdapter itemAdapter, int position);
        public RssFeed getRssFeed(ItemAdapter itemAdapter, int position);
        public int getItemCount(ItemAdapter itemAdapter);
    }

    public static interface Delegate {
        public void onItemClicked(ItemAdapter itemAdapter, RssItem rssItem);
        public void onVisitClicked(ItemAdapter itemAdapter, RssItem rssItem);
    }

    private static String TAG = ItemAdapter.class.getSimpleName();

    private Map<Long, Integer> rssFeedToColor = new HashMap<Long, Integer>();

    private RssItem expandedItem = null;
    private WeakReference<Delegate> delegate;
    private WeakReference<DataSource> dataSource;
    private int collapsedItemHeight;
    private int expandedItemHeight;

    @Override
    public ItemAdapterViewHolder onCreateViewHolder(ViewGroup viewGroup, int index) {
        View inflate = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.rss_item, viewGroup, false);
        return new ItemAdapterViewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(ItemAdapterViewHolder itemAdapterViewHolder, int index) {
       if (getDataSource() == null) {
           return;
       }

        RssItem rssItem = getDataSource().getRssItem(this, index);
        RssFeed rssFeed = getDataSource().getRssFeed(this, index);
        itemAdapterViewHolder.update(rssFeed, rssItem);
    }

    @Override
    public int getItemCount() {
        if (getDataSource() == null) {
            return 0;
        }
        return getDataSource().getItemCount(this);
    }

    public DataSource getDataSource() {
        if (dataSource == null) {
            return null;
        }
        return dataSource.get();
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = new WeakReference<DataSource>(dataSource);
    }

    public Delegate getDelegate() {
        if (delegate == null) {
            return null;
        }
        return delegate.get();
    }

    public void setDelegate(Delegate delegate){
        this.delegate = new WeakReference<Delegate>(delegate);
    }

    public RssItem getExpandedItem() {
        return expandedItem;
    }

    public void setExpandedItem(RssItem expandedItem) {
        this.expandedItem = expandedItem;
    }

    public int getCollapsedItemHeight() {
        return collapsedItemHeight;
    }

    private void setCollapsedItemHeight(int collapsedItemHeight){
        this.collapsedItemHeight = collapsedItemHeight;
    }

    public int getExpandedItemHeight() {
        return expandedItemHeight;
    }

    private void setExpandedItemHeight(int expandedItemHeight) {
        this.expandedItemHeight = expandedItemHeight;
    }


    class ItemAdapterViewHolder extends RecyclerView.ViewHolder implements ImageLoadingListener, View.OnClickListener, CompoundButton.OnCheckedChangeListener {
        boolean mOnTablet;
        boolean bContentExpanded;
        TextView mTitle;
        TextView mContent;
        // Phone Only
        TextView mFeed;
        View mHeadWrapper;
        ImageView mHeaderImage;
        CheckBox mArchiveCheckBox;
        CheckBox mFavoriteCheckBox;
        View mExpandedContentWrapper;
        TextView mExpandedContent;
        TextView mVisitSite;
        // Tablet Only
        TextView mCallOut;
        RssItem mRssItem;

        public ItemAdapterViewHolder(View itemView) {
            super(itemView);
            mTitle = (TextView) itemView.findViewById(R.id.tv_rss_item_title);
            mContent = (TextView) itemView.findViewById(R.id.tv_rss_item_content);

            // Attempt to recover phone Views
            if (itemView.findViewById(R.id.tv_rss_item_feed_title) != null) {
                mFeed = (TextView) itemView.findViewById(R.id.tv_rss_item_feed_title);
                mHeadWrapper = itemView.findViewById(R.id.fl_rss_item_image_header);
                mHeaderImage = (ImageView) mHeadWrapper.findViewById(R.id.iv_rss_item_image);
                mArchiveCheckBox = (CheckBox) itemView.findViewById(R.id.cb_rss_item_check_mark);
                mFavoriteCheckBox = (CheckBox) itemView.findViewById(R.id.cb_rss_item_favorite_star);
                mExpandedContentWrapper = itemView.findViewById(R.id.ll_rss_item_expanded_content_wrapper);
                mExpandedContent = (TextView) mExpandedContentWrapper.findViewById(R.id.tv_rss_item_content_full);
                mVisitSite = (TextView) mExpandedContentWrapper.findViewById(R.id.tv_rss_item_visit_site);
                mVisitSite.setOnClickListener(this);
                mArchiveCheckBox.setOnCheckedChangeListener(this);
                mFavoriteCheckBox.setOnCheckedChangeListener(this);
            } else {
                // Recover Tablet Views
                mOnTablet = true;
                mCallOut = (TextView) itemView.findViewById(R.id.tv_rss_item_callout);
                if (Build.VERSION.SDK_INT >= 21) {
                    mCallOut.setOutlineProvider((new ViewOutlineProvider() {
                        @TargetApi(21)
                        @Override
                        public void getOutline(View view, Outline outline) {
                            outline.setOval(0, 0, view.getWidth(), view.getHeight());
                        }
                    }));
                    mCallOut.setClipToOutline(true);
                }
            }
            itemView.setOnClickListener(this);
        }

        void update(RssFeed rssFeed, RssItem rssItem) {
            mRssItem = rssItem;
            mTitle.setText(rssItem.getTitle());
            mContent.setText(rssItem.getDescription());
            if (mOnTablet) {
                mCallOut.setText("" + Character.toUpperCase(rssFeed.getTitle().charAt(0)));
                Integer color = rssFeedToColor.get(rssFeed.getRowId());
                if (color == null) {
                    color = UIUtils.generateRandomColor(itemView.getResources().getColor(android.R.color.white));
                    rssFeedToColor.put(rssFeed.getRowId(), color);
                }
                mCallOut.setBackgroundColor(color);
                return;
            }
            mFeed.setText(rssFeed.getTitle());
            mExpandedContent.setText(rssItem.getDescription());
            if (rssItem.getImageUrl() != null) {
                mHeadWrapper.setVisibility(View.VISIBLE);
                mHeaderImage.setVisibility(View.VISIBLE);
                ImageLoader.getInstance().loadImage(rssItem.getImageUrl(), this);
            } else {
                mHeadWrapper.setVisibility(View.GONE);
            }
            animateContent(getExpandedItem() == rssItem);
        }
        /*
         * ImageLoadingListener
         */
        @Override
        public void onLoadingStarted(String imageUri, View view) {}

        @Override
        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
            Log.e(TAG, "onLoadingFailed: " + failReason.toString() + " for URL: " + imageUri);
        }

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            if (imageUri.equals(mRssItem.getImageUrl()   )) {
                mHeaderImage.setImageBitmap(loadedImage);
                mHeaderImage.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onLoadingCancelled(String imageUri, View view) {
            // Attempt a retry
            ImageLoader.getInstance().loadImage(imageUri, this);
        }

        /*
         * OnClickListener
         */

        @Override
        public void onClick(View view) {
           if (view == itemView) {
               if (getDelegate() != null) {
                   getDelegate().onItemClicked(ItemAdapter.this, mRssItem);
               }
           } else {
               if (getDelegate() != null) {
                   getDelegate().onVisitClicked(ItemAdapter.this, mRssItem);
               }
           }
        }

        /*
         * OnCheckedChangedListener
         */

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Log.v(TAG, "Checked changed to: " + isChecked);
        }

        /*
          * Private Methods
          */

        private void animateContent(final boolean expand) {
            if ((expand && bContentExpanded) || (!expand && !bContentExpanded)) {
                return;
            }

            int startingHeight = mExpandedContentWrapper.getMeasuredHeight();
            int finalHeight = mContent.getMeasuredHeight();
            if (expand) {
                setCollapsedItemHeight(itemView.getHeight());
                startingHeight = finalHeight;
                mExpandedContentWrapper.setAlpha(0f);
                mExpandedContentWrapper.setVisibility(View.VISIBLE);
                mExpandedContentWrapper.measure(
                        View.MeasureSpec.makeMeasureSpec(mContent.getWidth(), View.MeasureSpec.EXACTLY),
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                finalHeight = mExpandedContentWrapper.getMeasuredHeight();
            } else {
                mContent.setVisibility(View.VISIBLE);
            }
            startAnimator(startingHeight, finalHeight, new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float animatedFraction = valueAnimator.getAnimatedFraction();
                    float wrapperAlpha = expand ? animatedFraction : 1f - animatedFraction;
                    float contentAlpha = 1f - wrapperAlpha;

                    mExpandedContentWrapper.setAlpha(wrapperAlpha);
                    mContent.setAlpha(contentAlpha);
                    mExpandedContentWrapper.getLayoutParams().height = animatedFraction == 1f ?
                            ViewGroup.LayoutParams.WRAP_CONTENT :
                            (Integer) valueAnimator.getAnimatedValue();
                    mExpandedContentWrapper.requestLayout();
                    if (animatedFraction == 1f) {
                        if (expand) {
                            mContent.setVisibility(View.GONE);
                            setExpandedItemHeight(itemView.getHeight());
                        } else {
                            mExpandedContentWrapper.setVisibility(View.GONE);
                        }
                    }
                }
            });
            bContentExpanded = expand;
        } // end animateContent() method

        private void startAnimator(int start, int end, ValueAnimator.AnimatorUpdateListener animatorUpdateListener){
            ValueAnimator valueAnimator = ValueAnimator.ofInt(start, end);
            valueAnimator.addUpdateListener(animatorUpdateListener);
            valueAnimator.setDuration(itemView.getResources().getInteger(android.R.integer.config_mediumAnimTime));
            valueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            valueAnimator.start();
        }

    } // end inner class ItemAdapterViewHolder

} // end class ItemAdapter
