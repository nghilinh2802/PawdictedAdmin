package com.group7.pawdictedadmin.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.group7.pawdictedadmin.R;
import com.group7.pawdictedadmin.models.MessageItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_CUSTOMER = 1;
    private static final int TYPE_ADMIN = 2;

    private List<MessageItem> messages;
    private SimpleDateFormat timeFormat;

    public AdminChatAdapter(List<MessageItem> messages) {
        this.messages = messages;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getSender().equals("customer") ? TYPE_CUSTOMER : TYPE_ADMIN;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_CUSTOMER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_customer, parent, false);
            return new CustomerMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_admin, parent, false);
            return new AdminMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageItem message = messages.get(position);

        if (holder instanceof CustomerMessageViewHolder) {
            ((CustomerMessageViewHolder) holder).bind(message);
        } else if (holder instanceof AdminMessageViewHolder) {
            ((AdminMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void updateMessages(List<MessageItem> newMessages) {
        this.messages = newMessages;
        notifyDataSetChanged();
    }

    class CustomerMessageViewHolder extends RecyclerView.ViewHolder {
        TextView txtMessage, txtTime;

        CustomerMessageViewHolder(View itemView) {
            super(itemView);
            txtMessage = itemView.findViewById(R.id.txt_message);
            txtTime = itemView.findViewById(R.id.txt_time);
        }

        void bind(MessageItem message) {
            txtMessage.setText(message.getContent());
            txtTime.setText(timeFormat.format(new Date(message.getTime())));
        }
    }

    class AdminMessageViewHolder extends RecyclerView.ViewHolder {
        TextView txtMessage, txtTime;

        AdminMessageViewHolder(View itemView) {
            super(itemView);
            txtMessage = itemView.findViewById(R.id.txt_message);
            txtTime = itemView.findViewById(R.id.txt_time);
        }

        void bind(MessageItem message) {
            txtMessage.setText(message.getContent());
            txtTime.setText(timeFormat.format(new Date(message.getTime())));
        }
    }
}
