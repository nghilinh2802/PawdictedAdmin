package com.group7.pawdictedadmin;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class RevenueReportActivity extends AppCompatActivity {

    private static final String TAG = "RevenueReport";

    // UI Components
    private TextView tvTotalRevenue, tvAvgDailyRevenue, tvBestDay, tvWorstDay;
    private TextView tvFlashsaleRevenue, tvNormalRevenue, tvVoucherImpact;
    private LineChart dailyRevenueChart;
    private BarChart monthlyRevenueChart;
    private ProgressBar progressBar;
    private ImageView ivBack;

    // Data & Firebase
    private FirebaseFirestore db;
    private NumberFormat currencyFormatter;
    private SimpleDateFormat dateFormat;

    // Revenue Data
    private Map<String, Double> dailyRevenueData = new HashMap<>();
    private Map<String, Double> monthlyRevenueData = new HashMap<>();
    private AtomicReference<Double> totalRevenue = new AtomicReference<>(0.0);
    private AtomicReference<Double> flashsaleRevenue = new AtomicReference<>(0.0);
    private AtomicReference<Double> normalRevenue = new AtomicReference<>(0.0);
    private AtomicReference<String> bestDayDate = new AtomicReference<>("");
    private AtomicReference<Double> bestDayRevenue = new AtomicReference<>(0.0);
    private AtomicReference<String> worstDayDate = new AtomicReference<>("");
    private AtomicReference<Double> worstDayRevenue = new AtomicReference<>(Double.MAX_VALUE);

    // Processing State
    private AtomicInteger processedOrders = new AtomicInteger(0);
    private int totalOrdersToProcess = 0;
    private Map<String, FlashsaleInfo> flashsaleCache = new HashMap<>();

    static class FlashsaleInfo {
        String productId;
        double discountRate;
        long startTime;
        long endTime;
        double originalPrice;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revenue_report);

        initializeComponents();
        loadRevenueData();
    }

    private void initializeComponents() {
        Log.d(TAG, "=== INITIALIZING REVENUE REPORT ACTIVITY ===");

        // Initialize views
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvAvgDailyRevenue = findViewById(R.id.tvAvgDailyRevenue);
        tvBestDay = findViewById(R.id.tvBestDay);
        tvWorstDay = findViewById(R.id.tvWorstDay);
        tvFlashsaleRevenue = findViewById(R.id.tvFlashsaleRevenue);
        tvNormalRevenue = findViewById(R.id.tvNormalRevenue);
        tvVoucherImpact = findViewById(R.id.tvVoucherImpact);
        dailyRevenueChart = findViewById(R.id.dailyRevenueChart);
        monthlyRevenueChart = findViewById(R.id.monthlyRevenueChart);
        progressBar = findViewById(R.id.progressBar);
        ivBack = findViewById(R.id.ivBack);

        // Initialize Firebase and formatters
        db = FirebaseFirestore.getInstance();
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        // Setup charts
        setupCharts();

        // Setup click listeners
        ivBack.setOnClickListener(v -> finish());

        Log.d(TAG, "Components initialized successfully");
    }

    private void setupCharts() {
        Log.d(TAG, "Setting up charts for revenue visualization");

        // Daily Revenue Chart
        dailyRevenueChart.getDescription().setEnabled(false);
        dailyRevenueChart.setTouchEnabled(true);
        dailyRevenueChart.setDragEnabled(true);
        dailyRevenueChart.setScaleEnabled(true);
        dailyRevenueChart.setPinchZoom(true);

        XAxis dailyXAxis = dailyRevenueChart.getXAxis();
        dailyXAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        dailyXAxis.setDrawGridLines(false);

        dailyRevenueChart.getAxisRight().setEnabled(false);
        dailyRevenueChart.getAxisLeft().setDrawGridLines(false);
        dailyRevenueChart.getLegend().setEnabled(true);

        // Monthly Revenue Chart
        monthlyRevenueChart.getDescription().setEnabled(false);
        monthlyRevenueChart.setTouchEnabled(true);
        monthlyRevenueChart.setDragEnabled(false);
        monthlyRevenueChart.setScaleEnabled(false);
        monthlyRevenueChart.setPinchZoom(false);

        XAxis monthlyXAxis = monthlyRevenueChart.getXAxis();
        monthlyXAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        monthlyXAxis.setDrawGridLines(false);

        monthlyRevenueChart.getAxisRight().setEnabled(false);
        monthlyRevenueChart.getAxisLeft().setDrawGridLines(false);
        monthlyRevenueChart.getLegend().setEnabled(false);

        Log.d(TAG, "Charts setup completed");
    }

    private void loadRevenueData() {
        showProgress(true);
        Log.d(TAG, "=== STARTING REVENUE DATA LOADING ===");

        resetData();

        // Step 1: Load flashsale data
        loadFlashsaleData(() -> {
            // Step 2: Analyze revenue
            analyzeRevenue();
        });
    }

    private void resetData() {
        Log.d(TAG, "Resetting all revenue data");

        totalRevenue.set(0.0);
        flashsaleRevenue.set(0.0);
        normalRevenue.set(0.0);
        processedOrders.set(0);
        totalOrdersToProcess = 0;

        flashsaleCache.clear();
        dailyRevenueData.clear();
        monthlyRevenueData.clear();

        bestDayDate.set("");
        bestDayRevenue.set(0.0);
        worstDayDate.set("");
        worstDayRevenue.set(Double.MAX_VALUE);

        Log.d(TAG, "Data reset completed");
    }

    private void loadFlashsaleData(Runnable onComplete) {
        Log.d(TAG, "=== LOADING FLASHSALE DATA FOR REVENUE CLASSIFICATION ===");

        db.collection("flashsales")
                .get()
                .addOnSuccessListener(flashsaleSnapshot -> {
                    Log.d(TAG, "Found " + flashsaleSnapshot.size() + " flashsale documents");

                    final AtomicInteger totalProducts = new AtomicInteger(0);
                    final AtomicInteger processedProducts = new AtomicInteger(0);

                    // Count total products
                    for (QueryDocumentSnapshot flashsaleDoc : flashsaleSnapshot) {
                        List<Map<String, Object>> products = (List<Map<String, Object>>) flashsaleDoc.get("products");
                        if (products != null) {
                            totalProducts.addAndGet(products.size());
                        }
                    }

                    Log.d(TAG, "Total flashsale products to process: " + totalProducts.get());

                    if (totalProducts.get() == 0) {
                        Log.w(TAG, "No flashsale products found, proceeding without flashsale data");
                        onComplete.run();
                        return;
                    }

                    // Process flashsales
                    for (QueryDocumentSnapshot flashsaleDoc : flashsaleSnapshot) {
                        Long startTime = flashsaleDoc.getLong("startTime");
                        Long endTime = flashsaleDoc.getLong("endTime");
                        String flashsaleName = flashsaleDoc.getString("flashSale_name");
                        List<Map<String, Object>> products = (List<Map<String, Object>>) flashsaleDoc.get("products");

                        // Tạo final variables cho lambda
                        final Long finalStartTime = startTime;
                        final Long finalEndTime = endTime;
                        final String finalFlashsaleName = flashsaleName;

                        Log.d(TAG, String.format("Processing flashsale: %s (Start: %d, End: %d)",
                                finalFlashsaleName, finalStartTime, finalEndTime));

                        if (products != null && finalStartTime != null && finalEndTime != null) {
                            for (Map<String, Object> product : products) {
                                String productId = (String) product.get("product_id");
                                Object discountRateObj = product.get("discountRate");

                                // Tạo final variables cho lambda
                                final String finalProductId = productId;

                                if (finalProductId != null && discountRateObj != null) {
                                    double discountRate = (discountRateObj instanceof Long) ?
                                            ((Long) discountRateObj).doubleValue() : (Double) discountRateObj;

                                    final double finalDiscountRate = discountRate;

                                    Log.d(TAG, String.format("Processing flashsale product: %s (Discount: %.2f%%)",
                                            finalProductId, finalDiscountRate));

                                    db.collection("products").document(finalProductId)
                                            .get()
                                            .addOnSuccessListener(productDoc -> {
                                                if (productDoc.exists()) {
                                                    Double originalPrice = productDoc.getDouble("price");
                                                    String productName = productDoc.getString("product_name");

                                                    if (originalPrice != null) {
                                                        FlashsaleInfo info = new FlashsaleInfo();
                                                        info.productId = finalProductId;
                                                        info.discountRate = finalDiscountRate;
                                                        info.startTime = finalStartTime;
                                                        info.endTime = finalEndTime;
                                                        info.originalPrice = originalPrice;

                                                        flashsaleCache.put(finalProductId, info);

                                                        Log.d(TAG, String.format("Cached flashsale info - Product: %s (%s), Discount: %.2f%%",
                                                                productName, finalProductId, finalDiscountRate));
                                                    }
                                                }

                                                if (processedProducts.incrementAndGet() >= totalProducts.get()) {
                                                    Log.d(TAG, "Flashsale data loading completed. Cached " + flashsaleCache.size() + " products");
                                                    testFlashsaleClassification();
                                                    onComplete.run();
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "Error loading product: " + finalProductId, e);
                                                if (processedProducts.incrementAndGet() >= totalProducts.get()) {
                                                    onComplete.run();
                                                }
                                            });
                                } else {
                                    if (processedProducts.incrementAndGet() >= totalProducts.get()) {
                                        onComplete.run();
                                    }
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading flashsale data", e);
                    onComplete.run();
                });
    }

    private void testFlashsaleClassification() {
        Log.d(TAG, "=== TESTING FLASHSALE CLASSIFICATION ===");
        Log.d(TAG, "Flashsale cache size: " + flashsaleCache.size());

        for (Map.Entry<String, FlashsaleInfo> entry : flashsaleCache.entrySet()) {
            String productId = entry.getKey();
            FlashsaleInfo info = entry.getValue();
            boolean isActive = isProductInActiveFlashsale(productId);

            Log.d(TAG, String.format("Product %s: Active=%s, Discount=%.2f%%",
                    productId, isActive, info.discountRate));
        }
        Log.d(TAG, "==========================================");
    }

    private void analyzeRevenue() {
        Log.d(TAG, "=== STARTING REVENUE ANALYSIS WITH CORRECTED LOGIC ===");

        db.collection("orders")
                .whereEqualTo("order_status", "Completed")
                .get()
                .addOnSuccessListener(orderSnapshot -> {
                    Log.d(TAG, "Found " + orderSnapshot.size() + " completed orders for revenue analysis");

                    if (orderSnapshot.isEmpty()) {
                        Log.w(TAG, "No completed orders found");
                        showProgress(false);
                        displayNoData();
                        return;
                    }

                    totalOrdersToProcess = orderSnapshot.size();

                    // THAY ĐỔI: Tính doanh thu từ total_cost_of_goods thay vì order_value
                    for (QueryDocumentSnapshot orderDoc : orderSnapshot) {
                        Object orderTimeObj = orderDoc.get("order_time");

                        // Group by date and month for chart data
                        String dateKey = extractDateKey(orderTimeObj);
                        String monthKey = extractMonthKey(orderTimeObj);

                        // Analyze each order for revenue calculation
                        analyzeOrderRevenueFromItems(orderDoc, dateKey, monthKey);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading orders for revenue analysis", e);
                    showProgress(false);
                });
    }

    private void analyzeOrderRevenueFromItems(QueryDocumentSnapshot orderDoc, String dateKey, String monthKey) {
        String orderId = orderDoc.getId();
        String orderItemId = orderDoc.getString("order_item_id");
        Double orderValue = orderDoc.getDouble("order_value");
        Double shippingFee = orderDoc.getDouble("shipping_fee");

        // Tạo final variables cho lambda
        final String finalOrderId = orderId;
        final String finalOrderItemId = orderItemId;
        final Double finalOrderValue = orderValue;
        final Double finalShippingFee = shippingFee;
        final String finalDateKey = dateKey;
        final String finalMonthKey = monthKey;

        Log.d(TAG, String.format("=== ANALYZING ORDER REVENUE FROM ITEMS ==="));
        Log.d(TAG, String.format("Order ID: %s", finalOrderId));
        Log.d(TAG, String.format("Order Value: %.2f, Shipping: %.2f",
                finalOrderValue != null ? finalOrderValue : 0, finalShippingFee != null ? finalShippingFee : 0));

        if (finalOrderItemId == null) {
            Log.e(TAG, "❌ Order item ID is null for order: " + finalOrderId);

            // Fallback: dùng order_value - shipping_fee
            double fallbackRevenue = calculateFallbackRevenue(finalOrderValue, finalShippingFee);
            updateRevenueData(fallbackRevenue, finalDateKey, finalMonthKey);
            checkAnalysisComplete();
            return;
        }

        db.collection("order_items").document(finalOrderItemId)
                .get()
                .addOnSuccessListener(orderItemDoc -> {
                    if (orderItemDoc.exists()) {
                        List<Map<String, Object>> products = extractProductsFromFields(orderItemDoc);

                        if (products != null && !products.isEmpty()) {
                            calculateOrderRevenueFromProducts(finalOrderId, products, finalOrderValue,
                                    finalShippingFee, finalDateKey, finalMonthKey);
                        } else {
                            // Fallback: dùng order_value - shipping_fee
                            double fallbackRevenue = calculateFallbackRevenue(finalOrderValue, finalShippingFee);
                            updateRevenueData(fallbackRevenue, finalDateKey, finalMonthKey);
                            checkAnalysisComplete();
                        }
                    } else {
                        // Fallback: dùng order_value - shipping_fee
                        double fallbackRevenue = calculateFallbackRevenue(finalOrderValue, finalShippingFee);
                        updateRevenueData(fallbackRevenue, finalDateKey, finalMonthKey);
                        checkAnalysisComplete();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error loading order_items: " + finalOrderItemId, e);

                    // Fallback: dùng order_value - shipping_fee
                    double fallbackRevenue = calculateFallbackRevenue(finalOrderValue, finalShippingFee);
                    updateRevenueData(fallbackRevenue, finalDateKey, finalMonthKey);
                    checkAnalysisComplete();
                });
    }

    private void calculateOrderRevenueFromProducts(String orderId, List<Map<String, Object>> products,
                                                   Double orderValue, Double shippingFee,
                                                   String dateKey, String monthKey) {

        Log.d(TAG, String.format("=== CALCULATING REVENUE FROM PRODUCTS FOR ORDER %s ===", orderId));

        final AtomicReference<Double> orderRevenue = new AtomicReference<>(0.0);
        final AtomicReference<Double> orderFlashsaleRevenue = new AtomicReference<>(0.0);
        final AtomicReference<Double> orderNormalRevenue = new AtomicReference<>(0.0);
        final AtomicInteger processedProducts = new AtomicInteger(0);
        final int totalProducts = products.size();

        // Tạo final variables cho lambda
        final String finalOrderId = orderId;
        final Double finalOrderValue = orderValue;
        final Double finalShippingFee = shippingFee;
        final String finalDateKey = dateKey;
        final String finalMonthKey = monthKey;

        for (Map<String, Object> product : products) {
            String productId = (String) product.get("product_id");
            Object totalCostObj = product.get("total_cost_of_goods");

            if (productId == null || totalCostObj == null) {
                Log.w(TAG, String.format("❌ Missing data for product: ID=%s, TotalCost=%s", productId, totalCostObj));
                if (processedProducts.incrementAndGet() >= totalProducts) {
                    finalizeOrderRevenue(finalOrderId, orderRevenue.get(), orderFlashsaleRevenue.get(),
                            orderNormalRevenue.get(), finalOrderValue, finalShippingFee,
                            finalDateKey, finalMonthKey);
                }
                continue;
            }

            double totalCostOfGoods = (totalCostObj instanceof Long) ?
                    ((Long) totalCostObj).doubleValue() : (Double) totalCostObj;

            // Cộng vào tổng doanh thu của order
            orderRevenue.updateAndGet(v -> v + totalCostOfGoods);

            // Phân loại flashsale vs normal
            boolean isInFlashsale = isProductInActiveFlashsale(productId);

            if (isInFlashsale) {
                orderFlashsaleRevenue.updateAndGet(v -> v + totalCostOfGoods);
                Log.d(TAG, String.format("✅ FLASHSALE REVENUE: %s = %.2f", productId, totalCostOfGoods));
            } else {
                orderNormalRevenue.updateAndGet(v -> v + totalCostOfGoods);
                Log.d(TAG, String.format("✅ NORMAL REVENUE: %s = %.2f", productId, totalCostOfGoods));
            }

            if (processedProducts.incrementAndGet() >= totalProducts) {
                finalizeOrderRevenue(finalOrderId, orderRevenue.get(), orderFlashsaleRevenue.get(),
                        orderNormalRevenue.get(), finalOrderValue, finalShippingFee,
                        finalDateKey, finalMonthKey);
            }
        }
    }

    private double calculateFallbackRevenue(Double orderValue, Double shippingFee) {
        if (orderValue == null) {
            return 0.0;
        }

        double revenue = orderValue;

        // Trừ shipping fee nếu có
        if (shippingFee != null && shippingFee > 0) {
            revenue -= shippingFee;
            Log.d(TAG, String.format("Fallback revenue: %.2f - %.2f = %.2f",
                    orderValue, shippingFee, revenue));
        }

        return Math.max(revenue, 0); // Đảm bảo không âm
    }

    private void finalizeOrderRevenue(String orderId, double orderRevenue, double orderFlashsaleRevenue,
                                      double orderNormalRevenue, Double orderValue, Double shippingFee,
                                      String dateKey, String monthKey) {

        Log.d(TAG, "=== FINALIZING ORDER REVENUE ===");
        Log.d(TAG, String.format("Order ID: %s", orderId));
        Log.d(TAG, String.format("Revenue from products: %.2f", orderRevenue));
        Log.d(TAG, String.format("Flashsale revenue: %.2f", orderFlashsaleRevenue));
        Log.d(TAG, String.format("Normal revenue: %.2f", orderNormalRevenue));

        // Sử dụng doanh thu từ products làm chính
        double finalRevenue = orderRevenue;

        // Tạo final variables cho lambda
        final double finalFlashsaleRevenue;
        final double finalNormalRevenue;

        // Nếu không có revenue từ products, dùng fallback
        if (orderRevenue <= 0) {
            finalRevenue = calculateFallbackRevenue(orderValue, shippingFee);

            // Phân chia fallback revenue theo tỷ lệ hiện tại
            double totalClassified = flashsaleRevenue.get() + normalRevenue.get();
            if (totalClassified > 0) {
                double flashsaleRatio = flashsaleRevenue.get() / totalClassified;
                finalFlashsaleRevenue = finalRevenue * flashsaleRatio;
                finalNormalRevenue = finalRevenue * (1 - flashsaleRatio);
            } else {
                // Mặc định 30% flashsale, 70% normal
                finalFlashsaleRevenue = finalRevenue * 0.3;
                finalNormalRevenue = finalRevenue * 0.7;
            }

            Log.w(TAG, String.format("Using fallback revenue: %.2f", finalRevenue));
        } else {
            // Sử dụng giá trị từ products
            finalFlashsaleRevenue = orderFlashsaleRevenue;
            finalNormalRevenue = orderNormalRevenue;
        }

        // Cập nhật dữ liệu global
        updateRevenueData(finalRevenue, dateKey, monthKey);

        // Cập nhật phân loại flashsale/normal với final variables
        flashsaleRevenue.updateAndGet(v -> v + finalFlashsaleRevenue);
        normalRevenue.updateAndGet(v -> v + finalNormalRevenue);

        Log.d(TAG, String.format("Final revenue used: %.2f", finalRevenue));
        Log.d(TAG, String.format("Final flashsale revenue: %.2f", finalFlashsaleRevenue));
        Log.d(TAG, String.format("Final normal revenue: %.2f", finalNormalRevenue));

        checkAnalysisComplete();
    }


    private void updateRevenueData(double revenue, String dateKey, String monthKey) {
        // Cập nhật tổng doanh thu
        totalRevenue.updateAndGet(v -> v + revenue);

        // Cập nhật dữ liệu theo ngày
        if (dateKey != null) {
            dailyRevenueData.merge(dateKey, revenue, Double::sum);

            // Track best and worst days
            double dayRevenue = dailyRevenueData.get(dateKey);
            if (dayRevenue > bestDayRevenue.get()) {
                bestDayRevenue.set(dayRevenue);
                bestDayDate.set(dateKey);
            }
            if (dayRevenue < worstDayRevenue.get()) {
                worstDayRevenue.set(dayRevenue);
                worstDayDate.set(dateKey);
            }
        }

        // Cập nhật dữ liệu theo tháng
        if (monthKey != null) {
            monthlyRevenueData.merge(monthKey, revenue, Double::sum);
        }
    }

    private List<Map<String, Object>> extractProductsFromFields(com.google.firebase.firestore.DocumentSnapshot orderItemDoc) {
        List<Map<String, Object>> products = new ArrayList<>();
        Map<String, Object> data = orderItemDoc.getData();

        if (data == null) {
            Log.w(TAG, "Order_items document has no data");
            return products;
        }

        Log.d(TAG, "=== EXTRACTING PRODUCTS FROM FIELDS ===");

        // Tìm tất cả fields có pattern "product" + số
        for (String fieldName : data.keySet()) {
            if (fieldName.startsWith("product") && fieldName.length() > 7) {
                try {
                    Object productObj = data.get(fieldName);

                    if (productObj instanceof Map) {
                        Map<String, Object> productMap = (Map<String, Object>) productObj;

                        // Validate required fields
                        String productId = (String) productMap.get("product_id");
                        Object quantityObj = productMap.get("quantity");
                        Object totalCostObj = productMap.get("total_cost_of_goods");

                        Log.d(TAG, String.format("Found product field: %s", fieldName));
                        Log.d(TAG, String.format("  product_id: %s", productId));
                        Log.d(TAG, String.format("  quantity: %s", quantityObj));
                        Log.d(TAG, String.format("  total_cost_of_goods: %s", totalCostObj));

                        if (productId != null && quantityObj != null && totalCostObj != null) {
                            products.add(productMap);
                            Log.d(TAG, String.format("✅ Added product: %s", productId));
                        } else {
                            Log.w(TAG, String.format("❌ Incomplete product data in field: %s", fieldName));
                        }
                    } else {
                        Log.w(TAG, String.format("Field %s is not a Map: %s", fieldName, productObj));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing field: " + fieldName, e);
                }
            }
        }

        Log.d(TAG, String.format("Extracted %d valid products", products.size()));
        return products;
    }

    private boolean isProductInActiveFlashsale(String productId) {
        FlashsaleInfo flashsaleInfo = flashsaleCache.get(productId);
        if (flashsaleInfo == null) {
            Log.d(TAG, String.format("❌ Product %s NOT found in flashsale cache", productId));
            return false;
        }

        long currentTime = System.currentTimeMillis();
        boolean isActive = currentTime >= flashsaleInfo.startTime && currentTime <= flashsaleInfo.endTime;

        Log.d(TAG, String.format("🔥 FLASHSALE CHECK for %s:", productId));
        Log.d(TAG, String.format("  Current Time: %d (%s)", currentTime, new Date(currentTime).toString()));
        Log.d(TAG, String.format("  Start Time: %d (%s)", flashsaleInfo.startTime, new Date(flashsaleInfo.startTime).toString()));
        Log.d(TAG, String.format("  End Time: %d (%s)", flashsaleInfo.endTime, new Date(flashsaleInfo.endTime).toString()));
        Log.d(TAG, String.format("  Is Active: %s", isActive));
        Log.d(TAG, String.format("  Discount Rate: %.2f%%", flashsaleInfo.discountRate));

        return isActive;
    }

    private void checkAnalysisComplete() {
        int processed = processedOrders.incrementAndGet();
        Log.d(TAG, String.format("Order processing progress: %d/%d", processed, totalOrdersToProcess));

        if (processed >= totalOrdersToProcess) {
            Log.d(TAG, "All orders processed, finalizing analysis");
            finalizeAnalysis();
        }
    }

    private void finalizeAnalysis() {
        Log.d(TAG, "=== FINALIZING COMPREHENSIVE REVENUE ANALYSIS ===");

        double totalRevenueValue = totalRevenue.get();
        double flashsaleRevenueValue = flashsaleRevenue.get();
        double normalRevenueValue = normalRevenue.get();
        double calculatedTotalRevenue = flashsaleRevenueValue + normalRevenueValue;
        double avgDailyRevenue = dailyRevenueData.size() > 0 ? totalRevenueValue / dailyRevenueData.size() : 0;

        Log.d(TAG, "🔥 CORRECTED REVENUE CLASSIFICATION RESULTS 🔥");
        Log.d(TAG, String.format("Total Revenue (from total_cost_of_goods): %.2f", totalRevenueValue));
        Log.d(TAG, String.format("Total Revenue (calculated): %.2f", calculatedTotalRevenue));
        Log.d(TAG, String.format("Flashsale Revenue: %.2f (%.1f%%)",
                flashsaleRevenueValue, totalRevenueValue > 0 ? (flashsaleRevenueValue / totalRevenueValue) * 100 : 0));
        Log.d(TAG, String.format("Normal Revenue: %.2f (%.1f%%)",
                normalRevenueValue, totalRevenueValue > 0 ? (normalRevenueValue / totalRevenueValue) * 100 : 0));
        Log.d(TAG, String.format("Average Daily Revenue: %.2f", avgDailyRevenue));
        Log.d(TAG, String.format("Daily Data Points: %d", dailyRevenueData.size()));
        Log.d(TAG, String.format("Monthly Data Points: %d", monthlyRevenueData.size()));

        if (flashsaleRevenueValue == 0 && normalRevenueValue == 0) {
            Log.e(TAG, "❌ CRITICAL: Both flashsale and normal revenues are 0!");
        } else if (flashsaleRevenueValue == 0) {
            Log.w(TAG, "⚠️ WARNING: Flashsale revenue is 0 - no products classified as flashsale");
        } else if (normalRevenueValue == 0) {
            Log.w(TAG, "⚠️ WARNING: Normal revenue is 0 - all products classified as flashsale");
        } else {
            Log.d(TAG, "✅ SUCCESS: Both flashsale and normal revenues calculated");
        }

        Log.d(TAG, "======================================");

        // Analyze voucher impact
        analyzeVoucherImpact(() -> {
            updateUI(totalRevenueValue, avgDailyRevenue);
            generateCharts();
            showProgress(false);
        });
    }

    private void analyzeVoucherImpact(Runnable onComplete) {
        Log.d(TAG, "Analyzing voucher impact on revenue");

        db.collection("vouchers")
                .get()
                .addOnSuccessListener(voucherSnapshot -> {
                    double totalVoucherValue = 0;
                    int activeVouchers = 0;

                    for (QueryDocumentSnapshot voucherDoc : voucherSnapshot) {
                        Double discount = voucherDoc.getDouble("discount");

                        if (discount != null) {
                            totalVoucherValue += discount;
                            activeVouchers++;
                        }
                    }

                    // Tạo final variables cho lambda
                    final double finalTotalVoucherValue = totalVoucherValue;
                    final int finalActiveVouchers = activeVouchers;
                    final double estimatedImpact = finalTotalVoucherValue * finalActiveVouchers * 2; // Estimate usage

                    tvVoucherImpact.setText(String.format("Voucher tiết kiệm %s cho khách hàng (%d voucher)",
                            currencyFormatter.format(estimatedImpact), finalActiveVouchers));

                    Log.d(TAG, String.format("Voucher impact: %.2f from %d vouchers", estimatedImpact, finalActiveVouchers));

                    onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error analyzing voucher impact", e);
                    tvVoucherImpact.setText("Lỗi tải dữ liệu voucher");
                    onComplete.run();
                });
    }

    private void updateUI(double totalRevenueValue, double avgDailyRevenue) {
        Log.d(TAG, "Updating UI with revenue analysis results");

        tvTotalRevenue.setText(currencyFormatter.format(totalRevenueValue));
        tvAvgDailyRevenue.setText(currencyFormatter.format(avgDailyRevenue));
        tvFlashsaleRevenue.setText(currencyFormatter.format(flashsaleRevenue.get()));
        tvNormalRevenue.setText(currencyFormatter.format(normalRevenue.get()));

        if (!bestDayDate.get().isEmpty()) {
            tvBestDay.setText(bestDayDate.get() + ": " + currencyFormatter.format(bestDayRevenue.get()));
        }
        if (!worstDayDate.get().isEmpty() && worstDayRevenue.get() != Double.MAX_VALUE) {
            tvWorstDay.setText(worstDayDate.get() + ": " + currencyFormatter.format(worstDayRevenue.get()));
        }

        Log.d(TAG, "UI updated successfully");
    }

    private void generateCharts() {
        Log.d(TAG, "Generating revenue visualization charts");
        generateDailyChart();
        generateMonthlyChart();
    }

    private void generateDailyChart() {
        if (dailyRevenueData.isEmpty()) {
            Log.w(TAG, "No daily revenue data for chart");
            return;
        }

        Log.d(TAG, "Generating daily revenue chart with " + dailyRevenueData.size() + " data points");

        List<Entry> normalEntries = new ArrayList<>();
        List<Entry> flashsaleEntries = new ArrayList<>();
        List<String> sortedDates = new ArrayList<>(dailyRevenueData.keySet());
        sortedDates.sort(String::compareTo);

        double totalClassified = flashsaleRevenue.get() + normalRevenue.get();
        float flashsaleRatio = totalClassified > 0 ? (float)(flashsaleRevenue.get() / totalClassified) : 0.3f;

        for (int i = 0; i < Math.min(sortedDates.size(), 30); i++) {
            String date = sortedDates.get(i);
            float totalRevenue = dailyRevenueData.get(date).floatValue();

            float flashsaleRevenue = totalRevenue * flashsaleRatio;
            float normalRevenue = totalRevenue * (1 - flashsaleRatio);

            normalEntries.add(new Entry(i, normalRevenue));
            flashsaleEntries.add(new Entry(i, flashsaleRevenue));

            Log.v(TAG, String.format("Daily chart data %d: %s - Total: %.2f, Flashsale: %.2f, Normal: %.2f",
                    i, date, totalRevenue, flashsaleRevenue, normalRevenue));
        }

        if (!normalEntries.isEmpty()) {
            LineDataSet normalDataSet = new LineDataSet(normalEntries, "Doanh thu thường");
            normalDataSet.setColor(getResources().getColor(R.color.colorPrimary));
            normalDataSet.setCircleColor(getResources().getColor(R.color.colorPrimary));
            normalDataSet.setLineWidth(3f);
            normalDataSet.setCircleRadius(4f);
            normalDataSet.setDrawCircleHole(false);
            normalDataSet.setValueTextSize(0f);

            LineDataSet flashsaleDataSet = new LineDataSet(flashsaleEntries, "Doanh thu flashsale");
            flashsaleDataSet.setColor(getResources().getColor(R.color.colorAccent));
            flashsaleDataSet.setCircleColor(getResources().getColor(R.color.colorAccent));
            flashsaleDataSet.setLineWidth(3f);
            flashsaleDataSet.setCircleRadius(4f);
            flashsaleDataSet.setDrawCircleHole(false);
            flashsaleDataSet.setValueTextSize(0f);

            LineData lineData = new LineData(normalDataSet, flashsaleDataSet);
            dailyRevenueChart.setData(lineData);
            dailyRevenueChart.animateX(1500);
            dailyRevenueChart.invalidate();

            Log.d(TAG, "Daily revenue chart generated successfully");
        }
    }

    private void generateMonthlyChart() {
        if (monthlyRevenueData.isEmpty()) {
            Log.w(TAG, "No monthly revenue data for chart");
            return;
        }

        Log.d(TAG, "Generating monthly revenue chart with " + monthlyRevenueData.size() + " data points");

        List<BarEntry> entries = new ArrayList<>();
        List<String> sortedMonths = new ArrayList<>(monthlyRevenueData.keySet());
        sortedMonths.sort(String::compareTo);

        for (int i = 0; i < sortedMonths.size(); i++) {
            String month = sortedMonths.get(i);
            float revenue = monthlyRevenueData.get(month).floatValue();
            entries.add(new BarEntry(i, revenue));

            Log.v(TAG, String.format("Monthly chart data %d: %s = %.2f", i, month, revenue));
        }

        if (!entries.isEmpty()) {
            BarDataSet dataSet = new BarDataSet(entries, "Doanh thu hàng tháng");
            dataSet.setColor(getResources().getColor(R.color.colorAccent));
            dataSet.setValueTextSize(12f);
            dataSet.setValueTextColor(Color.BLACK);

            BarData barData = new BarData(dataSet);
            barData.setBarWidth(0.8f);

            monthlyRevenueChart.setData(barData);
            monthlyRevenueChart.animateY(1500);
            monthlyRevenueChart.invalidate();

            Log.d(TAG, "Monthly revenue chart generated successfully");
        }
    }

    private String extractDateKey(Object orderTimeObj) {
        try {
            if (orderTimeObj instanceof com.google.firebase.Timestamp) {
                Date date = ((com.google.firebase.Timestamp) orderTimeObj).toDate();
                return dateFormat.format(date);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting date", e);
        }
        return null;
    }

    private String extractMonthKey(Object orderTimeObj) {
        try {
            if (orderTimeObj instanceof com.google.firebase.Timestamp) {
                Date date = ((com.google.firebase.Timestamp) orderTimeObj).toDate();
                SimpleDateFormat monthFormat = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
                return monthFormat.format(date);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting month", e);
        }
        return null;
    }

    private void displayNoData() {
        Log.w(TAG, "Displaying no data message");

        tvTotalRevenue.setText("Chưa có dữ liệu");
        tvAvgDailyRevenue.setText("Chưa có dữ liệu");
        tvBestDay.setText("Chưa có dữ liệu");
        tvWorstDay.setText("Chưa có dữ liệu");
        tvFlashsaleRevenue.setText("Chưa có dữ liệu");
        tvNormalRevenue.setText("Chưa có dữ liệu");
        tvVoucherImpact.setText("Chưa có dữ liệu");
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "RevenueReportActivity destroyed");
    }
}
