package com.example.to_do_app.activitys;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.to_do_app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

/**
 * SignUpActivity — updated to auto sign-in and confirm device after successful registration.
 *
 * Flow:
 *  - Validate input and create user with email+password.
 *  - update displayName on the FirebaseUser profile (best-effort).
 *  - confirm/register device under /users/<userId>/devices/<deviceId> in Realtime Database.
 *  - After device confirmation (or if DB write fails), navigate to MainActivity with extra "open_home"=true
 *    so MainActivity should open HomeFragment on start.
 *
 * Notes:
 *  - createUserWithEmailAndPassword signs in the newly created user automatically.
 *  - We attempt to write device info and then proceed regardless of DB success/failure to avoid blocking the user.
 */
public class SignUpActivity extends AppCompatActivity {

    private EditText etFullName;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirm;
    private Button btnSignUp;
    private TextView tvGoToSignIn;
    private FirebaseAuth mAuth;
    private ProgressDialog progress;
    private DatabaseReference rootRef;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        etFullName = findViewById(R.id.text1);
        etEmail = findViewById(R.id.text2);
        etPassword = findViewById(R.id.text3);
        etConfirm = findViewById(R.id.text4);
        btnSignUp = findViewById(R.id.button_sign_in);
        tvGoToSignIn = findViewById(R.id.chuyen_dang_nhap);

        mAuth = FirebaseAuth.getInstance();
        rootRef = FirebaseDatabase.getInstance().getReference();

        progress = new ProgressDialog(this);
        progress.setCancelable(false);
        progress.setMessage("Đang đăng ký...");

        // Prefill email if provided
        String prefill = getIntent().getStringExtra("prefillEmail");
        if (prefill != null && !prefill.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(prefill).matches()) {
            etEmail.setText(prefill);
        }

        btnSignUp.setOnClickListener(v -> attemptSignUp());

        tvGoToSignIn.setOnClickListener(v -> {
            Intent i = new Intent(SignUpActivity.this, SignInActivity.class);
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            if (!email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                i.putExtra("prefillEmail", email);
            }
            startActivity(i);
            finish();
        });
    }

    private void attemptSignUp() {
        hideKeyboard();

        final String fullName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        final String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        final String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
        final String confirm = etConfirm.getText() != null ? etConfirm.getText().toString() : "";

        // Validate inputs
        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Nhập họ và tên");
            etFullName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Nhập email");
            etEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không hợp lệ");
            etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Nhập mật khẩu");
            etPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Mật khẩu ít nhất 6 ký tự");
            etPassword.requestFocus();
            return;
        }
        if (!password.equals(confirm)) {
            etConfirm.setError("Mật khẩu không khớp");
            etConfirm.requestFocus();
            return;
        }

        // Prevent multiple clicks
        btnSignUp.setEnabled(false);
        safeShowProgress();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    // Keep UI consistent: dismiss progress and re-enable on failure paths; on success we'll continue and finish activity.
                    if (task.isSuccessful()) {
                        // The newly created user is signed in automatically.
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Update profile display name (best-effort)
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(fullName)
                                    .build();
                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(updateTask -> {
                                        // proceed to confirm device and then MainActivity
                                        confirmDeviceAndProceed(user.getUid(), () -> {
                                            safeDismissProgress();
                                            btnSignUp.setEnabled(true);
                                            // Navigate to MainActivity and open HomeFragment
                                            Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                                            intent.putExtra("from_startapp", true);
                                            intent.putExtra("open_home", true);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                            startActivity(intent);
                                            finish();
                                        });
                                    })
                                    .addOnFailureListener(e -> {
                                        // Even if profile update fails, proceed
                                        confirmDeviceAndProceed(user.getUid(), () -> {
                                            safeDismissProgress();
                                            btnSignUp.setEnabled(true);
                                            Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                                            intent.putExtra("from_startapp", true);
                                            intent.putExtra("open_home", true);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                            startActivity(intent);
                                            finish();
                                        });
                                    });
                        } else {
                            // Unexpected: no user object, but treat as success and prompt sign-in fallback
                            safeDismissProgress();
                            btnSignUp.setEnabled(true);
                            Toast.makeText(SignUpActivity.this, "Đăng ký thành công. Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
                            intent.putExtra("prefillEmail", email);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        safeDismissProgress();
                        btnSignUp.setEnabled(true);
                        Exception ex = task.getException();
                        if (ex instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(SignUpActivity.this, "Email đã được sử dụng", Toast.LENGTH_LONG).show();
                        } else if (ex != null && ex.getMessage() != null) {
                            Toast.makeText(SignUpActivity.this, "Đăng ký thất bại: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(SignUpActivity.this, "Đăng ký thất bại: Lỗi không xác định", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    safeDismissProgress();
                    btnSignUp.setEnabled(true);
                    if (e instanceof FirebaseAuthUserCollisionException) {
                        Toast.makeText(SignUpActivity.this, "Email đã được sử dụng", Toast.LENGTH_LONG).show();
                    } else if (e != null && e.getMessage() != null) {
                        Toast.makeText(SignUpActivity.this, "Đăng ký thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(SignUpActivity.this, "Đăng ký thất bại: Lỗi không xác định", Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Confirm/register device under /users/<userId>/devices/<deviceId>.
     * Always calls onDone.run() whether DB write succeeds or fails.
     */
    private void confirmDeviceAndProceed(String userId, Runnable onDone) {
        if (userId == null || userId.isEmpty()) {
            onDone.run();
            return;
        }
        try {
            String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            if (deviceId == null || deviceId.isEmpty()) {
                deviceId = "android_" + Build.SERIAL;
            }
            DatabaseReference devRef = FirebaseDatabase.getInstance().getReference()
                    .child("users").child(userId).child("devices").child(deviceId);

            Map<String, Object> payload = new HashMap<>();
            payload.put("confirmed", true);
            payload.put("deviceId", deviceId);
            payload.put("model", Build.MODEL);
            payload.put("manufacturer", Build.MANUFACTURER);
            payload.put("sdk_int", Build.VERSION.SDK_INT);
            payload.put("timestamp", ServerValue.TIMESTAMP);

            devRef.setValue(payload)
                    .addOnSuccessListener(aVoid -> onDone.run())
                    .addOnFailureListener(e -> {
                        // Log, but proceed
                        android.util.Log.w("SignUpActivity", "Device confirmation failed: " + (e != null ? e.getMessage() : "unknown"));
                        onDone.run();
                    });
        } catch (Exception ex) {
            android.util.Log.w("SignUpActivity", "confirmDeviceAndProceed exception", ex);
            onDone.run();
        }
    }

    private void safeShowProgress() {
        try {
            if (!isFinishing() && !isDestroyed()) {
                if (progress == null) {
                    progress = new ProgressDialog(this);
                    progress.setCancelable(false);
                    progress.setMessage("Đang đăng ký...");
                }
                if (!progress.isShowing()) progress.show();
            }
        } catch (Exception ignored) {}
    }

    private void safeDismissProgress() {
        try {
            if (!isFinishing() && !isDestroyed() && progress != null && progress.isShowing()) {
                progress.dismiss();
            }
        } catch (Exception ignored) {}
    }

    private void hideKeyboard() {
        try {
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        safeDismissProgress();
        super.onDestroy();
    }
}