package com.group7.pawdictedadmin;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
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

public class TrendAnalysisActivity extends AppCompatActivity {

    private static final String TAG = "TrendAnalysis";

    // UI Components
    private TextView tvRevenueGrowth, tvProfitGrowth, tvOrderGrowth, tvMarginGrowth;
    private TextView tvCurrentMonthRevenue, tvLastMonthRevenue, tvCurrentMonthProfit, tvLastMonthProfit;
    private TextView tvCurrentMonthOrders, tvLastMonthOrders, tvTrendSummary;
    private LineChart trendChart;
    private ProgressBar progressBar;
    private ImageView ivBack;

    // Data & Firebase
    private FirebaseFirestore db;
    private NumberFormat currencyFormatter;
    private SimpleDateFormat dateFormat;

    // Trend Data
    private Map<String, Double> monthlyRevenueData = new HashMap<>();
    private Map<String, Double> monthlyProfitData = new HashMap<>();
    private Map<String, Integer> monthlyOrderData = new HashMap<>();

    // Current vs Last Month Data
    private AtomicReference<Double> currentMonthRevenue = new AtomicReference<>(0.0);
    private AtomicReference<Double> lastMonthRevenue = new AtomicReference<>(0.0);
    private AtomicReference<Double> currentMonthProfit = new AtomicReference<>(0.0);
    private AtomicReference<Double> lastMonthProfit = new AtomicReference<>(0.0);
    private AtomicReference<Integer> currentMonthOrders = new AtomicReference<>(0);
    private AtomicReference<Integer> lastMonthOrders = new AtomicReference<>(0);

    // Processing State
    private AtomicInteger completedAnalysis = new AtomicInteger(0);
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
        setContentView(R.layout.activity_trend_analysis);

        initializeComponents();
        loadTrendData();
    }

    private void initializeComponents() {
        // Initialize views
        tvRevenueGrowth = findViewById(R.id.tvRevenueGrowth);
        tvProfitGrowth = findViewById(R.id.tvProfitGrowth);
        tvOrderGrowth = findViewById(R.id.tvOrderGrowth);
        tvMarginGrowth = findViewById(R.id.tvMarginGrowth);
        tvCurrentMonthRevenue = findViewById(R.id.tvCurrentMonthRevenue);
        tvLastMonthRevenue = findViewById(R.id.tvLastMonthRevenue);
        tvCurrentMonthProfit = findViewById(R.id.tvCurrentMonthProfit);
        tvLastMonthProfit = findViewById(R.id.tvLastMonthProfit);
        tvCurrentMonthOrders = findViewById(R.id.tvCurrentMonthOrders);
        tvLastMonthOrders = findViewById(R.id.tvLastMonthOrders);
        tvTrendSummary = findViewById(R.id.tvTrendSummary);
        trendChart = findViewById(R.id.trendChart);
        progressBar = findViewById(R.id.progressBar);
        ivBack = findViewById(R.id.ivBack);

        // Initialize Firebase and formatters
        db = FirebaseFirestore.getInstance();
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        dateFormat = new SimpleDateFormat("MM/yyyy", Locale.getDefault());

        // Setup chart
        setupChart();

        // Setup click listeners
        ivBack.setOnClickListener(v -> finish());

        Log.d(TAG, "Components initialized");
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

        trendChart.getAxisRight().setEnabled(false);
        trendChart.getAxisLeft().setDrawGridLines(false);
        trendChart.getLegend().setEnabled(true);

        Log.d(TAG, "Chart setup completed");
    }

    private void loadTrendData() {
        showProgress(true);
        Log.d(TAG, "Loading trend analysis data");

        resetData();

        // Step 1: Load flashsale data
        loadFlashsaleData(() -> {
            // Step 2: Analyze trends
            analyzeTrends();
        });
    }

    private void resetData() {
        currentMonthRevenue.set(0.0);
        lastMonthRevenue.set(0.0);
        currentMonthProfit.set(0.0);
        lastMonthProfit.set(0.0);
        currentMonthOrders.set(0);
        lastMonthOrders.set(0);
        completedAnalysis.set(0);

        flashsaleCache.clear();
        monthlyRevenueData.clear();
        monthlyProfitData.clear();
        monthlyOrderData.clear();

        Log.d(TAG, "Data reset");
    }

    private void loadFlashsaleData(Runnable onComplete) {
        Log.d(TAG, "Loading flashsale data for trend analysis");

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

    private void analyzeTrends() {
        Log.d(TAG, "Analyzing trends");

        // Get current and last month dates
        Calendar now = Calendar.getInstance();
        Calendar lastMonth = Calendar.getInstance();
        lastMonth.add(Calendar.MONTH, -1);

        Date currentMonthStart = getStartOfMonth(now.getTime());
        Date currentMonthEnd = getEndOfMonth(now.getTime());
        Date lastMonthStart = getStartOfMonth(lastMonth.getTime());
        Date lastMonthEnd = getEndOfMonth(lastMonth.getTime());

        // Analyze current month
        analyzeMonthData(currentMonthStart, currentMonthEnd, true, () -> {
            // Analyze last month
            analyzeMonthData(lastMonthStart, lastMonthEnd, false, () -> {
                // Analyze historical data for chart
                analyzeHistoricalData(() -> {
                    finalizeAnalysis();
                });
            });
        });
    }

    private void analyzeMonthData(Date startDate, Date endDate, boolean isCurrentMonth, Runnable onComplete) {
        String monthLabel = isCurrentMonth ? "current" : "last";
        Log.d(TAG, String.format("Analyzing %s month data from %s to %s",
                monthLabel, dateFormat.format(startDate), dateFormat.format(endDate)));

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

                    // Calculate revenue from order_value
                    for (QueryDocumentSnapshot orderDoc : orderSnapshot) {
                        Double orderValue = orderDoc.getDouble("order_value");
                        if (orderValue != null) {
                            monthRevenue.updateAndGet(v -> v + orderValue);
                            monthOrders.incrementAndGet();
                        }
                    }

                    // Calculate profit for each order
                    for (QueryDocumentSnapshot orderDoc : orderSnapshot) {
                        String orderItemId = orderDoc.getString("order_item_id");

                        if (orderItemId != null) {
                            calculateOrderProfit(orderItemId, monthProfit, () -> {
                                if (processedOrders.incrementAndGet() >= totalOrders) {
                                    // Update month data
                                    if (isCurrentMonth) {
                                        currentMonthRevenue.set(monthRevenue.get());
                                        currentMonthProfit.set(monthProfit.get());
                                        currentMonthOrders.set(monthOrders.get());
                                    } else {
                                        lastMonthRevenue.set(monthRevenue.get());
                                        lastMonthProfit.set(monthProfit.get());
                                        lastMonthOrders.set(monthOrders.get());
                                    }

                                    Log.d(TAG, String.format("%s month results: Revenue=%.2f, Profit=%.2f, Orders=%d",
                                            monthLabel, monthRevenue.get(), monthProfit.get(), monthOrders.get()));

                                    onComplete.run();
                                }
                            });
                        } else {
                            if (processedOrders.incrementAndGet() >= totalOrders) {
                                if (isCurrentMonth) {
                                    currentMonthRevenue.set(monthRevenue.get());
                                    currentMonthProfit.set(monthProfit.get());
                                    currentMonthOrders.set(monthOrders.get());
                                } else {
                                    lastMonthRevenue.set(monthRevenue.get());
                                    lastMonthProfit.set(monthProfit.get());
                                    lastMonthOrders.set(monthOrders.get());
                                }
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

    private void calculateOrderProfit(String orderItemId, AtomicReference<Double> monthProfit, Runnable onComplete) {
        db.collection("order_items").document(orderItemId)
                .get()
                .addOnSuccessListener(orderItemDoc -> {
                    if (orderItemDoc.exists()) {
                        List<Map<String, Object>> products = (List<Map<String, Object>>) orderItemDoc.get("products");

                        if (products != null && !products.isEmpty()) {
                            final AtomicReference<Double> orderProfit = new AtomicReference<>(0.0);
                            final AtomicInteger processedProducts = new AtomicInteger(0);
                            final int totalProducts = products.size();

                            for (Map<String, Object> product : products) {
                                String productId = (String) product.get("product_id");
                                Object quantityObj = product.get("quantity");

                                if (productId != null && quantityObj != null) {
                                    int quantity = (quantityObj instanceof Long) ?
                                            ((Long) quantityObj).intValue() : (Integer) quantityObj;

                                    final String finalProductId = productId;
                                    final int finalQuantity = quantity;

                                    db.collection("products").document(finalProductId)
                                            .get()
                                            .addOnSuccessListener(productDoc -> {
                                                if (productDoc.exists()) {
                                                    Double price = productDoc.getDouble("price");
                                                    Double discount = productDoc.getDouble("discount");

                                                    if (price != null && price > 0) {
                                                        if (discount == null) discount = 0.0;

                                                        // Calculate profit
                                                        boolean isInFlashsale = isProductInActiveFlashsale(finalProductId);
                                                        double flashsaleDiscountRate = isInFlashsale ?
                                                                flashsaleCache.get(finalProductId).discountRate : 0;

                                                        double baseProfit = price * 0.3 * finalQuantity;
                                                        double productDiscount = discount * finalQuantity;
                                                        double flashsaleDiscount = isInFlashsale ?
                                                                (price * flashsaleDiscountRate / 100 * finalQuantity) : 0;
                                                        double totalDiscount = productDiscount + flashsaleDiscount;
                                                        double actualProfit = baseProfit - totalDiscount;

                                                        orderProfit.updateAndGet(v -> v + actualProfit);
                                                    }
                                                }

                                                if (processedProducts.incrementAndGet() >= totalProducts) {
                                                    monthProfit.updateAndGet(v -> v + orderProfit.get());
                                                    onComplete.run();
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                if (processedProducts.incrementAndGet() >= totalProducts) {
                                                    onComplete.run();
                                                }
                                            });
                                } else {
                                    if (processedProducts.incrementAndGet() >= totalProducts) {
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

    private boolean isProductInActiveFlashsale(String productId) {
        FlashsaleInfo flashsaleInfo = flashsaleCache.get(productId);
        if (flashsaleInfo == null) return false;

        long currentTime = System.currentTimeMillis();
        return currentTime >= flashsaleInfo.startTime && currentTime <= flashsaleInfo.endTime;
    }

    private void analyzeHistoricalData(Runnable onComplete) {
        Log.d(TAG, "Analyzing historical data for trend chart");

        // Get last 6 months data
        Calendar cal = Calendar.getInstance();

        final AtomicInteger processedMonths = new AtomicInteger(0);
        final int totalMonths = 6;

        for (int i = 0; i < totalMonths; i++) {
            Date monthStart = getStartOfMonth(cal.getTime());
            Date monthEnd = getEndOfMonth(cal.getTime());
            String monthKey = dateFormat.format(cal.getTime());

            final String finalMonthKey = monthKey;

            db.collection("orders")
                    .whereEqualTo("order_status", "Completed")
                    .whereGreaterThanOrEqualTo("order_time", monthStart)
                    .whereLessThan("order_time", monthEnd)
                    .get()
                    .addOnSuccessListener(monthSnapshot -> {
                        double monthRevenue = 0;
                        int monthOrders = 0;

                        for (QueryDocumentSnapshot orderDoc : monthSnapshot) {
                            Double orderValue = orderDoc.getDouble("order_value");
                            if (orderValue != null) {
                                monthRevenue += orderValue;
                                monthOrders++;
                            }
                        }

                        // Estimate profit (30% of revenue for simplicity)
                        double monthProfit = monthRevenue * 0.3;

                        synchronized (monthlyRevenueData) {
                            monthlyRevenueData.put(finalMonthKey, monthRevenue);
                            monthlyProfitData.put(finalMonthKey, monthProfit);
                            monthlyOrderData.put(finalMonthKey, monthOrders);
                        }

                        Log.d(TAG, String.format("Historical data for %s: Revenue=%.2f, Profit=%.2f, Orders=%d",
                                finalMonthKey, monthRevenue, monthProfit, monthOrders));

                        if (processedMonths.incrementAndGet() >= totalMonths) {
                            Log.d(TAG, "Historical data analysis completed");
                            onComplete.run();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading historical data for month: " + finalMonthKey, e);
                        if (processedMonths.incrementAndGet() >= totalMonths) {
                            onComplete.run();
                        }
                    });

            cal.add(Calendar.MONTH, -1);
        }
    }

    private void finalizeAnalysis() {
        Log.d(TAG, "Finalizing trend analysis");

        // Calculate growth rates
        double revenueGrowth = calculateGrowthRate(currentMonthRevenue.get(), lastMonthRevenue.get());
        double profitGrowth = calculateGrowthRate(currentMonthProfit.get(), lastMonthProfit.get());
        double orderGrowth = calculateGrowthRate(currentMonthOrders.get(), lastMonthOrders.get());

        // Calculate margin growth
        double currentMargin = currentMonthRevenue.get() > 0 ?
                (currentMonthProfit.get() / currentMonthRevenue.get()) * 100 : 0;
        double lastMargin = lastMonthRevenue.get() > 0 ?
                (lastMonthProfit.get() / lastMonthRevenue.get()) * 100 : 0;
        double marginGrowth = currentMargin - lastMargin;

        Log.d(TAG, "=== TREND ANALYSIS RESULTS ===");
        Log.d(TAG, String.format("Current Month: Revenue=%.2f, Profit=%.2f, Orders=%d, Margin=%.2f%%",
                currentMonthRevenue.get(), currentMonthProfit.get(), currentMonthOrders.get(), currentMargin));
        Log.d(TAG, String.format("Last Month: Revenue=%.2f, Profit=%.2f, Orders=%d, Margin=%.2f%%",
                lastMonthRevenue.get(), lastMonthProfit.get(), lastMonthOrders.get(), lastMargin));
        Log.d(TAG, String.format("Growth Rates: Revenue=%.2f%%, Profit=%.2f%%, Orders=%.2f%%, Margin=%.2f%%",
                revenueGrowth, profitGrowth, orderGrowth, marginGrowth));
        Log.d(TAG, String.format("Historical Data Points: %d months", monthlyRevenueData.size()));
        Log.d(TAG, "=============================");

        updateUI(revenueGrowth, profitGrowth, orderGrowth, marginGrowth);
        generateChart();
        showProgress(false);
    }

    private double calculateGrowthRate(double current, double previous) {
        if (previous == 0) return 0;
        return ((current - previous) / previous) * 100;
    }

    private void updateUI(double revenueGrowth, double profitGrowth, double orderGrowth, double marginGrowth) {
        // Update growth indicators
        setGrowthText(tvRevenueGrowth, revenueGrowth);
        setGrowthText(tvProfitGrowth, profitGrowth);
        setGrowthText(tvOrderGrowth, orderGrowth);
        setGrowthText(tvMarginGrowth, marginGrowth);

        // Update current month data
        tvCurrentMonthRevenue.setText(currencyFormatter.format(currentMonthRevenue.get()));
        tvCurrentMonthProfit.setText(currencyFormatter.format(currentMonthProfit.get()));
        tvCurrentMonthOrders.setText(String.valueOf(currentMonthOrders.get()));

        // Update last month data
        tvLastMonthRevenue.setText(currencyFormatter.format(lastMonthRevenue.get()));
        tvLastMonthProfit.setText(currencyFormatter.format(lastMonthProfit.get()));
        tvLastMonthOrders.setText(String.valueOf(lastMonthOrders.get()));

        // Generate trend summary
        generateTrendSummary(revenueGrowth, profitGrowth, orderGrowth);

        Log.d(TAG, "UI updated successfully");
    }

    private void setGrowthText(TextView textView, double growth) {
        String growthText = String.format("%+.1f%%", growth);
        textView.setText(growthText);

        int color = growth >= 0 ?
                getResources().getColor(R.color.success_color) :
                getResources().getColor(R.color.error_color);
        textView.setTextColor(color);
    }

    private void generateTrendSummary(double revenueGrowth, double profitGrowth, double orderGrowth) {
        StringBuilder summary = new StringBuilder();

        if (revenueGrowth > 0 && profitGrowth > 0 && orderGrowth > 0) {
            summary.append("📈 Xu hướng tăng trưởng tích cực trên tất cả các chỉ số. ");
        } else if (revenueGrowth > 0 && profitGrowth > 0) {
            summary.append("📊 Doanh thu và lợi nhuận đều tăng trưởng tốt. ");
        } else if (revenueGrowth > 0) {
            summary.append("💰 Doanh thu tăng nhưng cần cải thiện hiệu quả. ");
        } else {
            summary.append("⚠️ Cần có chiến lược cải thiện hiệu suất kinh doanh. ");
        }

        if (Math.abs(revenueGrowth) > 20) {
            summary.append("Biến động doanh thu lớn cần được theo dõi. ");
        }

        if (profitGrowth > revenueGrowth + 5) {
            summary.append("Hiệu quả quản lý chi phí được cải thiện đáng kể.");
        } else if (profitGrowth < revenueGrowth - 5) {
            summary.append("Cần xem xét tối ưu hóa chi phí và giá bán.");
        }

        tvTrendSummary.setText(summary.toString());
    }

    private void generateChart() {
        if (monthlyRevenueData.isEmpty()) {
            Log.w(TAG, "No historical data for trend chart");
            return;
        }

        List<Entry> revenueEntries = new ArrayList<>();
        List<Entry> profitEntries = new ArrayList<>();
        List<String> sortedMonths = new ArrayList<>(monthlyRevenueData.keySet());
        sortedMonths.sort(String::compareTo);

        for (int i = 0; i < sortedMonths.size(); i++) {
            String month = sortedMonths.get(i);
            float revenue = monthlyRevenueData.get(month).floatValue();
            float profit = monthlyProfitData.getOrDefault(month, 0.0).floatValue();

            revenueEntries.add(new Entry(i, revenue));
            profitEntries.add(new Entry(i, profit));
        }

        if (!revenueEntries.isEmpty()) {
            LineDataSet revenueDataSet = new LineDataSet(revenueEntries, "Doanh thu");
            revenueDataSet.setColor(getResources().getColor(R.color.colorPrimary));
            revenueDataSet.setCircleColor(getResources().getColor(R.color.colorPrimary));
            revenueDataSet.setLineWidth(3f);
            revenueDataSet.setCircleRadius(4f);
            revenueDataSet.setDrawCircleHole(false);
            revenueDataSet.setValueTextSize(0f);

            LineDataSet profitDataSet = new LineDataSet(profitEntries, "Lợi nhuận");
            profitDataSet.setColor(getResources().getColor(R.color.success_color));
            profitDataSet.setCircleColor(getResources().getColor(R.color.success_color));
            profitDataSet.setLineWidth(3f);
            profitDataSet.setCircleRadius(4f);
            profitDataSet.setDrawCircleHole(false);
            profitDataSet.setValueTextSize(0f);

            LineData lineData = new LineData(revenueDataSet, profitDataSet);
            trendChart.setData(lineData);
            trendChart.animateX(1500);
            trendChart.invalidate();

            Log.d(TAG, "Trend chart generated with " + sortedMonths.size() + " months");
        }
    }

    private Date getStartOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Date getEndOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    private void displayNoData() {
        tvRevenueGrowth.setText("0%");
        tvProfitGrowth.setText("0%");
        tvOrderGrowth.setText("0%");
        tvMarginGrowth.setText("0%");
        tvCurrentMonthRevenue.setText("Chưa có dữ liệu");
        tvLastMonthRevenue.setText("Chưa có dữ liệu");
        tvCurrentMonthProfit.setText("Chưa có dữ liệu");
        tvLastMonthProfit.setText("Chưa có dữ liệu");
        tvCurrentMonthOrders.setText("0");
        tvLastMonthOrders.setText("0");
        tvTrendSummary.setText("Chưa có đủ dữ liệu để phân tích xu hướng");

        Log.w(TAG, "No data available for trend analysis");
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
