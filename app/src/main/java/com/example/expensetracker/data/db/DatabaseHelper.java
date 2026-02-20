package com.example.expensetracker.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.expensetracker.data.model.Transaction;
import com.example.expensetracker.data.model.User;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ExpenseTracker.db";
    private static final int DATABASE_VERSION = 1;

    // Users Table
    private static final String TABLE_USERS = "users";
    private static final String KEY_USER_ID = "id";
    private static final String KEY_USER_NAME = "name";
    private static final String KEY_USER_EMAIL = "email";
    private static final String KEY_USER_PASSWORD = "password";

    // Transactions Table (Used for both Expense and Income)
    private static final String TABLE_TRANSACTIONS = "transactions";
    private static final String KEY_TRANS_ID = "id";
    private static final String KEY_TRANS_USER_ID = "user_id";
    private static final String KEY_TRANS_TITLE = "title"; // Title/Source
    private static final String KEY_TRANS_MERCHANT = "merchant";
    private static final String KEY_TRANS_AMOUNT = "amount";
    private static final String KEY_TRANS_DATE = "date";
    private static final String KEY_TRANS_CATEGORY = "category";
    private static final String KEY_TRANS_TYPE = "type"; // EXPENSE or INCOME

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + KEY_USER_ID + " INTEGER PRIMARY KEY,"
                + KEY_USER_NAME + " TEXT,"
                + KEY_USER_EMAIL + " TEXT UNIQUE,"
                + KEY_USER_PASSWORD + " TEXT" + ")";
        db.execSQL(CREATE_USERS_TABLE);

        String CREATE_TRANSACTIONS_TABLE = "CREATE TABLE " + TABLE_TRANSACTIONS + "("
                + KEY_TRANS_ID + " INTEGER PRIMARY KEY,"
                + KEY_TRANS_USER_ID + " INTEGER,"
                + KEY_TRANS_TITLE + " TEXT,"
                + KEY_TRANS_MERCHANT + " TEXT,"
                + KEY_TRANS_AMOUNT + " REAL,"
                + KEY_TRANS_DATE + " TEXT,"
                + KEY_TRANS_CATEGORY + " TEXT,"
                + KEY_TRANS_TYPE + " TEXT" + ")";
        db.execSQL(CREATE_TRANSACTIONS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
        onCreate(db);
    }

    // --- User Operations ---
    public long addUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_USER_NAME, user.getName());
        values.put(KEY_USER_EMAIL, user.getEmail());
        values.put(KEY_USER_PASSWORD, user.getPassword());
        long id = db.insert(TABLE_USERS, null, values);
        db.close();
        return id;
    }

    public boolean checkUser(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[] { KEY_USER_ID }, KEY_USER_EMAIL + "=?",
                new String[] { email }, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        db.close();
        return count > 0;
    }

    public boolean checkUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[] { KEY_USER_ID },
                KEY_USER_EMAIL + "=? AND " + KEY_USER_PASSWORD + "=?",
                new String[] { email, password }, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        db.close();
        return count > 0;
    }

    public User getUser(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[] { KEY_USER_ID, KEY_USER_NAME, KEY_USER_EMAIL, KEY_USER_PASSWORD },
                KEY_USER_EMAIL + "=?",
                new String[] { email }, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            User user = new User(
                    Integer.parseInt(cursor.getString(0)),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3));
            cursor.close();
            db.close();
            return user;
        }
        return null; // Not found
    }

    // --- Transaction Operations ---
    public long addTransaction(Transaction transaction) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_TRANS_USER_ID, transaction.getUserId());
        values.put(KEY_TRANS_TITLE, transaction.getTitle());
        values.put(KEY_TRANS_MERCHANT, transaction.getMerchant());
        values.put(KEY_TRANS_AMOUNT, transaction.getAmount());
        values.put(KEY_TRANS_DATE, transaction.getDate());
        values.put(KEY_TRANS_CATEGORY, transaction.getCategory());
        values.put(KEY_TRANS_TYPE, transaction.getType());

        long id = db.insert(TABLE_TRANSACTIONS, null, values);
        db.close();
        return id;
    }

    public List<Transaction> getAllTransactions(int userId) {
        List<Transaction> transactionList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_TRANSACTIONS + " WHERE " + KEY_TRANS_USER_ID + " = " + userId
                + " ORDER BY " + KEY_TRANS_DATE + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Transaction transaction = new Transaction();
                transaction.setId(Integer.parseInt(cursor.getString(0)));
                transaction.setUserId(Integer.parseInt(cursor.getString(1)));
                transaction.setTitle(cursor.getString(2));
                transaction.setMerchant(cursor.getString(3));
                transaction.setAmount(Double.parseDouble(cursor.getString(4)));
                transaction.setDate(cursor.getString(5));
                transaction.setCategory(cursor.getString(6));
                transaction.setType(cursor.getString(7));
                transactionList.add(transaction);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return transactionList;
    }

    public List<Transaction> getTransactionsByType(int userId, String type) {
        List<Transaction> transactionList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_TRANSACTIONS + " WHERE " + KEY_TRANS_USER_ID + " = " + userId
                + " AND " + KEY_TRANS_TYPE + " = '" + type + "' ORDER BY " + KEY_TRANS_DATE + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Transaction transaction = new Transaction();
                transaction.setId(Integer.parseInt(cursor.getString(0)));
                transaction.setUserId(Integer.parseInt(cursor.getString(1)));
                transaction.setTitle(cursor.getString(2));
                transaction.setMerchant(cursor.getString(3));
                transaction.setAmount(Double.parseDouble(cursor.getString(4)));
                transaction.setDate(cursor.getString(5));
                transaction.setCategory(cursor.getString(6));
                transaction.setType(cursor.getString(7));
                transactionList.add(transaction);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return transactionList;
    }

    // Get latest transactions (limit)
    public List<Transaction> getLatestTransactions(int userId, int limit) {
        List<Transaction> transactionList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_TRANSACTIONS + " WHERE " + KEY_TRANS_USER_ID + " = " + userId
                + " ORDER BY " + KEY_TRANS_DATE + " DESC LIMIT " + limit;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Transaction transaction = new Transaction();
                transaction.setId(Integer.parseInt(cursor.getString(0)));
                transaction.setUserId(Integer.parseInt(cursor.getString(1)));
                transaction.setTitle(cursor.getString(2));
                transaction.setMerchant(cursor.getString(3));
                transaction.setAmount(Double.parseDouble(cursor.getString(4)));
                transaction.setDate(cursor.getString(5));
                transaction.setCategory(cursor.getString(6));
                transaction.setType(cursor.getString(7));
                transactionList.add(transaction);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return transactionList;
    }

    public List<Transaction> getLatestTransactionsByType(int userId, String type, int limit) {
        List<Transaction> transactionList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_TRANSACTIONS + " WHERE " + KEY_TRANS_USER_ID + " = " + userId
                + " AND " + KEY_TRANS_TYPE + " = '" + type + "' ORDER BY " + KEY_TRANS_DATE + " DESC LIMIT " + limit;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Transaction transaction = new Transaction();
                transaction.setId(Integer.parseInt(cursor.getString(0)));
                transaction.setUserId(Integer.parseInt(cursor.getString(1)));
                transaction.setTitle(cursor.getString(2));
                transaction.setMerchant(cursor.getString(3));
                transaction.setAmount(Double.parseDouble(cursor.getString(4)));
                transaction.setDate(cursor.getString(5));
                transaction.setCategory(cursor.getString(6));
                transaction.setType(cursor.getString(7));
                transactionList.add(transaction);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return transactionList;
    }

    public double getTotalAmountByType(int userId, String type) {
        double total = 0;
        // SQLite query to sum amounts where the stored date matches the current year
        // and month
        // Dates are assumed to be in YYYY-MM-DD or comparable string format
        String selectQuery = "SELECT SUM(" + KEY_TRANS_AMOUNT + ") FROM " + TABLE_TRANSACTIONS +
                " WHERE " + KEY_TRANS_USER_ID + " = " + userId +
                " AND " + KEY_TRANS_TYPE + " = '" + type + "'" +
                " AND strftime('%Y-%m', " + KEY_TRANS_DATE + ") = strftime('%Y-%m', 'now', 'localtime')";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        db.close();
        return total;
    }

    public int updateTransaction(Transaction transaction) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_TRANS_TITLE, transaction.getTitle());
        values.put(KEY_TRANS_MERCHANT, transaction.getMerchant());
        values.put(KEY_TRANS_AMOUNT, transaction.getAmount());
        values.put(KEY_TRANS_DATE, transaction.getDate());
        values.put(KEY_TRANS_CATEGORY, transaction.getCategory());
        // Type typically doesn't change

        return db.update(TABLE_TRANSACTIONS, values, KEY_TRANS_ID + " = ?",
                new String[] { String.valueOf(transaction.getId()) });
    }

    public void deleteTransaction(Transaction transaction) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRANSACTIONS, KEY_TRANS_ID + " = ?",
                new String[] { String.valueOf(transaction.getId()) });
        db.close();
    }
}
