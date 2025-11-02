package com.example.to_do_app.activitys;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager.widget.ViewPager;

import com.example.to_do_app.R;
import com.example.to_do_app.adapters.ViewPagerAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * MainActivity: không có menu đăng xuất. Nếu user null -> redirect StartAppActivity.
 */
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private ViewPager myViewPager;
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(MainActivity.this, StartAppActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Áp dụng window insets cho root view
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main2), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bottomNav = findViewById(R.id.bottom_nav);
        myViewPager = findViewById(R.id.view_pager);
        setupViewPager();

        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.action_home) { myViewPager.setCurrentItem(0); return true; }
                if (id == R.id.action_add) { myViewPager.setCurrentItem(1); return true; }
                if (id == R.id.action_profile) { myViewPager.setCurrentItem(2); return true; }
                return false;
            }
        });

        myViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
            @Override public void onPageSelected(int position) {
                switch (position) {
                    case 0: bottomNav.getMenu().findItem(R.id.action_home).setChecked(true); break;
                    case 1: bottomNav.getMenu().findItem(R.id.action_add).setChecked(true); break;
                    case 2: bottomNav.getMenu().findItem(R.id.action_profile).setChecked(true); break;
                }
            }
            @Override public void onPageScrollStateChanged(int state) {}
        });
    }

    private void setupViewPager() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(
                getSupportFragmentManager(),
                ViewPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
        );
        myViewPager.setAdapter(adapter);
    }
}