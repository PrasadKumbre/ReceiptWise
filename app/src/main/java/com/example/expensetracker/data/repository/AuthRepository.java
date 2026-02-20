package com.example.expensetracker.data.repository;

import com.example.expensetracker.data.db.DatabaseHelper;
import com.example.expensetracker.data.model.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuthRepository {
    private final DatabaseHelper dbHelper;
    private final ExecutorService executor;

    public AuthRepository(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
        this.executor = Executors.newSingleThreadExecutor();
    }

    public interface AuthCallback {
        void onSuccess(User user);
        void onError(String message);
    }

    public void login(String email, String password, AuthCallback callback) {
        executor.execute(() -> {
            boolean exists = dbHelper.checkUser(email, password);
            if (exists) {
                User user = dbHelper.getUser(email);
                callback.onSuccess(user);
            } else {
                callback.onError("Invalid email or password");
            }
        });
    }

    public void signup(User user, AuthCallback callback) {
        executor.execute(() -> {
            if (dbHelper.checkUser(user.getEmail())) {
                callback.onError("Email already exists");
            } else {
                long id = dbHelper.addUser(user);
                if (id > -1) {
                    user.setId((int) id);
                    callback.onSuccess(user);
                } else {
                    callback.onError("Signup failed");
                }
            }
        });
    }
}
