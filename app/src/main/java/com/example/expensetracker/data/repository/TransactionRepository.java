package com.example.expensetracker.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.expensetracker.data.db.DatabaseHelper;
import com.example.expensetracker.data.model.Transaction;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransactionRepository {

    private final DatabaseHelper dbHelper;
    private final ExecutorService executor;

    public TransactionRepository(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void addTransaction(Transaction transaction, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            long result = dbHelper.addTransaction(transaction);
            callback.onResult(result != -1);
        });
    }

    public void getTransactions(int userId, String type, RepositoryCallback<List<Transaction>> callback) {
        executor.execute(() -> {
            List<Transaction> transactions = dbHelper.getTransactionsByType(userId, type);
            callback.onResult(transactions);
        });
    }
    
    public void getLatestTransactions(int userId, RepositoryCallback<List<Transaction>> callback) {
        executor.execute(() -> {
            List<Transaction> transactions = dbHelper.getLatestTransactions(userId, 3); // Top 3
            callback.onResult(transactions);
        });
    }
    
    public void getLatestIncome(int userId, RepositoryCallback<List<Transaction>> callback) {
        executor.execute(() -> {
            List<Transaction> transactions = dbHelper.getLatestTransactionsByType(userId, "INCOME", 3); // Top 3
            callback.onResult(transactions);
        });
    }
     public void getLatestExpense(int userId, RepositoryCallback<List<Transaction>> callback) {
        executor.execute(() -> {
            List<Transaction> transactions = dbHelper.getLatestTransactionsByType(userId, "EXPENSE", 3); // Top 3
            callback.onResult(transactions);
        });
    }

    public void getTotalExpense(int userId, RepositoryCallback<Double> callback) {
        executor.execute(() -> {
            double total = dbHelper.getTotalAmountByType(userId, "EXPENSE");
            callback.onResult(total);
        });
    }

    public void getTotalIncome(int userId, RepositoryCallback<Double> callback) {
        executor.execute(() -> {
            double total = dbHelper.getTotalAmountByType(userId, "INCOME");
            callback.onResult(total);
        });
    }


    public void updateTransaction(Transaction transaction, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
             int rows = dbHelper.updateTransaction(transaction);
             callback.onResult(rows > 0);
        });
    }

    public void deleteTransaction(Transaction transaction, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            dbHelper.deleteTransaction(transaction);
            callback.onResult(null);
        });
    }

    public interface RepositoryCallback<T> {
        void onResult(T result);
    }
}
