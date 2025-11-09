package com.example.to_do_app.activitys;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.Patterns;
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
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * SignInActivity — improved:
 * - Uses AlertDialog+layout dialog_progress.xml as progress indicator.
 * - Writes device info to both Realtime Database and Firestore.
 * - Checks email verification and allows resend / re-check.
 * - Better error handling (user-not-found, wrong-password, disabled).
 *
 * Note: This Activity expects the layout you provided (activity_sign_in.xml)
 * with IDs: user_name, user_password, sign_in_button, change_sign_up.
 */
public class SignInActivity extends AppCompatActivity {

    private EditText etUserName, etPassword;
    private Button btnSignIn, btnChangeSignUp;
    private FirebaseAuth mAuth;

    // Realtime DB root ref
    private DatabaseReference rootRef;

    // Firestore
    private FirebaseFirestore firestore;

    // Progress dialog replacement
    private AlertDialog progressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        mAuth = FirebaseAuth.getInstance();
        rootRef = FirebaseDatabase.getInstance().getReference();
        firestore = FirebaseFirestore.getInstance();

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

            // Attempt sign-in
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(signInTask -> {
                        if (signInTask.isSuccessful()) {
                            final FirebaseUser user = mAuth.getCurrentUser(); // final to avoid lambda capture issues
                            if (user == null) {
                                safeDismissProgress();
                                btnSignIn.setEnabled(true);
                                Toast.makeText(SignInActivity.this, "Đăng nhập thất bại (không tìm thấy user)", Toast.LENGTH_LONG).show();
                                return;
                            }

                            // Check if email is verified
                            if (!user.isEmailVerified()) {
                                safeDismissProgress();
                                showEmailNotVerifiedDialog(user);
                                btnSignIn.setEnabled(true);
                                return;
                            }

                            // Email verified -> proceed to confirm device and launch MainActivity
                            String userId = user.getUid();
                            confirmDeviceAndProceed(userId, () -> {
                                safeDismissProgress();
                                btnSignIn.setEnabled(true);
                                Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                                intent.putExtra("from_startapp", true);
                                intent.putExtra("open_home", true);
                                String displayName = user.getDisplayName();
                                if (displayName != null) intent.putExtra("displayName", displayName);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            });
                        } else {
                            // Handle errors with more specific messages
                            safeDismissProgress();
                            btnSignIn.setEnabled(true);

                            Exception ex = signInTask.getException();
                            String errMsg = (ex != null && ex.getMessage() != null) ? ex.getMessage() : "";

                            String userMessage = "Đăng nhập thất bại";
                            boolean redirectedToSignUp = false;

                            // Check FirebaseAuthException error codes first if available
                            if (ex instanceof FirebaseAuthException) {
                                String code = ((FirebaseAuthException) ex).getErrorCode();
                                if (code != null) {
                                    String lc = code.toLowerCase();
                                    if (lc.contains("user-not-found") || lc.contains("no-user") || lc.contains("user-not-exist")) {
                                        // Redirect to sign-up
                                        Toast.makeText(SignInActivity.this, "Email chưa được đăng ký. Chuyển sang trang đăng ký.", Toast.LENGTH_LONG).show();
                                        Intent i = new Intent(SignInActivity.this, SignUpActivity.class);
                                        i.putExtra("prefillEmail", etUserName.getText() != null ? etUserName.getText().toString().trim() : "");
                                        startActivity(i);
                                        redirectedToSignUp = true;
                                    } else if (lc.contains("wrong-password")) {
                                        userMessage = "Mật khẩu không đúng. Vui lòng thử lại.";
                                    } else if (lc.contains("user-disabled")) {
                                        userMessage = "Tài khoản đã bị vô hiệu hóa. Liên hệ hỗ trợ.";
                                    }
                                }
                            }

                            // Fallback checks on message text
                            if (!redirectedToSignUp) {
                                String lower = errMsg.toLowerCase();
                                if (lower.contains("no user") || lower.contains("user-not-found") || lower.contains("no user record")) {
                                    Toast.makeText(SignInActivity.this, "Email chưa được đăng ký. Chuyển sang trang đăng ký.", Toast.LENGTH_LONG).show();
                                    Intent i = new Intent(SignInActivity.this, SignUpActivity.class);
                                    i.putExtra("prefillEmail", etUserName.getText() != null ? etUserName.getText().toString().trim() : "");
                                    startActivity(i);
                                    redirectedToSignUp = true;
                                } else if (lower.contains("password") || lower.contains("wrong-password")) {
                                    userMessage = "Mật khẩu không đúng. Vui lòng thử lại.";
                                } else if (!errMsg.isEmpty()) {
                                    userMessage = userMessage + ": " + errMsg;
                                }
                            }

                            if (!redirectedToSignUp) {
                                Toast.makeText(SignInActivity.this, userMessage, Toast.LENGTH_LONG).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        safeDismissProgress();
                        btnSignIn.setEnabled(true);
                        String msg = "Đăng nhập thất bại";
                        if (e != null && e.getMessage() != null) {
                            String lower = e.getMessage().toLowerCase();
                            if (lower.contains("no user") || lower.contains("user-not-found")) {
                                Toast.makeText(SignInActivity.this, "Email chưa được đăng ký.", Toast.LENGTH_LONG).show();
                                Intent i = new Intent(SignInActivity.this, SignUpActivity.class);
                                i.putExtra("prefillEmail", etUserName.getText() != null ? etUserName.getText().toString().trim() : "");
                                startActivity(i);
                                return;
                            } else if (lower.contains("password") || lower.contains("wrong-password")) {
                                msg = "Mật khẩu không đúng. Vui lòng thử lại.";
                            } else {
                                msg += ": " + e.getMessage();
                            }
                        }
                        showToast(msg);
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
     * Confirm/register device under both RTDB and Firestore under /users/<userId>/devices/<deviceId>
     * Calls onDone.run() when writes complete (or have failed) so user won't be blocked.
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

            Map<String, Object> payload = new HashMap<>();
            payload.put("confirmed", true);
            payload.put("deviceId", deviceId);
            payload.put("model", Build.MODEL);
            payload.put("manufacturer", Build.MANUFACTURER);
            payload.put("sdk_int", Build.VERSION.SDK_INT);
            payload.put("timestamp", ServerValue.TIMESTAMP);

            // Write to Realtime DB
            DatabaseReference devRef = rootRef.child("users").child(userId).child("devices").child(deviceId);
            String finalDeviceId = deviceId;
            String finalDeviceId1 = deviceId;
            devRef.setValue(payload)
                    .addOnSuccessListener(aVoid -> {
                        // also write to Firestore
                        Map<String, Object> fsPayload = new HashMap<>();
                        fsPayload.put("confirmed", true);
                        fsPayload.put("deviceId", finalDeviceId1);
                        fsPayload.put("model", Build.MODEL);
                        fsPayload.put("manufacturer", Build.MANUFACTURER);
                        fsPayload.put("sdk_int", Build.VERSION.SDK_INT);
                        fsPayload.put("timestamp", FieldValue.serverTimestamp());

                        firestore.collection("users").document(userId)
                                .collection("devices").document(finalDeviceId1)
                                .set(fsPayload)
                                .addOnSuccessListener(aVoid2 -> onDone.run())
                                .addOnFailureListener(e -> {
                                    Log.w("SignInActivity", "Firestore device write failed: " + (e != null ? e.getMessage() : "unknown"));
                                    onDone.run();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.w("SignInActivity", "RTDB device write failed: " + (e != null ? e.getMessage() : "unknown"));
                        // Attempt Firestore write regardless
                        Map<String, Object> fsPayload = new HashMap<>();
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
                                    Log.w("SignInActivity", "Firestore device write also failed: " + (e2 != null ? e2.getMessage() : "unknown"));
                                    onDone.run();
                                });
                    });
        } catch (Exception ex) {
            Log.w("SignInActivity", "confirmDeviceAndProceed exception", ex);
            onDone.run();
        }
    }

    /**
     * Show dialog when user is not email-verified.
     * Allows resending verification email or rechecking verification status.
     */
    private void showEmailNotVerifiedDialog(FirebaseUser user) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Email chưa được xác thực");
        b.setMessage("Bạn chưa xác thực email. Vui lòng kiểm tra hộp thư. Bạn có muốn gửi lại email xác thực không?");

        b.setPositiveButton("Gửi lại email", (dialog, which) -> {
            safeShowProgress();
            user.sendEmailVerification()
                    .addOnSuccessListener(aVoid -> {
                        safeDismissProgress();
                        Toast.makeText(SignInActivity.this, "Đã gửi email xác thực. Vui lòng kiểm tra hộp thư.", Toast.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e -> {
                        safeDismissProgress();
                        String msg = "Gửi email xác thực thất bại";
                        if (e != null && e.getMessage() != null) msg += ": " + e.getMessage();
                        Toast.makeText(SignInActivity.this, msg, Toast.LENGTH_LONG).show();
                    });
        });

        b.setNeutralButton("Tôi đã xác thực", (dialog, which) -> {
            safeShowProgress();
            user.reload()
                    .addOnSuccessListener(aVoid -> {
                        safeDismissProgress();
                        FirebaseUser reloaded = mAuth.getCurrentUser();
                        if (reloaded != null && reloaded.isEmailVerified()) {
                            confirmDeviceAndProceed(reloaded.getUid(), () -> {
                                Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                                intent.putExtra("from_startapp", true);
                                intent.putExtra("open_home", true);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            });
                        } else {
                            Toast.makeText(SignInActivity.this, "Email vẫn chưa được xác thực. Vui lòng kiểm tra lại.", Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        safeDismissProgress();
                        String msg = "Không thể kiểm tra trạng thái xác thực";
                        if (e != null && e.getMessage() != null) msg += ": " + e.getMessage();
                        Toast.makeText(SignInActivity.this, msg, Toast.LENGTH_LONG).show();
                    });
        });

        b.setNegativeButton("Hủy", null);
        b.show();
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

    private void showToast(String msg) {
        if (!isFinishing() && !isDestroyed()) {
            runOnUiThread(() -> Toast.makeText(SignInActivity.this, msg, Toast.LENGTH_LONG).show());
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
        } catch (Exception ignored) {
        }
    }

    private void safeDismissProgress() {
        try {
            if (progressDialog != null && !isFinishing() && !isDestroyed() && progressDialog.isShowing()) {
                progressDialog.dismiss();
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