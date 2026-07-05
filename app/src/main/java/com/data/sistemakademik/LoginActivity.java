package com.data.sistemakademik;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.data.sistemakademik.model.LoginRequest;
import com.data.sistemakademik.model.LoginResponse;
import com.data.sistemakademik.network.ApiClient;
import com.data.sistemakademik.network.SessionManager;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etPassword;
    private Button btnLogin;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> handleLogin());
    }

    private void handleLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Username dan password wajib diisi", Toast.LENGTH_SHORT).show();
            return;
        }

        com.data.sistemakademik.utils.LoadingDialog loadingDialog = new com.data.sistemakademik.utils.LoadingDialog(this);
        loadingDialog.startLoadingDialog("Verifikasi Akun...");
        btnLogin.setEnabled(false);

        LoginRequest request = new LoginRequest(username, password);
        ApiClient.getApiService(this).login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                loadingDialog.dismissDialog();
                btnLogin.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginRes = response.body();
                    sessionManager.saveAuthToken(loginRes.token);
                    sessionManager.saveUserRole(loginRes.user.role);
                    sessionManager.saveUserName(loginRes.user.username);

                    Toast.makeText(LoginActivity.this, "Login Berhasil", Toast.LENGTH_SHORT).show();

                    // Navigate based on role
                    Intent intent;
                    switch (loginRes.user.role) {
                        case "Mahasiswa":
                            intent = new Intent(LoginActivity.this, StudentDashboardActivity.class);
                            break;
                        case "Dosen":
                            intent = new Intent(LoginActivity.this, LecturerDashboardActivity.class);
                            break;
                        case "Admin":
                            intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                            break;
                        default:
                            intent = new Intent(LoginActivity.this, MainActivity.class);
                            break;
                    }
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Kredensial tidak valid", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                loadingDialog.dismissDialog();
                btnLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Koneksi gagal: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
