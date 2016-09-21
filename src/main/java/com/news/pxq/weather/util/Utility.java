package com.news.pxq.weather.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.news.pxq.weather.db.WeatherDb;
import com.news.pxq.weather.model.City;
import com.news.pxq.weather.model.County;
import com.news.pxq.weather.model.Province;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 解析服务器返回的信息
 * <p>
 * Created by pxq on 2016/7/2.
 */
public class Utility {

    /**
     * 处理省份信息
     * 格式为：100150|xx,100154|xx
     *
     * @param weatherDb
     * @param response
     * @return
     */
    public synchronized static boolean handleProvincesResponse(WeatherDb weatherDb, String response) {
        if (!TextUtils.isEmpty(response)) {
            String[] provinces = response.split(",");
            if (provinces != null && provinces.length > 0) {
                for (String p : provinces) {
                    String[] array = p.split("\\|");
                    Province province = new Province();
                    province.setProvinceCode(array[0]);
                    province.setProvinceName(array[1]);
                    weatherDb.saveProvince(province);    //将省份保存到数据库

                }
                return true;
            }
        }
        return false;
    }

    /**
     * 处理城市信息
     *
     * @param weatherDb
     * @param response
     * @param provinceId
     * @return
     */
    public synchronized static boolean handleCitiesResponse(WeatherDb weatherDb, String response, int provinceId) {
        if (!TextUtils.isEmpty(response)) {
            String[] cites = response.split(",");
            if (cites != null && cites.length > 0)
                for (String c : cites) {
                    String[] array = c.split("\\|");
                    City city = new City();
                    city.setCityCode(array[0]);
                    city.setCityName(array[1]);
                    city.setProvinceId(provinceId);
                    weatherDb.saveCity(city);
                }

            return true;
        }
        return false;
    }

    /**
     * 处理县级城市
     *
     * @param weatherDb
     * @param response
     * @param cityId
     * @return
     */
    public synchronized static boolean handleCountiesResponse(WeatherDb weatherDb, String response, int cityId) {
        if (!TextUtils.isEmpty(response)) {
            String[] counties = response.split(",");
            if (counties != null && counties.length > 0) {
                for (String c : counties) {
                    String[] array = c.split("\\|");
                    County county = new County();
                    county.setCountyCode(array[0]);
                    county.setCountyName(array[1]);
                    county.setCityId(cityId);
                    weatherDb.saveCounty(county);

                }
                return true;
            }
        }
        return false;
    }

    /**
     * 处理获得的天气信息
     * xml格式
     *
     * @param context
     * @param response
     */

    public static void handleWeatherResponse(Context context, String response) {
        Log.e("Utility", response);
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser xmlPullParser = factory.newPullParser();
            xmlPullParser.setInput(new StringReader(response));
            int eventType = xmlPullParser.getEventType();
            String cityName = "";

            String temp = "";
            String fengli = "";
            String fengxiang = "";
            String sunset = "";
            String pTime = "";

            String highTemp = "";
            String lowTemp = "";
            String date = "";
            String type = "";

            boolean flag = false;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String nodeName = xmlPullParser.getName();
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if ("updatetime".equals(nodeName)) {
                            pTime = xmlPullParser.nextText();
                        } else if ("wendu".equals(nodeName))
                            temp = xmlPullParser.nextText();
                        else if ("fengli".equals(nodeName))
                            fengli = xmlPullParser.nextText();
                        else if ("fengxiang".equals(nodeName))
                            fengxiang = xmlPullParser.nextText();
                        else if ("city".equals(nodeName))
                            cityName = xmlPullParser.nextText();
                        else if ("sunset_1".equals(nodeName))
                            sunset = xmlPullParser.nextText();
                        else if ("high_1".equals(nodeName) || "high".equals(nodeName))
                            highTemp += xmlPullParser.nextText() + ",";
                        else if ("low_1".equals(nodeName) || "low".equals(nodeName))
                            lowTemp += xmlPullParser.nextText() + ",";
                        else if ("date_1".equals(nodeName) || "date".equals(nodeName))
                            date += xmlPullParser.nextText() + ",";
                        else if ("type_1".equals(nodeName) || "type".equals(nodeName))
                            type += xmlPullParser.nextText() + ",";
                        else if ("name".equals(nodeName)) {

                        }

                        break;
                    case XmlPullParser.END_TAG:
                        if ("forecast".equals(nodeName))
                            flag = true;
                        break;

                }
                if (flag)
                    break;
                eventType = xmlPullParser.next();

            }
            highTemp = highTemp.substring(0, highTemp.length() - 1);
            lowTemp = lowTemp.substring(0, lowTemp.length() - 1);
            date = date.substring(0, date.length() - 1);
            type = type.substring(0, type.length() - 1);
            saveWeatherInfo(context, cityName, temp, fengli, fengxiang, sunset, pTime, highTemp, lowTemp, date, type);
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 保存天气信息进sp中
     *
     * @param context
     * @param cityName
     * @param temp
     * @param fengli
     * @param fengxiang
     * @param sunset
     * @param ptime
     * @param high
     * @param low
     * @param date
     * @param type
     */
    public static void saveWeatherInfo(Context context, String cityName, String temp, String fengli, String fengxiang, String sunset, String ptime
            , String high, String low, String date, String type) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日", Locale.CHINA);

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();

        editor.putBoolean("city_selected", true);
        editor.putString("city_name", cityName);
        editor.putString("temp", temp);
        editor.putString("fengli", fengli);
        editor.putString("fengxiang", fengxiang);
        editor.putString("public_time", ptime);
        editor.putString("current_time", sdf.format(new Date()));
        editor.putString("high_temp", high);
        editor.putString("low_temp", low);
        editor.putString("date", date);
        editor.putString("type", type);

        editor.commit();
    }

    public static String handleLocateInfo(String response) {

        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONObject result = jsonObject.getJSONObject("result");
            JSONObject addressComponent = result.getJSONObject("addressComponent");
            String cityName = addressComponent.getString("district");
            if (cityName.contains("市") || cityName.contains("县"))
                cityName = cityName.substring(0, cityName.length() - 1);
            return cityName;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }
}
