package com.data.sistemakademik.network;

import com.data.sistemakademik.model.*;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {
    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @GET("api/auth/profile")
    Call<LoginResponse> getProfile();

    // Student endpoints
    @GET("api/mahasiswa/dashboard")
    Call<MahasiswaDashboardResponse> getMahasiswaDashboard();

    @GET("api/mahasiswa/krs")
    Call<KrsPageResponse> getKrsPage();

    @POST("api/mahasiswa/krs/pilih")
    Call<Map<String, Object>> pilihMataKuliah(@Body Map<String, String> body);

    @DELETE("api/mahasiswa/krs/hapus/{jadwalId}")
    Call<Map<String, Object>> hapusMataKuliah(@Path("jadwalId") String jadwalId);

    @POST("api/mahasiswa/krs/ajukan")
    Call<Map<String, Object>> ajukanKrs();

    @GET("api/mahasiswa/khs")
    Call<KhsResponse> getKhs();

    // Lecturer endpoints
    @GET("api/dosen/bimbingan")
    Call<DosenBimbinganResponse> getBimbinganList();

    @POST("api/dosen/bimbingan/review")
    Call<Map<String, Object>> reviewKrs(@Body Map<String, Object> body);

    @GET("api/dosen/kelas")
    Call<DosenKelasResponse> getKelasList();

    @POST("api/dosen/kelas")
    Call<Map<String, Object>> createKelas(@Body Map<String, Object> body);

    @GET("api/dosen/kelas/{jadwalId}/mahasiswa")
    Call<DosenKelasMahasiswaResponse> getKelasMahasiswa(@Path("jadwalId") String jadwalId);

    @POST("api/dosen/kelas/{jadwalId}/nilai")
    Call<Map<String, Object>> inputNilai(@Path("jadwalId") String jadwalId, @Body Map<String, Object> body);

    // Admin endpoints
    @GET("api/admin/finance")
    Call<AdminFinanceResponse> getFinanceOverview();

    @POST("api/admin/finance/verify")
    Call<Map<String, Object>> verifyPayment(@Body Map<String, String> body);

    @POST("api/admin/jadwal")
    Call<Map<String, Object>> createJadwal(@Body Map<String, Object> body);

    @GET("api/admin/resources")
    Call<AdminResourcesResponse> getAdminResources();
}
