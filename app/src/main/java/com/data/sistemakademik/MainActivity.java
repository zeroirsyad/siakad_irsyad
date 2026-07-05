package com.data.sistemakademik;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.data.sistemakademik.network.SessionManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager = new SessionManager(this);
        String token = sessionManager.getAuthToken();
        String role = sessionManager.getUserRole();

        if (token == null || role == null) {
            // Not logged in -> Goto Login
            startActivity(new Intent(this, LoginActivity.class));
        } else {
            // Logged in -> Route based on role
            switch (role) {
                case "Mahasiswa":
                    startActivity(new Intent(this, StudentDashboardActivity.class));
                    break;
                case "Dosen":
                    startActivity(new Intent(this, LecturerDashboardActivity.class));
                    break;
                case "Admin":
                    startActivity(new Intent(this, AdminDashboardActivity.class));
                    break;
                default:
                    startActivity(new Intent(this, LoginActivity.class));
                    break;
            }
        }
        finish();
    }
}