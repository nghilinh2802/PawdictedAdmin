package com.group7.pawdictedadmin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "ADMIN_LOGIN";
    private static final String PREFS = "admin_prefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_REMEMBER = "remember";

    private EditText edtEmail, edtPassword;
    private CheckBox chkRemember;
    private ProgressBar progressBar;
    private View overlay;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        chkRemember = findViewById(R.id.ckbRememberInfo);
        progressBar = findViewById(R.id.progressBar);
        overlay = findViewById(R.id.overlay);
        findViewById(R.id.btnLogin).setOnClickListener(v -> loginWithEmail());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadPrefs();
    }

    private void loadPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (prefs.getBoolean(KEY_REMEMBER, false)) {
            edtEmail.setText(prefs.getString(KEY_EMAIL, ""));
            edtPassword.setText(prefs.getString(KEY_PASSWORD, ""));
            chkRemember.setChecked(true);
        }
    }

    private void savePrefs(String email, String password, boolean remember) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
        if (remember) {
            editor.putString(KEY_EMAIL, email);
            editor.putString(KEY_PASSWORD, password);
            editor.putBoolean(KEY_REMEMBER, true);
        } else {
            editor.remove(KEY_EMAIL).remove(KEY_PASSWORD).remove(KEY_REMEMBER);
        }
        editor.apply();
    }

    private void loginWithEmail() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.title_fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        overlay.setVisibility(View.VISIBLE);

        // Kiểm tra email trong Firestore
        db.collection("customers")
                .whereEqualTo("customer_email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        overlay.setVisibility(View.GONE);
                        Toast.makeText(this, R.string.title_email_not_found, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DocumentSnapshot userDoc = snapshot.getDocuments().get(0);
                    String role = userDoc.getString("role");

                    // Kiểm tra role
                    if (!"Admin".equalsIgnoreCase(role) && !"Staff".equalsIgnoreCase(role)) {
                        progressBar.setVisibility(View.GONE);
                        overlay.setVisibility(View.GONE);
                        Toast.makeText(this, R.string.you_have_no_permission, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Xác thực với Firebase Authentication
                    mAuth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(this, task -> {
                                if (task.isSuccessful()) {
                                    savePrefs(email, password, chkRemember.isChecked());
                                    progressBar.setVisibility(View.GONE);
                                    overlay.setVisibility(View.GONE);
                                    Toast.makeText(this, R.string.title_login_successful, Toast.LENGTH_SHORT).show();
                                    navigateToAdminDashboard();
                                } else {
                                    progressBar.setVisibility(View.GONE);
                                    overlay.setVisibility(View.GONE);
                                    Toast.makeText(this, R.string.title_login_failed, Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "Login error", task.getException());
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    overlay.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi kết nối đến Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Firestore error", e);
                });
    }

    private void navigateToAdminDashboard() {
        startActivity(new Intent(this, AdminDashboardActivity.class));
        finish();
    }
}