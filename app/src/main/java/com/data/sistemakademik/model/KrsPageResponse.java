package com.data.sistemakademik.model;

import java.util.List;

public class KrsPageResponse {
    public boolean isLocked;
    public String message;
    public String uktStatus;
    public int maxSks;
    public int totalSelectedSks;
    public List<KrsItem> selectedKrs;
    public List<Jadwal> allSchedules;
}
