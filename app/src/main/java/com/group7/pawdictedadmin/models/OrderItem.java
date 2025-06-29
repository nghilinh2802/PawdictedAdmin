//package com.group7.pawdictedadmin.models;
//
//public class OrderItem {
//    private String productId;
//    private String productName;
//    private String productImage;
//    private String productSku;
//    private int quantity;
//    private double unitPrice;
//    private double totalPrice;
//    private String notes;
//
//    public OrderItem() {}
//
//    public OrderItem(String productId, String productName, int quantity, double unitPrice) {
//        this.productId = productId;
//        this.productName = productName;
//        this.quantity = quantity;
//        this.unitPrice = unitPrice;
//        this.totalPrice = quantity * unitPrice;
//    }
//
//    // Getters and Setters
//    public String getProductId() { return productId; }
//    public void setProductId(String productId) { this.productId = productId; }
//
//    public String getProductName() { return productName; }
//    public void setProductName(String productName) { this.productName = productName; }
//
//    public String getProductImage() { return productImage; }
//    public void setProductImage(String productImage) { this.productImage = productImage; }
//
//    public String getProductSku() { return productSku; }
//    public void setProductSku(String productSku) { this.productSku = productSku; }
//
//    public int getQuantity() { return quantity; }
//    public void setQuantity(int quantity) {
//        this.quantity = quantity;
//        this.totalPrice = quantity * unitPrice;
//    }
//
//    public double getUnitPrice() { return unitPrice; }
//    public void setUnitPrice(double unitPrice) {
//        this.unitPrice = unitPrice;
//        this.totalPrice = quantity * unitPrice;
//    }
//
//    public double getTotalPrice() { return totalPrice; }
//    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
//
//    public String getNotes() { return notes; }
//    public void setNotes(String notes) { this.notes = notes; }
//}


package com.group7.pawdictedadmin.models;

import com.google.firebase.firestore.PropertyName;

public class OrderItem {
    @PropertyName("product_id")
    private String productId;
    private int quantity;
    @PropertyName("total_cost_of_goods")
    private double totalCost;

    // Additional fields for UI display
    private String productName;
    private String variant;
    private String productImage;
    private double unitPrice;

    public OrderItem() {}

    public OrderItem(String productId, int quantity, double totalCost) {
        this.productId = productId;
        this.quantity = quantity;
        this.totalCost = totalCost;
    }

    // Firestore fields
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }

    // UI display fields
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getVariant() { return variant; }
    public void setVariant(String variant) { this.variant = variant; }

    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    @Override
    public String toString() {
        return "OrderItem{" +
                "productId='" + productId + '\'' +
                ", quantity=" + quantity +
                ", totalCost=" + totalCost +
                ", productName='" + productName + '\'' +
                '}';
    }
}