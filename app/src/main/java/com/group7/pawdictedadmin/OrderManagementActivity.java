package com.group7.pawdictedadmin;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.group7.pawdictedadmin.R;
import com.group7.pawdictedadmin.adapters.PendingOrderAdapter;
import com.group7.pawdictedadmin.models.Order;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OrderManagementActivity extends AppCompatActivity {

    private static final String TAG = "OrderManagement";
    private FirebaseFirestore db;
    private PendingOrderAdapter adapter;
    private List<Order> orderList = new ArrayList<>();
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_management);

        db = FirebaseFirestore.getInstance();
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> onBackPressed());

        setupRecyclerView();
        loadPendingOrders();
    }

    private void setupRecyclerView() {
        androidx.recyclerview.widget.RecyclerView recyclerView = findViewById(R.id.recycler_pending_orders);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PendingOrderAdapter(this, orderList);
        recyclerView.setAdapter(adapter);
    }

    private void loadPendingOrders() {
        db.collection("orders")
                .whereEqualTo("order_status", "Pending Payment")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            orderList.clear();
                            Log.d(TAG, "Number of pending orders: " + task.getResult().size());

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Order order = new Order();
                                order.setId(document.getId());

                                // Set basic order info
//                                order.setStatus(document.getString("order_status"));

                                // Handle order_time - có thể là Timestamp, String hoặc Long
                                Object orderTimeObj = document.get("order_time");
                                if (orderTimeObj instanceof Timestamp) {
                                    order.setOrderDate(((Timestamp) orderTimeObj).toDate());
                                } else if (orderTimeObj instanceof String) {
                                    // Nếu là string, bạn cần parse theo format của bạn
                                    Log.d(TAG, "Order time is string: " + orderTimeObj);
                                    // Tạm thời set current date
                                    order.setOrderDate(new Date());
                                } else if (orderTimeObj instanceof Long) {
                                    order.setOrderDate(new Date((Long) orderTimeObj));
                                } else {
                                    Log.w(TAG, "Unknown order_time format: " + orderTimeObj);
                                    order.setOrderDate(new Date());
                                }

                                // Set order value
                                Double orderValue = document.getDouble("order_value");
                                if (orderValue != null) {
                                    order.setTotalAmount(orderValue);
                                }

                                // Fetch customer details
                                String customerId = document.getString("customer_id");
                                if (customerId != null) {
                                    fetchCustomerDetails(order, customerId);
                                }

                                // Fetch order items - sửa logic này
                                String orderItemId = document.getString("order_item_id");
                                if (orderItemId != null) {
                                    fetchOrderItems(order, orderItemId);
                                }

                                orderList.add(order);
                            }

                            adapter.notifyDataSetChanged();
                        } else {
                            Log.e(TAG, "Error getting orders", task.getException());
                            Toast.makeText(OrderManagementActivity.this, "Failed to load orders", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void fetchCustomerDetails(Order order, String customerId) {
        db.collection("customers").document(customerId)
                .get()
                .addOnCompleteListener(customerTask -> {
                    if (customerTask.isSuccessful()) {
                        DocumentSnapshot customerDoc = customerTask.getResult();
                        if (customerDoc.exists()) {
                            Order.Customer customer = new Order.Customer();
                            customer.setName(customerDoc.getString("customer_name"));
                            customer.setPhone(customerDoc.getString("phone_number"));
                            customer.setAddress(customerDoc.getString("address"));
//                            customer.setAvatar_img(customerDoc.getString("avatar_img"));
                            order.setCustomer(customer);

                            // Tìm vị trí của order trong list và update
                            int position = orderList.indexOf(order);
                            if (position != -1) {
                                adapter.notifyItemChanged(position);
                            }
                        } else {
                            Log.w(TAG, "Customer not found for customerId: " + customerId);
                        }
                    } else {
                        Log.e(TAG, "Error fetching customer", customerTask.getException());
                    }
                });
    }

    private void fetchOrderItems(Order order, String orderItemId) {
        // Từ ảnh thứ 2, tôi thấy order_items có structure như một document
        // với product1, product2 là sub-objects
        db.collection("order_items").document(orderItemId)
                .get()
                .addOnCompleteListener(itemTask -> {
                    if (itemTask.isSuccessful()) {
                        DocumentSnapshot itemDoc = itemTask.getResult();
                        if (itemDoc.exists()) {
                            List<Order.OrderItem> items = new ArrayList<>();

                            // Parse các product từ document
                            // Từ ảnh, tôi thấy có product1, product2...
                            for (int i = 1; i <= 10; i++) { // Giả sử tối đa 10 products
                                String productKey = "product" + i;
                                Object productObj = itemDoc.get(productKey);

                                if (productObj instanceof java.util.Map) {
                                    @SuppressWarnings("unchecked")
                                    java.util.Map<String, Object> productMap = (java.util.Map<String, Object>) productObj;

                                    Order.OrderItem item = new Order.OrderItem();
                                    item.setProductId((String) productMap.get("product_id"));

                                    Object quantityObj = productMap.get("quantity");
                                    if (quantityObj instanceof Long) {
                                        item.setQuantity(((Long) quantityObj).intValue());
                                    } else if (quantityObj instanceof Integer) {
                                        item.setQuantity((Integer) quantityObj);
                                    }

                                    Object totalCostObj = productMap.get("total_cost_of_goods");
                                    if (totalCostObj instanceof Double) {
                                        item.setTotalCost((Double) totalCostObj);
                                    } else if (totalCostObj instanceof Long) {
                                        item.setTotalCost(((Long) totalCostObj).doubleValue());
                                    }

                                    items.add(item);
                                }
                            }

                            order.setItems(items);

                            // Update adapter
                            int position = orderList.indexOf(order);
                            if (position != -1) {
                                adapter.notifyItemChanged(position);
                            }

                            Log.d(TAG, "Found " + items.size() + " items for order " + order.getId());
                        } else {
                            Log.w(TAG, "Order item not found for orderItemId: " + orderItemId);
                        }
                    } else {
                        Log.e(TAG, "Error fetching order item", itemTask.getException());
                    }
                });
    }
}