package com.example.projectc;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ComplaintsAdapter extends RecyclerView.Adapter<ComplaintsAdapter.ComplaintViewHolder> {
    private List<Complaint> complaintList;
    private boolean isAdmin;
    private boolean isSuperAdmin;
    private String loggedInUserEmail;
    private List<String> assignedDepartments;

    public ComplaintsAdapter(List<Complaint> complaintList, boolean isAdmin, boolean isSuperAdmin,
                             String loggedInUserEmail, List<String> assignedDepartments) {
        this.complaintList = complaintList;
        this.isAdmin = isAdmin;
        this.isSuperAdmin = isSuperAdmin;
        this.loggedInUserEmail = loggedInUserEmail;
        this.assignedDepartments = assignedDepartments;
    }

    @NonNull
    @Override
    public ComplaintViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_complaint, parent, false);
        return new ComplaintViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ComplaintViewHolder holder, int position) {
        Complaint complaint = complaintList.get(position);
        Context context = holder.itemView.getContext();

        // Set basic information
        holder.dateTextView.setText(formatDate(complaint.getDate()));
        holder.departmentTextView.setText("Department: " + complaint.getDepartment());
        holder.typeTextView.setText("Complaint Type: "+ complaint.getType());
        holder.descriptionTextView.setText("Description: " + complaint.getDescription());

        // Set status with appropriate background
        holder.statusTextView.setText(complaint.getStatus());
        setStatusBackground(holder.statusTextView, complaint.getStatus());

        // Set contact information
        holder.contactPersonTextView.setText("Name: " + complaint.getContactPerson());
        holder.emailTextView.setText("Email: " + complaint.getEmail());
        holder.phoneTextView.setText("Phone: " + complaint.getPhone());

        // Set admin-specific information
        holder.locationTextView.setText("Location: " + complaint.getLocation());
        holder.priorityTextView.setText("Priority: " + complaint.getPriority());

        // Handle remarks
        if (complaint.getRemarks() != null && !complaint.getRemarks().isEmpty()) {
            holder.remarkLabelTextView.setVisibility(View.VISIBLE);
            holder.remarkTextView.setVisibility(View.VISIBLE);
            holder.remarkTextView.setText(complaint.getRemarks());
        } else {
            holder.remarkLabelTextView.setVisibility(View.GONE);
            holder.remarkTextView.setVisibility(View.GONE);
        }

        // Set up click listener for expand/collapse
        holder.cardView.setOnClickListener(v -> toggleExpansion(holder));

        // Set up PDF button
        holder.saveAsPdfButton.setOnClickListener(v -> {
            PdfGenerator.generatePdf(context, complaint);
        });

        // Set up admin controls
        setupAdminControls(holder, complaint, context);
    }

    private void toggleExpansion(ComplaintViewHolder holder) {
        boolean isExpanded = holder.expandableView.getVisibility() == View.VISIBLE;
        holder.expandableView.setVisibility(isExpanded ? View.GONE : View.VISIBLE);

        int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
            Complaint complaint = complaintList.get(position);

            // Show/hide buttons based on user role and expansion state
            if (isAdmin || isSuperAdmin || complaint.getEmail().equalsIgnoreCase(loggedInUserEmail)) {
                holder.buttonsContainer.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
                holder.saveAsPdfButton.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
            }
        }
    }


    private void setupAdminControls(ComplaintViewHolder holder, Complaint complaint, Context context) {
        if (isSuperAdmin) {
            holder.adminSection.setVisibility(View.VISIBLE);
            holder.updateStatusButton.setOnClickListener(v -> showUpdateDialog(context, complaint));
            holder.removeComplaintButton.setOnClickListener(v -> removeComplaint(context, complaint));
        }
        else if (isAdmin && (assignedDepartments.contains("ALL") ||
                assignedDepartments.contains(complaint.getDepartment()))) {
            holder.adminSection.setVisibility(View.VISIBLE);
            holder.updateStatusButton.setOnClickListener(v -> showUpdateDialog(context, complaint));
            holder.removeComplaintButton.setVisibility(View.GONE);
        }
        else if (complaint.getEmail().equals(loggedInUserEmail)) {
            // Regular user can see their own details but not admin controls
            holder.adminSection.setVisibility(View.GONE);
            holder.updateStatusButton.setVisibility(View.GONE);
            holder.removeComplaintButton.setVisibility(View.GONE);
        }
        else {
            // Hide all admin controls for other users
            holder.adminSection.setVisibility(View.GONE);
            holder.updateStatusButton.setVisibility(View.GONE);
            holder.removeComplaintButton.setVisibility(View.GONE);
        }
    }




    private void setStatusBackground(TextView statusTextView, String status) {
        int backgroundResId;
        switch (status.toLowerCase()) {
            case "pending":
                backgroundResId = R.drawable.bg_status_pending;
                break;
            case "in progress":
                backgroundResId = R.drawable.bg_status_in_progress;
                break;
            case "resolved":
                backgroundResId = R.drawable.bg_status_resolved;
                break;
            case "closed":
                backgroundResId = R.drawable.bg_status_closed;
                break;
            default:
                backgroundResId = R.drawable.bg_status_default;
                break;
        }
        statusTextView.setBackgroundResource(backgroundResId);
    }

    private void showUpdateDialog(Context context, Complaint complaint) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Update Complaint Status");

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_update_complaint, null);
        builder.setView(dialogView);

        Spinner statusSpinner = dialogView.findViewById(R.id.statusSpinner);
        EditText remarkEditText = dialogView.findViewById(R.id.remarkEditText);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                context, R.array.status_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(adapter);

        // Set current status in spinner
        String[] statusOptions = context.getResources().getStringArray(R.array.status_options);
        for (int i = 0; i < statusOptions.length; i++) {
            if (statusOptions[i].equalsIgnoreCase(complaint.getStatus())) {
                statusSpinner.setSelection(i);
                break;
            }
        }

        builder.setPositiveButton("Update", (dialog, which) -> {
            String status = statusSpinner.getSelectedItem().toString();
            String remarks = remarkEditText.getText().toString().trim();

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("complaints").document(complaint.getId())
                    .update("status", status, "remarks", remarks)
                    .addOnSuccessListener(aVoid -> {
                        complaint.setStatus(status);
                        complaint.setRemarks(remarks);
                        notifyDataSetChanged();
                        Toast.makeText(context, "Status updated", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void removeComplaint(Context context, Complaint complaint) {
        new AlertDialog.Builder(context)
                .setTitle("Confirm Removal")
                .setMessage("Are you sure you want to remove this complaint?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    db.collection("complaints").document(complaint.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                complaintList.remove(complaint);
                                notifyDataSetChanged();
                                Toast.makeText(context, "Complaint removed", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Removal failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String formatDate(Date date) {
        if (date != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a");
            return dateFormat.format(date);
        }
        return "Date not available";
    }

    public void updateList(List<Complaint> newComplaintList) {
        this.complaintList = newComplaintList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return complaintList.size();
    }

    static class ComplaintViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView dateTextView, departmentTextView, typeTextView, descriptionTextView;
        TextView statusTextView, remarkLabelTextView, remarkTextView;
        TextView contactPersonTextView, emailTextView, phoneTextView;
        TextView locationTextView, priorityTextView;
        View adminSection;
        View expandableView;
        View buttonsContainer;
        Button updateStatusButton, removeComplaintButton, saveAsPdfButton;

        public ComplaintViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            expandableView = itemView.findViewById(R.id.expandableView);

            // Compact view elements
            dateTextView = itemView.findViewById(R.id.dateTextView);
            departmentTextView = itemView.findViewById(R.id.complaintDepartment);
            typeTextView = itemView.findViewById(R.id.typeTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);

            // Expanded view elements
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            remarkLabelTextView = itemView.findViewById(R.id.remarkLabelTextView);
            remarkTextView = itemView.findViewById(R.id.remarkTextView);

            // Contact information
            contactPersonTextView = itemView.findViewById(R.id.contactPersonTextView);
            emailTextView = itemView.findViewById(R.id.emailTextView);
            phoneTextView = itemView.findViewById(R.id.phoneTextView);

            // Admin section
            adminSection = itemView.findViewById(R.id.adminSection);
            locationTextView = itemView.findViewById(R.id.locationTextView);
            priorityTextView = itemView.findViewById(R.id.priorityTextView);

            // Buttons container
            buttonsContainer = itemView.findViewById(R.id.buttonsContainer);
            updateStatusButton = itemView.findViewById(R.id.updateStatusButton);
            removeComplaintButton = itemView.findViewById(R.id.removeComplaintButton);
            saveAsPdfButton = itemView.findViewById(R.id.saveAsPdfButton);
        }
    }
}