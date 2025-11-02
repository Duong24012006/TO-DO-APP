package com.example.to_do_app.activitys;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.to_do_app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignInActivity extends AppCompatActivity {

    private EditText etUserName, etPassword;
    private Button btnSignIn, btnChangeSignUp;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        mAuth = FirebaseAuth.getInstance();

        etUserName = findViewById(R.id.user_name);
        etPassword = findViewById(R.id.user_password);
        btnSignIn = findViewById(R.id.sign_in_button);
        btnChangeSignUp = findViewById(R.id.change_sign_up);

        btnSignIn.setOnClickListener(v -> {
            hideKeyboard();
            String email = etUserName.getText() != null ? etUserName.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

            if (TextUtils.isEmpty(email)) {
                etUserName.setError("Vui lòng nhập email");
                etUserName.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(password)) {
                etPassword.setError("Vui lòng nhập mật khẩu");
                etPassword.requestFocus();
                return;
            }

            btnSignIn.setEnabled(false);

            // Đăng nhập với Firebase
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        btnSignIn.setEnabled(true);
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            String displayName = user != null ? user.getDisplayName() : null;

                            // Chuyển sang MainActivity, truyền displayName nếu có
                            Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                            intent.putExtra("displayName", displayName);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(SignInActivity.this, "Đăng nhập thất bại: " +
                                    (task.getException() != null ? task.getException().getMessage() : ""), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        btnChangeSignUp.setOnClickListener(v -> {
            hideKeyboard();
            startActivity(new Intent(SignInActivity.this, SignUpActivity.class));
        });
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