package com.group7.pawdictedadmin.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.group7.pawdictedadmin.OrderDetailActivity;
import com.group7.pawdictedadmin.R;
import com.group7.pawdictedadmin.models.Order;
import com.group7.pawdictedadmin.models.OrderItem;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class PendingOrderAdapter extends RecyclerView.Adapter<PendingOrderAdapter.ViewHolder> {

    private final Context context;
    private final List<Order> orderList;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault());

    public PendingOrderAdapter(Context context, List<Order> orderList) {
        this.context = context;
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orderList.get(position);

        // Set order code
        holder.tvOrderCode.setText("#" + (order.getId() != null ? order.getId() : "N/A"));

        // Set customer name
        Order.Customer customer = order.getCustomer();
        holder.tvCustomerName.setText(customer != null && customer.getName() != null ? customer.getName() : "Unknown");

        // Set order value
        holder.tvOrderValue.setText(String.format(Locale.getDefault(), "%,.0f₫", order.getTotalAmount() > 0 ? order.getTotalAmount() : 0));

        // Set status
        String status = order.getStatus() != null ? order.getStatus() : "N/A";
        holder.tvStatus.setText(status.toUpperCase());

        // Set status color based on status
        if (status.equalsIgnoreCase("Pending Payment")) {
            holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.holo_orange_dark));
        } else if (status.equalsIgnoreCase("Shipped")) {
            holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
        } else {
            holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
        }

        // Set order time
        if (order.getOrderDate() != null) {
            holder.tvOrderTime.setText(dateFormat.format(order.getOrderDate()));
        } else {
            holder.tvOrderTime.setText("N/A");
        }

        // Set item count
        List<OrderItem> items = order.getItems();
        int itemCount = (items != null) ? items.size() : 0;
        holder.tvItemCount.setText(context.getString(R.string.total) + itemCount + context.getString(R.string.n_products));

        // Set customer avatar if available
        if (customer != null && customer.getAvatarImg() != null && !customer.getAvatarImg().isEmpty()) {
            if (holder.imgCustomerAvatar != null) {
                Glide.with(context)
                        .load(customer.getAvatarImg())
                        .placeholder(R.mipmap.ic_launcher)
                        .error(R.mipmap.ic_launcher)
                        .circleCrop()
                        .into(holder.imgCustomerAvatar);
            }
        } else {
            if (holder.imgCustomerAvatar != null) {
                holder.imgCustomerAvatar.setImageResource(R.mipmap.ic_launcher);
            }
        }

        // Set click listener for the entire item
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, OrderDetailActivity.class);
            intent.putExtra("ORDER_ID", order.getId());
            context.startActivity(intent);
        });

        // Set click listener for order details button if available
        if (holder.btnOrderDetails != null) {
            holder.btnOrderDetails.setOnClickListener(v -> {
                Intent intent = new Intent(context, OrderDetailActivity.class);
                intent.putExtra("ORDER_ID", order.getId());
                context.startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        return orderList != null ? orderList.size() : 0;
    }

    // Method to update the list and refresh adapter
    public void updateOrders(List<Order> newOrderList) {
        if (newOrderList != null) {
            this.orderList.clear();
            this.orderList.addAll(newOrderList);
            notifyDataSetChanged();
        }
    }

    // Method to add new order
    public void addOrder(Order order) {
        if (order != null && orderList != null) {
            orderList.add(order);
            notifyItemInserted(orderList.size() - 1);
        }
    }

    // Method to remove order
    public void removeOrder(int position) {
        if (orderList != null && position >= 0 && position < orderList.size()) {
            orderList.remove(position);
            notifyItemRemoved(position);
        }
    }

    // Method to update specific order
    public void updateOrder(Order updatedOrder) {
        if (updatedOrder != null && orderList != null) {
            for (int i = 0; i < orderList.size(); i++) {
                if (orderList.get(i).getId().equals(updatedOrder.getId())) {
                    orderList.set(i, updatedOrder);
                    notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvOrderCode, tvCustomerName, tvOrderValue, tvStatus, tvOrderTime, tvItemCount;
        public ImageView imgCustomerAvatar;
        public MaterialButton btnOrderDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderCode = itemView.findViewById(R.id.tv_order_code);
            tvCustomerName = itemView.findViewById(R.id.tv_customer_name);
            tvOrderValue = itemView.findViewById(R.id.tv_order_value);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvOrderTime = itemView.findViewById(R.id.tv_order_time);
            tvItemCount = itemView.findViewById(R.id.tv_item_count);

            // These might be null if not present in layout
            imgCustomerAvatar = itemView.findViewById(R.id.img_customer_avatar);
//            btnOrderDetails = itemView.findViewById(R.id.btn_order_details);
        }
    }
}