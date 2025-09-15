package com.example.projectc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AdminDashboardActivity extends AppCompatActivity {
    private Button viewComplaintsButton, manageUsersButton, reportsButton;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

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
        setContentView(R.layout.activity_admin_dashboard);

        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        viewComplaintsButton = findViewById(R.id.viewComplaintsButton);


        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        // Get user email from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        String userEmail = sharedPreferences.getString("userEmail", "");

        // Set user email in navigation header
        View headerView = navigationView.getHeaderView(0);
        TextView adminEmailTextView = headerView.findViewById(R.id.adminEmailTextView);
        adminEmailTextView.setText(userEmail);

        // Set up navigation item selection
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_logout) {
                logoutUser();
                return true;
            } else if (id == R.id.nav_complaints) {
                startComplaintsActivity();
                return true;
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return false;
        });

        // Button click listeners
        viewComplaintsButton.setOnClickListener(v -> startComplaintsActivity());


    }

    private void startComplaintsActivity() {
        SharedPreferences adminPreferences = getSharedPreferences("AdminPrefs", MODE_PRIVATE);
        boolean isAdmin = adminPreferences.getBoolean("isAdmin", false);
        Set<String> departmentsSet = adminPreferences.getStringSet("departments", null);
        List<String> assignedDepartments = new ArrayList<>();
        if (departmentsSet != null) {
            assignedDepartments = new ArrayList<>(departmentsSet);
        }

        Intent intent = new Intent(this, AdminViewComplaintsActivity.class);
        intent.putExtra("isAdmin", isAdmin);
        intent.putExtra("departments", assignedDepartments.toArray(new String[0]));
        startActivity(intent);
    }

    private void logoutUser() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        drawerLayout.openDrawer(navigationView);
        return true;
    }
}