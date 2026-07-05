package com.data.sistemakademik.network;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "SiakadSession";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_ROLE = "user_role";
    private static final String KEY_NAME = "user_name";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void saveAuthToken(String token) {
        editor.putString(KEY_TOKEN, token);
        editor.apply();
    }

    public String getAuthToken() {
        return pref.getString(KEY_TOKEN, null);
    }

    public void saveUserRole(String role) {
        editor.putString(KEY_ROLE, role);
        editor.apply();
    }

    public String getUserRole() {
        return pref.getString(KEY_ROLE, null);
    }

    public void saveUserName(String name) {
        editor.putString(KEY_NAME, name);
        editor.apply();
    }

    public String getUserName() {
        return pref.getString(KEY_NAME, "");
    }

    public void clearSession() {
        editor.clear();
        editor.apply();
    }
}
