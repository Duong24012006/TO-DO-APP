package com.example.to_do_app.activitys;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.to_do_app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

/**
 * SignUpActivity (updated)
 *
 * Behavior changes:
 * - After successful registration, the app now navigates to SignInActivity (so user must sign in).
 * - The email field on SignInActivity will be prefilled with the registered email.
 * - Progress and button state handling preserved.
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        etFullName = findViewById(R.id.text1);
        etEmail = findViewById(R.id.text2);
        etPassword = findViewById(R.id.text3);
        etConfirm = findViewById(R.id.text4);
        btnSignUp = findViewById(R.id.button_sign_in);
        tvGoToSignIn = findViewById(R.id.chuyen_dang_nhap);

        mAuth = FirebaseAuth.getInstance();

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
            startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
            finish();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Intentionally empty to avoid auto-skip behavior.
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
                    // Ensure progress dismissed and button re-enabled in all outcomes
                    safeDismissProgress();
                    btnSignUp.setEnabled(true);

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(fullName)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(updateTask -> {
                                        // On success, notify user then navigate to SignInActivity with prefilled email
                                        Toast.makeText(SignUpActivity.this, "Đăng ký thành công. Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
                                        intent.putExtra("prefillEmail", email);
                                        // Clear task so user can't go back to signed-up state accidentally
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        finish();
                                    });
                        } else {
                            Toast.makeText(SignUpActivity.this, "Đăng ký thành công. Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
                            intent.putExtra("prefillEmail", email);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        }
                    } else {
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
                    } else {
                        Toast.makeText(SignUpActivity.this, "Đăng ký thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
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
}