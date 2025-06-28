package com.group7.pawdictedadmin.models;

import java.util.Date;

public class Product {
    private String id;
    private String name;
    private String description;
    private double price;
    private String imageUrl;
    private String category;
    private int stock;
    private boolean isActive;
    private Date createdAt;
    private Date updatedAt;
    private String sku;
    private double weight;
    private String brand;

    public Product() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.isActive = true;
    }

    public Product(String name, String description, double price, String imageUrl, String category, int stock) {
        this();
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.category = category;
        this.stock = stock;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
}
