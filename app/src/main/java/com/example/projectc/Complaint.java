package com.example.projectc;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Complaint {
    private String id; // Firestore Document ID (Firestore will generate this)
    private String department; // Department of the complaint
    private String type; // Type of complaint
    private String contactPerson; // Person lodging the complaint
    private String phone; // Contact phone number
    private String email; // Contact email
    private String description; // Detailed description of the complaint
    private String status; // Status of the complaint (e.g., New, In Progress, Resolved)
    private String remarks; // Additional remarks
    private String priority; // Priority level (e.g., High, Medium, Low)
    private String location; // Location or area related to the complaint

    @ServerTimestamp
    private Date date; // Timestamp of complaint registration

    // No-argument constructor required for Firestore deserialization
    public Complaint() {
        // Empty constructor needed for Firestore
    }

    // Constructor for new complaints (used when registering a new complaint)
    public Complaint(String department, String type, String contactPerson, String phone, String email,
                     String description, String priority, String location) {
        this.department = department;
        this.type = type;
        this.contactPerson = contactPerson;
        this.phone = phone;
        this.email = email;
        this.description = description;
        this.status = "New"; // Default status for new complaints
        this.remarks = ""; // Default empty remarks
        this.priority = priority;
        this.location = location;
    }

    // Getters and setters for all fields
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
