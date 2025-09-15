package com.example.projectc;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameField, emailField, phoneField, passwordField;
    private Button registerButton;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // Email pattern for validation
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

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
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_register);

        // Initialize UI elements
        nameField = findViewById(R.id.nameField);
        emailField = findViewById(R.id.emailField);
        phoneField = findViewById(R.id.phoneField);
        passwordField = findViewById(R.id.passwordField);
        registerButton = findViewById(R.id.registerButton);
        TextView loginRedirect = findViewById(R.id.loginRedirect); // "Already have an account?" TextView
        progressBar = findViewById(R.id.progressBar);

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Register Button Click Listener
        registerButton.setOnClickListener(view -> {
            String name = nameField.getText().toString().trim();
            String email = emailField.getText().toString().trim();
            String phone = phoneField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();
            String role = "client";

            // Validation check
            if (validateInputs(name, email, phone, password)) {
                registerUser(name, email, phone, password, role);
            }
        });

        // Redirect to LoginActivity when "Already have an account?" is clicked
        loginRedirect.setOnClickListener(view -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish(); // Close RegisterActivity so the user can't go back to it
        });
    }

    private boolean validateInputs(String name, String email, String phone, String password) {
        // Check if fields are empty
        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validate email format
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Simple password strength check (minimum 6 characters)
        if (password.length() < 6) {
            Toast.makeText(this, "Password should be at least 6 characters", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void registerUser(String name, String email, String phone, String password, String role) {
        progressBar.setVisibility(ProgressBar.VISIBLE);  // Show progress bar while registering

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String userId = authResult.getUser().getUid();
                    Map<String, Object> user = new HashMap<>();
                    user.put("name", name);
                    user.put("email", email);
                    user.put("phone", phone);
                    user.put("role", role);
                    user.put("status", "pending"); // Default status

                    // Store user data in Firestore
                    db.collection("users").document(userId).set(user)
                            .addOnSuccessListener(aVoid -> {
                                progressBar.setVisibility(ProgressBar.GONE);  // Hide progress bar
                                Toast.makeText(this, "Registered successfully.", Toast.LENGTH_SHORT).show();
                                // Redirect to Login Activity after successful registration
                                startActivity(new Intent(this, LoginActivity.class));
                                finish();  // Close RegisterActivity to prevent going back
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(ProgressBar.GONE);  // Hide progress bar
                                Toast.makeText(this, "Registration Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(ProgressBar.GONE);  // Hide progress bar
                    Toast.makeText(this, "Registration Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
