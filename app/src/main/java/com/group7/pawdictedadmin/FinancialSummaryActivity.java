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
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
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

public class FinancialSummaryActivity extends AppCompatActivity {

    private static final String TAG = "FinancialSummary";

    // UI Components
    private TextView tvTotalRevenue, tvTotalProfit, tvTotalOrders, tvAvgOrderValue;
    private TextView tvFlashsaleImpact, tvTopCategory, tvProfitMargin, tvBusinessHealth;
    private TextView tvQuarterlyGrowth, tvYearlyProjection, tvCostAnalysis, tvRecommendations;
    private PieChart revenueBreakdownChart, profitBreakdownChart;
    private BarChart quarterlyChart;
    private ProgressBar progressBar;
    private ImageView ivBack;

    // Data & Firebase
    private FirebaseFirestore db;
    private NumberFormat currencyFormatter;
    private SimpleDateFormat dateFormat;

    // Financial Summary Data
    private AtomicReference<Double> totalRevenue = new AtomicReference<>(0.0);
    private AtomicReference<Double> totalProfit = new AtomicReference<>(0.0);
    private AtomicReference<Double> flashsaleRevenue = new AtomicReference<>(0.0);
    private AtomicReference<Double> normalRevenue = new AtomicReference<>(0.0);
    private AtomicReference<Double> flashsaleProfit = new AtomicReference<>(0.0);
    private AtomicReference<Double> normalProfit = new AtomicReference<>(0.0);
    private AtomicReference<Integer> totalOrders = new AtomicReference<>(0);

    // Category and Time Analysis
    private Map<String, Double> categoryRevenueData = new HashMap<>();
    private Map<String, Double> categoryProfitData = new HashMap<>();
    private Map<String, Double> quarterlyRevenueData = new HashMap<>();
    private Map<String, Double> quarterlyProfitData = new HashMap<>();

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
        setContentView(R.layout.activity_financial_summary);

        initializeComponents();
        loadFinancialSummary();
    }

    private void initializeComponents() {
        Log.d(TAG, "=== INITIALIZING FINANCIAL SUMMARY ACTIVITY ===");

        // Initialize views
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvTotalProfit = findViewById(R.id.tvTotalProfit);
        tvTotalOrders = findViewById(R.id.tvTotalOrders);
        tvAvgOrderValue = findViewById(R.id.tvAvgOrderValue);
        tvFlashsaleImpact = findViewById(R.id.tvFlashsaleImpact);
        tvTopCategory = findViewById(R.id.tvTopCategory);
        tvProfitMargin = findViewById(R.id.tvProfitMargin);
        tvBusinessHealth = findViewById(R.id.tvBusinessHealth);
        tvQuarterlyGrowth = findViewById(R.id.tvQuarterlyGrowth);
        tvYearlyProjection = findViewById(R.id.tvYearlyProjection);
        tvCostAnalysis = findViewById(R.id.tvCostAnalysis);
        tvRecommendations = findViewById(R.id.tvRecommendations);

        // 🔥 CRITICAL FIX: Add null checks for charts
        revenueBreakdownChart = findViewById(R.id.revenueBreakdownChart);
        profitBreakdownChart = findViewById(R.id.profitBreakdownChart);
        quarterlyChart = findViewById(R.id.quarterlyChart);
        progressBar = findViewById(R.id.progressBar);
        ivBack = findViewById(R.id.ivBack);

        // ✅ VALIDATE CHART INITIALIZATION
        if (revenueBreakdownChart == null) {
            Log.e(TAG, "❌ CRITICAL: revenueBreakdownChart is NULL! Check layout XML ID.");
            throw new RuntimeException("revenueBreakdownChart not found in layout");
        }

        if (profitBreakdownChart == null) {
            Log.e(TAG, "❌ CRITICAL: profitBreakdownChart is NULL! Check layout XML ID.");
            throw new RuntimeException("profitBreakdownChart not found in layout");
        }

        if (quarterlyChart == null) {
            Log.e(TAG, "❌ CRITICAL: quarterlyChart is NULL! Check layout XML ID.");
            throw new RuntimeException("quarterlyChart not found in layout");
        }

        Log.d(TAG, "✅ All charts initialized successfully");

        // Initialize Firebase and formatters
        db = FirebaseFirestore.getInstance();
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Setup charts AFTER validation
        setupCharts();

        // Setup click listeners
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> finish());
        } else {
            Log.w(TAG, "⚠️ ivBack is null, back button won't work");
        }

        Log.d(TAG, "Components initialized successfully");
    }

    private void setupCharts() {
        Log.d(TAG, "Setting up charts with null safety");

        // Revenue Breakdown Chart
        if (revenueBreakdownChart != null) {
            Log.d(TAG, "Setting up revenue breakdown chart");
            revenueBreakdownChart.getDescription().setEnabled(false);
            revenueBreakdownChart.setTouchEnabled(true);
            revenueBreakdownChart.setRotationEnabled(true);
            revenueBreakdownChart.setHighlightPerTapEnabled(true);
            revenueBreakdownChart.setDrawHoleEnabled(true);
            revenueBreakdownChart.setHoleRadius(35f);
            revenueBreakdownChart.setTransparentCircleRadius(40f);
            Log.d(TAG, "✅ Revenue breakdown chart setup completed");
        } else {
            Log.e(TAG, "❌ Cannot setup revenue breakdown chart - it's null");
        }

        // Profit Breakdown Chart
        if (profitBreakdownChart != null) {
            Log.d(TAG, "Setting up profit breakdown chart");
            profitBreakdownChart.getDescription().setEnabled(false);
            profitBreakdownChart.setTouchEnabled(true);
            profitBreakdownChart.setRotationEnabled(true);
            profitBreakdownChart.setHighlightPerTapEnabled(true);
            profitBreakdownChart.setDrawHoleEnabled(true);
            profitBreakdownChart.setHoleRadius(35f);
            profitBreakdownChart.setTransparentCircleRadius(40f);
            Log.d(TAG, "✅ Profit breakdown chart setup completed");
        } else {
            Log.e(TAG, "❌ Cannot setup profit breakdown chart - it's null");
        }

        // Quarterly Chart
        if (quarterlyChart != null) {
            Log.d(TAG, "Setting up quarterly chart");
            quarterlyChart.getDescription().setEnabled(false);
            quarterlyChart.setTouchEnabled(true);
            quarterlyChart.setDragEnabled(false);
            quarterlyChart.setScaleEnabled(false);
            quarterlyChart.setPinchZoom(false);

            XAxis quarterlyXAxis = quarterlyChart.getXAxis();
            quarterlyXAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            quarterlyXAxis.setDrawGridLines(false);

            quarterlyChart.getAxisRight().setEnabled(false);
            quarterlyChart.getAxisLeft().setDrawGridLines(false);
            quarterlyChart.getLegend().setEnabled(true);
            Log.d(TAG, "✅ Quarterly chart setup completed");
        } else {
            Log.e(TAG, "❌ Cannot setup quarterly chart - it's null");
        }

        Log.d(TAG, "Charts setup completed with null safety");
    }

    private void loadFinancialSummary() {
        showProgress(true);
        Log.d(TAG, "Loading comprehensive financial summary");

        resetData();

        // Step 1: Load flashsale data
        loadFlashsaleData(() -> {
            // Step 2: Analyze comprehensive financial data
            analyzeComprehensiveFinancialData();
        });
    }

    private void resetData() {
        totalRevenue.set(0.0);
        totalProfit.set(0.0);
        flashsaleRevenue.set(0.0);
        normalRevenue.set(0.0);
        flashsaleProfit.set(0.0);
        normalProfit.set(0.0);
        totalOrders.set(0);
        processedOrders.set(0);
        totalOrdersToProcess = 0;

        flashsaleCache.clear();
        categoryRevenueData.clear();
        categoryProfitData.clear();
        quarterlyRevenueData.clear();
        quarterlyProfitData.clear();

        Log.d(TAG, "Data reset");
    }

    private void loadFlashsaleData(Runnable onComplete) {
        Log.d(TAG, "Loading flashsale data for comprehensive analysis");

        db.collection("flashsales")
                .get()
                .addOnSuccessListener(flashsaleSnapshot -> {
                    final AtomicInteger totalProducts = new AtomicInteger(0);
                    final AtomicInteger processedProducts = new AtomicInteger(0);

                    // Count total products
                    for (QueryDocumentSnapshot flashsaleDoc : flashsaleSnapshot) {
                        List<Map<String, Object>> products = (List<Map<String, Object>>) flashsaleDoc.get("products");
                        if (products != null) {
                            totalProducts.addAndGet(products.size());
                        }
                    }

                    if (totalProducts.get() == 0) {
                        onComplete.run();
                        return;
                    }

                    // Process flashsales
                    for (QueryDocumentSnapshot flashsaleDoc : flashsaleSnapshot) {
                        Long startTime = flashsaleDoc.getLong("startTime");
                        Long endTime = flashsaleDoc.getLong("endTime");
                        List<Map<String, Object>> products = (List<Map<String, Object>>) flashsaleDoc.get("products");

                        if (products != null && startTime != null && endTime != null) {
                            for (Map<String, Object> product : products) {
                                String productId = (String) product.get("product_id");
                                Object discountRateObj = product.get("discountRate");

                                if (productId != null && discountRateObj != null) {
                                    double discountRate = (discountRateObj instanceof Long) ?
                                            ((Long) discountRateObj).doubleValue() : (Double) discountRateObj;

                                    db.collection("products").document(productId)
                                            .get()
                                            .addOnSuccessListener(productDoc -> {
                                                if (productDoc.exists()) {
                                                    Double originalPrice = productDoc.getDouble("price");

                                                    if (originalPrice != null) {
                                                        FlashsaleInfo info = new FlashsaleInfo();
                                                        info.productId = productId;
                                                        info.discountRate = discountRate;
                                                        info.startTime = startTime;
                                                        info.endTime = endTime;
                                                        info.originalPrice = originalPrice;

                                                        flashsaleCache.put(productId, info);
                                                    }
                                                }

                                                if (processedProducts.incrementAndGet() >= totalProducts.get()) {
                                                    Log.d(TAG, "Flashsale data loaded: " + flashsaleCache.size() + " products");
                                                    onComplete.run();
                                                }
                                            })
                                            .addOnFailureListener(e -> {
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

    private void analyzeComprehensiveFinancialData() {
        Log.d(TAG, "Analyzing comprehensive financial data");

        db.collection("orders")
                .whereEqualTo("order_status", "Completed")
                .get()
                .addOnSuccessListener(orderSnapshot -> {
                    if (orderSnapshot.isEmpty()) {
                        showProgress(false);
                        displayNoData();
                        return;
                    }

                    totalOrdersToProcess = orderSnapshot.size();
                    totalOrders.set(totalOrdersToProcess);

                    // Calculate total revenue from order_value and group by quarters
                    double totalOrderValue = 0;
                    for (QueryDocumentSnapshot orderDoc : orderSnapshot) {
                        Double orderValue = orderDoc.getDouble("order_value");
                        Object orderTimeObj = orderDoc.get("order_time");

                        if (orderValue != null) {
                            totalOrderValue += orderValue;

                            // Group by quarter
                            String quarterKey = extractQuarterKey(orderTimeObj);
                            if (quarterKey != null) {
                                quarterlyRevenueData.merge(quarterKey, orderValue, Double::sum);
                            }
                        }
                    }
                    totalRevenue.set(totalOrderValue);

                    Log.d(TAG, String.format("Total revenue: %.2f from %d orders", totalOrderValue, totalOrdersToProcess));

                    // Analyze each order for detailed breakdown
                    for (QueryDocumentSnapshot orderDoc : orderSnapshot) {
                        analyzeOrderForSummary(orderDoc);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading orders for financial summary", e);
                    showProgress(false);
                });
    }

    private void analyzeOrderForSummary(QueryDocumentSnapshot orderDoc) {
        String orderId = orderDoc.getId();
        String orderItemId = orderDoc.getString("order_item_id");
        Object orderTimeObj = orderDoc.get("order_time");

        if (orderItemId == null) {
            checkAnalysisComplete();
            return;
        }

        final String quarterKey = extractQuarterKey(orderTimeObj);

        db.collection("order_items").document(orderItemId)
                .get()
                .addOnSuccessListener(orderItemDoc -> {
                    if (orderItemDoc.exists()) {
                        // 🔥 FIX: Use extractProductsFromFields instead of direct array access
                        List<Map<String, Object>> products = extractProductsFromFields(orderItemDoc);

                        if (products != null && !products.isEmpty()) {
                            calculateOrderSummaryMetrics(orderId, products, quarterKey);
                        } else {
                            Log.w(TAG, "No products found in order_items: " + orderItemId);
                            checkAnalysisComplete();
                        }
                    } else {
                        Log.w(TAG, "Order_items document not found: " + orderItemId);
                        checkAnalysisComplete();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading order_items: " + orderItemId, e);
                    checkAnalysisComplete();
                });
    }

    // 🔥 ADD THIS METHOD from other activities
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

    private void calculateOrderSummaryMetrics(String orderId, List<Map<String, Object>> products, String quarterKey) {
        final AtomicReference<Double> orderFlashsaleRevenue = new AtomicReference<>(0.0);
        final AtomicReference<Double> orderNormalRevenue = new AtomicReference<>(0.0);
        final AtomicReference<Double> orderFlashsaleProfit = new AtomicReference<>(0.0);
        final AtomicReference<Double> orderNormalProfit = new AtomicReference<>(0.0);
        final AtomicInteger processedProducts = new AtomicInteger(0);
        final int totalProducts = products.size();

        for (Map<String, Object> product : products) {
            String productId = (String) product.get("product_id");
            Object totalCostObj = product.get("total_cost_of_goods");
            Object quantityObj = product.get("quantity");

            if (productId == null || totalCostObj == null || quantityObj == null) {
                if (processedProducts.incrementAndGet() >= totalProducts) {
                    finalizeOrderSummaryMetrics(orderId, quarterKey, orderFlashsaleRevenue.get(),
                            orderNormalRevenue.get(), orderFlashsaleProfit.get(), orderNormalProfit.get());
                }
                continue;
            }

            double totalCostOfGoods = (totalCostObj instanceof Long) ?
                    ((Long) totalCostObj).doubleValue() : (Double) totalCostObj;
            int quantity = (quantityObj instanceof Long) ?
                    ((Long) quantityObj).intValue() : (Integer) quantityObj;

            final String finalProductId = productId;
            final double finalTotalCost = totalCostOfGoods;
            final int finalQuantity = quantity;

            // Get product details for comprehensive analysis
            db.collection("products").document(finalProductId)
                    .get()
                    .addOnSuccessListener(productDoc -> {
                        if (productDoc.exists()) {
                            Double price = productDoc.getDouble("price");
                            Double discount = productDoc.getDouble("discount");

                            if (price != null && price > 0) {
                                if (discount == null) discount = 0.0;

                                // Check if product is in active flashsale
                                boolean isInFlashsale = isProductInActiveFlashsale(finalProductId);
                                double flashsaleDiscountRate = isInFlashsale ?
                                        flashsaleCache.get(finalProductId).discountRate : 0;

                                // Calculate comprehensive metrics
                                calculateProductSummaryMetrics(finalProductId, finalTotalCost, price, discount,
                                        finalQuantity, isInFlashsale, flashsaleDiscountRate,
                                        orderFlashsaleRevenue, orderNormalRevenue,
                                        orderFlashsaleProfit, orderNormalProfit);
                            }
                        }

                        if (processedProducts.incrementAndGet() >= totalProducts) {
                            finalizeOrderSummaryMetrics(orderId, quarterKey, orderFlashsaleRevenue.get(),
                                    orderNormalRevenue.get(), orderFlashsaleProfit.get(), orderNormalProfit.get());
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (processedProducts.incrementAndGet() >= totalProducts) {
                            finalizeOrderSummaryMetrics(orderId, quarterKey, orderFlashsaleRevenue.get(),
                                    orderNormalRevenue.get(), orderFlashsaleProfit.get(), orderNormalProfit.get());
                        }
                    });
        }
    }

    private boolean isProductInActiveFlashsale(String productId) {
        FlashsaleInfo flashsaleInfo = flashsaleCache.get(productId);
        if (flashsaleInfo == null) return false;

        long currentTime = System.currentTimeMillis();
        return currentTime >= flashsaleInfo.startTime && currentTime <= flashsaleInfo.endTime;
    }

    private void calculateProductSummaryMetrics(String productId, double totalCostOfGoods, double price, double discount,
                                                int quantity, boolean isInFlashsale, double flashsaleDiscountRate,
                                                AtomicReference<Double> orderFlashsaleRevenue, AtomicReference<Double> orderNormalRevenue,
                                                AtomicReference<Double> orderFlashsaleProfit, AtomicReference<Double> orderNormalProfit) {

        // Revenue = total_cost_of_goods
        double revenue = totalCostOfGoods;

        // Profit = 30% of price - discounts
        double baseProfit = price * 0.3 * quantity;
        double productDiscount = discount * quantity;
        double flashsaleDiscount = isInFlashsale ? (price * flashsaleDiscountRate / 100 * quantity) : 0;
        double totalDiscount = productDiscount + flashsaleDiscount;
        double actualProfit = baseProfit - totalDiscount;

        // Category classification based on first 2 characters of product_id
        String category = productId.length() >= 2 ? productId.substring(0, 2).toUpperCase() : "XX";
        synchronized (categoryRevenueData) {
            categoryRevenueData.merge(category, revenue, Double::sum);
        }
        synchronized (categoryProfitData) {
            categoryProfitData.merge(category, actualProfit, Double::sum);
        }

        // Classify revenue and profit
        if (isInFlashsale) {
            orderFlashsaleRevenue.updateAndGet(v -> v + revenue);
            orderFlashsaleProfit.updateAndGet(v -> v + actualProfit);
        } else {
            orderNormalRevenue.updateAndGet(v -> v + revenue);
            orderNormalProfit.updateAndGet(v -> v + actualProfit);
        }

        Log.d(TAG, String.format("Product %s (%s): Revenue=%.2f, Profit=%.2f, InFlashsale=%s",
                productId, category, revenue, actualProfit, isInFlashsale));
    }

    private void finalizeOrderSummaryMetrics(String orderId, String quarterKey,
                                             double orderFlashsaleRevenue, double orderNormalRevenue,
                                             double orderFlashsaleProfit, double orderNormalProfit) {
        // Update global metrics
        flashsaleRevenue.updateAndGet(v -> v + orderFlashsaleRevenue);
        normalRevenue.updateAndGet(v -> v + orderNormalRevenue);
        flashsaleProfit.updateAndGet(v -> v + orderFlashsaleProfit);
        normalProfit.updateAndGet(v -> v + orderNormalProfit);
        totalProfit.updateAndGet(v -> v + orderFlashsaleProfit + orderNormalProfit);

        // Update quarterly profit data
        if (quarterKey != null) {
            double orderTotalProfit = orderFlashsaleProfit + orderNormalProfit;
            synchronized (quarterlyProfitData) {
                quarterlyProfitData.merge(quarterKey, orderTotalProfit, Double::sum);
            }
        }

        Log.d(TAG, String.format("Order %s: FlashsaleRev=%.2f, NormalRev=%.2f, FlashsaleProfit=%.2f, NormalProfit=%.2f",
                orderId, orderFlashsaleRevenue, orderNormalRevenue, orderFlashsaleProfit, orderNormalProfit));

        checkAnalysisComplete();
    }

    private void checkAnalysisComplete() {
        int processed = processedOrders.incrementAndGet();

        if (processed >= totalOrdersToProcess) {
            finalizeComprehensiveAnalysis();
        }
    }

    private void finalizeComprehensiveAnalysis() {
        Log.d(TAG, "Finalizing comprehensive financial analysis");

        double totalRevenueValue = totalRevenue.get();
        double totalProfitValue = totalProfit.get();
        double profitMargin = totalRevenueValue > 0 ? (totalProfitValue / totalRevenueValue) * 100 : 0;
        double avgOrderValue = totalOrders.get() > 0 ? totalRevenueValue / totalOrders.get() : 0;

        // Calculate flashsale impact
        double flashsaleImpact = flashsaleRevenue.get() > 0 ?
                (flashsaleRevenue.get() / totalRevenueValue) * 100 : 0;

        // Find top category
        String topCategory = findTopCategory();

        Log.d(TAG, "=== COMPREHENSIVE FINANCIAL SUMMARY ===");
        Log.d(TAG, String.format("Total Revenue: %.2f", totalRevenueValue));
        Log.d(TAG, String.format("Total Profit: %.2f", totalProfitValue));
        Log.d(TAG, String.format("Total Orders: %d", totalOrders.get()));
        Log.d(TAG, String.format("Average Order Value: %.2f", avgOrderValue));
        Log.d(TAG, String.format("Profit Margin: %.2f%%", profitMargin));
        Log.d(TAG, String.format("Flashsale Impact: %.2f%%", flashsaleImpact));
        Log.d(TAG, String.format("Top Category: %s", topCategory));
        Log.d(TAG, String.format("Category Data Points: %d", categoryRevenueData.size()));
        Log.d(TAG, String.format("Quarterly Data Points: %d", quarterlyRevenueData.size()));
        Log.d(TAG, "======================================");

        updateComprehensiveUI(totalRevenueValue, totalProfitValue, avgOrderValue, profitMargin,
                flashsaleImpact, topCategory);
        generateComprehensiveCharts();
        generateBusinessInsights(totalRevenueValue, totalProfitValue, profitMargin, flashsaleImpact);
        showProgress(false);
    }

    private String findTopCategory() {
        String topCategory = "Chưa xác định";
        double maxRevenue = 0;

        for (Map.Entry<String, Double> entry : categoryRevenueData.entrySet()) {
            if (entry.getValue() > maxRevenue) {
                maxRevenue = entry.getValue();
                topCategory = getCategoryName(entry.getKey());
            }
        }

        return topCategory;
    }

    private void updateComprehensiveUI(double totalRevenueValue, double totalProfitValue, double avgOrderValue,
                                       double profitMargin, double flashsaleImpact, String topCategory) {
        tvTotalRevenue.setText(currencyFormatter.format(totalRevenueValue));
        tvTotalProfit.setText(currencyFormatter.format(totalProfitValue));
        tvTotalOrders.setText(String.valueOf(totalOrders.get()));
        tvAvgOrderValue.setText(currencyFormatter.format(avgOrderValue));
        tvProfitMargin.setText(String.format("%.1f%%", profitMargin));
        tvFlashsaleImpact.setText(String.format("%.1f%% doanh thu từ flashsale", flashsaleImpact));
        tvTopCategory.setText("Danh mục hàng đầu: " + topCategory);

        // Business health assessment
        String healthStatus = assessBusinessHealth(profitMargin, flashsaleImpact);
        tvBusinessHealth.setText(healthStatus);

        Log.d(TAG, "Comprehensive UI updated successfully");
    }

    private String assessBusinessHealth(double profitMargin, double flashsaleImpact) {
        if (profitMargin > 25 && flashsaleImpact < 30) {
            return "🟢 Tình hình kinh doanh tốt - Lợi nhuận ổn định";
        } else if (profitMargin > 15 && flashsaleImpact < 50) {
            return "🟡 Tình hình kinh doanh khá - Cần tối ưu hóa";
        } else if (profitMargin > 5) {
            return "🟠 Tình hình kinh doanh trung bình - Cần cải thiện";
        } else {
            return "🔴 Cần xem xét lại chiến lược kinh doanh";
        }
    }

    private void generateComprehensiveCharts() {
        generateRevenueBreakdownChart();
        generateProfitBreakdownChart();
        generateQuarterlyChart();
    }

    private void generateRevenueBreakdownChart() {
        if (revenueBreakdownChart == null) {
            Log.e(TAG, "❌ Cannot generate revenue breakdown chart - chart is null");
            return;
        }

        Log.d(TAG, "Generating revenue breakdown chart");

        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(flashsaleRevenue.get().floatValue(), "Flashsale"));
        entries.add(new PieEntry(normalRevenue.get().floatValue(), "Thường"));

        PieDataSet dataSet = new PieDataSet(entries, "Phân tích doanh thu");
        dataSet.setColors(new int[]{
                getResources().getColor(R.color.colorAccent),
                getResources().getColor(R.color.colorPrimary)
        });
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData pieData = new PieData(dataSet);
        revenueBreakdownChart.setData(pieData);
        revenueBreakdownChart.animateY(1500);
        revenueBreakdownChart.invalidate();

        Log.d(TAG, "✅ Revenue breakdown chart generated successfully");
    }

    private void generateProfitBreakdownChart() {
        if (profitBreakdownChart == null) {
            Log.e(TAG, "❌ Cannot generate profit breakdown chart - chart is null");
            return;
        }

        if (categoryProfitData.isEmpty()) {
            Log.w(TAG, "No category profit data for breakdown chart");
            return;
        }

        Log.d(TAG, "Generating profit breakdown chart");

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        Map<String, Integer> categoryColors = new HashMap<>();
        categoryColors.put("CK", getResources().getColor(R.color.colorPrimary));
        categoryColors.put("AC", getResources().getColor(R.color.colorAccent));
        categoryColors.put("TO", getResources().getColor(R.color.warning_color));
        categoryColors.put("FU", getResources().getColor(R.color.success_color));
        categoryColors.put("PC", getResources().getColor(android.R.color.holo_blue_bright));
        categoryColors.put("FT", getResources().getColor(android.R.color.holo_red_light));

        for (Map.Entry<String, Double> entry : categoryProfitData.entrySet()) {
            String category = entry.getKey();
            double profit = entry.getValue();

            if (profit > 0) { // Only show positive profits
                String categoryName = getCategoryName(category);
                entries.add(new PieEntry((float) profit, categoryName));

                Integer color = categoryColors.get(category.toUpperCase());
                colors.add(color != null ? color : getResources().getColor(R.color.textColorSecondary));
            }
        }

        if (!entries.isEmpty()) {
            PieDataSet dataSet = new PieDataSet(entries, "Lợi nhuận theo danh mục");
            dataSet.setColors(colors);
            dataSet.setValueTextSize(12f);
            dataSet.setValueTextColor(Color.WHITE);

            PieData pieData = new PieData(dataSet);
            profitBreakdownChart.setData(pieData);
            profitBreakdownChart.animateY(1500);
            profitBreakdownChart.invalidate();

            Log.d(TAG, "✅ Profit breakdown chart generated successfully");
        }
    }

    private void generateQuarterlyChart() {
        if (quarterlyChart == null) {
            Log.e(TAG, "❌ Cannot generate quarterly chart - chart is null");
            return;
        }

        if (quarterlyRevenueData.isEmpty()) {
            Log.w(TAG, "No quarterly data for chart");
            return;
        }

        Log.d(TAG, "Generating quarterly chart");

        List<BarEntry> revenueEntries = new ArrayList<>();
        List<BarEntry> profitEntries = new ArrayList<>();
        List<String> sortedQuarters = new ArrayList<>(quarterlyRevenueData.keySet());
        sortedQuarters.sort(String::compareTo);

        for (int i = 0; i < sortedQuarters.size(); i++) {
            String quarter = sortedQuarters.get(i);
            float revenue = quarterlyRevenueData.get(quarter).floatValue();
            float profit = quarterlyProfitData.getOrDefault(quarter, 0.0).floatValue();

            revenueEntries.add(new BarEntry(i, revenue));
            profitEntries.add(new BarEntry(i + 0.4f, profit)); // Offset for grouped bars
        }

        if (!revenueEntries.isEmpty()) {
            BarDataSet revenueDataSet = new BarDataSet(revenueEntries, "Doanh thu");
            revenueDataSet.setColor(getResources().getColor(R.color.colorPrimary));
            revenueDataSet.setValueTextSize(10f);

            BarDataSet profitDataSet = new BarDataSet(profitEntries, "Lợi nhuận");
            profitDataSet.setColor(getResources().getColor(R.color.success_color));
            profitDataSet.setValueTextSize(10f);

            BarData barData = new BarData(revenueDataSet, profitDataSet);
            barData.setBarWidth(0.35f);

            quarterlyChart.setData(barData);
            quarterlyChart.animateY(1500);
            quarterlyChart.invalidate();

            Log.d(TAG, "✅ Quarterly chart generated successfully");
        }
    }

    private void generateBusinessInsights(double totalRevenue, double totalProfit, double profitMargin, double flashsaleImpact) {
        // Calculate quarterly growth
        calculateQuarterlyGrowth();

        // Generate yearly projection
        generateYearlyProjection(totalRevenue, totalProfit);

        // Analyze costs
        analyzeCosts(totalRevenue, totalProfit);

        // Generate recommendations
        generateRecommendations(profitMargin, flashsaleImpact);
    }

    private void calculateQuarterlyGrowth() {
        if (quarterlyRevenueData.size() >= 2) {
            List<String> sortedQuarters = new ArrayList<>(quarterlyRevenueData.keySet());
            sortedQuarters.sort(String::compareTo);

            String currentQuarter = sortedQuarters.get(sortedQuarters.size() - 1);
            String previousQuarter = sortedQuarters.get(sortedQuarters.size() - 2);

            double currentRevenue = quarterlyRevenueData.get(currentQuarter);
            double previousRevenue = quarterlyRevenueData.get(previousQuarter);

            double growth = previousRevenue > 0 ? ((currentRevenue - previousRevenue) / previousRevenue) * 100 : 0;

            tvQuarterlyGrowth.setText(String.format("Tăng trưởng quý: %+.1f%%", growth));

            int color = growth >= 0 ?
                    getResources().getColor(R.color.success_color) :
                    getResources().getColor(R.color.error_color);
            tvQuarterlyGrowth.setTextColor(color);
        } else {
            tvQuarterlyGrowth.setText("Chưa đủ dữ liệu quý");
        }
    }

    private void generateYearlyProjection(double totalRevenue, double totalProfit) {
        // Simple projection based on current performance
        Calendar now = Calendar.getInstance();
        int currentMonth = now.get(Calendar.MONTH) + 1; // 1-12

        double monthlyAvgRevenue = totalRevenue / currentMonth;
        double yearlyProjectedRevenue = monthlyAvgRevenue * 12;

        double monthlyAvgProfit = totalProfit / currentMonth;
        double yearlyProjectedProfit = monthlyAvgProfit * 12;

        tvYearlyProjection.setText(String.format("Dự báo năm: %s doanh thu, %s lợi nhuận",
                currencyFormatter.format(yearlyProjectedRevenue),
                currencyFormatter.format(yearlyProjectedProfit)));
    }

    private void analyzeCosts(double totalRevenue, double totalProfit) {
        double totalCosts = totalRevenue - totalProfit;
        double costRatio = totalRevenue > 0 ? (totalCosts / totalRevenue) * 100 : 0;

        String costAnalysis;
        if (costRatio < 70) {
            costAnalysis = String.format("✅ Chi phí kiểm soát tốt (%.1f%% doanh thu)", costRatio);
        } else if (costRatio < 80) {
            costAnalysis = String.format("⚠️ Chi phí ở mức trung bình (%.1f%% doanh thu)", costRatio);
        } else {
            costAnalysis = String.format("🔴 Chi phí cao, cần tối ưu (%.1f%% doanh thu)", costRatio);
        }

        tvCostAnalysis.setText(costAnalysis);
    }

    private void generateRecommendations(double profitMargin, double flashsaleImpact) {
        StringBuilder recommendations = new StringBuilder();

        if (profitMargin < 15) {
            recommendations.append("• Cần tăng giá bán hoặc giảm chi phí\n");
        }

        if (flashsaleImpact > 50) {
            recommendations.append("• Quá phụ thuộc flashsale, cần tăng doanh thu thường\n");
        }

        if (categoryRevenueData.size() < 3) {
            recommendations.append("• Nên mở rộng danh mục sản phẩm\n");
        }

        // Find underperforming categories
        String topCategory = findTopCategory();
        recommendations.append("• Tập trung phát triển danh mục: ").append(topCategory).append("\n");

        if (recommendations.length() == 0) {
            recommendations.append("• Duy trì chiến lược hiện tại\n• Tiếp tục theo dõi các chỉ số");
        }

        tvRecommendations.setText(recommendations.toString());
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

    private String extractQuarterKey(Object orderTimeObj) {
        try {
            if (orderTimeObj instanceof com.google.firebase.Timestamp) {
                Date date = ((com.google.firebase.Timestamp) orderTimeObj).toDate();
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);

                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH) + 1; // 1-12
                int quarter = (month - 1) / 3 + 1; // 1-4

                return String.format("Q%d-%d", quarter, year);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting quarter", e);
        }
        return null;
    }

    private void displayNoData() {
        tvTotalRevenue.setText("Chưa có dữ liệu");
        tvTotalProfit.setText("Chưa có dữ liệu");
        tvTotalOrders.setText("0");
        tvAvgOrderValue.setText("Chưa có dữ liệu");
        tvFlashsaleImpact.setText("Chưa có dữ liệu");
        tvTopCategory.setText("Chưa có dữ liệu");
        tvProfitMargin.setText("0%");
        tvBusinessHealth.setText("Chưa có đủ dữ liệu để đánh giá");
        tvQuarterlyGrowth.setText("Chưa có dữ liệu");
        tvYearlyProjection.setText("Chưa có dữ liệu");
        tvCostAnalysis.setText("Chưa có dữ liệu");
        tvRecommendations.setText("Cần có dữ liệu để đưa ra khuyến nghị");

        Log.w(TAG, "No data available for financial summary");
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Activity destroyed");
    }
}
