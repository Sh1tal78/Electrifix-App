package com.example.projectc;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {
    private List<User> userList; // Current list displayed in RecyclerView
    private List<User> originalList; // Full list used for filtering
    private FirebaseFirestore db;

    public UsersAdapter(List<User> userList, FirebaseFirestore db) {
        this.userList = new ArrayList<>(userList); // Copy to avoid affecting originalList
        this.originalList = new ArrayList<>(userList); // Maintain the full list for filtering
        this.db = db;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.nameTextView.setText("User Name: " + user.getName());
        holder.emailTextView.setText("Email: " + user.getEmail());
        holder.phoneTextView.setText("Phone Number: " + user.getPhone());


        holder.removeButton.setOnClickListener(view -> {
            db.collection("users").document(user.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(view.getContext(), "User removed!", Toast.LENGTH_SHORT).show();
                        userList.remove(position);
                        originalList.remove(user); // Also remove from the original list
                        notifyItemRemoved(position);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(view.getContext(), "Failed to remove user", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void filterList(String query) {
        if (TextUtils.isEmpty(query)) {
            // Reset to the original list if the query is empty
            userList = new ArrayList<>(originalList);
        } else {
            // Filter the list based on the query
            List<User> filteredList = new ArrayList<>();
            for (User user : originalList) {
                if (user.getName().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(user);
                }
            }
            userList = filteredList;
        }
        notifyDataSetChanged();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, emailTextView, phoneTextView;
        Button removeButton;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            emailTextView = itemView.findViewById(R.id.emailTextView);
            phoneTextView = itemView.findViewById(R.id.phoneTextView);

            removeButton = itemView.findViewById(R.id.removeButton);
        }
    }
}
