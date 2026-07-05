package com.data.sistemakademik;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.data.sistemakademik.model.Jadwal;
import com.data.sistemakademik.model.MahasiswaDashboardResponse;
import com.data.sistemakademik.network.ApiClient;
import com.data.sistemakademik.network.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentDashboardActivity extends AppCompatActivity {

    private TextView tvStudentName, tvStudentNim, tvStudentIpk, tvStudentSks, tvUktStatus, tvDosenPa;
    private Button btnGotoKrs;
    private android.view.View btnLogout;
    private LinearLayout scheduleContainer, khsContainer;
    private SessionManager sessionManager;
    private TextView tvKhsIpk, tvKhsSks;
    private android.widget.ProgressBar progressBarNilai;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        sessionManager = new SessionManager(this);

        tvStudentName = findViewById(R.id.tvStudentName);
        tvStudentNim = findViewById(R.id.tvStudentNim);
        tvStudentIpk = findViewById(R.id.tvStudentIpk);
        tvStudentSks = findViewById(R.id.tvStudentSks);
        tvUktStatus = findViewById(R.id.tvUktStatus);
        tvDosenPa = findViewById(R.id.tvDosenPa);
        btnGotoKrs = findViewById(R.id.btnGotoKrs);
        btnLogout = findViewById(R.id.btnLogout);
        scheduleContainer = findViewById(R.id.scheduleContainer);

        khsContainer = findViewById(R.id.khsContainer);
        tvKhsIpk = findViewById(R.id.tvKhsIpk);
        tvKhsSks = findViewById(R.id.tvKhsSks);
        progressBarNilai = findViewById(R.id.progressBarNilai);

        btnLogout.setOnClickListener(v -> {
            com.data.sistemakademik.utils.LoadingDialog loadingDialog = new com.data.sistemakademik.utils.LoadingDialog(StudentDashboardActivity.this);
            loadingDialog.startLoadingDialog("Keluar...");
            new android.os.Handler().postDelayed(() -> {
                loadingDialog.dismissDialog();
                sessionManager.clearSession();
                startActivity(new Intent(StudentDashboardActivity.this, LoginActivity.class));
                finish();
            }, 800);
        });

        btnGotoKrs.setOnClickListener(v -> {
            startActivity(new Intent(StudentDashboardActivity.this, KrsActivity.class));
        });
        android.view.View layoutHome = findViewById(R.id.layout_student_home);
        android.view.View layoutKrs = findViewById(R.id.layout_student_krs);
        android.view.View layoutNilai = findViewById(R.id.layout_student_nilai);
        android.view.View layoutProfile = findViewById(R.id.layout_student_profile);

        com.google.android.material.bottomnavigation.BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            layoutHome.setVisibility(android.view.View.GONE);
            layoutKrs.setVisibility(android.view.View.GONE);
            layoutNilai.setVisibility(android.view.View.GONE);
            layoutProfile.setVisibility(android.view.View.GONE);

            int itemId = item.getItemId();
            if (itemId == R.id.nav_student_home) {
                layoutHome.setVisibility(android.view.View.VISIBLE);
                return true;
            } else if (itemId == R.id.nav_student_krs) {
                layoutKrs.setVisibility(android.view.View.VISIBLE);
                return true;
            } else if (itemId == R.id.nav_student_nilai) {
                layoutNilai.setVisibility(android.view.View.VISIBLE);
                return true;
            } else if (itemId == R.id.nav_student_profile) {
                layoutProfile.setVisibility(android.view.View.VISIBLE);
                return true;
            }
            return false;
        });

        loadDashboardData();
        loadKhsData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
        loadKhsData();
    }

    private void loadKhsData() {
        if (progressBarNilai != null) {
            progressBarNilai.setVisibility(android.view.View.VISIBLE);
        }
        ApiClient.getApiService(this).getKhs().enqueue(new Callback<com.data.sistemakademik.model.KhsResponse>() {
            @Override
            public void onResponse(Call<com.data.sistemakademik.model.KhsResponse> call, Response<com.data.sistemakademik.model.KhsResponse> response) {
                if (progressBarNilai != null) {
                    progressBarNilai.setVisibility(android.view.View.GONE);
                }
                if (response.isSuccessful() && response.body() != null) {
                    com.data.sistemakademik.model.KhsResponse data = response.body();
                    tvKhsIpk.setText(String.format("%.2f", data.ipk));
                    tvKhsSks.setText(String.valueOf(data.sksTotal));

                    khsContainer.removeAllViews();
                    if (data.nilaiList == null || data.nilaiList.isEmpty()) {
                        TextView tvEmpty = new TextView(StudentDashboardActivity.this);
                        tvEmpty.setText("Belum ada nilai mata kuliah yang dipublikasikan.");
                        tvEmpty.setTextColor(Color.parseColor("#76777D"));
                        tvEmpty.setPadding(24, 24, 24, 24);
                        khsContainer.addView(tvEmpty);
                    } else {
                        for (com.data.sistemakademik.model.NilaiItem n : data.nilaiList) {
                            addKhsCard(n);
                        }
                    }
                } else {
                    Toast.makeText(StudentDashboardActivity.this, "Gagal memuat nilai", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<com.data.sistemakademik.model.KhsResponse> call, Throwable t) {
                if (progressBarNilai != null) {
                    progressBarNilai.setVisibility(android.view.View.GONE);
                }
                Toast.makeText(StudentDashboardActivity.this, "Koneksi ke server gagal", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addKhsCard(com.data.sistemakademik.model.NilaiItem n) {
        com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(this);
        card.setRadius(16 * getResources().getDisplayMetrics().density);
        card.setCardElevation(0);
        card.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
        card.setStrokeColor(Color.parseColor("#EAE7E9"));
        card.setCardBackgroundColor(Color.WHITE);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 24);
        card.setLayoutParams(params);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(32, 32, 32, 32);
        layout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        LinearLayout leftSection = new LinearLayout(this);
        leftSection.setOrientation(LinearLayout.VERTICAL);
        leftSection.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView title = new TextView(this);
        title.setText(n.mataKuliah.nama);
        title.setTextColor(Color.parseColor("#1B1B1D"));
        title.setTextSize(15);
        title.setTypeface(null, Typeface.BOLD);
        leftSection.addView(title);

        TextView codeAndSks = new TextView(this);
        codeAndSks.setText(n.mataKuliah.kode + " • " + n.mataKuliah.sks + " SKS");
        codeAndSks.setTextColor(Color.parseColor("#76777D"));
        codeAndSks.setTextSize(12);
        codeAndSks.setPadding(0, 4, 0, 8);
        leftSection.addView(codeAndSks);
        
        TextView scoreDetails = new TextView(this);
        scoreDetails.setText("Tugas: " + n.tugas + " | UTS: " + n.uts + " | UAS: " + n.uas);
        scoreDetails.setTextColor(Color.parseColor("#76777D"));
        scoreDetails.setTextSize(11);
        leftSection.addView(scoreDetails);

        layout.addView(leftSection);

        LinearLayout gradePill = new LinearLayout(this);
        gradePill.setOrientation(LinearLayout.VERTICAL);
        gradePill.setGravity(android.view.Gravity.CENTER);
        gradePill.setBackground(ContextCompat.getDrawable(this, R.drawable.icon_bg_indigo));
        gradePill.setPadding(32, 24, 32, 24);

        TextView gradeText = new TextView(this);
        gradeText.setText(n.huruf != null ? n.huruf : "-");
        gradeText.setTextColor(Color.parseColor("#4B41E1"));
        gradeText.setTextSize(20);
        gradeText.setTypeface(null, Typeface.BOLD);
        gradePill.addView(gradeText);

        TextView finalScore = new TextView(this);
        finalScore.setText(String.format("%.1f", n.akhir));
        finalScore.setTextColor(Color.parseColor("#4B41E1"));
        finalScore.setTextSize(11);
        gradePill.addView(finalScore);

        layout.addView(gradePill);
        card.addView(layout);
        khsContainer.addView(card);
    }

    private void loadDashboardData() {
        ApiClient.getApiService(this).getMahasiswaDashboard().enqueue(new Callback<MahasiswaDashboardResponse>() {
            @Override
            public void onResponse(Call<MahasiswaDashboardResponse> call, Response<MahasiswaDashboardResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    MahasiswaDashboardResponse data = response.body();

                    tvStudentName.setText(data.mahasiswa.nama);
                    tvStudentNim.setText("NIM: " + data.mahasiswa.nim);
                    tvStudentIpk.setText(String.valueOf(data.mahasiswa.ipk));
                    tvStudentSks.setText(data.mahasiswa.sksTotal + " SKS");
                    tvUktStatus.setText(data.mahasiswa.uktStatus);

                    if ("Lunas".equalsIgnoreCase(data.mahasiswa.uktStatus)) {
                        tvUktStatus.setBackground(ContextCompat.getDrawable(StudentDashboardActivity.this, R.drawable.chip_success_bg));
                        tvUktStatus.setTextColor(Color.parseColor("#009668"));
                    } else {
                        tvUktStatus.setBackground(ContextCompat.getDrawable(StudentDashboardActivity.this, R.drawable.chip_error_bg));
                        tvUktStatus.setTextColor(Color.parseColor("#BA1A1A"));
                    }

                    tvDosenPa.setText(data.mahasiswa.dosenPa != null ? data.mahasiswa.dosenPa : "Belum Ditentukan");

                    // Render Schedule
                    scheduleContainer.removeAllViews();
                    if (data.jadwalHariIni == null || data.jadwalHariIni.isEmpty()) {
                        TextView tvEmpty = new TextView(StudentDashboardActivity.this);
                        tvEmpty.setText("Tidak ada jadwal kuliah untuk hari ini.");
                        tvEmpty.setTextColor(Color.parseColor("#76777D"));
                        tvEmpty.setPadding(24, 24, 24, 24);
                        scheduleContainer.addView(tvEmpty);
                    } else {
                        boolean first = true;
                        for (Jadwal j : data.jadwalHariIni) {
                            addScheduleCard(j, first);
                            first = false;
                        }
                    }
                } else {
                    Toast.makeText(StudentDashboardActivity.this, "Gagal memuat profil akademik", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MahasiswaDashboardResponse> call, Throwable t) {
                Toast.makeText(StudentDashboardActivity.this, "Gagal menghubungi server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addScheduleCard(Jadwal j, boolean isActive) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.TOP);
        
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 0, 0, 8);
        row.setLayoutParams(rowParams);

        RelativeLayout timelineLeft = new RelativeLayout(this);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                (int) (48 * getResources().getDisplayMetrics().density),
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        timelineLeft.setLayoutParams(leftParams);

        View line = new View(this);
        line.setId(View.generateViewId());
        line.setBackgroundColor(Color.parseColor("#EAE7E9"));
        RelativeLayout.LayoutParams lineParams = new RelativeLayout.LayoutParams(
                (int) (2 * getResources().getDisplayMetrics().density),
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        lineParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        line.setLayoutParams(lineParams);
        timelineLeft.addView(line);

        ImageView dot = new ImageView(this);
        dot.setImageDrawable(ContextCompat.getDrawable(this, 
                isActive ? R.drawable.timeline_dot_active : R.drawable.timeline_dot_inactive));
        RelativeLayout.LayoutParams dotParams = new RelativeLayout.LayoutParams(
                (int) (12 * getResources().getDisplayMetrics().density),
                (int) (12 * getResources().getDisplayMetrics().density)
        );
        dotParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        dotParams.setMargins(0, (int) (8 * getResources().getDisplayMetrics().density), 0, 0);
        dot.setLayoutParams(dotParams);
        timelineLeft.addView(dot);

        row.addView(timelineLeft);

        com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(this);
        card.setRadius(16 * getResources().getDisplayMetrics().density);
        card.setCardElevation(0);
        card.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
        card.setStrokeColor(Color.parseColor("#EAE7E9"));
        card.setCardBackgroundColor(Color.WHITE);
        
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        card.setLayoutParams(cardParams);

        LinearLayout cardContent = new LinearLayout(this);
        cardContent.setOrientation(LinearLayout.VERTICAL);
        cardContent.setPadding(
                (int) (16 * getResources().getDisplayMetrics().density),
                (int) (16 * getResources().getDisplayMetrics().density),
                (int) (16 * getResources().getDisplayMetrics().density),
                (int) (16 * getResources().getDisplayMetrics().density)
        );

        TextView title = new TextView(this);
        title.setText(j.mataKuliah.nama);
        title.setTextColor(Color.parseColor("#1B1B1D"));
        title.setTextSize(15);
        title.setTypeface(null, Typeface.BOLD);
        cardContent.addView(title);

        TextView codeAndSks = new TextView(this);
        codeAndSks.setText(j.mataKuliah.kode + " • " + j.mataKuliah.sks + " SKS");
        codeAndSks.setTextColor(Color.parseColor("#76777D"));
        codeAndSks.setTextSize(12);
        codeAndSks.setPadding(0, 4, 0, 8);
        cardContent.addView(codeAndSks);

        LinearLayout timeAndRoom = new LinearLayout(this);
        timeAndRoom.setOrientation(LinearLayout.HORIZONTAL);
        timeAndRoom.setGravity(android.view.Gravity.CENTER_VERTICAL);

        ImageView clockIcon = new ImageView(this);
        clockIcon.setImageDrawable(ContextCompat.getDrawable(this, android.R.drawable.ic_menu_today));
        clockIcon.setColorFilter(Color.parseColor("#4B41E1"));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                (int) (14 * getResources().getDisplayMetrics().density),
                (int) (14 * getResources().getDisplayMetrics().density)
        );
        iconParams.setMargins(0, 0, (int) (8 * getResources().getDisplayMetrics().density), 0);
        clockIcon.setLayoutParams(iconParams);
        timeAndRoom.addView(clockIcon);

        TextView detailsText = new TextView(this);
        detailsText.setText(j.jamMulai + " - " + j.jamSelesai + " | " + j.ruangan);
        detailsText.setTextColor(Color.parseColor("#4B41E1"));
        detailsText.setTextSize(12);
        detailsText.setTypeface(null, Typeface.BOLD);
        timeAndRoom.addView(detailsText);

        cardContent.addView(timeAndRoom);
        card.addView(cardContent);
        row.addView(card);

        scheduleContainer.addView(row);
    }
}
