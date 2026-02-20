package com.example.expensetracker.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.expensetracker.data.db.DatabaseHelper;
import com.example.expensetracker.data.repository.AuthRepository;
import com.example.expensetracker.data.repository.TransactionRepository;

public class ViewModelFactory implements ViewModelProvider.Factory {

    private final DatabaseHelper dbHelper;

    public ViewModelFactory(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AuthViewModel.class)) {
            return (T) new AuthViewModel(new AuthRepository(dbHelper));
        } else if (modelClass.isAssignableFrom(TransactionViewModel.class)) {
            return (T) new TransactionViewModel(new TransactionRepository(dbHelper));
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
