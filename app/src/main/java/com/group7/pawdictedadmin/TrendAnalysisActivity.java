package com.group7.pawdictedadmin;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.group7.pawdictedadmin.R;

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

public class TrendAnalysisActivity extends AppCompatActivity {
    private static final String TAG = "TrendAnalysisActivity";

    private FirebaseFirestore db;

    // Header
    private ImageView ivBack;

    // Progress Bar
    private ProgressBar progressBar;

    // Growth Indicators
    private TextView tvRevenueGrowth, tvProfitGrowth, tvOrderGrowth, tvMarginGrowth;

    // Current vs Last Month
    private TextView tvCurrentMonthRevenue, tvCurrentMonthProfit, tvCurrentMonthOrders;
    private TextView tvLastMonthRevenue, tvLastMonthProfit, tvLastMonthOrders;

    // Analysis Sections
    private TextView tvTrendSummary;
    private TextView tvQuarterlyGrowth;
    private TextView tvYearlyProjection;
    private TextView tvCostAnalysis;
    private TextView tvRecommendations;

    // Chart
    private LineChart trendChart;

    // Data storage
    private double currentMonthRevenue = 0;
    private double currentMonthProfit = 0;
    private int currentMonthOrders = 0;
    private double lastMonthRevenue = 0;
    private double lastMonthProfit = 0;
    private int lastMonthOrders = 0;

    // Category data storage
    private Map<String, Double> currentMonthCategoryRevenue = new HashMap<>();
    private Map<String, Double> currentMonthCategoryProfit = new HashMap<>();
    private Map<String, Double> lastMonthCategoryRevenue = new HashMap<>();
    private Map<String, Double> lastMonthCategoryProfit = new HashMap<>();

    // Flashsale cache
    private Map<String, FlashsaleInfo> flashsaleCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trend_analysis);

        initViews();
        db = FirebaseFirestore.getInstance();

        setupClickListeners();

        // Gọi loadFlashsaleData với callback
        loadFlashsaleData(() -> {
            // Sau khi load xong flashsale data thì load trend data
            loadTrendData();
        });
    }

    private void initViews() {
        // Header
        ivBack = findViewById(R.id.ivBack);

        // Progress Bar
        progressBar = findViewById(R.id.progressBar);

        // Growth Indicators
        tvRevenueGrowth = findViewById(R.id.tvRevenueGrowth);
        tvProfitGrowth = findViewById(R.id.tvProfitGrowth);
        tvOrderGrowth = findViewById(R.id.tvOrderGrowth);
        tvMarginGrowth = findViewById(R.id.tvMarginGrowth);

        // Current vs Last Month
        tvCurrentMonthRevenue = findViewById(R.id.tvCurrentMonthRevenue);
        tvCurrentMonthProfit = findViewById(R.id.tvCurrentMonthProfit);
        tvCurrentMonthOrders = findViewById(R.id.tvCurrentMonthOrders);
        tvLastMonthRevenue = findViewById(R.id.tvLastMonthRevenue);
        tvLastMonthProfit = findViewById(R.id.tvLastMonthProfit);
        tvLastMonthOrders = findViewById(R.id.tvLastMonthOrders);

        // Analysis Sections
        tvTrendSummary = findViewById(R.id.tvTrendSummary);
        tvQuarterlyGrowth = findViewById(R.id.tvQuarterlyGrowth);
        tvYearlyProjection = findViewById(R.id.tvYearlyProjection);
        tvCostAnalysis = findViewById(R.id.tvCostAnalysis);
        tvRecommendations = findViewById(R.id.tvRecommendations);

        // Chart
        trendChart = findViewById(R.id.trendChart);

        setupChart();
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());
    }

    private void setupChart() {
        trendChart.getDescription().setEnabled(false);
        trendChart.setTouchEnabled(true);
        trendChart.setDragEnabled(true);
        trendChart.setScaleEnabled(true);
        trendChart.setPinchZoom(true);

        XAxis xAxis = trendChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis leftAxis = trendChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);

        trendChart.getAxisRight().setEnabled(false);
        trendChart.getLegend().setEnabled(true);
    }

    private void loadFlashsaleData(Runnable onComplete) {
        Log.d(TAG, "loadFlashsaleData: Loading flashsale data for classification");
        showLoading(true);

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
                                                        info.flashsaleName = flashsaleName;

                                                        flashsaleCache.put(productId, info);

                                                        Log.d(TAG, String.format("loadFlashsaleData: Cached flashsale '%s' for product '%s' (%s) - %.2f%% discount",
                                                                flashsaleName, productName, productId, discountRate));
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
                    showLoading(false);
                    Toast.makeText(this, "Lỗi tải dữ liệu flashsale: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    onComplete.run();
                });
    }

    private void loadTrendData() {
        Log.d(TAG, "Starting trend analysis...");

        // Debug flashsale cache trước khi bắt đầu
        debugFlashsaleCache();

        Calendar cal = Calendar.getInstance();

        // Current month dates
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date currentMonthStart = cal.getTime();

        cal.add(Calendar.MONTH, 1);
        Date currentMonthEnd = cal.getTime();

        // Last month dates
        cal.add(Calendar.MONTH, -2);
        Date lastMonthStart = cal.getTime();

        cal.add(Calendar.MONTH, 1);
        Date lastMonthEnd = cal.getTime();

        Log.d(TAG, String.format("Date ranges - Current: %s to %s, Last: %s to %s",
                currentMonthStart, currentMonthEnd, lastMonthStart, lastMonthEnd));

        // Analyze both months
        analyzeMonthData(currentMonthStart, currentMonthEnd, true, () -> {
            analyzeMonthData(lastMonthStart, lastMonthEnd, false, () -> {
                runOnUiThread(() -> {
                    updateUI();
                    showLoading(false);
                });
            });
        });
    }

    private void analyzeMonthData(Date startDate, Date endDate, boolean isCurrentMonth, Runnable onComplete) {
        String monthLabel = isCurrentMonth ? "current" : "last";
        Log.d(TAG, String.format("Analyzing %s month data with Pawdicted profit logic", monthLabel));

        db.collection("orders")
                .whereEqualTo("order_status", "Completed")
                .whereGreaterThanOrEqualTo("order_time", startDate)
                .whereLessThan("order_time", endDate)
                .get()
                .addOnSuccessListener(orderSnapshot -> {
                    if (orderSnapshot.isEmpty()) {
                        Log.w(TAG, "No orders found for " + monthLabel + " month");
                        onComplete.run();
                        return;
                    }

                    final AtomicReference<Double> monthRevenue = new AtomicReference<>(0.0);
                    final AtomicReference<Double> monthProfit = new AtomicReference<>(0.0);
                    final AtomicInteger monthOrders = new AtomicInteger(0);
                    final AtomicInteger processedOrders = new AtomicInteger(0);
                    final int totalOrders = orderSnapshot.size();

                    // Khởi tạo category tracking
                    final Map<String, Double> categoryRevenue = new HashMap<>();
                    final Map<String, Double> categoryProfit = new HashMap<>();

                    // Tính doanh thu từ order_value (theo logic Pawdicted)
                    for (QueryDocumentSnapshot orderDoc : orderSnapshot) {
                        Double orderValue = orderDoc.getDouble("order_value");
                        if (orderValue != null) {
                            monthRevenue.updateAndGet(v -> v + orderValue);
                            monthOrders.incrementAndGet();
                        }
                    }

                    Log.d(TAG, String.format("%s month - Total orders: %d, Revenue from order_value: %.2f",
                            monthLabel, monthOrders.get(), monthRevenue.get()));

                    // Tính lợi nhuận chi tiết cho từng order
                    for (QueryDocumentSnapshot orderDoc : orderSnapshot) {
                        String orderItemId = orderDoc.getString("order_item_id");

                        if (orderItemId != null) {
                            calculateOrderProfitWithCategoryAndRevenue(orderItemId, monthProfit,
                                    categoryProfit, categoryRevenue, () -> {
                                        if (processedOrders.incrementAndGet() >= totalOrders) {
                                            // Sử dụng method updateMonthDataWithCategories
                                            updateMonthDataWithCategories(isCurrentMonth, monthRevenue.get(),
                                                    monthProfit.get(), monthOrders.get(), categoryRevenue, categoryProfit);

                                            Log.d(TAG, String.format("%s month Pawdicted results: Revenue=%.2f, Profit=%.2f, Orders=%d, Margin=%.2f%%",
                                                    monthLabel, monthRevenue.get(), monthProfit.get(), monthOrders.get(),
                                                    monthRevenue.get() > 0 ? (monthProfit.get() / monthRevenue.get() * 100) : 0));

                                            onComplete.run();
                                        }
                                    });
                        } else {
                            if (processedOrders.incrementAndGet() >= totalOrders) {
                                updateMonthDataWithCategories(isCurrentMonth, monthRevenue.get(),
                                        monthProfit.get(), monthOrders.get(), categoryRevenue, categoryProfit);
                                onComplete.run();
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error analyzing " + monthLabel + " month data", e);
                    onComplete.run();
                });
    }

    private void calculateOrderProfitWithCategoryAndRevenue(String orderItemId,
                                                            AtomicReference<Double> monthProfit,
                                                            Map<String, Double> categoryProfit,
                                                            Map<String, Double> categoryRevenue,
                                                            Runnable onComplete) {

        db.collection("order_items").document(orderItemId)
                .get()
                .addOnSuccessListener(orderItemDoc -> {
                    if (orderItemDoc.exists()) {
                        List<Map<String, Object>> products = extractProductsFromFields(orderItemDoc);

                        if (products != null && !products.isEmpty()) {
                            final AtomicReference<Double> orderProfit = new AtomicReference<>(0.0);
                            final AtomicInteger processedProducts = new AtomicInteger(0);
                            final int totalProducts = products.size();

                            for (Map<String, Object> product : products) {
                                String productId = (String) product.get("product_id");
                                Object quantityObj = product.get("quantity");
                                Object totalCostObj = product.get("total_cost_of_goods");

                                if (productId != null && quantityObj != null && totalCostObj != null) {
                                    int quantity = convertToInt(quantityObj);
                                    double totalCostOfGoods = convertToDouble(totalCostObj);

                                    // Cập nhật category revenue
                                    updateCategoryRevenue(categoryRevenue, productId, totalCostOfGoods);

                                    calculateProductProfitWithCategory(productId, quantity,
                                            orderProfit, categoryProfit, () -> {
                                                if (processedProducts.incrementAndGet() >= totalProducts) {
                                                    monthProfit.updateAndGet(v -> v + orderProfit.get());
                                                    onComplete.run();
                                                }
                                            });
                                } else {
                                    if (processedProducts.incrementAndGet() >= totalProducts) {
                                        monthProfit.updateAndGet(v -> v + orderProfit.get());
                                        onComplete.run();
                                    }
                                }
                            }
                        } else {
                            onComplete.run();
                        }
                    } else {
                        onComplete.run();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error calculating order profit: " + orderItemId, e);
                    onComplete.run();
                });
    }

    private void calculateProductProfitWithCategory(String productId, int quantity,
                                                    AtomicReference<Double> orderProfit,
                                                    Map<String, Double> categoryProfit,
                                                    Runnable onComplete) {

        db.collection("products").document(productId)
                .get()
                .addOnSuccessListener(productDoc -> {
                    if (productDoc.exists()) {
                        Double price = productDoc.getDouble("price");
                        Double discount = productDoc.getDouble("discount");

                        if (price != null && price > 0) {
                            if (discount == null) discount = 0.0;

                            // Tính lợi nhuận theo logic Pawdicted
                            double profit = calculatePawdictedProfitLogic(productId, price, discount, quantity);

                            // Cập nhật order profit
                            orderProfit.updateAndGet(v -> v + profit);

                            // Cập nhật category profit
                            updateCategoryProfit(categoryProfit, productId, profit);

                            Log.d(TAG, String.format("Product %s profit: %.2f added to order and category",
                                    productId, profit));
                        }
                    }
                    onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting product: " + productId, e);
                    onComplete.run();
                });
    }

    private double calculatePawdictedProfitLogic(String productId, double price,
                                                 double productDiscount, int quantity) {

        Log.d(TAG, "=== PAWDICTED PROFIT CALCULATION ===");
        Log.d(TAG, String.format("Input - Product: %s, Price: %.2f, Discount: %.2f, Quantity: %d",
                productId, price, productDiscount, quantity));

        // STEP 1: Tính lợi nhuận cơ bản (30% của giá)
        double baseProfit = price * 0.3 * quantity;
        Log.d(TAG, String.format("STEP 1 - Base Profit: %.2f × 0.3 × %d = %.2f",
                price, quantity, baseProfit));

        // STEP 2: Tính discount từ sản phẩm
        double productDiscountAmount = productDiscount * quantity;
        Log.d(TAG, String.format("STEP 2 - Product Discount: %.2f × %d = %.2f",
                productDiscount, quantity, productDiscountAmount));

        // STEP 3: Kiểm tra flashsale với logging chi tiết
        Log.d(TAG, String.format("STEP 3 - Checking flashsale for product: %s", productId));
        FlashsaleInfo flashsaleInfo = getActiveFlashsaleForProduct(productId);
        double flashsaleDiscountAmount = 0.0;

        if (flashsaleInfo != null) {
            // Sử dụng originalPrice từ flashsale hoặc price từ parameter
            double priceForFlashsale = flashsaleInfo.originalPrice > 0 ? flashsaleInfo.originalPrice : price;
            flashsaleDiscountAmount = (priceForFlashsale * flashsaleInfo.discountRate / 100) * quantity;

            Log.d(TAG, String.format("STEP 3 - FLASHSALE FOUND! '%s'", flashsaleInfo.flashsaleName));
            Log.d(TAG, String.format("STEP 3 - Product %s: %.2f%% discount on price %.2f",
                    productId, flashsaleInfo.discountRate, priceForFlashsale));
            Log.d(TAG, String.format("STEP 3 - Flashsale Discount Amount: %.2f × %.2f / 100 × %d = %.2f",
                    priceForFlashsale, flashsaleInfo.discountRate, quantity, flashsaleDiscountAmount));
        } else {
            Log.d(TAG, String.format("STEP 3 - NO ACTIVE FLASHSALE found for product: %s", productId));
        }

        // STEP 4: Tính tổng discount
        double totalDiscount = productDiscountAmount + flashsaleDiscountAmount;
        Log.d(TAG, String.format("STEP 4 - Total Discount: %.2f + %.2f = %.2f",
                productDiscountAmount, flashsaleDiscountAmount, totalDiscount));

        // STEP 5: Áp dụng logic Pawdicted
        double finalProfit;
        double costPortion = price * 0.7 * quantity; // 70% là cost

        if (totalDiscount <= baseProfit) {
            // Discount chỉ ảnh hưởng đến 30% lợi nhuận
            finalProfit = baseProfit - totalDiscount;
            Log.d(TAG, String.format("STEP 5a - Discount trong giới hạn profit: %.2f - %.2f = %.2f",
                    baseProfit, totalDiscount, finalProfit));
        } else {
            // Discount vượt quá 30% lợi nhuận, ăn vào cost
            double excessDiscount = totalDiscount - baseProfit;
            finalProfit = -(excessDiscount); // Lợi nhuận âm

            Log.d(TAG, String.format("STEP 5b - Discount vượt quá profit:"));
            Log.d(TAG, String.format("  Excess discount: %.2f - %.2f = %.2f",
                    totalDiscount, baseProfit, excessDiscount));
            Log.d(TAG, String.format("  Final profit (negative): %.2f", finalProfit));
        }

        // STEP 6: Tính tỷ suất lợi nhuận
        double totalRevenue = price * quantity;
        double profitMargin = totalRevenue > 0 ? (finalProfit / totalRevenue) * 100 : 0;

        Log.d(TAG, String.format("=== PROFIT CALCULATION SUMMARY ==="));
        Log.d(TAG, String.format("Product: %s", productId));
        Log.d(TAG, String.format("Base Profit: %.2f", baseProfit));
        Log.d(TAG, String.format("Product Discount: %.2f", productDiscountAmount));
        Log.d(TAG, String.format("Flashsale Discount: %.2f", flashsaleDiscountAmount));
        Log.d(TAG, String.format("Total Discount: %.2f", totalDiscount));
        Log.d(TAG, String.format("Final Profit: %.2f", finalProfit));
        Log.d(TAG, String.format("Profit Margin: %.2f%%", profitMargin));
        Log.d(TAG, String.format("Flashsale Status: %s", flashsaleInfo != null ? "ACTIVE" : "NONE"));
        if (flashsaleInfo != null) {
            Log.d(TAG, String.format("Flashsale Details: %s", flashsaleInfo));
        }
        Log.d(TAG, "================================");

        return finalProfit;
    }

    private FlashsaleInfo getActiveFlashsaleForProduct(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            Log.w(TAG, "Invalid productId for flashsale lookup: " + productId);
            return null;
        }

        Log.d(TAG, String.format("Looking up flashsale for product: %s", productId));

        FlashsaleInfo flashsaleInfo = flashsaleCache.get(productId);

        if (flashsaleInfo == null) {
            Log.d(TAG, String.format("No flashsale found for product: %s", productId));
            return null;
        }

        // Kiểm tra thời gian hiện tại
        long currentTime = System.currentTimeMillis();
        boolean isActive = currentTime >= flashsaleInfo.startTime && currentTime <= flashsaleInfo.endTime;

        Log.d(TAG, String.format("Flashsale check for %s: Current=%d, Start=%d, End=%d, Active=%s",
                productId, currentTime, flashsaleInfo.startTime, flashsaleInfo.endTime, isActive));

        if (isActive) {
            Log.d(TAG, String.format("ACTIVE FLASHSALE found for %s: %s", productId, flashsaleInfo));
            return flashsaleInfo;
        } else {
            Log.d(TAG, String.format("Flashsale for %s is NOT ACTIVE (expired or not started)", productId));
            return null;
        }
    }

    private void updateCategoryProfit(Map<String, Double> categoryProfit, String productId, double profit) {
        if (productId != null && productId.length() >= 2) {
            String categoryCode = productId.substring(0, 2).toUpperCase();
            String categoryName = getCategoryNameFromCode(categoryCode);

            synchronized (categoryProfit) {
                categoryProfit.merge(categoryName, profit, Double::sum);
            }
        }
    }

    private void updateCategoryRevenue(Map<String, Double> categoryRevenue, String productId, double revenue) {
        if (productId != null && productId.length() >= 2) {
            String categoryCode = productId.substring(0, 2).toUpperCase();
            String categoryName = getCategoryNameFromCode(categoryCode);

            synchronized (categoryRevenue) {
                categoryRevenue.merge(categoryName, revenue, Double::sum);
            }

            Log.d(TAG, String.format("Category %s revenue updated: +%.2f", categoryName, revenue));
        }
    }

    private String getCategoryNameFromCode(String categoryCode) {
        switch (categoryCode.toUpperCase()) {
            case "CK": return "Carriers & Kennels";
            case "AC": return "Accessories";
            case "TO": return "Toys";
            case "FU": return "Furniture";
            case "PC": return "Pet Care";
            default: return "Unknown";
        }
    }

    private List<Map<String, Object>> extractProductsFromFields(DocumentSnapshot doc) {
        List<Map<String, Object>> products = new ArrayList<>();

        Log.d(TAG, "=== EXTRACTING PRODUCTS FROM ORDER_ITEMS ===");

        // Duyệt qua các field product1, product2, ... product20
        for (int i = 1; i <= 20; i++) {
            String productField = "product" + i;

            // Lấy map product từ document
            Map<String, Object> productMap = (Map<String, Object>) doc.get(productField);

            if (productMap != null) {
                String productId = (String) productMap.get("product_id");
                Object quantity = productMap.get("quantity");
                Object totalCostOfGoods = productMap.get("total_cost_of_goods");

                Log.d(TAG, String.format("Found %s: ID=%s, Quantity=%s, Cost=%s",
                        productField, productId, quantity, totalCostOfGoods));

                if (productId != null && quantity != null) {
                    Map<String, Object> product = new HashMap<>();
                    product.put("product_id", productId);
                    product.put("quantity", quantity);
                    product.put("total_cost_of_goods", totalCostOfGoods);
                    products.add(product);

                    Log.d(TAG, String.format("Added product: %s (Qty: %s)", productId, quantity));
                }
            }
        }

        Log.d(TAG, String.format("Total products extracted: %d", products.size()));
        return products;
    }

    private int convertToInt(Object obj) {
        if (obj instanceof Long) {
            return ((Long) obj).intValue();
        } else if (obj instanceof Integer) {
            return (Integer) obj;
        } else if (obj instanceof Double) {
            return ((Double) obj).intValue();
        }
        return 0;
    }

    private double convertToDouble(Object obj) {
        if (obj instanceof Long) {
            return ((Long) obj).doubleValue();
        } else if (obj instanceof Integer) {
            return ((Integer) obj).doubleValue();
        } else if (obj instanceof Double) {
            return (Double) obj;
        } else if (obj instanceof Float) {
            return ((Float) obj).doubleValue();
        }
        return 0.0;
    }

    private void updateMonthDataWithCategories(boolean isCurrentMonth, double revenue, double profit,
                                               int orders, Map<String, Double> categoryRevenue,
                                               Map<String, Double> categoryProfit) {

        if (isCurrentMonth) {
            currentMonthRevenue = revenue;
            currentMonthProfit = profit;
            currentMonthOrders = orders;

            // Clear và copy category data
            currentMonthCategoryRevenue.clear();
            currentMonthCategoryRevenue.putAll(categoryRevenue);

            currentMonthCategoryProfit.clear();
            currentMonthCategoryProfit.putAll(categoryProfit);

            Log.d(TAG, String.format("Updated current month data: Revenue=%.2f, Profit=%.2f, Orders=%d",
                    revenue, profit, orders));

            // Log category data
            for (Map.Entry<String, Double> entry : categoryRevenue.entrySet()) {
                Log.d(TAG, String.format("Current month category %s: Revenue=%.2f, Profit=%.2f",
                        entry.getKey(), entry.getValue(),
                        categoryProfit.getOrDefault(entry.getKey(), 0.0)));
            }

        } else {
            lastMonthRevenue = revenue;
            lastMonthProfit = profit;
            lastMonthOrders = orders;

            // Clear và copy category data
            lastMonthCategoryRevenue.clear();
            lastMonthCategoryRevenue.putAll(categoryRevenue);

            lastMonthCategoryProfit.clear();
            lastMonthCategoryProfit.putAll(categoryProfit);

            Log.d(TAG, String.format("Updated last month data: Revenue=%.2f, Profit=%.2f, Orders=%d",
                    revenue, profit, orders));

            // Log category data
            for (Map.Entry<String, Double> entry : categoryRevenue.entrySet()) {
                Log.d(TAG, String.format("Last month category %s: Revenue=%.2f, Profit=%.2f",
                        entry.getKey(), entry.getValue(),
                        categoryProfit.getOrDefault(entry.getKey(), 0.0)));
            }
        }
    }

    private void updateUI() {
        Log.d(TAG, "Updating UI with trend data...");

        // Update current month
        tvCurrentMonthRevenue.setText(String.format(Locale.getDefault(), "%.0f VNĐ", currentMonthRevenue));
        tvCurrentMonthProfit.setText(String.format(Locale.getDefault(), "%.0f VNĐ", currentMonthProfit));
        tvCurrentMonthOrders.setText(String.valueOf(currentMonthOrders));

        // Update last month
        tvLastMonthRevenue.setText(String.format(Locale.getDefault(), "%.0f VNĐ", lastMonthRevenue));
        tvLastMonthProfit.setText(String.format(Locale.getDefault(), "%.0f VNĐ", lastMonthProfit));
        tvLastMonthOrders.setText(String.valueOf(lastMonthOrders));

        // Calculate and update growth
        updateGrowthIndicators();

        // Update analysis sections
        updateAnalysisSections();

        // Update chart
        updateTrendChart();

        Log.d(TAG, "UI updated successfully");
    }

    private void updateGrowthIndicators() {
        // Revenue growth
        double revenueGrowth = lastMonthRevenue > 0 ?
                ((currentMonthRevenue - lastMonthRevenue) / lastMonthRevenue) * 100 : 0;
        tvRevenueGrowth.setText(String.format(Locale.getDefault(), "%.1f%%", revenueGrowth));
        tvRevenueGrowth.setTextColor(revenueGrowth >= 0 ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));

        // Profit growth
        double profitGrowth = lastMonthProfit > 0 ?
                ((currentMonthProfit - lastMonthProfit) / lastMonthProfit) * 100 : 0;
        tvProfitGrowth.setText(String.format(Locale.getDefault(), "%.1f%%", profitGrowth));
        tvProfitGrowth.setTextColor(profitGrowth >= 0 ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));

        // Order growth
        double orderGrowth = lastMonthOrders > 0 ?
                ((double)(currentMonthOrders - lastMonthOrders) / lastMonthOrders) * 100 : 0;
        tvOrderGrowth.setText(String.format(Locale.getDefault(), "%.1f%%", orderGrowth));
        tvOrderGrowth.setTextColor(orderGrowth >= 0 ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));

        // Margin growth
        double currentMargin = currentMonthRevenue > 0 ? (currentMonthProfit / currentMonthRevenue) * 100 : 0;
        double lastMargin = lastMonthRevenue > 0 ? (lastMonthProfit / lastMonthRevenue) * 100 : 0;
        double marginGrowth = lastMargin > 0 ? currentMargin - lastMargin : 0;
        tvMarginGrowth.setText(String.format(Locale.getDefault(), "%.1f%%", marginGrowth));
        tvMarginGrowth.setTextColor(marginGrowth >= 0 ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));
    }

    private void updateAnalysisSections() {
        // Trend Summary
        String trendSummary = generateTrendSummary();
        tvTrendSummary.setText(trendSummary);

        // Quarterly Growth (simplified calculation)
        double quarterlyGrowth = lastMonthRevenue > 0 ?
                ((currentMonthRevenue - lastMonthRevenue) / lastMonthRevenue) * 100 * 3 : 0;
        tvQuarterlyGrowth.setText(String.format(Locale.getDefault(), "%.1f%%", quarterlyGrowth));

        // Yearly Projection
        String yearlyProjection = generateYearlyProjection();
        tvYearlyProjection.setText(yearlyProjection);

        // Cost Analysis
        String costAnalysis = generateCostAnalysis();
        tvCostAnalysis.setText(costAnalysis);

        // Recommendations
        String recommendations = generateRecommendations();
        tvRecommendations.setText(recommendations);
    }

    private String generateTrendSummary() {
        double revenueGrowth = lastMonthRevenue > 0 ?
                ((currentMonthRevenue - lastMonthRevenue) / lastMonthRevenue) * 100 : 0;

        if (revenueGrowth > 10) {
            return "Kinh doanh đang phát triển mạnh mẽ với mức tăng trưởng doanh thu vượt 10%. Đây là tín hiệu tích cực cho sự phát triển của cửa hàng.";
        } else if (revenueGrowth > 0) {
            return "Doanh thu có xu hướng tăng trưởng ổn định. Cần duy trì các chiến lược hiện tại và tìm kiếm cơ hội mở rộng.";
        } else {
            return "Doanh thu có dấu hiệu giảm so với tháng trước. Cần xem xét lại chiến lược kinh doanh và tìm giải pháp cải thiện.";
        }
    }

    private String generateYearlyProjection() {
        double monthlyAverage = (currentMonthRevenue + lastMonthRevenue) / 2;
        double yearlyProjection = monthlyAverage * 12;

        return String.format(Locale.getDefault(),
                "Dự báo doanh thu năm: %.0f VNĐ\nDựa trên xu hướng hiện tại, cửa hàng có thể đạt mức doanh thu này trong năm nay.",
                yearlyProjection);
    }

    private String generateCostAnalysis() {
        double currentMargin = currentMonthRevenue > 0 ? (currentMonthProfit / currentMonthRevenue) * 100 : 0;

        if (currentMargin > 25) {
            return "Tỷ suất lợi nhuận hiện tại rất tốt (>25%). Chi phí được kiểm soát hiệu quả.";
        } else if (currentMargin > 15) {
            return "Tỷ suất lợi nhuận ở mức trung bình (15-25%). Có thể tối ưu thêm chi phí để tăng lợi nhuận.";
        } else {
            return "Tỷ suất lợi nhuận thấp (<15%). Cần xem xét lại cấu trúc chi phí và giá bán.";
        }
    }

    private String generateRecommendations() {
        double revenueGrowth = lastMonthRevenue > 0 ?
                ((currentMonthRevenue - lastMonthRevenue) / lastMonthRevenue) * 100 : 0;
        double profitGrowth = lastMonthProfit > 0 ?
                ((currentMonthProfit - lastMonthProfit) / lastMonthProfit) * 100 : 0;

        StringBuilder recommendations = new StringBuilder();

        if (revenueGrowth < 0) {
            recommendations.append("• Tăng cường marketing và khuyến mãi\n");
            recommendations.append("• Xem xét mở rộng sản phẩm mới\n");
        }

        if (profitGrowth < revenueGrowth) {
            recommendations.append("• Tối ưu hóa chi phí vận hành\n");
            recommendations.append("• Điều chỉnh giá bán phù hợp\n");
        }

        if (currentMonthOrders < lastMonthOrders) {
            recommendations.append("• Cải thiện trải nghiệm khách hàng\n");
            recommendations.append("• Tăng cường chăm sóc khách hàng cũ\n");
        }

        if (recommendations.length() == 0) {
            recommendations.append("Kinh doanh đang phát triển tốt. Tiếp tục duy trì các chiến lược hiện tại.");
        }

        return recommendations.toString();
    }

    private void updateTrendChart() {
        List<Entry> revenueEntries = new ArrayList<>();
        List<Entry> profitEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        // Add data points
        revenueEntries.add(new Entry(0, (float) lastMonthRevenue));
        revenueEntries.add(new Entry(1, (float) currentMonthRevenue));

        profitEntries.add(new Entry(0, (float) lastMonthProfit));
        profitEntries.add(new Entry(1, (float) currentMonthProfit));

        labels.add("Tháng trước");
        labels.add("Tháng này");

        // Create datasets
        LineDataSet revenueDataSet = new LineDataSet(revenueEntries, "Doanh thu");
        revenueDataSet.setColor(Color.parseColor("#9c162c"));
        revenueDataSet.setCircleColor(Color.parseColor("#9c162c"));
        revenueDataSet.setLineWidth(3f);
        revenueDataSet.setCircleRadius(6f);
        revenueDataSet.setValueTextSize(12f);

        LineDataSet profitDataSet = new LineDataSet(profitEntries, "Lợi nhuận");
        profitDataSet.setColor(Color.parseColor("#4CAF50"));
        profitDataSet.setCircleColor(Color.parseColor("#4CAF50"));
        profitDataSet.setLineWidth(3f);
        profitDataSet.setCircleRadius(6f);
        profitDataSet.setValueTextSize(12f);

        LineData lineData = new LineData(revenueDataSet, profitDataSet);
        trendChart.setData(lineData);

        trendChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        trendChart.invalidate();
    }

    private void debugFlashsaleCache() {
        Log.d(TAG, "=== FLASHSALE CACHE DEBUG ===");
        Log.d(TAG, String.format("Total cached products: %d", flashsaleCache.size()));

        if (flashsaleCache.isEmpty()) {
            Log.w(TAG, "Flashsale cache is EMPTY!");
        } else {
            long currentTime = System.currentTimeMillis();
            int activeCount = 0;

            for (Map.Entry<String, FlashsaleInfo> entry : flashsaleCache.entrySet()) {
                FlashsaleInfo info = entry.getValue();
                boolean isActive = currentTime >= info.startTime && currentTime <= info.endTime;

                if (isActive) activeCount++;

                Log.d(TAG, String.format("Product: %s", entry.getKey()));
                Log.d(TAG, String.format("  Flashsale: %s", info.flashsaleName));
                Log.d(TAG, String.format("  Discount: %.2f%%", info.discountRate));
                Log.d(TAG, String.format("  Original Price: %.2f", info.originalPrice));
                Log.d(TAG, String.format("  Start: %d (%s)", info.startTime, new Date(info.startTime)));
                Log.d(TAG, String.format("  End: %d (%s)", info.endTime, new Date(info.endTime)));
                Log.d(TAG, String.format("  Current: %d (%s)", currentTime, new Date(currentTime)));
                Log.d(TAG, String.format("  Active: %s", isActive));
            }

            Log.d(TAG, String.format("Active flashsales: %d/%d", activeCount, flashsaleCache.size()));
        }
        Log.d(TAG, "============================");
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // Inner class for flashsale info
    private static class FlashsaleInfo {
        String productId;
        double discountRate;
        long startTime;
        long endTime;
        double originalPrice;
        String flashsaleName;

        @Override
        public String toString() {
            return String.format("FlashsaleInfo{id='%s', name='%s', discount=%.2f%%, price=%.2f, start=%d, end=%d}",
                    productId, flashsaleName, discountRate, originalPrice, startTime, endTime);
        }
    }
}
