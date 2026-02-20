package com.example.expensetracker.ui.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.R;
import com.example.expensetracker.data.db.DatabaseHelper;
import com.example.expensetracker.data.model.Transaction;
import com.example.expensetracker.ui.expense.TransactionAdapter;
import com.example.expensetracker.utils.OCRManager;
import com.example.expensetracker.viewmodel.TransactionViewModel;
import com.example.expensetracker.viewmodel.ViewModelFactory;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TransactionViewModel transactionViewModel;
    private OCRManager ocrManager;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private TransactionAdapter expenseAdapter;
    private TransactionAdapter incomeAdapter;
    private int userId = -1;
    private com.example.expensetracker.utils.SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        sessionManager = new com.example.expensetracker.utils.SessionManager(getContext());

        // Get User ID from Activity or Session
        if (getActivity() != null && getActivity().getIntent() != null) {
            userId = getActivity().getIntent().getIntExtra("USER_ID", -1);
        }
        if (userId == -1) {
            userId = sessionManager.getUserId();
        }

        // Init ViewModel
        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        ViewModelFactory factory = new ViewModelFactory(dbHelper);
        transactionViewModel = new ViewModelProvider(this, factory).get(TransactionViewModel.class);

        // Init Views
        MaterialButton cvScanReceipt = view.findViewById(R.id.cvScanReceipt);
        MaterialButton cvUploadReceipt = view.findViewById(R.id.cvUploadReceipt);
        View ivLogout = view.findViewById(R.id.ivLogout);
        
        // Logout Listener
        if (ivLogout != null) {
            ivLogout.setOnClickListener(v -> {
                sessionManager.logoutUser();
                Intent intent = new Intent(getActivity(), com.example.expensetracker.ui.auth.LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        }
        
        RecyclerView rvRecentExpenses = view.findViewById(R.id.rvRecentExpenses);
        RecyclerView rvRecentIncome = view.findViewById(R.id.rvRecentIncome);
        
        // Rupee Animation
        View ivRupeeAnim = view.findViewById(R.id.ivRupeeAnim);
        if (ivRupeeAnim != null) {
            ObjectAnimator scaleDown = ObjectAnimator.ofPropertyValuesHolder(
                    ivRupeeAnim,
                    PropertyValuesHolder.ofFloat("scaleX", 1.2f),
                    PropertyValuesHolder.ofFloat("scaleY", 1.2f));
            scaleDown.setDuration(1200);
            scaleDown.setRepeatCount(ObjectAnimator.INFINITE);
            scaleDown.setRepeatMode(ObjectAnimator.REVERSE);
            scaleDown.setInterpolator(new AccelerateDecelerateInterpolator());
            scaleDown.start();
        }

        // Adapters
        expenseAdapter = new TransactionAdapter(Collections.emptyList(), null); // No click listener for now
        rvRecentExpenses.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecentExpenses.setAdapter(expenseAdapter);

        incomeAdapter = new TransactionAdapter(Collections.emptyList(), null);
        rvRecentIncome.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecentIncome.setAdapter(incomeAdapter);

        // Observers
        transactionViewModel.getRecentExpense().observe(getViewLifecycleOwner(), transactions -> {
            expenseAdapter.updateList(transactions);
        });

        transactionViewModel.getRecentIncome().observe(getViewLifecycleOwner(), transactions -> {
            incomeAdapter.updateList(transactions);
        });
        
        transactionViewModel.getOperationResult().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                loadData();
            }
        });
        
        // Dashboard Summary Observers
        android.widget.TextView tvTotalExpense = view.findViewById(R.id.tvTotalExpense);
        android.widget.TextView tvTotalIncome = view.findViewById(R.id.tvTotalIncome);
        
        transactionViewModel.getTotalExpense().observe(getViewLifecycleOwner(), total -> {
            tvTotalExpense.setText(String.format(Locale.getDefault(), "₹%.2f", total));
        });
        
        transactionViewModel.getTotalIncome().observe(getViewLifecycleOwner(), total -> {
            tvTotalIncome.setText(String.format(Locale.getDefault(), "₹%.2f", total));
        });


        // Load Data
        loadData();

        // OCR Setup
        ocrManager = new OCRManager(getContext());
        
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        processReceipt(imageBitmap);
                    }
                }
        );

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openCamera();
                    } else {
                        Toast.makeText(getContext(), "Camera permission required for OCR", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        cvScanReceipt.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });


        // File Picker
        ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        handleFileSelection(uri);
                    }
                }
        );

        cvUploadReceipt.setOnClickListener(v -> {
            filePickerLauncher.launch("*/*"); // Allow all, check MIME type manually if needed
        });

        // ... (existing code for cvScanReceipt)
        
        return view;
    }

    private void handleFileSelection(android.net.Uri uri) {
        try {
            String mimeType = getContext().getContentResolver().getType(uri);
            Bitmap bitmap = null;

            if (mimeType != null && mimeType.startsWith("image/")) {
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    android.graphics.ImageDecoder.Source source = android.graphics.ImageDecoder.createSource(getContext().getContentResolver(), uri);
                    bitmap = android.graphics.ImageDecoder.decodeBitmap(source);
                    // Ensure mutable config if needed (ML Kit usually fine with immutable)
                    bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true); 
                } else {
                    bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), uri);
                }
            } else if (mimeType != null && mimeType.equals("application/pdf")) {
                bitmap = pdfToBitmap(uri);
            } else {
                Toast.makeText(getContext(), "Unsupported file type: " + mimeType, Toast.LENGTH_SHORT).show();
                return;
            }

            if (bitmap != null) {
                processReceipt(bitmap);
            } else {
                Toast.makeText(getContext(), "Failed to read file.", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap pdfToBitmap(android.net.Uri uri) {
        try {
            android.os.ParcelFileDescriptor pfd = getContext().getContentResolver().openFileDescriptor(uri, "r");
            if (pfd != null) {
                android.graphics.pdf.PdfRenderer renderer = new android.graphics.pdf.PdfRenderer(pfd);
                if (renderer.getPageCount() > 0) {
                    android.graphics.pdf.PdfRenderer.Page page = renderer.openPage(0); // Render first page
                    
                    // High-res render for better OCR
                    int width = page.getWidth() * 2; 
                    int height = page.getHeight() * 2;
                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    
                    page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    
                    page.close();
                    renderer.close();
                    pfd.close();
                    return bitmap;
                }
                renderer.close();
                pfd.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    private void loadData() {
        if (userId != -1) {
            transactionViewModel.loadDashboardData(userId);
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(takePictureIntent);
    }

    private void processReceipt(Bitmap bitmap) {
        Toast.makeText(getContext(), "Processing Receipt...", Toast.LENGTH_SHORT).show();
        
        ocrManager.analyzeReceipt(bitmap, new OCRManager.OCRCallback() {
            @Override
            public void onSuccess(OCRManager.OCRResult result) {
                if (result == null || result.getRawText() == null || result.getRawText().isEmpty()) {
                    Toast.makeText(getContext(), "No text detected.", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // OCRManager now returns the parsed result on the Main Thread
                showConfirmDialog(result);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "OCR Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showConfirmDialog(OCRManager.OCRResult result) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_expense, null);
        
        com.google.android.material.textfield.TextInputLayout tilTitle = dialogView.findViewById(R.id.tilTitle);
        com.google.android.material.textfield.TextInputLayout tilAmount = dialogView.findViewById(R.id.tilAmount);
        com.google.android.material.textfield.TextInputLayout tilDate = dialogView.findViewById(R.id.tilDate);
        
        TextInputEditText etTitle = dialogView.findViewById(R.id.etTitle);
        TextInputEditText etMerchant = dialogView.findViewById(R.id.etMerchant);
        TextInputEditText etAmount = dialogView.findViewById(R.id.etAmount);
        TextInputEditText etDate = dialogView.findViewById(R.id.etDate);
        android.widget.AutoCompleteTextView etCategory = dialogView.findViewById(R.id.etCategory);
        
        // Hide Source Dropdown for Expenses
        com.google.android.material.textfield.TextInputLayout tilSource = dialogView.findViewById(R.id.tilSource);
        if (tilSource != null) tilSource.setVisibility(View.GONE);

        etTitle.setText("Receipt Scan");
        etMerchant.setText(result.getMerchant() != null ? result.getMerchant() : "");
        etAmount.setText(String.valueOf(result.getAmount()));
        
        // Date Logic: Prefer OCR date (formatted as dd-MM-yyyy for UI), else Today
        String ocrDateDb = result.getDate(); // yyyy-MM-dd
        String uiDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date()); // Default Today
        
        if (ocrDateDb != null) {
            uiDate = convertDateToUi(ocrDateDb);
        }
        
        etDate.setText(uiDate);
        etDate.setFocusable(false);
        etDate.setClickable(true);
        etDate.setOnClickListener(v -> showDatePicker(etDate));
        
        // Category Dropdown
        String[] categories = {"Food", "Transport", "Shopping", "Entertainment", "Bills", "Health", "Education", "Travel", "Investment", "Utilities", "Others"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categories);
        etCategory.setAdapter(adapter);
        
        etCategory.setText(result.getCategory(), false);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Confirm Receipt Details")
                .setView(dialogView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();
        
        dialog.show();
        
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            boolean isValid = true;
            String title = etTitle.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();
            String finalUiDate = etDate.getText().toString().trim();
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
             SimpleDateFormat uiFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            if (finalUiDate.isEmpty()) {
                tilDate.setError("Date is required");
                isValid = false;
            } else {
                 try {
                     Date inputDate = uiFormat.parse(finalUiDate);
                     java.util.Calendar inputCal = java.util.Calendar.getInstance();
                     inputCal.setTime(inputDate);
                     java.util.Calendar todayCal = java.util.Calendar.getInstance();
                     
                     if (inputCal.get(java.util.Calendar.YEAR) > todayCal.get(java.util.Calendar.YEAR) ||
                         (inputCal.get(java.util.Calendar.YEAR) == todayCal.get(java.util.Calendar.YEAR) && 
                          inputCal.get(java.util.Calendar.DAY_OF_YEAR) > todayCal.get(java.util.Calendar.DAY_OF_YEAR))) {
                             tilDate.setError("Date cannot be in future");
                             isValid = false;
                     } else {
                         tilDate.setError(null);
                     }
                } catch(Exception e) {
                    tilDate.setError("Invalid date");
                    isValid = false;
                }
            }

            if (isValid) {
                String dbDate = convertDateToDb(finalUiDate);
                
                Transaction transaction = new Transaction(userId, title, etMerchant.getText().toString(), amount, dbDate, category);
                transactionViewModel.addTransaction(transaction);
                Toast.makeText(getContext(), "Expense Added", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
    }
    
    private void showDatePicker(TextInputEditText etDate) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        try {
             SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
             Date date = sdf.parse(etDate.getText().toString());
             if(date != null) calendar.setTime(date);
        } catch (Exception e) {
             // Use current date
        }
        
        new android.app.DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            java.util.Calendar selectedDate = java.util.Calendar.getInstance();
            selectedDate.set(year, month, dayOfMonth);
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            etDate.setText(sdf.format(selectedDate.getTime()));
        }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show();
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
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // OCRManager (ML Kit) doesn't need explicit stop/close in current version
    }
}
