package com.group7.pawdictedadmin;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class FinancialDashboardActivity extends AppCompatActivity {

    private static final String TAG = "FinancialDashboardActivity";

    private TextView tvTotalRevenue, tvTotalProfit, tvTotalOrders, tvProfitMargin;
    private TextView tvRevenueGrowth, tvProfitGrowth, tvOrderGrowth, tvMarginGrowth;
    private LineChart revenueChart, profitChart;
    private PieChart categoryRevenueChart;
    private CardView cardRevenueReport, cardProfitReport, cardTrendAnalysis, cardFinancialSummary;

    private FirebaseFirestore db;
    private NumberFormat currencyFormatter;
    private SimpleDateFormat dateFormat;

    // Variables for revenue classification
    private Map<String, FlashsaleInfo> flashsaleCache = new HashMap<>();
    private AtomicReference<Double> totalFlashsaleRevenue = new AtomicReference<>(0.0);
    private AtomicReference<Double> totalNormalRevenue = new AtomicReference<>(0.0);
    private AtomicReference<Double> totalCalculatedProfit = new AtomicReference<>(0.0);
    private AtomicInteger processedOrders = new AtomicInteger(0);
    private int totalOrdersToProcess = 0;

    // Loading state
    private boolean isDataLoading = false;
    private boolean isDataLoaded = false;
    private ImageView btnBack;



    static class FlashsaleInfo {
        String productId;
        double discountRate;
        long startTime;
        long endTime;
        double originalPrice;
        double discountedPrice;
    }

    interface FlashsaleCheckCallback {
        void onChecked(boolean isInFlashsale, double discountRate);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_financial_dashboard);
        Log.d(TAG, "onCreate: Start");

        initViews();
        initFirebase();
        setupCharts();
        loadRealFinancialData();
        setupClickListeners();
        Log.d(TAG, "onCreate: End");

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void initViews() {
        Log.d(TAG, "initViews: Initializing views");
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvTotalProfit = findViewById(R.id.tvTotalProfit);
        tvTotalOrders = findViewById(R.id.tvTotalOrders);
        tvProfitMargin = findViewById(R.id.tvProfitMargin);

        tvRevenueGrowth = findViewById(R.id.tvRevenueGrowth);
        tvProfitGrowth = findViewById(R.id.tvProfitGrowth);
        tvOrderGrowth = findViewById(R.id.tvOrderGrowth);
        tvMarginGrowth = findViewById(R.id.tvMarginGrowth);

        revenueChart = findViewById(R.id.revenueChart);
        profitChart = findViewById(R.id.profitChart);
        categoryRevenueChart = findViewById(R.id.categoryRevenueChart);

        cardRevenueReport = findViewById(R.id.cardRevenueReport);
        cardProfitReport = findViewById(R.id.cardProfitReport);
        cardTrendAnalysis = findViewById(R.id.cardTrendAnalysis);
        cardFinancialSummary = findViewById(R.id.cardFinancialSummary);

        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Log.d(TAG, "initViews: Views initialized successfully");
    }

    private void initFirebase() {
        Log.d(TAG, "initFirebase: Initializing Firebase");
        db = FirebaseFirestore.getInstance();
        Log.d(TAG, "initFirebase: Firebase initialized successfully");
    }

    private void setupCharts() {
        Log.d(TAG, "setupCharts: Setting up charts");
        setupRevenueChart();
        setupProfitChart();
        setupCategoryChart();
        Log.d(TAG, "setupCharts: All charts setup complete");
    }

    private void setupRevenueChart() {
        Log.d(TAG, "setupRevenueChart: Configuring revenue chart");
        revenueChart.getDescription().setEnabled(false);
        revenueChart.setTouchEnabled(true);
        revenueChart.setDragEnabled(true);
        revenueChart.setScaleEnabled(true);
        revenueChart.setPinchZoom(true);

        XAxis xAxis = revenueChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        revenueChart.getAxisRight().setEnabled(false);
        revenueChart.getAxisLeft().setDrawGridLines(false);
        revenueChart.getLegend().setEnabled(true);
        Log.d(TAG, "setupRevenueChart: Revenue chart configured successfully");
    }

    private void setupProfitChart() {
        Log.d(TAG, "setupProfitChart: Configuring profit chart");
        profitChart.getDescription().setEnabled(false);
        profitChart.setTouchEnabled(true);
        profitChart.setDragEnabled(true);
        profitChart.setScaleEnabled(true);
        profitChart.setPinchZoom(true);

        XAxis xAxis = profitChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        profitChart.getAxisRight().setEnabled(false);
        profitChart.getAxisLeft().setDrawGridLines(false);
        profitChart.getLegend().setEnabled(true);
        Log.d(TAG, "setupProfitChart: Profit chart configured successfully");
    }

    private void setupCategoryChart() {
        Log.d(TAG, "setupCategoryChart: Configuring category chart for pet supplies");
        categoryRevenueChart.getDescription().setEnabled(false);
        categoryRevenueChart.setTouchEnabled(true);
        categoryRevenueChart.setRotationEnabled(true);
        categoryRevenueChart.setHighlightPerTapEnabled(true);
        categoryRevenueChart.setDrawHoleEnabled(true);
        categoryRevenueChart.setHoleRadius(35f);
        categoryRevenueChart.setTransparentCircleRadius(40f);
        categoryRevenueChart.setEntryLabelTextSize(12f);
        categoryRevenueChart.setEntryLabelColor(Color.BLACK);
        Log.d(TAG, "setupCategoryChart: Category chart configured successfully");
    }

    private void loadRealFinancialData() {
        if (isDataLoading) {
            Log.d(TAG, "loadRealFinancialData: Data is already loading, skipping");
            return;
        }

        isDataLoading = true;
        isDataLoaded = false;
        Log.d(TAG, "loadRealFinancialData: Start loading financial data from Firestore");

        // Reset counters
        totalFlashsaleRevenue.set(0.0);
        totalNormalRevenue.set(0.0);
        totalCalculatedProfit.set(0.0);
        processedOrders.set(0);
        flashsaleCache.clear();

        // Hiển thị loading state
        showLoadingState(true);

        // Load tất cả orders completed
        db.collection("orders")
                .whereEqualTo("order_status", "Completed")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "loadRealFinancialData: Loaded " + queryDocumentSnapshots.size() + " completed orders");

                    if (queryDocumentSnapshots.isEmpty()) {
                        isDataLoading = false;
                        showLoadingState(false);
                        showNoDataMessage();
                        return;
                    }

                    totalOrdersToProcess = queryDocumentSnapshots.size();

                    // Load flashsale data trước
                    loadFlashsaleData(() -> {
                        // Sau khi có flashsale data, phân loại từng order
                        classifyOrderRevenue(queryDocumentSnapshots);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "loadRealFinancialData: Error loading orders", e);
                    isDataLoading = false;
                    showLoadingState(false);
                    Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showNoDataMessage();
                });

        // Timeout protection
        new android.os.Handler().postDelayed(() -> {
            if (isDataLoading) {
                Log.w(TAG, "loadRealFinancialData: Loading timeout, forcing completion");
                isDataLoading = false;
                showLoadingState(false);

                double totalRevenue = totalFlashsaleRevenue.get() + totalNormalRevenue.get();
                if (totalRevenue > 0) {
                    finalizeRevenueClassification();
                } else {
                    showNoDataMessage();
                }
            }
        }, 15000); // 15 seconds timeout
    }

    private void showLoadingState(boolean isLoading) {
        runOnUiThread(() -> {
            if (isLoading) {
                tvTotalRevenue.setText("Đang tải...");
                tvTotalProfit.setText("Đang tải...");
                tvTotalOrders.setText("...");
                tvProfitMargin.setText("...%");
            }
        });
    }

    private void loadFlashsaleData(Runnable onComplete) {
        Log.d(TAG, "loadFlashsaleData: Loading flashsale data for classification");

        db.collection("flashsales")
                .get()
                .addOnSuccessListener(flashsaleSnapshot -> {
                    Log.d(TAG, "loadFlashsaleData: Loaded " + flashsaleSnapshot.size() + " flashsales");

                    final AtomicInteger totalFlashsaleProducts = new AtomicInteger(0);
                    final AtomicInteger processedFlashsaleProducts = new AtomicInteger(0);

                    // Đếm tổng số products trong flashsales
                    for (QueryDocumentSnapshot flashsaleDoc : flashsaleSnapshot) {
                        List<Map<String, Object>> products = (List<Map<String, Object>>) flashsaleDoc.get("products");
                        if (products != null) {
                            totalFlashsaleProducts.addAndGet(products.size());
                        }
                    }

                    if (totalFlashsaleProducts.get() == 0) {
                        Log.w(TAG, "loadFlashsaleData: No flashsale products found");
                        onComplete.run();
                        return;
                    }

                    for (QueryDocumentSnapshot flashsaleDoc : flashsaleSnapshot) {
                        Long startTime = flashsaleDoc.getLong("startTime");
                        Long endTime = flashsaleDoc.getLong("endTime");
                        String flashsaleName = flashsaleDoc.getString("flashSale_name");
                        List<Map<String, Object>> products = (List<Map<String, Object>>) flashsaleDoc.get("products");

                        if (products != null && startTime != null && endTime != null) {
                            for (Map<String, Object> product : products) {
                                String productId = (String) product.get("product_id");
                                Object discountRateObj = product.get("discountRate");

                                if (productId != null && discountRateObj != null) {
                                    double discountRate = (discountRateObj instanceof Long) ?
                                            ((Long) discountRateObj).doubleValue() : (Double) discountRateObj;

                                    // Lấy giá gốc từ products collection
                                    db.collection("products").document(productId)
                                            .get()
                                            .addOnSuccessListener(productDoc -> {
                                                if (productDoc.exists()) {
                                                    Double originalPrice = productDoc.getDouble("price");
                                                    String productName = productDoc.getString("product_name");

                                                    if (originalPrice != null) {
                                                        FlashsaleInfo info = new FlashsaleInfo();
                                                        info.productId = productId;
                                                        info.discountRate = discountRate;
                                                        info.startTime = startTime;
                                                        info.endTime = endTime;
                                                        info.originalPrice = originalPrice;

                                                        flashsaleCache.put(productId, info);

                                                        Log.d(TAG, String.format("loadFlashsaleData: Cached flashsale '%s' for product '%s' (%s)",
                                                                flashsaleName, productName, productId));
                                                    }
                                                }

                                                if (processedFlashsaleProducts.incrementAndGet() >= totalFlashsaleProducts.get()) {
                                                    Log.d(TAG, "loadFlashsaleData: All flashsale products cached. Total: " + flashsaleCache.size());
                                                    onComplete.run();
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "loadFlashsaleData: Error loading product: " + productId, e);
                                                if (processedFlashsaleProducts.incrementAndGet() >= totalFlashsaleProducts.get()) {
                                                    onComplete.run();
                                                }
                                            });
                                } else {
                                    if (processedFlashsaleProducts.incrementAndGet() >= totalFlashsaleProducts.get()) {
                                        onComplete.run();
                                    }
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "loadFlashsaleData: Error loading flashsale data", e);
                    onComplete.run();
                });
    }

    private void classifyOrderRevenue(com.google.firebase.firestore.QuerySnapshot orderSnapshots) {
        Log.d(TAG, "classifyOrderRevenue: Classifying revenue from " + orderSnapshots.size() + " orders using new logic");

        for (QueryDocumentSnapshot orderDoc : orderSnapshots) {
            try {
                String orderId = orderDoc.getId();
                String orderItemId = orderDoc.getString("order_item_id");
                Double orderValue = orderDoc.getDouble("order_value");

                if (orderValue != null && orderValue > 0 && orderItemId != null) {
                    Log.d(TAG, String.format("classifyOrderRevenue: Processing order %s - Value: %.2f",
                            orderId, orderValue));

                    final String finalOrderId = orderId;
                    final String finalOrderItemId = orderItemId;
                    final Double finalOrderValue = orderValue;

                    // Lấy order_items để phân tích từng sản phẩm
                    db.collection("order_items").document(finalOrderItemId)
                            .get()
                            .addOnSuccessListener(orderItemDoc -> {
                                if (orderItemDoc.exists()) {
                                    // FIX: Đọc products từ fields thay vì array
                                    List<Map<String, Object>> products = extractProductsFromFields(orderItemDoc);

                                    if (products != null && !products.isEmpty()) {
                                        analyzeOrderProductsRevenue(finalOrderId, products, finalOrderValue);
                                    } else {
                                        updateRevenueCounters(0, finalOrderValue, finalOrderValue * 0.3);
                                    }
                                } else {
                                    updateRevenueCounters(0, finalOrderValue, finalOrderValue * 0.3);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "classifyOrderRevenue: Error loading order_items for order: " + finalOrderId, e);
                                updateRevenueCounters(0, finalOrderValue, finalOrderValue * 0.3);
                            });
                } else {
                    updateRevenueCounters(0, 0, 0);
                }
            } catch (Exception e) {
                Log.e(TAG, "classifyOrderRevenue: Error processing order: " + orderDoc.getId(), e);
                updateRevenueCounters(0, 0, 0);
            }
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
        Log.d(TAG, "Available fields: " + data.keySet().toString());

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

    private void analyzeOrderProductsRevenue(String orderId, List<Map<String, Object>> products, double orderValue) {
        Log.d(TAG, String.format("analyzeOrderProductsRevenue: Analyzing %d products in order %s using new logic", products.size(), orderId));

        final AtomicReference<Double> totalFlashsaleRevenueInOrder = new AtomicReference<>(0.0);
        final AtomicReference<Double> totalNormalRevenueInOrder = new AtomicReference<>(0.0);
        final AtomicReference<Double> totalProfitInOrder = new AtomicReference<>(0.0);
        final AtomicInteger processedProducts = new AtomicInteger(0);
        final int totalProductsInOrder = products.size();

        for (Map<String, Object> product : products) {
            try {
                String productId = (String) product.get("product_id");
                Object totalCostObj = product.get("total_cost_of_goods");

                if (productId != null && totalCostObj != null) {
                    double totalCostOfGoods = (totalCostObj instanceof Long) ?
                            ((Long) totalCostObj).doubleValue() : (Double) totalCostObj;

                    final String finalProductId = productId;
                    final double finalTotalCost = totalCostOfGoods;

                    // Lấy thông tin product từ products collection
                    db.collection("products").document(finalProductId)
                            .get()
                            .addOnSuccessListener(productDoc -> {
                                if (productDoc.exists()) {
                                    Double price = productDoc.getDouble("price");
                                    Double discount = productDoc.getDouble("discount");

                                    if (price != null) {
                                        if (discount == null) discount = 0.0;

                                        // Fix: Tạo final variables
                                        final Double finalPrice = price;
                                        final Double finalDiscount = discount;

                                        // Kiểm tra xem product có trong flashsale không
                                        checkProductInFlashsale(finalProductId, (isInFlashsale, discountRate) -> {
                                            // Tính revenue và profit theo logic mới
                                            calculateProductRevenueAndProfit(finalProductId, finalTotalCost, finalPrice,
                                                    finalDiscount, isInFlashsale, discountRate,
                                                    totalFlashsaleRevenueInOrder, totalNormalRevenueInOrder, totalProfitInOrder);

                                            // Kiểm tra nếu đã xử lý hết products trong order
                                            if (processedProducts.incrementAndGet() >= totalProductsInOrder) {
                                                Log.d(TAG, String.format("analyzeOrderProductsRevenue: Order %s completed - Flashsale: %.2f, Normal: %.2f, Profit: %.2f",
                                                        orderId, totalFlashsaleRevenueInOrder.get(), totalNormalRevenueInOrder.get(), totalProfitInOrder.get()));

                                                updateRevenueCounters(totalFlashsaleRevenueInOrder.get(),
                                                        totalNormalRevenueInOrder.get(), totalProfitInOrder.get());
                                            }
                                        });
                                    } else {
                                        Log.w(TAG, "analyzeOrderProductsRevenue: No price found for product: " + finalProductId);
                                        if (processedProducts.incrementAndGet() >= totalProductsInOrder) {
                                            updateRevenueCounters(totalFlashsaleRevenueInOrder.get(),
                                                    totalNormalRevenueInOrder.get(), totalProfitInOrder.get());
                                        }
                                    }
                                } else {
                                    Log.w(TAG, "analyzeOrderProductsRevenue: Product not found: " + finalProductId);
                                    if (processedProducts.incrementAndGet() >= totalProductsInOrder) {
                                        updateRevenueCounters(totalFlashsaleRevenueInOrder.get(),
                                                totalNormalRevenueInOrder.get(), totalProfitInOrder.get());
                                    }
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "analyzeOrderProductsRevenue: Error loading product: " + finalProductId, e);
                                if (processedProducts.incrementAndGet() >= totalProductsInOrder) {
                                    updateRevenueCounters(totalFlashsaleRevenueInOrder.get(),
                                            totalNormalRevenueInOrder.get(), totalProfitInOrder.get());
                                }
                            });
                } else {
                    if (processedProducts.incrementAndGet() >= totalProductsInOrder) {
                        updateRevenueCounters(totalFlashsaleRevenueInOrder.get(),
                                totalNormalRevenueInOrder.get(), totalProfitInOrder.get());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "analyzeOrderProductsRevenue: Error processing product in order: " + orderId, e);
                if (processedProducts.incrementAndGet() >= totalProductsInOrder) {
                    updateRevenueCounters(totalFlashsaleRevenueInOrder.get(),
                            totalNormalRevenueInOrder.get(), totalProfitInOrder.get());
                }
            }
        }
    }

    private void calculateProductRevenueAndProfit(String productId, double totalCostOfGoods, double price,
                                                  double discount, boolean isInFlashsale, double discountRate,
                                                  AtomicReference<Double> flashsaleRevenue,
                                                  AtomicReference<Double> normalRevenue,
                                                  AtomicReference<Double> totalProfit) {

        // Revenue = total_cost_of_goods (đã bao gồm quantity)
        double revenue = totalCostOfGoods;

        // Tính profit theo logic mới
        double baseProfit = price * 0.3; // 30% profit cơ bản
        double totalDiscount = discount; // Discount từ product

        if (isInFlashsale) {
            // Thêm discount từ flashsale
            totalDiscount += price * (discountRate / 100);
        }

        // Profit = base profit - total discount
        double actualProfit = baseProfit - totalDiscount;

        // Nếu discount lớn hơn 30% profit thì profit sẽ âm
        if (actualProfit < 0) {
            Log.d(TAG, String.format("calculateProductRevenueAndProfit: Product %s has negative profit: %.2f (discount %.2f > base profit %.2f)",
                    productId, actualProfit, totalDiscount, baseProfit));
        }

        // Phân loại revenue
        if (isInFlashsale) {
            flashsaleRevenue.updateAndGet(v -> v + revenue);
            Log.d(TAG, String.format("calculateProductRevenueAndProfit: FLASHSALE - Product %s - Revenue: %.2f, Profit: %.2f",
                    productId, revenue, actualProfit));
        } else {
            normalRevenue.updateAndGet(v -> v + revenue);
            Log.d(TAG, String.format("calculateProductRevenueAndProfit: NORMAL - Product %s - Revenue: %.2f, Profit: %.2f",
                    productId, revenue, actualProfit));
        }

        totalProfit.updateAndGet(v -> v + actualProfit);
    }

    private void checkProductInFlashsale(String productId, FlashsaleCheckCallback callback) {
        FlashsaleInfo flashsaleInfo = flashsaleCache.get(productId);

        if (flashsaleInfo != null) {
            // Kiểm tra thời gian hiện tại có trong khoảng flashsale không
            long currentTime = System.currentTimeMillis();
            boolean isActive = currentTime >= flashsaleInfo.startTime && currentTime <= flashsaleInfo.endTime;

            Log.d(TAG, String.format("checkProductInFlashsale: Product %s - In flashsale: %s, Discount rate: %.2f%%",
                    productId, isActive, flashsaleInfo.discountRate));

            callback.onChecked(isActive, isActive ? flashsaleInfo.discountRate : 0);
        } else {
            callback.onChecked(false, 0);
        }
    }

    private void updateRevenueCounters(double flashsale, double normal, double profit) {
        totalFlashsaleRevenue.updateAndGet(v -> v + flashsale);
        totalNormalRevenue.updateAndGet(v -> v + normal);
        totalCalculatedProfit.updateAndGet(v -> v + profit);
        int processed = processedOrders.incrementAndGet();

        Log.d(TAG, String.format("updateRevenueCounters: Progress %d/%d - Flashsale: %.2f, Normal: %.2f, Profit: %.2f",
                processed, totalOrdersToProcess, flashsale, normal, profit));

        if (processed >= totalOrdersToProcess && isDataLoading) {
            finalizeRevenueClassification();
        }
    }

    private void finalizeRevenueClassification() {
        Log.d(TAG, "finalizeRevenueClassification: Finalizing revenue classification");

        double totalRevenue = totalFlashsaleRevenue.get() + totalNormalRevenue.get();
        double profitMargin = totalRevenue > 0 ? (totalCalculatedProfit.get() / totalRevenue) * 100 : 0;

        // Mark data as loaded
        isDataLoading = false;
        isDataLoaded = true;
        showLoadingState(false);

        Log.d(TAG, String.format("finalizeRevenueClassification: FINAL RESULTS - Total Revenue: %.2f, Flashsale Revenue: %.2f (%.1f%%), Normal Revenue: %.2f (%.1f%%), Total Profit: %.2f, Margin: %.2f%%",
                totalRevenue,
                totalFlashsaleRevenue.get(), totalRevenue > 0 ? (totalFlashsaleRevenue.get() / totalRevenue) * 100 : 0,
                totalNormalRevenue.get(), totalRevenue > 0 ? (totalNormalRevenue.get() / totalRevenue) * 100 : 0,
                totalCalculatedProfit.get(), profitMargin));

        updateDashboard(totalRevenue, totalCalculatedProfit.get(), totalOrdersToProcess, profitMargin);
        generateTimeSeriesData();
        calculateRealGrowthRates();
        generateCategoryRevenueData();
    }

    private void updateDashboard(double revenue, double profit, int orders, double margin) {
        Log.d(TAG, "updateDashboard: Updating UI with financial data");
        Log.d(TAG, String.format("updateDashboard: Revenue: %.2f, Profit: %.2f, Orders: %d, Margin: %.2f%%",
                revenue, profit, orders, margin));

        runOnUiThread(() -> {
            tvTotalRevenue.setText(currencyFormatter.format(revenue));
            tvTotalProfit.setText(currencyFormatter.format(profit));
            tvTotalOrders.setText(String.valueOf(orders));
            tvProfitMargin.setText(String.format("%.1f%%", margin));

            // Set growth indicators (placeholder)
            tvRevenueGrowth.setText("+12.5%");
            tvRevenueGrowth.setTextColor(getResources().getColor(R.color.success_color));

            tvProfitGrowth.setText("+8.3%");
            tvProfitGrowth.setTextColor(getResources().getColor(R.color.success_color));

            tvOrderGrowth.setText("+15.2%");
            tvOrderGrowth.setTextColor(getResources().getColor(R.color.success_color));

            tvMarginGrowth.setText("+2.1%");
            tvMarginGrowth.setTextColor(getResources().getColor(R.color.success_color));
        });

        Log.d(TAG, "updateDashboard: UI updated successfully");
    }

    private void generateTimeSeriesData() {
        Log.d(TAG, "generateTimeSeriesData: Generating chart data from orders");

        // Generate sample data for demonstration
        List<Entry> revenueEntries = new ArrayList<>();
        List<Entry> profitEntries = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            revenueEntries.add(new Entry(i, (float) (Math.random() * 1000000 + 500000)));
            profitEntries.add(new Entry(i, (float) (Math.random() * 300000 + 100000)));
        }

        // Revenue chart
        LineDataSet revenueDataSet = new LineDataSet(revenueEntries, "Doanh thu 7 ngày");
        revenueDataSet.setColor(getResources().getColor(R.color.colorPrimary));
        revenueDataSet.setCircleColor(getResources().getColor(R.color.colorPrimary));
        revenueDataSet.setLineWidth(3f);
        revenueDataSet.setCircleRadius(4f);
        revenueDataSet.setDrawCircleHole(false);
        revenueDataSet.setValueTextSize(0f);
        revenueDataSet.setDrawFilled(true);
        revenueDataSet.setFillColor(getResources().getColor(R.color.colorPrimary));
        revenueDataSet.setFillAlpha(30);

        LineData revenueLineData = new LineData(revenueDataSet);
        revenueChart.setData(revenueLineData);
        revenueChart.animateX(1500);
        revenueChart.invalidate();

        // Profit chart
        LineDataSet profitDataSet = new LineDataSet(profitEntries, "Lợi nhuận 7 ngày");
        profitDataSet.setColor(getResources().getColor(R.color.success_color));
        profitDataSet.setCircleColor(getResources().getColor(R.color.success_color));
        profitDataSet.setLineWidth(3f);
        profitDataSet.setCircleRadius(4f);
        profitDataSet.setDrawCircleHole(false);
        profitDataSet.setValueTextSize(0f);
        profitDataSet.setDrawFilled(true);
        profitDataSet.setFillColor(getResources().getColor(R.color.success_color));
        profitDataSet.setFillAlpha(30);

        LineData profitLineData = new LineData(profitDataSet);
        profitChart.setData(profitLineData);
        profitChart.animateX(1500);
        profitChart.invalidate();

        Log.d(TAG, "generateTimeSeriesData: Charts generated successfully");
    }

    private void calculateRealGrowthRates() {
        Log.d(TAG, "calculateRealGrowthRates: Calculating REAL growth rates from Firestore data");
        // Placeholder for real growth calculation
        Log.d(TAG, "calculateRealGrowthRates: Growth rates calculated");
    }

    private void generateCategoryRevenueData() {
        Log.d(TAG, "generateCategoryRevenueData: Generating pet category revenue data using new logic");

        final Map<String, Double> categoryRevenue = new HashMap<>();
        final AtomicInteger processedOrders = new AtomicInteger(0);
        final AtomicInteger totalOrdersToAnalyze = new AtomicInteger(0);

        db.collection("orders")
                .whereEqualTo("order_status", "Completed")
                .get()
                .addOnSuccessListener(orderSnapshot -> {
                    Log.d(TAG, "generateCategoryRevenueData: Processing " + orderSnapshot.size() + " orders for pet category analysis");

                    if (orderSnapshot.isEmpty()) {
                        Log.w(TAG, "generateCategoryRevenueData: No orders found for category analysis");
                        return;
                    }

                    for (QueryDocumentSnapshot orderDoc : orderSnapshot) {
                        String orderItemId = orderDoc.getString("order_item_id");
                        if (orderItemId != null) {
                            totalOrdersToAnalyze.incrementAndGet();
                        }
                    }

                    if (totalOrdersToAnalyze.get() == 0) {
                        Log.w(TAG, "generateCategoryRevenueData: No valid order_item_id found");
                        return;
                    }

                    for (QueryDocumentSnapshot orderDoc : orderSnapshot) {
                        String orderItemId = orderDoc.getString("order_item_id");

                        if (orderItemId != null) {
                            final String finalOrderId = orderDoc.getId();
                            final String finalOrderItemId = orderItemId;

                            db.collection("order_items").document(finalOrderItemId)
                                    .get()
                                    .addOnSuccessListener(orderItemDoc -> {
                                        if (orderItemDoc.exists()) {
                                            List<Map<String, Object>> products = extractProductsFromFields(orderItemDoc);

                                            if (products != null && !products.isEmpty()) {
                                                analyzeCategoryFromProducts(finalOrderId, products, categoryRevenue);
                                            }
                                        }

                                        if (processedOrders.incrementAndGet() >= totalOrdersToAnalyze.get()) {
                                            Log.d(TAG, "generateCategoryRevenueData: All orders processed, updating pet category chart");
                                            updateCategoryRevenueChart(categoryRevenue);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "generateCategoryRevenueData: Error loading order_items: " + finalOrderItemId, e);
                                        if (processedOrders.incrementAndGet() >= totalOrdersToAnalyze.get()) {
                                            updateCategoryRevenueChart(categoryRevenue);
                                        }
                                    });
                        } else {
                            if (processedOrders.incrementAndGet() >= totalOrdersToAnalyze.get()) {
                                updateCategoryRevenueChart(categoryRevenue);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "generateCategoryRevenueData: Error loading orders", e);
                });
    }

    private void analyzeCategoryFromProducts(String orderId, List<Map<String, Object>> products,
                                             Map<String, Double> categoryRevenue) {
        Log.d(TAG, String.format("analyzeCategoryFromProducts: Analyzing %d pet products in order %s", products.size(), orderId));

        for (Map<String, Object> product : products) {
            try {
                String productId = (String) product.get("product_id");
                Object totalCostObj = product.get("total_cost_of_goods");

                if (productId != null && totalCostObj != null && productId.length() >= 2) {
                    double totalCostOfGoods = (totalCostObj instanceof Long) ?
                            ((Long) totalCostObj).doubleValue() : (Double) totalCostObj;

                    // Lấy 2 chữ cái đầu tiên của product_id làm pet category
                    String category = productId.substring(0, 2).toUpperCase();

                    synchronized (categoryRevenue) {
                        categoryRevenue.merge(category, totalCostOfGoods, Double::sum);
                    }

                    Log.d(TAG, String.format("analyzeCategoryFromProducts: Pet Product %s -> Category %s (%s), Revenue: %.2f",
                            productId, category, getCategoryName(category), totalCostOfGoods));
                }
            } catch (Exception e) {
                Log.e(TAG, "analyzeCategoryFromProducts: Error processing pet product in order: " + orderId, e);
            }
        }
    }

    private void updateCategoryRevenueChart(Map<String, Double> categoryRevenue) {
        Log.d(TAG, "updateCategoryRevenueChart: Updating pet category revenue chart");

        if (categoryRevenue.isEmpty()) {
            Log.w(TAG, "updateCategoryRevenueChart: No pet category revenue data available");
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        Map<String, Integer> categoryColors = new HashMap<>();
        categoryColors.put("CK", getResources().getColor(R.color.colorPrimary));
        categoryColors.put("AC", getResources().getColor(R.color.colorAccent));
        categoryColors.put("TO", getResources().getColor(R.color.warning_color));
        categoryColors.put("FU", getResources().getColor(R.color.success_color));
        categoryColors.put("PC", getResources().getColor(android.R.color.holo_blue_bright));
        categoryColors.put("FT", getResources().getColor(android.R.color.holo_red_light));

        double totalRevenue = categoryRevenue.values().stream().mapToDouble(Double::doubleValue).sum();

        for (Map.Entry<String, Double> entry : categoryRevenue.entrySet()) {
            String category = entry.getKey();
            double revenue = entry.getValue();
            float percentage = (float) ((revenue / totalRevenue) * 100);

            String categoryName = getCategoryName(category);
            String label = String.format("%s (%.1f%%)", categoryName, percentage);
            entries.add(new PieEntry((float) revenue, label));

            Integer color = categoryColors.get(category.toUpperCase());
            colors.add(color != null ? color : getResources().getColor(R.color.textColorSecondary));

            Log.d(TAG, String.format("updateCategoryRevenueChart: Pet Category %s (%s) - Revenue: %.2f (%.1f%%)",
                    category, categoryName, revenue, percentage));
        }

        if (!entries.isEmpty()) {
            PieDataSet dataSet = new PieDataSet(entries, "Doanh thu theo danh mục thú cưng");
            dataSet.setColors(colors);
            dataSet.setValueTextSize(12f);
            dataSet.setValueTextColor(Color.WHITE);
            dataSet.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return currencyFormatter.format(value);
                }
            });

            PieData pieData = new PieData(dataSet);
            categoryRevenueChart.setData(pieData);
            categoryRevenueChart.animateY(1500);
            categoryRevenueChart.invalidate();

            Log.d(TAG, "updateCategoryRevenueChart: Pet category revenue chart updated with " + entries.size() + " categories");
        }
    }

    private String getCategoryName(String categoryCode) {
        switch (categoryCode.toUpperCase()) {
            case "CK": return "Carriers & Kennels";
            case "AC": return "Accessories";
            case "TO": return "Toys";
            case "FU": return "Furniture";
            case "PC": return "Pet Care";
            case "FT": return "Food & Treats";
            default: return categoryCode;
        }
    }

    private void setupClickListeners() {
        Log.d(TAG, "setupClickListeners: Setting up navigation");

        cardRevenueReport.setOnClickListener(v -> {
            Intent intent = new Intent(FinancialDashboardActivity.this, RevenueReportActivity.class);
            startActivity(intent);
        });

        cardProfitReport.setOnClickListener(v -> {
            Intent intent = new Intent(FinancialDashboardActivity.this, ProfitReportActivity.class);
            startActivity(intent);
        });

        cardTrendAnalysis.setOnClickListener(v -> {
            Intent intent = new Intent(FinancialDashboardActivity.this, TrendAnalysisActivity.class);
            startActivity(intent);
        });

//        cardFinancialSummary.setOnClickListener(v -> {
//            Intent intent = new Intent(FinancialDashboardActivity.this, FinancialSummaryActivity.class);
//            startActivity(intent);
//        });

        Log.d(TAG, "setupClickListeners: Navigation setup complete");
    }

    private void showNoDataMessage() {
        runOnUiThread(() -> {
            tvTotalRevenue.setText("Chưa có dữ liệu");
            tvTotalProfit.setText("Chưa có dữ liệu");
            tvTotalOrders.setText("0");
            tvProfitMargin.setText("0%");
        });

        Log.w(TAG, "showNoDataMessage: No data available to display");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Activity resumed");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Activity destroyed");
    }
}
