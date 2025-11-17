package com.example.to_do_app.activitys;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager.widget.ViewPager;

import com.example.to_do_app.R;
import com.example.to_do_app.adapters.ViewPagerAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class MainActivity extends AppCompatActivity {

    private static final String KEY_SELECTED_PAGE = "selected_page";
    private static final String PREFS = "todo_prefs";
    private static final String KEY_DISPLAY_NAME = "display_name";

    private BottomNavigationView bottomNav;
    private ViewPager myViewPager;
    private FirebaseAuth mAuth;
    private SharedPreferences prefs;
    private TextView tvGreeting;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        // forward to StartAppActivity for cold launcher start so StartApp is shown first
        Intent intent = getIntent();
        String action = (intent != null) ? intent.getAction() : null;
        boolean hasLauncherCategory = intent != null && intent.hasCategory(Intent.CATEGORY_LAUNCHER);
        boolean fromStartApp = intent != null && intent.getBooleanExtra("from_startapp", false);

        if (isTaskRoot() && Intent.ACTION_MAIN.equals(action) && hasLauncherCategory && !fromStartApp) {
            Intent i = new Intent(this, StartAppActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
            return;
        }

        // Enforce auth: if not signed in -> send to SignInActivity and finish main
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Intent i = new Intent(MainActivity.this, SignInActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
            return;
        }

        // Proceed to set content
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        // find greeting TextView (ensure layout has this id)
        tvGreeting = findViewById(R.id.tvGreeting);
        updateGreeting();

        // Apply window insets to root view while preserving original padding
        View root = findViewById(R.id.main2);
        if (root != null) {
            final int padL = root.getPaddingLeft();
            final int padT = root.getPaddingTop();
            final int padR = root.getPaddingRight();
            final int padB = root.getPaddingBottom();

            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(
                        padL + systemBars.left,
                        padT + systemBars.top,
                        padR + systemBars.right,
                        padB
                );
                return insets;
            });
        }

        bottomNav = findViewById(R.id.bottom_nav);
        myViewPager = findViewById(R.id.view_pager);

        setupViewPager();

        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> onBottomNavItemSelected(item));
        }

        if (myViewPager != null) {
            myViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
                @Override public void onPageSelected(int position) {
                    if (bottomNav == null) return;
                    MenuItem menuItem = null;
                    switch (position) {
                        case 0:
                            menuItem = bottomNav.getMenu().findItem(R.id.action_home);
                            break;
                        case 1:
                            menuItem = bottomNav.getMenu().findItem(R.id.action_add);
                            break;
                        case 2:
                            menuItem = bottomNav.getMenu().findItem(R.id.action_profile);
                            break;
                    }
                    if (menuItem != null) menuItem.setChecked(true);
                }
                @Override public void onPageScrollStateChanged(int state) {}
            });
        }

        // restore selected page if available
        if (savedInstanceState != null) {
            int page = savedInstanceState.getInt(KEY_SELECTED_PAGE, 0);
            if (myViewPager != null) myViewPager.setCurrentItem(page, false);
        } else {
            if (bottomNav != null && myViewPager != null) {
                int initial = myViewPager.getCurrentItem();
                MenuItem m = null;
                if (initial == 0) m = bottomNav.getMenu().findItem(R.id.action_home);
                else if (initial == 1) m = bottomNav.getMenu().findItem(R.id.action_add);
                else if (initial == 2) m = bottomNav.getMenu().findItem(R.id.action_profile);
                if (m != null) m.setChecked(true);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Refresh greeting in case profile changed
        updateGreeting();
    }

    private boolean onBottomNavItemSelected(@NonNull MenuItem item) {
        if (myViewPager == null) return false;
        int id = item.getItemId();
        if (id == R.id.action_home) { myViewPager.setCurrentItem(0, true); return true; }
        if (id == R.id.action_add) { myViewPager.setCurrentItem(1, true); return true; }
        if (id == R.id.action_profile) { myViewPager.setCurrentItem(2, true); return true; }
        return false;
    }

    private void setupViewPager() {
        if (myViewPager == null) return;
        ViewPagerAdapter adapter = new ViewPagerAdapter(
                getSupportFragmentManager(),
                ViewPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
        );
        myViewPager.setAdapter(adapter);
        myViewPager.setOffscreenPageLimit(2);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (myViewPager != null) {
            outState.putInt(KEY_SELECTED_PAGE, myViewPager.getCurrentItem());
        }
    }

    /**
     * Update the greeting TextView using Firebase displayName with fallbacks.
     * Preference order:
     * 1) FirebaseUser.getDisplayName()
     * 2) cached name in SharedPreferences
     * 3) email local-part (before @)
     * 4) "User"
     *
     * The chosen name is saved into SharedPreferences for future fallback.
     */
    private void updateGreeting() {
        if (tvGreeting == null) return;

        FirebaseUser user = mAuth.getCurrentUser();
        String nameToShow = null;

        if (user != null) {
            String displayName = user.getDisplayName();
            if (displayName != null && !displayName.trim().isEmpty()) {
                nameToShow = displayName.trim();
            } else {
                String saved = prefs.getString(KEY_DISPLAY_NAME, null);
                if (saved != null && !saved.trim().isEmpty()) {
                    nameToShow = saved;
                } else {
                    String email = user.getEmail();
                    if (email != null && email.contains("@")) {
                        nameToShow = email.substring(0, email.indexOf("@"));
                    } else if (email != null) {
                        nameToShow = email;
                    }
                }
            }
        } else {
            nameToShow = "User";
        }

        if (nameToShow == null || nameToShow.trim().isEmpty()) {
            nameToShow = "User";
        }

        prefs.edit().putString(KEY_DISPLAY_NAME, nameToShow).apply();

        tvGreeting.setText("Xin chào, " + nameToShow);
    }

    /**
     * Sign out current user and open SignInActivity (clearing back stack).
     */
    private void signOutAndGoToSignIn() {
        mAuth.signOut();
        Intent i = new Intent(MainActivity.this, SignInActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }
}