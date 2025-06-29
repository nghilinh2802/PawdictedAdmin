package com.group7.pawdictedadmin.services;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.group7.pawdictedadmin.models.ChatRoom;
import com.group7.pawdictedadmin.models.MessageItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminChatService {
    private FirebaseFirestore db;

    public AdminChatService() {
        this.db = FirebaseFirestore.getInstance();
    }

    public ListenerRegistration getAllChatRooms(OnChatRoomsLoadedListener listener) {
        return db.collection("chats")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        if (listener != null) {
                            listener.onFailure("Error loading chat rooms: " + e.getMessage());
                        }
                        return;
                    }

                    List<ChatRoom> chatRooms = new ArrayList<>();
                    if (queryDocumentSnapshots != null) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            try {
                                ChatRoom chatRoom = ChatRoom.fromFirestore(document);
                                chatRooms.add(chatRoom);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }

                    fetchCustomerNamesForChatRooms(chatRooms, listener);
                });
    }

    private void fetchCustomerNamesForChatRooms(List<ChatRoom> chatRooms, OnChatRoomsLoadedListener listener) {
        List<ChatRoom> roomsNeedingNames = new ArrayList<>();

        for (ChatRoom room : chatRooms) {
            if (room.getCustomer_name() == null ||
                    room.getCustomer_name().startsWith("Khách hàng") ||
                    room.getCustomer_name().equals("Khách vãng lai")) {
                roomsNeedingNames.add(room);
            }
        }

        if (roomsNeedingNames.isEmpty()) {
            chatRooms.sort((a, b) -> Long.compare(b.getLastMessageTime(), a.getLastMessageTime()));
            if (listener != null) {
                listener.onSuccess(chatRooms);
            }
            return;
        }

        int[] completedCount = {0};
        int totalCount = roomsNeedingNames.size();

        for (ChatRoom room : roomsNeedingNames) {
            if (room.getCustomer_id().startsWith("guest_")) {
                room.setCustomer_name("Khách vãng lai");
                completedCount[0]++;

                if (completedCount[0] == totalCount) {
                    chatRooms.sort((a, b) -> Long.compare(b.getLastMessageTime(), a.getLastMessageTime()));
                    if (listener != null) {
                        listener.onSuccess(chatRooms);
                    }
                }
            } else {
                db.collection("customers")
                        .whereEqualTo("customer_id", room.getCustomer_id())
                        .limit(1)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (!querySnapshot.isEmpty()) {
                                QueryDocumentSnapshot customerDoc = (QueryDocumentSnapshot) querySnapshot.getDocuments().get(0);
                                String customerName = customerDoc.getString("customer_name");
                                if (customerName != null && !customerName.isEmpty()) {
                                    room.setCustomer_name(customerName);
                                }
                            }

                            completedCount[0]++;
                            if (completedCount[0] == totalCount) {
                                chatRooms.sort((a, b) -> Long.compare(b.getLastMessageTime(), a.getLastMessageTime()));
                                if (listener != null) {
                                    listener.onSuccess(chatRooms);
                                }
                            }
                        })
                        .addOnFailureListener(error -> {
                            completedCount[0]++;
                            if (completedCount[0] == totalCount) {
                                chatRooms.sort((a, b) -> Long.compare(b.getLastMessageTime(), a.getLastMessageTime()));
                                if (listener != null) {
                                    listener.onSuccess(chatRooms);
                                }
                            }
                        });
            }
        }
    }

    public void sendAdminMessage(String chatId, String content, OnMessageSentListener listener) {
        Map<String, Object> message = new HashMap<>();
        message.put("content", content);
        message.put("time", System.currentTimeMillis());

        DocumentReference chatRef = db.collection("chats").document(chatId);

        chatRef.update("pawdictedSent", FieldValue.arrayUnion(message))
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) {
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onFailure(e.getMessage());
                    }
                });
    }

    public ListenerRegistration loadChatMessages(String chatId, OnMessagesLoadedListener listener) {
        DocumentReference chatRef = db.collection("chats").document(chatId);

        return chatRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                if (listener != null) {
                    listener.onFailure("Error loading messages: " + e.getMessage());
                }
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                List<MessageItem> allMessages = new ArrayList<>();

                List<HashMap<String, Object>> customerMessages =
                        (List<HashMap<String, Object>>) documentSnapshot.get("customerSent");
                if (customerMessages != null) {
                    for (HashMap<String, Object> msg : customerMessages) {
                        String content = (String) msg.get("content");
                        Long time = (Long) msg.get("time");
                        if (content != null && time != null) {
                            allMessages.add(new MessageItem(content, time, "customer"));
                        }
                    }
                }

                List<HashMap<String, Object>> adminMessages =
                        (List<HashMap<String, Object>>) documentSnapshot.get("pawdictedSent");
                if (adminMessages != null) {
                    for (HashMap<String, Object> msg : adminMessages) {
                        String content = (String) msg.get("content");
                        Long time = (Long) msg.get("time");
                        if (content != null && time != null) {
                            allMessages.add(new MessageItem(content, time, "admin"));
                        }
                    }
                }

                Collections.sort(allMessages, (m1, m2) -> Long.compare(m1.getTime(), m2.getTime()));

                if (listener != null) {
                    listener.onSuccess(allMessages);
                }
            } else {
                if (listener != null) {
                    listener.onSuccess(new ArrayList<>());
                }
            }
        });
    }

    public interface OnChatRoomsLoadedListener {
        void onSuccess(List<ChatRoom> chatRooms);
        void onFailure(String error);
    }

    public interface OnMessageSentListener {
        void onSuccess();
        void onFailure(String error);
    }

    public interface OnMessagesLoadedListener {
        void onSuccess(List<MessageItem> messages);
        void onFailure(String error);
    }
}
