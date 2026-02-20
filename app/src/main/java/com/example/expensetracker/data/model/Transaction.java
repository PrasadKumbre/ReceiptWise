package com.example.expensetracker.data.model;

public class Transaction {
    private int id;
    private int userId;
    private String title; // Also serves as Source for Income and Merchant for Expense if needed generically, but strict fields requested.
    // Making this generic enough for both or strictly following requirements.
    // Req: Expense (title, merchant, amount, date, category), Income (source, amount, date)
    // I will use a unified model but fields might be null for Income.
    
    private String merchant; // For Expense
    private String source; // For Income
    private double amount;
    private String date;
    private String category; // For Expense
    private String type; // "EXPENSE" or "INCOME"

    public Transaction() {
    }

    // Constructor for Expense
    public Transaction(int userId, String title, String merchant, double amount, String date, String category) {
        this.userId = userId;
        this.title = title;
        this.merchant = merchant;
        this.amount = amount;
        this.date = date;
        this.category = category;
        this.type = "EXPENSE";
    }

    // Constructor for Income
    public Transaction(int userId, String source, double amount, String date) {
        this.userId = userId;
        this.source = source;
        this.amount = amount;
        this.date = date;
        this.type = "INCOME";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMerchant() { return merchant; }
    public void setMerchant(String merchant) { this.merchant = merchant; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
