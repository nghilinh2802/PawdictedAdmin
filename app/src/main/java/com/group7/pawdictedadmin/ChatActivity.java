package com.group7.pawdictedadmin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.ListenerRegistration;
import com.group7.pawdictedadmin.adapters.AdminChatAdapter;
import com.group7.pawdictedadmin.models.MessageItem;
import com.group7.pawdictedadmin.services.AdminChatService;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {
    private EditText edtMessage;
    private MaterialButton btnSend;
    private RecyclerView recyclerChat;
    private ImageView imgBack;
    private TextView txtCustomerName;
    private AdminChatAdapter chatAdapter;
    private AdminChatService adminChatService;
    private List<MessageItem> messageList;
    private ListenerRegistration messageListener;

    private String chatId;
    private String customerId;
    private String customerName;

    private boolean isSending = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        getIntentData();
        initViews();
        setupChatService();
        setupRecyclerView();
        setupSendButton();
        setupBackButton();
        loadMessages();
        updateUI();
    }

    private void getIntentData() {
        Intent intent = getIntent();
        chatId = intent.getStringExtra("chat_id");
        customerId = intent.getStringExtra("customer_id");
        customerName = intent.getStringExtra("customer_name");
        

        Log.d("ChatActivity", "Chat ID: " + chatId);
        Log.d("ChatActivity", "Customer ID: " + customerId);
        Log.d("ChatActivity", "Customer Name: " + customerName);
    }

    private void initViews() {
        edtMessage = findViewById(R.id.edt_message);
        btnSend = findViewById(R.id.btnSend);
        recyclerChat = findViewById(R.id.recycler_chat);
        imgBack = findViewById(R.id.imgBack);
        txtCustomerName = findViewById(R.id.txt_customer_name);
    }

    private void setupChatService() {
        adminChatService = new AdminChatService();
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        chatAdapter = new AdminChatAdapter(messageList);
        recyclerChat.setAdapter(chatAdapter);
        recyclerChat.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupSendButton() {
        btnSend.setOnClickListener(v -> sendMessage());

        edtMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                            event.getAction() == KeyEvent.ACTION_DOWN)) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void setupBackButton() {
        if (imgBack != null) {
            imgBack.setOnClickListener(v -> finish());
        }
    }

    private void sendMessage() {
        if (isSending) return;

        String messageContent = edtMessage.getText().toString().trim();

        if (messageContent.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tin nhắn", Toast.LENGTH_SHORT).show();
            return;
        }

        isSending = true;
        btnSend.setEnabled(false);
        btnSend.setText("...");

        adminChatService.sendAdminMessage(chatId, messageContent, new AdminChatService.OnMessageSentListener() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    edtMessage.setText("");
                    isSending = false;
                    btnSend.setEnabled(true);
                    btnSend.setText("📤");
                    Toast.makeText(ChatActivity.this, "Tin nhắn đã được gửi", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    isSending = false;
                    btnSend.setEnabled(true);
                    btnSend.setText("📤");
                    Toast.makeText(ChatActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void loadMessages() {
        if (chatId == null) return;

        messageListener = adminChatService.loadChatMessages(chatId, new AdminChatService.OnMessagesLoadedListener() {
            @Override
            public void onSuccess(List<MessageItem> messages) {
                runOnUiThread(() -> {
                    chatAdapter.updateMessages(messages);
                    if (messages.size() > 0) {
                        recyclerChat.scrollToPosition(messages.size() - 1);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(ChatActivity.this, "Lỗi load tin nhắn: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateUI() {
        if (txtCustomerName != null && customerName != null) {
            txtCustomerName.setText("Chat với " + customerName);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Chat - " + (customerName != null ? customerName : "Khách hàng"));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) {
            messageListener.remove();
        }
    }
}
