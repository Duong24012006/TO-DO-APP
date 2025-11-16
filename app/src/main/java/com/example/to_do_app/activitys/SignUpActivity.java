package com.example.to_do_app.activitys;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.to_do_app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

/**
 * SignUpActivity — merged and corrected
 *
 * Key behaviors:
 * - After successful registration, user is kept signed-in and navigated directly to MainActivity.
 * - Uses only addOnCompleteListener to centralize handling and avoid duplicate flows.
 * - Replaces deprecated ProgressDialog with AlertDialog + ProgressBar.
 * - Ensures progress dialog is dismissed and sign-up button re-enabled on every path.
 */
public class SignUpActivity extends AppCompatActivity {

    private EditText etFullName;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirm;
    private Button btnSignUp;
    private TextView tvGoToSignIn;
    private FirebaseAuth mAuth;
    private AlertDialog progressDialog;

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

        // Prefill email if provided
        String prefill = getIntent().getStringExtra("prefillEmail");
        if (prefill != null && !prefill.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(prefill).matches()) {
            etEmail.setText(prefill);
        }

        btnSignUp.setOnClickListener(v -> attemptSignUp());

        tvGoToSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            if (!email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                intent.putExtra("prefillEmail", email);
            }
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Intentionally empty to avoid auto-skip behavior here; StartApp will auto-redirect on app open if needed
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
                    // Always dismiss progress and re-enable button
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
                                        // After profile update, user is already signed in -> go to MainActivity
                                        Toast.makeText(SignUpActivity.this, "Đăng ký thành công. Bạn đã được đăng nhập.", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                                        intent.putExtra("from_startapp", true);
                                        intent.putExtra("displayName", fullName);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        finish();
                                    });
                        } else {
                            // Very rare: no current user even though creation succeeded; still go to MainActivity to continue UX
                            Toast.makeText(SignUpActivity.this, "Đăng ký thành công. Bạn đã được đăng nhập.", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                            intent.putExtra("from_startapp", true);
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
                });
    }

    private void safeShowProgress() {
        try {
            if (progressDialog == null) {
                ProgressBar pb = new ProgressBar(this);
                LinearLayout layout = new LinearLayout(this);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(40, 30, 40, 30);
                layout.setGravity(Gravity.CENTER);
                layout.addView(pb);

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setView(layout);
                builder.setCancelable(false);
                progressDialog = builder.create();
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

