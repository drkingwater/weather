package com.news.pxq.weather.listenner;

/**
 * Created by pxq on 2016/7/2.
 */
public interface HttpCallbackListener  {

    void onFinish(String response);

    void onError(Exception e);
}
