package com.group7.pawdictedadmin.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.group7.pawdictedadmin.R;
import com.group7.pawdictedadmin.models.Order;
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

        holder.tvOrderCode.setText("#" + (order.getId() != null ? order.getId() : "N/A"));
        Order.Customer customer = order.getCustomer();
        holder.tvCustomerName.setText(customer != null && customer.getName() != null ? customer.getName() : "Unknown");
        holder.tvOrderValue.setText(String.format("%,.0f₫", order.getTotalAmount() > 0 ? order.getTotalAmount() : 0));
        holder.tvStatus.setText(order.getStatus() != null ? order.getStatus().toUpperCase() : "N/A");

        if (order.getOrderDate() != null) {
            holder.tvOrderTime.setText(dateFormat.format(order.getOrderDate()));
        } else {
            holder.tvOrderTime.setText("N/A");
        }

        List<Order.OrderItem> items = order.getItems();
        int itemCount = (items != null) ? items.size() : 0;
        holder.tvItemCount.setText("Tổng cộng: " + itemCount + " sản phẩm");

        Glide.with(context)
                .load(R.mipmap.ic_logo)
                .placeholder(R.mipmap.ic_logo)
                .error(R.mipmap.ic_logo)
                .circleCrop()
                .into(holder.imgCustomerAvatar);
    }

    @Override
    public int getItemCount() {
        return orderList != null ? orderList.size() : 0;
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
            imgCustomerAvatar = itemView.findViewById(R.id.img_customer_avatar);
//            btnOrderDetails = itemView.findViewById(R.id.tv_order_details); // Enable if in layout
        }
    }
}