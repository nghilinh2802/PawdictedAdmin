//package com.group7.pawdictedadmin;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.util.Log;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.google.firebase.firestore.ListenerRegistration;
//import com.group7.pawdictedadmin.adapters.ChatRoomAdapter;
//import com.group7.pawdictedadmin.models.ChatRoom;
//import com.group7.pawdictedadmin.services.AdminChatService;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class CustomerServiceActivity extends AppCompatActivity implements ChatRoomAdapter.OnChatRoomClickListener {
//    private RecyclerView recyclerChatRooms;
//    private ChatRoomAdapter chatRoomAdapter;
//    private AdminChatService adminChatService;
//    private List<ChatRoom> chatRoomList;
//    private ListenerRegistration chatRoomsListener;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_customer_service);
//
//        initViews();
//        setupRecyclerView();
//        setupChatService();
//        loadChatRooms();
//    }
//
//    private void initViews() {
//        recyclerChatRooms = findViewById(R.id.recycler_chat_rooms);
//    }
//
//    private void setupRecyclerView() {
//        chatRoomList = new ArrayList<>();
//        chatRoomAdapter = new ChatRoomAdapter(chatRoomList, this);
//        recyclerChatRooms.setAdapter(chatRoomAdapter);
//        recyclerChatRooms.setLayoutManager(new LinearLayoutManager(this));
//    }
//
//    private void setupChatService() {
//        adminChatService = new AdminChatService();
//    }
//
//    private void loadChatRooms() {
//        chatRoomsListener = adminChatService.getAllChatRooms(new AdminChatService.OnChatRoomsLoadedListener() {
//            @Override
//            public void onSuccess(List<ChatRoom> chatRooms) {
//                runOnUiThread(() -> {
//                    chatRoomAdapter.updateChatRooms(chatRooms);
//                });
//            }
//
//            @Override
//            public void onFailure(String error) {
//                runOnUiThread(() -> {
//                    Toast.makeText(CustomerServiceActivity.this, "Lỗi load chat rooms: " + error, Toast.LENGTH_SHORT).show();
//                });
//            }
//        });
//    }
//
//    @Override
//    public void onChatRoomClick(ChatRoom chatRoom) {
//        Intent intent = new Intent(this, ChatActivity.class);
//        intent.putExtra("chat_id", chatRoom.getChat_id());
//        intent.putExtra("customer_id", chatRoom.getCustomer_id());
//        intent.putExtra("customer_name", chatRoom.getCustomer_name());
//        startActivity(intent);
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (chatRoomsListener != null) {
//            chatRoomsListener.remove();
//        }
//    }
//}



package com.group7.pawdictedadmin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.group7.pawdictedadmin.adapters.ChatRoomAdapter;
import com.group7.pawdictedadmin.models.ChatRoom;
import com.group7.pawdictedadmin.services.AdminChatService;

import java.util.ArrayList;
import java.util.List;

public class CustomerServiceActivity extends AppCompatActivity implements ChatRoomAdapter.OnChatRoomClickListener {
    private static final String TAG = "CustomerService";
    private RecyclerView recyclerChatRooms;
    private ChatRoomAdapter chatRoomAdapter;
    private AdminChatService adminChatService;
    private List<ChatRoom> chatRoomList;
    private ListenerRegistration chatRoomsListener;
    private FirebaseFirestore db;
    private ImageView btnBack;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_service);

        db = FirebaseFirestore.getInstance();
        initViews();
        setupRecyclerView();
        setupChatService();
        loadChatRooms();

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void initViews() {
        recyclerChatRooms = findViewById(R.id.recycler_chat_rooms);
    }

    private void setupRecyclerView() {
        chatRoomList = new ArrayList<>();
        chatRoomAdapter = new ChatRoomAdapter(chatRoomList, this);
        recyclerChatRooms.setAdapter(chatRoomAdapter);
        recyclerChatRooms.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupChatService() {
        adminChatService = new AdminChatService();
    }

    private void loadChatRooms() {
        chatRoomsListener = adminChatService.getAllChatRooms(new AdminChatService.OnChatRoomsLoadedListener() {
            @Override
            public void onSuccess(List<ChatRoom> chatRooms) {
                runOnUiThread(() -> {
                    // Fetch avatar cho từng chat room
                    fetchAvatarsForChatRooms(chatRooms);
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(CustomerServiceActivity.this, "Lỗi load chat rooms: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void fetchAvatarsForChatRooms(List<ChatRoom> chatRooms) {
        int[] processedCount = {0}; // Sử dụng array để có thể modify trong lambda

        for (ChatRoom chatRoom : chatRooms) {
            String customerId = chatRoom.getCustomer_id();

            if (customerId != null && !customerId.startsWith("guest_")) {
                // Fetch avatar từ collection customers
                db.collection("customers").document(customerId)
                        .get()
                        .addOnCompleteListener(task -> {
                            processedCount[0]++;

                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    String avatarUrl = document.getString("avatar_img");
                                    chatRoom.setCustomer_avatar(avatarUrl);

                                    // Cập nhật tên customer nếu chưa có
                                    if (chatRoom.getCustomer_name() == null ||
                                            chatRoom.getCustomer_name().startsWith("Khách hàng")) {
                                        String customerName = document.getString("customer_name");
                                        if (customerName != null && !customerName.isEmpty()) {
                                            chatRoom.setCustomer_name(customerName);
                                        }
                                    }
                                }
                            } else {
                                Log.e(TAG, "Error fetching customer data for " + customerId, task.getException());
                            }

                            // Cập nhật adapter khi đã xử lý xong tất cả
                            if (processedCount[0] == chatRooms.size()) {
                                chatRoomAdapter.updateChatRooms(chatRooms);
                            }
                        });
            } else {
                // Đối với guest user, không có avatar
                processedCount[0]++;
                if (processedCount[0] == chatRooms.size()) {
                    chatRoomAdapter.updateChatRooms(chatRooms);
                }
            }
        }

        // Trường hợp không có chat room nào cần fetch avatar
        if (chatRooms.isEmpty()) {
            chatRoomAdapter.updateChatRooms(chatRooms);
        }
    }

    @Override
    public void onChatRoomClick(ChatRoom chatRoom) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("chat_id", chatRoom.getChat_id());
        intent.putExtra("customer_id", chatRoom.getCustomer_id());
        intent.putExtra("customer_name", chatRoom.getCustomer_name());
        intent.putExtra("customer_avatar", chatRoom.getCustomer_avatar()); // Truyền avatar sang ChatActivity
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatRoomsListener != null) {
            chatRoomsListener.remove();
        }
    }
}
