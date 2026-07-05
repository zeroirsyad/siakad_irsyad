package com.data.sistemakademik.model;

public class KrsItem {
    public String id;
    public String mahasiswaId;
    public String jadwalId;
    public Jadwal jadwal;
    public String status; // "Draft", "Menunggu Persetujuan", "Divalidasi", "Ditolak / Revisi"
    public String catatan;
}
