package com.example.apofe.voicealytics;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by DASAPP on 27.02.2018.
 */

public class PrefManager {
    // Shared preferences file name
    private static final String PREF_NAME = "DASAPP";
    private static final String IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch"; // Переменная для проверки приложения на первый запуск
    private static final String USER_PROFILE_PHONE_NUMBER = "number";  // Переменная для хранения города пользователя
    private static final String USER_PROFILE_NAME = "name";  // Переменная для хранения имени пользователя
    private static final String USER_PROFILE_UID = "0";  // Переменная для хранения id пользователя
    private static final String USER_PROFILE_API = "https://sound.retail-info.ru/json/";  // Переменная для хранения api пользователя
    private static final String USER_PROFILE_RADIUS = "350";  // Переменная для хранения радиуса отклонения
    private static final String USER_PROFILE_TIME = "175";  // Переменная для хранения времени записи
    private static final String USER_PROFILE_KEY = "key";  // Переменная для хранения id пользователя
    private static final String USER_PROFILE_LNG = "lng";  // Переменная для хранения долготы
    private static final String USER_PROFILE_LAT = "lat";  // Переменная для хранения штроты
    private static final String USER_RECORDED = "recorded";  // Переменная для хранения часового пояса
    private static final String USER_CHECK_GEO = "check_geo";  // Переменная для хранения часового пояса

    SharedPreferences pref;
    SharedPreferences.Editor editor;
    Context _context;
    // shared pref mode
    int PRIVATE_MODE = 0;

    public PrefManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    public void setUserProfileName(String userName) {  // Сохраняем имя пользователя
        editor.putString(USER_PROFILE_NAME, userName);
        editor.commit();
    }

    public void setUserProfilePhoneNumber(String phoneNumber) {  // Сохраняем телефон пользователя
        editor.putString(USER_PROFILE_PHONE_NUMBER, phoneNumber);
        editor.commit();
    }

    public void setUserProfileUid(String uid) {  // Сохраняем id пользователя
        editor.putString(USER_PROFILE_UID, uid);
        editor.commit();
    }

    public void setUserProfileApi(String api) {  // Сохраняем id пользователя
        editor.putString(USER_PROFILE_API, api);
        editor.commit();
    }

    public void setUserProfileRadius(String radius) {  // Сохраняем id пользователя
        editor.putString(USER_PROFILE_RADIUS, radius);
        editor.commit();
    }

    public void setUserProfileTime(String time) {  // Сохраняем id пользователя
        editor.putString(USER_PROFILE_TIME, time);
        editor.commit();
    }

    public void setUserProfileKey(String key) {  // Сохраняем id пользователя
        editor.putString(USER_PROFILE_KEY, key);
        editor.commit();
    }

    public void setUserProfileLng(String lng) {  // Сохраняем id пользователя
        editor.putString(USER_PROFILE_LNG, lng);
        editor.commit();
    }


    public void setUserProfileLat(String lat) {  // Сохраняем id пользователя
        editor.putString(USER_PROFILE_LAT, lat);
        editor.commit();
    }

    public void setUserCheckGeo(boolean checkGeo) {  // Пишем запускалось приложение уже или нет
        editor.putBoolean(USER_CHECK_GEO, checkGeo);
        editor.commit();
    }

    public boolean isFirstTimeLaunch() { // Возвращает открывалось приложение или нет
        return pref.getBoolean(IS_FIRST_TIME_LAUNCH, true);
    }

    public void setFirstTimeLaunch(boolean isFirstTime) {  // Пишем запускалось приложение уже или нет
        editor.putBoolean(IS_FIRST_TIME_LAUNCH, isFirstTime);
        editor.commit();
    }

    public boolean isCheckGeo() { // Возвращает открывалось приложение или нет
        return pref.getBoolean(USER_CHECK_GEO, false);
    }

    public String isUserName() {   // Возвращает имя пользователя
        return pref.getString(USER_PROFILE_NAME, "Имя");
    }

    public String isUserUid() {   // Возвращает id пользователя
        return pref.getString(USER_PROFILE_UID, "0");
    }

    public String isUserApi() {   // Возвращает id пользователя
        return pref.getString(USER_PROFILE_API, "https://sound.retail-info.ru/json/");
    }

    public String isUserRadius() {   // Возвращает id пользователя
        return pref.getString(USER_PROFILE_RADIUS, "350");
    }

    public String isUserTime() {   // Возвращает id пользователя
        return pref.getString(USER_PROFILE_TIME, "175");
    }

    public String isUserKey() {   // Возвращает ключ пользователя
        return pref.getString(USER_PROFILE_KEY, "key");
    }

    public String isUserLng() {   // Возвращает ключ пользователя
        return pref.getString(USER_PROFILE_LNG, "lng");
    }

    public String isUserlat() {   // Возвращает ключ пользователя
        return pref.getString(USER_PROFILE_LAT, "lat");
    }

    public boolean isUserRecorded() {   // Возвращает часовой пояс пользователя
        return pref.getBoolean(USER_RECORDED, false);
    }

    public void setUserRecorded(boolean recorded) {  // Сохраняем часовой пояс пользователя
        editor.putBoolean(USER_RECORDED, recorded);
        editor.commit();
    }

    public String isUserPhoneNumber() {   // Возвращает номер телефона
        return pref.getString(USER_PROFILE_PHONE_NUMBER, "+7(999)999-99-99");
    }

}

