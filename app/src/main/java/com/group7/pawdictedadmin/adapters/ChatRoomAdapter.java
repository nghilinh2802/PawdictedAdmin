//package com.group7.pawdictedadmin.adapters;
//
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.group7.pawdictedadmin.R;
//import com.group7.pawdictedadmin.models.ChatRoom;
//
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.List;
//import java.util.Locale;
//
//public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.ChatRoomViewHolder> {
//    private List<ChatRoom> chatRooms;
//    private OnChatRoomClickListener listener;
//    private SimpleDateFormat timeFormat;
//
//    public interface OnChatRoomClickListener {
//        void onChatRoomClick(ChatRoom chatRoom);
//    }
//
//    public ChatRoomAdapter(List<ChatRoom> chatRooms, OnChatRoomClickListener listener) {
//        this.chatRooms = chatRooms;
//        this.listener = listener;
//        this.timeFormat = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
//    }
//
//    @NonNull
//    @Override
//    public ChatRoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.item_chat_room, parent, false);
//        return new ChatRoomViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull ChatRoomViewHolder holder, int position) {
//        ChatRoom chatRoom = chatRooms.get(position);
//        holder.bind(chatRoom);
//    }
//
//    @Override
//    public int getItemCount() {
//        return chatRooms.size();
//    }
//
//    public void updateChatRooms(List<ChatRoom> newChatRooms) {
//        this.chatRooms = newChatRooms;
//        notifyDataSetChanged();
//    }
//
//    class ChatRoomViewHolder extends RecyclerView.ViewHolder {
//        TextView txtCustomerName, txtLastMessage, txtTime, txtUnreadCount;
//        View unreadIndicator;
//
//        ChatRoomViewHolder(View itemView) {
//            super(itemView);
//            txtCustomerName = itemView.findViewById(R.id.txt_customer_name);
//            txtLastMessage = itemView.findViewById(R.id.txt_last_message);
//            txtTime = itemView.findViewById(R.id.txt_time);
//            txtUnreadCount = itemView.findViewById(R.id.txt_unread_count);
//            unreadIndicator = itemView.findViewById(R.id.unread_indicator);
//
//            itemView.setOnClickListener(v -> {
//                if (listener != null) {
//                    listener.onChatRoomClick(chatRooms.get(getAdapterPosition()));
//                }
//            });
//        }
//
//        void bind(ChatRoom chatRoom) {
//            txtCustomerName.setText(chatRoom.getCustomer_name());
//            txtLastMessage.setText(chatRoom.getLastMessage());
//            txtTime.setText(timeFormat.format(new Date(chatRoom.getLastMessageTime())));
//
//            int unreadCount = chatRoom.getUnreadCount();
//            if (unreadCount > 0) {
//                txtUnreadCount.setVisibility(View.VISIBLE);
//                txtUnreadCount.setText(String.valueOf(unreadCount));
//                unreadIndicator.setVisibility(View.VISIBLE);
//                itemView.setBackgroundResource(R.drawable.chat_room_unread_background);
//            } else {
//                txtUnreadCount.setVisibility(View.GONE);
//                unreadIndicator.setVisibility(View.GONE);
//                itemView.setBackgroundResource(R.drawable.chat_room_background);
//            }
//        }
//    }
//}





package com.group7.pawdictedadmin.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
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
        ImageView imgCustomerAvatar; // Thêm ImageView cho avatar
        View unreadIndicator;

        ChatRoomViewHolder(View itemView) {
            super(itemView);
            txtCustomerName = itemView.findViewById(R.id.txt_customer_name);
            txtLastMessage = itemView.findViewById(R.id.txt_last_message);
            txtTime = itemView.findViewById(R.id.txt_time);
            txtUnreadCount = itemView.findViewById(R.id.txt_unread_count);
            imgCustomerAvatar = itemView.findViewById(R.id.img_customer_avatar); // Khởi tạo ImageView
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

            // Load avatar sử dụng Glide
            if (chatRoom.getCustomer_avatar() != null && !chatRoom.getCustomer_avatar().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(chatRoom.getCustomer_avatar())
                        .placeholder(R.mipmap.ic_account_footer_red) // Placeholder khi đang load
                        .error(R.mipmap.ic_account_footer_red) // Hiển thị khi load lỗi
                        .circleCrop() // Tạo avatar tròn
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(imgCustomerAvatar);
            } else {
                // Hiển thị avatar mặc định nếu không có avatar
                imgCustomerAvatar.setImageResource(R.mipmap.ic_account_footer_red);
            }

            int unreadCount = chatRoom.getUnreadCount();
            if (unreadCount > 0) {
                txtUnreadCount.setVisibility(View.VISIBLE);
                txtUnreadCount.setText(String.valueOf(unreadCount));
                unreadIndicator.setVisibility(View.VISIBLE);
                itemView.setBackgroundResource(R.drawable.chat_room_background);
            } else {
                txtUnreadCount.setVisibility(View.GONE);
                unreadIndicator.setVisibility(View.GONE);
                itemView.setBackgroundResource(R.drawable.chat_room_unread_background);
            }
        }
    }
}