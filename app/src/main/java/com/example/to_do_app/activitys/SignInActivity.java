package com.example.to_do_app.activitys;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.to_do_app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

/**
 * SignInActivity — simplified, with device confirmation on successful login.
 *
 * Changes:
 * - After successful sign-in we auto-confirm/register the device under
 *   /users/<userId>/devices/<deviceId> in Realtime Database (confirmed=true, timestamp).
 * - Only after device confirmation (or a DB failure) we navigate to MainActivity and request opening HomeFragment.
 * - We pass intent extra "open_home" = true so MainActivity can show HomeFragment on start.
 */
public class SignInActivity extends AppCompatActivity {

    private EditText etUserName, etPassword;
    private Button btnSignIn, btnChangeSignUp;
    private FirebaseAuth mAuth;
    private ProgressDialog progress;

    private DatabaseReference rootRef;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        mAuth = FirebaseAuth.getInstance();
        rootRef = FirebaseDatabase.getInstance().getReference();

        etUserName = findViewById(R.id.user_name);
        etPassword = findViewById(R.id.user_password);
        btnSignIn = findViewById(R.id.sign_in_button);
        btnChangeSignUp = findViewById(R.id.change_sign_up);

        // Prefill email if provided by SignUpActivity
        String prefillEmail = getIntent().getStringExtra("prefillEmail");
        if (prefillEmail != null && !prefillEmail.isEmpty()) {
            etUserName.setText(prefillEmail);
        }

        btnSignIn.setOnClickListener(v -> {
            hideKeyboard();

            String email = etUserName.getText() != null ? etUserName.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

            // Basic validation
            if (email.isEmpty()) {
                etUserName.setError("Vui lòng nhập email");
                etUserName.requestFocus();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etUserName.setError("Email không hợp lệ");
                etUserName.requestFocus();
                return;
            }
            if (password.isEmpty()) {
                etPassword.setError("Vui lòng nhập mật khẩu");
                etPassword.requestFocus();
                return;
            }
            if (password.length() < 6) {
                etPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
                etPassword.requestFocus();
                return;
            }

            // Prevent multiple clicks
            btnSignIn.setEnabled(false);
            safeShowProgress();

            // Directly attempt sign-in
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(signInTask -> {
                        if (signInTask.isSuccessful()) {
                            // On success, confirm device then navigate to MainActivity -> HomeFragment
                            FirebaseUser user = mAuth.getCurrentUser();
                            String userId = (user != null) ? user.getUid() : null;
                            confirmDeviceAndProceed(userId, () -> {
                                safeDismissProgress();
                                btnSignIn.setEnabled(true);
                                // Navigate to MainActivity and request opening HomeFragment
                                Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                                intent.putExtra("from_startapp", true);
                                intent.putExtra("open_home", true); // MainActivity should read this and open HomeFragment
                                String displayName = user != null ? user.getDisplayName() : null;
                                if (displayName != null) intent.putExtra("displayName", displayName);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            });
                        } else {
                            // Ensure UI restored
                            safeDismissProgress();
                            btnSignIn.setEnabled(true);

                            // Try to detect common reason: user not found -> send to SignUp
                            String errMsg = "";
                            if (signInTask.getException() != null && signInTask.getException().getMessage() != null) {
                                errMsg = signInTask.getException().getMessage();
                            }

                            // Firebase's message for non-existing user often contains "There is no user record" or "There is no user"
                            if (!errMsg.isEmpty() && (errMsg.toLowerCase().contains("no user") || errMsg.toLowerCase().contains("no user record") || errMsg.toLowerCase().contains("user-not-found"))) {
                                Toast.makeText(SignInActivity.this, "Email chưa được đăng ký. Chuyển sang trang đăng ký.", Toast.LENGTH_LONG).show();
                                Intent i = new Intent(SignInActivity.this, SignUpActivity.class);
                                i.putExtra("prefillEmail", etUserName.getText() != null ? etUserName.getText().toString().trim() : "");
                                startActivity(i);
                                // keep SignIn on back stack so user can return
                            } else {
                                String msg = "Đăng nhập thất bại";
                                if (!errMsg.isEmpty()) msg += ": " + errMsg;
                                showToast(msg);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        safeDismissProgress();
                        btnSignIn.setEnabled(true);
                        String msg = "Đăng nhập thất bại";
                        if (e != null && e.getMessage() != null) msg += ": " + e.getMessage();

                        // If failure indicates user doesn't exist, redirect to SignUp
                        String lower = (e != null && e.getMessage() != null) ? e.getMessage().toLowerCase() : "";
                        if (lower.contains("no user") || lower.contains("no user record") || lower.contains("user-not-found")) {
                            Toast.makeText(SignInActivity.this, "Email chưa được đăng ký.", Toast.LENGTH_LONG).show();
                            Intent i = new Intent(SignInActivity.this, SignUpActivity.class);
                            i.putExtra("prefillEmail", etUserName.getText() != null ? etUserName.getText().toString().trim() : "");
                            startActivity(i);
                        } else {
                            showToast(msg);
                        }
                    });
        });

        btnChangeSignUp.setOnClickListener(v -> {
            hideKeyboard();
            Intent i = new Intent(SignInActivity.this, SignUpActivity.class);
            String email = etUserName.getText() != null ? etUserName.getText().toString().trim() : "";
            if (!email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                i.putExtra("prefillEmail", email);
            }
            startActivity(i);
            // keep SignIn on back stack so user can return
        });
    }

    /**
     * Confirm/register device under /users/<userId>/devices/<deviceId>
     * Calls onDone.run() when DB write completes or on failure (we still proceed).
     */
    private void confirmDeviceAndProceed(String userId, Runnable onDone) {
        if (userId == null || userId.isEmpty()) {
            // Nothing to confirm; proceed immediately
            onDone.run();
            return;
        }

        try {
            String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            if (deviceId == null || deviceId.isEmpty()) {
                // fallback to build fields
                deviceId = "android_" + Build.SERIAL;
            }
            DatabaseReference devRef = rootRef.child("users").child(userId).child("devices").child(deviceId);

            Map<String, Object> payload = new HashMap<>();
            payload.put("confirmed", true);
            payload.put("deviceId", deviceId);
            payload.put("model", Build.MODEL);
            payload.put("manufacturer", Build.MANUFACTURER);
            payload.put("sdk_int", Build.VERSION.SDK_INT);
            payload.put("timestamp", ServerValue.TIMESTAMP);

            // Write and wait for completion; whether success or failure we continue to onDone
            devRef.setValue(payload)
                    .addOnSuccessListener(aVoid -> {
                        // success - proceed
                        onDone.run();
                    })
                    .addOnFailureListener(e -> {
                        // Failure to write shouldn't block user - log and proceed
                        Log.w("SignInActivity", "Device confirmation failed: " + (e != null ? e.getMessage() : "unknown"));
                        onDone.run();
                    });
        } catch (Exception ex) {
            // On any exception proceed
            Log.w("SignInActivity", "confirmDeviceAndProceed exception", ex);
            onDone.run();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // intentionally do not auto-skip to MainActivity here.
    }

    private void hideKeyboard() {
        try {
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        } catch (Exception ignored) { }
    }

    private void showToast(String msg) {
        if (!isFinishing() && !isDestroyed()) {
            runOnUiThread(() -> Toast.makeText(SignInActivity.this, msg, Toast.LENGTH_LONG).show());
        }
    }

    private void safeShowProgress() {
        try {
            if (progress == null) {
                progress = new ProgressDialog(this);
                progress.setCancelable(false);
                progress.setMessage("Đang xử lý...");
            }
            if (!isFinishing() && !isDestroyed() && !progress.isShowing()) {
                progress.show();
            }
        } catch (Exception ignored) { }
    }

    private void safeDismissProgress() {
        try {
            if (progress != null && !isFinishing() && !isDestroyed() && progress.isShowing()) {
                progress.dismiss();
            }
        } catch (Exception ignored) { }
    }

    @Override
    protected void onDestroy() {
        safeDismissProgress();
        super.onDestroy();
    }
}