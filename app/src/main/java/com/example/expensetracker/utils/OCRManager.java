package com.example.expensetracker.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OCRManager {
    private static final String TAG = "OCRManager";
    private final TextRecognizer recognizer;
    private final Context context;

    public interface OCRCallback {
        void onSuccess(OCRResult result);
        void onFailure(Exception e);
    }

    public OCRManager(Context context) {
        this.context = context;
        // Initialize ML Kit Text Recognizer (for Latin script)
        this.recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    public void analyzeReceipt(Bitmap bitmap, OCRCallback callback) {
        if (bitmap == null) {
            callback.onFailure(new Exception("Bitmap is null"));
            return;
        }

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    // Offload parsing to background thread
                    new Thread(() -> {
                        OCRResult result = parseTextObject(visionText);
                        // Return to main thread if needed, or let caller handle it.
                        // Usually callbacks are expected on Main, but let's stick to background for heavy lifting
                        // and let Fragment runOnUiThread.
                        // Actually, ML Kit callbacks are on Main. Let's run parsing on BG, then callback on Main.
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            callback.onSuccess(result)
                        );
                    }).start();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "ML Kit OCR Failed", e);
                    callback.onFailure(e);
                });
    }

    private OCRResult parseTextObject(Text visionText) {
        OCRResult result = new OCRResult();
        String fullText = visionText.getText();
        result.setRawText(fullText);

        Log.d(TAG, "Parsing Text Object...");

        // --- 1. Merchant Name Extraction (Geometric Strategy) ---
        // Goal: Find the largest text in the top 30% of lines.

        java.util.List<Text.Line> allLines = new java.util.ArrayList<>();
        for (Text.TextBlock block : visionText.getTextBlocks()) {
            allLines.addAll(block.getLines());
        }

        // Sort by vertical position (top to bottom)
        java.util.Collections.sort(allLines, (l1, l2) -> {
            int top1 = (l1.getBoundingBox() != null) ? l1.getBoundingBox().top : 0;
            int top2 = (l2.getBoundingBox() != null) ? l2.getBoundingBox().top : 0;
            return Integer.compare(top1, top2);
        });

        String[] merchantBlocklist = {
            "welcome", "receipt", "invoice", "tax", "copy", "customer",
            "original", "duplicate", "bill", "cash", "credit", "card",
            "sale", "transaction", "merchant", "store", "shop", "tel:", "ph:", "date:", "time:",
            "payment", "total", "amount", "thank", "you", "visit", "again", "order", "table", "guest"
        };

        Text.Line bestMerchantLine = null;
        int maxLineHeight = 0;

        // Only look at the top 10 lines (or fewer if short receipt)
        int linesToScan = Math.min(allLines.size(), 10);

        for (int i = 0; i < linesToScan; i++) {
            Text.Line line = allLines.get(i);
            String text = line.getText().trim();
            if (text.length() < 3) continue;

            String lower = text.toLowerCase();
            boolean isBlocklisted = false;
            for (String blockWord : merchantBlocklist) {
                if (lower.contains(blockWord)) {
                    isBlocklisted = true;
                    break;
                }
            }

            if (isBlocklisted) continue;

            // Exclude dates/amounts
            if (text.matches(".*\\d\\d\\.\\d\\d.*") || text.matches(".*\\d{4}.*")) continue;

            // Check Height (Font Size)
            int height = (line.getBoundingBox() != null) ? line.getBoundingBox().height() : 0;

            // Give preference to earlier lines if heights are very similar (within 10%)
            // Otherwise, pick strictly larger height
            if (height > maxLineHeight) {
                maxLineHeight = height;
                bestMerchantLine = line;
            }
        }

        if (bestMerchantLine != null) {
            result.setMerchant(toTitleCase(bestMerchantLine.getText()));
        }

        // --- 2. Advanced Date Extraction (Existing Logic) ---
        String[] linesStr = fullText.split("\\n");
        String extractedDate = null;

        String dateRegex = "\\b(\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4}|\\d{4}[./-]\\d{1,2}[./-]\\d{1,2}|\\d{1,2}\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\.?[\\s,.-]+\\d{2,4})\\b";
        Pattern datePattern = Pattern.compile(dateRegex, Pattern.CASE_INSENSITIVE);

        // Pass 1: "Date:" Context
        for (String line : linesStr) {
            if (line.toLowerCase().matches(".*\\b(date|dt|time)\\b.*")) {
                Matcher m = datePattern.matcher(line);
                if (m.find()) {
                    extractedDate = normalizeDate(m.group(1));
                    break;
                }
            }
        }
        // Pass 2: Global
        if (extractedDate == null) {
            Matcher m = datePattern.matcher(fullText);
            if (m.find()) {
                extractedDate = normalizeDate(m.group(1));
            }
        }
        if (extractedDate != null) result.setDate(extractedDate);

        // --- 3. Amount Extraction (Strict "Total" Priority) ---
        double foundAmount = 0.0;
        java.util.Map<String, Integer> keywordScores = new java.util.HashMap<>();
        keywordScores.put("grand total", 50);
        keywordScores.put("total amount", 45);
        keywordScores.put("total due", 45);
        keywordScores.put("net amount", 40);
        keywordScores.put("balance due", 40);
        keywordScores.put("amount due", 40);
        keywordScores.put("total", 30);
        keywordScores.put("payment", 20);

        int bestScore = -1;
        double bestCandidateAmount = 0.0;
        Pattern pricePattern = Pattern.compile("(\\d{1,3}(?:[,.]\\d{3})*(?:[.,]\\d{2}))");

        for (int i = 0; i < linesStr.length; i++) {
            String line = linesStr[i].toLowerCase();
            String cleanLine = line.replaceAll("[^a-z ]", "");

            if (cleanLine.contains("subtotal") || cleanLine.contains("sub total") ||
                cleanLine.contains("tax") || cleanLine.contains("vat") || cleanLine.contains("tip") || cleanLine.contains("gratuity")) {
                continue;
            }

            int currentScore = 0;
            for (java.util.Map.Entry<String, Integer> entry : keywordScores.entrySet()) {
                if (cleanLine.contains(entry.getKey())) {
                    if (entry.getValue() > currentScore) currentScore = entry.getValue();
                }
            }

            if (currentScore > 0) {
                Matcher m = pricePattern.matcher(linesStr[i]);
                if (m.find()) {
                    if (currentScore >= bestScore) {
                        bestScore = currentScore;
                        bestCandidateAmount = parseAmount(m.group(1));
                    }
                } else if (i + 1 < linesStr.length) {
                    String nextLine = linesStr[i+1];
                    Matcher nextM = pricePattern.matcher(nextLine);
                    if (nextM.find()) {
                        if (currentScore >= bestScore) {
                            bestScore = currentScore;
                            bestCandidateAmount = parseAmount(nextM.group(1));
                        }
                    }
                }
            }
        }

        if (bestCandidateAmount > 0) {
            foundAmount = bestCandidateAmount;
        } else {
             // FALLBACK: Bottom-up 5 lines
             double maxBottomVal = 0.0;
             int startLine = Math.max(0, linesStr.length - 5);
             for (int i = startLine; i < linesStr.length; i++) {
                 Matcher m = pricePattern.matcher(linesStr[i]);
                 while (m.find()) {
                     double val = parseAmount(m.group(1));
                     if (val > maxBottomVal && val < 50000 && val != 2023 && val != 2024) {
                         maxBottomVal = val;
                     }
                 }
             }
             foundAmount = maxBottomVal;
        }
        if (foundAmount > 0) result.setAmount(foundAmount);

        // --- 4. Category Prediction ---
        if (result.getMerchant() != null) {
            String m = result.getMerchant().toLowerCase();
            if (m.matches(".*(mcdonald|burger|cafe|coffee|restaurant|pizza|bar|grill|diner|eats|food).*")) {
                result.setCategory("Food");
            } else if (m.matches(".*(uber|taxi|fuel|shell|parking|gas|petrol|transport).*")) {
                result.setCategory("Transport");
            } else if (m.matches(".*(amazon|walmart|costco|target|store|market|supermarket|grocer|retail|shop).*")) {
                result.setCategory("Shopping");
            } else if (m.matches(".*(pharmacy|doctor|hospital|clinic|health|cvs|walgreens|med).*")) {
                result.setCategory("Health");
            } else if (m.matches(".*(netflix|spotify|cinema|movie|theatre|entertainment).*")) {
                result.setCategory("Entertainment");
            } else {
                result.setCategory("Other");
            }
        } else {
             result.setCategory("Other");
        }

        return result;
    }

    private double parseAmount(String valStr) {
        try {
            valStr = valStr.replace(",", "");
            return Double.parseDouble(valStr);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String normalizeDate(String rawDate) {
        try {
            if (rawDate.contains("-")) return rawDate;
             if (rawDate.contains("/")) {
                String[] parts = rawDate.split("/");
                if (parts.length == 3) {
                     int p1 = Integer.parseInt(parts[0]);
                     int p2 = Integer.parseInt(parts[1]);
                     String year = parts[2];
                     if (year.length() == 2) year = "20" + year;
                     if (p1 > 12) return String.format(java.util.Locale.US, "%s-%02d-%02d", year, p2, p1);
                     else return String.format(java.util.Locale.US, "%s-%02d-%02d", year, p1, p2);
                }
            }
            return rawDate;
        } catch (Exception e) {
            return rawDate;
        }
    }

    private String toTitleCase(String input) {
        StringBuilder titleCase = new StringBuilder();
        boolean nextTitleCase = true;
        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            } else {
                c = Character.toLowerCase(c);
            }
            titleCase.append(c);
        }
        return titleCase.toString();
    }

    public static class OCRResult {
        private String rawText;
        private double amount;
        private String date;
        private String merchant;
        private String category;

        public String getRawText() { return rawText; }
        public void setRawText(String rawText) { this.rawText = rawText; }
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getMerchant() { return merchant; }
        public void setMerchant(String merchant) { this.merchant = merchant; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }
}
