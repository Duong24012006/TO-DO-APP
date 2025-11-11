package com.example.to_do_app.activitys;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;

import com.example.to_do_app.R;
import com.example.to_do_app.adapters.ViewPagerAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * MainActivity — updated to handle incoming intent extras and forward payloads to HomeFragment:
 *  - "open_home" (boolean) -> open HomeFragment (page 0)
 *  - "displayName" (String) -> save to prefs and update greeting
 *  - "home_payload" (String) -> save to profile_prefs (for fallback) and broadcast ACTION_SCHEDULE_APPLIED so HomeFragment receives and updates UI
 *  - "selected_day" (int) -> forwarded in broadcast so HomeFragment can select the correct day
 *
 * Also listens for Layout6Activity.ACTION_SCHEDULE_APPLIED via LocalBroadcastManager
 * so MainActivity can switch to the Home page immediately when a schedule is applied.
 */
public class MainActivity extends AppCompatActivity {

    private static final String KEY_SELECTED_PAGE = "selected_page";
    private static final String PREFS = "todo_prefs";
    private static final String KEY_DISPLAY_NAME = "display_name";

    // profile prefs and home payload key names (must match HomeFragment)
    private static final String PROFILE_PREFS = "profile_prefs";
    private static final String HOME_DISPLAY_ACTIVITIES_KEY = "home_display_activities";
    private static final String PREF_ACTIVE_SCHEDULE = "active_schedule_name";
    private static final String HOME_DISPLAY_ACTIVITIES_SUFFIX = "_"; // will append schedule name when provided

    private BottomNavigationView bottomNav;
    private ViewPager myViewPager;
    private FirebaseAuth mAuth;
    private SharedPreferences prefs;
    private TextView tvGreeting;

    // Broadcast receiver to react when a schedule is applied (Layout6Activity sends this)
    private BroadcastReceiver scheduleAppliedReceiver;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
                        padB + systemBars.bottom
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

        // Handle intent extras (open_home, displayName, home_payload) on first creation
        handleIntentExtras(getIntent());
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Refresh greeting in case profile changed
        updateGreeting();
        // Register receiver so MainActivity can react when a schedule is applied
        registerScheduleAppliedReceiver();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unregister the receiver
        unregisterScheduleAppliedReceiver();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntentExtras(intent);
    }

    /**
     * Handle incoming intent extras. Also forwards home_payload to HomeFragment by broadcasting
     * ACTION_SCHEDULE_APPLIED so HomeFragment (which listens for that) will parse and display immediately.
     */
    private void handleIntentExtras(Intent intent) {
        if (intent == null) return;
        boolean openHome = intent.getBooleanExtra("open_home", false);
        String displayNameExtra = intent.getStringExtra("displayName");
        String homePayload = intent.getStringExtra("home_payload");
        int selectedDay = intent.getIntExtra("selected_day", -1);
        String scheduleName = intent.getStringExtra(Layout6Activity.EXTRA_SCHEDULE_NAME);

        // store displayName if provided
        if (displayNameExtra != null && !displayNameExtra.trim().isEmpty()) {
            prefs.edit().putString(KEY_DISPLAY_NAME, displayNameExtra.trim()).apply();
            updateGreeting();
        }

        // If payload provided (typically when Layout6Activity launches MainActivity), persist it to profile_prefs
        // so HomeFragment can read it later if needed.
        if (homePayload != null && !homePayload.trim().isEmpty()) {
            try {
                SharedPreferences profilePrefs = getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
                String key = HOME_DISPLAY_ACTIVITIES_KEY;
                if (scheduleName != null && !scheduleName.trim().isEmpty()) {
                    key = HOME_DISPLAY_ACTIVITIES_KEY + "_" + scheduleName;
                    // also persist active schedule name for HomeFragment fallback
                    profilePrefs.edit().putString(PREF_ACTIVE_SCHEDULE, scheduleName).apply();
                }
                profilePrefs.edit().putString(key, homePayload).apply();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            // Broadcast locally so HomeFragment (if active) receives payload immediately.
            try {
                Intent b = new Intent(Layout6Activity.ACTION_SCHEDULE_APPLIED);
                if (scheduleName != null) b.putExtra(Layout6Activity.EXTRA_SCHEDULE_NAME, scheduleName);
                b.putExtra("home_payload", homePayload);
                if (selectedDay >= 0) b.putExtra("selected_day", selectedDay);
                LocalBroadcastManager.getInstance(this).sendBroadcast(b);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        // If requested, open Home page
        if (openHome && myViewPager != null) {
            myViewPager.post(() -> {
                myViewPager.setCurrentItem(0, false);
                if (bottomNav != null) {
                    MenuItem mi = bottomNav.getMenu().findItem(R.id.action_home);
                    if (mi != null) mi.setChecked(true);
                }
            });
        }
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

    /**
     * Register receiver to handle schedule-applied events from Layout6Activity.
     * When received, switch the UI to Home page and optionally show a toast.
     */
    private void registerScheduleAppliedReceiver() {
        try {
            if (scheduleAppliedReceiver != null) return;
            scheduleAppliedReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent == null) return;
                    String scheduleName = intent.getStringExtra(Layout6Activity.EXTRA_SCHEDULE_NAME);
                    String payload = intent.getStringExtra("home_payload");
                    int selDay = intent.getIntExtra("selected_day", -1);

                    // switch to Home page
                    if (myViewPager != null) {
                        myViewPager.post(() -> {
                            myViewPager.setCurrentItem(0, true);
                            if (bottomNav != null) {
                                MenuItem mi = bottomNav.getMenu().findItem(R.id.action_home);
                                if (mi != null) mi.setChecked(true);
                            }
                        });
                    }

                    // optionally show a short toast to confirm
                    if (scheduleName != null && !scheduleName.isEmpty()) {
                        Toast.makeText(MainActivity.this, "Đã áp dụng lịch: " + scheduleName, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Đã áp dụng lịch", Toast.LENGTH_SHORT).show();
                    }
                }
            };
            IntentFilter f = new IntentFilter(Layout6Activity.ACTION_SCHEDULE_APPLIED);
            LocalBroadcastManager.getInstance(this).registerReceiver(scheduleAppliedReceiver, f);
        } catch (Exception ex) {
            // don't crash the app if registration fails
            ex.printStackTrace();
        }
    }

    private void unregisterScheduleAppliedReceiver() {
        try {
            if (scheduleAppliedReceiver != null) {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(scheduleAppliedReceiver);
                scheduleAppliedReceiver = null;
            }
        } catch (Exception ignored) {}
    }
}