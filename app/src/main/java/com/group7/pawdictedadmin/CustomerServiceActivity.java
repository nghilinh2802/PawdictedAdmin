package com.group7.pawdictedadmin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.ListenerRegistration;
import com.group7.pawdictedadmin.adapters.ChatRoomAdapter;
import com.group7.pawdictedadmin.models.ChatRoom;
import com.group7.pawdictedadmin.services.AdminChatService;

import java.util.ArrayList;
import java.util.List;

public class CustomerServiceActivity extends AppCompatActivity implements ChatRoomAdapter.OnChatRoomClickListener {
    private RecyclerView recyclerChatRooms;
    private ChatRoomAdapter chatRoomAdapter;
    private AdminChatService adminChatService;
    private List<ChatRoom> chatRoomList;
    private ListenerRegistration chatRoomsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_service);

        initViews();
        setupRecyclerView();
        setupChatService();
        loadChatRooms();
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
                    chatRoomAdapter.updateChatRooms(chatRooms);
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

    @Override
    public void onChatRoomClick(ChatRoom chatRoom) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("chat_id", chatRoom.getChat_id());
        intent.putExtra("customer_id", chatRoom.getCustomer_id());
        intent.putExtra("customer_name", chatRoom.getCustomer_name());
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
