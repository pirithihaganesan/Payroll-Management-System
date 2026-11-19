# Software Requirements Specification (SRS)
**Project:** Employee Attendance and Payroll Management Analysis System
**Scenario:** 13 | **Class:** 25IT3A&B | **Department of Information Technology, Velalar College of Engineering and Technology, Erode - 12**
**Academic Year:** 2026-27 (Odd) | **Register No.:** 732925ITR073 | **Version:** 1.0

---

## 1. Introduction

### 1.1 Purpose
This document specifies the requirements of the Employee Attendance and Payroll Management Analysis System, a Java + DBMS + Python application that maintains employee records, daily attendance, leave, monthly salary calculation and workforce attendance analysis.

### 1.2 Problem statement
An organization keeps employee details, daily attendance, working hours, leave records and salary information manually. This causes attendance errors, difficulty calculating monthly salaries, and delays in identifying absenteeism patterns and preparing payroll reports. The system automates these tasks and analyses attendance patterns.

### 1.3 Scope
In scope: employee master data, attendance capture, leave application/approval, rule-based monthly payroll, reports, and Python-based analytics with visualizations.
Out of scope: biometric devices, bank transfer, income-tax slabs beyond a flat professional tax, multi-user login/roles (a single administrator is assumed).

### 1.4 Definitions
| Term | Meaning |
|---|---|
| LOP | Loss of Pay - salary deducted for unpaid days |
| Gross | Basic + HRA + DA |
| PF | Provident Fund deduction |
| Payable days | Days for which the employee is paid |
| Working day | Monday-Friday |

### 1.5 Stakeholders / users
| User | Needs |
|---|---|
| HR/Payroll administrator (primary) | Manage employees, record attendance and leave, run payroll, view reports |
| Management | Payroll cost and absenteeism analysis |
| Employee | Receives payslip (printed by administrator) |

---

## 2. Overall description

### 2.1 Product perspective
Three cooperating components sharing one SQLite database:
1. **Java console application** - data entry, business rules, payroll, reports.
2. **SQLite database** - persistent storage, integrity constraints.
3. **Python analytics module** - retrieval (SQL), preprocessing (pandas) and visualization (matplotlib).

### 2.2 Operating environment
Java 11+ (developed on JDK 21), SQLite 3 via `sqlite-jdbc`, Python 3.9+ with pandas and matplotlib; Windows/Linux/macOS.

### 2.3 Assumptions and constraints
- Working week is Monday-Friday; weekends are not recorded.
- One attendance record per employee per day; one payslip per employee per month.
- Days with no attendance record in a month are treated as loss-of-pay.
- Single administrator user.

---

## 3. Functional requirements

### 3.1 Employee management (FR-1 ... FR-6)
| ID | Requirement |
|---|---|
| FR-1 | Add an employee with name, department, designation, basic salary and joining date. |
| FR-2 | View all employees and search by name. |
| FR-3 | Update basic salary, department and designation. |
| FR-4 | Deactivate an employee (soft delete); history is retained. |
| FR-5 | Departments and designations are created automatically when new. |
| FR-6 | Validate inputs: name 2-60 letters, salary 1 - 1,000,000, joining date not in the future. |

### 3.2 Attendance (FR-7 ... FR-12)
| ID | Requirement |
|---|---|
| FR-7 | Record daily attendance: Present, Absent or Half-Day, with optional check-in/check-out (HH:mm). |
| FR-8 | Compute working hours from check-in/out; defaults to 8 h (Present) / 4 h (Half-Day) when times are not given. |
| FR-9 | Reject duplicate attendance for the same employee and date. |
| FR-10 | Reject weekend dates, future dates, dates before joining and inactive employees. |
| FR-11 | Check-out must be later than check-in. |
| FR-12 | Show an employee-wise monthly attendance summary. |

### 3.3 Leave management (FR-13 ... FR-18)
| ID | Requirement |
|---|---|
| FR-13 | Apply for leave: Casual, Sick, Earned or Loss of Pay, with date range and reason. |
| FR-14 | Count only working days in the range; reject ranges with none or with end before start. |
| FR-15 | Reject leave overlapping an existing pending/approved leave. |
| FR-16 | Administrator approves or rejects a pending leave. |
| FR-17 | Approval writes attendance rows (Paid Leave for Casual/Sick/Earned, Unpaid Leave for LOP) atomically; fails (rolled back) if attendance already exists on a day. |
| FR-18 | Track the number of days each employee is absent or on leave (reports FR-24, FR-26). |

### 3.4 Payroll (FR-19 ... FR-23)
Salary rules (predefined):

| Component | Rule |
|---|---|
| HRA | 20 % of basic |
| DA | 10 % of basic |
| Gross | Basic + HRA + DA |
| PF | 12 % of basic |
| Professional tax | Rs 200 if gross > Rs 15,000, else 0 |
| Payable days | Present + 0.5 x Half-Day + Paid Leave (capped at working days) |
| LOP days | Working days - payable days (absent, unpaid leave and unrecorded days) |
| LOP deduction | Gross / working days x LOP days |
| Net salary | Gross - (PF + professional tax + LOP deduction), never below 0 |

| ID | Requirement |
|---|---|
| FR-19 | Generate a payslip for one employee and month using the rules above. |
| FR-20 | Generate payslips for all active employees for a month (skipping those already generated or without attendance). |
| FR-21 | Reject payroll for future months, months before joining, months already generated, or months with no attendance. |
| FR-22 | Store each payslip as a snapshot so later salary changes do not alter history. |
| FR-23 | Print a payslip. |

### 3.5 Reports (FR-24 ... FR-28)
| ID | Requirement |
|---|---|
| FR-24 | Employee-wise attendance summary for a month. |
| FR-25 | Department-wise attendance report (present %, absent %). |
| FR-26 | Monthly payroll report with totals; department-wise payroll cost. |
| FR-27 | Frequent-absentee list (absent days >= threshold). |
| FR-28 | Leave summary per employee and leave type. |

### 3.6 Analytics - Python (FR-29 ... FR-32)
| ID | Requirement |
|---|---|
| FR-29 | Retrieve employee, attendance, leave and payroll data from the database using SQL joins. |
| FR-30 | Preprocess: type conversion, duplicate removal, missing-value handling, outlier check (IQR), derived features (month, weekday, LOP %, attendance %). |
| FR-31 | Generate 8 visualizations (attendance mix, department absenteeism, payroll trend, department payroll, weekday absence, leave types, working-hour distribution, attendance vs LOP). |
| FR-32 | Export cleaned CSV files and a text summary of findings. |

---

## 4. Non-functional requirements
| ID | Category | Requirement |
|---|---|---|
| NFR-1 | Integrity | Database constraints (PK, FK, UNIQUE, CHECK) enforce rules even if the application is bypassed. |
| NFR-2 | Accuracy | Monetary values rounded to 2 decimals (HALF_UP); payroll logic covered by unit tests. |
| NFR-3 | Usability | Menu-driven console; every invalid input produces a clear message and re-prompt, never a crash. |
| NFR-4 | Performance | Monthly payroll for 1,000 employees in under 10 seconds (indexed queries). |
| NFR-5 | Portability | Single-file database; no server install; runs on Windows, Linux, macOS. |
| NFR-6 | Maintainability | Layered design (model / DAO / business logic / UI); parameterised SQL only. |
| NFR-7 | Security | Prepared statements prevent SQL injection. (No authentication in this version.) |
| NFR-8 | Reproducibility | Seed data generated with a fixed random seed. |

## 5. External interfaces
- **User interface:** text console with numbered menus.
- **Database interface:** JDBC (Java) and `sqlite3` (Python) on `payroll.db`.
- **Output:** console reports, printable payslip, PNG charts, CSV files.

## 6. Data requirements
Six tables: `department`, `designation`, `employee`, `attendance`, `leave_record`, `payroll` (see `database/normalization.md` for ER diagram, relational model and normalization to 3NF).

## 7. Requirements traceability
| Scenario requirement | Covered by |
|---|---|
| 1. Maintain employee details | FR-1 - FR-6 |
| 2. Record daily attendance | FR-7 - FR-12 |
| 3. Leave records, days absent | FR-13 - FR-18 |
| 4. Monthly salary from rules, working days, deductions | FR-19 - FR-23 |
| 5. Attendance summaries, payroll, department reports | FR-24 - FR-28 |
| 6. Python retrieve, preprocess, visualize | FR-29 - FR-32 |

## 8. Acceptance criteria
The system is accepted when all test cases in `docs/testing.md` pass, a payslip for any seeded employee/month matches the hand calculation, and the Python script produces all eight charts from a freshly built database.
