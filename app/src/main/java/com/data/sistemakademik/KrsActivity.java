package com.data.sistemakademik;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.data.sistemakademik.model.Jadwal;
import com.data.sistemakademik.model.KrsItem;
import com.data.sistemakademik.model.KrsPageResponse;
import com.data.sistemakademik.network.ApiClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class KrsActivity extends AppCompatActivity {

    private View layoutLocked, layoutKrsContainer;
    private TextView tvMaxSks, tvSelectedSks, tvLockMessage, tvCollisionDetails;
    private Button btnBackFromLock, btnSubmitKrs;
    private LinearLayout draftKrsContainer, catalogContainer, collisionWarningCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_krs);

        layoutLocked = findViewById(R.id.layoutLocked);
        layoutKrsContainer = findViewById(R.id.layoutKrsContainer);
        tvMaxSks = findViewById(R.id.tvMaxSks);
        tvSelectedSks = findViewById(R.id.tvSelectedSks);
        tvLockMessage = findViewById(R.id.tvLockMessage);
        tvCollisionDetails = findViewById(R.id.tvCollisionDetails);
        btnBackFromLock = findViewById(R.id.btnBackFromLock);
        btnSubmitKrs = findViewById(R.id.btnSubmitKrs);
        draftKrsContainer = findViewById(R.id.draftKrsContainer);
        catalogContainer = findViewById(R.id.catalogContainer);
        collisionWarningCard = findViewById(R.id.collisionWarningCard);

        btnBackFromLock.setOnClickListener(v -> finish());
        btnSubmitKrs.setOnClickListener(v -> submitKrs());

        loadKrsPage();
    }

    private void loadKrsPage() {
        ApiClient.getApiService(this).getKrsPage().enqueue(new Callback<KrsPageResponse>() {
            @Override
            public void onResponse(Call<KrsPageResponse> call, Response<KrsPageResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    KrsPageResponse krsData = response.body();

                    if (krsData.isLocked) {
                        layoutLocked.setVisibility(View.VISIBLE);
                        layoutKrsContainer.setVisibility(View.GONE);
                        tvLockMessage.setText(krsData.message);
                    } else {
                        layoutLocked.setVisibility(View.GONE);
                        layoutKrsContainer.setVisibility(View.VISIBLE);

                        tvMaxSks.setText(krsData.maxSks + " SKS");
                        tvSelectedSks.setText(krsData.totalSelectedSks + " SKS");

                        // Render Draft list
                        renderDraftKrs(krsData.selectedKrs);

                        // Render Catalog
                        renderCatalog(krsData.allSchedules, krsData.selectedKrs);

                        // Render Schedule Collision Warning
                        checkAndRenderCollisions(krsData.selectedKrs);
                    }
                } else {
                    Toast.makeText(KrsActivity.this, "Gagal memuat halaman KRS", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<KrsPageResponse> call, Throwable t) {
                Toast.makeText(KrsActivity.this, "Gagal menghubungi server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderDraftKrs(List<KrsItem> selectedKrs) {
        draftKrsContainer.removeAllViews();
        if (selectedKrs == null || selectedKrs.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("Belum ada kelas yang dipilih.");
            tvEmpty.setTextColor(Color.parseColor("#76777D"));
            tvEmpty.setPadding(20, 20, 20, 20);
            draftKrsContainer.addView(tvEmpty);
        } else {
            for (KrsItem item : selectedKrs) {
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setBackground(ContextCompat.getDrawable(this, R.drawable.card_dark_bg));
                card.setPadding(28, 28, 28, 28);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 0, 20);
                card.setLayoutParams(params);

                TextView title = new TextView(this);
                title.setText(item.jadwal.mataKuliah.nama + " (" + item.jadwal.mataKuliah.kode + ")");
                title.setTextColor(Color.parseColor("#1B1B1D"));
                title.setTextSize(15);
                title.setTypeface(null, Typeface.BOLD);
                card.addView(title);

                TextView details = new TextView(this);
                details.setText("SKS: " + item.jadwal.mataKuliah.sks + " | Status: " + item.status + "\n🕒 " + item.jadwal.hari + ", " + item.jadwal.jamMulai + " - " + item.jadwal.jamSelesai);
                details.setTextColor(Color.parseColor("#76777D"));
                details.setTextSize(12);
                details.setPadding(0, 6, 0, 12);
                card.addView(details);

                if (!"Divalidasi".equalsIgnoreCase(item.status) && !"Menunggu Persetujuan".equalsIgnoreCase(item.status)) {
                    Button btnDelete = new Button(this);
                    btnDelete.setText("Hapus Mata Kuliah");
                    btnDelete.setTextColor(Color.WHITE);
                    btnDelete.setTextSize(12);
                    btnDelete.setAllCaps(false);
                    btnDelete.setBackground(ContextCompat.getDrawable(this, R.drawable.button_gradient));
                    btnDelete.getBackground().setTint(Color.parseColor("#EF4444")); // Red delete tint
                    btnDelete.setOnClickListener(v -> deleteCourse(item.jadwalId));
                    card.addView(btnDelete);
                }

                draftKrsContainer.addView(card);
            }
        }
    }

    private void renderCatalog(List<Jadwal> allSchedules, List<KrsItem> selectedKrs) {
        catalogContainer.removeAllViews();
        if (allSchedules == null || allSchedules.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("Katalog mata kuliah kosong.");
            tvEmpty.setTextColor(Color.parseColor("#76777D"));
            tvEmpty.setPadding(20, 20, 20, 20);
            catalogContainer.addView(tvEmpty);
        } else {
            for (Jadwal j : allSchedules) {
                // Check if already in selected
                boolean alreadySelected = false;
                for (KrsItem k : selectedKrs) {
                    if (k.jadwalId.equals(j.id)) {
                        alreadySelected = true;
                        break;
                    }
                }

                if (alreadySelected) continue;

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setBackground(ContextCompat.getDrawable(this, R.drawable.card_dark_bg));
                card.setPadding(28, 28, 28, 28);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 0, 20);
                card.setLayoutParams(params);

                TextView title = new TextView(this);
                title.setText(j.mataKuliah.nama + " (" + j.mataKuliah.kode + ")");
                title.setTextColor(Color.parseColor("#1B1B1D"));
                title.setTextSize(15);
                title.setTypeface(null, Typeface.BOLD);
                card.addView(title);

                TextView details = new TextView(this);
                details.setText("SKS: " + j.mataKuliah.sks + " | Kelas " + j.ruangan + "\n🕒 " + j.hari + ", " + j.jamMulai + " - " + j.jamSelesai + "\nDosen: " + j.dosen.nama);
                details.setTextColor(Color.parseColor("#76777D"));
                details.setTextSize(12);
                details.setPadding(0, 6, 0, 12);
                card.addView(details);

                Button btnSelect = new Button(this);
                btnSelect.setText("Pilih Kelas");
                btnSelect.setTextColor(Color.WHITE);
                btnSelect.setTextSize(12);
                btnSelect.setAllCaps(false);
                btnSelect.setBackground(ContextCompat.getDrawable(this, R.drawable.button_gradient));
                btnSelect.setOnClickListener(v -> chooseCourse(j.id));
                card.addView(btnSelect);

                catalogContainer.addView(card);
            }
        }
    }

    private void checkAndRenderCollisions(List<KrsItem> krsList) {
        collisionWarningCard.setVisibility(View.GONE);
        if (krsList == null || krsList.size() < 2) return;

        StringBuilder builder = new StringBuilder();
        boolean hasConflict = false;

        for (int i = 0; i < krsList.size(); i++) {
            Jadwal j1 = krsList.get(i).jadwal;
            for (int k = i + 1; k < krsList.size(); k++) {
                Jadwal j2 = krsList.get(k).jadwal;

                if (j1.hari.equalsIgnoreCase(j2.hari)) {
                    int start1 = timeToMinutes(j1.jamMulai);
                    int end1 = timeToMinutes(j1.jamSelesai);
                    int start2 = timeToMinutes(j2.jamMulai);
                    int end2 = timeToMinutes(j2.jamSelesai);

                    if (start1 < end2 && start2 < end1) {
                        hasConflict = true;
                        builder.append("• ").append(j1.mataKuliah.nama).append(" bentrok dengan ")
                                .append(j2.mataKuliah.nama).append(" pada hari ")
                                .append(j1.hari).append(" (")
                                .append(j1.jamMulai).append("-").append(j1.jamSelesai)
                                .append(" & ").append(j2.jamMulai).append("-").append(j2.jamSelesai).append(")\n");
                    }
                }
            }
        }

        if (hasConflict) {
            collisionWarningCard.setVisibility(View.VISIBLE);
            tvCollisionDetails.setText(builder.toString().trim());
        }
    }

    private int timeToMinutes(String time) {
        try {
            String[] parts = time.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return 0;
        }
    }

    private void chooseCourse(String jadwalId) {
        Map<String, String> body = new HashMap<>();
        body.put("jadwalId", jadwalId);

        ApiClient.getApiService(this).pilihMataKuliah(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(KrsActivity.this, "Kelas berhasil ditambahkan ke draft", Toast.LENGTH_SHORT).show();
                    loadKrsPage(); // Reload lists
                } else {
                    String errorMsg = "Gagal memilih kelas";
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
                    Toast.makeText(KrsActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(KrsActivity.this, "Gagal menghubungi server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteCourse(String jadwalId) {
        ApiClient.getApiService(this).hapusMataKuliah(jadwalId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(KrsActivity.this, "Mata kuliah dihapus dari draft", Toast.LENGTH_SHORT).show();
                    loadKrsPage();
                } else {
                    Toast.makeText(KrsActivity.this, "Gagal menghapus mata kuliah", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(KrsActivity.this, "Gagal menghubungi server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitKrs() {
        btnSubmitKrs.setEnabled(false);
        btnSubmitKrs.setText("Mengajukan...");

        ApiClient.getApiService(this).ajukanKrs().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                btnSubmitKrs.setEnabled(true);
                btnSubmitKrs.setText("Ajukan Rencana Studi (KRS)");

                if (response.isSuccessful()) {
                    Toast.makeText(KrsActivity.this, "KRS berhasil diajukan ke Dosen PA!", Toast.LENGTH_LONG).show();
                    loadKrsPage();
                } else {
                    Toast.makeText(KrsActivity.this, "Gagal mengajukan KRS", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                btnSubmitKrs.setEnabled(true);
                btnSubmitKrs.setText("Ajukan Rencana Studi (KRS)");
                Toast.makeText(KrsActivity.this, "Gagal menghubungi server", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
