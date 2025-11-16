package com.example.to_do_app.activitys;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
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
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

/**
 * SignInActivity — simplified and corrected
 *
 * Key changes:
 * - Only uses addOnCompleteListener (no addOnFailureListener) to avoid duplicate handling.
 * - Uses instanceof checks for FirebaseAuth exceptions instead of parsing error strings.
 * - On successful sign-in navigates to MainActivity and clears back stack.
 */
public class SignInActivity extends AppCompatActivity {

    private EditText etUserName, etPassword;
    private Button btnSignIn, btnChangeSignUp;
    private FirebaseAuth mAuth;
    private ProgressDialog progress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        mAuth = FirebaseAuth.getInstance();

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

            // Directly attempt sign-in and handle in onComplete only
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(signInTask -> {
                        safeDismissProgress();
                        btnSignIn.setEnabled(true);

                        if (signInTask.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            String displayName = user != null ? user.getDisplayName() : null;

                            // Move to MainActivity and clear back stack
                            Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                            intent.putExtra("from_startapp", true);
                            if (displayName != null) intent.putExtra("displayName", displayName);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Exception ex = signInTask.getException();
                            if (ex instanceof FirebaseAuthInvalidUserException) {
                                // user not found -> send to SignUp
                                Toast.makeText(SignInActivity.this, "Email chưa được đăng ký. Chuyển sang trang đăng ký.", Toast.LENGTH_LONG).show();
                                Intent i = new Intent(SignInActivity.this, SignUpActivity.class);
                                i.putExtra("prefillEmail", email);
                                startActivity(i);
                            } else if (ex instanceof FirebaseAuthInvalidCredentialsException) {
                                // wrong password
                                showToast("Mật khẩu không đúng. Vui lòng thử lại.");
                                etPassword.requestFocus();
                            } else {
                                String msg = "Đăng nhập thất bại";
                                if (ex != null && ex.getMessage() != null) msg += ": " + ex.getMessage();
                                showToast(msg);
                            }
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

    @Override
    protected void onStart() {
        super.onStart();
        // intentionally do not auto-skip to MainActivity here — StartApp handles auto-redirect on app open
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