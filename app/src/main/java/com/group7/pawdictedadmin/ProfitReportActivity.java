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

public class ProfitReportActivity extends AppCompatActivity {

    private static final String TAG = "ProfitReport";

    // UI Components
    private TextView tvTotalProfit, tvAvgDailyProfit, tvProfitMargin, tvBestProfitDay;
    private TextView tvFlashsaleProfit, tvNormalProfit, tvProfitImpact;
    private LineChart dailyProfitChart;
    private BarChart categoryProfitChart;
    private ProgressBar progressBar;
    private ImageView ivBack;

    // Data & Firebase
    private FirebaseFirestore db;
    private NumberFormat currencyFormatter;
    private SimpleDateFormat dateFormat;

    // Profit Data
    private Map<String, Double> dailyProfitData = new HashMap<>();
    private Map<String, Double> categoryProfitData = new HashMap<>();
    private AtomicReference<Double> totalRevenue = new AtomicReference<>(0.0);
    private AtomicReference<Double> totalProfit = new AtomicReference<>(0.0);
    private AtomicReference<Double> flashsaleProfit = new AtomicReference<>(0.0);
    private AtomicReference<Double> normalProfit = new AtomicReference<>(0.0);
    private AtomicReference<Double> profitWithoutFlashsale = new AtomicReference<>(0.0);
    private AtomicReference<String> bestProfitDate = new AtomicReference<>("");
    private AtomicReference<Double> bestProfitAmount = new AtomicReference<>(0.0);

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
        setContentView(R.layout.activity_profit_report);

        initializeComponents();
        loadProfitData();
    }

    private void initializeComponents() {
        Log.d(TAG, "=== INITIALIZING PROFIT REPORT ACTIVITY ===");

        // Initialize views
        tvTotalProfit = findViewById(R.id.tvTotalProfit);
        tvAvgDailyProfit = findViewById(R.id.tvAvgDailyProfit);
        tvProfitMargin = findViewById(R.id.tvProfitMargin);
        tvBestProfitDay = findViewById(R.id.tvBestProfitDay);
        tvFlashsaleProfit = findViewById(R.id.tvFlashsaleProfit);
        tvNormalProfit = findViewById(R.id.tvNormalProfit);
        tvProfitImpact = findViewById(R.id.tvProfitImpact);
        dailyProfitChart = findViewById(R.id.dailyProfitChart);
        categoryProfitChart = findViewById(R.id.categoryProfitChart);
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
        Log.d(TAG, "Setting up charts for profit visualization");

        // Daily Profit Chart
        dailyProfitChart.getDescription().setEnabled(false);
        dailyProfitChart.setTouchEnabled(true);
        dailyProfitChart.setDragEnabled(true);
        dailyProfitChart.setScaleEnabled(true);
        dailyProfitChart.setPinchZoom(true);

        XAxis dailyXAxis = dailyProfitChart.getXAxis();
        dailyXAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        dailyXAxis.setDrawGridLines(false);

        dailyProfitChart.getAxisRight().setEnabled(false);
        dailyProfitChart.getAxisLeft().setDrawGridLines(false);
        dailyProfitChart.getLegend().setEnabled(true);

        // Category Profit Chart
        categoryProfitChart.getDescription().setEnabled(false);
        categoryProfitChart.setTouchEnabled(true);
        categoryProfitChart.setDragEnabled(false);
        categoryProfitChart.setScaleEnabled(false);
        categoryProfitChart.setPinchZoom(false);

        XAxis categoryXAxis = categoryProfitChart.getXAxis();
        categoryXAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        categoryXAxis.setDrawGridLines(false);

        categoryProfitChart.getAxisRight().setEnabled(false);
        categoryProfitChart.getAxisLeft().setDrawGridLines(false);
        categoryProfitChart.getLegend().setEnabled(false);

        Log.d(TAG, "Charts setup completed");
    }

    private void loadProfitData() {
        showProgress(true);
        Log.d(TAG, "=== STARTING PROFIT DATA LOADING ===");

        resetData();

        // Step 1: Load flashsale data
        loadFlashsaleData(() -> {
            // Step 2: Analyze profit
            analyzeProfit();
        });
    }

    private void resetData() {
        Log.d(TAG, "Resetting all profit data");

        totalRevenue.set(0.0);
        totalProfit.set(0.0);
        flashsaleProfit.set(0.0);
        normalProfit.set(0.0);
        profitWithoutFlashsale.set(0.0);
        processedOrders.set(0);
        totalOrdersToProcess = 0;

        flashsaleCache.clear();
        dailyProfitData.clear();
        categoryProfitData.clear();

        bestProfitDate.set("");
        bestProfitAmount.set(0.0);

        Log.d(TAG, "Data reset completed");
    }

    private void loadFlashsaleData(Runnable onComplete) {
        Log.d(TAG, "=== LOADING FLASHSALE DATA FOR PROFIT CALCULATION ===");

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

                        Log.d(TAG, String.format("Processing flashsale: %s (Start: %d, End: %d)",
                                flashsaleName, startTime, endTime));

                        if (products != null && startTime != null && endTime != null) {
                            for (Map<String, Object> product : products) {
                                String productId = (String) product.get("product_id");
                                Object discountRateObj = product.get("discountRate");

                                if (productId != null && discountRateObj != null) {
                                    double discountRate = (discountRateObj instanceof Long) ?
                                            ((Long) discountRateObj).doubleValue() : (Double) discountRateObj;

                                    Log.d(TAG, String.format("Processing flashsale product: %s (Discount: %.2f%%)",
                                            productId, discountRate));

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

                                                        Log.d(TAG, String.format("Cached flashsale info - Product: %s (%.1f%% discount)",
                                                                productId, discountRate));
                                                    }
                                                }

                                                if (processedProducts.incrementAndGet() >= totalProducts.get()) {
                                                    Log.d(TAG, "Flashsale data loading completed. Cached " + flashsaleCache.size() + " products");
                                                    onComplete.run();
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "Error loading product: " + productId, e);
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

    private void analyzeProfit() {
        Log.d(TAG, "=== STARTING PROFIT ANALYSIS ===");

        db.collection("orders")
                .whereEqualTo("order_status", "Completed")
                .get()
                .addOnSuccessListener(orderSnapshot -> {
                    Log.d(TAG, "Found " + orderSnapshot.size() + " completed orders for profit analysis");

                    if (orderSnapshot.isEmpty()) {
                        Log.w(TAG, "No completed orders found");
                        showProgress(false);
                        displayNoData();
                        return;
                    }

                    totalOrdersToProcess = orderSnapshot.size();

                    // Calculate total revenue from order_value
                    double totalOrderValue = 0;
                    for (QueryDocumentSnapshot orderDoc : orderSnapshot) {
                        Double orderValue = orderDoc.getDouble("order_value");
                        if (orderValue != null) {
                            totalOrderValue += orderValue;
                        }
                    }
                    totalRevenue.set(totalOrderValue);

                    Log.d(TAG, String.format("Total revenue calculated: %.2f from %d orders", totalOrderValue, totalOrdersToProcess));

                    // Analyze each order for profit calculation
                    for (QueryDocumentSnapshot orderDoc : orderSnapshot) {
                        analyzeOrderProfit(orderDoc);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading orders for profit analysis", e);
                    showProgress(false);
                });
    }

    private void analyzeOrderProfit(QueryDocumentSnapshot orderDoc) {
        String orderId = orderDoc.getId();
        String orderItemId = orderDoc.getString("order_item_id");
        Object orderTimeObj = orderDoc.get("order_time");
        Double orderValue = orderDoc.getDouble("order_value");

        Log.d(TAG, String.format("=== ANALYZING ORDER PROFIT ==="));
        Log.d(TAG, String.format("Order ID: %s", orderId));
        Log.d(TAG, String.format("Order Value: %.2f", orderValue != null ? orderValue : 0));
        Log.d(TAG, String.format("Order Item ID: %s", orderItemId));

        if (orderItemId == null) {
            Log.e(TAG, "❌ Order item ID is null for order: " + orderId);
            checkAnalysisComplete();
            return;
        }

        final String dateKey = extractDateKey(orderTimeObj);
        Log.d(TAG, String.format("Extracted date key: %s", dateKey));

        db.collection("order_items").document(orderItemId)
                .get()
                .addOnSuccessListener(orderItemDoc -> {
                    if (orderItemDoc.exists()) {
                        Log.d(TAG, "✅ Order_items document exists");

                        // FIX: Đọc products từ fields thay vì array
                        List<Map<String, Object>> products = extractProductsFromFields(orderItemDoc);

                        if (products != null && !products.isEmpty()) {
                            Log.d(TAG, String.format("✅ Found %d products in order %s", products.size(), orderId));
                            calculateOrderProfit(orderId, products, dateKey);
                        } else {
                            Log.e(TAG, "❌ No products found in order_items: " + orderItemId);
                            checkAnalysisComplete();
                        }
                    } else {
                        Log.e(TAG, "❌ Order_items document not found: " + orderItemId);
                        checkAnalysisComplete();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error loading order_items: " + orderItemId, e);
                    checkAnalysisComplete();
                });
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

    private void calculateOrderProfit(String orderId, List<Map<String, Object>> products, String dateKey) {
        Log.d(TAG, String.format("=== CALCULATING PROFIT FOR ORDER %s ===", orderId));
        Log.d(TAG, String.format("Number of products: %d", products.size()));

        final AtomicReference<Double> orderFlashsaleProfit = new AtomicReference<>(0.0);
        final AtomicReference<Double> orderNormalProfit = new AtomicReference<>(0.0);
        final AtomicReference<Double> orderProfitWithoutFlashsale = new AtomicReference<>(0.0);
        final AtomicInteger processedProducts = new AtomicInteger(0);
        final int totalProducts = products.size();

        for (Map<String, Object> product : products) {
            String productId = (String) product.get("product_id");
            Object quantityObj = product.get("quantity");
            Object totalCostObj = product.get("total_cost_of_goods");

            Log.d(TAG, String.format("🔍 Processing product: %s", productId));
            Log.d(TAG, String.format("  Raw quantity: %s (type: %s)", quantityObj, quantityObj.getClass().getSimpleName()));
            Log.d(TAG, String.format("  Raw total_cost: %s (type: %s)", totalCostObj, totalCostObj.getClass().getSimpleName()));

            if (productId == null || quantityObj == null || totalCostObj == null) {
                Log.w(TAG, String.format("❌ Missing data for product: ID=%s, Quantity=%s, TotalCost=%s",
                        productId, quantityObj, totalCostObj));
                if (processedProducts.incrementAndGet() >= totalProducts) {
                    finalizeOrderProfitCalculation(orderId, dateKey, orderFlashsaleProfit.get(),
                            orderNormalProfit.get(), orderProfitWithoutFlashsale.get());
                }
                continue;
            }

            // Convert quantity properly
            int quantity;
            if (quantityObj instanceof Long) {
                quantity = ((Long) quantityObj).intValue();
            } else if (quantityObj instanceof Integer) {
                quantity = (Integer) quantityObj;
            } else if (quantityObj instanceof Double) {
                quantity = ((Double) quantityObj).intValue();
            } else {
                Log.e(TAG, "❌ Cannot convert quantity to int: " + quantityObj);
                if (processedProducts.incrementAndGet() >= totalProducts) {
                    finalizeOrderProfitCalculation(orderId, dateKey, orderFlashsaleProfit.get(),
                            orderNormalProfit.get(), orderProfitWithoutFlashsale.get());
                }
                continue;
            }

            Log.d(TAG, String.format("✅ Product %s validated: Quantity=%d", productId, quantity));

            final String finalProductId = productId;
            final int finalQuantity = quantity;

            // Get product details for profit calculation
            db.collection("products").document(finalProductId)
                    .get()
                    .addOnSuccessListener(productDoc -> {
                        if (productDoc.exists()) {
                            Double price = productDoc.getDouble("price");
                            Double discount = productDoc.getDouble("discount");
                            String productName = productDoc.getString("product_name");

                            Log.d(TAG, String.format("📦 Product details loaded: %s", productName));
                            Log.d(TAG, String.format("  Price: %.2f", price != null ? price : 0));
                            Log.d(TAG, String.format("  Discount: %.2f", discount != null ? discount : 0));

                            if (price != null && price > 0) {
                                if (discount == null) discount = 0.0;

                                // Check flashsale status
                                boolean isInFlashsale = isProductInActiveFlashsale(finalProductId);
                                double flashsaleDiscountRate = isInFlashsale ?
                                        flashsaleCache.get(finalProductId).discountRate : 0;

                                Log.d(TAG, String.format("🔥 Flashsale check: %s (Rate: %.2f%%)", isInFlashsale, flashsaleDiscountRate));

                                // Calculate profit
                                calculateProductProfit(finalProductId, price, discount, finalQuantity,
                                        isInFlashsale, flashsaleDiscountRate,
                                        orderFlashsaleProfit, orderNormalProfit, orderProfitWithoutFlashsale);
                            } else {
                                Log.e(TAG, "❌ Invalid price for product: " + finalProductId + ", price: " + price);
                            }
                        } else {
                            Log.e(TAG, "❌ Product document not found: " + finalProductId);
                        }

                        if (processedProducts.incrementAndGet() >= totalProducts) {
                            finalizeOrderProfitCalculation(orderId, dateKey, orderFlashsaleProfit.get(),
                                    orderNormalProfit.get(), orderProfitWithoutFlashsale.get());
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Error loading product: " + finalProductId, e);
                        if (processedProducts.incrementAndGet() >= totalProducts) {
                            finalizeOrderProfitCalculation(orderId, dateKey, orderFlashsaleProfit.get(),
                                    orderNormalProfit.get(), orderProfitWithoutFlashsale.get());
                        }
                    });
        }
    }

    private boolean isProductInActiveFlashsale(String productId) {
        FlashsaleInfo flashsaleInfo = flashsaleCache.get(productId);
        if (flashsaleInfo == null) {
            Log.d(TAG, String.format("Product %s not found in flashsale cache", productId));
            return false;
        }

        long currentTime = System.currentTimeMillis();
        boolean isActive = currentTime >= flashsaleInfo.startTime && currentTime <= flashsaleInfo.endTime;

        Log.d(TAG, String.format("Flashsale time check for %s - Current: %d, Start: %d, End: %d, Active: %s",
                productId, currentTime, flashsaleInfo.startTime, flashsaleInfo.endTime, isActive));

        return isActive;
    }

    private void calculateProductProfit(String productId, double price, double discount, int quantity,
                                        boolean isInFlashsale, double flashsaleDiscountRate,
                                        AtomicReference<Double> orderFlashsaleProfit,
                                        AtomicReference<Double> orderNormalProfit,
                                        AtomicReference<Double> orderProfitWithoutFlashsale) {

        Log.d(TAG, "=== CALCULATING PROFIT FOR PRODUCT ===");
        Log.d(TAG, String.format("Product ID: %s", productId));
        Log.d(TAG, String.format("Input - Price: %.2f, Discount: %.2f, Quantity: %d", price, discount, quantity));
        Log.d(TAG, String.format("Input - IsInFlashsale: %s, FlashsaleDiscountRate: %.2f%%", isInFlashsale, flashsaleDiscountRate));

        // Step 1: Calculate base profit (30% of price)
        double baseProfit = price * 0.3 * quantity;
        Log.d(TAG, String.format("STEP 1 - Base Profit Calculation:"));
        Log.d(TAG, String.format("  Formula: price × 0.3 × quantity"));
        Log.d(TAG, String.format("  Calculation: %.2f × 0.3 × %d = %.2f", price, quantity, baseProfit));

        // Step 2: Calculate product discount
        double productDiscount = discount * quantity;
        Log.d(TAG, String.format("STEP 2 - Product Discount Calculation:"));
        Log.d(TAG, String.format("  Formula: discount × quantity"));
        Log.d(TAG, String.format("  Calculation: %.2f × %d = %.2f", discount, quantity, productDiscount));

        // Step 3: Calculate flashsale discount
        double flashsaleDiscount = isInFlashsale ? (price * flashsaleDiscountRate / 100 * quantity) : 0;
        Log.d(TAG, String.format("STEP 3 - Flashsale Discount Calculation:"));
        if (isInFlashsale) {
            Log.d(TAG, String.format("  Formula: price × flashsaleDiscountRate / 100 × quantity"));
            Log.d(TAG, String.format("  Calculation: %.2f × %.2f / 100 × %d = %.2f", price, flashsaleDiscountRate, quantity, flashsaleDiscount));
        } else {
            Log.d(TAG, String.format("  Product not in flashsale, flashsale discount = 0"));
        }

        // Step 4: Calculate total discount
        double totalDiscount = productDiscount + flashsaleDiscount;
        Log.d(TAG, String.format("STEP 4 - Total Discount Calculation:"));
        Log.d(TAG, String.format("  Formula: productDiscount + flashsaleDiscount"));
        Log.d(TAG, String.format("  Calculation: %.2f + %.2f = %.2f", productDiscount, flashsaleDiscount, totalDiscount));

        // Step 5: Calculate actual profit
        double actualProfit = baseProfit - totalDiscount;
        Log.d(TAG, String.format("STEP 5 - Actual Profit Calculation:"));
        Log.d(TAG, String.format("  Formula: baseProfit - totalDiscount"));
        Log.d(TAG, String.format("  Calculation: %.2f - %.2f = %.2f", baseProfit, totalDiscount, actualProfit));

        if (actualProfit < 0) {
            Log.w(TAG, String.format("  WARNING: Negative profit! Loss of %.2f", Math.abs(actualProfit)));
        }

        // Step 6: Category classification
        String category = productId.length() >= 2 ? productId.substring(0, 2).toUpperCase() : "XX";
        Log.d(TAG, String.format("STEP 6 - Category Classification:"));
        Log.d(TAG, String.format("  Product ID: %s -> Category: %s (%s)", productId, category, getCategoryName(category)));

        synchronized (categoryProfitData) {
            double oldCategoryProfit = categoryProfitData.getOrDefault(category, 0.0);
            categoryProfitData.merge(category, actualProfit, Double::sum);
            double newCategoryProfit = categoryProfitData.get(category);
            Log.d(TAG, String.format("  Category %s profit: %.2f -> %.2f (added %.2f)",
                    category, oldCategoryProfit, newCategoryProfit, actualProfit));
        }

        // Step 7: Classify profit by flashsale/normal
        Log.d(TAG, String.format("STEP 7 - Profit Classification:"));
        if (isInFlashsale) {
            double oldFlashsaleProfit = orderFlashsaleProfit.get();
            orderFlashsaleProfit.updateAndGet(v -> v + actualProfit);
            Log.d(TAG, String.format("  FLASHSALE PROFIT: %.2f -> %.2f (added %.2f)",
                    oldFlashsaleProfit, orderFlashsaleProfit.get(), actualProfit));
        } else {
            double oldNormalProfit = orderNormalProfit.get();
            orderNormalProfit.updateAndGet(v -> v + actualProfit);
            Log.d(TAG, String.format("  NORMAL PROFIT: %.2f -> %.2f (added %.2f)",
                    oldNormalProfit, orderNormalProfit.get(), actualProfit));
        }

        // Step 8: Calculate profit without flashsale
        Log.d(TAG, String.format("STEP 8 - Profit Without Flashsale Calculation:"));
        if (isInFlashsale) {
            double profitWithoutFlashsaleForProduct = baseProfit - productDiscount;
            Log.d(TAG, String.format("  Formula: baseProfit - productDiscount (no flashsale discount)"));
            Log.d(TAG, String.format("  Calculation: %.2f - %.2f = %.2f", baseProfit, productDiscount, profitWithoutFlashsaleForProduct));

            double oldProfitWithoutFlashsale = orderProfitWithoutFlashsale.get();
            orderProfitWithoutFlashsale.updateAndGet(v -> v + profitWithoutFlashsaleForProduct);
            Log.d(TAG, String.format("  Profit without flashsale: %.2f -> %.2f (added %.2f)",
                    oldProfitWithoutFlashsale, orderProfitWithoutFlashsale.get(), profitWithoutFlashsaleForProduct));
        } else {
            double oldProfitWithoutFlashsale = orderProfitWithoutFlashsale.get();
            orderProfitWithoutFlashsale.updateAndGet(v -> v + actualProfit);
            Log.d(TAG, String.format("  Normal product, profit without flashsale = actual profit"));
            Log.d(TAG, String.format("  Profit without flashsale: %.2f -> %.2f (added %.2f)",
                    oldProfitWithoutFlashsale, orderProfitWithoutFlashsale.get(), actualProfit));
        }

        Log.d(TAG, String.format("=== PRODUCT PROFIT CALCULATION COMPLETE ==="));
        Log.d(TAG, String.format("SUMMARY for %s:", productId));
        Log.d(TAG, String.format("  Base Profit: %.2f", baseProfit));
        Log.d(TAG, String.format("  Product Discount: %.2f", productDiscount));
        Log.d(TAG, String.format("  Flashsale Discount: %.2f", flashsaleDiscount));
        Log.d(TAG, String.format("  Total Discount: %.2f", totalDiscount));
        Log.d(TAG, String.format("  Actual Profit: %.2f", actualProfit));
        Log.d(TAG, String.format("  Category: %s", category));
        Log.d(TAG, String.format("  Classification: %s", isInFlashsale ? "FLASHSALE" : "NORMAL"));
        Log.d(TAG, "============================================");
    }

    private void finalizeOrderProfitCalculation(String orderId, String dateKey,
                                                double orderFlashsaleProfit, double orderNormalProfit,
                                                double orderProfitWithoutFlashsale) {

        Log.d(TAG, "=== FINALIZING ORDER PROFIT CALCULATION ===");
        Log.d(TAG, String.format("Order ID: %s", orderId));
        Log.d(TAG, String.format("Date Key: %s", dateKey));

        // Update global profit metrics
        Log.d(TAG, "STEP 1 - Update Global Flashsale Profit:");
        double oldGlobalFlashsaleProfit = flashsaleProfit.get();
        flashsaleProfit.updateAndGet(v -> v + orderFlashsaleProfit);
        Log.d(TAG, String.format("  Global Flashsale Profit: %.2f -> %.2f (added %.2f)",
                oldGlobalFlashsaleProfit, flashsaleProfit.get(), orderFlashsaleProfit));

        Log.d(TAG, "STEP 2 - Update Global Normal Profit:");
        double oldGlobalNormalProfit = normalProfit.get();
        normalProfit.updateAndGet(v -> v + orderNormalProfit);
        Log.d(TAG, String.format("  Global Normal Profit: %.2f -> %.2f (added %.2f)",
                oldGlobalNormalProfit, normalProfit.get(), orderNormalProfit));

        Log.d(TAG, "STEP 3 - Update Global Total Profit:");
        double orderTotalProfit = orderFlashsaleProfit + orderNormalProfit;
        double oldGlobalTotalProfit = totalProfit.get();
        totalProfit.updateAndGet(v -> v + orderTotalProfit);
        Log.d(TAG, String.format("  Order Total Profit: %.2f + %.2f = %.2f",
                orderFlashsaleProfit, orderNormalProfit, orderTotalProfit));
        Log.d(TAG, String.format("  Global Total Profit: %.2f -> %.2f (added %.2f)",
                oldGlobalTotalProfit, totalProfit.get(), orderTotalProfit));

        Log.d(TAG, "STEP 4 - Update Global Profit Without Flashsale:");
        double oldGlobalProfitWithoutFlashsale = profitWithoutFlashsale.get();
        profitWithoutFlashsale.updateAndGet(v -> v + orderProfitWithoutFlashsale);
        Log.d(TAG, String.format("  Global Profit Without Flashsale: %.2f -> %.2f (added %.2f)",
                oldGlobalProfitWithoutFlashsale, profitWithoutFlashsale.get(), orderProfitWithoutFlashsale));

        // Update daily profit data
        if (dateKey != null) {
            Log.d(TAG, "STEP 5 - Update Daily Profit Data:");
            synchronized (dailyProfitData) {
                double oldDailyProfit = dailyProfitData.getOrDefault(dateKey, 0.0);
                dailyProfitData.merge(dateKey, orderTotalProfit, Double::sum);
                double newDailyProfit = dailyProfitData.get(dateKey);
                Log.d(TAG, String.format("  Daily profit for %s: %.2f -> %.2f (added %.2f)",
                        dateKey, oldDailyProfit, newDailyProfit, orderTotalProfit));

                if (newDailyProfit > bestProfitAmount.get()) {
                    String oldBestDate = bestProfitDate.get();
                    double oldBestAmount = bestProfitAmount.get();
                    bestProfitAmount.set(newDailyProfit);
                    bestProfitDate.set(dateKey);
                    Log.d(TAG, String.format("  NEW BEST PROFIT DAY: %s (%.2f) -> %s (%.2f)",
                            oldBestDate, oldBestAmount, dateKey, newDailyProfit));
                }
            }
        } else {
            Log.w(TAG, "STEP 5 - SKIPPED: No date key for daily profit data");
        }

        Log.d(TAG, "=== ORDER PROFIT CALCULATION SUMMARY ===");
        Log.d(TAG, String.format("Order %s Results:", orderId));
        Log.d(TAG, String.format("  Flashsale Profit: %.2f", orderFlashsaleProfit));
        Log.d(TAG, String.format("  Normal Profit: %.2f", orderNormalProfit));
        Log.d(TAG, String.format("  Total Order Profit: %.2f", orderTotalProfit));
        Log.d(TAG, String.format("  Profit Without Flashsale: %.2f", orderProfitWithoutFlashsale));
        Log.d(TAG, "==========================================");

        checkAnalysisComplete();
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
        Log.d(TAG, "=== FINALIZING COMPREHENSIVE PROFIT ANALYSIS ===");

        double totalProfitValue = totalProfit.get();
        double totalRevenueValue = totalRevenue.get();
        double profitMargin = totalRevenueValue > 0 ? (totalProfitValue / totalRevenueValue) * 100 : 0;
        double avgDailyProfit = dailyProfitData.size() > 0 ? totalProfitValue / dailyProfitData.size() : 0;

        Log.d(TAG, "STEP 1 - Basic Calculations:");
        Log.d(TAG, String.format("  Total Profit: %.2f", totalProfitValue));
        Log.d(TAG, String.format("  Total Revenue: %.2f", totalRevenueValue));
        Log.d(TAG, String.format("  Profit Margin Calculation: (%.2f / %.2f) × 100 = %.2f%%",
                totalProfitValue, totalRevenueValue, profitMargin));
        Log.d(TAG, String.format("  Daily Profit Data Points: %d", dailyProfitData.size()));
        Log.d(TAG, String.format("  Average Daily Profit: %.2f / %d = %.2f",
                totalProfitValue, dailyProfitData.size(), avgDailyProfit));

        // Calculate flashsale impact
        double flashsaleProfitImpact = flashsaleProfit.get() - (profitWithoutFlashsale.get() - normalProfit.get());
        Log.d(TAG, "STEP 2 - Flashsale Impact Calculation:");
        Log.d(TAG, String.format("  Formula: flashsaleProfit - (profitWithoutFlashsale - normalProfit)"));
        Log.d(TAG, String.format("  Calculation: %.2f - (%.2f - %.2f) = %.2f",
                flashsaleProfit.get(), profitWithoutFlashsale.get(), normalProfit.get(), flashsaleProfitImpact));

        Log.d(TAG, "=== FINAL PROFIT ANALYSIS RESULTS ===");
        Log.d(TAG, String.format("Total Revenue: %.2f", totalRevenueValue));
        Log.d(TAG, String.format("Total Profit: %.2f", totalProfitValue));
        Log.d(TAG, String.format("Flashsale Profit: %.2f", flashsaleProfit.get()));
        Log.d(TAG, String.format("Normal Profit: %.2f", normalProfit.get()));
        Log.d(TAG, String.format("Profit Margin: %.2f%%", profitMargin));
        Log.d(TAG, String.format("Profit without Flashsale: %.2f", profitWithoutFlashsale.get()));
        Log.d(TAG, String.format("Flashsale Profit Impact: %.2f", flashsaleProfitImpact));
        Log.d(TAG, String.format("Average Daily Profit: %.2f", avgDailyProfit));
        Log.d(TAG, String.format("Daily Data Points: %d", dailyProfitData.size()));
        Log.d(TAG, String.format("Category Data Points: %d", categoryProfitData.size()));

        // Log category breakdown
        Log.d(TAG, "CATEGORY PROFIT BREAKDOWN:");
        for (Map.Entry<String, Double> entry : categoryProfitData.entrySet()) {
            Log.d(TAG, String.format("  %s (%s): %.2f",
                    entry.getKey(), getCategoryName(entry.getKey()), entry.getValue()));
        }

        Log.d(TAG, "=====================================");

        updateUI(totalProfitValue, avgDailyProfit, profitMargin, flashsaleProfitImpact);
        generateCharts();
        showProgress(false);
    }

    private void updateUI(double totalProfitValue, double avgDailyProfit, double profitMargin, double flashsaleImpact) {
        Log.d(TAG, "Updating UI with profit analysis results");

        tvTotalProfit.setText(currencyFormatter.format(totalProfitValue));
        tvAvgDailyProfit.setText(currencyFormatter.format(avgDailyProfit));
        tvProfitMargin.setText(String.format("%.1f%%", profitMargin));
        tvFlashsaleProfit.setText(currencyFormatter.format(flashsaleProfit.get()));
        tvNormalProfit.setText(currencyFormatter.format(normalProfit.get()));

        double flashsaleImpactPercent = totalProfitValue > 0 ? (flashsaleProfit.get() / totalProfitValue) * 100 : 0;
        tvProfitImpact.setText(String.format("Flashsale đóng góp %.1f%% lợi nhuận (Tác động: %s)",
                flashsaleImpactPercent, currencyFormatter.format(flashsaleImpact)));

        if (!bestProfitDate.get().isEmpty()) {
            tvBestProfitDay.setText(bestProfitDate.get() + ": " + currencyFormatter.format(bestProfitAmount.get()));
        }

        Log.d(TAG, "UI updated successfully");
    }

    private void generateCharts() {
        Log.d(TAG, "Generating profit visualization charts");
        generateDailyChart();
        generateCategoryChart();
    }

    private void generateDailyChart() {
        if (dailyProfitData.isEmpty()) {
            Log.w(TAG, "No daily profit data for chart");
            return;
        }

        Log.d(TAG, "Generating daily profit chart with " + dailyProfitData.size() + " data points");

        List<Entry> entries = new ArrayList<>();
        List<String> sortedDates = new ArrayList<>(dailyProfitData.keySet());
        sortedDates.sort(String::compareTo);

        for (int i = 0; i < Math.min(sortedDates.size(), 30); i++) {
            String date = sortedDates.get(i);
            float profit = dailyProfitData.get(date).floatValue();
            entries.add(new Entry(i, profit));
            Log.v(TAG, String.format("Daily chart data point %d: %s = %.2f", i, date, profit));
        }

        if (!entries.isEmpty()) {
            LineDataSet dataSet = new LineDataSet(entries, "Lợi nhuận hàng ngày");
            dataSet.setColor(getResources().getColor(R.color.success_color));
            dataSet.setCircleColor(getResources().getColor(R.color.success_color));
            dataSet.setLineWidth(3f);
            dataSet.setCircleRadius(4f);
            dataSet.setDrawCircleHole(false);
            dataSet.setValueTextSize(0f);
            dataSet.setDrawFilled(true);
            dataSet.setFillColor(getResources().getColor(R.color.success_color));
            dataSet.setFillAlpha(30);

            LineData lineData = new LineData(dataSet);
            dailyProfitChart.setData(lineData);
            dailyProfitChart.animateX(1500);
            dailyProfitChart.invalidate();

            Log.d(TAG, "Daily profit chart generated successfully");
        }
    }

    private void generateCategoryChart() {
        if (categoryProfitData.isEmpty()) {
            Log.w(TAG, "No category profit data for chart");
            return;
        }

        Log.d(TAG, "Generating category profit chart with " + categoryProfitData.size() + " categories");

        List<BarEntry> entries = new ArrayList<>();
        List<String> categories = new ArrayList<>(categoryProfitData.keySet());
        categories.sort(String::compareTo);

        for (int i = 0; i < categories.size(); i++) {
            String category = categories.get(i);
            float profit = categoryProfitData.get(category).floatValue();
            entries.add(new BarEntry(i, profit));

            Log.d(TAG, String.format("Category chart data: %s (%s) = %.2f",
                    category, getCategoryName(category), profit));
        }

        if (!entries.isEmpty()) {
            BarDataSet dataSet = new BarDataSet(entries, "Lợi nhuận theo danh mục");
            dataSet.setColors(new int[]{
                    getResources().getColor(R.color.success_color),
                    getResources().getColor(R.color.colorPrimary),
                    getResources().getColor(R.color.colorAccent),
                    getResources().getColor(R.color.warning_color),
                    getResources().getColor(android.R.color.holo_blue_bright),
                    getResources().getColor(android.R.color.holo_red_light)
            });
            dataSet.setValueTextSize(12f);
            dataSet.setValueTextColor(Color.BLACK);

            BarData barData = new BarData(dataSet);
            barData.setBarWidth(0.8f);

            categoryProfitChart.setData(barData);
            categoryProfitChart.animateY(1500);
            categoryProfitChart.invalidate();

            Log.d(TAG, "Category profit chart generated successfully");
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

    private void displayNoData() {
        Log.w(TAG, "Displaying no data message");

        tvTotalProfit.setText("Chưa có dữ liệu");
        tvAvgDailyProfit.setText("Chưa có dữ liệu");
        tvProfitMargin.setText("0%");
        tvFlashsaleProfit.setText("Chưa có dữ liệu");
        tvNormalProfit.setText("Chưa có dữ liệu");
        tvProfitImpact.setText("Chưa có dữ liệu");
        tvBestProfitDay.setText("Chưa có dữ liệu");
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ProfitReportActivity destroyed");
    }
}
