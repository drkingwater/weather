package com.news.pxq.weather.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.news.pxq.weather.R;
import com.news.pxq.weather.contains.WeatherItem;
import com.news.pxq.weather.db.WeatherDb;
import com.news.pxq.weather.listenner.HttpCallbackListener;
import com.news.pxq.weather.popupwindow.MorePopupWindow;
import com.news.pxq.weather.util.HttpUtil;
import com.news.pxq.weather.util.Utility;
import com.news.pxq.weather.view.WeatherChartView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pxq on 2016/7/2.
 */
public class WeatherActivity extends AppCompatActivity {

    private static final String TAG = "WeatherActivity";

    private LinearLayout weatherInfoLayout;

    private SwipeRefreshLayout swipeRefreshLayout;

    private Button add;

    private Button more;

    private TextView currentTemp;

    /**
     * 当前选择的城市
     */
    private String selectedCity;

    private TextView temp2;

    /**
     * 天气发布时间
     */
    private TextView pTime;

    private TextView currentTime;

    /**
     * 未来日期
     */
    private GridView dateGrid;

    /**
     * 天气图片
     */
    private GridView weatherPicGrid;

    private Spinner mSpinner;   //选中的城市列表

    private List<String> mSpinnerList;

    private ArrayAdapter mSpinnerAdapter;

    private long exitTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

      /*  if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT){
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
*/


        setContentView(R.layout.weather_layout);

        initToolBar();
        initGridView();

        final String countyCode = getIntent().getStringExtra("county_code");  //choose_area传过来的参数
        final String county_name = getIntent().getStringExtra("county_name");  //choose_area传过来的参数
        selectedCity = getIntent().getStringExtra("selected_city");  //choose_area传过来的参数

        //获取设置信息
        final SharedPreferences prefs = PreferenceManager.
                getDefaultSharedPreferences(WeatherActivity.this);
        final SharedPreferences settingsSp = getSharedPreferences(SettingsActivity.SETTINGS_SP, MODE_PRIVATE);
        boolean isUpdateLaunch = settingsSp.getBoolean("update_launch", false);
        if (!TextUtils.isEmpty(countyCode) || !TextUtils.isEmpty(county_name)) {     //判断是否从choose_are中选择城市
            pTime.setText("同步中...");
            if (!TextUtils.isEmpty(countyCode))
                queryWeatherCode(countyCode);
            else
                queryWeatherbyName(county_name);
        } else if (isUpdateLaunch) {             //判断是否启动更新
            Log.e(TAG, isUpdateLaunch + "");
            String cityName = prefs.getString("city_name", "");
            String result = WeatherDb.getInstance(WeatherActivity.this).queryCountyCode(cityName);
            if (!TextUtils.isEmpty(cityName) || !TextUtils.isEmpty(result)) {
                //queryWeatherInfoByCityName(cityName);
                if (!TextUtils.isEmpty(result))
                    queryWeatherCode(result);
                else
                    queryWeatherbyName(cityName);

            }
        } else
            showWeather();

        //更新天气数据

        assert swipeRefreshLayout != null;
        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_green_light,
                android.R.color.holo_orange_light, android.R.color.holo_blue_light);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                pTime.setText("同步中...");
                String cityName = prefs.getString("city_name", "");
                String result = WeatherDb.getInstance(WeatherActivity.this).queryCountyCode(cityName);
                if (!TextUtils.isEmpty(cityName) || !TextUtils.isEmpty(result)) {
                    //queryWeatherInfoByCityName(cityName);
                    if (!TextUtils.isEmpty(result))
                        queryWeatherCode(result);
                    else
                        queryWeatherbyName(cityName);

                }
            }
        });

        initSelectedCity();

    }

    /**
     * 初始化ToolBar
     */
    private void initToolBar() {
        add = (Button) findViewById(R.id.add_btn);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WeatherActivity.this, ChooseAreaActivity.class);
                intent.putExtra("from_weather_activity", true);
                startActivity(intent);
                finish();
                return;
            }
        });

        more = (Button) findViewById(R.id.more_btn);
        more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //showPopupWindow(v);
                MorePopupWindow.getInstance(WeatherActivity.this).showMorePopupWindow(v);
            }
        });

    }

    /**
     * 初始化Spinner
     */
    private void initSelectedCity() {
        mSpinnerList = WeatherDb.getInstance(WeatherActivity.this).loadAllSelectedCity();

        int position = 0;
        if (mSpinnerList.contains(selectedCity)) {
            position = mSpinnerList.indexOf(selectedCity);
        }
        mSpinnerAdapter = new ArrayAdapter(WeatherActivity.this, R.layout.spinner_item, mSpinnerList);
        mSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mSpinner.setAdapter(mSpinnerAdapter);
        mSpinner.setSelection(position);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String cityName = mSpinnerList.get(position);
                if (selectedCity == null || !selectedCity.equals(cityName)) {   //如果选择的城市不是当前已选择的城市,则显示所选城市的天气信息
                    String countyCode = WeatherDb.getInstance(WeatherActivity.this).queryCountyCode(cityName);
                    if (!TextUtils.isEmpty(countyCode))
                        queryWeatherCode(countyCode);
                    else
                        queryWeatherbyName(cityName);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Toast.makeText(WeatherActivity.this, "onNothingSelected", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initGridView() {

        weatherInfoLayout = (LinearLayout) findViewById(R.id.weather_info_layout);

        //cityName = (TextView) findViewById(R.id.city_name);

        pTime = (TextView) findViewById(R.id.publish_text);

        currentTemp = (TextView) findViewById(R.id.temp);

        temp2 = (TextView) findViewById(R.id.temp2);

        currentTime = (TextView) findViewById(R.id.current_date);

        dateGrid = (GridView) findViewById(R.id.date_grid);

        weatherPicGrid = (GridView) findViewById(R.id.weather_pic_grid);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);

        mSpinner = (Spinner) findViewById(R.id.spinner);

    }

    /**
     * 从sp中读取天气信息
     */
    private void showWeather() {

        SharedPreferences sp = PreferenceManager.
                getDefaultSharedPreferences(this);
        selectedCity = sp.getString("city_name", "");
        currentTemp.setText("当前温度：" + sp.getString("temp", "") + "℃");
        pTime.setText("今天" + sp.getString("public_time", "") + "发布");
        currentTime.setText(sp.getString("current_time", ""));
        temp2.setText("风向：" + sp.getString("fengxiang", "") + "  风力：" + sp.getString("fengli", ""));
        weatherInfoLayout.setVisibility(View.VISIBLE);
        //cityName.setVisibility(View.VISIBLE);

        //设置未来5天的日期
        String d = sp.getString("date", "");
        if (!TextUtils.isEmpty(d)) {
            String[] tempD = d.split(",");

            List<String> dateList = new ArrayList<>();
            dateList.add("昨天");
            dateList.add("今天");
            dateList.add("明天");

            for (int i = 3; i < tempD.length; i++) {
                //Log.e("WeatherActivity", tempD[i].length() + "");
                String da = tempD[i].substring(0, tempD[i].length() - 3);
                dateList.add(da);
                //  date[i].setText(da);
            }

            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dateList);
            dateGrid.setAdapter(arrayAdapter);
        }

        WeatherChartView chartView = (WeatherChartView) findViewById(R.id.line_char);

        //设置最高温度
        String high = sp.getString("high_temp", "");
        String[] highTemp;
        if (!TextUtils.isEmpty(high)) {
            highTemp = high.split(",");
            int[] temp = new int[highTemp.length];
            for (int i = 0; i < highTemp.length; i++) {
                String[] array = highTemp[i].split(" ");
                array[1] = array[1].substring(0, array[1].length() - 1);
                temp[i] = Integer.parseInt(array[1]);
            }
            // set day
            chartView.setTempDay(temp);
        }

        //设置最低温度
        String low = sp.getString("low_temp", "");
        String[] lowTemp;
        if (!TextUtils.isEmpty(low)) {
            lowTemp = low.split(",");
            int[] temp = new int[lowTemp.length];
            for (int i = 0; i < lowTemp.length; i++) {
                String[] array = lowTemp[i].split(" ");
                array[1] = array[1].substring(0, array[1].length() - 1);
                temp[i] = Integer.parseInt(array[1]);
                //    Log.e("WeatherActivity", "Low:" + array[1]);
            }
            // set day
            chartView.setTempNight(temp);
        }

        chartView.startDraw();

        //设置天气
        String t = sp.getString("type", "");
        if (!TextUtils.isEmpty(t)) {
            String[] ty = t.split(",");

            //List<String> weatherList = new ArrayList<>();
            List<Map<String, Object>> weatherPic = new ArrayList<>();
            for (int i = 0; i < ty.length; i++) {
                // Log.e("xxx", ty[i]);
                if (i % 2 == 0) {
                    Map<String, Object> item = new HashMap<>();
                    if (WeatherItem.weatherItem.containsKey(ty[i])) {
                        //weatherList.add(ty[i]);
                        item.put("text_item", ty[i]);
                        item.put("image_item", WeatherItem.weatherItem.get(ty[i]));   //天气图片
                        weatherPic.add(item);

                    }
                }
            }
            //设置适配器

            SimpleAdapter simpleAdapter = new SimpleAdapter(this, weatherPic, R.layout.weather_grid_item, new String[]{"text_item", "image_item"}, new int[]{R.id.text_item, R.id.image_item});
            weatherPicGrid.setAdapter(simpleAdapter);

        }
        //刷新完成
        stopRefreshing();

    }

    /**
     * 查询指定城市的天气
     *
     * @param countyCode
     */
    private void queryWeatherCode(String countyCode) {
        startRefreshing();
        //Log.e(TAG, "queryWeatherCode");
        String address = "http://www.weather.com.cn/data/list3/city" +
                countyCode + ".xml";
        queryFromServer(address, "countyCode");
    }

    private void queryWeatherbyName(String cityName) {
        startRefreshing();

        String address = "http://wthrcdn.etouch.cn/WeatherApi?city=" +
                cityName;
        queryFromServer(address, "cityName");
    }

    /**
     * 按天气代号查询
     *
     * @param weatherCode
     */
    private void queryWeatherInfo(String weatherCode) {
        //http://wthrcdn.etouch.cn/WeatherApi?citykey=101300501
        String address = "http://wthrcdn.etouch.cn/WeatherApi?citykey=" +
                weatherCode;
        queryFromServer(address, "weatherCode");
    }


    /**
     * 从服务器获取天气信息
     * 更加天气代号或者城市代号查询
     *
     * @param address
     * @param type
     */
    private void queryFromServer(final String address, final String type) {
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(final String response) {

                if ("countyCode".equals(type)) {

                    if (!TextUtils.isEmpty(response)) {
                        // 从服务器返回的数据中解析出天气代号
                        String[] array = response.split("\\|");
                        if (array != null && array.length == 2) {

                            String weatherCode = array[1];
                            queryWeatherInfo(weatherCode);
                        }
                    }
                } else if ("weatherCode".equals(type) || "cityName".equals(type)) {
                    Log.e(TAG, response);
                    Utility.handleWeatherResponse(WeatherActivity.this,
                            response);
                    if (TextUtils.isEmpty(response))
                        Toast.makeText(WeatherActivity.this, "暂无该城市数据", Toast.LENGTH_SHORT).show();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showWeather();
                        }
                    });
                }

            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pTime.setText("同步失败");
                        Toast.makeText(WeatherActivity.this, "网络连接失败", Toast.LENGTH_SHORT).show();
                        stopRefreshing();
                    }
                });

            }
        });
    }

    private void startRefreshing() {
        if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing())
            swipeRefreshLayout.setRefreshing(true);
    }

    /**
     * 停止刷新动画
     */
    private void stopRefreshing() {
        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing())
            swipeRefreshLayout.setRefreshing(false);
    }


    @Override
    public void onBackPressed() {
        exit();
    }

    public void exit() {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            Toast.makeText(getApplicationContext(), "再按一次退出程序",
                    Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        } else {
            finish();
            //System.exit(0);
        }
    }
}
