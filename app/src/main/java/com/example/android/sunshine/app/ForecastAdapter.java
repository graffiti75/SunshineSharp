/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.sunshine.app;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.fragment.ForecastFragment;

/**
 * {@link ForecastAdapter} exposes a list of weather forecasts
 * from a {@link android.database.Cursor} to a {@link android.support.v7.widget.RecyclerView}.
 */
public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ForecastAdapterViewHolder> {

    //--------------------------------------------------
    // Constants
    //--------------------------------------------------

    public static final String LOG_TAG = "Sunshine";

    //--------------------------------------------------
    // Attributes
    //--------------------------------------------------

    private Cursor mCursor;
    final private Context mContext;
    final private ForecastAdapterOnClickHandler mClickHandler;
    final private View mEmptyView;
    final private ItemChoiceManager mICM;

    //--------------------------------------------------
    // View Holder
    //--------------------------------------------------

    /**
     * Cache of the children views for a forecast list item.
     */
    public class ForecastAdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public final ImageView mIconView;
        public final TextView mHighTempView;
        public final TextView mLowTempView;

        public ForecastAdapterViewHolder(View view) {
            super(view);
            Log.i(LOG_TAG, "ForecastAdapter.ForecastAdapterViewHolder().");
            mIconView = (ImageView) view.findViewById(R.id.list_item_icon);
            mHighTempView = (TextView) view.findViewById(R.id.list_item_high_textview);
            mLowTempView = (TextView) view.findViewById(R.id.list_item_low_textview);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Log.i(LOG_TAG, "ForecastAdapter.ForecastAdapterViewHolder().onClick().");
            int adapterPosition = getAdapterPosition();
            mCursor.moveToPosition(adapterPosition);
            int dateColumnIndex = mCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATE);
            mClickHandler.onClick(mCursor.getLong(dateColumnIndex), this);
            mICM.onClick(this);
        }
    }

    //--------------------------------------------------
    // Interface
    //--------------------------------------------------

    public interface ForecastAdapterOnClickHandler {
        void onClick(Long date, ForecastAdapterViewHolder vh);
    }

    //--------------------------------------------------
    // Constructor
    //--------------------------------------------------

    public ForecastAdapter(Context context, ForecastAdapterOnClickHandler dh, View emptyView, int choiceMode) {
        Log.i(LOG_TAG, "ForecastAdapter.ForecastAdapter().");
        mContext = context;
        mClickHandler = dh;
        mEmptyView = emptyView;
        mICM = new ItemChoiceManager(this);
        mICM.setChoiceMode(choiceMode);
    }

    //--------------------------------------------------
    // Adapter
    //--------------------------------------------------

    // This takes advantage of the fact that the viewGroup passed to onCreateViewHolder is the
    // RecyclerView that will be used to contain the view, so that it can get the current
    // ItemSelectionManager from the view.
    // One could implement this pattern without modifying RecyclerView by taking advantage
    // of the view tag to store the ItemChoiceManager.
    @Override
    public ForecastAdapterViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        Log.i(LOG_TAG, "ForecastAdapter.onCreateViewHolder().");
        if (viewGroup instanceof RecyclerView) {
            int layoutId = R.layout.list_item_forecast_today;
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(layoutId, viewGroup, false);
            view.setFocusable(true);
            return new ForecastAdapterViewHolder(view);
        } else {
            throw new RuntimeException("Not bound to RecyclerView");
        }
    }

    @Override
    public void onBindViewHolder(ForecastAdapterViewHolder forecastAdapterViewHolder, int position) {
        Log.i(LOG_TAG, "ForecastAdapter.onBindViewHolder().");
        mCursor.moveToPosition(position);
        int weatherId = mCursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);
        int defaultImage = Utility.getIconResourceForWeatherCondition(weatherId);
        if (Utility.usingLocalGraphics(mContext)) {
            forecastAdapterViewHolder.mIconView.setImageResource(defaultImage);
        } else {
            Glide.with(mContext)
                .load(Utility.getArtUrlForWeatherCondition(mContext, weatherId))
                .error(defaultImage)
                .crossFade()
                .into(forecastAdapterViewHolder.mIconView);
        }

        // This enables better animations. even if we lose state due to a device rotation,
        // the animator can use this to re-find the original view.
        ViewCompat.setTransitionName(forecastAdapterViewHolder.mIconView, "iconView" + position);

        // For accessibility, we don't want a content description for the icon field because the
        // information is repeated in the description view and the icon is not individually selectable.

        // Read high temperature from cursor.
        double high = mCursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP);
        String highString = Utility.formatTemperature(mContext, high);
        forecastAdapterViewHolder.mHighTempView.setText(highString);
        forecastAdapterViewHolder.mHighTempView.setContentDescription(mContext.getString(R.string.a11y_high_temp, highString));

        // Read low temperature from cursor.
        double low = mCursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP);
        String lowString = Utility.formatTemperature(mContext, low);
        forecastAdapterViewHolder.mLowTempView.setText(lowString);
        forecastAdapterViewHolder.mLowTempView.setContentDescription(mContext.getString(R.string.a11y_low_temp, lowString));

        mICM.onBindViewHolder(forecastAdapterViewHolder, position);
    }

    @Override
    public int getItemCount() {
        if (null == mCursor) return 0;
        return 1;
    }

    //--------------------------------------------------
    // Methods
    //--------------------------------------------------

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.i(LOG_TAG, "ForecastAdapter.onRestoreInstanceState().");
        mICM.onRestoreInstanceState(savedInstanceState);
    }

    public void onSaveInstanceState(Bundle outState) {
        Log.i(LOG_TAG, "ForecastAdapter.onSaveInstanceState().");
        mICM.onSaveInstanceState(outState);
    }

    public int getSelectedItemPosition() {
        Log.i(LOG_TAG, "ForecastAdapter.getSelectedItemPosition().");
        return mICM.getSelectedItemPosition();
    }

    public void swapCursor(Cursor newCursor) {
        Log.i(LOG_TAG, "ForecastAdapter.swapCursor().");
        mCursor = newCursor;
        notifyDataSetChanged();
        mEmptyView.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    public Cursor getCursor() {
        Log.i(LOG_TAG, "ForecastAdapter.getCursor().");
        return mCursor;
    }

    public void selectView(RecyclerView.ViewHolder viewHolder) {
        Log.i(LOG_TAG, "ForecastAdapter.selectView().");
        if (viewHolder instanceof ForecastAdapterViewHolder) {
            ForecastAdapterViewHolder vfh = (ForecastAdapterViewHolder)viewHolder;
            vfh.onClick(vfh.itemView);
        }
    }
}