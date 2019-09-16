package com.example.apofe.voicealytics;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.naman14.androidlame.AndroidLame;
import com.naman14.androidlame.LameBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import com.example.apofe.voicealytics.sphinx.SpeechRecognizer;
import com.example.apofe.voicealytics.sphinx.SpeechRecognizerSetup;

import static android.os.SystemClock.sleep;
import static android.widget.Toast.makeText;

// распознавание голоса


// НОВАЯ ЗАПИСЬ
public class AudioFragment extends Fragment implements
        RecognitionListener {

    private static final long MINIMUM_DISTANCE_FOR_UPDATES = 1; // в метрах
    private static final long MINIMUM_TIME_BETWEEN_UPDATES = 1000; // в мс
    List<String> listToUpload = new ArrayList<String>();
    TextView geopos;
    FileOutputStream outputStream;
    boolean isRecording = false;
    public static List<Integer> bufferList = new ArrayList<Integer>();
    public static List<byte[]> mp3List = new ArrayList<byte[]>();
    boolean isKeyWordSayed = false;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private Button startRecordButton; // начать запись
    private Button stopRecordButton; // остановить запись
    private Button sendRecordNow; // отправка записи и начало новой
    private File fileAudio; // сам файл
    private String fileName; // имя файла
    private PrefManager prefManager;
    private Context context;
    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // формат времени, для отправки на сервер
    private SimpleDateFormat formatterGMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // формат времени, для отправки на сервер
    private double latAccurancy = 0; // широта изначально 0, т.к. данных может не быть
    private double lngAccurancy = 0; // долгота изначально 0, т.к. данных может не быть
    private String dateRecord; // время когда была сделана запись
    private String GMTDateRecord; // дата по Гринвичу
    private LinearLayout uploadLinear; // блок с вьюхой и кнопкой для загрузки
    private TextView countFileToUpload; // кол-во файлов для загрузки
    private Chronometer chronometer; // штука где выводим время записи
    public static long recordMillisTime; // для отслеживания записи менее 10 секунд
    // эта простая и крутая штука - слушатель сервиса
    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // обновляем текст и поля на экране
            getListToUpload();
        }
    };


    // распознавание голоса
    /* Named searches allow to quickly reconfigure the decoder */
    private static final String KWS_SEARCH = "wakeup";
    private static final String FORECAST_SEARCH = "forecast";
    private static final String DIGITS_SEARCH = "digits";
    private static final String PHONE_SEARCH = "phones";
    private static final String MENU_SEARCH = "menu";

    /* Keyword we are looking for to activate menu */
    private static final String KEYPHRASE = "нож";

    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    private SpeechRecognizer recognizer;
    private HashMap<String, Integer> captions;
    private TextView textView;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.audio_fragment, container, false);
        context = getContext();
        prefManager = new PrefManager(context);

        // подключаем определение гео
        mLocationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        mLocationListener = new MyLocationListener();
        geopos = rootView.findViewById(R.id.geopos);
        // Регистрируемся для обновлений
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) // разрешение на геопозицию
                == PackageManager.PERMISSION_GRANTED) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    MINIMUM_TIME_BETWEEN_UPDATES, MINIMUM_DISTANCE_FOR_UPDATES,
                    mLocationListener);
            // Получаем текущие координаты при запуске
            if (isGeoDisabled()) {
                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                geopos.setText("Lat:-1\nLng:-1");
            } else {
                while (latAccurancy == 0 && lngAccurancy == 0) {
                    showCurrentLocation();
                }
                geopos.setText("Lat:" + latAccurancy + "\nLng:" + lngAccurancy);
            }


            // Recognizer initialization is a time-consuming and it involves IO,
            // so we execute it in async task
            new SetupTask(this).execute();
        }


        LocalBroadcastManager.getInstance(context).registerReceiver(bReceiver, new IntentFilter("message"));

        // текстовая вьюха и блок для для загрузки на сервер
        uploadLinear = rootView.findViewById(R.id.upload_file_linear);
        countFileToUpload = rootView.findViewById(R.id.count_file_to_upload);
        uploadLinear.setVisibility(View.GONE);

        // изначально получаем список уже сделанных записей для того чтобы можно было их отправить
        getListToUpload();

        // кнопка начать работу
        startRecordButton = (Button) rootView.findViewById(R.id.start_new_audio);
        startRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // вызываем метод
                isKeyWordSayed = false;
                startWork();
            }
        });

        // кнопка закончить работу
        stopRecordButton = (Button) rootView.findViewById(R.id.stop_new_audio);
        stopRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // вызываем метод
                finishWork();
            }
        });

        // кнопка отправить запись и начанть новую
        sendRecordNow = (Button) rootView.findViewById(R.id.send_audio_now);
        sendRecordNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // вызываем метод
                // проверяем длительность записи
                isKeyWordSayed = true;
                if (recordMillisTime < 10000) {
                    // если меньше 10 секунд
                    Toast.makeText(context, "Запись не может быть менее 10 секунд", Toast.LENGTH_SHORT).show();
                } else {
                    stopRecord();
                    sleep(1000);
                    startRecord();
                }
            }
        });

        // вьюха для отображения времени записи
        chronometer = rootView.findViewById(R.id.record_chronometer);
        // переопределяем метод, чтобы считать сколько милисекунд идет запись
        chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                // пишем милисекунды в переменную
                recordMillisTime = SystemClock.elapsedRealtime() - chronometer.getBase();
                // поскольку у нас один фиг работает счетчик, то тут сразу и геопозицию проверять будем =)
                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                boolean geoStatus = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                if (Math.abs(Math.round((Double.parseDouble(prefManager.isUserlat()) - latAccurancy) * 100000)) >= Integer.parseInt(prefManager.isUserRadius()) || Math.abs(Math.round((Double.parseDouble(prefManager.isUserLng()) - lngAccurancy) * 100000)) >= Integer.parseInt((prefManager.isUserRadius()))) {
                    geoStatus = false;
                }
                if (geoStatus == false) {
                    // если мы вышли из зоны записи, или отключили гео
                }
            }
        });

        startUploadService();

        // т.к. это фрагммент - возвращаем вьюху
        return rootView;
    }


    // метод который запускает сервис для загрузки
    public void startUploadService() {
        Intent myIntent = new Intent(context, BootBroadcast.class);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.SECOND, 10);

        // сервис стартует через alarmManager
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                getContext(), 1, myIntent, 0);
        AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        long interval = 5 * 60 * 1000; //
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(), interval, pendingIntent);
    }


    // метод получения списка файлов для загрузки
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
            // если элементы в списке есть, запускаем сервис загрузки
            if (listToUpload.size() > 0 && !listToUpload.get(0).equals("")) {

                // пишем количество файлов для загрузки
                countFileToUpload.setText(getResources().getString(R.string.files_to_upload) + listToUpload.size());
                // показываем блок загрузки
                uploadLinear.setVisibility(View.VISIBLE);
                if (!isMyServiceRunning(UploadRecordService.class)) {
//                    Toast.makeText(getContext(), "Сервис запущен.", Toast.LENGTH_SHORT).show();
//                    startUploadService();
                } else {

                }
            } else {
                // если данных нет, то прячем блок загрузки
                uploadLinear.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // начинаем работу
    public void startWork() {
        //recognizer.stop();
        if (showCurrentLocation()) {
            // с помощью нехитрых преобразований мы получаем отклонение по широте и долготе от изначальной точки
        }
        // если нет проверки геопозиции
        if (prefManager.isCheckGeo() && lngAccurancy != 0 && latAccurancy != 0) {
            startRecord();
            // прячем кнопку начать запись, показываем остановить
            startRecordButton.setVisibility(View.GONE);
            stopRecordButton.setVisibility(View.VISIBLE);
            sendRecordNow.setVisibility(View.VISIBLE);

        } else {
            // перед записью проверяем геопозицию
            if (Math.abs(Math.round((Double.parseDouble(prefManager.isUserlat()) - latAccurancy) * 100000)) >= Integer.parseInt(prefManager.isUserRadius()) || Math.abs(Math.round((Double.parseDouble(prefManager.isUserLng()) - lngAccurancy) * 100000)) >= Integer.parseInt((prefManager.isUserRadius()))) {
                Toast.makeText(getContext(), "Запись невозможна, вы не находитесь в нужной геопозиции или выключен GPS. Отклонение lat:" + Math.abs(Math.round((Double.parseDouble(prefManager.isUserlat()) - latAccurancy) * 100000)) + " lng:" + Math.abs(Math.round((Double.parseDouble(prefManager.isUserLng()) - lngAccurancy) * 100000)), Toast.LENGTH_SHORT).show();
            } else {
                startRecord();
                // прячем кнопку начать запись, показываем остановить
                startRecordButton.setVisibility(View.GONE);
                stopRecordButton.setVisibility(View.VISIBLE);
                sendRecordNow.setVisibility(View.VISIBLE);
            }
        }

    }

    // закончили работу
    public void finishWork() {
        // меняем переменную что запись не ведется
        prefManager.setUserRecorded(false);
        // останавливаем запись
        // переключаем слушатель, для остановки записи
        isRecording = false;
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.stop();
        // прячем кнопку остановить запись, показываем начать
        startRecordButton.setVisibility(View.VISIBLE);
        stopRecordButton.setVisibility(View.GONE);
        sendRecordNow.setVisibility(View.GONE);
        sleep(700);
        switchSearch("wakeup");

    }

    // начинаем новую запись
    public void startRecord() {
        // проверяем, есть ли данные геолокации
        if (showCurrentLocation()) {

        }
        // если нет проверки геопозиции
        if (prefManager.isCheckGeo() && lngAccurancy != 0 && latAccurancy != 0) {
            try {
                // переключаем листенер, для того чтобы запись не останавливалась
                isRecording = true;
                prefManager.setUserRecorded(true);
                // начинаем отсчет времени
                chronometer.setBase(SystemClock.elapsedRealtime());
                chronometer.start();
//                Toast.makeText(getContext(), "Запись начата", Toast.LENGTH_SHORT).show();
                // получаем путь к новому файлу
                fileName = getFilePath();
                // Запускаем основной метод
                Thread myThread = new Thread( // создаём новый поток
                        new Runnable() { // описываем объект Runnable в конструкторе
                            public void run() {
                                if (isKeyWordSayed)
                                startRecordingSecond();
                                else
                                    startRecording();
                            }
                        }
                );
                // стартуем поток
                myThread.start();

                // меняем переменную в настройках, от нее зависит работа перехода по меню (если идет запись - перехода нет)

            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        } else {

            // перед записью проверяем геопозицию
            if (Math.abs(Math.round((Double.parseDouble(prefManager.isUserlat()) - latAccurancy) * 100000)) >= Integer.parseInt(prefManager.isUserRadius()) || Math.abs(Math.round((Double.parseDouble(prefManager.isUserLng()) - lngAccurancy) * 100000)) >= Integer.parseInt((prefManager.isUserRadius()))) {
                Toast.makeText(getContext(), "Запись невозможна, вы не находитесь в нужной геопозиции или выключен GPS.", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    // переключаем листенер, для того чтобы запись не останавливалась
                    isRecording = true;
                    prefManager.setUserRecorded(true);
                    // начинаем отсчет времени
                    chronometer.setBase(SystemClock.elapsedRealtime());
                    chronometer.start();
//                Toast.makeText(getContext(), "Запись начата", Toast.LENGTH_SHORT).show();
                    // получаем путь к новому файлу
                    fileName = getFilePath();
                    // Запускаем основной метод
                    Thread myThread = new Thread( // создаём новый поток
                            new Runnable() { // описываем объект Runnable в конструкторе
                                public void run() {
                                    startRecording();
                                }
                            }
                    );
                    // стартуем поток
                    myThread.start();

                    // меняем переменную в настройках, от нее зависит работа перехода по меню (если идет запись - перехода нет)

                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // останавливаем запись
    public void stopRecord() {
        // проверяем длительность записи
        if (recordMillisTime < 10000) {
            // если меньше 10 секунд
            Toast.makeText(context, "Запись не может быть менее 10 секунд", Toast.LENGTH_SHORT).show();
        } else if (isKeyWordSayed & !isRecording){ // если запись идет более 10 секунд
            /*
            Механизм текущей ситуации таков: запись будет идти до ключевого слова. После ключевого слова
            проходит ещё 15 секунд и запись останавилвается. Переменные, в которые сохраняется звук
            были вынесены за пределы startRecording, чтоб не терялись данные
             */
            for (int i = 0; i < mp3List.size(); i++) {
                try {
                    outputStream.write(mp3List.get(i), 0, bufferList.get(i));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //Очистка данных перед сохранением файла, чтоб оно не мешало новым записям
            isRecording = false;
            mp3List.clear();
            bufferList.clear();
            // проверяем мы просто сохраняем новый файл, или останавливаем работу вообще
            if (prefManager.isUserRecorded()) {
                // записываем данные аудиозаписи в текстовый док, для того, чтобы потом выгружать их отсюда на сервер
                try {
                    // путь к файлу со списком для загрузки
                    FileWriter writer = new FileWriter(Environment.getExternalStorageDirectory() + "/voiceAlytics/" + prefManager.isUserPhoneNumber() + "recordLists.txt", true);
                    BufferedWriter bufferWriter = new BufferedWriter(writer);
                    // пишем нужные данные (имя файла ///широта;долгота///дата записи///длительность записи)
                    if (recordMillisTime > Integer.parseInt(prefManager.isUserTime()) * 1000) {
                        recordMillisTime = Integer.parseInt(prefManager.isUserTime()) * 1000;
                    }


                    bufferWriter.write(fileName + "///" + lngAccurancy + ";" + latAccurancy + "///" + dateRecord + "///" + GMTDateRecord + "///" + (recordMillisTime / 1000) + "~~~");
                    bufferWriter.close();
                    // обновляем данные для загрузки
                    getListToUpload();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {

            }
        }
    }

    // для каждой записи формируем путь и новое название с привязкой ко времени
    private String getFilePath() {
        // пишем дату записи
        Date recordDate = new Date();
        dateRecord = formatter.format(recordDate);
        formatterGMT.setTimeZone(TimeZone.getTimeZone("GMT"));
        GMTDateRecord = formatterGMT.format(recordDate);
        // даем название файлу
        fileName = "audio_" + dateRecord;
        fileName = fileName.replace(" ", "_");
        // создаем новый файл
        final File dir = new File(Environment.getExternalStorageDirectory() + "/voiceAlytics/");
        fileAudio = new File(Environment.getExternalStorageDirectory() + "/voiceAlytics/" + fileName + ".mp3");

        if (!dir.exists()) {
            if (!dir.mkdir()) {
                // если пути нет - создаем его
                dir.mkdirs();
            }
        }
        return (fileAudio.getPath());
    }


    private void startRecording() {
        //Это первая часть, для записи голоса он использует микрофон и буфер от либы. Я его в ней объявил статический, чтоб имелся доступ
        int minBuffer;
        int inSamplerate = 8000;
        AndroidLame androidLame = new AndroidLame();
        AudioRecord audioRecord = null;

        minBuffer = AudioRecord.getMinBufferSize(inSamplerate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        short[] buffer = new short[inSamplerate * 2 * 5];

        byte[] mp3buffer = new byte[(int) (1000)];

        try {//как мне кажется, проблема пустых записей стоит тут
            outputStream = new FileOutputStream(new File(fileName));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        androidLame = new LameBuilder()
                .setInSampleRate(inSamplerate)
                .setOutChannels(1)
                .setOutBitrate(32)
                .setOutSampleRate(inSamplerate)
                .build();
        int bytesRead = 0;

        while (isRecording) {
            bytesRead = SpeechRecognizer.recorder.read(buffer, 0, minBuffer);
            System.out.println(bytesRead);
            if (bytesRead > 0) {
                int bytesEncoded = androidLame.encode(buffer, buffer, bytesRead, mp3buffer);
                System.out.println("encod" + bytesEncoded);
                if (bytesEncoded > 0) {
                    if (recordMillisTime > Integer.parseInt(prefManager.isUserTime()) * 1000) {
                        mp3List.remove(0);
                        bufferList.remove(0);
                    }
                    mp3List.add(mp3buffer);
                    bufferList.add(bytesEncoded);

                    mp3buffer = new byte[(int) (1000)];//7200 + buffer.length * 2 * 1.25
                    System.out.println(mp3buffer.length);
                }
            }
            if (recordMillisTime >= 15000 & isKeyWordSayed){
                isRecording = false;
                Handler handler = new Handler(getActivity().getBaseContext().getMainLooper());
                handler.post( new Runnable() {
                    @Override
                    public void run() {
                        stopRecord();
                        finishWork();
                    }
                } );

            }
        }
        if (isKeyWordSayed) {
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
            }
        }
        androidLame.close();

    }
    public void startRecordingSecond() {
        //Запись второй части голосовухи
        int minBuffer;
        int inSamplerate = 8000;
        AudioRecord audioRecord;
        AndroidLame androidLame = new AndroidLame();

        minBuffer = AudioRecord.getMinBufferSize(inSamplerate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC, inSamplerate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, minBuffer * 2);

        short[] buffer = new short[inSamplerate * 2 * 5];

        byte[] mp3buffer = new byte[(int) (1000)];

        try {//как мне кажется, проблема пустых записей стоит тут
            outputStream = new FileOutputStream(new File(fileName));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        androidLame = new LameBuilder()
                .setInSampleRate(inSamplerate)
                .setOutChannels(1)
                .setOutBitrate(32)
                .setOutSampleRate(inSamplerate)
                .build();
        audioRecord.startRecording();

        int bytesRead = 0;



        while (isRecording) {
            bytesRead = audioRecord.read(buffer, 0, minBuffer);
            System.out.println(bytesRead);
            if (bytesRead > 0) {
                int bytesEncoded = androidLame.encode(buffer, buffer, bytesRead, mp3buffer);
                System.out.println("encod" + bytesEncoded);
                if (bytesEncoded > 0) {
                    if (recordMillisTime > Integer.parseInt(prefManager.isUserTime()) * 1000) {
                        mp3List.remove(0);
                        bufferList.remove(0);
                    }
                    mp3List.add(mp3buffer);
                    bufferList.add(bytesEncoded);

                    mp3buffer = new byte[(int) (1000)];//7200 + buffer.length * 2 * 1.25
                    System.out.println(mp3buffer.length);
                }
            }
            if (recordMillisTime >= 15000 & isKeyWordSayed){
                isRecording = false;
                Handler handler = new Handler(getActivity().getBaseContext().getMainLooper());
                handler.post( new Runnable() {
                    @Override
                    public void run() {
                        stopRecord();
                        finishWork();
                    }
                } );

            }
        }

        audioRecord.stop();
        audioRecord.release();
        androidLame.close();

    }


    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    @Override
    public void onResume() {
        super.onResume();
        // Регистрируемся для обновлений
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) // разрешение на геопозицию
                == PackageManager.PERMISSION_GRANTED) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    MINIMUM_TIME_BETWEEN_UPDATES, MINIMUM_DISTANCE_FOR_UPDATES,
                    mLocationListener);

            // Получаем текущие координаты при запуске
            if (isGeoDisabled()) {
                geopos.setText("Lat:-1\nLng:-1");
            } else {
                while (latAccurancy == 0 && lngAccurancy == 0) {
                    showCurrentLocation();
                }
                geopos.setText("Lat:" + latAccurancy + "\nLng:" + lngAccurancy);
            }
        }
    }

    protected boolean showCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) // разрешение на геопозицию
                == PackageManager.PERMISSION_GRANTED) {
            Location location = mLocationManager
                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) {
                location = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }


            if (location != null) {
                latAccurancy = location.getLatitude();
                lngAccurancy = location.getLongitude();
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public boolean isGeoDisabled() {
        LocationManager mLocationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        boolean mIsGPSEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean mIsNetworkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean mIsGeoDisabled = !mIsGPSEnabled && !mIsNetworkEnabled;
        return mIsGeoDisabled;
    }

    // Прослушиваем изменения
    private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
        }

        public void onStatusChanged(String s, int i, Bundle b) {
        }

        public void onProviderDisabled(String s) {
        }

        public void onProviderEnabled(String s) {
        }
    }


    // распознавание голоса
    private class SetupTask extends AsyncTask<Void, Void, Exception> {
        WeakReference<AudioFragment> activityReference;

        SetupTask(AudioFragment activity) {
            this.activityReference = new WeakReference<>(activity);
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                Assets assets = new Assets(getActivity().getApplicationContext());
                File assetDir = assets.syncAssets();
                activityReference.get().setupRecognizer(assetDir);
            } catch (IOException e) {
                return e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Exception result) {
            if (result != null) {

//                ((TextView) activityReference.get().findViewById(R.id.caption_text))
//                        .setText("Failed to init recognizer " + result);
            } else {
                activityReference.get().switchSearch(KWS_SEARCH);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @Nullable String[] permissions, @Nullable int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Recognizer initialization is a time-consuming and it involves IO,
                // so we execute it in async task
                new SetupTask(this).execute();
            } else {
//                finish();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }

    /**
     * In partial result we get quick updates about current hypothesis. In
     * keyword spotting mode we can react here, in other modes we need to wait
     * for final result in onResult.
     */
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;
        //Тут происходит фиксация того факта, что слово было сказано и обнуляет счётчик времени
        stopRecord();
        isKeyWordSayed = true;
        recordMillisTime = 0;
        System.out.println(hypothesis);
        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE)) {
            makeText(getActivity().getApplicationContext(), "KEY PHRASE", Toast.LENGTH_LONG).show();

            if (prefManager.isUserRecorded()){
                recognizer.stop();
                sleep(700);
                startRecord();
            } else {
                recognizer.stop();
                sleep(700);
                startWork();
            }

//            switchSearch(MENU_SEARCH);
        } else if (text.equals(DIGITS_SEARCH))
            switchSearch(DIGITS_SEARCH);
        else if (text.equals(PHONE_SEARCH))
            switchSearch(PHONE_SEARCH);
        else if (text.equals(FORECAST_SEARCH))
            switchSearch(FORECAST_SEARCH);
//        else

            makeText(getActivity().getApplicationContext(), text, Toast.LENGTH_LONG).show();
    }

    /**
     * This callback is called when we stop the recognizer.
     */
    @Override
    public void onResult(Hypothesis hypothesis) {

        makeText(getActivity().getApplicationContext(), "тишина в эфире", Toast.LENGTH_LONG).show();
        if (hypothesis != null) {
            String text = hypothesis.getHypstr();
            makeText(getActivity().getApplicationContext(), text, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBeginningOfSpeech() {
    }

    /**
     * We stop recognizer here to get a final result
     */
    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(KWS_SEARCH)) {
            switchSearch(KWS_SEARCH);
        }

    }

    private void switchSearch(String searchName) {
        recognizer.stop();
        System.out.println(searchName);
        System.out.println("++++++++++++++++++++++++++++++++");

        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName.equals(KWS_SEARCH))
            recognizer.startListening(searchName, 2000);
        else
            recognizer.startListening(searchName, 2000);

//        String caption = getResources().getString(captions.get(searchName));

//        makeText(getActivity().getApplicationContext(), caption, Toast.LENGTH_LONG).show();
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "ru-ru-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-ru-ru.dict"))
                .setBoolean("-remove_noise", false)
                .setKeywordThreshold(1e-7f)
                .setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)

                .getRecognizer();
        recognizer.addListener(this);

        /* In your application you might not need to add all those searches.
          They are added here for demonstration. You can leave just one.
         */

        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

    }

    @Override
    public void onError(Exception error) {

        makeText(getActivity().getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();

    }

    @Override
    public void onTimeout() {
        switchSearch(KWS_SEARCH);
    }

}