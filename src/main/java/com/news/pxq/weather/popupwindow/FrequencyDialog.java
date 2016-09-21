package com.news.pxq.weather.popupwindow;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;


import com.news.pxq.weather.R;
import com.news.pxq.weather.activity.SettingsActivity;
import com.news.pxq.weather.listenner.FrequencyDLGCallBackListener;

/**
 * Created by pxq on 2016/8/4.
 */
public class FrequencyDialog {

    public static String[] items = {"每小时", "每2小时", "每5小时", "每8小时", "每12小时"};

    public static int[] time = {1, 2, 5, 8, 12};

    private SharedPreferences mSp;

    private SharedPreferences.Editor mEditor;

    private AlertDialog.Builder mBuilder;

    public FrequencyDialog(final Activity context){

        mBuilder = new AlertDialog.Builder(context);

        //设置标题
        mBuilder.setTitle(R.string.update_frequency);

        mBuilder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        mSp = context.getSharedPreferences(SettingsActivity.SETTINGS_SP, Context.MODE_APPEND);

        mEditor = mSp.edit();
    }

    public void showDialog(int index, final FrequencyDLGCallBackListener mCBListener){
        //设置选中的item
        mBuilder.setSingleChoiceItems(items, index, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Toast.makeText(context, items[which], Toast.LENGTH_SHORT).show();

                mEditor.putInt("checked_position", which);
                mEditor.commit();
                dialog.dismiss();

                mCBListener.onDismiss(which);
            }
        });
        mBuilder.create().show();
    }

}
