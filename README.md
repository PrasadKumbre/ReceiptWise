# <img width="50" height="50" alt="logo_12" src="https://github.com/user-attachments/assets/1d749040-2051-4370-ab1e-712d959e7491" /> ReceiptWise – Smart Income & Expense Tracker

ReceiptWise is an Android application that helps users track income and expenses efficiently using **OCR (Optical Character Recognition)**.  
The app can scan receipts, automatically extract important details like **merchant name, date, and total amount**, and categorize transactions.

---

## ✨ Features

- 📷 **Receipt Scanning**
  - Capture or upload receipt images
  - Uses Google ML Kit Text Recognition

- 🔎 **Smart OCR Parsing**
  - Merchant name detection
  - Multi-format date extraction
  - Intelligent total amount detection
  - Noise filtering for messy receipts

- 🧾 **Transaction Management**
  - Add / Edit / Delete income
  - Categorization (Food, Transport, Shopping, etc.)

- 🔍 **Search & Filter**
  - Search transactions instantly
  - Filter by Week / Month / Year / All

- 📊 **Analytics**
  - View spending/income patterns
  - Visual summaries

- 🎨 **Modern UI**
  - Material Design components
  - Smooth horizontal scroll interactions

---

## 🏗️ Tech Stack

| Component | Technology |
|----------|------------|
| Language | Java |
| Platform | Android |
| OCR Engine | Google ML Kit Text Recognition |
| UI | Material Components / ConstraintLayout |
| Architecture | Utility-based modular design |
| Database | Room / SQLite (Local Storage) |

---

## 📦 Dependencies

Add these to your `build.gradle (Module: app)`:

```gradle
dependencies {
    implementation 'com.google.mlkit:text-recognition:16.0.0'
    implementation 'com.google.android.material:material:1.11.0'
}
````

---

## 🔐 Permissions Required

```xml
<uses-permission android:name="android.permission.CAMERA"/>
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
```

---

## 📷 OCR Workflow

1. User captures/selects receipt image
2. Image converted → `InputImage`
3. ML Kit processes text
4. OCRManager parses:

   * Merchant
   * Date
   * Amount
   * Category

---

## 🧠 OCR Parsing Intelligence

ReceiptWise handles complex receipts by:

✅ Detecting **largest top text** → Merchant
✅ Supporting **multiple date formats**:

* `12/01/2025`
* `2025-01-12`
* `12 Jan 2025`
* `Jan 12, 2025`

✅ Recognizing **Total Variants**:

* Grand Total
* Total Amount
* Bill Total
* Net Amount
* MRP Total
* Amount Payable

✅ Ignoring:

* Subtotal
* GST / CGST / SGST
* Discounts
* Rounding
* Change

---

## 🗄️ Database Structure

ReceiptWise uses a **local Room / SQLite database** to store transactions.

### 📑 Table: `income_table`

| Column   | Type         | Description              |
| -------- | ------------ | ------------------------ |
| id       | INTEGER (PK) | Unique transaction ID    |
| title    | TEXT         | Income title/description |
| amount   | REAL         | Income amount            |
| category | TEXT         | Income category          |
| date     | TEXT         | Transaction date         |
| notes    | TEXT         | Optional notes           |

---

### 📑 Table: `expense_table`

| Column      | Type         | Description            |
| ----------- | ------------ | ---------------------- |
| id          | INTEGER (PK) | Unique expense ID      |
| merchant    | TEXT         | Merchant/shop name     |
| amount      | REAL         | Total amount           |
| category    | TEXT         | Expense category       |
| date        | TEXT         | Bill/receipt date      |
| paymentMode | TEXT         | Cash / UPI / Card      |
| rawText     | TEXT         | OCR raw extracted text |

---

### 📑 Table: `category_table` *(Optional)*

| Column | Type         | Description      |
| ------ | ------------ | ---------------- |
| id     | INTEGER (PK) | Category ID      |
| name   | TEXT         | Category name    |
| type   | TEXT         | Income / Expense |

---

### 📑 Table: `analytics_cache` *(Optional)*

| Column       | Type         | Description         |
| ------------ | ------------ | ------------------- |
| id           | INTEGER (PK) | Cache ID            |
| period       | TEXT         | Week / Month / Year |
| totalIncome  | REAL         | Aggregated income   |
| totalExpense | REAL         | Aggregated expense  |

---

## 🗂️ Project Structure

```
com.example.expensetracker
│
├── utils
│   └── OCRManager.java
│
├── ui
│   ├── fragments
│   ├── adapters
│   └── layouts
│
├── model
│
└── database
    ├── entities
    ├── dao
    └── AppDatabase.java
```

---

## 🎯 Core Components

### 🔹 OCRManager

Handles:

* ML Kit initialization
* Receipt analysis
* Text parsing
* Data extraction

---

## 🎨 UI Highlights

* Custom Toolbar
* Horizontal Scroll Filters + Search
* RecyclerView Transaction List
* Floating Action Button
* Total Capsule Summary

---

## 📸 Screenshots 

 Dasboard/ Home Page                     |     | Expense Page                            |
 --------------------------------------- |---- | --------------------------------------- |
 <img width="400" height="800" alt="logo_12" src="https://github.com/user-attachments/assets/6d95f8f6-1c6f-404b-a777-74b435c54f5f" /> |     | <img width="400" height="800" alt="logo_12" src="https://github.com/user-attachments/assets/27318ffc-7039-40b1-a041-1973438c7aa7" /> |

Income Page                     |     | Analytics Page                            |
 --------------------------------------- |---- | --------------------------------------- |
 <img width="400" height="800" alt="logo_12" src="https://github.com/user-attachments/assets/48402d50-88e6-42aa-aac8-d18e82e89961" /> |     | <img width="400" height="800" alt="logo_12" src="https://github.com/user-attachments/assets/6f3bb95f-5d82-41c8-83ca-36fc189d31ca" /> |


---

## 🚀 Getting Started

### 1️⃣ Clone Repository

```bash
https://github.com/PrasadKumbre/ReceiptWise.git
```

---

### 2️⃣ Open in Android Studio

* Open Android Studio
* Select **Open Existing Project**
* Sync Gradle

---

### 3️⃣ Run App

* Connect emulator/device
* Grant camera permissions
* Start scanning receipts

---

## 🧪 Testing Suggestions

Test OCR with:

✔ Grocery receipts
✔ Restaurant bills
✔ Petrol/Fuel bills
✔ Online order printouts
✔ Messy/blurred receipts

---

## ⚠️ Known Limitations

* OCR accuracy depends on image quality
* Handwritten receipts may fail
* Extremely stylized fonts may reduce detection

---

## 🔮 Future Improvements

* GST / Tax breakdown extraction
* Multi-currency support
* Export to CSV / PDF
* Cloud sync
* Spending insights
* Receipt image storage

---

## 🤝 Contributing

Contributions are welcome!

1. Fork the repo
2. Create feature branch
3. Commit changes
4. Open Pull Request

---

## 📜 License

This project is licensed under the **MIT License**.

---

## 👨‍💻 Developer

**Prasad Kumbre**

---

## ⭐ Support

If you like this project:

⭐ Star the repository
🐛 Report issues
💡 Suggest features
