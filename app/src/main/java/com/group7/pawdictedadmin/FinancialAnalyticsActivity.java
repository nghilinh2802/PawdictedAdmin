package com.group7.pawdictedadmin;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FinancialAnalyticsActivity extends AppCompatActivity {

    private TextView tvTotalRevenue, tvTotalOrders, tvTotalCustomers, tvAvgOrderValue;
    private TextView tvRevenueGrowth, tvOrderGrowth, tvTopProduct, tvTopCategory;
    private LineChart revenueChart;
    private BarChart categoryChart;
    private PieChart orderStatusChart;
    private ProgressBar progressBar;
    private ImageView ivBack;
    private CardView cardRevenueInsight, cardInventoryAlert, cardCustomerInsight;

    private FirebaseFirestore db;
    private NumberFormat currencyFormatter;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_financial_analytics);

        initViews();
        initFirebase();
        setupCharts();
        setupClickListeners();
        loadAnalyticsData();
    }

    private void initViews() {
        // KPI Cards
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvTotalOrders = findViewById(R.id.tvTotalOrders);
        tvTotalCustomers = findViewById(R.id.tvTotalCustomers);
        tvAvgOrderValue = findViewById(R.id.tvAvgOrderValue);

        // Growth Indicators
        tvRevenueGrowth = findViewById(R.id.tvRevenueGrowth);
        tvOrderGrowth = findViewById(R.id.tvOrderGrowth);
        tvTopProduct = findViewById(R.id.tvTopProduct);
        tvTopCategory = findViewById(R.id.tvTopCategory);

        // Charts
        revenueChart = findViewById(R.id.revenueChart);
        categoryChart = findViewById(R.id.categoryChart);
        orderStatusChart = findViewById(R.id.orderStatusChart);

        // Other views
        progressBar = findViewById(R.id.progressBar);
        ivBack = findViewById(R.id.ivBack);
        cardRevenueInsight = findViewById(R.id.cardRevenueInsight);
        cardInventoryAlert = findViewById(R.id.cardInventoryAlert);
        cardCustomerInsight = findViewById(R.id.cardCustomerInsight);

        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        handler = new Handler();
    }

    private void initFirebase() {
        db = FirebaseFirestore.getInstance();
    }

    private void setupCharts() {
        setupRevenueChart();
        setupCategoryChart();
        setupOrderStatusChart();
    }

    private void setupRevenueChart() {
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
        revenueChart.getLegend().setEnabled(false);
    }

    private void setupCategoryChart() {
        categoryChart.getDescription().setEnabled(false);
        categoryChart.setTouchEnabled(true);
        categoryChart.setDragEnabled(false);
        categoryChart.setScaleEnabled(false);
        categoryChart.setPinchZoom(false);

        XAxis xAxis = categoryChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        categoryChart.getAxisRight().setEnabled(false);
        categoryChart.getAxisLeft().setDrawGridLines(false);
    }

    private void setupOrderStatusChart() {
        orderStatusChart.getDescription().setEnabled(false);
        orderStatusChart.setTouchEnabled(true);
        orderStatusChart.setRotationEnabled(true);
        orderStatusChart.setHighlightPerTapEnabled(true);
        orderStatusChart.setDrawHoleEnabled(true);
        orderStatusChart.setHoleRadius(40f);
        orderStatusChart.setTransparentCircleRadius(45f);
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());

        cardRevenueInsight.setOnClickListener(v -> {
            // Mở chi tiết phân tích doanh thu
        });

        cardInventoryAlert.setOnClickListener(v -> {
            // Mở cảnh báo tồn kho
        });

        cardCustomerInsight.setOnClickListener(v -> {
            // Mở phân tích khách hàng
        });
    }

    private void loadAnalyticsData() {
        showProgress(true);

        // Load dữ liệu tổng quan
        loadKPIData();

        // Load dữ liệu biểu đồ
        loadRevenueChartData();
        loadCategoryChartData();
        loadOrderStatusChartData();

        handler.postDelayed(() -> showProgress(false), 2000);
    }

    private void loadKPIData() {
        // Tính tổng doanh thu
        db.collection("orders")
                .whereEqualTo("status", "Completed")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        double totalRevenue = 0;
                        int totalOrders = 0;
                        Map<String, Integer> customerCount = new HashMap<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Double amount = document.getDouble("totalAmount");
                            String customerId = document.getString("customerId");

                            if (amount != null) {
                                totalRevenue += amount;
                                totalOrders++;
                            }

                            if (customerId != null) {
                                customerCount.put(customerId, 1);
                            }
                        }

                        double avgOrderValue = totalOrders > 0 ? totalRevenue / totalOrders : 0;

                        // Cập nhật UI
                        tvTotalRevenue.setText(currencyFormatter.format(totalRevenue));
                        tvTotalOrders.setText(String.valueOf(totalOrders));
                        tvTotalCustomers.setText(String.valueOf(customerCount.size()));
                        tvAvgOrderValue.setText(currencyFormatter.format(avgOrderValue));

                        // Tính growth (giả lập)
                        tvRevenueGrowth.setText("+12.5%");
                        tvRevenueGrowth.setTextColor(getResources().getColor(android.R.color.holo_green_dark));

                        tvOrderGrowth.setText("+8.3%");
                        tvOrderGrowth.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    }
                });

        // Load sản phẩm bán chạy nhất
        loadTopSellingData();
    }

    private void loadTopSellingData() {
        // Giả lập dữ liệu sản phẩm bán chạy
        tvTopProduct.setText("Royal Canin Adult");
        tvTopCategory.setText("Thức ăn khô");
    }

    private void loadRevenueChartData() {
        List<Entry> entries = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();

        // Tạo dữ liệu doanh thu 30 ngày gần nhất
        for (int i = 29; i >= 0; i--) {
            calendar.add(Calendar.DAY_OF_YEAR, -i);

            // Giả lập dữ liệu doanh thu ngẫu nhiên
            float revenue = (float) (Math.random() * 3000000 + 1000000); // 1-4 triệu VNĐ
            entries.add(new Entry(29 - i, revenue));

            calendar = Calendar.getInstance(); // Reset calendar
        }

        LineDataSet dataSet = new LineDataSet(entries, "Doanh thu (VNĐ)");
        dataSet.setColor(getResources().getColor(R.color.colorPrimary));
        dataSet.setCircleColor(getResources().getColor(R.color.colorPrimary));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(0f); // Ẩn giá trị trên điểm
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(getResources().getColor(R.color.colorPrimary));
        dataSet.setFillAlpha(50);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);
        revenueChart.setData(lineData);
        revenueChart.animateX(1500);
        revenueChart.invalidate();
    }

    private void loadCategoryChartData() {
        List<BarEntry> entries = new ArrayList<>();
        String[] categories = {"Thức ăn", "Đồ chơi", "Phụ kiện", "Y tế", "Vệ sinh"};

        // Giả lập dữ liệu bán hàng theo danh mục
        for (int i = 0; i < categories.length; i++) {
            float sales = (float) (Math.random() * 50 + 10); // 10-60 sản phẩm
            entries.add(new BarEntry(i, sales));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Số lượng bán");
        dataSet.setColors(new int[]{
                getResources().getColor(R.color.colorPrimary),
                getResources().getColor(R.color.colorAccent),
                getResources().getColor(android.R.color.holo_blue_bright),
                getResources().getColor(android.R.color.holo_green_light),
                getResources().getColor(android.R.color.holo_orange_light)
        });
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLACK);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.8f);

        categoryChart.setData(barData);
        categoryChart.animateY(1500);
        categoryChart.invalidate();
    }

    private void loadOrderStatusChartData() {
        List<PieEntry> entries = new ArrayList<>();

        // Giả lập dữ liệu trạng thái đơn hàng
        entries.add(new PieEntry(45f, "Hoàn thành"));
        entries.add(new PieEntry(25f, "Đang xử lý"));
        entries.add(new PieEntry(20f, "Đang giao"));
        entries.add(new PieEntry(7f, "Đã hủy"));
        entries.add(new PieEntry(3f, "Chờ xác nhận"));

        PieDataSet dataSet = new PieDataSet(entries, "Trạng thái đơn hàng");
        dataSet.setColors(new int[]{
                getResources().getColor(android.R.color.holo_green_light),
                getResources().getColor(android.R.color.holo_orange_light),
                getResources().getColor(android.R.color.holo_blue_bright),
                getResources().getColor(android.R.color.holo_red_light),
                getResources().getColor(R.color.textColorSecondary)
        });
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(2f);

        PieData pieData = new PieData(dataSet);
        orderStatusChart.setData(pieData);
        orderStatusChart.animateXY(1500, 1500);
        orderStatusChart.invalidate();
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
