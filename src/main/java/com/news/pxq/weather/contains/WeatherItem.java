package com.news.pxq.weather.contains;

import com.news.pxq.weather.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by pxq on 2016/7/4.
 */
public class WeatherItem {

   // public static final Stirng

    public static Map<String, Object> weatherItem = new HashMap<String, Object>(){
        {
            put("中雨", R.drawable.m_rain);
            put("小雨", R.drawable.s_rain);
            put("大雨", R.drawable.l_rain);
            put("晴", R.drawable.sunny);
            put("阴", R.drawable.overcast);
            put("多云", R.drawable.cloudy);
            put("雷阵雨", R.drawable.light);
            put("阵雨", R.drawable.zhen);
            put("暴雨", R.drawable.strong);
            put("大暴雨", R.drawable.bigstrong);
            put("小到中雨", R.drawable.s_m);
            put("中到大雨", R.drawable.m_l);
            put("大到暴雨", R.drawable.l_st);
        }
    };
}
