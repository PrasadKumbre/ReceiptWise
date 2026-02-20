package com.example.expensetracker.ui.expense;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.R;
import com.example.expensetracker.data.db.DatabaseHelper;
import com.example.expensetracker.data.model.Transaction;
import com.example.expensetracker.ui.analytics.AnalyticsFragment;
import com.example.expensetracker.viewmodel.TransactionViewModel;
import com.example.expensetracker.viewmodel.ViewModelFactory;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ExpenseFragment extends Fragment {

    private TransactionViewModel transactionViewModel;
    private TransactionAdapter adapter;
    private int userId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expense, container, false);

        if (getActivity() != null && getActivity().getIntent() != null) {
            userId = getActivity().getIntent().getIntExtra("USER_ID", -1);
        }

        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        ViewModelFactory factory = new ViewModelFactory(dbHelper);
        transactionViewModel = new ViewModelProvider(this, factory).get(TransactionViewModel.class);

        RecyclerView rvExpenses = view.findViewById(R.id.rvExpenses);
        FloatingActionButton fabAdd = view.findViewById(R.id.fabAddExpense);
        View btnAnalytics = view.findViewById(R.id.btnAnalytics);
        TextView tvTotalAmount = view.findViewById(R.id.tvTotalAmount);

        adapter = new TransactionAdapter(null, this::showEditDeleteDialog);
        rvExpenses.setLayoutManager(new LinearLayoutManager(getContext()));
        rvExpenses.setAdapter(adapter);

        transactionViewModel.getExpenses().observe(getViewLifecycleOwner(), expenses -> {
            adapter.updateList(expenses);

            // Calculate Total for Filtered List
            double total = 0;
            if (expenses != null) {
                for (Transaction t : expenses) {
                    total += t.getAmount();
                }
            }
            if (tvTotalAmount != null) {
                tvTotalAmount.setText(String.format(Locale.getDefault(), "Total Amount: ₹%.2f", total));
            }
        });

        transactionViewModel.getOperationResult().observe(getViewLifecycleOwner(), success -> {
            if (success)
                loadData();
        });

        loadData();

        fabAdd.setOnClickListener(v -> showAddDialog());

        com.google.android.material.chip.ChipGroup cgFilters = view.findViewById(R.id.cgFilters);
        com.google.android.material.textfield.TextInputLayout tilSearch = view.findViewById(R.id.tilSearch);
        android.widget.EditText etSearch = view.findViewById(R.id.etSearch);

        // Filter Listeners
        cgFilters.setOnCheckedChangeListener((group, checkedId) -> {
            String filter = "Month"; // Default
            if (checkedId == R.id.chipWeek)
                filter = "Week";
            else if (checkedId == R.id.chipMonth)
                filter = "Month";
            else if (checkedId == R.id.chipYear)
                filter = "Year";
            else if (checkedId == R.id.chipAll)
                filter = "All";

            transactionViewModel.setExpenseFilter(filter);
        });

        // Search Listeners (End Icon Click)
        tilSearch.setEndIconOnClickListener(v -> {
            String query = etSearch.getText().toString();
            transactionViewModel.setExpenseSearch(query);
            // Close keyboard
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireContext()
                    .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        });

        // Search IME Action
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearch.getText().toString();
                transactionViewModel.setExpenseSearch(query);
                // Close keyboard
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireContext()
                        .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                return true;
            }
            return false;
        });

        // Optional: Real-time search
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                transactionViewModel.setExpenseSearch(s.toString());
            }

            public void afterTextChanged(android.text.Editable s) {
            }
        });

        btnAnalytics.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, AnalyticsFragment.newInstance(userId, "EXPENSE"))
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void loadData() {
        if (userId != -1) {
            transactionViewModel.loadExpenses(userId);
        }
    }

    private void showAddDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_expense, null);

        com.google.android.material.textfield.TextInputLayout tilTitle = dialogView.findViewById(R.id.tilTitle);
        com.google.android.material.textfield.TextInputLayout tilAmount = dialogView.findViewById(R.id.tilAmount);
        com.google.android.material.textfield.TextInputLayout tilDate = dialogView.findViewById(R.id.tilDate);
        com.google.android.material.textfield.TextInputLayout tilSource = dialogView.findViewById(R.id.tilSource);

        TextInputEditText etTitle = dialogView.findViewById(R.id.etTitle);
        TextInputEditText etMerchant = dialogView.findViewById(R.id.etMerchant);
        TextInputEditText etAmount = dialogView.findViewById(R.id.etAmount);
        TextInputEditText etDate = dialogView.findViewById(R.id.etDate);
        android.widget.AutoCompleteTextView etCategory = dialogView.findViewById(R.id.etCategory);

        // Ensure proper visibility for Expense logic
        if (tilSource != null)
            tilSource.setVisibility(View.GONE);
        if (tilTitle != null)
            tilTitle.setVisibility(View.VISIBLE);

        // Default Date: dd-MM-yyyy
        SimpleDateFormat uiFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        etDate.setText(uiFormat.format(new Date()));
        etDate.setFocusable(false);
        etDate.setClickable(true);
        etDate.setOnClickListener(v -> showDatePicker(etDate));

        // Category Dropdown
        String[] categories = { "Food", "Transport", "Shopping", "Entertainment", "Bills", "Health", "Education",
                "Travel", "Investment", "Utilities", "Others" };
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, categories);
        etCategory.setAdapter(adapter);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add Expense")
                .setView(dialogView)
                .setPositiveButton("Save", null) // Set listener later to prevent auto-dismiss
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            boolean isValid = true;
            String title = etTitle.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();
            String uiDate = etDate.getText().toString().trim();
            String category = etCategory.getText().toString();

            // Validate Title
            if (title.isEmpty()) {
                tilTitle.setError("Title is required");
                isValid = false;
            } else {
                tilTitle.setError(null);
            }

            // Validate Amount
            double amount = 0;
            if (amountStr.isEmpty()) {
                tilAmount.setError("Amount is required");
                isValid = false;
            } else {
                try {
                    amount = Double.parseDouble(amountStr);
                    if (amount <= 0) {
                        tilAmount.setError("Amount must be > 0");
                        isValid = false;
                    } else {
                        tilAmount.setError(null);
                    }
                } catch (NumberFormatException e) {
                    tilAmount.setError("Invalid amount");
                    isValid = false;
                }
            }

            // Validate Date
            if (uiDate.isEmpty()) {
                tilDate.setError("Date is required");
                isValid = false;
            } else {
                try {
                    Date inputDate = uiFormat.parse(uiDate);
                    Date today = new Date();
                    // Reset time part for strict date comparison if needed, but simple comparison
                    // works for "future" check
                    // Let's reset today to midnight to allow "today" entries even if time differs
                    // slightly
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                    cal.set(java.util.Calendar.MINUTE, 0);
                    cal.set(java.util.Calendar.SECOND, 0);
                    cal.set(java.util.Calendar.MILLISECOND, 0);
                    Date todayMidnight = cal.getTime();

                    if (inputDate != null && inputDate.after(new Date())) { // Strictly Check against current moment?
                                                                            // User said "not grater then todays date"
                        // If user picks tomorrow, it is future. If user picks today, it is allowed.
                        // SimpleDateFormat parse returns 00:00:00. new Date() returns current time.
                        // So if inputDate > new Date(), it's definitely future.
                        // But if I pick today (e.g. 20th Feb 00:00) and now is 20th Feb 23:00,
                        // inputDate (20th 00:00) is BEFORE now. So it works.
                        // If I pick tomorrow (21st Feb 00:00), it is AFTER now.
                        // Wait, what if I pick today 20th, and compare with 20th 23:00?
                        // 20th 00:00 < 20th 23:00. So safe.
                        // But we need to allow "Today". check: if (inputDate > today_end_of_day) ->
                        // Invalid.
                        // Actually user said "not grater then todays date". So Future dates are
                        // blocked.
                        // A simple way: check if date part is after today's date part.

                        java.util.Calendar inputCal = java.util.Calendar.getInstance();
                        inputCal.setTime(inputDate);

                        java.util.Calendar todayCal = java.util.Calendar.getInstance();

                        // Clear time for comparison of just DATES
                        if (inputCal.get(java.util.Calendar.YEAR) > todayCal.get(java.util.Calendar.YEAR) ||
                                (inputCal.get(java.util.Calendar.YEAR) == todayCal.get(java.util.Calendar.YEAR) &&
                                        inputCal.get(java.util.Calendar.DAY_OF_YEAR) > todayCal
                                                .get(java.util.Calendar.DAY_OF_YEAR))) {
                            tilDate.setError("Date cannot be in future");
                            isValid = false;
                        } else {
                            tilDate.setError(null);
                        }
                    } else {
                        tilDate.setError(null);
                    }
                } catch (Exception e) {
                    tilDate.setError("Invalid date");
                    isValid = false;
                }
            }

            if (isValid) {
                String merchant = etMerchant.getText().toString();
                String dbDate = convertDateToDb(uiDate);

                Transaction t = new Transaction(userId, title, merchant, amount, dbDate, category);
                transactionViewModel.addTransaction(t);
                dialog.dismiss();
            }
        });
    }

    private void showEditDeleteDialog(Transaction transaction) {
        String[] options = { "View Details", "Update", "Delete" };
        new MaterialAlertDialogBuilder(requireContext())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showTransactionDetailsDialog(transaction);
                    } else if (which == 1) {
                        showUpdateDialog(transaction);
                    } else {
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Delete Transaction")
                                .setMessage("Are you sure you want to delete this transaction?")
                                .setPositiveButton("Delete", (d, w) -> {
                                    transactionViewModel.deleteTransaction(transaction);
                                    Toast.makeText(getContext(), "Deleted", Toast.LENGTH_SHORT).show();
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                })
                .show();
    }

    private void showTransactionDetailsDialog(Transaction transaction) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_transaction_details, null);

        TextView tvDetailAmount = dialogView.findViewById(R.id.tvDetailAmount);
        TextView tvDetailType = dialogView.findViewById(R.id.tvDetailType);
        TextView tvDetailTitle = dialogView.findViewById(R.id.tvDetailTitle);
        TextView tvDetailCategory = dialogView.findViewById(R.id.tvDetailCategory);
        TextView tvDetailDate = dialogView.findViewById(R.id.tvDetailDate);
        TextView tvDetailMerchant = dialogView.findViewById(R.id.tvDetailMerchant);
        TextView tvLabelTitle = dialogView.findViewById(R.id.tvLabelTitle);
        android.widget.ImageButton btnClose = dialogView.findViewById(R.id.btnClose);
        android.widget.LinearLayout layoutMerchant = dialogView.findViewById(R.id.layoutMerchant);

        // Populate Data
        tvDetailAmount.setText("\u20B9" + String.format("%.2f", transaction.getAmount()));
        tvDetailType.setText(transaction.getType());

        // Color Coding
        if ("EXPENSE".equals(transaction.getType())) {
            tvDetailAmount
                    .setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.colorError));
            tvDetailType.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFEBEE")));
            tvDetailType
                    .setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.colorError));
            tvLabelTitle.setText("Title");
            tvDetailTitle.setText(transaction.getTitle());

            if (transaction.getMerchant() != null && !transaction.getMerchant().isEmpty()) {
                layoutMerchant.setVisibility(View.VISIBLE);
                tvDetailMerchant.setText(transaction.getMerchant());
            } else {
                layoutMerchant.setVisibility(View.GONE);
            }
        } else {
            tvDetailAmount
                    .setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.colorSuccess));
            tvDetailType.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E8F5E9")));
            tvDetailType
                    .setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.colorSuccess));
            tvLabelTitle.setText("Source");

            String displayTitle = transaction.getSource();
            if (displayTitle == null || displayTitle.isEmpty()) {
                displayTitle = transaction.getTitle();
            }
            tvDetailTitle.setText(displayTitle != null ? displayTitle : "Income");
            layoutMerchant.setVisibility(View.GONE);
        }

        tvDetailCategory.setText(transaction.getCategory());

        // Date Formatting
        tvDetailDate.setText(convertDateToUi(transaction.getDate()));

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Transparent background for CardView radius to show
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showUpdateDialog(Transaction transaction) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_expense, null);

        com.google.android.material.textfield.TextInputLayout tilTitle = dialogView.findViewById(R.id.tilTitle);
        com.google.android.material.textfield.TextInputLayout tilAmount = dialogView.findViewById(R.id.tilAmount);
        com.google.android.material.textfield.TextInputLayout tilDate = dialogView.findViewById(R.id.tilDate);
        com.google.android.material.textfield.TextInputLayout tilSource = dialogView.findViewById(R.id.tilSource);

        TextInputEditText etTitle = dialogView.findViewById(R.id.etTitle);
        TextInputEditText etMerchant = dialogView.findViewById(R.id.etMerchant);
        TextInputEditText etAmount = dialogView.findViewById(R.id.etAmount);
        TextInputEditText etDate = dialogView.findViewById(R.id.etDate);
        android.widget.AutoCompleteTextView etCategory = dialogView.findViewById(R.id.etCategory);

        // Ensure proper visibility for Expense logic
        if (tilSource != null)
            tilSource.setVisibility(View.GONE);
        if (tilTitle != null)
            tilTitle.setVisibility(View.VISIBLE);

        etTitle.setText(transaction.getTitle());
        etMerchant.setText(transaction.getMerchant());
        etAmount.setText(String.valueOf(transaction.getAmount()));

        // Load Date: DB (yyyy-MM-dd) -> UI (dd-MM-yyyy)
        etDate.setText(convertDateToUi(transaction.getDate()));
        etDate.setFocusable(false);
        etDate.setClickable(true);
        etDate.setOnClickListener(v -> showDatePicker(etDate));

        // Category Dropdown
        String[] categories = { "Food", "Transport", "Shopping", "Entertainment", "Bills", "Health", "Education",
                "Travel", "Investment", "Utilities", "Others" };
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, categories);
        etCategory.setAdapter(adapter);

        etCategory.setText(transaction.getCategory(), false); // false to not filter

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Update Expense")
                .setView(dialogView)
                .setPositiveButton("Update", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            boolean isValid = true;
            String title = etTitle.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();
            String uiDate = etDate.getText().toString().trim();

            // Validations
            if (title.isEmpty()) {
                tilTitle.setError("Title is required");
                isValid = false;
            } else {
                tilTitle.setError(null);
            }

            double amount = 0;
            if (amountStr.isEmpty()) {
                tilAmount.setError("Amount is required");
                isValid = false;
            } else {
                try {
                    amount = Double.parseDouble(amountStr);
                    if (amount <= 0) {
                        tilAmount.setError("Amount must be > 0");
                        isValid = false;
                    } else {
                        tilAmount.setError(null);
                    }
                } catch (NumberFormatException e) {
                    tilAmount.setError("Invalid amount");
                    isValid = false;
                }
            }

            SimpleDateFormat uiFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            if (uiDate.isEmpty()) {
                tilDate.setError("Date is required");
                isValid = false;
            } else {
                try {
                    Date inputDate = uiFormat.parse(uiDate);
                    java.util.Calendar inputCal = java.util.Calendar.getInstance();
                    inputCal.setTime(inputDate);
                    java.util.Calendar todayCal = java.util.Calendar.getInstance();

                    if (inputCal.get(java.util.Calendar.YEAR) > todayCal.get(java.util.Calendar.YEAR) ||
                            (inputCal.get(java.util.Calendar.YEAR) == todayCal.get(java.util.Calendar.YEAR) &&
                                    inputCal.get(java.util.Calendar.DAY_OF_YEAR) > todayCal
                                            .get(java.util.Calendar.DAY_OF_YEAR))) {
                        tilDate.setError("Date cannot be in future");
                        isValid = false;
                    } else {
                        tilDate.setError(null);
                    }
                } catch (Exception e) {
                    tilDate.setError("Invalid date");
                    isValid = false;
                }
            }

            if (isValid) {
                transaction.setTitle(title);
                transaction.setMerchant(etMerchant.getText().toString());
                transaction.setAmount(amount);
                transaction.setDate(convertDateToDb(uiDate));
                transaction.setCategory(etCategory.getText().toString());

                transactionViewModel.updateTransaction(transaction);
                Toast.makeText(getContext(), "Updated", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
    }

    private void showDatePicker(TextInputEditText etDate) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            Date date = sdf.parse(etDate.getText().toString());
            if (date != null)
                calendar.setTime(date);
        } catch (Exception e) {
            // Use current date
        }

        new android.app.DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            java.util.Calendar selectedDate = java.util.Calendar.getInstance();
            selectedDate.set(year, month, dayOfMonth);
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            etDate.setText(sdf.format(selectedDate.getTime()));
        }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH),
                calendar.get(java.util.Calendar.DAY_OF_MONTH)).show();
    }

    private String convertDateToDb(String uiDate) {
        try {
            SimpleDateFormat uiFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return dbFormat.format(uiFormat.parse(uiDate));
        } catch (Exception e) {
            return uiDate; // Fallback
        }
    }

    private String convertDateToUi(String dbDate) {
        try {
            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat uiFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            return uiFormat.format(dbFormat.parse(dbDate));
        } catch (Exception e) {
            return dbDate; // Fallback
        }
    }
}
