package com.data.sistemakademik.model;

import java.util.List;

public class AdminFinanceResponse {
    public static class Summary {
        public int totalMahasiswa;
        public int totalLunas;
        public int totalBelumLunas;
        public int persentaseLunas;
    }
    
    public Summary summary;
    public List<Mahasiswa> mahasiswa;
}
