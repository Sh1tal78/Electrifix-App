package com.example.projectc;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.List;
import java.util.Locale;

public class ViewComplaintsActivity extends AppCompatActivity {
    private RecyclerView complaintsRecyclerView;
    private ComplaintsAdapter adapter;
    private FirebaseFirestore db;
    private boolean isAdmin = false; // Flag for admin status
    private boolean isSuperAdmin = false; // Flag for super admin status
    private View progressBar; // Progress Bar
    private TextView noComplaintsMessage; // "No Complaints Yet" message
    private SearchView searchView; // SearchView for searching complaints
    private List<Complaint> complaintsList = new ArrayList<>(); // Full list of complaints

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

        complaintsRecyclerView = findViewById(R.id.complaintsRecyclerView);
        complaintsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        db = FirebaseFirestore.getInstance();

        // Initialize the Progress Bar, No Complaints message, and SearchView
        progressBar = findViewById(R.id.progressBar);
        noComplaintsMessage = findViewById(R.id.noComplaintsMessage);
        searchView = findViewById(R.id.searchView);

        // Initially, show progress bar while data is being fetched
        progressBar.setVisibility(View.VISIBLE);
        noComplaintsMessage.setVisibility(View.GONE);
        complaintsRecyclerView.setVisibility(View.GONE);

        // Get the logged-in user's email
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String loggedInUserEmail = auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : null;

        if (loggedInUserEmail != null) {
            // Check if the user is a Super Admin
            SharedPreferences superAdminPrefs = getSharedPreferences("SuperAdminPrefs", MODE_PRIVATE);
            isSuperAdmin = superAdminPrefs.getBoolean("isSuperAdmin", false);

            // Check if the user is an Admin
            SharedPreferences adminPrefs = getSharedPreferences("AdminPrefs", MODE_PRIVATE);
            isAdmin = adminPrefs.getBoolean("isAdmin", false);

            // Fetch complaints based on the user's role
            fetchComplaints(loggedInUserEmail);
        } else {
            Log.e("Auth", "User not logged in");
            progressBar.setVisibility(View.GONE);
        }

        setupSearchFunctionality();
    }

    private void fetchComplaints(String loggedInUserEmail) {
        db.collection("complaints")
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        complaintsList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Complaint complaint = document.toObject(Complaint.class);
                            complaint.setId(document.getId());
                            complaintsList.add(complaint);
                        }

                        if (complaintsList.isEmpty()) {
                            noComplaintsMessage.setVisibility(View.VISIBLE);
                            complaintsRecyclerView.setVisibility(View.GONE);
                        } else {
                            // Pass the Super Admin and Admin flags to the adapter
                            adapter = new ComplaintsAdapter(complaintsList, isAdmin, isSuperAdmin, loggedInUserEmail, new ArrayList<>());
                            complaintsRecyclerView.setAdapter(adapter);
                            complaintsRecyclerView.setVisibility(View.VISIBLE);
                            noComplaintsMessage.setVisibility(View.GONE);
                        }

                        progressBar.setVisibility(View.GONE);
                    } else {
                        Log.e("Firestore", "Error getting documents: ", task.getException());
                        Toast.makeText(ViewComplaintsActivity.this, "Error fetching complaints", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    }
                }).addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching complaints: ", e);
                    Toast.makeText(ViewComplaintsActivity.this, "Error fetching complaints", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
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

        // Convert query to lowercase and trim it
        String lowerQuery = query.toLowerCase().trim();

        for (Complaint complaint : complaintsList) {
            boolean matches = false;

            // Match against various complaint fields
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

            // If status matches or any other field matches, add to the filtered list
            if (matches) {
                filteredList.add(complaint);
            }
        }

        // If no complaints match the query, show the "No Complaints" message
        if (filteredList.isEmpty()) {
            noComplaintsMessage.setVisibility(View.VISIBLE);
            complaintsRecyclerView.setVisibility(View.GONE);
        } else {
            noComplaintsMessage.setVisibility(View.GONE);
            complaintsRecyclerView.setVisibility(View.VISIBLE);
        }

        // Update the adapter with the filtered list
        adapter.updateList(filteredList);
    }
}