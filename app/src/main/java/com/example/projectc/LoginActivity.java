package com.example.projectc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private EditText emailField, passwordField;
    private Button loginButton;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Disable night mode for all API levels
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            getTheme().applyStyle(R.style.Theme_ProjectC, true);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        super.onCreate(savedInstanceState);

        // Check if the user is already logged in
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        String userRole = sharedPreferences.getString("userRole", null);

        if (userRole != null) {
            navigateToDashboard(userRole);
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        // Initialize UI elements
        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        loginButton = findViewById(R.id.loginButton);
        progressBar = findViewById(R.id.progressBar);

        // Links for Forgot Password and Registration
        TextView forgotPasswordLink = findViewById(R.id.forgotPasswordLink);
        forgotPasswordLink.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, ResetPasswordActivity.class);
            startActivity(intent);
        });

        TextView registerLink = findViewById(R.id.registerLink);
        registerLink.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loginButton.setOnClickListener(view -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            // Check for empty fields
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(LoginActivity.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show progress bar during login
            progressBar.setVisibility(View.VISIBLE);

            // Authenticate user using Firebase Authentication
            auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        // Check if the user is a Super Admin
                        checkIfSuperAdmin(email);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(LoginActivity.this, "Login Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    });
        });
    }

    // Check if the user is a Super Admin by querying Firestore
    private void checkIfSuperAdmin(String email) {
        db.collection("superadmins")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // User is a Super Admin
                        saveLoginState("superadmin", email);  // Save user role as superadmin
                        saveSuperAdminDetails(email);  // Save Super Admin details
                        Toast.makeText(LoginActivity.this, "Welcome Super Admin!", Toast.LENGTH_SHORT).show();
                        navigateToDashboard("superadmin");
                    } else {
                        // User is not a Super Admin, check if they are an Admin or Client
                        checkAdminCredentials(email);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LoginActivity.this, "Error checking Super Admin status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }

    // Check if the user is an Admin by querying Firestore
    private void checkAdminCredentials(String email) {
        db.collection("admins")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // User is an Admin
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            List<String> assignedDepartments = (List<String>) document.get("departments");
                            boolean is7thAdmin = assignedDepartments != null && assignedDepartments.contains("ALL");

                            saveLoginState("admin", email);  // Save user role as admin
                            saveAdminDetails(email, assignedDepartments, is7thAdmin);  // Save admin details like departments

                            Toast.makeText(LoginActivity.this, "Welcome Admin!", Toast.LENGTH_SHORT).show();
                            navigateToDashboard("admin");
                            return;
                        }
                    }

                    // If not an Admin, treat the user as a Client
                    saveLoginState("client", email);  // Save user role as client
                    Toast.makeText(LoginActivity.this, "Welcome Client!", Toast.LENGTH_SHORT).show();
                    navigateToDashboard("client");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LoginActivity.this, "Error checking Admin status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }

    // Save login state in SharedPreferences
    private void saveLoginState(String role, String email) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("userRole", role);
        if (email != null) editor.putString("userEmail", email);  // Save email if provided
        editor.apply();

        // Debugging log for email
        Log.d("LoginActivity", "User email saved: " + email);
    }

    // Save Super Admin details in SharedPreferences
    private void saveSuperAdminDetails(String email) {
        SharedPreferences sharedPreferences = getSharedPreferences("SuperAdminPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("email", email);
        editor.putBoolean("isSuperAdmin", true);
        editor.apply();
    }

    // Save admin details (email, departments, etc.)
    private void saveAdminDetails(String email, List<String> assignedDepartments, boolean is7thAdmin) {
        SharedPreferences sharedPreferences = getSharedPreferences("AdminPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("email", email);
        editor.putBoolean("isAdmin", true);
        editor.putBoolean("is7thAdmin", is7thAdmin);
        editor.putStringSet("departments", new HashSet<>(assignedDepartments));
        editor.apply();
    }

    // Navigate to the appropriate dashboard based on role
    private void navigateToDashboard(String role) {
        Intent intent;
        switch (role) {
            case "superadmin":
                intent = new Intent(this, SuperAdminDashboardActivity.class);
                break;
            case "admin":
                intent = new Intent(this, AdminDashboardActivity.class);
                break;
            case "client":
                intent = new Intent(this, ClientDashboardActivity.class);
                break;
            default:
                Toast.makeText(this, "Unknown role", Toast.LENGTH_SHORT).show();
                return;
        }
        startActivity(intent);
        finish();
    }
}