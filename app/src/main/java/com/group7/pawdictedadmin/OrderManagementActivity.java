//package com.group7.pawdictedadmin;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.View;
//import android.widget.ImageView;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
//
//import com.group7.pawdictedadmin.adapters.PendingOrderAdapter;
//import com.group7.pawdictedadmin.models.Order;
//import com.group7.pawdictedadmin.models.OrderItem;
//import com.google.android.gms.tasks.OnCompleteListener;
//import com.google.android.gms.tasks.Task;
//import com.google.firebase.Timestamp;
//import com.google.firebase.firestore.DocumentSnapshot;
//import com.google.firebase.firestore.FirebaseFirestore;
//import com.google.firebase.firestore.QueryDocumentSnapshot;
//import com.google.firebase.firestore.QuerySnapshot;
//
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//import java.util.Map;
//
//public class OrderManagementActivity extends AppCompatActivity {
//
//    private static final String TAG = "OrderManagement";
//    private static final int ORDER_DETAIL_REQUEST = 1001;
//
//    private FirebaseFirestore db;
//    private PendingOrderAdapter adapter;
//    private List<Order> orderList = new ArrayList<>();
//
//    // UI Components
//    private ImageView btnBack;
//    private RecyclerView recyclerView;
//    private SwipeRefreshLayout swipeRefreshLayout;
//    private ProgressBar progressBar;
//    private TextView tvEmptyState;
//
//    // Loading counters
//    private int totalOrdersToLoad = 0;
//    private int loadedOrdersCount = 0;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_order_management);
//
//        db = FirebaseFirestore.getInstance();
//
//        initViews();
//        setupRecyclerView();
//        setupSwipeRefresh();
//        loadPendingOrders();
//    }
//
//    private void initViews() {
//        btnBack = findViewById(R.id.btnBack);
//        recyclerView = findViewById(R.id.recycler_pending_orders);
////        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
////        progressBar = findViewById(R.id.progress_bar);
////        tvEmptyState = findViewById(R.id.tv_empty_state);
//
//        btnBack.setOnClickListener(v -> onBackPressed());
//    }
//
//    private void setupRecyclerView() {
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
//        adapter = new PendingOrderAdapter(this, orderList);
//        recyclerView.setAdapter(adapter);
//    }
//
//    private void setupSwipeRefresh() {
//        if (swipeRefreshLayout != null) {
//            swipeRefreshLayout.setOnRefreshListener(() -> {
//                loadPendingOrders();
//            });
//            swipeRefreshLayout.setColorSchemeResources(
//                    android.R.color.holo_blue_bright,
//                    android.R.color.holo_green_light,
//                    android.R.color.holo_orange_light,
//                    android.R.color.holo_red_light
//            );
//        }
//    }
//
//    private void loadPendingOrders() {
//        showLoading(true);
//
//        db.collection("orders")
//                .whereEqualTo("order_status", "Pending Payment")
//                .get()
//                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                        if (task.isSuccessful()) {
//                            QuerySnapshot result = task.getResult();
//                            if (result != null) {
//                                orderList.clear();
//                                totalOrdersToLoad = result.size();
//                                loadedOrdersCount = 0;
//
//                                Log.d(TAG, "Number of pending orders: " + totalOrdersToLoad);
//
//                                if (totalOrdersToLoad == 0) {
//                                    showEmptyState(true);
//                                    showLoading(false);
//                                    return;
//                                }
//
//                                showEmptyState(false);
//
//                                for (QueryDocumentSnapshot document : result) {
//                                    Order order = parseBasicOrderData(document);
//                                    orderList.add(order);
//
//                                    // Load additional data for each order
//                                    loadOrderCompleteData(order, document);
//                                }
//                            } else {
//                                showEmptyState(true);
//                                showLoading(false);
//                            }
//                        } else {
//                            Log.e(TAG, "Error getting orders", task.getException());
//                            Toast.makeText(OrderManagementActivity.this, R.string.failed_to_load_orders, Toast.LENGTH_SHORT).show();
//                            showLoading(false);
//                        }
//                    }
//                });
//    }
//
//    private Order parseBasicOrderData(QueryDocumentSnapshot document) {
//        Order order = new Order();
//        order.setId(document.getId());
//
//        // Set order status
//        String status = document.getString("order_status");
//        order.setStatus(status != null ? status : "Unknown");
//
//        // Handle order_time - multiple formats support
//        Object orderTimeObj = document.get("order_time");
//        if (orderTimeObj instanceof Timestamp) {
//            order.setOrderDate(((Timestamp) orderTimeObj).toDate());
//        } else if (orderTimeObj instanceof Date) {
//            order.setOrderDate((Date) orderTimeObj);
//        } else if (orderTimeObj instanceof Long) {
//            order.setOrderDate(new Date((Long) orderTimeObj));
//        } else if (orderTimeObj instanceof String) {
//            Log.d(TAG, "Order time is string: " + orderTimeObj);
//            // For string format, you might need to parse it according to your format
//            order.setOrderDate(new Date()); // Default to current date
//        } else {
//            Log.w(TAG, "Unknown order_time format: " + orderTimeObj);
//            order.setOrderDate(new Date());
//        }
//
//        // Set order value
//        Double orderValue = document.getDouble("order_value");
//        order.setTotalAmount(orderValue != null ? orderValue : 0.0);
//
//        // Set payment method
//        String paymentMethod = document.getString("payment_method");
//        order.setPaymentMethod(paymentMethod != null ? paymentMethod : "Cash on Delivery");
//
//        // Set shipping fee
//        Double shippingFee = document.getDouble("shipping_fee");
//        order.setShippingFee(shippingFee != null ? shippingFee : 30000.0);
//
//        return order;
//    }
//
//    private void loadOrderCompleteData(Order order, QueryDocumentSnapshot document) {
//        // Load customer details
//        String customerId = document.getString("customer_id");
//        if (customerId != null) {
//            fetchCustomerDetails(order, customerId);
//        } else {
//            // Set default customer if no customer ID
//            Order.Customer defaultCustomer = new Order.Customer();
//            defaultCustomer.setName("Unknown Customer");
//            order.setCustomer(defaultCustomer);
//            onOrderDataLoaded();
//        }
//
//        // Load order items
//        String orderItemId = document.getString("order_item_id");
//        if (orderItemId != null) {
//            fetchOrderItems(order, orderItemId);
//        } else {
//            // Set empty items list if no order item ID
//            order.setItems(new ArrayList<>());
//            onOrderDataLoaded();
//        }
//    }
//
//    private void fetchCustomerDetails(Order order, String customerId) {
//        db.collection("customers").document(customerId)
//                .get()
//                .addOnCompleteListener(customerTask -> {
//                    if (customerTask.isSuccessful()) {
//                        DocumentSnapshot customerDoc = customerTask.getResult();
//                        if (customerDoc != null && customerDoc.exists()) {
//                            Order.Customer customer = new Order.Customer();
//                            customer.setName(customerDoc.getString("customer_name"));
//                            customer.setPhone(customerDoc.getString("phone_number"));
//                            customer.setAddress(customerDoc.getString("address"));
//                            customer.setAvatarImg(customerDoc.getString("avatar_img"));
//                            order.setCustomer(customer);
//
//                            Log.d(TAG, "Customer loaded for order " + order.getId() + ": " + customer.getName());
//                        } else {
//                            Log.w(TAG, "Customer not found for customerId: " + customerId);
//                            // Set default customer
//                            Order.Customer defaultCustomer = new Order.Customer();
//                            defaultCustomer.setName("Unknown Customer");
//                            order.setCustomer(defaultCustomer);
//                        }
//                    } else {
//                        Log.e(TAG, "Error fetching customer", customerTask.getException());
//                        // Set default customer on error
//                        Order.Customer defaultCustomer = new Order.Customer();
//                        defaultCustomer.setName("Unknown Customer");
//                        order.setCustomer(defaultCustomer);
//                    }
//
//                    onOrderDataLoaded();
//                });
//    }
//
//    private void fetchOrderItems(Order order, String orderItemId) {
//        db.collection("order_items").document(orderItemId)
//                .get()
//                .addOnCompleteListener(itemTask -> {
//                    if (itemTask.isSuccessful()) {
//                        DocumentSnapshot itemDoc = itemTask.getResult();
//                        if (itemDoc != null && itemDoc.exists()) {
//                            List<OrderItem> items = parseOrderItemsFromDocument(itemDoc);
//                            order.setItems(items);
//                            Log.d(TAG, "Found " + items.size() + " items for order " + order.getId());
//                        } else {
//                            Log.w(TAG, "Order item not found for orderItemId: " + orderItemId);
//                            order.setItems(new ArrayList<>());
//                        }
//                    } else {
//                        Log.e(TAG, "Error fetching order item", itemTask.getException());
//                        order.setItems(new ArrayList<>());
//                    }
//
//                    onOrderDataLoaded();
//                });
//    }
//
//    private List<OrderItem> parseOrderItemsFromDocument(DocumentSnapshot itemDoc) {
//        List<OrderItem> items = new ArrayList<>();
//
//        // Parse products from document (product1, product2, etc.)
//        for (int i = 1; i <= 20; i++) {
//            String productKey = "product" + i;
//            Object productObj = itemDoc.get(productKey);
//
//            if (productObj instanceof Map) {
//                @SuppressWarnings("unchecked")
//                Map<String, Object> productMap = (Map<String, Object>) productObj;
//
//                OrderItem item = new OrderItem();
//
//                // Set product ID
//                String productId = (String) productMap.get("product_id");
//                item.setProductId(productId);
//
//                // Parse quantity
//                Object quantityObj = productMap.get("quantity");
//                if (quantityObj instanceof Long) {
//                    item.setQuantity(((Long) quantityObj).intValue());
//                } else if (quantityObj instanceof Integer) {
//                    item.setQuantity((Integer) quantityObj);
//                } else {
//                    item.setQuantity(1);
//                }
//
//                // Parse total cost
//                Object totalCostObj = productMap.get("total_cost_of_goods");
//                if (totalCostObj instanceof Double) {
//                    item.setTotalCost((Double) totalCostObj);
//                } else if (totalCostObj instanceof Long) {
//                    item.setTotalCost(((Long) totalCostObj).doubleValue());
//                } else if (totalCostObj instanceof Integer) {
//                    item.setTotalCost(((Integer) totalCostObj).doubleValue());
//                } else {
//                    item.setTotalCost(0.0);
//                }
//
//                items.add(item);
//            }
//        }
//
//        return items;
//    }
//
//    private void onOrderDataLoaded() {
//        loadedOrdersCount++;
//        Log.d(TAG, "Order data loaded: " + loadedOrdersCount + "/" + totalOrdersToLoad);
//
//        if (loadedOrdersCount >= totalOrdersToLoad) {
//            // All orders loaded, update UI
//            runOnUiThread(() -> {
//                adapter.notifyDataSetChanged();
//                showLoading(false);
//
//                if (orderList.isEmpty()) {
//                    showEmptyState(true);
//                } else {
//                    showEmptyState(false);
//                }
//            });
//        }
//    }
//
//    private void showLoading(boolean show) {
//        if (progressBar != null) {
//            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
//        }
//
//        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
//            swipeRefreshLayout.setRefreshing(false);
//        }
//    }
//
//    private void showEmptyState(boolean show) {
//        if (tvEmptyState != null) {
//            tvEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
//            tvEmptyState.setText("No pending orders found");
//        }
//
//        if (recyclerView != null) {
//            recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
//        }
//    }
//
////    @Override
////    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
////        super.onActivityResult(requestCode, resultCode, data);
////
////        if (requestCode == ORDER_DETAIL_REQUEST && resultCode == RESULT_OK) {
////            // Refresh the order list when returning from order detail
////            if (data != null) {
////                String orderId = data.getStringExtra("ORDER_ID");
////                String newStatus = data.getStringExtra("NEW_STATUS");
////
////                if (orderId != null && "Shipped".equals(newStatus)) {
////                    // Remove the order from pending list since it's no longer pending
////                    removeOrderFromList(orderId);
////                }
////            }
////        }
////    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (requestCode == ORDER_DETAIL_REQUEST && resultCode == RESULT_OK) {
//            if (data != null) {
//                String orderId = data.getStringExtra("ORDER_ID");
//                String newStatus = data.getStringExtra("NEW_STATUS");
//                boolean refreshNeeded = data.getBooleanExtra("REFRESH_NEEDED", false);
//
//                if (orderId != null && newStatus != null && refreshNeeded) {
//                    // Remove the order from the list if it's no longer pending
//                    if ("Shipped".equals(newStatus) || "Confirmed".equals(newStatus)) {
//                        removeOrderFromList(orderId);
//                    }
//                    // Reload the entire list to ensure data consistency
//                    loadPendingOrders();
//                } else {
//                    Log.w(TAG, "Invalid or missing data in result intent");
//                }
//            } else {
//                Log.w(TAG, "No data returned from OrderDetailActivity");
//            }
//        }
//    }
//
//
//    private void removeOrderFromList(String orderId) {
//        for (int i = 0; i < orderList.size(); i++) {
//            if (orderList.get(i).getId().equals(orderId)) {
//                orderList.remove(i);
//                adapter.notifyItemRemoved(i);
//
//                if (orderList.isEmpty()) {
//                    showEmptyState(true);
//                }
//                break;
//            }
//        }
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        // Optionally refresh data when activity resumes
//        // loadPendingOrders();
//    }
//
//    @Override
//    public void onBackPressed() {
//        super.onBackPressed();
//    }
//}



package com.group7.pawdictedadmin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.group7.pawdictedadmin.adapters.PendingOrderAdapter;
import com.group7.pawdictedadmin.models.Order;
import com.group7.pawdictedadmin.models.OrderItem;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class OrderManagementActivity extends AppCompatActivity {

    private static final String TAG = "OrderManagement";
    private static final int ORDER_DETAIL_REQUEST = 1001;

    private FirebaseFirestore db;
    private PendingOrderAdapter adapter;
    private List<Order> orderList = new ArrayList<>();

    // UI Components
    private ImageView btnBack;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView tvEmptyState;

    // Loading counters
    private int totalOrdersToLoad = 0;
    private int loadedOrdersCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_management);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupSwipeRefresh();
        loadPendingOrders();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        recyclerView = findViewById(R.id.recycler_pending_orders);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        progressBar = findViewById(R.id.progress_bar);
        tvEmptyState = findViewById(R.id.tv_empty_state);

        btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PendingOrderAdapter(this, orderList);
        recyclerView.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadPendingOrders();
            });
            swipeRefreshLayout.setColorSchemeResources(
                    android.R.color.holo_blue_bright,
                    android.R.color.holo_green_light,
                    android.R.color.holo_orange_light,
                    android.R.color.holo_red_light
            );
        }
    }

    private void loadPendingOrders() {
        showLoading(true);

        db.collection("orders")
                .whereEqualTo("order_status", "Pending Payment")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot result = task.getResult();
                            if (result != null) {
                                orderList.clear();
                                totalOrdersToLoad = result.size();
                                loadedOrdersCount = 0;

                                Log.d(TAG, "Number of pending orders: " + totalOrdersToLoad);

                                if (totalOrdersToLoad == 0) {
                                    showEmptyState(true);
                                    showLoading(false);
                                    return;
                                }

                                showEmptyState(false);

                                for (QueryDocumentSnapshot document : result) {
                                    Order order = parseBasicOrderData(document);
                                    orderList.add(order);

                                    // Load additional data for each order
                                    loadOrderCompleteData(order, document);
                                }
                            } else {
                                showEmptyState(true);
                                showLoading(false);
                            }
                        } else {
                            Log.e(TAG, "Error getting orders", task.getException());
                            Toast.makeText(OrderManagementActivity.this, R.string.failed_to_load_orders, Toast.LENGTH_SHORT).show();
                            showLoading(false);
                        }
                    }
                });
    }

    private Order parseBasicOrderData(QueryDocumentSnapshot document) {
        Order order = new Order();
        order.setId(document.getId());

        // Set order status
        String status = document.getString("order_status");
        order.setStatus(status != null ? status : "Unknown");

        // Handle order_time - multiple formats support
        Object orderTimeObj = document.get("order_time");
        if (orderTimeObj instanceof Timestamp) {
            order.setOrderDate(((Timestamp) orderTimeObj).toDate());
        } else if (orderTimeObj instanceof Date) {
            order.setOrderDate((Date) orderTimeObj);
        } else if (orderTimeObj instanceof Long) {
            order.setOrderDate(new Date((Long) orderTimeObj));
        } else if (orderTimeObj instanceof String) {
            Log.d(TAG, "Order time is string: " + orderTimeObj);
            // For string format, you might need to parse it according to your format
            order.setOrderDate(new Date()); // Default to current date
        } else {
            Log.w(TAG, "Unknown order_time format: " + orderTimeObj);
            order.setOrderDate(new Date());
        }

        // Set order value
        Double orderValue = document.getDouble("order_value");
        order.setTotalAmount(orderValue != null ? orderValue : 0.0);

        // Set payment method
        String paymentMethod = document.getString("payment_method");
        order.setPaymentMethod(paymentMethod != null ? paymentMethod : "Cash on Delivery");

        // Set shipping fee
        Double shippingFee = document.getDouble("shipping_fee");
        order.setShippingFee(shippingFee != null ? shippingFee : 30000.0);

        return order;
    }

    private void loadOrderCompleteData(Order order, QueryDocumentSnapshot document) {
        // Load customer details
        String customerId = document.getString("customer_id");
        if (customerId != null) {
            fetchCustomerDetails(order, customerId);
        } else {
            // Set default customer if no customer ID
            Order.Customer defaultCustomer = new Order.Customer();
            defaultCustomer.setName("Unknown Customer");
            order.setCustomer(defaultCustomer);
            onOrderDataLoaded();
        }

        // Load order items
        String orderItemId = document.getString("order_item_id");
        if (orderItemId != null) {
            fetchOrderItems(order, orderItemId);
        } else {
            // Set empty items list if no order item ID
            order.setItems(new ArrayList<>());
            onOrderDataLoaded();
        }
    }

    private void fetchCustomerDetails(Order order, String customerId) {
        db.collection("customers").document(customerId)
                .get()
                .addOnCompleteListener(customerTask -> {
                    if (customerTask.isSuccessful()) {
                        DocumentSnapshot customerDoc = customerTask.getResult();
                        if (customerDoc != null && customerDoc.exists()) {
                            Order.Customer customer = new Order.Customer();
                            customer.setName(customerDoc.getString("customer_name"));
                            customer.setPhone(customerDoc.getString("phone_number"));
                            customer.setAddress(customerDoc.getString("address"));
                            customer.setAvatarImg(customerDoc.getString("avatar_img"));
                            order.setCustomer(customer);

                            Log.d(TAG, "Customer loaded for order " + order.getId() + ": " + customer.getName());
                        } else {
                            Log.w(TAG, "Customer not found for customerId: " + customerId);
                            // Set default customer
                            Order.Customer defaultCustomer = new Order.Customer();
                            defaultCustomer.setName("Unknown Customer");
                            order.setCustomer(defaultCustomer);
                        }
                    } else {
                        Log.e(TAG, "Error fetching customer", customerTask.getException());
                        // Set default customer on error
                        Order.Customer defaultCustomer = new Order.Customer();
                        defaultCustomer.setName("Unknown Customer");
                        order.setCustomer(defaultCustomer);
                    }

                    onOrderDataLoaded();
                });
    }

    private void fetchOrderItems(Order order, String orderItemId) {
        db.collection("order_items").document(orderItemId)
                .get()
                .addOnCompleteListener(itemTask -> {
                    if (itemTask.isSuccessful()) {
                        DocumentSnapshot itemDoc = itemTask.getResult();
                        if (itemDoc != null && itemDoc.exists()) {
                            List<OrderItem> items = parseOrderItemsFromDocument(itemDoc);
                            order.setItems(items);
                            Log.d(TAG, "Found " + items.size() + " items for order " + order.getId());
                        } else {
                            Log.w(TAG, "Order item not found for orderItemId: " + orderItemId);
                            order.setItems(new ArrayList<>());
                        }
                    } else {
                        Log.e(TAG, "Error fetching order item", itemTask.getException());
                        order.setItems(new ArrayList<>());
                    }

                    onOrderDataLoaded();
                });
    }

    private List<OrderItem> parseOrderItemsFromDocument(DocumentSnapshot itemDoc) {
        List<OrderItem> items = new ArrayList<>();

        // Parse products from document (product1, product2, etc.)
        for (int i = 1; i <= 20; i++) {
            String productKey = "product" + i;
            Object productObj = itemDoc.get(productKey);

            if (productObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> productMap = (Map<String, Object>) productObj;

                OrderItem item = new OrderItem();

                // Set product ID
                String productId = (String) productMap.get("product_id");
                item.setProductId(productId);

                // Parse quantity
                Object quantityObj = productMap.get("quantity");
                if (quantityObj instanceof Long) {
                    item.setQuantity(((Long) quantityObj).intValue());
                } else if (quantityObj instanceof Integer) {
                    item.setQuantity((Integer) quantityObj);
                } else {
                    item.setQuantity(1);
                }

                // Parse total cost
                Object totalCostObj = productMap.get("total_cost_of_goods");
                if (totalCostObj instanceof Double) {
                    item.setTotalCost((Double) totalCostObj);
                } else if (totalCostObj instanceof Long) {
                    item.setTotalCost(((Long) totalCostObj).doubleValue());
                } else if (totalCostObj instanceof Integer) {
                    item.setTotalCost(((Integer) totalCostObj).doubleValue());
                } else {
                    item.setTotalCost(0.0);
                }

                items.add(item);
            }
        }

        return items;
    }

    private void onOrderDataLoaded() {
        loadedOrdersCount++;
        Log.d(TAG, "Order data loaded: " + loadedOrdersCount + "/" + totalOrdersToLoad);

        if (loadedOrdersCount >= totalOrdersToLoad) {
            // Sort orders by orderDate (oldest to newest)
            Collections.sort(orderList, new Comparator<Order>() {
                @Override
                public int compare(Order o1, Order o2) {
                    if (o1.getOrderDate() == null || o2.getOrderDate() == null) {
                        return 0; // Handle null cases
                    }
                    return o1.getOrderDate().compareTo(o2.getOrderDate()); // Ascending order
                }
            });

            // All orders loaded, update UI
            runOnUiThread(() -> {
                adapter.notifyDataSetChanged();
                showLoading(false);

                if (orderList.isEmpty()) {
                    showEmptyState(true);
                } else {
                    showEmptyState(false);
                }
            });
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void showEmptyState(boolean show) {
        if (tvEmptyState != null) {
            tvEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
            tvEmptyState.setText("No pending orders found");
        }

        if (recyclerView != null) {
            recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ORDER_DETAIL_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                String orderId = data.getStringExtra("ORDER_ID");
                String newStatus = data.getStringExtra("NEW_STATUS");
                boolean refreshNeeded = data.getBooleanExtra("REFRESH_NEEDED", false);

                if (orderId != null && newStatus != null && refreshNeeded) {
                    // Remove the order from the list if it's no longer pending
                    if ("Shipped".equals(newStatus) || "Confirmed".equals(newStatus)) {
                        removeOrderFromList(orderId);
                    }
                    // Reload the entire list to ensure data consistency
                    loadPendingOrders();
                } else {
                    Log.w(TAG, "Invalid or missing data in result intent");
                }
            } else {
                Log.w(TAG, "No data returned from OrderDetailActivity");
            }
        }
    }

    private void removeOrderFromList(String orderId) {
        for (int i = 0; i < orderList.size(); i++) {
            if (orderList.get(i).getId().equals(orderId)) {
                orderList.remove(i);
                adapter.notifyItemRemoved(i);

                if (orderList.isEmpty()) {
                    showEmptyState(true);
                }
                break;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Optionally refresh data when activity resumes
        // loadPendingOrders();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}