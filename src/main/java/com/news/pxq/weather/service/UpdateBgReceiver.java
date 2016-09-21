package com.news.pxq.weather.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by pxq on 2016/8/7.
 */
public class UpdateBgReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("UpdateBgReceiver", "onReceive");
        Intent mService = new Intent(context, UpdateBgService.class);
        context.startService(mService);
    }
}
