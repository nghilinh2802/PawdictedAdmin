//package com.group7.pawdictedadmin.models;
//
//import java.util.Date;
//
//public class Customer {
//    private String id;
//    private String name;
//    private String email;
//    private String phone;
//    private String address;
//    private Date registrationDate;
//    private Date lastOrderDate;
//    private int totalOrders;
//    private double totalSpent;
//    private String status; // active, inactive, blocked
//    private String notes;
//
//    public Customer() {
//        this.registrationDate = new Date();
//        this.status = "active";
//        this.totalOrders = 0;
//        this.totalSpent = 0.0;
//    }
//
//    // Getters and Setters
//    public String getId() { return id; }
//    public void setId(String id) { this.id = id; }
//
//    public String getName() { return name; }
//    public void setName(String name) { this.name = name; }
//
//    public String getEmail() { return email; }
//    public void setEmail(String email) { this.email = email; }
//
//    public String getPhone() { return phone; }
//    public void setPhone(String phone) { this.phone = phone; }
//
//    public String getAddress() { return address; }
//    public void setAddress(String address) { this.address = address; }
//
//    public Date getRegistrationDate() { return registrationDate; }
//    public void setRegistrationDate(Date registrationDate) { this.registrationDate = registrationDate; }
//
//    public Date getLastOrderDate() { return lastOrderDate; }
//    public void setLastOrderDate(Date lastOrderDate) { this.lastOrderDate = lastOrderDate; }
//
//    public int getTotalOrders() { return totalOrders; }
//    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }
//
//    public double getTotalSpent() { return totalSpent; }
//    public void setTotalSpent(double totalSpent) { this.totalSpent = totalSpent; }
//
//    public String getStatus() { return status; }
//    public void setStatus(String status) { this.status = status; }
//
//    public String getNotes() { return notes; }
//    public void setNotes(String notes) { this.notes = notes; }
//}
//

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
    private String status;
    private String notes;
    private String customer_username;
    private String gender;
    private Date dob;
    private String avatar_img;
    private String role;

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

    public String getCustomer_username() { return customer_username; }
    public void setCustomer_username(String customer_username) { this.customer_username = customer_username; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Date getDob() { return dob; }
    public void setDob(Date dob) { this.dob = dob; }

    public String getAvatar_img() { return avatar_img; }
    public void setAvatar_img(String avatar_img) { this.avatar_img = avatar_img; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    @Override
    public String toString() {
        return "Customer{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}