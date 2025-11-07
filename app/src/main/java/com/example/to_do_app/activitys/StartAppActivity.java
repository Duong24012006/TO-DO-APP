package com.example.to_do_app.activitys;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;

import com.example.to_do_app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * StartAppActivity:
 * - If FirebaseAuth has a current user, skip Start screen and go to MainActivity (open_home=true).
 * - Otherwise show Start screen: user must tap BẮT ĐẦU -> SignInActivity.
 * - If device was previously confirmed for a user (saved in prefs), we check DB for validity and
 *   prefill SignIn with the associated email (but do NOT auto-sign-in without credentials).
 */
public class StartAppActivity extends AppCompatActivity {

    private ProgressDialog progress;
    private FirebaseAuth mAuth;
    private DatabaseReference rootRef;

    private static final String PREFS = "todo_prefs";
    private static final String KEY_CONFIRMED_USER_ID = "confirmed_user_id"; // optional local marker
    private static final String KEY_CONFIRMED_USER_EMAIL = "confirmed_user_email";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // edge-to-edge handling
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_start_app);

        mAuth = FirebaseAuth.getInstance();
        rootRef = FirebaseDatabase.getInstance().getReference();

        // If user already signed in (Firebase persistence), skip Start and go to MainActivity (open Home)
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Intent intent = new Intent(StartAppActivity.this, MainActivity.class);
            intent.putExtra("from_startapp", true);
            intent.putExtra("open_home", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // otherwise show start screen and set up UI
        final View mainView = findViewById(R.id.main);
        final int initialPaddingLeft = mainView != null ? mainView.getPaddingLeft() : 0;
        final int initialPaddingTop = mainView != null ? mainView.getPaddingTop() : 0;
        final int initialPaddingRight = mainView != null ? mainView.getPaddingRight() : 0;
        final int initialPaddingBottom = mainView != null ? mainView.getPaddingBottom() : 0;

        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                Insets navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
                v.setPadding(
                        initialPaddingLeft,
                        initialPaddingTop + statusBars.top,
                        initialPaddingRight,
                        initialPaddingBottom + navBars.bottom
                );
                return insets;
            });
        }

        Button startButton = findViewById(R.id.batdau);
        if (startButton != null) {
            startButton.setOnClickListener(v -> {
                // Guard against double clicks
                startButton.setEnabled(false);
                hideKeyboard();

                safeShowProgress("Đang chuyển...");

                String confirmedUserId = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_CONFIRMED_USER_ID, null);
                String confirmedEmail = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_CONFIRMED_USER_EMAIL, null);

                if (confirmedUserId != null && !confirmedUserId.isEmpty()) {
                    // check DB for device entry under that user
                    String deviceId = getLocalDeviceId();
                    DatabaseReference devRef = rootRef.child("users").child(confirmedUserId).child("devices").child(deviceId);
                    devRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            boolean confirmed = false;
                            if (snapshot != null && snapshot.exists()) {
                                Object c = snapshot.child("confirmed").getValue();
                                if (c instanceof Boolean) confirmed = (Boolean) c;
                                else if (c instanceof String) confirmed = Boolean.parseBoolean((String) c);
                            }
                            Intent intent = new Intent(StartAppActivity.this, SignInActivity.class);
                            if (confirmed && confirmedEmail != null && !confirmedEmail.isEmpty()) {
                                intent.putExtra("prefillEmail", confirmedEmail);
                                Toast.makeText(StartAppActivity.this, "Thiết bị này đã được xác nhận cho tài khoản " + confirmedEmail, Toast.LENGTH_SHORT).show();
                            }
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            safeDismissProgress();
                            finish();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // DB error — just go to SignIn without prefill
                            Intent intent = new Intent(StartAppActivity.this, SignInActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            safeDismissProgress();
                            finish();
                        }
                    });
                } else {
                    // No local confirmed user info => go to SignIn normally
                    Intent intent = new Intent(StartAppActivity.this, SignInActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    safeDismissProgress();
                    finish();
                }
            });
        }
    }

    /**
     * Renamed helper to avoid clash with ContextWrapper#getDeviceId().
     * Returns a best-effort stable device identifier for local use (ANDROID_ID or fallback).
     */
    private String getLocalDeviceId() {
        try {
            String id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            if (id == null || id.isEmpty()) id = "android_" + Build.SERIAL;
            return id;
        } catch (Exception ex) {
            return "android_unknown";
        }
    }

    private void hideKeyboard() {
        try {
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        } catch (Exception ignored) {
        }
    }

    private void safeShowProgress(String message) {
        try {
            if (progress == null) {
                progress = new ProgressDialog(this);
                progress.setCancelable(false);
            }
            progress.setMessage(message == null ? "Đang xử lý..." : message);
            if (!isFinishing() && !isDestroyed() && !progress.isShowing()) {
                progress.show();
            }
        } catch (Exception ignored) {
        }
    }

    private void safeDismissProgress() {
        try {
            if (progress != null && !isFinishing() && !isDestroyed() && progress.isShowing()) {
                progress.dismiss();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        safeDismissProgress();
        super.onDestroy();
    }
}