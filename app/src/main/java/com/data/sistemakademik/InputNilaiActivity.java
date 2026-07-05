package com.data.sistemakademik;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.data.sistemakademik.model.DosenKelasResponse;
import com.data.sistemakademik.model.Jadwal;
import com.data.sistemakademik.model.Mahasiswa;
import com.data.sistemakademik.network.ApiClient;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InputNilaiActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextInputEditText etSearchClass;
    private LinearLayout listClassesContainer;
    private android.widget.ProgressBar progressBar;
    private List<Jadwal> allSchedules = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_nilai);

        btnBack = findViewById(R.id.btnBack);
        etSearchClass = findViewById(R.id.etSearchClass);
        listClassesContainer = findViewById(R.id.listClassesContainer);
        progressBar = findViewById(R.id.progressBar);

        btnBack.setOnClickListener(v -> finish());

        etSearchClass.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterClasses(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadClasses();
    }

    private void loadClasses() {
        progressBar.setVisibility(android.view.View.VISIBLE);
        ApiClient.getApiService(this).getKelasList().enqueue(new Callback<DosenKelasResponse>() {
            @Override
            public void onResponse(Call<DosenKelasResponse> call, Response<DosenKelasResponse> response) {
                progressBar.setVisibility(android.view.View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    allSchedules = response.body().kelasList;
                    renderClasses(allSchedules);
                } else {
                    showEmptyPlaceholder("Gagal memuat daftar kelas. Kode: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<DosenKelasResponse> call, Throwable t) {
                progressBar.setVisibility(android.view.View.GONE);
                showEmptyPlaceholder("Koneksi ke server terputus.");
            }
        });
    }

    private void filterClasses(String query) {
        if (query.trim().isEmpty()) {
            renderClasses(allSchedules);
            return;
        }
        List<Jadwal> filteredList = new ArrayList<>();
        for (Jadwal j : allSchedules) {
            if (j.mataKuliah.nama.toLowerCase().contains(query.toLowerCase()) || 
                j.ruangan.toLowerCase().contains(query.toLowerCase()) ||
                j.mataKuliah.kode.toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(j);
            }
        }
        renderClasses(filteredList);
    }

    private void renderClasses(List<Jadwal> classes) {
        listClassesContainer.removeAllViews();
        if (classes == null || classes.isEmpty()) {
            showEmptyPlaceholder("Tidak ada kelas yang ditemukan.");
            return;
        }

        for (Jadwal j : classes) {
            MaterialCardView card = new MaterialCardView(this);
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
            cardContent.setPadding(40, 40, 40, 40);

            TextView title = new TextView(this);
            title.setText(j.mataKuliah.nama);
            title.setTextColor(Color.parseColor("#1B1B1D"));
            title.setTextSize(16);
            title.setTypeface(null, Typeface.BOLD);
            cardContent.addView(title);

            TextView codeAndSks = new TextView(this);
            codeAndSks.setText(j.mataKuliah.kode + " • " + j.mataKuliah.sks + " SKS");
            codeAndSks.setTextColor(Color.parseColor("#76777D"));
            codeAndSks.setTextSize(13);
            codeAndSks.setPadding(0, 8, 0, 16);
            cardContent.addView(codeAndSks);

            Button btnPilih = new Button(this);
            btnPilih.setText("Pilih Kelas");
            btnPilih.setTextColor(Color.WHITE);
            btnPilih.setTextSize(13);
            btnPilih.setAllCaps(false);
            btnPilih.setBackground(ContextCompat.getDrawable(this, R.drawable.button_gradient));
            btnPilih.setOnClickListener(v -> fetchEnrolledStudentsForGrade(j.id, j.mataKuliah.nama));
            cardContent.addView(btnPilih);

            card.addView(cardContent);
            listClassesContainer.addView(card);
        }
    }

    private void showEmptyPlaceholder(String message) {
        listClassesContainer.removeAllViews();
        TextView tvEmpty = new TextView(this);
        tvEmpty.setText(message);
        tvEmpty.setTextColor(Color.parseColor("#BA1A1A")); // Red color for error visibility, or use generic grey
        tvEmpty.setPadding(24, 24, 24, 24);
        listClassesContainer.addView(tvEmpty);
    }

    private void fetchEnrolledStudentsForGrade(String jadwalId, String namaMk) {
        ApiClient.getApiService(this).getKelasMahasiswa(jadwalId).enqueue(new Callback<com.data.sistemakademik.model.DosenKelasMahasiswaResponse>() {
            @Override
            public void onResponse(Call<com.data.sistemakademik.model.DosenKelasMahasiswaResponse> call, Response<com.data.sistemakademik.model.DosenKelasMahasiswaResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Mahasiswa> list = response.body().mahasiswaList;
                    if (list == null || list.isEmpty()) {
                        Toast.makeText(InputNilaiActivity.this, "Belum ada mahasiswa yang tervalidasi di kelas ini.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    showStudentSelectionDialog(jadwalId, namaMk, list);
                } else {
                    Toast.makeText(InputNilaiActivity.this, "Gagal memuat data mahasiswa", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<com.data.sistemakademik.model.DosenKelasMahasiswaResponse> call, Throwable t) {
                Toast.makeText(InputNilaiActivity.this, "Koneksi ke server gagal", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showStudentSelectionDialog(String jadwalId, String namaMk, List<Mahasiswa> mahasiswaList) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pilih Mahasiswa - " + namaMk);

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

                com.data.sistemakademik.utils.LoadingDialog loadingDialog = new com.data.sistemakademik.utils.LoadingDialog(InputNilaiActivity.this);
                loadingDialog.startLoadingDialog("Menyimpan Nilai...");

                ApiClient.getApiService(InputNilaiActivity.this).inputNilai(jadwalId, body).enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        loadingDialog.dismissDialog();
                        if (response.isSuccessful()) {
                            Toast.makeText(InputNilaiActivity.this, "Nilai berhasil disimpan & dikonversi!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(InputNilaiActivity.this, "Gagal menginput nilai", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        loadingDialog.dismissDialog();
                        Toast.makeText(InputNilaiActivity.this, "Gagal menghubungi server", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (NumberFormatException e) {
                Toast.makeText(InputNilaiActivity.this, "Nilai harus berupa angka valid", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Batal", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}
