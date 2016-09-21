package com.news.pxq.weather.activity;




import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.news.pxq.weather.R;
import com.news.pxq.weather.listenner.FrequencyDLGCallBackListener;
import com.news.pxq.weather.popupwindow.FrequencyDialog;
import com.news.pxq.weather.service.UpdateBgService;

import java.util.List;


public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";

    public static final String SETTINGS_SP = "settingsSp";

    private Switch update_launch;

    private Switch update_bg;

    private Switch open_notific;

    private boolean isBinding = false;

    private boolean isExisted = false;  //通知是否存在，初始为false不存在

    private TextView mFrequencyText;

    private LinearLayout mFrequencyLayout;

    private SharedPreferences mSp;

    private SharedPreferences.Editor mEditor;

    private FrequencyDialog mFrequencyDialog;

    private UpdateBgService.NotificationBinder mBinder;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBinder = (UpdateBgService.NotificationBinder) service;
            //mBinder.showNotification();
            isExisted = false;      //初始化通知状态
            isBinding = true;
            listenNotification();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, "onServiceDisconnected");
            isBinding = false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);

        mFrequencyDialog = new FrequencyDialog(SettingsActivity.this);
        mSp = getSharedPreferences(SETTINGS_SP, MODE_APPEND);
        mEditor = mSp.edit();

        initView();

    }


    private void initView() {
        //启动更新
        update_launch = (Switch) findViewById(R.id.update_launch);
        update_launch.setChecked(mSp.getBoolean("update_launch", false));
        update_launch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Log.e("Settings", update_launch.isChecked() + "");
                mEditor.putBoolean("update_launch", update_launch.isChecked());
                mEditor.commit();
            }
        });

        //后台更新
        update_bg = (Switch) findViewById(R.id.update_bg);
        update_bg.setChecked(mSp.getBoolean("update_bg", false));
        update_bg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Log.e("Settings", update_bg.isChecked() + "");
                boolean isChecked = update_bg.isChecked();
                open_notific.setClickable(isChecked);       //设置Switch是否点击
                mFrequencyLayout.setEnabled(isChecked);   //设置频率布局是否可用
                mFrequencyLayout.setClickable(isChecked);

                doUpdateBgChecked(isChecked);

                mEditor.putBoolean("update_bg", isChecked);

                mEditor.commit();
            }
        });


        //通知栏常驻
        open_notific = (Switch) findViewById(R.id.open_notification);
        open_notific.setChecked(mSp.getBoolean("open_notification", false));
        open_notific.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                openNotification(open_notific.isChecked());

                mEditor.putBoolean("open_notification", open_notific.isChecked());
                mEditor.commit();
            }
        });

        //设置更新频率
        mFrequencyLayout = (LinearLayout) findViewById(R.id.frequency_lin);
        mFrequencyLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //FrequencyPopupWindow.getInstance(SettingsActivity.this).showMorePopupWindow(v);

                //Log.e(TAG, mFrequencyDialog.toString());
                mFrequencyDialog.showDialog(mSp.getInt("checked_position", 0), new FrequencyDLGCallBackListener() {
                    @Override
                    public void onDismiss(int which) {
                        mFrequencyText.setText(FrequencyDialog.items[which]);
                        mEditor.putInt("frequency", FrequencyDialog.time[which]);
                        mEditor.commit();
                        doUpdateBgChecked(update_bg.isChecked());
                    }
                });
            }
        });

       initUpdateBgSettings(update_bg.isChecked());

        //设置初始更新时间频率
        mFrequencyText = (TextView) findViewById(R.id.frequency_text);
        mFrequencyText.setText(FrequencyDialog.items[mSp.getInt("checked_position", 0)]);

    }

    private void initUpdateBgSettings(boolean isChecked){
        if (isChecked) {
            mFrequencyLayout.setAlpha(1f);    //设置频率布局的透明度
            openNotification(open_notific.isChecked());  //是否开启通知
        } else {
            mFrequencyLayout.setAlpha(0.5f);
            mFrequencyLayout.setClickable(false);
            open_notific.setClickable(isChecked);
        }
    }

    /**
     * 开启后台更新
     *
     * @param isChecked
     */
    private void doUpdateBgChecked(boolean isChecked) {
        if (isChecked)
            mFrequencyLayout.setAlpha(1f);    //设置频率布局的透明度
        else
            mFrequencyLayout.setAlpha(0.5f);

        Intent mService = new Intent(SettingsActivity.this, UpdateBgService.class);

        //mService.putExtra("frequency", mSp.getInt("frequency", 1));
        if (isChecked) {
            stopService(mService); //停止服务
            startService(mService);
            openNotification(open_notific.isChecked());
            //bindService(mService, connection, BIND_AUTO_CREATE);

        } else {       //停止后台服务
            stopService(mService);
            if (mBinder != null && isBinding) {
                unbindService(connection);
                isBinding = false;     //绑定状态
            }
        }
    }

    private void openNotification(boolean isChecked){
        Intent mService = new Intent(SettingsActivity.this, UpdateBgService.class);
        if (isChecked){
            bindService(mService, connection, BIND_AUTO_CREATE);
        } else{
            if (mBinder != null && isBinding) {
                unbindService(connection);
                isBinding = false;     //绑定状态
            }
        }
    }

    /**
     * 监听通知的状态
     * 判断是否要打开或者关闭
     */
    private void listenNotification() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isBinding) {

                    if (open_notific.isChecked() && !isExisted) {      //如果请求打开通知，且通知不存在
                        mBinder.showNotification();      //开启通知
                        isExisted = true;              //设置状态为true 通知已存在
                      //  Log.e(TAG, "listenNotification + 打开");
                    } else if (!open_notific.isChecked() && isExisted) {    //请求关闭通知，且通知已存在
                        Log.e(TAG, "cancelNotification");
                        mBinder.cancelNotification();      //取消通知
                        isExisted = false;                 //设置状态为false 通知不存在
                     //   Log.e(TAG, "listenNotification + 关闭");
                    }
                }
              //  Log.e(TAG, "listenNotification + 停止");
            }
        }).start();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBinder != null && isBinding) {
            unbindService(connection);
            isBinding = false;
        }
    }
}
