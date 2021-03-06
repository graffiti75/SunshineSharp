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
package com.example.android.sunshine.app.fragment;

import android.app.Activity;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.TextView;

import com.example.android.sunshine.app.ForecastAdapter;
import com.example.android.sunshine.app.R;
import com.example.android.sunshine.app.Utility;
import com.example.android.sunshine.app.data.WeatherContract;

/**
 * Encapsulates fetching the forecast and displaying it as a {@link android.support.v7.widget.RecyclerView} layout.
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    //--------------------------------------------------
    // Constants
    //--------------------------------------------------

    public static final String LOG_TAG = "Sunshine";

    private static final int FORECAST_LOADER = 0;

    // For the forecast view we're showing only a small subset of the stored data.
    // Specify the columns we need.
    private static final String[] FORECAST_COLUMNS = {
        // In this case the id needs to be fully qualified with a table name, since the content
        // provider joins the location & weather tables in the background (both have an _id column).
        // On the one hand, that's annoying.  On the other, you can search the weather table using
        // the location set by the user, which is only in the Location table.
        // So the convenience is worth it.
        WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
        WeatherContract.WeatherEntry.COLUMN_DATE,
        WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
        WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
        WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
        WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
        WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
        WeatherContract.LocationEntry.COLUMN_COORD_LAT,
        WeatherContract.LocationEntry.COLUMN_COORD_LONG
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    public static final int COL_WEATHER_ID = 0;
    public static final int COL_WEATHER_DATE = 1;
    public static final int COL_WEATHER_DESC = 2;
    public static final int COL_WEATHER_MAX_TEMP = 3;
    public static final int COL_WEATHER_MIN_TEMP = 4;
    public static final int COL_LOCATION_SETTING = 5;
    public static final int COL_WEATHER_CONDITION_ID = 6;
    public static final int COL_COORD_LAT = 7;
    public static final int COL_COORD_LONG = 8;

    //--------------------------------------------------
    // Attributes
    //--------------------------------------------------

    private ForecastAdapter mForecastAdapter;
    private RecyclerView mRecyclerView;
    private boolean mAutoSelectView;
    private int mChoiceMode;
    private boolean mHoldForTransition;
    private long mInitialSelectedDate = -1;

    //--------------------------------------------------
    // Constructor
    //--------------------------------------------------

    public ForecastFragment() {}

    //--------------------------------------------------
    // Fragment Life Cycle
    //--------------------------------------------------

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(LOG_TAG, "ForecastFragment.onCreate().");
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(activity, attrs, savedInstanceState);
        Log.i(LOG_TAG, "ForecastFragment.onInflate().");
        TypedArray a = activity.obtainStyledAttributes(attrs, R.styleable.ForecastFragment, 0, 0);
        mChoiceMode = a.getInt(R.styleable.ForecastFragment_android_choiceMode, AbsListView.CHOICE_MODE_NONE);
        mAutoSelectView = a.getBoolean(R.styleable.ForecastFragment_autoSelectView, false);
        mHoldForTransition = a.getBoolean(R.styleable.ForecastFragment_sharedElementTransitions, false);
        a.recycle();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(LOG_TAG, "ForecastFragment.onCreateView().");
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Get a reference to the RecyclerView, and attach this adapter to it.
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerview_forecast);

        // Set the layout manager.
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        View emptyView = rootView.findViewById(R.id.recyclerview_forecast_empty);

        // Use this setting to improve performance if you know that changes in content do not change
        // the layout size of the RecyclerView.
        mRecyclerView.setHasFixedSize(true);

        // The ForecastAdapter will take data from a source and
        // use it to populate the RecyclerView it's attached to.
        mForecastAdapter = new ForecastAdapter(getActivity(),
            new ForecastAdapter.ForecastAdapterOnClickHandler() {
            @Override
            public void onClick(Long date, ForecastAdapter.ForecastAdapterViewHolder vh) {}
        }, emptyView, mChoiceMode);

        // Specify an adapter (see also next example).
        mRecyclerView.setAdapter(mForecastAdapter);

        // If there's instance state, mine it for useful information.
        // The end-goal here is that the user never knows that turning their device sideways
        // does crazy lifecycle related things.  It should feel like some stuff stretched out,
        // or magically appeared to take advantage of room, but data or place in the app was never
        // actually *lost*.
        if (savedInstanceState != null) {
            mForecastAdapter.onRestoreInstanceState(savedInstanceState);
        }
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(LOG_TAG, "ForecastFragment.onActivityCreated().");
        // We hold for transition here just in-case the activity needs to be re-created.
        // In a standard return transition, this doesn't actually make a difference.
        if (mHoldForTransition) {
            getActivity().supportPostponeEnterTransition();
        }
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.i(LOG_TAG, "ForecastFragment.onSaveInstanceState().");
        // When tablets rotate, the currently selected list item needs to be saved.
        mForecastAdapter.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "ForecastFragment.onDestroy().");
        super.onDestroy();
        if (null != mRecyclerView) {
            mRecyclerView.clearOnScrollListeners();
        }
    }

    //--------------------------------------------------
    // Methods
    //--------------------------------------------------

    // Since we read the location when we create the loader, all we need to do is restart things.
    public void onLocationChanged() {
        Log.i(LOG_TAG, "ForecastFragment.onLocationChanged().");
        getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
    }

    public void setInitialSelectedDate(long initialSelectedDate) {
        Log.i(LOG_TAG, "ForecastFragment.setInitialSelectedDate().");
        mInitialSelectedDate = initialSelectedDate;
    }

    // Updates the empty list view with contextually relevant information that the user can
    // use to determine why they aren't seeing weather.
    private void updateEmptyView() {
        Log.i(LOG_TAG, "ForecastFragment.updateEmptyView().");
        if (mForecastAdapter.getItemCount() == 0) {
            TextView tv = (TextView) getView().findViewById(R.id.recyclerview_forecast_empty);
            if (null != tv) {
                int message = 0;
                if (!Utility.isNetworkAvailable(getActivity())) {
                    message = R.string.empty_forecast_list_no_network;
                }
                tv.setText(message);
            }
        }
    }

    //--------------------------------------------------
    // LoaderManager LoaderCallbacks
    //--------------------------------------------------

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Log.i(LOG_TAG, "ForecastFragment.onCreateLoader().");
        // This is called when a new Loader needs to be created.  This
        // fragment only uses one loader, so we don't care about checking the id.
        // To only show current and future dates, filter the query to return weather only for
        // dates after or including today.
        // Sort order:  Ascending, by date.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";

        String locationSetting = Utility.getPreferredLocation(getActivity());
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
            locationSetting, System.currentTimeMillis());

        return new CursorLoader(getActivity(), weatherForLocationUri, FORECAST_COLUMNS, null,
            null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, final Cursor data) {
        Log.i(LOG_TAG, "ForecastFragment.onLoadFinished().");
        mForecastAdapter.swapCursor(data);
        updateEmptyView();
        if (data.getCount() == 0) {
            getActivity().supportStartPostponedEnterTransition();
        } else {
            mRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    // Since we know we're going to get items, we keep the listener around until
                    // we see Children.
                    if (mRecyclerView.getChildCount() > 0) {
                        mRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                        int position = mForecastAdapter.getSelectedItemPosition();
                        if (position == RecyclerView.NO_POSITION && -1 != mInitialSelectedDate) {
                            Cursor data = mForecastAdapter.getCursor();
                            int count = data.getCount();
                            int dateColumn = data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATE);
                            for (int i = 0; i < count; i++) {
                                data.moveToPosition(i);
                                if (data.getLong(dateColumn) == mInitialSelectedDate) {
                                    position = i;
                                    break;
                                }
                            }
                        }
                        if (position == RecyclerView.NO_POSITION) position = 0;
                        // If we don't need to restart the loader, and there's a desired position to restore
                        // to, do so now.
                        mRecyclerView.smoothScrollToPosition(position);
                        RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(position);
                        if (null != vh && mAutoSelectView) {
                            mForecastAdapter.selectView(vh);
                        }
                        if (mHoldForTransition) {
                            getActivity().supportStartPostponedEnterTransition();
                        }
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.i(LOG_TAG, "ForecastFragment.onLoaderReset().");
        mForecastAdapter.swapCursor(null);
    }
}