package com.news.pxq.weather.service;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;

import android.os.IBinder;

import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import com.news.pxq.weather.R;
import com.news.pxq.weather.activity.SettingsActivity;
import com.news.pxq.weather.activity.WeatherActivity;
import com.news.pxq.weather.db.WeatherDb;
import com.news.pxq.weather.listenner.HttpCallbackListener;
import com.news.pxq.weather.util.HttpUtil;
import com.news.pxq.weather.util.Utility;

/**
 * Created by pxq on 2016/8/4.
 */
public class UpdateBgService extends Service {

    private static final String TAG = "UpdateBgService";

    private NotificationManager mNM;

    private int NOTIFICATION = 123;

    private SharedPreferences mSp;

    private SharedPreferences mSettingsSp;


    private int frequency = 0;

    private final static long HOUR = 60 * 60 * 1000;

    private long updateTime = 0;

    private Notification notification;

    private int i = 0;


    private final NotificationBinder mBinder = new NotificationBinder();

    public class NotificationBinder extends Binder {

        public void showNotification() {
            showNotific();
        }

        public void cancelNotification() {
            cancelNotific();
        }

    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        mSp = PreferenceManager.getDefaultSharedPreferences(this);

        mSettingsSp = getSharedPreferences(SettingsActivity.SETTINGS_SP, MODE_APPEND);

        initNotification();

        //showNotific();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO 更新天气

        frequency = mSettingsSp.getInt("frequency", 1);
        UpdateWeather();

        AlarmManager mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        updateTime = frequency * HOUR + SystemClock.elapsedRealtime();

        Intent i = new Intent(this, UpdateBgReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, i, 0);
        mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, updateTime, pendingIntent);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");
        cancelNotific();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void UpdateWeather() {

        Log.e(TAG, Thread.currentThread().getId() + " Thread");

        String cityName = mSp.getString("city_name", "");
        String result = WeatherDb.getInstance(UpdateBgService.this).queryCountyCode(cityName);
        if (!TextUtils.isEmpty(result))
            updateWeather(result, "countyCode");

    }

    private void updateWeather(String code, final String type) {

        String address;
        if ("countyCode".equals(type)) {
            address = "http://www.weather.com.cn/data/list3/city" +
                    code + ".xml";
        } else
            address = "http://wthrcdn.etouch.cn/WeatherApi?citykey=" + code;

        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                Log.e(TAG, response);
                if ("countyCode".equals(type)) {
                    if (!TextUtils.isEmpty(response)) {
                        // 从服务器返回的数据中解析出天气代号
                        String[] array = response.split("\\|");
                        if (array != null && array.length == 2) {

                            String weatherCode = array[1];
                            updateWeather(weatherCode, "weatherCode");
                        }
                    }
                } else
                    Utility.handleWeatherResponse(UpdateBgService.this, response);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void initNotification() {

        Intent intent = new Intent(UpdateBgService.this, WeatherActivity.class);
        //intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
       // PendingIntent contentIntent = PendingIntent.getActivity(UpdateBgService.this, 0, intent, 0);

        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification_content);
        contentView.setImageViewResource(R.id.title_image, R.drawable.weather);
        contentView.setTextViewText(R.id.city, mSp.getString("city_name", "xxxx"));
        contentView.setTextViewText(R.id.temp_notification, mSp.getString("temp", "yyy") + "℃");

        notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.weather)
                
                .setContentTitle(mSp.getString("city_name", "无"))
                .setContentText(mSp.getString("temp", "0") + "℃")
             //   .setWhen(mSp.get)
             //   .setContentIntent(contentIntent)
                .build();
        notification.flags = Notification.FLAG_ONGOING_EVENT; //设置"正在运行"


    }

    private void showNotific() {
        Log.e(TAG, "showNotific");
        mNM.notify(NOTIFICATION, notification);
    }

    private void cancelNotific() {
        mNM.cancel(NOTIFICATION);
    }

}
