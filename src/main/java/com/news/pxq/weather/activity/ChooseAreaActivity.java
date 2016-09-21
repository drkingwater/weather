package com.news.pxq.weather.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.news.pxq.weather.R;
import com.news.pxq.weather.db.WeatherDb;
import com.news.pxq.weather.listenner.HttpCallbackListener;
import com.news.pxq.weather.model.City;
import com.news.pxq.weather.model.County;
import com.news.pxq.weather.model.Province;

import com.news.pxq.weather.util.HttpUtil;
import com.news.pxq.weather.util.Utility;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pxq on 2016/7/2.
 */
public class ChooseAreaActivity extends AppCompatActivity {

    private static final String TAG = "ChooseAreaActivity";

    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;
    public static final int QUIT = 3;

    public static final int LOCATECODE = 100;
    public static final int WRITESTORAGE = 101;

    private ProgressDialog progressDialog;
    private TextView titleText;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private WeatherDb weatherDb;
    private List<String> datalist = new ArrayList<>();

    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;

    private Province selectedProvince;  //选中的省份
    private City selectedCity;       //选中的城市
    private int currentLevel;  //当前级别

    //private boolean isExit = false;  //判断是否退出程序
    private boolean isFromWeatherActivity = false;

    public LocationClient mLocationClient = null;
    //public BDLocationListener myListener = new MyLocationListener();
    double lat;
    double log;

  /*  Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //isExit = false;
        }
    };*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choose_area);

        isFromWeatherActivity = getIntent().getBooleanExtra("from_weather_activity", false);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (sp.getBoolean("city_selected", false) && !isFromWeatherActivity) {
            Intent intent = new Intent(this, WeatherActivity.class);
            startActivity(intent);
            this.finish();
            return;
        }

        //定位
        mLocationClient = new LocationClient(getApplicationContext());     //声明LocationClient类
        mLocationClient.registerLocationListener(new BDLocationListener() {
            @Override
            public void onReceiveLocation(BDLocation bdLocation) {
                if (bdLocation == null) {
                    Toast.makeText(ChooseAreaActivity.this, "定位失败", Toast.LENGTH_SHORT).show();
                    return;
                }
                StringBuffer sb = new StringBuffer(256);

                /* 获取经纬度 */
                lat = bdLocation.getLatitude();
                log = bdLocation.getLongitude();

                sb.append("latitude:  " + lat + "\n");
                sb.append("longitude: " + log);

                Log.e("ChooseArea", sb.toString());

                mLocationClient.stop();

                queryFromServer(null, "locate");
            }
        });    //注册监听函数

        Button locate = (Button) findViewById(R.id.locate);
        assert locate != null;
        locate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //申请权限
                if (ContextCompat.checkSelfPermission(ChooseAreaActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    //未取得权限
                    ActivityCompat.requestPermissions(ChooseAreaActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATECODE);
                } else
                    mLocationClient.start();   //已取得权限
            }
        });

        // getSupportActionBar().hide();

        listView = (ListView) findViewById(R.id.list_view);
        titleText = (TextView) findViewById(R.id.title_text);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_expandable_list_item_1, datalist);
        listView.setAdapter(adapter);

        weatherDb = WeatherDb.getInstance(this);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(position);
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(position);
                    queryCounties();
                } else if (currentLevel == LEVEL_COUNTY) {   //已选择具体地点
                    currentLevel = QUIT;
                    String countyCode = countyList.get(position).getCountyCode();
                    String countyName = countyList.get(position).getCountyName();
                    //把选中的城市添加进数据库
                    WeatherDb.getInstance(ChooseAreaActivity.this).addSelectedCity(countyName, countyCode);
                    //显示天气信息
                    Intent intent = new Intent(ChooseAreaActivity.this, WeatherActivity.class);
                    intent.putExtra("county_code", countyCode);
                    intent.putExtra("selected_city", countyName);
                    startActivity(intent);
                    finish();
                }
            }
        });
        queryProvinces();
        initLocation();
    }

    private void initLocation() {
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy
        );//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系
        int span = 1000;
        option.setScanSpan(span);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的

        option.setOpenGps(true);//可选，默认false,设置是否使用gps

        mLocationClient.setLocOption(option);
    }

    /**
     * 获取省份信息，优先查找数据库，无数据则查找服务器
     */
    private void queryProvinces() {
        provinceList = weatherDb.loadProvinces();

        if (provinceList.size() > 0) {
            datalist.clear();
            for (Province province : provinceList) {
                datalist.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();  //更新listView的信息
            listView.setSelection(0);
            titleText.setText("中国");
            currentLevel = LEVEL_PROVINCE;
            Log.e("ChooseActivity", "province");
        } else {
            queryFromServer(null, "province");
        }
    }

    /**
     * 从服务器查找信息
     *
     * @param code
     * @param type
     */
    private void queryFromServer(final String code, final String type) {
        String address;

        if (!TextUtils.isEmpty(code)) {
            address = "http://www.weather.com.cn/data/list3/city" + code +
                    ".xml";
        } else if ("locate".equals(type))
            address = "http://api.map.baidu.com/geocoder/v2/?location=" + lat + "," + log + "&output=json&ak=lQuEN5YZfQTjvjKG1erX7jfK3TGqgrPF&pois=1&mcode=61:1B:F7:4D:E3:8A:CE:EE:38:78:C4:E8:AA:86:2B:47:7B:B2:C1:B7;com.news.pxq.weather";
            //address = "http://api.map.baidu.com/geocoder/v2/?output=json&ak=lQuEN5YZfQTjvjKG1erX7jfK3TGqgrPF&mcode=61:1B:F7:4D:E3:8A:CE:EE:38:78:C4:E8:AA:86:2B:47:7B:B2:C1:B7;com.news.pxq.weather";
        else
            address = "http://www.weather.com.cn/data/list3/city.xml";
        Log.e("Choose", address);
        showProgressDialog();
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                boolean result = false;
                String cityName = null;
                if ("province".equals(type)) {
                    result = Utility.handleProvincesResponse(weatherDb, response);
                } else if ("city".equals(type)) {
                    result = Utility.handleCitiesResponse(weatherDb, response, selectedProvince.getId());
                } else if ("county".equals(type)) {
                    result = Utility.handleCountiesResponse(weatherDb, response, selectedCity.getId());
                } else if ("locate".equals(type)) {
                    cityName = Utility.handleLocateInfo(response);
                    Log.e(TAG, cityName + "定位成功");
                    if (!TextUtils.isEmpty(cityName)) {
                        closeProgressDialog();
                        toWeatherInfo(cityName);
                    } else
                        result = false;
                }

                if (result) {
                    //通过runOnUiThread()方法返回主线程处理

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)) {
                                queryProvinces();
                            } else if ("city".equals(type)) {
                                queryCities();
                            } else if ("county".equals(type))
                                queryCounties();

                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(ChooseAreaActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    /**
     * 关闭对话框
     */
    private void closeProgressDialog() {

        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    /**
     * 显示对话框
     */
    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);

        }
        progressDialog.show();
    }

    private void toWeatherInfo(String cityName) {
        WeatherDb.getInstance(ChooseAreaActivity.this).addSelectedCity(cityName, "");
        Intent intent = new Intent(ChooseAreaActivity.this, WeatherActivity.class);
        String countyCode = WeatherDb.getInstance(ChooseAreaActivity.this).queryCountyCode(cityName);
        intent.putExtra("county_name", cityName);
        intent.putExtra("selected_city", cityName);
        startActivity(intent);
        finish();
    }

    /**
     * 获取所选城市下县信息，优先查找数据库，无数据则查找服务器
     */
    private void queryCounties() {

        countyList = weatherDb.loadCounties(selectedCity.getId());
        if (countyList.size() > 0) {
            datalist.clear();
            for (County county : countyList) {
                datalist.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectedCity.getCityName());
            currentLevel = LEVEL_COUNTY;
            Log.e("ChooseActivity", "county");
        } else {
            queryFromServer(selectedCity.getCityCode(), "county");
        }
    }

    /**
     * 获取所选省份下城市信息，优先查找数据库，无数据则查找服务器
     */
    private void queryCities() {
        cityList = weatherDb.loadCities(selectedProvince.getId());
        if (cityList.size() > 0) {
            datalist.clear();
            for (City c : cityList) {
                datalist.add(c.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectedProvince.getProvinceName());
            currentLevel = LEVEL_CITY;
            Log.e("ChooseActivity", "city");
        } else {
            queryFromServer(selectedProvince.getProvinceCode(), "city");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case LOCATECODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    mLocationClient.start();   //已取得权限
                } else
                    Toast.makeText(ChooseAreaActivity.this, "需要定位权限", Toast.LENGTH_SHORT).show();
                break;
         /*   case WRITESTORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    //取得写SD卡权限
                    mLocationClient.start();
                }else
                    Toast.makeText(ChooseAreaActivity.this, "需要读写SD卡权限", Toast.LENGTH_SHORT).show();*/
        }
    }

    @Override
    public void onBackPressed() {
        //  super.onBackPressed();
        switch (currentLevel) {
            case LEVEL_CITY:
                queryProvinces();
                break;
            case LEVEL_COUNTY:
                queryCities();
                break;

            default:
                if (isFromWeatherActivity) {
                    Intent intent = new Intent(this, WeatherActivity.class);
                    startActivity(intent);
                }
                this.finish();


                //    exit();
        }

    }


    /*private void exit() {
        if (!isExit) {
            isExit = true;
            Toast.makeText(ChooseAreaActivity.this, "再按一次返回键退出程序", Toast.LENGTH_SHORT).show();
            //延迟2秒发送消息
            handler.sendEmptyMessageDelayed(0, 2000);
        } else {
            finish();
            System.exit(0);
        }
    }*/

    @Override
    protected void onDestroy() {
        if (mLocationClient != null && mLocationClient.isStarted()) {
            mLocationClient.stop();
            mLocationClient = null;
        }
        super.onDestroy();
    }
}
