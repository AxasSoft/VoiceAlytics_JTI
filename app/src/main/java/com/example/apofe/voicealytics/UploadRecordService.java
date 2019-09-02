package com.example.apofe.voicealytics;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import okhttp3.MediaType;

public class UploadRecordService extends Service {
    public static final int notify = 5 * 1000;  // через какое время повторяем загрузку
    private final MediaType MEDIA_TYPE_MP3 = MediaType.parse("multipart/form-data"); // тип файла для отправки
    private PrefManager prefManager;
    private List<String> recordData = new ArrayList<String>(); // данные записи для отправки (имя файла / время / длина / позиция)
    private List<String> listToUpload = new ArrayList<String>(); // список файлов для загрузки
    private List<String> newListToUpload = new ArrayList<String>(); // обновленный список файлов для загрузки
    private File fileAudio; // загружаемый файл
    private String idRec; // айди записи полученный от сервера
    private String uploadStatus; // статус загрузки
    private Handler mHandler = new Handler();   // запускаем через Handler чтобы приложение не крашилось
    private Timer mTimer = null;    // таймер

    public UploadRecordService() {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //Toast.makeText(this, "Служба создана", Toast.LENGTH_SHORT).show();
    }


    // выполняем при запуске сервиса
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        prefManager = new PrefManager(this);
//        // получаем спиок файлов
//        getListToUpload();
//
//        if (mTimer != null) //если таймер уже создан - продолжаем работу
//            mTimer.cancel();
//        else // иначе пересоздаем таймер
//            mTimer = new Timer();
//        mTimer.scheduleAtFixedRate(new TimeDisplay(), 0, notify);
        sendBroadcast(true);
        return super.onStartCommand(intent, flags, startId);
    }
//
//
//    //задача которую выполняем каждые n секунд
//    class TimeDisplay extends TimerTask {
//        @Override
//        public void run() {
//            // запускаем
//            mHandler.post(new Runnable() {
//                @Override
//                public void run() {
//                    // проверяем есть ли интернет
//                    if (hasConnection(getApplicationContext())) {
//                        // если интернет есть - загружаем файл
//                        getListToUpload();
//                        uploadNextRecord();
//                    }
//                }
//            });
//        }
//    }
//
//    // получаем список файлов
//    public void getListToUpload() {
//        // список читаем из файла
//        try (BufferedReader br = new BufferedReader(new FileReader(Environment.getExternalStorageDirectory() + "/voiceAlytics/" + prefManager.isUserPhoneNumber() + "recordLists.txt"))) {
//            StringBuilder sb = new StringBuilder();
//            String line = br.readLine();
//            while (line != null) {
//                sb.append(line);
//                sb.append(System.lineSeparator());
//                line = br.readLine();
//            }
//            String everything = sb.toString();
//            everything = everything.trim();
//            // заносим список файлов в список. Делаем его linked, т.к. иначе не поддерживается remove
//            listToUpload = new LinkedList<String>(Arrays.asList(everything.split("~~~")));
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }
//
//    // проверка связи с интернетом
//    public static boolean hasConnection(final Context context) {
//        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
//        if (wifiInfo != null && wifiInfo.isConnected()) {
//            return true;
//        }
//        wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
//        if (wifiInfo != null && wifiInfo.isConnected()) {
//            return true;
//        }
//        wifiInfo = cm.getActiveNetworkInfo();
//        if (wifiInfo != null && wifiInfo.isConnected()) {
//            return true;
//        }
//        return false;
//    }
//
//    // метод загрузки файла
//    public void uploadNextRecord() {
//        // проверяем есть ли файлы для загрузки
//        if (listToUpload != null && listToUpload.size() > 0 && !listToUpload.get(0).equals("")) {
//            // если да, получаем первую запись
//            recordData = Arrays.asList(listToUpload.get(0).split("///"));
//            // проверяем есть ли файл вообще
//            File f = new File(recordData.get(0));
//            if (!f.exists()) {
//                // если файла нет удаляем запись
//                try {
//                    listToUpload.remove(0);
//                    // обновляем документ со списком файлов для загрузки
//                    FileWriter writer = new FileWriter(Environment.getExternalStorageDirectory() + "/voiceAlytics/" + prefManager.isUserPhoneNumber() + "recordLists.txt", false);
//                    BufferedWriter bufferWriter = new BufferedWriter(writer);
//                    // пишем нужные данные
//                    bufferWriter.write(android.text.TextUtils.join("~~~", listToUpload));
//                    bufferWriter.close();
//                    // запускаем метод для чтения из файла, чтобы обновить данные
//                    getListToUpload();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                // перезапускаем метод, чтобы получить новый файл
//                uploadNextRecord();
//            } else {
//                // если файл найден запускаем метод отправки
//                new UploadFile().execute();
//            }
//
//        }
//    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        mTimer.cancel();    // грохаем таймер
        //Toast.makeText(this, "Служба остановлена", Toast.LENGTH_SHORT).show();
    }

    // метод отправляющий данные в активити, благодаря ему она обновляет инфу без перезагрузки
    private void sendBroadcast(boolean success) {
        Intent intent = new Intent("message");
        intent.putExtra("success", success); // переменные просто чтобы было
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

//    //UPLOAD MP3 ASYNC CLASS
//    public class UploadFile extends AsyncTask<String, Void, String> {
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//        }
//
//        @Override
//        protected String doInBackground(String... params) {
//            // строка запроса
//            String source = prefManager.isUserApi() + "put";
//            fileAudio = new File(recordData.get(0));
//
//            // в теле запроса передаем файл и доп параметры
//            RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
//                    .addFormDataPart("file", recordData.get(0) , RequestBody.create(MEDIA_TYPE_MP3, fileAudio))
//                    .addFormDataPart("pos", recordData.get(1))
//                    .addFormDataPart("token", prefManager.isUserKey())
//                    .addFormDataPart("localTime", recordData.get(2))
//                    .addFormDataPart("time", recordData.get(3))
//                    .addFormDataPart("dur", recordData.get(4))
//                    .addFormDataPart("version", getResources().getString(R.string.app_version))
//                    .build();
//
//            OkHttpClient client = new OkHttpClient();
//            // отправляем запрос
//            Request request = new Request.Builder()
//                    .url(source)
//                    .post(requestBody)
//                    .build();
//
//            String responseData = "";
//            JSONObject jsonObject;
//
//            // получаем данные от api
//            try {
//                Response response = client.newCall(request).execute();
//                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
//                responseData = response.body().string();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            // проверяем полученные данные
//            try {
//
//                jsonObject = new JSONObject(responseData);
//                boolean success = jsonObject.getBoolean("success");
//                // если всё гуд
//                if (success == true) {
//                    // файл загружен
//                    uploadStatus = responseData;
//                    idRec = jsonObject.getString("id_rec");
//                } else {
////                     все не гуд
//                    uploadStatus = responseData;
//                }
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//            return uploadStatus;
//        }
//
//        @Override
//        protected void onPostExecute(String result) {
//            // проверяем получен ли файл сервером
//            if (uploadStatus != null && !idRec.equals("-1")) {
//                // если файл загружен
////                Toast.makeText(getApplicationContext(), "файл " + recordData.get(0) + " успешно загружен", Toast.LENGTH_SHORT).show();
//
//                // проверяем есть ли записи для отправки
//                if ((listToUpload != null) && (listToUpload.size() > 0)) {
//                    try {
//                        // удаляем запись, которую уже отправили
//                        getListToUpload();
//                        sleep(1000);
//                        listToUpload.remove(0);
//                        // удаляем сам файл который отправили
//                        fileAudio.delete();
//                        // добавляем пустой элемент массива, чтобы не было слияния
//                        listToUpload.add("");
//                        // обновляем документ со списком файлов для загрузки
//                        FileWriter writer = new FileWriter(Environment.getExternalStorageDirectory() + "/voiceAlytics/" + prefManager.isUserPhoneNumber() + "recordLists.txt", false);
//                        BufferedWriter bufferWriter = new BufferedWriter(writer);
//                        // пишем нужные данные
//                        bufferWriter.write(android.text.TextUtils.join("~~~", listToUpload));
//                        bufferWriter.close();
//                        // запускаем метод для чтения из файла, чтобы обновить данные
//                        getListToUpload();
//
//
//                        // путь к файлу со списком уже загруженных файлов
//                        FileWriter uploadWriter = new FileWriter(Environment.getExternalStorageDirectory() + "/voiceAlytics/" + prefManager.isUserPhoneNumber() + "uploadRecordLists.txt", true);
//                        BufferedWriter uploadBufferWriter = new BufferedWriter(uploadWriter);
//                        // пишем нужные данные (имя файла ///широта;долгота///дата записи///длительность записи)
//                        uploadBufferWriter.write(recordData.get(0) + "///" + idRec + "~~~");
//                        uploadBufferWriter.close();
//
//                        // отправляем сообщение, чтобы изменить кол-во файлов
//                        sendBroadcast(true);
//
//                        // если у нас больше нет файлов для загрузки - выводим сообщение и закрываем сервис
//                        if (listToUpload.get(0).equals("")) {
////                            Toast.makeText(getApplicationContext(), "Все файлы успешно загружены", Toast.LENGTH_SHORT).show();
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                } else {
////                    Toast.makeText(getApplicationContext(), "Все файлы успешно загружены", Toast.LENGTH_SHORT).show();
//                }
//            } else {
////                Toast.makeText(getApplicationContext(), "Невозможно установить связь с сервером, пожалуйста попробуйте позже.", Toast.LENGTH_SHORT).show();
//            }
//            super.onPostExecute(result);
//        }
//    }
}
