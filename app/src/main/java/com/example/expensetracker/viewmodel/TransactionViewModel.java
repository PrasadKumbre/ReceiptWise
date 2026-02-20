package com.example.expensetracker.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.expensetracker.data.model.Transaction;
import com.example.expensetracker.data.repository.TransactionRepository;

import java.util.Date;
import java.util.List;

public class TransactionViewModel extends ViewModel {

    private final TransactionRepository transactionRepository;
    
    private final MutableLiveData<List<Transaction>> expensesLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Transaction>> incomeLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Transaction>> allExpensesLiveData = new MutableLiveData<>(); // Raw
    private final MutableLiveData<List<Transaction>> allIncomeLiveData = new MutableLiveData<>(); // Raw

    private final MutableLiveData<List<Transaction>> recentTransactionsLiveData = new MutableLiveData<>(); // Mixed or recent
    private final MutableLiveData<List<Transaction>> recentIncomeLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Transaction>> recentExpenseLiveData = new MutableLiveData<>();
    
    // Dashboard Totals
    private final MutableLiveData<Double> totalExpenseLiveData = new MutableLiveData<>();
    private final MutableLiveData<Double> totalIncomeLiveData = new MutableLiveData<>();
    
    private final MutableLiveData<Boolean> operationResult = new MutableLiveData<>();
    
    private List<Transaction> allExpenses = new java.util.ArrayList<>();
    private List<Transaction> allIncome = new java.util.ArrayList<>();
    
    // Filter States
    private String currentExpenseFilter = "Month";
    private String currentExpenseSearch = "";
    private String currentIncomeFilter = "Month";
    private String currentIncomeSearch = "";

    public TransactionViewModel(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public LiveData<List<Transaction>> getExpenses() { return expensesLiveData; }
    public LiveData<List<Transaction>> getIncome() { return incomeLiveData; }
    public LiveData<List<Transaction>> getAllExpensesRaw() { return allExpensesLiveData; }
    public LiveData<List<Transaction>> getAllIncomeRaw() { return allIncomeLiveData; }

    public LiveData<List<Transaction>> getRecentTransactions() { return recentTransactionsLiveData; }
    public LiveData<List<Transaction>> getRecentIncome() { return recentIncomeLiveData; }
    public LiveData<List<Transaction>> getRecentExpense() { return recentExpenseLiveData; }
    public LiveData<Double> getTotalExpense() { return totalExpenseLiveData; }
    public LiveData<Double> getTotalIncome() { return totalIncomeLiveData; }
    public LiveData<Boolean> getOperationResult() { return operationResult; }

    public void loadExpenses(int userId) {
        transactionRepository.getTransactions(userId, "EXPENSE", result -> {
            allExpenses = result;
            allExpensesLiveData.postValue(result); // Post Raw Data
            applyExpenseFilters();
        });
    }

    public void loadIncome(int userId) {
        transactionRepository.getTransactions(userId, "INCOME", result -> {
            allIncome = result;
            allIncomeLiveData.postValue(result); // Post Raw Data
            applyIncomeFilters();
        });
    }

    // --- Filter Logic ---

    public void setExpenseFilter(String filterType) {
        this.currentExpenseFilter = filterType;
        applyExpenseFilters();
    }

    public void setExpenseSearch(String query) {
        this.currentExpenseSearch = query.toLowerCase().trim();
        applyExpenseFilters();
    }

    public void setIncomeFilter(String filterType) {
        this.currentIncomeFilter = filterType;
        applyIncomeFilters();
    }

    public void setIncomeSearch(String query) {
        this.currentIncomeSearch = query.toLowerCase().trim();
        applyIncomeFilters();
    }

    private void applyExpenseFilters() {
        if (allExpenses == null) return;
        List<Transaction> filtered = filterList(allExpenses, currentExpenseFilter, currentExpenseSearch);
        expensesLiveData.postValue(filtered);
    }

    private void applyIncomeFilters() {
        if (allIncome == null) return;
        List<Transaction> filtered = filterList(allIncome, currentIncomeFilter, currentIncomeSearch);
        incomeLiveData.postValue(filtered);
    }

    private List<Transaction> filterList(List<Transaction> originalList, String timeFilter, String searchQuery) {
        List<Transaction> filtered = new java.util.ArrayList<>();
        
        // Date Logic
        java.util.Calendar cal = java.util.Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        
        long startTime = 0; // 0 means "All"
        
        if (timeFilter.equals("Week")) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, -7);
            startTime = cal.getTimeInMillis();
        } else if (timeFilter.equals("Month")) {
            cal.add(java.util.Calendar.MONTH, -1);
            startTime = cal.getTimeInMillis();
        } else if (timeFilter.equals("Year")) {
            cal.add(java.util.Calendar.YEAR, -1);
            startTime = cal.getTimeInMillis();
        }

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());

        for (Transaction t : originalList) {
            boolean matchesSearch = true;
            boolean matchesTime = true;

            // Search Filter
            if (!searchQuery.isEmpty()) {
                String searchTarget = (t.getTitle() + " " + t.getMerchant() + " " + t.getSource() + " " + t.getCategory() + " " + t.getAmount()).toLowerCase();
                if (!searchTarget.contains(searchQuery)) {
                    matchesSearch = false;
                }
            }

            // Time Filter
            if (!timeFilter.equals("All")) {
                try {
                    Date tDate = sdf.parse(t.getDate());
                    if (tDate != null && tDate.getTime() < startTime) {
                        matchesTime = false;
                    }
                } catch (Exception e) {
                    // If date parse fails, maybe include it or exclude? Let's exclude for safety
                    matchesTime = false; 
                }
            }

            if (matchesSearch && matchesTime) {
                filtered.add(t);
            }
        }
        return filtered;
    }
    
    public void loadDashboardData(int userId) {
        transactionRepository.getLatestIncome(userId, result -> recentIncomeLiveData.postValue(result));
        transactionRepository.getLatestExpense(userId, result -> recentExpenseLiveData.postValue(result));
        
        // Load Totals
        transactionRepository.getTotalExpense(userId, result -> totalExpenseLiveData.postValue(result));
        transactionRepository.getTotalIncome(userId, result -> totalIncomeLiveData.postValue(result));
    }

    public void addTransaction(Transaction transaction) {
        transactionRepository.addTransaction(transaction, result -> {
            operationResult.postValue(result);
        });
    }
    
    public void updateTransaction(Transaction transaction) {
        transactionRepository.updateTransaction(transaction, result -> operationResult.postValue(result));
    }

    public void deleteTransaction(Transaction transaction) {
        transactionRepository.deleteTransaction(transaction, result -> {
             operationResult.postValue(true); 
        });
    }
}
