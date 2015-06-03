package io.bloc.android.blocly.ui.adapter;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import io.bloc.android.blocly.BloclyApplication;
import io.bloc.android.blocly.R;
import io.bloc.android.blocly.api.DataSource;
import io.bloc.android.blocly.api.model.RssFeed;
import io.bloc.android.blocly.api.model.RssItem;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemAdapterViewHolder> {

    private static String TAG = ItemAdapter.class.getSimpleName();

    @Override
    public ItemAdapterViewHolder onCreateViewHolder(ViewGroup viewGroup, int index) {
        View inflate = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.rss_item, viewGroup, false);
        return new ItemAdapterViewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(ItemAdapterViewHolder itemAdapterViewHolder, int index) {
        DataSource sharedDataSource = BloclyApplication.getSharedDataSource();
        itemAdapterViewHolder.update(sharedDataSource.getFeeds().get(0), sharedDataSource.getItems().get(index));
    }

    @Override
    public int getItemCount() {
        return BloclyApplication.getSharedDataSource().getItems().size();
    }

    class ItemAdapterViewHolder extends RecyclerView.ViewHolder implements ImageLoadingListener, View.OnClickListener, CompoundButton.OnCheckedChangeListener {
        boolean bContentExpanded;
        TextView mTitle;
        TextView mFeed;
        TextView mContent;
        View mHeadWrapper;
        ImageView mHeaderImage;
        CheckBox mArchiveCheckBox;
        CheckBox mFavoriteCheckBox;
        View mExpandedContentWrapper;
        TextView mExpandedContent;
        TextView mVisitSite;
        RssItem mRssItem;

        public ItemAdapterViewHolder(View itemView) {
            super(itemView);
            mTitle = (TextView) itemView.findViewById(R.id.tv_rss_item_title);
            mFeed = (TextView) itemView.findViewById(R.id.tv_rss_item_feed_title);
            mContent = (TextView) itemView.findViewById(R.id.tv_rss_item_content);
            mHeadWrapper = itemView.findViewById(R.id.fl_rss_item_image_header);
            mHeaderImage = (ImageView) mHeadWrapper.findViewById(R.id.iv_rss_item_image);
            mArchiveCheckBox = (CheckBox) itemView.findViewById(R.id.cb_rss_item_check_mark);
            mFavoriteCheckBox = (CheckBox) itemView.findViewById(R.id.cb_rss_item_favorite_star);
            mExpandedContentWrapper = itemView.findViewById(R.id.ll_rss_item_expanded_content_wrapper);
            mExpandedContent = (TextView) mExpandedContentWrapper.findViewById(R.id.tv_rss_item_content_full);
            mVisitSite = (TextView) mExpandedContentWrapper.findViewById(R.id.tv_rss_item_visit_site);

            itemView.setOnClickListener(this);
            mVisitSite.setOnClickListener(this);
            mArchiveCheckBox.setOnCheckedChangeListener(this);
            mFavoriteCheckBox.setOnCheckedChangeListener(this);
        }

        void update(RssFeed rssFeed, RssItem rssItem) {
            mRssItem = rssItem;
            mFeed.setText(rssFeed.getTitle());
            mTitle.setText(rssItem.getTitle());
            mContent.setText(rssItem.getDescription());
            mExpandedContent.setText(rssItem.getDescription());
            if (rssItem.getImageUrl() != null) {
                mHeadWrapper.setVisibility(View.VISIBLE);
                mHeaderImage.setVisibility(View.VISIBLE);
                ImageLoader.getInstance().loadImage(rssItem.getImageUrl(), this);
            } else {
                mHeadWrapper.setVisibility(View.GONE);
            }
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
               animateContent(!bContentExpanded);
           } else {
               Toast.makeText(view.getContext(), "Visit " + mRssItem.getUrl(), Toast.LENGTH_SHORT).show();
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
