package com.example.to_do_app.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.to_do_app.R;
import com.example.to_do_app.activitys.SignInActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class ProfileFragment extends Fragment {

    // Firebase Auth
    private FirebaseAuth mAuth;

    // UI Components
    private ImageView ivAvatar;
    private TextView tvUserName;
    private TextView tvUserEmail;
    private Button btnLogout;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.profile_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        ivAvatar = view.findViewById(R.id.iv_avatar);
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvUserEmail = view.findViewById(R.id.tv_user_email);
        btnLogout = view.findViewById(R.id.btn_logout);

        // Load user profile information
        loadUserProfile();

        // Set up logout button listener
        btnLogout.setOnClickListener(v -> logoutUser());
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // Get user info
            String name = currentUser.getDisplayName();
            String email = currentUser.getEmail();
            Uri photoUrl = currentUser.getPhotoUrl();

            // Set user name
            if (name != null && !name.isEmpty()) {
                tvUserName.setText(name);
            } else {
                // If display name is not set, use the part of the email before "@"
                if (email != null && email.contains("@")) {
                    tvUserName.setText(email.substring(0, email.indexOf("@")));
                } else {
                    tvUserName.setText("Người dùng");
                }
            }

            // Set user email
            tvUserEmail.setText(email);

            // Set user avatar using Glide
            if (photoUrl != null && getContext() != null) {
                Glide.with(getContext())
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_profile);
            }
        } else {
            // No user is logged in
            tvUserName.setText("Khách");
            tvUserEmail.setText("Vui lòng đăng nhập");
            btnLogout.setText("Đăng nhập");
            btnLogout.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), SignInActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                if (getActivity() != null) {
                    getActivity().finish();
                }
            });
        }
    }

    private void logoutUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return; // Already logged out

        // Sign out from Firebase
        mAuth.signOut();

        // Show a toast message
        Toast.makeText(getContext(), "Đã đăng xuất", Toast.LENGTH_SHORT).show();

        // Navigate back to the SignInActivity
        Intent intent = new Intent(getActivity(), SignInActivity.class);
        // Clear the activity stack
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // Finish the current activity
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}
