package com.group7.pawdictedadmin.models;

import java.util.Date;

public class Customer {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private Date registrationDate;
    private Date lastOrderDate;
    private int totalOrders;
    private double totalSpent;
    private String status; // active, inactive, blocked
    private String notes;

    public Customer() {
        this.registrationDate = new Date();
        this.status = "active";
        this.totalOrders = 0;
        this.totalSpent = 0.0;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Date getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(Date registrationDate) { this.registrationDate = registrationDate; }

    public Date getLastOrderDate() { return lastOrderDate; }
    public void setLastOrderDate(Date lastOrderDate) { this.lastOrderDate = lastOrderDate; }

    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }

    public double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(double totalSpent) { this.totalSpent = totalSpent; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

