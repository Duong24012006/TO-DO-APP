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

public class StartAppActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_start_app);

        final View mainView = findViewById(R.id.main);
        final int initialPaddingLeft = mainView.getPaddingLeft();
        final int initialPaddingRight = mainView.getPaddingRight();

        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            // Get the insets for the system bars that are affecting the content
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            Insets navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());

            // Apply the top and bottom insets as padding, preserving the original horizontal padding
            v.setPadding(
                initialPaddingLeft,
                statusBars.top,
                initialPaddingRight,
                navBars.bottom
            );

            // Return the insets so that child views can also consume them
            return insets;
        });

        Button startButton = findViewById(R.id.batdau);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StartAppActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Close StartAppActivity so user cannot go back to it
            }
        });
    }
}
