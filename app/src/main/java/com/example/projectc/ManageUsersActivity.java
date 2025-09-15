package com.example.projectc;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;

import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ManageUsersActivity extends AppCompatActivity {
    private RecyclerView usersRecyclerView;
    private UsersAdapter adapter;
    private FirebaseFirestore db;
    private ProgressBar progressBar;
    private TextView noUsersMessage;
    private SearchView searchView;

    private List<User> userList = new ArrayList<>(); // Full list of users

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
        setContentView(R.layout.activity_manage_users);

        // Initialize views
        usersRecyclerView = findViewById(R.id.usersRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        noUsersMessage = findViewById(R.id.noUsersMessage);
        searchView = findViewById(R.id.searchView);

        // Set up RecyclerView layout manager
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize Firestore instance
        db = FirebaseFirestore.getInstance();

        // Show the progress bar while data is loading
        progressBar.setVisibility(View.VISIBLE);
        noUsersMessage.setVisibility(View.GONE);

        // Fetch users from Firestore, excluding the 'super_admin' role
        fetchUsers();

        // Set up SearchView listener
        setupSearchView();
    }

    /**
     * Fetch users from Firestore excluding 'super_admin' role and update the RecyclerView.
     */
    private void fetchUsers() {
        db.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful() && task.getResult() != null) {
                        userList.clear(); // Clear the list before adding new data

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String role = document.getString("role");

                            // Exclude users with "admin" or "superadmin" roles
                            if (!"admin".equals(role) && !"superadmin".equals(role)) {
                                String id = document.getId();
                                String name = document.getString("name");
                                String email = document.getString("email");
                                String phone = document.getString("phone");
                                String status = document.getString("status");

                                User user = new User(id, name, email, phone, role, status);
                                userList.add(user);
                            }
                        }

                        if (userList.isEmpty()) {
                            noUsersMessage.setVisibility(View.VISIBLE);
                            noUsersMessage.setText("No users found.");
                        } else {
                            noUsersMessage.setVisibility(View.GONE);
                            adapter = new UsersAdapter(userList, db);
                            usersRecyclerView.setAdapter(adapter);
                        }
                    } else {
                        noUsersMessage.setVisibility(View.VISIBLE);
                        noUsersMessage.setText("Failed to load users.");
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    noUsersMessage.setVisibility(View.VISIBLE);
                    noUsersMessage.setText("Error loading users: " + e.getMessage());
                });
    }

    /**
     * Set up the SearchView to filter the user list.
     */
    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterUsers(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterUsers(newText);
                return false;
            }
        });
    }

    /**
     * Filter the user list based on the query and update the RecyclerView.
     *
     * @param query The search query.
     */
    private void filterUsers(String query) {
        if (adapter != null && !TextUtils.isEmpty(query)) {
            adapter.filterList(query);
        } else if (adapter != null) {
            adapter.filterList(""); // Reset the list if the query is empty
        }
    }
}
