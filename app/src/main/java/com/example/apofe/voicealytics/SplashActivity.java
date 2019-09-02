package com.example.apofe.voicealytics;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class SplashActivity extends AppCompatActivity {

    private ProgressDialog mProgressDialog;
    private String api; // переменная в которой храним API
    private String radius;
    private String time;
    private PrefManager prefManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.SplashTheme);
        super.onCreate(savedInstanceState);

        // ставим предыдущие значения на переменные
        prefManager = new PrefManager(this);
        api = prefManager.isUserApi();
        radius = prefManager.isUserRadius();
        time = prefManager.isUserTime();

        //Запросы разрешений
        // запрос разрешения на доступ к микрофону
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    10);
        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) // разрешение на запись файлов
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    20);
        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) // разрешение на геопозицию
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    30);
        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) // разрешение на отслеживание статуса интернета
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_NETWORK_STATE},
                    40);
        } else {
            // все разрешения есть - переходим к получениб данных с сервера
            new GetAppData().execute();
        }
    }

    // переопределяем что будет когда даем (или нет) разрешения
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 10) { // рзрешение на доступ к микрофону
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) { // разрешение дано, запрашиваем доступ к записи
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        20);
            } else {
                // Отказ в доступе
                finish();
            }
        } else if (requestCode == 20) { // доступ к записи файлов
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) { // разрешение дано, запрашиваем доступ геолокации
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        30);
            } else {
                // Отказ в доступе
                finish();
            }
        } else if (requestCode == 30) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) { // разрешение дано, идем к получению данных с сервера
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_NETWORK_STATE},
                        40);
            } else {
                // Отказ в доступе
                finish();
            }
        } else if (requestCode == 40) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) { // разрешение дано, идем к получению данных с сервера
                new GetAppData().execute();
            } else {
                // Отказ в доступе
                finish();
            }
        }
    }

    public void startAppActivity() {
        // в зависимости от того, какой раз открываем приложение - отправляем на определенный экран
        if (prefManager.isFirstTimeLaunch()) {
            Intent intent = new Intent(SplashActivity.this, LoginUser.class);
            startActivity(intent);
            finish();
        } else {
            Intent intent = new Intent(SplashActivity.this, Audio.class);
            startActivity(intent);
            finish();
        }
    }


    //LOGIN USER ASYNC CLASS
    public class GetAppData extends AsyncTask<String, Void, String> {
        // Сначала покажем диалоговое окно прогресса
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(SplashActivity.this);
            mProgressDialog.setMessage("Регистрация. Подождите...");
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {

            // строка запроса
            String source = "http://service.retail-info.ru/sound1.json";

            OkHttpClient client = new OkHttpClient();
            // отправляем запрос
            Request request = new Request.Builder()
                    .url(source)
                    .build();

            String responseData = "";
            JSONObject jsonObject;

            // получаем данные от api
            try {
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                responseData = response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // проверяем полученные данные
            try {

                jsonObject = new JSONObject(responseData);
                boolean success = jsonObject.getBoolean("success");
                // если всё гуд
                if (success == true) {
                    // данные вернулись
                    api = jsonObject.getString("url");
                    radius = jsonObject.getString("radius");
                    time = jsonObject.getString("time");
                } else {
//                     все не гуд
                    api = null;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return api;
        }

        @Override
        protected void onPostExecute(String result) {
            // закрываем диалоговое окно с индикатором прогресса
            mProgressDialog.dismiss();
            if (api != null) {
                PrefManager prefManager = new PrefManager(SplashActivity.this);
                // сохраняем все данные
                prefManager.setUserProfileApi(api);
                prefManager.setUserProfileRadius(radius);
                prefManager.setUserProfileTime(time);

                // запускаем метод перехода
                startAppActivity();
            } else {
                // нет связи с интернетом, данные не вернулись просто запускаем метод перехода
                startAppActivity();
            }
            super.onPostExecute(result);
        }
    }
}
