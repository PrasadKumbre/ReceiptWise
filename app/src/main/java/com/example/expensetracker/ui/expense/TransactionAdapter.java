package com.example.expensetracker.ui.expense;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.R;
import com.example.expensetracker.data.model.Transaction;

import java.util.ArrayList;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<Transaction> transactions;
    private final OnItemLongClickListener longClickListener;

    public interface OnItemLongClickListener {
        void onItemLongClick(Transaction transaction);
    }

    public TransactionAdapter(List<Transaction> transactions, OnItemLongClickListener longClickListener) {
        this.transactions = transactions != null ? transactions : new ArrayList<>();
        this.longClickListener = longClickListener;
    }

    public void updateList(List<Transaction> newTransactions) {
        this.transactions = newTransactions != null ? newTransactions : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        
        // Expense: Title is usually Title or Merchant. Income: Source.
        if ("EXPENSE".equals(transaction.getType())) {
            holder.tvTitle.setText(transaction.getTitle());
            holder.tvCategory.setText(transaction.getCategory() != null ? transaction.getCategory() : "Expense");
            holder.tvAmount.setText("- \u20B9" + String.format("%.2f", transaction.getAmount()));
            holder.tvAmount.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.colorError));
            
            // Icon Styling for Expense
            holder.ivIcon.setImageResource(R.drawable.ic_expense);
            holder.ivIcon.setColorFilter(holder.itemView.getContext().getResources().getColor(R.color.colorError));
            holder.cvIcon.setCardBackgroundColor(Color.parseColor("#FFEBEE")); // Light Red
            
        } else {
            String displayTitle = transaction.getSource();
            if (displayTitle == null || displayTitle.isEmpty()) {
                displayTitle = transaction.getTitle();
            }
            holder.tvTitle.setText(displayTitle != null ? displayTitle : "Income");
            holder.tvCategory.setText(transaction.getCategory() != null ? transaction.getCategory() : "Income");
            holder.tvAmount.setText("+ \u20B9" + String.format("%.2f", transaction.getAmount()));
            holder.tvAmount.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.colorSuccess));
            
            // Icon Styling for Income
            holder.ivIcon.setImageResource(R.drawable.ic_income);
            holder.ivIcon.setColorFilter(holder.itemView.getContext().getResources().getColor(R.color.colorSuccess));
            holder.cvIcon.setCardBackgroundColor(Color.parseColor("#E8F5E9")); // Light Green
        }
        
        // Date Formatting: yyyy-MM-dd -> dd-MM-yyyy
        String originalDate = transaction.getDate();
        if (originalDate != null) {
            try {
                 java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                 java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault());
                 java.util.Date date = inputFormat.parse(originalDate);
                 holder.tvDate.setText(outputFormat.format(date));
            } catch (Exception e) {
                holder.tvDate.setText(originalDate);
            }
        } else {
            holder.tvDate.setText("");
        }
        
        holder.tvCategory.setVisibility(View.VISIBLE);

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(transaction);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvAmount, tvCategory;
        android.widget.ImageView ivIcon;
        androidx.cardview.widget.CardView cvIcon;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            cvIcon = itemView.findViewById(R.id.cvIcon);
        }
    }
}
