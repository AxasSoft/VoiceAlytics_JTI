package com.example.apofe.voicealytics;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.os.SystemClock.sleep;

public class BootBroadcast extends BroadcastReceiver {
    // запускаем чекалку через bootBroadcast

    private final MediaType MEDIA_TYPE_MP3 = MediaType.parse("multipart/form-data"); // тип файла для отправки
    private PrefManager prefManager;
    private List<String> recordData = new ArrayList<String>(); // данные записи для отправки (имя файла / время / длина / позиция)
    private List<String> listToUpload = new ArrayList<String>(); // список файлов для загрузки
    private List<String> newListToUpload = new ArrayList<String>(); // обновленный список файлов для загрузки
    private File fileAudio; // загружаемый файл
    private String idRec; // айди записи полученный от сервера
    private String uploadStatus; // статус загрузки
    private Context context;

    // проверка связи с интернетом
    public static boolean hasConnection(final Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiInfo != null && wifiInfo.isConnected()) {
            return true;
        }
        wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (wifiInfo != null && wifiInfo.isConnected()) {
            return true;
        }
        wifiInfo = cm.getActiveNetworkInfo();
        if (wifiInfo != null && wifiInfo.isConnected()) {
            return true;
        }
        return false;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        prefManager = new PrefManager(context);
        this.context = context;
        if (hasConnection(context)) {
            // если интернет есть - загружаем файл
            getListToUpload();
            uploadNextRecord();
        }
    }

    public void getListToUpload() {
        // список читаем из файла
        try (BufferedReader br = new BufferedReader(new FileReader(Environment.getExternalStorageDirectory() + "/voiceAlytics/" + prefManager.isUserPhoneNumber() + "recordLists.txt"))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            String everything = sb.toString();
            everything = everything.trim();
            // заносим список файлов в список. Делаем его linked, т.к. иначе не поддерживается remove
            listToUpload = new LinkedList<String>(Arrays.asList(everything.split("~~~")));

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // метод загрузки файла
    public void uploadNextRecord() {
        // проверяем есть ли файлы для загрузки
        if (listToUpload != null && listToUpload.size() > 0 && !listToUpload.get(0).equals("")) {
            // если да, получаем первую запись
            recordData = Arrays.asList(listToUpload.get(0).split("///"));
            // проверяем есть ли файл вообще
            File f = new File(recordData.get(0));
            if (!f.exists()) {
                // если файла нет удаляем запись
                try {
                    listToUpload.remove(0);
                    // обновляем документ со списком файлов для загрузки
                    FileWriter writer = new FileWriter(Environment.getExternalStorageDirectory() + "/voiceAlytics/" + prefManager.isUserPhoneNumber() + "recordLists.txt", false);
                    BufferedWriter bufferWriter = new BufferedWriter(writer);
                    // пишем нужные данные
                    bufferWriter.write(android.text.TextUtils.join("~~~", listToUpload));
                    bufferWriter.close();
                    // запускаем метод для чтения из файла, чтобы обновить данные
                    getListToUpload();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // перезапускаем метод, чтобы получить новый файл
                uploadNextRecord();
            } else {
                // если файл найден запускаем метод отправки
                new UploadFile().execute();
            }

        }
    }

    //UPLOAD MP3 ASYNC CLASS
    public class UploadFile extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            // строка запроса
            String source = "https://voicealytics.tk/json/put";
            System.out.println(source);
            fileAudio = new File(recordData.get(0));

            // в теле запроса передаем файл и доп параметры
            RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("file", recordData.get(0), RequestBody.create(MEDIA_TYPE_MP3, fileAudio))
                    .addFormDataPart("pos", recordData.get(1))
                    .addFormDataPart("token", prefManager.isUserKey())
                    .addFormDataPart("localTime", recordData.get(2))
                    .addFormDataPart("time", recordData.get(3))
                    .addFormDataPart("dur", recordData.get(4))
                    .addFormDataPart("version", context.getResources().getString(R.string.app_version))
                    .build();

            OkHttpClient client = new OkHttpClient();
            // отправляем запрос
            Request request = new Request.Builder()
                    .url(source)
                    .post(requestBody)
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
                System.out.println(jsonObject);
                boolean success = jsonObject.getBoolean("success");
                // если всё гуд
                if (success == true) {
                    // файл загружен
                    uploadStatus = responseData;
                    idRec = jsonObject.getString("id_rec");
                } else {
//                     все не гуд
                    uploadStatus = responseData;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return uploadStatus;
        }

        @Override
        protected void onPostExecute(String result) {
            // проверяем получен ли файл сервером
            if (uploadStatus != null && !idRec.equals("-1")) {
                // если файл загружен
//                Toast.makeText(getApplicationContext(), "файл " + recordData.get(0) + " успешно загружен", Toast.LENGTH_SHORT).show();

                // проверяем есть ли записи для отправки
                if ((listToUpload != null) && (listToUpload.size() > 0)) {
                    try {
                        // удаляем запись, которую уже отправили
                        getListToUpload();
                        sleep(1000);
                        listToUpload.remove(0);
                        // удаляем сам файл который отправили
                        fileAudio.delete();
                        // добавляем пустой элемент массива, чтобы не было слияния
                        listToUpload.add("");
                        // обновляем документ со списком файлов для загрузки
                        FileWriter writer = new FileWriter(Environment.getExternalStorageDirectory() + "/voiceAlytics/" + prefManager.isUserPhoneNumber() + "recordLists.txt", false);
                        BufferedWriter bufferWriter = new BufferedWriter(writer);
                        // пишем нужные данные
                        bufferWriter.write(android.text.TextUtils.join("~~~", listToUpload));
                        bufferWriter.close();
                        // запускаем метод для чтения из файла, чтобы обновить данные
                        getListToUpload();


                        // путь к файлу со списком уже загруженных файлов
                        FileWriter uploadWriter = new FileWriter(Environment.getExternalStorageDirectory() + "/voiceAlytics/" + prefManager.isUserPhoneNumber() + "uploadRecordLists.txt", true);
                        BufferedWriter uploadBufferWriter = new BufferedWriter(uploadWriter);
                        // пишем нужные данные (имя файла ///широта;долгота///дата записи///длительность записи)
                        uploadBufferWriter.write(recordData.get(0) + "///" + idRec + "~~~");
                        uploadBufferWriter.close();

                        // отправляем сообщение, чтобы изменить кол-во файлов
                        context.startService(new Intent(context, UploadRecordService.class));
//                        sendBroadcast(true);

                        // если у нас больше нет файлов для загрузки - выводим сообщение и закрываем сервис
                        if (listToUpload.get(0).equals("")) {
//                            Toast.makeText(getApplicationContext(), "Все файлы успешно загружены", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
//                    Toast.makeText(getApplicationContext(), "Все файлы успешно загружены", Toast.LENGTH_SHORT).show();
                }
            } else {
//                Toast.makeText(getApplicationContext(), "Невозможно установить связь с сервером, пожалуйста попробуйте позже.", Toast.LENGTH_SHORT).show();
            }
            super.onPostExecute(result);
        }
    }
}
