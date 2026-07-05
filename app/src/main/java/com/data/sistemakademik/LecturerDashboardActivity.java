package com.data.sistemakademik;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.data.sistemakademik.model.DosenBimbinganResponse;
import com.data.sistemakademik.model.DosenKelasResponse;
import com.data.sistemakademik.model.Jadwal;
import com.data.sistemakademik.model.KrsItem;
import com.data.sistemakademik.model.Mahasiswa;
import com.data.sistemakademik.network.ApiClient;
import com.data.sistemakademik.network.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LecturerDashboardActivity extends AppCompatActivity {

    private TextView tvLecturerName, tvLecturerNidn;
    private android.view.View btnLecturerLogout;
    private LinearLayout supervisionContainer, lecturerClassesContainer;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_dashboard);

        sessionManager = new SessionManager(this);

        tvLecturerName = findViewById(R.id.tvLecturerName);
        tvLecturerNidn = findViewById(R.id.tvLecturerNidn);
        btnLecturerLogout = findViewById(R.id.btnLecturerLogout);
        supervisionContainer = findViewById(R.id.supervisionContainer);
        lecturerClassesContainer = findViewById(R.id.lecturerClassesContainer);

        btnLecturerLogout.setOnClickListener(v -> {
            com.data.sistemakademik.utils.LoadingDialog loadingDialog = new com.data.sistemakademik.utils.LoadingDialog(LecturerDashboardActivity.this);
            loadingDialog.startLoadingDialog("Keluar...");
            new android.os.Handler().postDelayed(() -> {
                loadingDialog.dismissDialog();
                sessionManager.clearSession();
                startActivity(new Intent(LecturerDashboardActivity.this, LoginActivity.class));
                finish();
            }, 800);
        });

        Button btnGotoInputNilai = findViewById(R.id.btnGotoInputNilai);
        if (btnGotoInputNilai != null) {
            btnGotoInputNilai.setOnClickListener(v -> {
                startActivity(new Intent(LecturerDashboardActivity.this, InputNilaiActivity.class));
            });
        }

        android.view.View layoutHome = findViewById(R.id.layout_lecturer_home);
        android.view.View layoutBimbingan = findViewById(R.id.layout_lecturer_bimbingan);
        android.view.View layoutKelas = findViewById(R.id.layout_lecturer_kelas);
        android.view.View layoutProfile = findViewById(R.id.layout_lecturer_profile);

        com.google.android.material.bottomnavigation.BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            layoutHome.setVisibility(android.view.View.GONE);
            layoutBimbingan.setVisibility(android.view.View.GONE);
            layoutKelas.setVisibility(android.view.View.GONE);
            layoutProfile.setVisibility(android.view.View.GONE);

            int itemId = item.getItemId();
            if (itemId == R.id.nav_lecturer_home) {
                layoutHome.setVisibility(android.view.View.VISIBLE);
                return true;
            } else if (itemId == R.id.nav_lecturer_bimbingan) {
                layoutBimbingan.setVisibility(android.view.View.VISIBLE);
                return true;
            } else if (itemId == R.id.nav_lecturer_kelas) {
                layoutKelas.setVisibility(android.view.View.VISIBLE);
                return true;
            } else if (itemId == R.id.nav_lecturer_profile) {
                layoutProfile.setVisibility(android.view.View.VISIBLE);
                return true;
            }
            return false;
        });

        setupCreateKelasForm();
        loadSupervisionData();
        loadTaughtClasses();
    }

    private void setupCreateKelasForm() {
        EditText etKodeMk = findViewById(R.id.etKodeMk);
        EditText etNamaMk = findViewById(R.id.etNamaMk);
        EditText etSksMk = findViewById(R.id.etSksMk);
        EditText etSemesterMk = findViewById(R.id.etSemesterMk);
        Button btnCreateKelasManual = findViewById(R.id.btnCreateKelasManual);

        btnCreateKelasManual.setOnClickListener(v -> {
            String kodeMk = etKodeMk.getText().toString().trim();
            String namaMk = etNamaMk.getText().toString().trim();
            String sksMk = etSksMk.getText().toString().trim();
            String semMk = etSemesterMk.getText().toString().trim();

            if (kodeMk.isEmpty() || namaMk.isEmpty() || sksMk.isEmpty() || semMk.isEmpty()) {
                Toast.makeText(this, "Semua field kriteria mata kuliah wajib diisi!", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> body = new HashMap<>();
            body.put("kodeMk", kodeMk);
            body.put("namaMk", namaMk);
            body.put("sks", sksMk);
            body.put("semester", semMk);
            // Jadwal will be set by admin later, backend handles default value

            btnCreateKelasManual.setEnabled(false);
            btnCreateKelasManual.setText("Menyimpan...");

            ApiClient.getApiService(this).createKelas(body).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    btnCreateKelasManual.setEnabled(true);
                    btnCreateKelasManual.setText("Simpan & Buka Kelas");

                    if (response.isSuccessful()) {
                        Toast.makeText(LecturerDashboardActivity.this, "Kelas berhasil dibuka!", Toast.LENGTH_SHORT).show();
                        // Clear form
                        etKodeMk.setText("");
                        etNamaMk.setText("");
                        etSksMk.setText("");
                        etSemesterMk.setText("");
                        
                        loadTaughtClasses(); // Refresh list
                    } else {
                        Toast.makeText(LecturerDashboardActivity.this, "Gagal membuat kelas.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    btnCreateKelasManual.setEnabled(true);
                    btnCreateKelasManual.setText("Simpan & Buka Kelas");
                    Toast.makeText(LecturerDashboardActivity.this, "Koneksi ke server gagal.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSupervisionData();
        loadTaughtClasses();
    }

    private void loadSupervisionData() {
        ApiClient.getApiService(this).getBimbinganList().enqueue(new Callback<DosenBimbinganResponse>() {
            @Override
            public void onResponse(Call<DosenBimbinganResponse> call, Response<DosenBimbinganResponse> response) {
                supervisionContainer.removeAllViews();
                if (response.isSuccessful() && response.body() != null) {
                    List<Mahasiswa> list = response.body().bimbinganList;

                    boolean hasPending = false;
                    if (list != null) {
                        for (Mahasiswa m : list) {
                            // Check if has pending KRS
                            boolean pendingKrs = false;
                            StringBuilder krsSummary = new StringBuilder();
                            if (m.krs != null) {
                                for (KrsItem k : m.krs) {
                                    if ("Menunggu Persetujuan".equalsIgnoreCase(k.status)) {
                                        pendingKrs = true;
                                        hasPending = true;
                                        krsSummary.append("- ").append(k.jadwal.mataKuliah.nama).append(" (").append(k.jadwal.mataKuliah.sks).append(" SKS)\n");
                                    }
                                }
                            }

                            if (pendingKrs) {
                                addSupervisionCard(m, krsSummary.toString().trim());
                            }
                        }
                    }

                    if (!hasPending) {
                        showNoSupervisionPlaceholder();
                    }
                } else {
                    showNoSupervisionPlaceholder();
                }
            }

            @Override
            public void onFailure(Call<DosenBimbinganResponse> call, Throwable t) {
                showNoSupervisionPlaceholder();
            }
        });
    }

    private void showNoSupervisionPlaceholder() {
        TextView tvEmpty = new TextView(this);
        tvEmpty.setText("Tidak ada pengajuan rencana studi aktif.");
        tvEmpty.setTextColor(Color.parseColor("#76777D"));
        tvEmpty.setPadding(24, 24, 24, 24);
        supervisionContainer.addView(tvEmpty);
    }

    private void addSupervisionCard(Mahasiswa m, String krsSummary) {
        com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(this);
        card.setRadius(18 * getResources().getDisplayMetrics().density);
        card.setCardElevation(0);
        card.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
        card.setStrokeColor(Color.parseColor("#EAE7E9"));
        card.setCardBackgroundColor(Color.WHITE);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);

        LinearLayout cardContent = new LinearLayout(this);
        cardContent.setOrientation(LinearLayout.VERTICAL);
        cardContent.setPadding(
                (int) (20 * getResources().getDisplayMetrics().density),
                (int) (20 * getResources().getDisplayMetrics().density),
                (int) (20 * getResources().getDisplayMetrics().density),
                (int) (20 * getResources().getDisplayMetrics().density)
        );

        // Header with avatar placeholder
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, 12);

        ImageView avatar = new ImageView(this);
        avatar.setImageDrawable(ContextCompat.getDrawable(this, android.R.drawable.ic_menu_myplaces));
        avatar.setColorFilter(Color.parseColor("#4B41E1"));
        avatar.setBackground(ContextCompat.getDrawable(this, R.drawable.icon_bg_indigo));
        int density = (int) getResources().getDisplayMetrics().density;
        avatar.setPadding(8 * density, 8 * density, 8 * density, 8 * density);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(36 * density, 36 * density);
        avatarParams.setMarginEnd(12 * density);
        avatar.setLayoutParams(avatarParams);
        header.addView(avatar);

        LinearLayout nameInfo = new LinearLayout(this);
        nameInfo.setOrientation(LinearLayout.VERTICAL);
        
        TextView name = new TextView(this);
        name.setText(m.nama);
        name.setTextColor(Color.parseColor("#1B1B1D"));
        name.setTextSize(15);
        name.setTypeface(null, Typeface.BOLD);
        nameInfo.addView(name);

        TextView nimText = new TextView(this);
        nimText.setText("NIM: " + m.nim + " | IPK: " + m.ipk);
        nimText.setTextColor(Color.parseColor("#76777D"));
        nimText.setTextSize(12);
        nameInfo.addView(nimText);

        header.addView(nameInfo);
        cardContent.addView(header);

        TextView sectionTitle = new TextView(this);
        sectionTitle.setText("MATA KULIAH DIAJUKAN:");
        sectionTitle.setTextColor(Color.parseColor("#4B41E1"));
        sectionTitle.setTextSize(10);
        sectionTitle.setTypeface(null, Typeface.BOLD);
        sectionTitle.setPadding(0, 4, 0, 6);
        cardContent.addView(sectionTitle);

        TextView details = new TextView(this);
        details.setText(krsSummary);
        details.setTextColor(Color.parseColor("#45464D"));
        details.setTextSize(13);
        details.setPadding(0, 0, 0, 16);
        cardContent.addView(details);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button btnApprove = new Button(this);
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, (int) (44 * density), 1f);
        p1.setMarginEnd(8 * density);
        btnApprove.setLayoutParams(p1);
        btnApprove.setText("Approve");
        btnApprove.setTextColor(Color.WHITE);
        btnApprove.setTextSize(13);
        btnApprove.setPadding(0, 0, 0, 0);
        btnApprove.setAllCaps(false);
        android.graphics.drawable.Drawable approveBg = ContextCompat.getDrawable(this, R.drawable.button_gradient).mutate();
        btnApprove.setBackground(approveBg);
        btnApprove.setOnClickListener(v -> reviewKrs(m.id, "Approve"));
        btnRow.addView(btnApprove);

        Button btnReject = new Button(this);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, (int) (44 * density), 1f);
        btnReject.setLayoutParams(p2);
        btnReject.setText("Reject");
        btnReject.setTextColor(Color.WHITE);
        btnReject.setTextSize(13);
        btnReject.setPadding(0, 0, 0, 0);
        btnReject.setAllCaps(false);
        android.graphics.drawable.Drawable rejectBg = ContextCompat.getDrawable(this, R.drawable.button_gradient).mutate();
        rejectBg.setTint(Color.parseColor("#BA1A1A"));
        btnReject.setBackground(rejectBg);
        btnReject.setOnClickListener(v -> reviewKrs(m.id, "Reject"));
        btnRow.addView(btnReject);

        cardContent.addView(btnRow);
        card.addView(cardContent);
        supervisionContainer.addView(card);
    }

    private void reviewKrs(String mahasiswaId, String action) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(action + " KRS");
        builder.setMessage("Masukkan catatan revisi/persetujuan:");

        final EditText input = new EditText(this);
        input.setHint("Catatan dosen...");
        builder.setView(input);

        builder.setPositiveButton("Simpan", (dialog, which) -> {
            String catatan = input.getText().toString().trim();

            Map<String, Object> body = new HashMap<>();
            body.put("mahasiswaId", mahasiswaId);
            body.put("action", action);
            body.put("catatan", catatan.isEmpty() ? "Disetujui oleh Dosen PA" : catatan);

            ApiClient.getApiService(LecturerDashboardActivity.this).reviewKrs(body).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(LecturerDashboardActivity.this, "KRS berhasil di-review", Toast.LENGTH_SHORT).show();
                        loadSupervisionData();
                    } else {
                        Toast.makeText(LecturerDashboardActivity.this, "Gagal memproses review", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    Toast.makeText(LecturerDashboardActivity.this, "Gagal menghubungi server", Toast.LENGTH_SHORT).show();
                }
            });
        });

        builder.setNegativeButton("Batal", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void loadTaughtClasses() {
        ApiClient.getApiService(this).getKelasList().enqueue(new Callback<DosenKelasResponse>() {
            @Override
            public void onResponse(Call<DosenKelasResponse> call, Response<DosenKelasResponse> response) {
                lecturerClassesContainer.removeAllViews();
                if (response.isSuccessful() && response.body() != null) {
                    List<Jadwal> classes = response.body().kelasList;

                    if (classes == null || classes.isEmpty()) {
                        showNoClassesPlaceholder();
                    } else {
                        for (Jadwal j : classes) {
                            addClassCard(j);
                        }
                    }
                } else {
                    showNoClassesPlaceholder();
                }
            }

            @Override
            public void onFailure(Call<DosenKelasResponse> call, Throwable t) {
                showNoClassesPlaceholder();
            }
        });
    }

    private void showNoClassesPlaceholder() {
        TextView tvEmpty = new TextView(this);
        tvEmpty.setText("Tidak ada kelas mengajar yang terdaftar.");
        tvEmpty.setTextColor(Color.parseColor("#76777D"));
        tvEmpty.setPadding(24, 24, 24, 24);
        lecturerClassesContainer.addView(tvEmpty);
    }

    private void addClassCard(Jadwal j) {
        com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(this);
        card.setRadius(18 * getResources().getDisplayMetrics().density);
        card.setCardElevation(0);
        card.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
        card.setStrokeColor(Color.parseColor("#EAE7E9"));
        card.setCardBackgroundColor(Color.WHITE);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);

        LinearLayout cardContent = new LinearLayout(this);
        cardContent.setOrientation(LinearLayout.VERTICAL);
        cardContent.setPadding(
                (int) (20 * getResources().getDisplayMetrics().density),
                (int) (20 * getResources().getDisplayMetrics().density),
                (int) (20 * getResources().getDisplayMetrics().density),
                (int) (20 * getResources().getDisplayMetrics().density)
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
        codeAndSks.setPadding(0, 4, 0, 12);
        cardContent.addView(codeAndSks);

        LinearLayout detailsLayout = new LinearLayout(this);
        detailsLayout.setOrientation(LinearLayout.HORIZONTAL);
        detailsLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        detailsLayout.setPadding(0, 0, 0, 16);

        ImageView clockIcon = new ImageView(this);
        clockIcon.setImageDrawable(ContextCompat.getDrawable(this, android.R.drawable.ic_menu_today));
        clockIcon.setColorFilter(Color.parseColor("#4B41E1"));
        int density = (int) getResources().getDisplayMetrics().density;
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(14 * density, 14 * density);
        iconParams.setMarginEnd(8 * density);
        clockIcon.setLayoutParams(iconParams);
        detailsLayout.addView(clockIcon);

        TextView detailsText = new TextView(this);
        detailsText.setText(j.hari + ", " + j.jamMulai + " - " + j.jamSelesai + " | Ruang " + j.ruangan);
        detailsText.setTextColor(Color.parseColor("#4B41E1"));
        detailsText.setTextSize(12);
        detailsText.setTypeface(null, Typeface.BOLD);
        detailsLayout.addView(detailsText);

        cardContent.addView(detailsLayout);

        card.addView(cardContent);
        lecturerClassesContainer.addView(card);
    }

    private void fetchEnrolledStudentsForGrade(String jadwalId) {
        ApiClient.getApiService(this).getKelasMahasiswa(jadwalId).enqueue(new Callback<com.data.sistemakademik.model.DosenKelasMahasiswaResponse>() {
            @Override
            public void onResponse(Call<com.data.sistemakademik.model.DosenKelasMahasiswaResponse> call, Response<com.data.sistemakademik.model.DosenKelasMahasiswaResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Mahasiswa> list = response.body().mahasiswaList;
                    if (list == null || list.isEmpty()) {
                        Toast.makeText(LecturerDashboardActivity.this, "Belum ada mahasiswa yang mengambil kelas ini atau KRS belum divalidasi.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    showStudentSelectionDialog(jadwalId, list);
                } else {
                    Toast.makeText(LecturerDashboardActivity.this, "Gagal memuat data mahasiswa", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<com.data.sistemakademik.model.DosenKelasMahasiswaResponse> call, Throwable t) {
                Toast.makeText(LecturerDashboardActivity.this, "Koneksi ke server gagal", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showStudentSelectionDialog(String jadwalId, List<Mahasiswa> mahasiswaList) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pilih Mahasiswa");

        String[] names = new String[mahasiswaList.size()];
        for (int i = 0; i < mahasiswaList.size(); i++) {
            names[i] = mahasiswaList.get(i).nama + " (" + mahasiswaList.get(i).nim + ")";
        }

        builder.setItems(names, (dialog, which) -> {
            Mahasiswa selectedStudent = mahasiswaList.get(which);
            showGradeInputDialog(jadwalId, selectedStudent);
        });

        builder.setNegativeButton("Batal", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showGradeInputDialog(String jadwalId, Mahasiswa student) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Input Nilai - " + student.nama);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final EditText etTugas = new EditText(this);
        etTugas.setHint("Nilai Tugas (20%)");
        layout.addView(etTugas);

        final EditText etUts = new EditText(this);
        etUts.setHint("Nilai UTS (30%)");
        layout.addView(etUts);

        final EditText etUas = new EditText(this);
        etUas.setHint("Nilai UAS (50%)");
        layout.addView(etUas);

        builder.setView(layout);

        builder.setPositiveButton("Simpan", (dialog, which) -> {
            try {
                double tugas = Double.parseDouble(etTugas.getText().toString().trim());
                double uts = Double.parseDouble(etUts.getText().toString().trim());
                double uas = Double.parseDouble(etUas.getText().toString().trim());

                List<Map<String, Object>> nilaiList = new ArrayList<>();
                Map<String, Object> record = new HashMap<>();
                record.put("mahasiswaId", student.id);
                record.put("tugas", tugas);
                record.put("uts", uts);
                record.put("uas", uas);
                nilaiList.add(record);

                Map<String, Object> body = new HashMap<>();
                body.put("nilaiList", nilaiList);

                ApiClient.getApiService(LecturerDashboardActivity.this).inputNilai(jadwalId, body).enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(LecturerDashboardActivity.this, "Nilai berhasil disimpan & dikonversi!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(LecturerDashboardActivity.this, "Gagal menginput nilai", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        Toast.makeText(LecturerDashboardActivity.this, "Gagal menghubungi server", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (NumberFormatException e) {
                Toast.makeText(LecturerDashboardActivity.this, "Nilai harus berupa angka valid", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Batal", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}
