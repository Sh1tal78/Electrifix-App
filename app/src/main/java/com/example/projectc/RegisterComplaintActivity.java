package com.example.projectc;

import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterComplaintActivity extends AppCompatActivity {
    private Spinner departmentSpinner, prioritySpinner;
    private EditText typeEditText, contactPersonEditText, phoneEditText, emailEditText, descriptionEditText, locationEditText;
    private Button submitComplaintButton;
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
        setContentView(R.layout.activity_register_complaint);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize UI elements
        departmentSpinner = findViewById(R.id.departmentSpinner);
        prioritySpinner = findViewById(R.id.prioritySpinner);
        typeEditText = findViewById(R.id.typeEditText);
        contactPersonEditText = findViewById(R.id.contactPersonEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        emailEditText = findViewById(R.id.emailEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        locationEditText = findViewById(R.id.locationEditText);
        submitComplaintButton = findViewById(R.id.submitComplaintButton);

        // Populate Department Spinner
        ArrayAdapter<CharSequence> departmentAdapter = new ArrayAdapter<>(this, R.layout.spinner_dropdown_item, getResources().getStringArray(R.array.departments));
        departmentAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        departmentSpinner.setAdapter(departmentAdapter);

        // Populate Priority Spinner
        ArrayAdapter<CharSequence> priorityAdapter = ArrayAdapter.createFromResource(this, R.array.priority_levels, android.R.layout.simple_spinner_item);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);

        // Handle Complaint Submission
        submitComplaintButton.setOnClickListener(view -> {
            String department = departmentSpinner.getSelectedItem().toString();
            String priority = prioritySpinner.getSelectedItem().toString();
            String type = typeEditText.getText().toString();
            String contactPerson = contactPersonEditText.getText().toString();
            String phone = phoneEditText.getText().toString();
            String email = emailEditText.getText().toString();
            String description = descriptionEditText.getText().toString();
            String location = locationEditText.getText().toString();

            if (type.isEmpty() || contactPerson.isEmpty() || phone.isEmpty() || email.isEmpty() || description.isEmpty() || location.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Disable the submit button to prevent double clicks
            submitComplaintButton.setEnabled(false);

            // Check if a complaint already exists for the same email and type
            db.collection("complaints")
                    .whereEqualTo("email", email)
                    .whereEqualTo("type", type)  // Optionally add more fields for unique identification
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            // If complaint already exists
                            Toast.makeText(this, "Complaint already registered.", Toast.LENGTH_SHORT).show();
                            submitComplaintButton.setEnabled(true); // Re-enable the submit button
                        } else {
                            // If no existing complaint, create and add the new complaint
                            Complaint complaint = new Complaint(department, type, contactPerson, phone, email, description, priority, location);
                            db.collection("complaints").add(complaint)
                                    .addOnSuccessListener(documentReference -> {
                                        Toast.makeText(this, "Complaint Registered Successfully!", Toast.LENGTH_SHORT).show();
                                        submitComplaintButton.setEnabled(true); // Re-enable the submit button after success
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Failed to Register Complaint.", Toast.LENGTH_SHORT).show();
                                        submitComplaintButton.setEnabled(true); // Re-enable the submit button after failure
                                    });
                        }
                    });
        });
    }
}
