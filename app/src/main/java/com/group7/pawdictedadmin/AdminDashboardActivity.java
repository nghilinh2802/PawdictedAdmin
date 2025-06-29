package com.group7.pawdictedadmin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Source;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "ADMIN_DASHBOARD";

    private ImageView imgAvatar, btnMenu;
    private TextView txtUsername, txtGreeting, txtTotalOrders, txtTotalRevenue, txtPendingMessages;
    private CardView cardChat, cardFinancial, cardOrders, cardUsers, cardProducts;
    private TextView btnViewAllActivities;
    private RecyclerView recyclerViewActivities;
    private LinearLayout layoutEmptyActivities;
    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private RecentActivityAdapter recentActivityAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Initialize views
        imgAvatar = findViewById(R.id.imgAvatar);
        txtUsername = findViewById(R.id.txtUsername);
        txtGreeting = findViewById(R.id.txtGreeting);
        txtTotalOrders = findViewById(R.id.txtTotalOrders);

        txtPendingMessages = findViewById(R.id.txtPendingMessages);
        btnMenu = findViewById(R.id.btnMenu);
        cardChat = findViewById(R.id.card_chat);
        cardFinancial = findViewById(R.id.card_financial);
        cardOrders = findViewById(R.id.card_orders);
        cardUsers = findViewById(R.id.card_users);
        cardProducts = findViewById(R.id.card_products);
        btnViewAllActivities = findViewById(R.id.btnViewAllActivities);
        recyclerViewActivities = findViewById(R.id.recyclerViewActivities);
        layoutEmptyActivities = findViewById(R.id.layoutEmptyActivities);
        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Setup Drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, R.string.open_drawer_description, R.string.close_drawer_description);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navView.setNavigationItemSelectedListener(this);

        // Setup RecyclerView for Recent Activities
        setupRecentActivitiesRecyclerView();

        // Setup click listeners
        setupClickListeners();

        // Load admin info
        loadAdminInfo();

        // Load dynamic data
        loadStatsData();
        loadRecentActivities();
    }

    public void openProfile(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private void setupClickListeners() {
        // Menu button to open drawer
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Card click listeners
        cardChat.setOnClickListener(v -> {
            Intent intent = new Intent(this, CustomerServiceActivity.class);
            startActivity(intent);
        });

        cardFinancial.setOnClickListener(v -> {
            Intent intent = new Intent(this, FinancialDashboardActivity.class);
            startActivity(intent);
        });

        cardOrders.setOnClickListener(v -> {
            Intent intent = new Intent(this, OrderManagementActivity.class);
            startActivity(intent);
        });

        cardUsers.setOnClickListener(v -> {
            Intent intent = new Intent(this, RoleManagementActivity.class);
            startActivity(intent);
        });

        cardProducts.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProductManagementActivity.class);
            startActivity(intent);
        });

        // View all activities button
        btnViewAllActivities.setOnClickListener(v -> {
            Intent intent = new Intent(this, AllActivitiesActivity.class);
            startActivity(intent);
        });
    }

    private void setupRecentActivitiesRecyclerView() {
        recyclerViewActivities.setLayoutManager(new LinearLayoutManager(this));
        recentActivityAdapter = new RecentActivityAdapter(new ArrayList<>());
        recyclerViewActivities.setAdapter(recentActivityAdapter);
    }

    private void loadStatsData() {
        // Load total orders
        db.collection("orders")
                .whereEqualTo("order_status", "Pending Payment")
                .get(Source.SERVER)
                .addOnSuccessListener(querySnapshot -> {
                    txtTotalOrders.setText(String.valueOf(querySnapshot.size()));
                })
                .addOnFailureListener(e -> {
                    txtTotalOrders.setText("0");
                    Toast.makeText(this, "Failed to load orders", Toast.LENGTH_SHORT).show();
                });

        // Load pending messages
//        db.collection("messages")
//                .whereEqualTo("status", "pending")
//                .get(Source.SERVER)
//                .addOnSuccessListener(querySnapshot -> {
//                    txtPendingMessages.setText(String.valueOf(querySnapshot.size()));
//                })
//                .addOnFailureListener(e -> {
//                    txtPendingMessages.setText("0");
//                    Toast.makeText(this, "Failed to load messages", Toast.LENGTH_SHORT).show();
//                });

        // Load number of unique chats (people who sent messages)
        db.collection("chats")
                .get(Source.SERVER)
                .addOnSuccessListener(querySnapshot -> {
                    txtPendingMessages.setText(String.valueOf(querySnapshot.size()));
                })
                .addOnFailureListener(e -> {
                    txtPendingMessages.setText("0");
                    Toast.makeText(this, "Failed to load chat count", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadRecentActivities() {
        db.collection("activities")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(5)
                .get(Source.SERVER)
                .addOnSuccessListener(querySnapshot -> {
                    List<RecentActivity> activities = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String type = doc.getString("type");
                        String title = doc.getString("title");
                        String description = doc.getString("description");
                        String time = doc.getString("time");
                        String iconRes = doc.getString("icon");
                        String status = doc.getString("status");
                        boolean isHighPriority = doc.getBoolean("isHighPriority") != null && doc.getBoolean("isHighPriority");
                        activities.add(new RecentActivity(type, title, description, time, iconRes, status, isHighPriority));
                    }
                    if (activities.isEmpty()) {
                        layoutEmptyActivities.setVisibility(View.VISIBLE);
                        recyclerViewActivities.setVisibility(View.GONE);
                    } else {
                        layoutEmptyActivities.setVisibility(View.GONE);
                        recyclerViewActivities.setVisibility(View.VISIBLE);
                        recentActivityAdapter.updateActivities(activities);
                    }
                })
                .addOnFailureListener(e -> {
                    layoutEmptyActivities.setVisibility(View.VISIBLE);
                    recyclerViewActivities.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load recent activities", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadAdminInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            txtUsername.setText("Paw'dicted");
            imgAvatar.setImageResource(R.mipmap.ic_logo);
            return;
        }

        String uid = user.getUid();
        db.collection("customers").document(uid)
                .get(Source.SERVER)
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String username = doc.getString("customer_username");
                        String avatar = doc.getString("avatar_img");

                        txtUsername.setText(username != null ? username : "Paw'dicted");
                        if (avatar != null && !avatar.isEmpty()) {
                            Glide.with(this).load(avatar)
                                    .placeholder(R.mipmap.ic_logo)
                                    .circleCrop()
                                    .into(imgAvatar);
                        } else {
                            imgAvatar.setImageResource(R.mipmap.ic_logo);
                        }
                    } else {
                        txtUsername.setText("Paw'dicted");
                        imgAvatar.setImageResource(R.mipmap.ic_logo);
                    }
                })
                .addOnFailureListener(e -> {
                    txtUsername.setText("Paw'dicted");
                    imgAvatar.setImageResource(R.mipmap.ic_logo);
                });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent intent = null;

        if (id == R.id.nav_dashboard) {
            Toast.makeText(this, "Already on Dashboard", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_settings) {
            intent = new Intent(this, SettingManagementActivity.class);
        } else if (id == R.id.nav_logout) {
            mAuth.signOut();
            intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
        }

        if (intent != null) {
            startActivity(intent);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private static class RecentActivity {
        String type;
        String title;
        String description;
        String time;
        String iconRes;
        String status;
        boolean isHighPriority;

        RecentActivity(String type, String title, String description, String time, String iconRes, String status, boolean isHighPriority) {
            this.type = type;
            this.title = title;
            this.description = description;
            this.time = time;
            this.iconRes = iconRes;
            this.status = status;
            this.isHighPriority = isHighPriority;
        }
    }

    private class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.ViewHolder> {
        private List<RecentActivity> activities;

        RecentActivityAdapter(List<RecentActivity> activities) {
            this.activities = activities;
        }

        void updateActivities(List<RecentActivity> newActivities) {
            this.activities = newActivities;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_recent_layout, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            RecentActivity activity = activities.get(position);
            holder.txtActivityTitle.setText(activity.title);
            holder.txtActivityDescription.setText(activity.description);
            holder.txtActivityTime.setText(activity.time);

            // Set icon based on activity type
            int iconResId = R.drawable.ic_shopping_cart; // Default icon
            if ("chat".equals(activity.type)) {
                iconResId = R.mipmap.ic_chat;
            } else if ("order".equals(activity.type)) {
                iconResId = R.drawable.ic_shopping_cart;
            }
            holder.imgActivityIcon.setImageResource(iconResId);

            // Set status badge
            if (activity.status != null && !activity.status.isEmpty()) {
                holder.txtActivityStatus.setText(activity.status);
                holder.txtActivityStatus.setVisibility(View.VISIBLE);
                // Adjust status badge color based on status
                int statusColor = "MỚI".equals(activity.status) ? 0xFFef4444 : 0xFF64748b;
                holder.txtActivityStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(statusColor));
            } else {
                holder.txtActivityStatus.setVisibility(View.GONE);
            }

            // Set priority indicator
            holder.viewPriorityIndicator.setVisibility(activity.isHighPriority ? View.VISIBLE : View.GONE);

            // Set action button click listener
            holder.btnActivityAction.setVisibility(View.VISIBLE);
            holder.btnActivityAction.setOnClickListener(v -> {
                Intent intent;
                if ("chat".equals(activity.type)) {
                    intent = new Intent(AdminDashboardActivity.this, ChatActivity.class);
                } else if ("order".equals(activity.type)) {
                    intent = new Intent(AdminDashboardActivity.this, OrderManagementActivity.class);
                } else {
                    intent = new Intent(AdminDashboardActivity.this, MainActivity.class);
                }
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return activities.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            CardView cardIcon;
            ImageView imgActivityIcon, btnActivityAction;
            TextView txtActivityTitle, txtActivityDescription, txtActivityTime, txtActivityStatus;
            View viewPriorityIndicator;

            ViewHolder(View itemView) {
                super(itemView);
                cardIcon = itemView.findViewById(R.id.cardIcon);
                imgActivityIcon = itemView.findViewById(R.id.imgActivityIcon);
                txtActivityTitle = itemView.findViewById(R.id.txtActivityTitle);
                txtActivityDescription = itemView.findViewById(R.id.txtActivityDescription);
                txtActivityTime = itemView.findViewById(R.id.txtActivityTime);
                txtActivityStatus = itemView.findViewById(R.id.txtActivityStatus);
                btnActivityAction = itemView.findViewById(R.id.btnActivityAction);
                viewPriorityIndicator = itemView.findViewById(R.id.viewPriorityIndicator);
            }
        }
    }
}