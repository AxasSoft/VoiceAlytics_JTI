package com.example.apofe.voicealytics;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.widget.TextView;


public class Audio extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private PrefManager prefManager;
    private Fragment fragment;
    private Class fragmentClass;
    // основной фрагмент где идет запись
    private Class fragmentClassRecord;
    private Fragment fragmentRecord;


    private PendingIntent uploadPendingIntent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // устанавливаем номер телефона в шторку
        prefManager = new PrefManager(this);
        TextView appbarPhone = (TextView) navigationView.getHeaderView(0).findViewById(R.id.appbar_phone);
        appbarPhone.setText(prefManager.isUserPhoneNumber());

        // показываем основной фрагмент
        fragmentRecord = null;
        fragmentClassRecord = AudioFragment.class;
        fragmentClass = fragmentClassRecord;
        try {
            fragmentRecord = (Fragment) fragmentClassRecord.newInstance();
            fragment = (Fragment) AudioListFragment.class.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Вставляем фрагмент, заменяя текущий фрагмент
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().remove(fragment);
        fragmentManager.beginTransaction().replace(R.id.container, fragmentRecord).commit();

    }

    // переопределяем кнопку назад
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (fragmentClass != AudioFragment.class) { // если у нас открыта не запись и мы нажимаем назад
            // чтобы кнопка назад работала корректно
            fragmentClass = fragmentClassRecord;
            // устанавливаем заголовок
            setTitle("Запись");
            // показываем основной фрагмент и удаляем любой другой
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().show(fragmentRecord).commit();
            fragmentManager.beginTransaction().remove(fragment).commit();
        } else {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // переходы по меню
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        // Перед переходом удаляем текущий фрагмент
        FragmentManager fragmentManagers = getSupportFragmentManager();
        fragmentManagers.beginTransaction().remove(fragment).commit();

        int id = item.getItemId();

        // новая запись
        if (id == R.id.nav_create_audio) {
            // чтобы корректно отрабатывала кнопка назад
            fragmentClass = fragmentClassRecord;
            // показываем основной фрагмент и удаляем любой другой
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().show(fragmentRecord).commit();
            fragmentManager.beginTransaction().remove(fragment).commit();
            // Выделяем выбранный пункт меню в шторке
            item.setChecked(true);
            // Выводим выбранный пункт в заголовке
            setTitle(item.getTitle());

        } else if (id == R.id.nav_audio_list) { // мои записи
            // выбираем нужный фрагмент
            fragmentClass = AudioListFragment.class;
            try {
                fragment = (Fragment) fragmentClass.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            // прячем основной фрагмент и добавляем на его место новый
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().hide(fragmentRecord).commit();
            fragmentManager.beginTransaction().add(R.id.container, fragment).commit();
            // Выделяем выбранный пункт меню в шторке
            item.setChecked(true);
            // Выводим выбранный пункт в заголовке
            setTitle(item.getTitle());

        } else if (id == R.id.nav_statistics) { // статистика
            // выбираем нужный фрагмент
            fragmentClass = StatisticsFragment.class;

            try {
                fragment = (Fragment) fragmentClass.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            // прячем основной фрагмент и добавляем на его место новый
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().hide(fragmentRecord).commit();
            fragmentManager.beginTransaction().add(R.id.container, fragment).commit();
            // Выделяем выбранный пункт меню в шторке
            item.setChecked(true);
            // Выводим выбранный пункт в заголовке
            setTitle(item.getTitle());

        } else if (id == R.id.nav_mark) { // марки
            // выбираем нужный фрагмент
            fragmentClass = MarksFragment.class;

            try {
                fragment = (Fragment) fragmentClass.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            // прячем основной фрагмент и добавляем на его место новый
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().hide(fragmentRecord).commit();
            fragmentManager.beginTransaction().add(R.id.container, fragment).commit();
            // Выделяем выбранный пункт меню в шторке
            item.setChecked(true);
            // Выводим выбранный пункт в заголовке
            setTitle(item.getTitle());

        } else if (id == R.id.nav_send_log) { // отправляем лог

        } else if (id == R.id.nav_exit) { // выход из профиля
            prefManager.editor.clear();
            prefManager.editor.commit();
            Intent intent = new Intent(Audio.this, SplashActivity.class);
            startActivity(intent);
            finish();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}