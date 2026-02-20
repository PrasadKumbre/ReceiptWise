package com.example.expensetracker.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.expensetracker.R;
import com.example.expensetracker.data.db.DatabaseHelper;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.viewmodel.AuthViewModel;
import com.example.expensetracker.viewmodel.ViewModelFactory;
import com.example.expensetracker.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private SessionManager sessionManager;
    private TextInputEditText etEmail, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Session Check
        sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
             Intent intent = new Intent(LoginActivity.this, MainActivity.class);
             intent.putExtra("USER_ID", sessionManager.getUserId());
             intent.putExtra("USER_NAME", sessionManager.getUserName());
             startActivity(intent);
             finish();
             return;
        }

        setContentView(R.layout.activity_login);

        // Init ViewModel
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        ViewModelFactory factory = new ViewModelFactory(dbHelper);
        authViewModel = new ViewModelProvider(this, factory).get(AuthViewModel.class);

        // Init Views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvGoToSignup = findViewById(R.id.tvGoToSignup);

        // Observers
        authViewModel.getUser().observe(this, user -> {
            if (user != null) {
                // Save Session
                sessionManager.createLoginSession(user.getId(), user.getName(), user.getEmail());
                
                // Navigate to Main Activity
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("USER_ID", user.getId());
                intent.putExtra("USER_NAME", user.getName());
                startActivity(intent);
                finish();
            }
        });

        authViewModel.getError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });

        // Listeners
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString();
            String password = etPassword.getText().toString();

            if (validateInput(email, password)) {
                authViewModel.login(email, password);
            }
        });

        tvGoToSignup.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        });
    }

    private boolean validateInput(String email, String password) {
        if (email.isEmpty()) {
            etEmail.setError("Email required");
            return false;
        }
        if (password.isEmpty()) {
            etPassword.setError("Password required");
            return false;
        }
        return true;
    }
}
