package com.example.to_do_app.activitys;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;

import com.example.to_do_app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * StartAppActivity:
 * - Always show start page when no authenticated user.
 * - If user is already authenticated, auto-enter MainActivity.
 * - When user taps "BẮT ĐẦU", always go to SignInActivity.
 */
public class StartAppActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_start_app);

        mAuth = FirebaseAuth.getInstance();

        // Insets handling (preserve original padding)
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
                // Always go to SignInActivity
                Intent intent = new Intent(StartAppActivity.this, SignInActivity.class);
                // ensure SignInActivity will be fresh
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                // finish StartApp so back won't return here and possibly bypass sign-in
                finish();
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // If a user is already authenticated, skip StartApp and go to MainActivity
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Intent i = new Intent(StartAppActivity.this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        }
        // Otherwise keep StartApp visible so user must tap "BẮT ĐẦU"
    }
}