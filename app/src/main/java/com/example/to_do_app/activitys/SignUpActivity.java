package com.example.to_do_app.activitys;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.to_do_app.R;
import com.google.firebase.database.ServerValue;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * SignUpActivity — corrected so lambda-captured locals are final/effectively-final.
 *
 * Fixes applied:
 * - Marked local variables used inside nested lambdas as final.
 * - Declared devRef, payload and fsPayload as final inside confirmDeviceAndProceed so nested listeners can capture them.
 * - Uses AlertDialog + dialog_progress.xml for progress UI.
 * - Writes device info to both RTDB and Firestore (best-effort), updates displayName, sends verification email.
 */
public class SignUpActivity extends AppCompatActivity {

    private EditText etFullName;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirm;
    private Button btnSignUp;
    private View tvGoToSignIn;
    private FirebaseAuth mAuth;
    private DatabaseReference rootRef;
    private FirebaseFirestore firestore;

    // Progress replacement
    private AlertDialog progressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up); // the XML you provided

        etFullName = findViewById(R.id.text1);
        etEmail = findViewById(R.id.text2);
        etPassword = findViewById(R.id.text3);
        etConfirm = findViewById(R.id.text4);
        btnSignUp = findViewById(R.id.button_sign_in);
        tvGoToSignIn = findViewById(R.id.chuyen_dang_nhap);

        mAuth = FirebaseAuth.getInstance();
        rootRef = FirebaseDatabase.getInstance().getReference();
        firestore = FirebaseFirestore.getInstance();

        // Prefill email if provided
        String prefill = getIntent().getStringExtra("prefillEmail");
        if (prefill != null && !prefill.isEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(prefill).matches()) {
            etEmail.setText(prefill);
        }

        btnSignUp.setOnClickListener(v -> attemptSignUp());

        tvGoToSignIn.setOnClickListener(v -> {
            Intent i = new Intent(SignUpActivity.this, SignInActivity.class);
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            if (!email.isEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
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
        if (fullName.isEmpty()) {
            etFullName.setError("Nhập họ và tên");
            etFullName.requestFocus();
            return;
        }
        if (email.isEmpty()) {
            etEmail.setError("Nhập email");
            etEmail.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không hợp lệ");
            etEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
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
                    if (task.isSuccessful()) {
                        final FirebaseUser user = mAuth.getCurrentUser(); // final to avoid lambda-capture issues
                        if (user != null) {
                            // Update profile display name (best-effort)
                            final UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(fullName)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(updateTask -> {
                                        // after updating profile, send verification email and confirm device
                                        sendVerificationEmail(user);
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
                                    })
                                    .addOnFailureListener(e -> {
                                        // Proceed even if profile update fails
                                        sendVerificationEmail(user);
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
                            // unexpected: proceed to sign-in fallback
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
     * Send verification email to the user (best-effort).
     */
    private void sendVerificationEmail(FirebaseUser user) {
        try {
            if (user == null) return;
            user.sendEmailVerification()
                    .addOnSuccessListener(aVoid -> Toast.makeText(SignUpActivity.this, "Đã gửi email xác thực. Vui lòng kiểm tra hộp thư.", Toast.LENGTH_LONG).show())
                    .addOnFailureListener(e -> Log.w("SignUpActivity", "Failed to send verification email: " + (e != null ? e.getMessage() : "unknown")));
        } catch (Exception ex) {
            Log.w("SignUpActivity", "sendVerificationEmail exception", ex);
        }
    }

    /**
     * Confirm/register device under both RTDB and Firestore.
     * Always calls onDone.run() whether writes succeed or fail.
     */
    private void confirmDeviceAndProceed(String userId, Runnable onDone) {
        if (userId == null || userId.isEmpty()) {
            onDone.run();
            return;
        }
        try {
            final String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            final String finalDeviceId = (deviceId == null || deviceId.isEmpty()) ? ("android_" + Build.SERIAL) : deviceId;

            final DatabaseReference devRef = rootRef.child("users").child(userId).child("devices").child(finalDeviceId);

            final Map<String, Object> payload = new HashMap<>();
            payload.put("confirmed", true);
            payload.put("deviceId", finalDeviceId);
            payload.put("model", Build.MODEL);
            payload.put("manufacturer", Build.MANUFACTURER);
            payload.put("sdk_int", Build.VERSION.SDK_INT);
            payload.put("timestamp", ServerValue.TIMESTAMP);

            // write to RTDB
            devRef.setValue(payload)
                    .addOnSuccessListener(aVoid -> {
                        // then write to Firestore
                        final Map<String, Object> fsPayload = new HashMap<>();
                        fsPayload.put("confirmed", true);
                        fsPayload.put("deviceId", finalDeviceId);
                        fsPayload.put("model", Build.MODEL);
                        fsPayload.put("manufacturer", Build.MANUFACTURER);
                        fsPayload.put("sdk_int", Build.VERSION.SDK_INT);
                        fsPayload.put("timestamp", FieldValue.serverTimestamp());

                        firestore.collection("users").document(userId)
                                .collection("devices").document(finalDeviceId)
                                .set(fsPayload)
                                .addOnSuccessListener(aVoid2 -> onDone.run())
                                .addOnFailureListener(e -> {
                                    Log.w("SignUpActivity", "Firestore device write failed: " + (e != null ? e.getMessage() : "unknown"));
                                    onDone.run();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.w("SignUpActivity", "RTDB device write failed: " + (e != null ? e.getMessage() : "unknown"));
                        // attempt Firestore anyway
                        final Map<String, Object> fsPayload = new HashMap<>();
                        fsPayload.put("confirmed", true);
                        fsPayload.put("deviceId", finalDeviceId);
                        fsPayload.put("model", Build.MODEL);
                        fsPayload.put("manufacturer", Build.MANUFACTURER);
                        fsPayload.put("sdk_int", Build.VERSION.SDK_INT);
                        fsPayload.put("timestamp", FieldValue.serverTimestamp());

                        firestore.collection("users").document(userId)
                                .collection("devices").document(finalDeviceId)
                                .set(fsPayload)
                                .addOnSuccessListener(aVoid2 -> onDone.run())
                                .addOnFailureListener(e2 -> {
                                    Log.w("SignUpActivity", "Firestore device write also failed: " + (e2 != null ? e2.getMessage() : "unknown"));
                                    onDone.run();
                                });
                    });
        } catch (Exception ex) {
            Log.w("SignUpActivity", "confirmDeviceAndProceed exception", ex);
            onDone.run();
        }
    }

    private void safeShowProgress() {
        try {
            if (progressDialog == null) {
                AlertDialog.Builder b = new AlertDialog.Builder(this);
                b.setCancelable(false);
                View v = LayoutInflater.from(this).inflate(R.layout.dialog_progress, null);
                b.setView(v);
                progressDialog = b.create();
            }
            if (!isFinishing() && !isDestroyed() && !progressDialog.isShowing()) {
                progressDialog.show();
            }
        } catch (Exception ignored) {}
    }

    private void safeDismissProgress() {
        try {
            if (progressDialog != null && !isFinishing() && !isDestroyed() && progressDialog.isShowing()) {
                progressDialog.dismiss();
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