package com.example.to_do_app.activitys;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.to_do_app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class StartAppActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_start_app);

        mAuth = FirebaseAuth.getInstance();

        // Nếu user đã đăng nhập -> chuyển ngay tới MainActivity (reset task)
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Intent i = new Intent(StartAppActivity.this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
            return;
        }

        // Insets handling (bảo toàn padding ban đầu)
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
                // Mở màn đăng nhập
                Intent intent = new Intent(StartAppActivity.this, SignInActivity.class);
                startActivity(intent);
            });
        }
    }
}