package com.example.to_do_app.activitys;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.to_do_app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

/**
 * SignInActivity — merged and corrected
 *
 * Key behaviors:
 * - Uses only addOnCompleteListener to centralize handling and avoid duplicate flows.
 * - Uses instanceof checks for FirebaseAuth exceptions.
 * - Ensures progress dialog is dismissed and sign-in button re-enabled on every path.
 * - Prefills email when provided and allows navigation to SignUpActivity.
 */
public class SignInActivity extends AppCompatActivity {

    private EditText etUserName, etPassword;
    private Button btnSignIn, btnChangeSignUp;
    private FirebaseAuth mAuth;
    private AlertDialog progressDialog;

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

            // Attempt sign-in and handle all results in onComplete
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(signInTask -> {
                        // Always dismiss progress and re-enable button
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
                            return;
                        }

                        Exception ex = signInTask.getException();
                        // Prefer typed exception checks
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
                            // Fallback: attempt to inspect message (compatibility) or show generic
                            String msg = "Đăng nhập thất bại";
                            if (ex != null && ex.getMessage() != null && !ex.getMessage().isEmpty()) {
                                String lower = ex.getMessage().toLowerCase();
                                // Some Firebase backends return messages mentioning user-not-found; handle defensively
                                if (lower.contains("no user") || lower.contains("user-not-found") || lower.contains("no user record")) {
                                    Toast.makeText(SignInActivity.this, "Email chưa được đăng ký. Chuyển sang trang đăng ký.", Toast.LENGTH_LONG).show();
                                    Intent i = new Intent(SignInActivity.this, SignUpActivity.class);
                                    i.putExtra("prefillEmail", email);
                                    startActivity(i);
                                    return;
                                }
                                msg += ": " + ex.getMessage();
                            }
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
        } catch (Exception ignored) { }
    }

    private void safeDismissProgress() {
        try {
            if (progressDialog != null && !isFinishing() && !isDestroyed() && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        } catch (Exception ignored) { }
    }

    @Override
    protected void onDestroy() {
        safeDismissProgress();
        super.onDestroy();
    }
}