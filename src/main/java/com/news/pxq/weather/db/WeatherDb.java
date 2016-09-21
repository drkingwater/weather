package com.news.pxq.weather.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.news.pxq.weather.model.City;
import com.news.pxq.weather.model.County;
import com.news.pxq.weather.model.Province;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pxq on 2016/7/2.
 */
public class WeatherDb {

    public static final String DBNAME = "Weather";

    public static final int VERSION = 1;

    private static WeatherDb weatherDb;

    private SQLiteDatabase db;

    private WeatherDb(Context context){
        WeatherOpenHelper weatherOpenHelper = new WeatherOpenHelper(context, DBNAME, null, VERSION);
        db = weatherOpenHelper.getWritableDatabase();
    }

    public synchronized static WeatherDb getInstance(Context context){
        if (weatherDb == null){
            weatherDb = new WeatherDb(context);
        }

        return weatherDb;
    }

    /**
     * 将省份保存到数据库
     *
     * @param province
     */
    public void saveProvince(Province province){
        if (province != null){
            ContentValues contentValues = new ContentValues();
            contentValues.put("province_code", province.getProvinceCode());
            contentValues.put("province_name", province.getProvinceName());
            db.insert("Province", null, contentValues);
        }
    }

    /**
     * 将城市信息保存到数据库
     *
     * @param city
     */
    public void saveCity(City city){
        if (city != null){
            ContentValues contentValues = new ContentValues();
            contentValues.put("city_code", city.getCityCode());
            contentValues.put("city_name", city.getCityName());
            contentValues.put("province_id", city.getProvinceId());
            db.insert("City", null, contentValues);
        }
    }

    /**
     * 将县信息保存到数据库
     *
     * @param county
     */
    public void saveCounty(County county){
        if (county != null){
            ContentValues contentValues = new ContentValues();
            contentValues.put("county_name", county.getCountyName());
            contentValues.put("county_code", county.getCountyCode());
            contentValues.put("city_id", county.getCityId());
            db.insert("County", null, contentValues);
        }
    }

    /**
     * 获取所有省份信息
     *
     * @return
     */
    public List<Province> loadProvinces(){
        List<Province> list = new ArrayList<>();
        Cursor cursor = db.query("Province", null, null, null, null, null, null);
        if(cursor.moveToFirst()) {
            do {
                Province province = new Province();
                province.setId(cursor.getInt(cursor.getColumnIndex("id")));
                province.setProvinceCode(cursor.getString(cursor.getColumnIndex("province_code")));
                province.setProvinceName(cursor.getString(cursor.getColumnIndex("province_name")));
                list.add(province);
            }while (cursor.moveToNext());
        }

        return list;
    }

    /**
     * 获取指定省份下所有城市信息
     *
     * @return
     */
    public List<City> loadCities(int provinceId){
        List<City> list = new ArrayList<>();
        Cursor cursor = db.query("City", null, "province_id = ?", new String[]{String.valueOf(provinceId)}, null, null, null);
        if(cursor.moveToFirst()) {
            do {
                City city = new City();
                city.setId(cursor.getInt(cursor.getColumnIndex("id")));
                city.setCityCode(cursor.getString(cursor.getColumnIndex("city_code")));
                city.setCityName(cursor.getString(cursor.getColumnIndex("city_name")));
                city.setProvinceId(provinceId);
                list.add(city);
            }while (cursor.moveToNext());
        }

        return list;
    }

    /**
     * 获取指定城市下所有县信息
     *
     * @return
     */
    public List<County> loadCounties(int cityId){
        List<County> list = new ArrayList<>();
        Cursor cursor = db.query("County", null, "city_id = ?", new String[]{String.valueOf(cityId)}, null, null, null);
        if(cursor.moveToFirst()) {
            do {
                County county = new County();
                county.setId(cursor.getInt(cursor.getColumnIndex("id")));
                county.setCountyCode(cursor.getString(cursor.getColumnIndex("county_code")));
                county.setCountyName(cursor.getString(cursor.getColumnIndex("county_name")));
                county.setCityId(cityId);
                list.add(county);
            }while (cursor.moveToNext());
        }

        return list;
    }

    /**
     * 查询指定城市的代码
     *
     * @param countyName  城市名
     * @return
     */
    public String queryCountyCode(String countyName){
        String countyCode = null;
        Cursor cursor = db.query("County", null, "county_name = ?", new String[]{countyName}, null, null, null);
        if (cursor.moveToFirst()){
            countyCode = cursor.getString(cursor.getColumnIndex("county_code"));
        }

        return countyCode;
    }

    /**
     * 添加选中的城市
     *
     * @param countyName
     * @param countyCode
     */
    public void addSelectedCity(String countyName, String countyCode){

        ContentValues mValues = new ContentValues();
        mValues.put("county_code", countyCode);
        mValues.put("county_name", countyName);
        db.insert("Save", null, mValues);

    }

    /**
     * 获取所有用户选中的城市
     *
     * @return
     */
    public List<String> loadAllSelectedCity(){
        List<String> list = new ArrayList<>();
        Cursor mCursor = db.query("Save", null, null, null, null, null, null);
        if (mCursor.moveToFirst()){
            do {
                String countyName = mCursor.getString(mCursor.getColumnIndex("county_name"));
                list.add(countyName);
            }while(mCursor.moveToNext());
        }
        return list;
    }

    public void deleteCity(String cityName){
        db.delete("Save", "county_name = ?", new String[]{cityName});
    }
}
