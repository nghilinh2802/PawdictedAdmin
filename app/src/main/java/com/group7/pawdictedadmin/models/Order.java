//package com.group7.pawdictedadmin.models;
//
//import com.google.firebase.firestore.PropertyName;
//
//import java.util.Date;
//import java.util.List;
//
//public class Order {
//    private String id;
//    @PropertyName("customer_id")
//    private String customerId;
//    private Customer customer; // Nested object for customer details
//    private List<OrderItem> items; // List of order items
//    @PropertyName("order_value")
//    private double totalAmount;
//    @PropertyName("order_status")
//    private String status;
//    @PropertyName("order_time")
//    private Date orderDate;
//
//    // Nested class for OrderItem
//    public static class OrderItem {
//        @PropertyName("product_id")
//        private String productId;
//        private int quantity;
//        @PropertyName("total_cost_of_goods")
//        private double totalCost;
//
//        public OrderItem() {}
//
//        public String getProductId() { return productId; }
//        public void setProductId(String productId) { this.productId = productId; }
//        public int getQuantity() { return quantity; }
//        public void setQuantity(int quantity) { this.quantity = quantity; }
//        public double getTotalCost() { return totalCost; }
//        public void setTotalCost(double totalCost) { this.totalCost = totalCost; }
//    }
//
//    // Nested class for Customer
//    public static class Customer {
//        @PropertyName("customer_name")
//        private String name;
//        @PropertyName("phone_number")
//        private String phone;
//        private String address;
//        private String avatar_img;
//
//        public Customer(String name, String phone, String address, String avatar_img) {
//            this.name = name;
//            this.phone = phone;
//            this.address = address;
//            this.avatar_img = avatar_img;
//        }
//
//        public String getAvatar_img() {
//            return avatar_img;
//        }
//
//        public void setAvatar_img(String avatar_img) {
//            this.avatar_img = avatar_img;
//        }
//
//        public Customer() {}
//
//        public String getName() { return name; }
//        public void setName(String name) { this.name = name; }
//        public String getPhone() { return phone; }
//        public void setPhone(String phone) { this.phone = phone; }
//        public String getAddress() { return address; }
//        public void setAddress(String address) { this.address = address; }
//    }
//
//    public Order() {
//        this.orderDate = new Date();
//        this.status = "Pending";
//    }
//
//    // Getters and Setters
//    public String getId() { return id; }
//    public void setId(String id) { this.id = id; }
//
//    public String getCustomerId() { return customerId; }
//    public void setCustomerId(String customerId) { this.customerId = customerId; }
//
//    public Customer getCustomer() { return customer; }
//    public void setCustomer(Customer customer) { this.customer = customer; }
//
//    public List<OrderItem> getItems() { return items; }
//    public void setItems(List<OrderItem> items) { this.items = items; }
//
//    public double getTotalAmount() { return totalAmount; }
//    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
//
//    public String getStatus() { return status; }
//    public void setStatus(String status) { this.status = status; }
//
//    public Date getOrderDate() { return orderDate; }
//    public void setOrderDate(Date orderDate) { this.orderDate = orderDate; }
//
//    @Override
//    public boolean equals(Object obj) {
//        if (this == obj) return true;
//        if (obj == null || getClass() != obj.getClass()) return false;
//        Order order = (Order) obj;
//        return id != null ? id.equals(order.id) : order.id == null;
//    }
//
//    @Override
//    public int hashCode() {
//        return id != null ? id.hashCode() : 0;
//    }
//}


package com.group7.pawdictedadmin.models;

import com.google.firebase.firestore.PropertyName;

import java.util.Date;
import java.util.List;

public class Order {
    private String id;
    @PropertyName("customer_id")
    private String customerId;
    private Customer customer;
    private List<OrderItem> items;
    @PropertyName("order_value")
    private double totalAmount;
    @PropertyName("order_status")
    private String status;
    @PropertyName("order_time")
    private Date orderDate;
    @PropertyName("order_item_id")
    private String orderItemId;

    // Additional fields
    private String paymentMethod;
    private double shippingFee;

    // Nested class for Customer
    public static class Customer {
        @PropertyName("customer_name")
        private String name;
        @PropertyName("phone_number")
        private String phone;
        private String address;
        @PropertyName("avatar_img")
        private String avatarImg;

        public Customer() {}

        public Customer(String name, String phone, String address, String avatarImg) {
            this.name = name;
            this.phone = phone;
            this.address = address;
            this.avatarImg = avatarImg;
        }

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }

        public String getAvatarImg() { return avatarImg; }
        public void setAvatarImg(String avatarImg) { this.avatarImg = avatarImg; }
    }

    public Order() {
        this.orderDate = new Date();
        this.status = "Pending";
        this.paymentMethod = "Cash on Delivery";
        this.shippingFee = 30000; // Default shipping fee
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getOrderDate() { return orderDate; }
    public void setOrderDate(Date orderDate) { this.orderDate = orderDate; }

    public String getOrderItemId() { return orderItemId; }
    public void setOrderItemId(String orderItemId) { this.orderItemId = orderItemId; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public double getShippingFee() { return shippingFee; }
    public void setShippingFee(double shippingFee) { this.shippingFee = shippingFee; }

    // Helper methods
    public double getFinalPrice() {
        return totalAmount + shippingFee;
    }

    public boolean isPending() {
        return "Pending Payment".equals(status);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Order order = (Order) obj;
        return id != null ? id.equals(order.id) : order.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Order{" +
                "id='" + id + '\'' +
                ", customerId='" + customerId + '\'' +
                ", totalAmount=" + totalAmount +
                ", status='" + status + '\'' +
                ", orderDate=" + orderDate +
                '}';
    }
}