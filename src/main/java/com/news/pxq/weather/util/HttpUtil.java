package com.news.pxq.weather.util;


import com.news.pxq.weather.listenner.HttpCallbackListener;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by pxq on 2016/7/2.
 */
public class HttpUtil {

    public static void sendHttpRequest(final String address, final HttpCallbackListener listener){

        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(address);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);

                    InputStream inputStream = connection.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                    StringBuffer response = new StringBuffer();
                    String line = null;

                    while((line = bufferedReader.readLine()) != null){
                        response.append(line);
                    }

                    if (listener != null){
                        listener.onFinish(response.toString());      //回调
                    }

                }catch (Exception e){
                    e.printStackTrace();
                    if (listener != null){
                        listener.onError(e);
                    }

                }finally{
                    if (connection != null){
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }
}
