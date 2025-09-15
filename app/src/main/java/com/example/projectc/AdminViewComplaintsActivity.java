

package com.example.projectc;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class AdminViewComplaintsActivity extends AppCompatActivity {
    private RecyclerView complaintsRecyclerView;
    private ComplaintsAdapter adapter;
    private FirebaseFirestore db;
    private boolean isAdmin = false;
    private boolean isSuperAdmin = false;
    private List<String> assignedDepartments = new ArrayList<>();
    private View progressBar;
    private TextView noComplaintsMessage;
    private SearchView searchView;
    private List<Complaint> complaintsList = new ArrayList<>();

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
        setContentView(R.layout.activity_view_complaints);

        // Initialize views
        complaintsRecyclerView = findViewById(R.id.complaintsRecyclerView);
        complaintsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        db = FirebaseFirestore.getInstance();

        progressBar = findViewById(R.id.progressBar);
        noComplaintsMessage = findViewById(R.id.noComplaintsMessage);
        searchView = findViewById(R.id.searchView);

        // Set initial visibility
        progressBar.setVisibility(View.VISIBLE);
        noComplaintsMessage.setVisibility(View.GONE);
        complaintsRecyclerView.setVisibility(View.GONE);

        // Handle back press to navigate to dashboard
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateToDashboard();
            }
        });

        // Retrieve intent extras
        Intent intent = getIntent();
        isAdmin = intent.getBooleanExtra("isAdmin", false);
        String[] departments = intent.getStringArrayExtra("departments");

        if (departments != null) {
            assignedDepartments = Arrays.asList(departments);
        } else {
            Toast.makeText(this, "No departments assigned to admin!", Toast.LENGTH_SHORT).show();
        }

        // Fetch complaints if user is an admin
        if (isAdmin) {
            fetchComplaints();
        }

        // Setup search functionality
        setupSearchFunctionality();
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(AdminViewComplaintsActivity.this, AdminDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void fetchComplaints() {
        Log.d("AdminViewComplaints", "Fetching complaints for admin...");
        progressBar.setVisibility(View.VISIBLE);  // Ensure progress bar is visible during loading

        db.collection("complaints")
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE); // Hide progress bar once the task completes

                    if (task.isSuccessful() && task.getResult() != null) {
                        complaintsList.clear();
                        Log.d("AdminViewComplaints", "Documents fetched: " + task.getResult().size());
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Complaint complaint = document.toObject(Complaint.class);
                            complaint.setId(document.getId());

                            // Filter complaints by assigned departments
                            if (assignedDepartments.contains("ALL") || assignedDepartments.contains(complaint.getDepartment())) {
                                complaintsList.add(complaint);
                            }
                        }

                        updateRecyclerView();
                    } else {
                        Log.e("Firestore", "Error: ", task.getException());
                        Toast.makeText(this, "Error fetching complaints", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateRecyclerView() {
        if (complaintsList.isEmpty()) {
            noComplaintsMessage.setVisibility(View.VISIBLE);
            complaintsRecyclerView.setVisibility(View.GONE);
        } else {
            adapter = new ComplaintsAdapter(complaintsList, isAdmin, isSuperAdmin,
                    FirebaseAuth.getInstance().getCurrentUser().getEmail(), assignedDepartments);
            complaintsRecyclerView.setAdapter(adapter);
            complaintsRecyclerView.setVisibility(View.VISIBLE);
            noComplaintsMessage.setVisibility(View.GONE);
        }
    }

    private void setupSearchFunctionality() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterComplaints(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterComplaints(newText);
                return true;
            }
        });
    }

    private void filterComplaints(String query) {
        List<Complaint> filteredList = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String lowerQuery = query.toLowerCase().trim();

        for (Complaint complaint : complaintsList) {
            boolean matches = false;

            if (complaint.getDescription() != null && complaint.getDescription().toLowerCase().contains(lowerQuery)) {
                matches = true;
            } else if (complaint.getContactPerson() != null && complaint.getContactPerson().toLowerCase().contains(lowerQuery)) {
                matches = true;
            } else if (complaint.getStatus() != null && complaint.getStatus().toLowerCase().contains(lowerQuery)) {
                matches = true;
            } else if (complaint.getDepartment() != null && complaint.getDepartment().toLowerCase().contains(lowerQuery)) {
                matches = true;
            } else if (complaint.getEmail() != null && complaint.getEmail().toLowerCase().contains(lowerQuery)) {
                matches = true;
            } else if (complaint.getType() != null && complaint.getType().toLowerCase().contains(lowerQuery)) {
                matches = true;
            } else if (complaint.getDate() != null) {
                String formattedDate = dateFormat.format(complaint.getDate());
                if (formattedDate.toLowerCase().contains(lowerQuery)) {
                    matches = true;
                }
            } else if (complaint.getPhone() != null && complaint.getPhone().toLowerCase().contains(lowerQuery)) {
                matches = true;
            }

            if (matches) {
                filteredList.add(complaint);
            }
        }

        if (filteredList.isEmpty()) {
            noComplaintsMessage.setVisibility(View.VISIBLE);
            complaintsRecyclerView.setVisibility(View.GONE);
        } else {
            noComplaintsMessage.setVisibility(View.GONE);
            complaintsRecyclerView.setVisibility(View.VISIBLE);
        }

        if (adapter != null) {
            adapter.updateList(filteredList);
        }
    }
}