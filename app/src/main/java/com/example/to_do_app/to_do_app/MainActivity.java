package com.example.to_do_app.to_do_app;

import android.os.Bundle;
import android.view.MenuItem;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.to_do_app.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private ViewPager2 myViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_nav);
        myViewPager = findViewById(R.id.view_pager);

        setupViewPager();

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_home) {
                myViewPager.setCurrentItem(0, true);
            } else if (id == R.id.action_add) {
                myViewPager.setCurrentItem(1, true);
            } else if (id == R.id.action_profile) {
                myViewPager.setCurrentItem(2, true);
            } else {
                return false;
            }
            return true;
        });

        // Khi vuốt ViewPager2 -> cập nhật BottomNavigationView
        myViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        bottomNav.getMenu().findItem(R.id.action_home).setChecked(true);
                        break;
                    case 1:
                        bottomNav.getMenu().findItem(R.id.action_add).setChecked(true);
                        break;
                    case 2:
                        bottomNav.getMenu().findItem(R.id.action_profile).setChecked(true);
                        break;
                }
            }
        });
    }

    private void setupViewPager() {
        // ViewPager2Adapter nhận FragmentActivity, không dùng FragmentManager hay BEHAVIOR
        ViewPager2Adapter adapter = new ViewPager2Adapter(this);
        myViewPager.setAdapter(adapter);
    }
}
