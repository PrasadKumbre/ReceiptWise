package com.example.expensetracker.ui.analytics;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.expensetracker.R;
import com.example.expensetracker.data.db.DatabaseHelper;
import com.example.expensetracker.data.model.Transaction;
import com.example.expensetracker.viewmodel.TransactionViewModel;
import com.example.expensetracker.viewmodel.ViewModelFactory;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import android.util.TypedValue;
import android.content.Context;

public class AnalyticsFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";
    private static final String ARG_TYPE = "type";

    private int userId;
    private String type;

    private TransactionViewModel transactionViewModel;
    private PieChart pieChart;
    private BarChart barChart;
    private MaterialButtonToggleGroup toggleButton;

    private List<Transaction> allTransactions = new ArrayList<>();

    public static AnalyticsFragment newInstance(int userId, String type) {
        AnalyticsFragment fragment = new AnalyticsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_USER_ID, userId);
        args.putString(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getInt(ARG_USER_ID);
            type = getArguments().getString(ARG_TYPE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_analytics, container, false);

        pieChart = view.findViewById(R.id.pieChart);
        barChart = view.findViewById(R.id.barChart);
        toggleButton = view.findViewById(R.id.toggleButton);

        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        ViewModelFactory factory = new ViewModelFactory(dbHelper);
        transactionViewModel = new ViewModelProvider(this, factory)
                .get(TransactionViewModel.class);

        // Default selection first
        toggleButton.check(R.id.btnMonth);

        if ("EXPENSE".equals(type)) {
            transactionViewModel.getAllExpensesRaw().observe(getViewLifecycleOwner(), transactions -> {
                allTransactions = transactions;
                applyCurrentFilter();
            });
            transactionViewModel.loadExpenses(userId);
        } else {
            transactionViewModel.getAllIncomeRaw().observe(getViewLifecycleOwner(), transactions -> {
                allTransactions = transactions;
                applyCurrentFilter();
            });
            transactionViewModel.loadIncome(userId);
        }

        toggleButton.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                filterTransactions(checkedId);
            }
        });

        return view;
    }

    private void applyCurrentFilter() {
        int checkedId = toggleButton.getCheckedButtonId();
        if (checkedId == View.NO_ID) {
            checkedId = R.id.btnMonth;
        }
        filterTransactions(checkedId);
    }

    // Safe & Strict Date Parser
    private Date parseDateStrictly(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty())
            return null;

        SimpleDateFormat[] formats = new SimpleDateFormat[] {
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
                new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        };

        for (SimpleDateFormat sdf : formats) {
            sdf.setLenient(false); // Forces exact matches
            try {
                return sdf.parse(dateStr);
            } catch (ParseException ignored) {
                // Try the next format in the array
            }
        }
        return null; // Invalid date
    }

    private void filterTransactions(int checkedId) {
        if (checkedId == View.NO_ID) {
            checkedId = R.id.btnMonth;
        }

        List<Transaction> filteredList = new ArrayList<>();

        Calendar currentCal = Calendar.getInstance();
        int currYear = currentCal.get(Calendar.YEAR);
        int currMonth = currentCal.get(Calendar.MONTH);
        int currWeek = currentCal.get(Calendar.WEEK_OF_YEAR);

        for (Transaction t : allTransactions) {
            if (checkedId == R.id.btnAll) {
                filteredList.add(t);
                continue;
            }

            Date tDate = parseDateStrictly(t.getDate());
            if (tDate == null)
                continue;

            Calendar tCal = Calendar.getInstance();
            tCal.setTime(tDate);
            int tYear = tCal.get(Calendar.YEAR);
            int tMonth = tCal.get(Calendar.MONTH);
            int tWeek = tCal.get(Calendar.WEEK_OF_YEAR);

            if (checkedId == R.id.btnYear) {
                // Match Year
                if (tYear == currYear) {
                    filteredList.add(t);
                }
            } else if (checkedId == R.id.btnMonth) {
                // Match Year and Month
                if (tYear == currYear && tMonth == currMonth) {
                    filteredList.add(t);
                }
            } else if (checkedId == R.id.btnWeek) {
                // Match Year and Week (Simplified)
                // Note: deeply accurate week calculation might need to handle year boundaries
                // (week 1 of 2026 might start in 2025)
                // But for simple expense tracker, matching WEEK_OF_YEAR and YEAR is usually
                // sufficient.
                if (tYear == currYear && tWeek == currWeek) {
                    filteredList.add(t);
                }
            }
        }

        // Pass the checkedId so the chart knows how to format itself
        setupCharts(filteredList, checkedId);
    }

    private void setupCharts(List<Transaction> transactions, int checkedId) {
        setupPieChart(transactions);
        setupBarChart(transactions, checkedId);
    }

    private void setupPieChart(List<Transaction> transactions) {
        Map<String, Float> categoryMap = new HashMap<>();

        for (Transaction t : transactions) {
            String key = t.getCategory();
            if (key == null || key.isEmpty())
                key = "Unknown";

            categoryMap.put(key, categoryMap.getOrDefault(key, 0f) + (float) t.getAmount());
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Float> entry : categoryMap.entrySet()) {
            if (entry.getValue() > 0) { // Prevents empty slices from messing up the chart
                entries.add(new PieEntry(entry.getValue(), entry.getKey()));
            }
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);

        int textColor = getThemeColor(requireContext(), com.google.android.material.R.attr.colorOnSurface);
        int surfaceColor = getThemeColor(requireContext(), com.google.android.material.R.attr.colorSurface);

        dataSet.setValueTextColor(textColor);
        dataSet.setValueTextSize(12f);

        PieData data = new PieData(dataSet);

        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setWordWrapEnabled(true);
        pieChart.getLegend().setTextColor(textColor);
        pieChart.setEntryLabelColor(textColor);
        pieChart.setHoleColor(surfaceColor);
        pieChart.animateY(800);
        pieChart.invalidate();
    }

    // Dynamic Bar Chart Scaling & Grouping
    private void setupBarChart(List<Transaction> transactions, int checkedId) {
        Map<String, Float> dateMap = new TreeMap<>();

        // Determine if we should group by Month (Year/All) or by Day (Week/Month)
        boolean groupByMonth = (checkedId == R.id.btnYear || checkedId == R.id.btnAll);

        // Formats for sorting keys: "2026-02" vs "2026-02-20"
        SimpleDateFormat sortFormat = groupByMonth ? new SimpleDateFormat("yyyy-MM", Locale.getDefault())
                : new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Formats for X-Axis Labels: "Feb 2026" vs "20 Feb"
        SimpleDateFormat labelFormat = groupByMonth ? new SimpleDateFormat("MMM yyyy", Locale.getDefault())
                : new SimpleDateFormat("dd MMM", Locale.getDefault());

        for (Transaction t : transactions) {
            Date date = parseDateStrictly(t.getDate());
            if (date != null) {
                // Group the amounts by the chosen format
                String sortableDate = sortFormat.format(date);
                dateMap.put(sortableDate, dateMap.getOrDefault(sortableDate, 0f) + (float) t.getAmount());
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        int i = 0;
        for (Map.Entry<String, Float> entry : dateMap.entrySet()) {
            entries.add(new BarEntry(i, entry.getValue()));
            try {
                Date d = sortFormat.parse(entry.getKey());
                labels.add(labelFormat.format(d));
            } catch (ParseException e) {
                labels.add(entry.getKey());
            }
            i++;
        }

        // Change label based on grouping
        BarDataSet dataSet = new BarDataSet(entries, groupByMonth ? "Monthly Total" : "Daily Total");
        dataSet.setColors(ColorTemplate.VORDIPLOM_COLORS);

        int textColor = getThemeColor(requireContext(), com.google.android.material.R.attr.colorOnSurface);

        dataSet.setValueTextColor(textColor);
        dataSet.setValueTextSize(10f);

        BarData data = new BarData(dataSet);
        barChart.setData(data);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setTextColor(textColor);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(Math.min(labels.size(), 7));
        xAxis.setTextColor(textColor);

        barChart.getAxisLeft().setTextColor(textColor);
        barChart.getAxisRight().setTextColor(textColor);

        barChart.setDragEnabled(true);
        barChart.setScaleEnabled(true);

        // Adjust visible range to fit the new groupings
        if (groupByMonth) {
            barChart.setVisibleXRangeMaximum(12f); // Show 12 months at a glance
        } else if (checkedId == R.id.btnMonth) {
            barChart.setVisibleXRangeMaximum(14f); // Show 14 days at a glance
        } else {
            barChart.setVisibleXRangeMaximum(7f); // Show 7 days
        }

        if (!entries.isEmpty()) {
            // Auto-scroll to the most recent data
            barChart.moveViewToX(entries.size() - 1);
        }

        barChart.animateY(800);
        barChart.invalidate();
    }

    // Helper to get theme color
    private int getThemeColor(Context context, int attributeResId) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attributeResId, typedValue, true);
        return typedValue.data;
    }
}