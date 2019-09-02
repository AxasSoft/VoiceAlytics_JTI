package com.example.apofe.voicealytics;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

//МАРКИ
public class MarksFragment extends Fragment {

    private WebView webView;
    private ProgressDialog mProgressDialog;
    private String marks;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView =
                inflater.inflate(R.layout.marks_fragment, container, false);
        // вьюха которая отображает данные
        webView = rootView.findViewById(R.id.statistics_text_view);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        // Сначала покажем диалоговое окно прогресса
        mProgressDialog = new ProgressDialog(getContext());
        mProgressDialog.setMessage("Загрузка. Подождите...");
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();

        // запрашиваем марку с сервера
        Thread myThread = new Thread( // создаём новый поток
                new Runnable() { // описываем объект Runnable в конструкторе
                    public void run() {
                        doInBackground();//запускаем метод получения данных
                    }
                }
        );
        myThread.start();

        return rootView;
    }

    // работаем в отдельном потоке
    protected String doInBackground() {

        // строка запроса
        String source = "https://rap.smart-audio.kz/info";

        OkHttpClient client = new OkHttpClient();
        // отправляем запрос
        Request request = new Request.Builder()
                .url(source)
                .build();
        // получаем данные от api
        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            marks = "good";
        } catch (IOException e) {
            e.printStackTrace();
        }
        onPostExecute(marks);
        return marks;
    }

    //
    protected void onPostExecute(String result) {
        // закрываем диалоговое окно с индикатором прогресса
        mProgressDialog.dismiss();
        // статистика вернулась, выводим ее в textView
        if (marks == "good") {
            // чтобы не было ошибок с вебом, запрашиваем через пост
            webView.post(new Runnable() {
                @Override
                public void run() {
                    webView.loadUrl("https://rap.smart-audio.kz/info");
                }
            });

        } else {
            // нет связи с интернетом, данные не удалось получить
            Toast.makeText(getContext(), "Невозможно установить связь с сервером. Пожалуйста, попробуйте позже.", Toast.LENGTH_SHORT).show();
        }

    }
}