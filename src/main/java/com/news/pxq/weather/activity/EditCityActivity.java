package com.news.pxq.weather.activity;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;

import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.news.pxq.weather.R;
import com.news.pxq.weather.db.WeatherDb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditCityActivity extends AppCompatActivity {

    private static final String TAG = "EditCityActivity";

    private ListView mCityList;

    private ArrayAdapter mAdapter;

    private List<String> mList;

    private LinearLayout mCancel;

    private LinearLayout mConfirm;

    private TextView mDelete;

    private int mCount = 0;

    private List<Integer> mTarget = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.action_edit_layout);

        initListView();
        initActionView();

    }

    private void initListView() {
        mCityList = (ListView) findViewById(R.id.selected_city_list);
        mList = WeatherDb.getInstance(EditCityActivity.this).loadAllSelectedCity();
        mAdapter = new ArrayAdapter(EditCityActivity.this, android.R.layout.simple_list_item_multiple_choice, mList);
        mCityList.setAdapter(mAdapter);
        mCityList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CheckedTextView mCheckedText = (CheckedTextView) view;
                mCheckedText.setChecked(!mCheckedText.isChecked());
                if (mCheckedText.isChecked()) {
                    mCount++;
                    mTarget.add(position);
                } else {
                    mCount--;
                    mTarget.remove((Object) position);     //防止index和Object冲突,指定删除的是Object
                }
                if (mCount > 0)
                    mDelete.setText("删除(" + mCount + ")");
                else
                    mDelete.setText("完成");
            }
        });

    }

    private void initActionView() {

        mDelete = (TextView) findViewById(R.id.delete);

        //取消
        mCancel = (LinearLayout) findViewById(R.id.cancel_layout);
        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(EditCityActivity.this, "取消", Toast.LENGTH_SHORT).show();
                onBackPressed();
            }
        });

        //确认
        mConfirm = (LinearLayout) findViewById(R.id.confirm_layout);
        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int index : mTarget) {
                    //Log.e(TAG, mList.get(index));
                    WeatherDb.getInstance(EditCityActivity.this).deleteCity(mList.get(index));
                }
                //处理完后返回
                if (mCount == mList.size()) {
                    clearSp();
                    Intent intent = new Intent(EditCityActivity.this, ChooseAreaActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                }
                onBackPressed();
            }
        });
    }


    /**
     * 清空Sp，让"selected_city"为false
     */
    private void clearSp() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(EditCityActivity.this);
        SharedPreferences.Editor mEditor = sp.edit();
        mEditor.clear();
        mEditor.commit();
    }

    @Override
    public void onBackPressed() {
        Intent mIntent = new Intent(EditCityActivity.this, WeatherActivity.class);
        startActivity(mIntent);
        finish();
    }

}
