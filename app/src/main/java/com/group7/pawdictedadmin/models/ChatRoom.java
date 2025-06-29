//package com.group7.pawdictedadmin.models;
//
//import com.google.firebase.firestore.QueryDocumentSnapshot;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//public class ChatRoom {
//    private String chat_id;
//    private String customer_id;
//    private String customer_name;
//    private List<Map<String, Object>> customerSent;
//    private List<Map<String, Object>> pawdictedSent;
//
//    public ChatRoom() {}
//
//    public static ChatRoom fromFirestore(QueryDocumentSnapshot document) {
//        ChatRoom chatRoom = new ChatRoom();
//        chatRoom.chat_id = document.getId();
//        chatRoom.customer_id = document.getString("customer_id");
//
//        chatRoom.customer_name = document.getString("customer_name");
//
//        if (chatRoom.customer_name == null || chatRoom.customer_name.isEmpty()) {
//            if (chatRoom.customer_id != null && chatRoom.customer_id.startsWith("guest_")) {
//                chatRoom.customer_name = "Khách vãng lai";
//            } else {
//                chatRoom.customer_name = "Khách hàng " + (chatRoom.customer_id != null ?
//                        chatRoom.customer_id.substring(0, Math.min(6, chatRoom.customer_id.length())) : "");
//            }
//        }
//
//        chatRoom.customerSent = (List<Map<String, Object>>) document.get("customerSent");
//        chatRoom.pawdictedSent = (List<Map<String, Object>>) document.get("pawdictedSent");
//
//        if (chatRoom.customerSent == null) chatRoom.customerSent = new ArrayList<>();
//        if (chatRoom.pawdictedSent == null) chatRoom.pawdictedSent = new ArrayList<>();
//
//        return chatRoom;
//    }
//
//    public String getLastMessage() {
//        long lastCustomerTime = getLastMessageTime(customerSent);
//        long lastPawdictedTime = getLastMessageTime(pawdictedSent);
//
//        if (lastCustomerTime > lastPawdictedTime && !customerSent.isEmpty()) {
//            return "Khách: " + customerSent.get(customerSent.size() - 1).get("content");
//        } else if (!pawdictedSent.isEmpty()) {
//            return "Admin: " + pawdictedSent.get(pawdictedSent.size() - 1).get("content");
//        }
//        return "Chưa có tin nhắn";
//    }
//
//    public long getLastMessageTime() {
//        long lastCustomerTime = getLastMessageTime(customerSent);
//        long lastPawdictedTime = getLastMessageTime(pawdictedSent);
//        return Math.max(lastCustomerTime, lastPawdictedTime);
//    }
//
//    private long getLastMessageTime(List<Map<String, Object>> messages) {
//        if (messages == null || messages.isEmpty()) return 0;
//        Object time = messages.get(messages.size() - 1).get("time");
//        return time instanceof Long ? (Long) time : 0;
//    }
//
//    public int getUnreadCount() {
//        return customerSent != null ? customerSent.size() : 0;
//    }
//
//    // Getters and Setters
//    public String getChat_id() { return chat_id; }
//    public void setChat_id(String chat_id) { this.chat_id = chat_id; }
//    public String getCustomer_id() { return customer_id; }
//    public void setCustomer_id(String customer_id) { this.customer_id = customer_id; }
//    public String getCustomer_name() { return customer_name; }
//    public void setCustomer_name(String customer_name) { this.customer_name = customer_name; }
//    public List<Map<String, Object>> getCustomerSent() { return customerSent; }
//    public void setCustomerSent(List<Map<String, Object>> customerSent) { this.customerSent = customerSent; }
//    public List<Map<String, Object>> getPawdictedSent() { return pawdictedSent; }
//    public void setPawdictedSent(List<Map<String, Object>> pawdictedSent) { this.pawdictedSent = pawdictedSent; }
//}



package com.group7.pawdictedadmin.models;

import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatRoom {
    private String chat_id;
    private String customer_id;
    private String customer_name;
    private String customer_avatar; // Thêm field để lưu avatar
    private List<Map<String, Object>> customerSent;
    private List<Map<String, Object>> pawdictedSent;

    public ChatRoom() {}

    public static ChatRoom fromFirestore(QueryDocumentSnapshot document) {
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.chat_id = document.getId();
        chatRoom.customer_id = document.getString("customer_id");
        chatRoom.customer_name = document.getString("customer_name");
        chatRoom.customer_avatar = document.getString("customer_avatar"); // Lấy avatar từ Firestore

        if (chatRoom.customer_name == null || chatRoom.customer_name.isEmpty()) {
            if (chatRoom.customer_id != null && chatRoom.customer_id.startsWith("guest_")) {
                chatRoom.customer_name = "Khách vãng lai";
            } else {
                chatRoom.customer_name = "Khách hàng " + (chatRoom.customer_id != null ?
                        chatRoom.customer_id.substring(0, Math.min(6, chatRoom.customer_id.length())) : "");
            }
        }

        chatRoom.customerSent = (List<Map<String, Object>>) document.get("customerSent");
        chatRoom.pawdictedSent = (List<Map<String, Object>>) document.get("pawdictedSent");

        if (chatRoom.customerSent == null) chatRoom.customerSent = new ArrayList<>();
        if (chatRoom.pawdictedSent == null) chatRoom.pawdictedSent = new ArrayList<>();

        return chatRoom;
    }

    public String getLastMessage() {
        long lastCustomerTime = getLastMessageTime(customerSent);
        long lastPawdictedTime = getLastMessageTime(pawdictedSent);

        if (lastCustomerTime > lastPawdictedTime && !customerSent.isEmpty()) {
            return "Khách: " + customerSent.get(customerSent.size() - 1).get("content");
        } else if (!pawdictedSent.isEmpty()) {
            return "Admin: " + pawdictedSent.get(pawdictedSent.size() - 1).get("content");
        }
        return "Chưa có tin nhắn";
    }

    public long getLastMessageTime() {
        long lastCustomerTime = getLastMessageTime(customerSent);
        long lastPawdictedTime = getLastMessageTime(pawdictedSent);
        return Math.max(lastCustomerTime, lastPawdictedTime);
    }

    private long getLastMessageTime(List<Map<String, Object>> messages) {
        if (messages == null || messages.isEmpty()) return 0;
        Object time = messages.get(messages.size() - 1).get("time");
        return time instanceof Long ? (Long) time : 0;
    }

    public int getUnreadCount() {
        return customerSent != null ? customerSent.size() : 0;
    }

    // Getters and Setters
    public String getChat_id() { return chat_id; }
    public void setChat_id(String chat_id) { this.chat_id = chat_id; }

    public String getCustomer_id() { return customer_id; }
    public void setCustomer_id(String customer_id) { this.customer_id = customer_id; }

    public String getCustomer_name() { return customer_name; }
    public void setCustomer_name(String customer_name) { this.customer_name = customer_name; }

    // Getter và Setter cho customer_avatar
    public String getCustomer_avatar() { return customer_avatar; }
    public void setCustomer_avatar(String customer_avatar) { this.customer_avatar = customer_avatar; }

    public List<Map<String, Object>> getCustomerSent() { return customerSent; }
    public void setCustomerSent(List<Map<String, Object>> customerSent) { this.customerSent = customerSent; }

    public List<Map<String, Object>> getPawdictedSent() { return pawdictedSent; }
    public void setPawdictedSent(List<Map<String, Object>> pawdictedSent) { this.pawdictedSent = pawdictedSent; }
}