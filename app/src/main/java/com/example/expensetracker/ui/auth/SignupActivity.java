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
import com.example.expensetracker.data.model.User;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.viewmodel.AuthViewModel;
import com.example.expensetracker.viewmodel.ViewModelFactory;
import com.example.expensetracker.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;

public class SignupActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private SessionManager sessionManager;
    private TextInputEditText etName, etEmail, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        sessionManager = new SessionManager(this);

        // Init ViewModel
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        ViewModelFactory factory = new ViewModelFactory(dbHelper);
        authViewModel = new ViewModelProvider(this, factory).get(AuthViewModel.class);

        // Init Views
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        Button btnSignup = findViewById(R.id.btnSignup);
        TextView tvGoToLogin = findViewById(R.id.tvGoToLogin);

        // Observers
        authViewModel.getUser().observe(this, user -> {
            if (user != null) {
                // Save Session
                sessionManager.createLoginSession(user.getId(), user.getName(), user.getEmail());

                // Navigate to Main Activity
                Intent intent = new Intent(SignupActivity.this, MainActivity.class);
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
        btnSignup.setOnClickListener(v -> {
            String name = etName.getText().toString();
            String email = etEmail.getText().toString();
            String password = etPassword.getText().toString();

            if (validateInput(name, email, password)) {
                User user = new User(name, email, password);
                authViewModel.signup(user);
            }
        });

        tvGoToLogin.setOnClickListener(v -> {
            finish(); // Go back to Login
        });
    }

    private boolean validateInput(String name, String email, String password) {
        if (name.isEmpty()) {
            etName.setError("Name required");
            return false;
        }
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
