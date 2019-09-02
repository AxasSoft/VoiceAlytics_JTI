package com.example.apofe.voicealytics;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pinball83.maskededittext.MaskedEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginUser extends AppCompatActivity {

    private ProgressDialog mProgressDialog;
    private String token; // переменная в которой храним токен пользователя (получаем от API)
    private MaskedEditText editPhone; // поле ввода номера телефона
    private EditText editPass; // поле ввода пароля
    private String lng;
    private String lat;
    private PrefManager prefManager;
    private String api; // url из настроек


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_user);
        getSupportActionBar().setElevation(10); // Задаем тень для ActionBar
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM); //разрешаем смену ActionBar
        getSupportActionBar().setCustomView(R.layout.appbar_with_return_img); //Указываем профиль с которого подтянется новый ActionBar
        TextView appbarName = (TextView) findViewById(R.id.appbar_name);
        appbarName.setText(getResources().getString(R.string.registration_appbar)); //Текст который будет в appbar

        editPhone = (MaskedEditText) findViewById(R.id.phone_number);
        editPass = findViewById(R.id.editPassword);


        prefManager = new PrefManager(this);
        api = prefManager.isUserApi();

        Button loginButton = (Button) findViewById(R.id.loginButton);


        loginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                String phone = editPhone.getText().toString();
                phone.replace("+","");
                phone.replaceAll("-", "");
                phone.replace("+", "");
                phone.replace("(", "");
                phone.replace(")", "");

                new LoginRegisterUser().execute(phone);

            }
        });

    }

    // Кнопка назад в левом верхнем углу
    public void appbarExitButton(View view) { //При нажатии назад, закрываем экран
        finish();
    }

    // запускаем класс обращения к API
    public void registrationUser(View view) {
        new LoginRegisterUser().execute();
    }


    //LOGIN USER ASYNC CLASS
    public class LoginRegisterUser extends AsyncTask<String, Void, String> {
        // Сначала покажем диалоговое окно прогресса
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(LoginUser.this);
            mProgressDialog.setMessage("Регистрация. Подождите...");
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {

            // строка запроса
            String source = api + "phone/login";
            System.out.println(source);
            // парсим телефон в нужный формат
            String phone = editPhone.getText().toString();
            phone = phone.replace("+","");
            phone = phone.replaceAll("-", "");
            phone = phone.replace("+", "");
            phone = phone.replace("(", "");
            phone = phone.replace(")", "");
            System.out.println(phone);
            System.out.println(editPass.getText());

            // билдим данные для пост запроса
            FormBody.Builder formBuilder = new FormBody.Builder()
                    .add("phone", phone);
            formBuilder.add("pass", editPass.getText().toString());
            RequestBody formBody = formBuilder.build();

            OkHttpClient client = new OkHttpClient();
            // отправляем запрос
            Request request = new Request.Builder()
                    .url(source)
                    .post(formBody)
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
                System.out.println(responseData);

                jsonObject = new JSONObject(responseData);
                boolean success = jsonObject.getBoolean("success");
                // если всё гуд
                if (success == true) {
                    // данные вернулись
                    // забираем token
                    token = jsonObject.getString("token");
                    prefManager.setUserCheckGeo(jsonObject.getBoolean("mobile"));
                    // забираем координаты
                    JSONObject jsonObject1 = jsonObject.getJSONObject("pos");
                    lng = jsonObject1.getString("lng");
                    lat = jsonObject1.getString("lat");

                } else {
//                     все не гуд
                    token = null;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return token;
        }

        @Override
        protected void onPostExecute(String result) {
            // закрываем диалоговое окно с индикатором прогресса
            mProgressDialog.dismiss();
            if (token != null) {
                // сохраняем все данные
                prefManager.setUserProfilePhoneNumber(editPhone.getText().toString());
                prefManager.setUserProfileKey(token);
                prefManager.setUserProfileLat(lat);
                prefManager.setUserProfileLng(lng);
                prefManager.setFirstTimeLaunch(false);
                // переходим на основной экран
                Intent intent = new Intent(LoginUser.this, Audio.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(getApplicationContext(), "Неверный логин или пароль.\nПожалуйста измените данные и повторите вход.", Toast.LENGTH_SHORT).show();
            }
            super.onPostExecute(result);
        }
    }
}