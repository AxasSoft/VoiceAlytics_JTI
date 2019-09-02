package com.example.apofe.voicealytics;

import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


// МОИ ЗАПИСИ
public class AudioListFragment extends Fragment {
    List<String> uploadedList = new ArrayList<String>();
    ArrayList<HashMap<String, String>> arrayList = new ArrayList<>();
    private PrefManager prefManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.audio_list_fragment, container, false);

        prefManager = new PrefManager(getContext());
        // данные читаем из 2-х файлов. Сначала из тех, что не отправлены
        getListUploaded("recordLists.txt", 0);
        getListUploaded("uploadRecordLists.txt", 1);


        ListView listView = rootView.findViewById(R.id.listView);

        SimpleAdapter adapter = new SimpleAdapter(getContext(), arrayList, android.R.layout.simple_list_item_2,
                new String[]{"fileName", "fileId"},
                new int[]{android.R.id.text1, android.R.id.text2});
        listView.setAdapter(adapter);

        return rootView;
    }


    // метод получения списка файлов для загрузки
    public void getListUploaded(String listFileName, int type) {
        // список читаем из файла
        try (BufferedReader br = new BufferedReader(new FileReader(Environment.getExternalStorageDirectory() + "/voiceAlytics/" + prefManager.isUserPhoneNumber() + listFileName))) {
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
            uploadedList = new LinkedList<String>(Arrays.asList(everything.split("~~~")));
            for (int i = uploadedList.size() - 1; i >= 0; i--) {
                if (uploadedList != null && !uploadedList.get(i).equals("")) {
                    List<String> recordData = Arrays.asList(uploadedList.get(i).split("///"));
                    HashMap<String, String> map = new HashMap<>();
                    String fileName = recordData.get(0).replace("audio_", "");
                    fileName = fileName.replace("_", " ");
                    fileName = fileName.replace(".mp3", "");
                    fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                    map.put("fileName", fileName);
                    // у нас есть два типа записей которые отправлены и которые еще нет. Вот здесь их и различаем
                    if (type == 0) {
                        map.put("fileId", "Запись еще не отправлена");
                    } else if (type == 1) {
                        map.put("fileId", "Отправлено (ID " + recordData.get(1) + ")");
                    }

                    arrayList.add(map);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
