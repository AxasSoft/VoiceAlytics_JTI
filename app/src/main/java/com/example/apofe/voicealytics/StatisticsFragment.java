package com.example.apofe.voicealytics;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


// СТАТИСТИКА
public class StatisticsFragment extends Fragment {

    private WebView webView;
    private ProgressDialog mProgressDialog;
    private String statistics;
    private PrefManager prefManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView =
                inflater.inflate(R.layout.statistics_fragment, container, false);
        prefManager = new PrefManager(getContext());
        // вьюха которая отображает данные
        webView = rootView.findViewById(R.id.statistics_text_view);
        // Сначала покажем диалоговое окно прогресса
        mProgressDialog = new ProgressDialog(getContext());
        mProgressDialog.setMessage("Загружаем статистику. Подождите...");
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();

        // запрашиваем данные статистики с сервера
        // запрашиваем марку с сервера
        Thread myThread = new Thread( // создаём новый поток
                new Runnable() { // описываем объект Runnable в конструкторе
                    public void run() {
                        doInBackground();// получаем данные от сервера
                    }
                }
        );
        // стартуем поток
        myThread.start();

        return rootView;
    }


    protected String doInBackground() {
        // строка запроса
        String source = prefManager.isUserApi() + "stat?token=" + prefManager.isUserKey();

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
                statistics = jsonObject.getString("html");
            } else {
//                     все не гуд
                statistics = null;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        onPostExecute(statistics);
        return statistics;
    }

    protected void onPostExecute(String result) {
        // закрываем диалоговое окно с индикатором прогресса
        mProgressDialog.dismiss();
        // статистика вернулась, выводим ее в textView
        if (statistics != null) {
            // чтобы не было ошибок с вебом, запрашиваем через пост
            webView.post(new Runnable() {
                @Override
                public void run() {
                    webView.loadDataWithBaseURL(null, statistics, "text/html", "utf-8", null);
                }
            });
        } else {
            // нет связи с интернетом, данные не удалось получить
            Toast.makeText(getContext(), "Невозможно установить связь с сервером. Пожалуйста, попробуйте позже.", Toast.LENGTH_SHORT).show();
        }
    }
}

