# Testing Document

## 1. Test strategy and techniques

| Technique | Where applied |
|---|---|
| **Unit testing** (white box) | `PayrollCalculator`, `Validator`, enums - `java-app/test/payroll/TestRunner.java` |
| **Equivalence partitioning** | Valid / invalid names, salaries, dates, statuses |
| **Boundary value analysis** | Gross exactly at / above the Rs 15,000 tax threshold, 0 and negative salary, month 12/13, Feb with 20 working days, payable days capped at working days |
| **Statement / branch coverage** | Both branches of professional tax, LOP = 0 and LOP > 0, net floored at 0 |
| **Database constraint testing** | `database/test_db.py` (PK, FK, UNIQUE, CHECK) |
| **Integration testing** | Java DAOs against SQLite (cases I01-I14) |
| **Data verification** | Payroll arithmetic recomputed from attendance in SQL (D13-D18) |
| **Pipeline testing** | Python retrieval, preprocessing and chart generation (P01-P08) |
| **System / acceptance testing** | End-to-end scenario via `--demo` and manual session (S01-S03) |

### How to run
```bash
# Unit tests (no database needed)
cd java-app && javac -d out src/payroll/*.java test/payroll/*.java && java -cp out payroll.TestRunner
# Database tests
python database/test_db.py
# Python pipeline
python python-analysis/analysis.py
# Integration/system tests (need the SQLite JDBC jar in java-app/lib - see README)
java -cp "out:lib/*" payroll.Main --db /tmp/test.db --schema ../database/schema.sql --demo
```

**Status legend:** PASS = executed, actual result matches expected. RUN = integration case that exercises Java + JDBC and must be executed after adding the SQLite JDBC jar (it could not be downloaded in the build environment); record the observed result in the Actual column.

## 2. Unit test cases (executed) - `TestRunner`

| ID | Test | Input | Expected | Actual | Result |
|---|---|---|---|---|---|
| U01 | Working days Aug 2026 | 2026-08 | 21 | 21 | PASS |
| U02 | Working days Sep 2026 | 2026-09 | 22 | 22 | PASS |
| U03 | Working days Feb 2026 (boundary: shortest month) | 2026-02 | 20 | 20 | PASS |
| U04 | Working days Fri 9 Oct to Mon 12 Oct | range | 2 | 2 | PASS |
| U05 | Saturday is not a working day | 2026-10-10 | false | false | PASS |
| U06 | Full attendance, basic 10,000 (hand calc: gross 13,000, PF 1,200) | 22 present | net 11,800.00 | 11,800.00 | PASS |
| U06b | No professional tax when gross 13,000 <= 15,000 | same | 0.0 | 0.0 | PASS |
| U07 | Emp 1 Sep: 1 absent day, basic 20,500 | 21 present | LOP 1,211.36; net 22,778.64 | 1,211.36; 22,778.64 | PASS |
| U08 | Emp 34 Oct: 3 absent + 2 half-days | 17 present, 2 half | payable 18.0, LOP days 4.0, net 11,123.64 | 18.0, 4.0, 11,123.64 | PASS |
| U09 | Emp 7 Aug: paid leave counted as payable | 16 present, 1 half, 2 paid | payable 18.5, net 53,112.38 | 18.5, 53,112.38 | PASS |
| U10 | Emp 14 Sep: heavy absenteeism | 14 present, 1 half | net 16,746.82 | 16,746.82 | PASS |
| U11 | Payable days capped at working days | 30 present in 22-day month | 22.0 | 22.0 | PASS |
| U12 | Zero basic salary | basic 0 | IllegalArgumentException | thrown | PASS |
| U13 | No attendance at all, net cannot go negative | 0 days | net 0.0 | 0.0 | PASS |
| U14 | Valid name trimmed | "  Asha Menon " | "Asha Menon" | "Asha Menon" | PASS |
| U15 | Name containing digits | "Asha123" | rejected | rejected | PASS |
| U16 | Negative salary | -5 | rejected | rejected | PASS |
| U17 | Wrong date format | "31-12-2026" | rejected | rejected | PASS |
| U18 | Invalid month | "2026-13" | rejected | rejected | PASS |
| U19 | Hours 09:00-17:30 | times | 8.5 | 8.5 | PASS |
| U20 | Check-out before check-in | 17:00 / 09:00 | rejected | rejected | PASS |
| U21 | Enum lookup is case-insensitive | "half-day" | HALF_DAY | HALF_DAY | PASS |
| U22 | Loss of Pay leave is unpaid | enum | false | false | PASS |

Console output: `sample-io/java_unit_test_output.txt` (27 assertions, 0 failures).

## 3. Database test cases (executed) - `database/test_db.py`

| ID | Test | Expected | Actual | Result |
|---|---|---|---|---|
| D01 | Duplicate attendance same employee/date | UNIQUE violation | UNIQUE constraint failed | PASS |
| D02 | Negative basic salary | CHECK violation | CHECK constraint failed | PASS |
| D03 | Invalid attendance status "Late" | CHECK violation | CHECK constraint failed | PASS |
| D04 | Attendance for employee 9999 | FK violation | FOREIGN KEY constraint failed | PASS |
| D05 | Employee with department 99 | FK violation | FOREIGN KEY constraint failed | PASS |
| D06 | Second payslip for same employee/month | UNIQUE violation | UNIQUE constraint failed | PASS |
| D07 | Payroll month 13 | CHECK violation | CHECK constraint failed | PASS |
| D08 | Leave end date before start date | CHECK violation | CHECK constraint failed | PASS |
| D09 | Duplicate department name | UNIQUE violation | UNIQUE constraint failed | PASS |
| D10 | Delete department that has employees | FK violation | FOREIGN KEY constraint failed | PASS |
| D11 | Seed employee count | 40 | 40 | PASS |
| D12 | Seed payslip count (3 x 40) | 120 | 120 | PASS |
| D13 | gross - deductions = net on every row | 0 violations | 0 | PASS |
| D14 | basic + HRA + DA = gross on every row | 0 violations | 0 | PASS |
| D15 | No attendance on weekends | 0 | 0 | PASS |
| D16 | working days - payable = LOP days | 0 violations | 0 | PASS |
| D17 | No orphan attendance rows | 0 | 0 | PASS |
| D18 | Payable days recomputed from attendance equal stored value (120 payslips) | 0 mismatches | 0 | PASS |

Output: `sample-io/db_test_output.txt` (18 passed, 0 failed).

## 4. Python pipeline test cases (executed) - `analysis.py`

| ID | Test | Expected | Actual | Result |
|---|---|---|---|---|
| P01 | Retrieval from database | 40 emp, 2,600 attendance, 65 leave, 120 payroll rows | 40 / 2600 / 65 / 120 | PASS |
| P02 | Duplicate attendance removal | 0 duplicates | 0 | PASS |
| P03 | Missing check-in/out only on Absent/Leave rows | Present/Half-Day rows lacking times = 0 | 0 | PASS |
| P04 | Outlier check on full-day working hours (IQR) | reported | 0 outliers in [6.50, 10.50] | PASS |
| P05 | Payroll integrity after preprocessing | 0 mismatches | 0 | PASS |
| P06 | Months covered | 2026-08, 09, 10 | 2026-08, 2026-09, 2026-10 | PASS |
| P07 | Charts generated | 8 PNG files | 8 files in `python-analysis/output/` | PASS |
| P08 | Missing database | clear error, exit | message "Database not found ... run init_db.py" | PASS (by code review) |

## 5. Integration test cases (Java + SQLite) - RUN after adding the JDBC jar

| ID | Feature | Steps / input | Expected result | Actual | Result |
|---|---|---|---|---|---|
| I01 | Add employee | Name Asha Menon, Engineering, Senior Executive, 32000, 2025-01-06 | ID 41 created; appears in list | _record_ | RUN |
| I02 | New department auto-created | Add employee with department "Legal" | Department row created, employee linked | _record_ | RUN |
| I03 | Mark attendance (valid) | Emp 41, 2026-11-02, Present, 09:00-17:30 | Recorded, hours 8.5 | _record_ | RUN |
| I04 | Duplicate attendance | Repeat I03 | "Attendance already recorded for this employee on 2026-11-02" | _record_ | RUN |
| I05 | Weekend attendance | Emp 41, 2026-11-07 (Saturday) | "Weekend - not a working day" | _record_ | RUN |
| I06 | Future date | Date after today | "Cannot mark attendance for a future date" | _record_ | RUN |
| I07 | Before joining date | Emp 41, 2024-12-02 | "Date is before the employee's joining date" | _record_ | RUN |
| I08 | Inactive employee | Deactivate 41, mark attendance | "Employee not found or inactive" | _record_ | RUN |
| I09 | Apply leave | Emp 41, Sick, 2026-11-04 to 2026-11-05 | Leave #n Pending, 2 days | _record_ | RUN |
| I10 | Overlapping leave | Apply 2026-11-05 to 2026-11-06 | "Overlaps an existing pending/approved leave" | _record_ | RUN |
| I11 | Approve leave | Approve I09 | Status Approved; 2 Paid Leave attendance rows created | _record_ | RUN |
| I12 | Approve already-approved leave | Approve again | "already Approved" | _record_ | RUN |
| I13 | Generate payroll (seeded) | Emp 1, 2026-10 -> existing payslip | "Payroll for 2026-10 already generated" | _record_ | RUN |
| I14 | Generate payroll, no attendance | Emp 41, 2026-10 | "No attendance recorded for 2026-10..." | _record_ | RUN |
| I15 | Leave over existing attendance | Approve leave covering a day with attendance | Error, transaction rolled back, nothing inserted | _record_ | RUN |
| I16 | SQL injection attempt | Search name `' OR 1=1 --` | No rows (parameterised query) | _record_ | RUN |

## 6. System / acceptance test cases

| ID | Scenario | Expected | Actual | Result |
|---|---|---|---|---|
| S01 | `--demo` run on an empty database | 3 employees, October 2026 attendance, approved leave, 3 payslips, reports print without error | _record_ | RUN |
| S02 | Payslip hand-check | Printed payslip for Emp 7 Aug 2026 equals hand calculation (net 53,112.38) | Matches (sample-io/java_payslip_sample_output.txt, produced by real Java run) | PASS |
| S03 | Invalid-input handling | Enter "abc" for a number, "2026-13" for a month: re-prompt, no crash | _record_ | RUN |
