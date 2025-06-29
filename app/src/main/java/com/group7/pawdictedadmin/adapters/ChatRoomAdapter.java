package com.group7.pawdictedadmin.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.group7.pawdictedadmin.R;
import com.group7.pawdictedadmin.models.ChatRoom;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.ChatRoomViewHolder> {
    private List<ChatRoom> chatRooms;
    private OnChatRoomClickListener listener;
    private SimpleDateFormat timeFormat;

    public interface OnChatRoomClickListener {
        void onChatRoomClick(ChatRoom chatRoom);
    }

    public ChatRoomAdapter(List<ChatRoom> chatRooms, OnChatRoomClickListener listener) {
        this.chatRooms = chatRooms;
        this.listener = listener;
        this.timeFormat = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public ChatRoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_room, parent, false);
        return new ChatRoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatRoomViewHolder holder, int position) {
        ChatRoom chatRoom = chatRooms.get(position);
        holder.bind(chatRoom);
    }

    @Override
    public int getItemCount() {
        return chatRooms.size();
    }

    public void updateChatRooms(List<ChatRoom> newChatRooms) {
        this.chatRooms = newChatRooms;
        notifyDataSetChanged();
    }

    class ChatRoomViewHolder extends RecyclerView.ViewHolder {
        TextView txtCustomerName, txtLastMessage, txtTime, txtUnreadCount;
        View unreadIndicator;

        ChatRoomViewHolder(View itemView) {
            super(itemView);
            txtCustomerName = itemView.findViewById(R.id.txt_customer_name);
            txtLastMessage = itemView.findViewById(R.id.txt_last_message);
            txtTime = itemView.findViewById(R.id.txt_time);
            txtUnreadCount = itemView.findViewById(R.id.txt_unread_count);
            unreadIndicator = itemView.findViewById(R.id.unread_indicator);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onChatRoomClick(chatRooms.get(getAdapterPosition()));
                }
            });
        }

        void bind(ChatRoom chatRoom) {
            txtCustomerName.setText(chatRoom.getCustomer_name());
            txtLastMessage.setText(chatRoom.getLastMessage());
            txtTime.setText(timeFormat.format(new Date(chatRoom.getLastMessageTime())));

            int unreadCount = chatRoom.getUnreadCount();
            if (unreadCount > 0) {
                txtUnreadCount.setVisibility(View.VISIBLE);
                txtUnreadCount.setText(String.valueOf(unreadCount));
                unreadIndicator.setVisibility(View.VISIBLE);
                itemView.setBackgroundResource(R.drawable.chat_room_unread_background);
            } else {
                txtUnreadCount.setVisibility(View.GONE);
                unreadIndicator.setVisibility(View.GONE);
                itemView.setBackgroundResource(R.drawable.chat_room_background);
            }
        }
    }
}
