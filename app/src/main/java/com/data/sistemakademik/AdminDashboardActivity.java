package com.data.sistemakademik;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.data.sistemakademik.model.AdminFinanceResponse;
import com.data.sistemakademik.model.AdminResourcesResponse;
import com.data.sistemakademik.model.KrsPageResponse;
import com.data.sistemakademik.model.Mahasiswa;
import com.data.sistemakademik.network.ApiClient;
import com.data.sistemakademik.network.SessionManager;
import com.google.android.material.textfield.TextInputEditText;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvFinanceSummary;
    private LinearLayout studentVerifyListContainer;
    private Spinner spinnerMataKuliah, spinnerDosen;
    private TextInputEditText etHari, etJamMulai, etJamSelesai, etRuangan;
    private Button btnCreateJadwal;
    private android.view.View btnAdminLogout;
    private SessionManager sessionManager;

    private List<String> mkIds = new ArrayList<>();
    private List<String> dosenIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        sessionManager = new SessionManager(this);

        tvFinanceSummary = findViewById(R.id.tvFinanceSummary);
        studentVerifyListContainer = findViewById(R.id.studentVerifyListContainer);
        spinnerMataKuliah = findViewById(R.id.spinnerMataKuliah);
        spinnerDosen = findViewById(R.id.spinnerDosen);
        etHari = findViewById(R.id.etHari);
        etJamMulai = findViewById(R.id.etJamMulai);
        etJamSelesai = findViewById(R.id.etJamSelesai);
        etRuangan = findViewById(R.id.etRuangan);
        btnCreateJadwal = findViewById(R.id.btnCreateJadwal);
        btnAdminLogout = findViewById(R.id.btnAdminLogout);

        btnAdminLogout.setOnClickListener(v -> {
            com.data.sistemakademik.utils.LoadingDialog loadingDialog = new com.data.sistemakademik.utils.LoadingDialog(AdminDashboardActivity.this);
            loadingDialog.startLoadingDialog("Keluar...");
            new android.os.Handler().postDelayed(() -> {
                loadingDialog.dismissDialog();
                sessionManager.clearSession();
                startActivity(new Intent(AdminDashboardActivity.this, LoginActivity.class));
                finish();
            }, 800);
        });
        btnCreateJadwal.setOnClickListener(v -> handleCreateJadwal());

        android.view.View layoutHome = findViewById(R.id.layout_admin_home);
        android.view.View layoutKeuangan = findViewById(R.id.layout_admin_keuangan);
        android.view.View layoutJadwal = findViewById(R.id.layout_admin_jadwal);
        android.view.View layoutProfile = findViewById(R.id.layout_admin_profile);

        com.google.android.material.bottomnavigation.BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            layoutHome.setVisibility(android.view.View.GONE);
            layoutKeuangan.setVisibility(android.view.View.GONE);
            layoutJadwal.setVisibility(android.view.View.GONE);
            layoutProfile.setVisibility(android.view.View.GONE);

            int itemId = item.getItemId();
            if (itemId == R.id.nav_admin_home) {
                layoutHome.setVisibility(android.view.View.VISIBLE);
                return true;
            } else if (itemId == R.id.nav_admin_keuangan) {
                layoutKeuangan.setVisibility(android.view.View.VISIBLE);
                return true;
            } else if (itemId == R.id.nav_admin_jadwal) {
                layoutJadwal.setVisibility(android.view.View.VISIBLE);
                return true;
            } else if (itemId == R.id.nav_admin_profile) {
                layoutProfile.setVisibility(android.view.View.VISIBLE);
                return true;
            }
            return false;
        });

        loadFinanceData();
        loadSpinnersData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFinanceData();
        loadSpinnersData();
    }

    private void loadFinanceData() {
        ApiClient.getApiService(this).getFinanceOverview().enqueue(new Callback<AdminFinanceResponse>() {
            @Override
            public void onResponse(Call<AdminFinanceResponse> call, Response<AdminFinanceResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AdminFinanceResponse finance = response.body();
                    tvFinanceSummary.setText(String.format("Total Mahasiswa: %d | Lunas: %d | Belum Lunas: %d (%d%% Lunas)",
                             finance.summary.totalMahasiswa,
                             finance.summary.totalLunas,
                             finance.summary.totalBelumLunas,
                             finance.summary.persentaseLunas));

                    renderStudentVerifyList(finance.mahasiswa);
                } else {
                    Toast.makeText(AdminDashboardActivity.this, "Gagal memuat keuangan", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AdminFinanceResponse> call, Throwable t) {
                Toast.makeText(AdminDashboardActivity.this, "Gagal menghubungi server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderStudentVerifyList(List<Mahasiswa> list) {
        studentVerifyListContainer.removeAllViews();
        if (list == null || list.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("Tidak ada mahasiswa terdaftar.");
            tvEmpty.setTextColor(Color.parseColor("#76777D"));
            tvEmpty.setPadding(24, 24, 24, 24);
            studentVerifyListContainer.addView(tvEmpty);
        } else {
            int density = (int) getResources().getDisplayMetrics().density;
            for (Mahasiswa m : list) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                row.setPadding(0, 12 * density, 0, 12 * density);

                ImageView avatar = new ImageView(this);
                avatar.setImageDrawable(ContextCompat.getDrawable(this, android.R.drawable.ic_menu_myplaces));
                avatar.setColorFilter(Color.parseColor("#4B41E1"));
                avatar.setBackground(ContextCompat.getDrawable(this, R.drawable.icon_bg_indigo));
                avatar.setPadding(6 * density, 6 * density, 6 * density, 6 * density);
                LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(32 * density, 32 * density);
                avatarParams.setMarginEnd(12 * density);
                avatar.setLayoutParams(avatarParams);
                row.addView(avatar);

                LinearLayout textCol = new LinearLayout(this);
                textCol.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                textCol.setLayoutParams(colParams);

                TextView name = new TextView(this);
                name.setText(m.nama);
                name.setTextColor(Color.parseColor("#1B1B1D"));
                name.setTextSize(14);
                name.setTypeface(null, Typeface.BOLD);
                textCol.addView(name);

                TextView nimStatus = new TextView(this);
                nimStatus.setText("NIM: " + m.nim);
                nimStatus.setTextColor(Color.parseColor("#76777D"));
                nimStatus.setTextSize(11);
                textCol.addView(nimStatus);

                row.addView(textCol);

                if ("Belum Lunas".equalsIgnoreCase(m.uktStatus)) {
                    TextView badge = new TextView(this);
                    badge.setText("Belum Lunas");
                    badge.setTextColor(Color.parseColor("#BA1A1A"));
                    badge.setTextSize(11);
                    badge.setTypeface(null, Typeface.BOLD);
                    badge.setBackground(ContextCompat.getDrawable(this, R.drawable.chip_error_bg));
                    badge.setPadding(12 * density, 6 * density, 12 * density, 6 * density);
                    LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    badgeParams.setMarginEnd(12 * density);
                    badge.setLayoutParams(badgeParams);
                    row.addView(badge);

                    Button btnVerify = new Button(this);
                    btnVerify.setText("Verify");
                    btnVerify.setTextColor(Color.WHITE);
                    btnVerify.setTextSize(11);
                    btnVerify.setAllCaps(false);
                    LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            (int)(36 * density)
                    );
                    btnVerify.setLayoutParams(btnParams);
                    btnVerify.setBackground(ContextCompat.getDrawable(this, R.drawable.button_gradient));
                    btnVerify.setOnClickListener(v -> verifyPayment(m.id));
                    row.addView(btnVerify);
                } else {
                    TextView badge = new TextView(this);
                    badge.setText("Lunas");
                    badge.setTextColor(Color.parseColor("#009668"));
                    badge.setTextSize(11);
                    badge.setTypeface(null, Typeface.BOLD);
                    badge.setBackground(ContextCompat.getDrawable(this, R.drawable.chip_success_bg));
                    badge.setPadding(12 * density, 6 * density, 12 * density, 6 * density);
                    row.addView(badge);
                }

                studentVerifyListContainer.addView(row);
                
                View divider = new View(this);
                LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1
                );
                dividerParams.setMargins(0, 4 * density, 0, 4 * density);
                divider.setLayoutParams(dividerParams);
                divider.setBackgroundColor(Color.parseColor("#EAE7E9"));
                studentVerifyListContainer.addView(divider);
            }
        }
    }

    private void verifyPayment(String mahasiswaId) {
        Map<String, String> body = new HashMap<>();
        body.put("mahasiswaId", mahasiswaId);

        ApiClient.getApiService(this).verifyPayment(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminDashboardActivity.this, "UKT Mahasiswa berhasil diverifikasi!", Toast.LENGTH_SHORT).show();
                    loadFinanceData(); // Reload
                } else {
                    Toast.makeText(AdminDashboardActivity.this, "Gagal memverifikasi", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(AdminDashboardActivity.this, "Koneksi gagal", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSpinnersData() {
        ApiClient.getApiService(this).getAdminResources().enqueue(new Callback<AdminResourcesResponse>() {
            @Override
            public void onResponse(Call<AdminResourcesResponse> call, Response<AdminResourcesResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<String> mkNames = new ArrayList<>();
                    List<String> dosenNames = new ArrayList<>();

                    mkIds.clear();
                    dosenIds.clear();

                    if (response.body().mataKuliah != null) {
                        for (com.data.sistemakademik.model.MataKuliah mk : response.body().mataKuliah) {
                            mkIds.add(mk.id);
                            mkNames.add(mk.nama + " (" + mk.kode + ")");
                        }
                    }

                    if (response.body().dosen != null) {
                        for (com.data.sistemakademik.model.Dosen d : response.body().dosen) {
                            dosenIds.add(d.id);
                            dosenNames.add(d.nama);
                        }
                    }

                    ArrayAdapter<String> mkAdapter = new ArrayAdapter<>(AdminDashboardActivity.this,
                            android.R.layout.simple_spinner_item, mkNames);
                    mkAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerMataKuliah.setAdapter(mkAdapter);

                    ArrayAdapter<String> dosenAdapter = new ArrayAdapter<>(AdminDashboardActivity.this,
                            android.R.layout.simple_spinner_item, dosenNames);
                    dosenAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerDosen.setAdapter(dosenAdapter);
                } else {
                    Toast.makeText(AdminDashboardActivity.this, "Gagal mengambil data dropdown", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AdminResourcesResponse> call, Throwable t) {
                Toast.makeText(AdminDashboardActivity.this, "Koneksi gagal mengambil data dropdown", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleCreateJadwal() {
        if (spinnerMataKuliah.getSelectedItem() == null || spinnerDosen.getSelectedItem() == null) {
            Toast.makeText(this, "Pilih mata kuliah & dosen terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        String mkId = mkIds.get(spinnerMataKuliah.getSelectedItemPosition());
        String dosenId = dosenIds.get(spinnerDosen.getSelectedItemPosition());

        String hari = etHari.getText().toString().trim();
        String jamMulai = etJamMulai.getText().toString().trim();
        String jamSelesai = etJamSelesai.getText().toString().trim();
        String ruangan = etRuangan.getText().toString().trim();

        if (hari.isEmpty() || jamMulai.isEmpty() || jamSelesai.isEmpty() || ruangan.isEmpty()) {
            Toast.makeText(this, "Semua kolom jadwal wajib diisi", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("mataKuliahId", mkId);
        body.put("dosenId", dosenId);
        body.put("hari", hari);
        body.put("jamMulai", jamMulai);
        body.put("jamSelesai", jamSelesai);
        body.put("ruangan", ruangan);

        btnCreateJadwal.setEnabled(false);
        btnCreateJadwal.setText("Memproses...");

        ApiClient.getApiService(this).createJadwal(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                btnCreateJadwal.setEnabled(true);
                btnCreateJadwal.setText("Buat Jadwal Kuliah");

                if (response.isSuccessful()) {
                    Toast.makeText(AdminDashboardActivity.this, "Jadwal berhasil ditambahkan!", Toast.LENGTH_SHORT).show();
                    etHari.setText("");
                    etJamMulai.setText("");
                    etJamSelesai.setText("");
                    etRuangan.setText("");
                } else {
                    String errorMsg = "Gagal membuat jadwal";
                    try {
                        errorMsg = response.errorBody().string();
                        if (errorMsg.contains("message")) {
                            int index = errorMsg.indexOf("\"message\":\"");
                            if (index != -1) {
                                int end = errorMsg.indexOf("\"", index + 11);
                                errorMsg = errorMsg.substring(index + 11, end);
                            }
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                    Toast.makeText(AdminDashboardActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                btnCreateJadwal.setEnabled(true);
                btnCreateJadwal.setText("Buat Jadwal Kuliah");
                Toast.makeText(AdminDashboardActivity.this, "Koneksi gagal", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
