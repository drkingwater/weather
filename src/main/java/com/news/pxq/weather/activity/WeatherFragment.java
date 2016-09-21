package com.news.pxq.weather.activity;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.news.pxq.weather.R;

/**
 * Created by pxq on 2016/7/19.
 */
public class WeatherFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.weather_fragment, container, false);
        return view;
    }
}
