package com.example.to_do_app.activitys;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
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
 * - Always show start page.
 * - If user is already authenticated, do NOT auto-enter MainActivity.
 * - When user taps "BẮT ĐẦU", always go to SignInActivity.
 * This guarantees the user must go through SignInActivity first.
 */
public class StartAppActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
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
        // Intentionally do NOT auto-redirect based on FirebaseAuth here.
        // This ensures StartAppActivity is always shown first and user must explicitly tap "BẮT ĐẦU".
        // (If you previously added mAuth.signOut() to force re-login, ensure it's handled consistently.)
        FirebaseUser currentUser = mAuth.getCurrentUser();
        // OPTIONAL: If you want to prevent showing "BẮT ĐẦU" to already signed in users and force them to sign in again,
        // uncomment the following to always go to SignInActivity regardless of currentUser:
        //
        // Intent i = new Intent(StartAppActivity.this, SignInActivity.class);
        // i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        // startActivity(i);
        // finish();
    }
}