package com.example.expensetracker.ui.income;

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
import com.example.expensetracker.ui.expense.TransactionAdapter;
import com.example.expensetracker.viewmodel.TransactionViewModel;
import com.example.expensetracker.viewmodel.ViewModelFactory;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class IncomeFragment extends Fragment {

    private TransactionViewModel transactionViewModel;
    private TransactionAdapter adapter;
    private int userId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_income, container, false);

        if (getActivity() != null && getActivity().getIntent() != null) {
            userId = getActivity().getIntent().getIntExtra("USER_ID", -1);
        }

        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        ViewModelFactory factory = new ViewModelFactory(dbHelper);
        transactionViewModel = new ViewModelProvider(this, factory).get(TransactionViewModel.class);

        RecyclerView rvIncome = view.findViewById(R.id.rvIncome);
        FloatingActionButton fabAdd = view.findViewById(R.id.fabAddIncome);
        View btnAnalytics = view.findViewById(R.id.btnAnalytics);
        TextView tvTotalAmount = view.findViewById(R.id.tvTotalAmount);

        adapter = new TransactionAdapter(null, this::showEditDeleteDialog);
        rvIncome.setLayoutManager(new LinearLayoutManager(getContext()));
        rvIncome.setAdapter(adapter);

        transactionViewModel.getIncome().observe(getViewLifecycleOwner(), income -> {
            adapter.updateList(income);

            // Calculate Total for Filtered List
            double total = 0;
            if (income != null) {
                for (Transaction t : income) {
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

            transactionViewModel.setIncomeFilter(filter);
        });

        // Search Listeners (End Icon Click)
        tilSearch.setEndIconOnClickListener(v -> {
            String query = etSearch.getText().toString();
            transactionViewModel.setIncomeSearch(query);
            // Close keyboard
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireContext()
                    .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        });

        // Search IME Action
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearch.getText().toString();
                transactionViewModel.setIncomeSearch(query);
                // Close keyboard
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireContext()
                        .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                return true;
            }
            return false;
        });

        // Real-time search
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                transactionViewModel.setIncomeSearch(s.toString());
            }

            public void afterTextChanged(android.text.Editable s) {
            }
        });

        btnAnalytics.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, AnalyticsFragment.newInstance(userId, "INCOME"))
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void loadData() {
        if (userId != -1) {
            transactionViewModel.loadIncome(userId);
        }
    }

    private void showAddDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_expense, null);

        com.google.android.material.textfield.TextInputLayout tilTitle = dialogView.findViewById(R.id.tilTitle);
        com.google.android.material.textfield.TextInputLayout tilSource = dialogView.findViewById(R.id.tilSource);
        com.google.android.material.textfield.TextInputLayout tilMerchant = dialogView.findViewById(R.id.tilMerchant);

        TextInputEditText etTitle = dialogView.findViewById(R.id.etTitle);
        TextInputEditText etAmount = dialogView.findViewById(R.id.etAmount);
        TextInputEditText etDate = dialogView.findViewById(R.id.etDate);
        android.widget.AutoCompleteTextView etCategory = dialogView.findViewById(R.id.etCategory);
        android.widget.AutoCompleteTextView etSource = dialogView.findViewById(R.id.etSource);

        // Hide irrelevant fields for Income
        if (tilMerchant != null)
            tilMerchant.setVisibility(View.GONE);
        if (tilTitle != null)
            tilTitle.setVisibility(View.GONE); // Use Source Dropdown instead
        if (tilSource != null)
            tilSource.setVisibility(View.VISIBLE);

        // Date Logic
        SimpleDateFormat uiFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        etDate.setText(uiFormat.format(new Date()));
        etDate.setFocusable(false);
        etDate.setClickable(true);
        etDate.setOnClickListener(v -> showDatePicker(etDate));

        // Category Dropdown
        String[] categories = { "Salary", "Business", "Investment", "Gift", "Bonus", "Others" };
        android.widget.ArrayAdapter<String> catAdapter = new android.widget.ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, categories);
        etCategory.setAdapter(catAdapter);

        // Source Dropdown
        String[] sources = { "Bank", "Cash", "UPI", "Cheque", "Online", "Others" };
        android.widget.ArrayAdapter<String> sourceAdapter = new android.widget.ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, sources);
        etSource.setAdapter(sourceAdapter);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add Income")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String source = etSource.getText().toString();
                    String amountStr = etAmount.getText().toString();
                    String uiDate = etDate.getText().toString();
                    String category = etCategory.getText().toString();

                    if (!amountStr.isEmpty()) {
                        double amount = Double.parseDouble(amountStr);
                        String dbDate = convertDateToDb(uiDate);

                        Transaction t = new Transaction();
                        t.setUserId(userId);
                        t.setTitle(source);
                        t.setAmount(amount);
                        t.setDate(dbDate);
                        t.setType("INCOME");
                        t.setSource(source);
                        t.setCategory(category);

                        transactionViewModel.addTransaction(t);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
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

        // Income Styling
        tvDetailAmount
                .setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.colorSuccess));
        tvDetailType.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E8F5E9")));
        tvDetailType.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.colorSuccess));
        tvLabelTitle.setText("Source");

        String displayTitle = transaction.getSource();
        if (displayTitle == null || displayTitle.isEmpty()) {
            displayTitle = transaction.getTitle();
        }
        tvDetailTitle.setText(displayTitle != null ? displayTitle : "Income");
        layoutMerchant.setVisibility(View.GONE);

        tvDetailCategory.setText(transaction.getCategory());
        tvDetailDate.setText(convertDateToUi(transaction.getDate()));

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

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
        com.google.android.material.textfield.TextInputLayout tilSource = dialogView.findViewById(R.id.tilSource);
        com.google.android.material.textfield.TextInputLayout tilMerchant = dialogView.findViewById(R.id.tilMerchant);

        TextInputEditText etTitle = dialogView.findViewById(R.id.etTitle);
        TextInputEditText etAmount = dialogView.findViewById(R.id.etAmount);
        TextInputEditText etDate = dialogView.findViewById(R.id.etDate);
        android.widget.AutoCompleteTextView etCategory = dialogView.findViewById(R.id.etCategory);
        android.widget.AutoCompleteTextView etSource = dialogView.findViewById(R.id.etSource);

        // Hide irrelevant fields for Income
        if (tilMerchant != null)
            tilMerchant.setVisibility(View.GONE);
        if (tilTitle != null)
            tilTitle.setVisibility(View.GONE);
        if (tilSource != null)
            tilSource.setVisibility(View.VISIBLE);

        etSource.setText(transaction.getSource(), false);
        etAmount.setText(String.valueOf(transaction.getAmount()));

        etDate.setText(convertDateToUi(transaction.getDate()));
        etDate.setFocusable(false);
        etDate.setClickable(true);
        etDate.setOnClickListener(v -> showDatePicker(etDate));

        // Category Dropdown
        String[] categories = { "Salary", "Business", "Investment", "Gift", "Bonus", "Others" };
        android.widget.ArrayAdapter<String> catAdapter = new android.widget.ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, categories);
        etCategory.setAdapter(catAdapter);
        etCategory.setText(transaction.getCategory(), false);

        // Source Dropdown
        String[] sources = { "Bank", "Cash", "UPI", "Cheque", "Online", "Others" };
        android.widget.ArrayAdapter<String> sourceAdapter = new android.widget.ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, sources);
        etSource.setAdapter(sourceAdapter);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Update Income")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String source = etSource.getText().toString();

                    transaction.setSource(source);
                    transaction.setTitle(source);
                    transaction.setAmount(Double.parseDouble(etAmount.getText().toString()));

                    String uiDate = etDate.getText().toString();
                    transaction.setDate(convertDateToDb(uiDate));
                    transaction.setCategory(etCategory.getText().toString());

                    transactionViewModel.updateTransaction(transaction);
                    Toast.makeText(getContext(), "Updated", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
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
            return uiDate;
        }
    }

    private String convertDateToUi(String dbDate) {
        try {
            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat uiFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            return uiFormat.format(dbFormat.parse(dbDate));
        } catch (Exception e) {
            return dbDate;
        }
    }
}
