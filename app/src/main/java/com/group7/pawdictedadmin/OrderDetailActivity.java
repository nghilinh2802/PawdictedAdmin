package com.group7.pawdictedadmin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.group7.pawdictedadmin.models.Order;
import com.group7.pawdictedadmin.models.OrderItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderDetailActivity extends AppCompatActivity {

    private static final String TAG = "OrderDetailActivity";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault());
    private static final int SHIPPING_FEE = 30000;

    private FirebaseFirestore db;
    private Order currentOrder;
    private String orderId;
    private List<OrderItem> orderItems = new ArrayList<>();
    private int loadedProductCount = 0;

    // UI Components
    private ImageView btnBack;
    private TextView tvCustomerName, tvPhone, tvAddress;
    private LinearLayout llProducts;
    private TextView tvTotalCost, tvShippingFee, tvFinalPrice;
    private TextView tvOrderCode, tvPaymentMethod, tvOrderTime;
    private MaterialButton btnConfirmOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        // Get order ID from intent
        orderId = getIntent().getStringExtra("ORDER_ID");
        if (orderId == null) {
            Toast.makeText(this, "Order ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        initViews();
        loadOrderDetails();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvCustomerName = findViewById(R.id.tv_customer_name);
        tvPhone = findViewById(R.id.tv_phone);
        tvAddress = findViewById(R.id.tv_address);
        llProducts = findViewById(R.id.ll_products);
        tvTotalCost = findViewById(R.id.tv_total_cost);
        tvShippingFee = findViewById(R.id.tv_shipping_fee);
        tvFinalPrice = findViewById(R.id.tv_final_price);
        tvOrderCode = findViewById(R.id.tv_order_code);
        tvPaymentMethod = findViewById(R.id.tv_payment_method);
        tvOrderTime = findViewById(R.id.tv_order_time);
        btnConfirmOrder = findViewById(R.id.btn_confirm_order);

        btnBack.setOnClickListener(v -> onBackPressed());
        btnConfirmOrder.setOnClickListener(v -> showConfirmDialog());
    }

    private void loadOrderDetails() {
        showLoadingState();

        db.collection("orders").document(orderId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            DocumentSnapshot document = task.getResult();
                            currentOrder = new Order();
                            currentOrder.setId(document.getId());

                            // Parse order data
                            parseOrderData(document);
                            updateBasicOrderInfo();

                            // Load customer details
                            String customerId = document.getString("customer_id");
                            if (customerId != null) {
                                loadCustomerDetails(customerId);
                            }

                            // Load order items
                            String orderItemId = document.getString("order_item_id");
                            if (orderItemId != null) {
                                loadOrderItems(orderItemId);
                            } else {
                                hideLoadingState();
                                updatePriceSummary();
                            }

                        } else {
                            Log.e(TAG, "Error getting order details", task.getException());
                            Toast.makeText(OrderDetailActivity.this, "Failed to load order details", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                });
    }

    private void parseOrderData(DocumentSnapshot document) {
        // Status
        String status = document.getString("order_status");
        currentOrder.setStatus(status != null ? status : "Unknown");

        // Order value
        Double orderValue = document.getDouble("order_value");
        currentOrder.setTotalAmount(orderValue != null ? orderValue : 0.0);

        // Order time - handle different timestamp formats
        Object orderTimeObj = document.get("order_time");
        if (orderTimeObj instanceof Timestamp) {
            currentOrder.setOrderDate(((Timestamp) orderTimeObj).toDate());
        } else if (orderTimeObj instanceof Date) {
            currentOrder.setOrderDate((Date) orderTimeObj);
        } else if (orderTimeObj instanceof Long) {
            currentOrder.setOrderDate(new Date((Long) orderTimeObj));
        } else {
            currentOrder.setOrderDate(new Date());
        }

        // Payment method
        String paymentMethod = document.getString("payment_method");
        currentOrder.setPaymentMethod(paymentMethod != null ? paymentMethod : "Cash on Delivery");

        // Shipping fee
        Double shippingFee = document.getDouble("shipping_fee");
        currentOrder.setShippingFee(shippingFee != null ? shippingFee : SHIPPING_FEE);

        // Order item ID
        String orderItemId = document.getString("order_item_id");
        currentOrder.setOrderItemId(orderItemId);
    }

    private void loadCustomerDetails(String customerId) {
        db.collection("customers").document(customerId)
                .get()
                .addOnCompleteListener(customerTask -> {
                    if (customerTask.isSuccessful() && customerTask.getResult().exists()) {
                        DocumentSnapshot customerDoc = customerTask.getResult();
                        Order.Customer customer = new Order.Customer();
                        customer.setName(customerDoc.getString("customer_name"));
                        customer.setPhone(customerDoc.getString("phone_number"));
                        customer.setAddress(customerDoc.getString("address"));
                        customer.setAvatarImg(customerDoc.getString("avatar_img"));
                        currentOrder.setCustomer(customer);

                        updateCustomerInfo();
                    } else {
                        Log.e(TAG, "Error fetching customer details", customerTask.getException());
                        // Set default customer info
                        updateCustomerInfo();
                    }
                });
    }

    private void loadOrderItems(String orderItemId) {
        db.collection("order_items").document(orderItemId)
                .get()
                .addOnCompleteListener(itemTask -> {
                    if (itemTask.isSuccessful() && itemTask.getResult().exists()) {
                        DocumentSnapshot itemDoc = itemTask.getResult();
                        parseOrderItems(itemDoc);
                    } else {
                        Log.e(TAG, "Error fetching order items", itemTask.getException());
                        hideLoadingState();
                        updatePriceSummary();
                    }
                });
    }

    private void parseOrderItems(DocumentSnapshot itemDoc) {
        orderItems.clear();
        loadedProductCount = 0;

        // Parse products from document
        for (int i = 1; i <= 20; i++) { // Increased limit to handle more products
            String productKey = "product" + i;
            Object productObj = itemDoc.get(productKey);

            if (productObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> productMap = (Map<String, Object>) productObj;

                OrderItem item = new OrderItem();
                item.setProductId((String) productMap.get("product_id"));

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
                } else {
                    item.setTotalCost(0.0);
                }

                orderItems.add(item);
            }
        }

        currentOrder.setItems(orderItems);

        // Load product details for each item
        if (orderItems.isEmpty()) {
            hideLoadingState();
            updatePriceSummary();
        } else {
            for (OrderItem item : orderItems) {
                loadProductDetails(item);
            }
        }
    }

    private void loadProductDetails(OrderItem item) {
        if (item.getProductId() == null || item.getProductId().isEmpty()) {
            onProductDetailLoaded();
            return;
        }

        db.collection("products").document(item.getProductId())
                .get()
                .addOnCompleteListener(productTask -> {
                    if (productTask.isSuccessful() && productTask.getResult().exists()) {
                        DocumentSnapshot productDoc = productTask.getResult();

                        // Set product details
                        item.setProductName(productDoc.getString("product_name"));
                        item.setProductImage(productDoc.getString("product_image"));
                        item.setVariant(productDoc.getString("variant"));

                        // Set unit price
                        Double unitPrice = productDoc.getDouble("price");
                        if (unitPrice != null) {
                            item.setUnitPrice(unitPrice);
                        }

                        Log.d(TAG, "Product loaded: " + item.getProductName());
                    } else {
                        Log.e(TAG, "Error fetching product details for ID: " + item.getProductId(), productTask.getException());
                        // Set default values
                        item.setProductName("Unknown Product");
                        item.setVariant("N/A");
                    }

                    onProductDetailLoaded();
                });
    }

    private void onProductDetailLoaded() {
        loadedProductCount++;
        Log.d(TAG, "Product detail loaded: " + loadedProductCount + "/" + orderItems.size());

        if (loadedProductCount >= orderItems.size()) {
            // All products loaded
            hideLoadingState();
            updateProductList();
            updatePriceSummary();
        }
    }

    private void updateBasicOrderInfo() {
        tvOrderCode.setText("#" + (currentOrder.getId() != null ? currentOrder.getId() : "N/A"));
        tvPaymentMethod.setText(currentOrder.getPaymentMethod());

        if (currentOrder.getOrderDate() != null) {
            tvOrderTime.setText(dateFormat.format(currentOrder.getOrderDate()));
        } else {
            tvOrderTime.setText("N/A");
        }

        // Show/hide confirm button based on order status
        if (currentOrder.isPending()) {
            btnConfirmOrder.setVisibility(View.VISIBLE);
        } else {
            btnConfirmOrder.setVisibility(View.GONE);
        }
    }

    private void updateCustomerInfo() {
        Order.Customer customer = currentOrder.getCustomer();
        if (customer != null) {
            tvCustomerName.setText(customer.getName() != null ? customer.getName() : "Unknown Customer");
            tvPhone.setText(customer.getPhone() != null ? customer.getPhone() : "N/A");
            tvAddress.setText(customer.getAddress() != null ? customer.getAddress() : "N/A");
        } else {
            tvCustomerName.setText("Unknown Customer");
            tvPhone.setText("N/A");
            tvAddress.setText("N/A");
        }
    }

    private void updateProductList() {
        llProducts.removeAllViews();

        if (orderItems != null && !orderItems.isEmpty()) {
            for (OrderItem item : orderItems) {
                View productView = createProductItemView(item);
                llProducts.addView(productView);
            }
        } else {
            // Show empty state
            TextView emptyView = new TextView(this);
            emptyView.setText("No products found");
            emptyView.setTextSize(14);
            emptyView.setPadding(16, 16, 16, 16);
            llProducts.addView(emptyView);
        }
    }

    private View createProductItemView(OrderItem item) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_product_detail, llProducts, false);

        TextView tvProductName = view.findViewById(R.id.tv_product_name);
        TextView tvProductVariant = view.findViewById(R.id.tv_product_variant);
        TextView tvPrice = view.findViewById(R.id.tv_price);
        TextView tvQuantity = view.findViewById(R.id.tv_quantity);
        ImageView imgProduct = view.findViewById(R.id.img_product);

        // Set product info
        String productName = item.getProductName() != null ? item.getProductName() : "Unknown Product";
        String productVariant = item.getVariant() != null ? item.getVariant() : "N/A";

        tvProductName.setText(productName);
        tvProductVariant.setText(productVariant);
        tvQuantity.setText("x" + item.getQuantity());
        tvPrice.setText(formatPrice(item.getTotalCost()));

        // Load product image
        if (item.getProductImage() != null && !item.getProductImage().isEmpty()) {
            Glide.with(this)
                    .load(item.getProductImage())
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .into(imgProduct);
        } else {
            imgProduct.setImageResource(R.mipmap.ic_launcher);
        }

        return view;
    }

    private void updatePriceSummary() {
        double totalCost = currentOrder.getTotalAmount();
        double shippingFee = currentOrder.getShippingFee();

        tvTotalCost.setText(formatPrice(totalCost-shippingFee));
        tvShippingFee.setText(formatPrice(shippingFee));
        tvFinalPrice.setText(formatPrice(totalCost));
    }

    private String formatPrice(double price) {
        return String.format(Locale.getDefault(), "đ%,.0f", price);
    }

    private void showConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_order)
                .setMessage(R.string.confirm_order_message)
                .setPositiveButton(R.string.confirm, (dialog, which) -> confirmOrder())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmOrder() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("order_status", "Shipped");
        updates.put("updated_at", new Date());

        db.collection("orders").document(orderId)
                .update(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(OrderDetailActivity.this, R.string.order_confirmed_successfully, Toast.LENGTH_SHORT).show();
                        currentOrder.setStatus("Shipped");
                        btnConfirmOrder.setVisibility(View.GONE);

                        // Set result to refresh the previous activity
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("ORDER_ID", orderId);
                        resultIntent.putExtra("NEW_STATUS", "Shipped");
                        setResult(RESULT_OK, resultIntent);

                    } else {
                        Log.e(TAG, "Error updating order status", task.getException());
                        Toast.makeText(OrderDetailActivity.this, R.string.failed_to_confirm_order_please_try_again, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showLoadingState() {
        // You can add a progress bar or loading indicator here
        btnConfirmOrder.setEnabled(false);
    }

    private void hideLoadingState() {
        btnConfirmOrder.setEnabled(true);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}