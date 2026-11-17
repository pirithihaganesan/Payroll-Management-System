"""
Employee Attendance & Payroll - data retrieval, preprocessing and visualization.

Steps
  1. RETRIEVE  - read employee, attendance, leave and payroll data from the SQLite database (SQL joins).
  2. PREPROCESS - type conversion, missing-value handling, duplicate removal, outlier check, feature engineering.
  3. VISUALIZE - 8 charts saved as PNG in ./output, plus cleaned CSVs and a text summary.

Usage:  python analysis.py [path/to/payroll.db]
"""
import sqlite3
import sys
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import pandas as pd

HERE = Path(__file__).parent
DB = Path(sys.argv[1]) if len(sys.argv) > 1 else HERE.parent / "database" / "payroll.db"
OUT = HERE / "output"
OUT.mkdir(exist_ok=True)

# Colour-blind-safe palette (Okabe-Ito); series colours are fixed per entity, never cycled by rank.
C = {"blue": "#0072B2", "orange": "#E69F00", "green": "#009E73", "red": "#D55E00",
     "purple": "#CC79A7", "sky": "#56B4E9", "grey": "#7A7A7A"}
STATUS_COLOR = {"Present": C["green"], "Half-Day": C["sky"], "Paid Leave": C["blue"],
                "Unpaid Leave": C["orange"], "Absent": C["red"]}
STATUS_ORDER = list(STATUS_COLOR)

plt.rcParams.update({"axes.spines.top": False, "axes.spines.right": False, "axes.grid": True,
                     "grid.alpha": 0.25, "figure.dpi": 110, "savefig.bbox": "tight",
                     "axes.titleweight": "bold", "font.size": 10})


# ----------------------------------------------------------------------------- 1. RETRIEVE
def retrieve(db_path):
    if not db_path.exists():
        sys.exit(f"Database not found: {db_path}\nRun: python ../database/init_db.py")
    con = sqlite3.connect(db_path)
    emp = pd.read_sql_query("""
        SELECT e.emp_id, e.emp_name, d.dept_name, g.title AS designation,
               e.basic_salary, e.join_date, e.status
        FROM employee e JOIN department d ON d.dept_id = e.dept_id
                        JOIN designation g ON g.desig_id = e.desig_id""", con)
    att = pd.read_sql_query("""
        SELECT a.att_id, a.emp_id, a.att_date, a.status, a.check_in, a.check_out, a.working_hours,
               d.dept_name
        FROM attendance a JOIN employee e ON e.emp_id = a.emp_id
                          JOIN department d ON d.dept_id = e.dept_id""", con)
    leave = pd.read_sql_query("""
        SELECT l.leave_id, l.emp_id, l.leave_type, l.start_date, l.end_date, l.days, l.status,
               d.dept_name
        FROM leave_record l JOIN employee e ON e.emp_id = l.emp_id
                            JOIN department d ON d.dept_id = e.dept_id""", con)
    pay = pd.read_sql_query("""
        SELECT p.*, e.emp_name, d.dept_name, g.title AS designation
        FROM payroll p JOIN employee e ON e.emp_id = p.emp_id
                       JOIN department d ON d.dept_id = e.dept_id
                       JOIN designation g ON g.desig_id = e.desig_id""", con)
    con.close()
    return emp, att, leave, pay


# --------------------------------------------------------------------------- 2. PREPROCESS
def preprocess(emp, att, leave, pay, log):
    log("PREPROCESSING REPORT")
    log("-" * 60)
    log(f"Raw rows -> employees {len(emp)}, attendance {len(att)}, leave {len(leave)}, payroll {len(pay)}")

    # types
    emp["join_date"] = pd.to_datetime(emp["join_date"])
    att["att_date"] = pd.to_datetime(att["att_date"])
    leave["start_date"] = pd.to_datetime(leave["start_date"])
    leave["end_date"] = pd.to_datetime(leave["end_date"])

    # duplicates
    d = att.duplicated(subset=["emp_id", "att_date"]).sum()
    att = att.drop_duplicates(subset=["emp_id", "att_date"])
    log(f"Duplicate attendance rows removed: {d}")

    # missing values
    log("Missing check-in/out (expected for Absent/Leave rows): "
        f"{att['check_in'].isna().sum()} / {att['check_out'].isna().sum()}")
    bad = att[att["status"].isin(["Present", "Half-Day"]) & att["check_in"].isna()]
    log(f"Present/Half-Day rows lacking times: {len(bad)}")
    att["check_in"] = att["check_in"].fillna("")
    att["check_out"] = att["check_out"].fillna("")

    # outliers (IQR rule on working hours of worked days)
    worked = att[att["status"] == "Present"]["working_hours"]   # half-days are legitimately ~4 h
    q1, q3 = worked.quantile([.25, .75])
    lo, hi = q1 - 1.5 * (q3 - q1), q3 + 1.5 * (q3 - q1)
    n_out = int(((worked < lo) | (worked > hi)).sum())
    log(f"Full-day working-hour outliers outside [{lo:.2f}, {hi:.2f}]: {n_out}")

    # feature engineering
    att["month"] = att["att_date"].dt.to_period("M").astype(str)
    att["weekday"] = pd.Categorical(att["att_date"].dt.day_name(),
                                    ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday"], ordered=True)
    att["check_in_hour"] = pd.to_numeric(
        att["check_in"].str.slice(0, 2).replace("", pd.NA), errors="coerce") \
        + pd.to_numeric(att["check_in"].str.slice(3, 5).replace("", pd.NA), errors="coerce") / 60
    att["is_absent"] = (att["status"] == "Absent").astype(int)
    att["is_present_like"] = att["status"].isin(["Present", "Half-Day"]).astype(int)
    pay["period"] = pd.to_datetime(dict(year=pay["pay_year"], month=pay["pay_month"], day=1))
    pay["month"] = pay["period"].dt.strftime("%Y-%m")
    pay["lop_pct_of_gross"] = (pay["lop_deduction"] / pay["gross_salary"] * 100).round(2)
    pay["attendance_pct"] = (pay["payable_days"] / pay["working_days"] * 100).round(2)

    # integrity cross-check: net = gross - total deductions
    mism = (abs(pay["gross_salary"] - pay["total_deductions"] - pay["net_salary"]) > 0.01).sum()
    log(f"Payroll rows where gross - deductions != net: {mism}")
    log(f"Attendance months covered: {sorted(att['month'].unique())}")
    log("")
    return emp, att, leave, pay


# --------------------------------------------------------------------------- 3. VISUALIZE
def save(fig, name):
    fig.savefig(OUT / name)
    plt.close(fig)
    print("  saved", name)


def chart_status_mix(att):
    t = att.groupby(["month", "status"]).size().unstack(fill_value=0)[STATUS_ORDER]
    pct = t.div(t.sum(axis=1), axis=0) * 100
    fig, ax = plt.subplots(figsize=(8, 4.5))
    left = None
    bottom = pd.Series(0.0, index=pct.index)
    for s in STATUS_ORDER:
        ax.bar(pct.index, pct[s], bottom=bottom, color=STATUS_COLOR[s], label=s,
               edgecolor="white", linewidth=1.5, width=0.55)
        for i, v in enumerate(pct[s]):
            if v >= 6:
                ax.text(i, bottom.iloc[i] + v / 2, f"{v:.0f}%", ha="center", va="center",
                        fontsize=9, color="black" if s in ("Half-Day",) else "white")
        bottom += pct[s]
    ax.set_ylabel("Share of employee-days (%)")
    ax.set_title("Attendance status mix by month")
    ax.legend(ncol=5, loc="upper center", bbox_to_anchor=(0.5, -0.08), frameon=False)
    ax.grid(axis="x", visible=False)
    save(fig, "01_attendance_status_mix.png")


def chart_dept_absence(att):
    g = att.groupby("dept_name")["is_absent"].mean().mul(100).sort_values()
    fig, ax = plt.subplots(figsize=(7, 4))
    bars = ax.barh(g.index, g.values, color=C["red"], height=0.6)
    ax.bar_label(bars, fmt="%.1f%%", padding=3)
    ax.set_xlabel("Absent days (% of recorded days)")
    ax.set_title("Absenteeism rate by department")
    ax.grid(axis="y", visible=False)
    ax.set_xlim(0, g.max() * 1.18)
    save(fig, "02_department_absenteeism.png")


def chart_payroll_trend(pay):
    g = pay.groupby("month")[["gross_salary", "net_salary"]].sum() / 1e5
    fig, ax = plt.subplots(figsize=(7.5, 4.2))
    ax.plot(g.index, g["gross_salary"], marker="o", color=C["blue"], linewidth=2, markersize=8)
    ax.plot(g.index, g["net_salary"], marker="o", color=C["orange"], linewidth=2, markersize=8)
    for col, color, dy in (("gross_salary", C["blue"], 4), ("net_salary", C["orange"], -14)):
        for x, y in g[col].items():
            ax.annotate(f"{y:.1f}", (x, y), textcoords="offset points", xytext=(0, dy), ha="center", fontsize=9)
    ax.text(-0.12, g["gross_salary"].iloc[0], "Gross", color=C["blue"], ha="right", va="center", fontweight="bold")
    ax.text(-0.12, g["net_salary"].iloc[0], "Net", color=C["orange"], ha="right", va="center", fontweight="bold")
    ax.set_xlim(-0.5, len(g) - 0.5)
    ax.set_ylabel("Total payroll (₹ lakh)")
    ax.set_title("Monthly payroll cost: gross vs net")
    ax.set_ylim(0, g["gross_salary"].max() * 1.15)
    save(fig, "03_monthly_payroll_trend.png")


def chart_dept_payroll(pay):
    latest = pay[pay["month"] == pay["month"].max()]
    g = latest.groupby("dept_name")["net_salary"].sum().div(1e5).sort_values()
    fig, ax = plt.subplots(figsize=(7, 4))
    bars = ax.barh(g.index, g.values, color=C["blue"], height=0.6)
    ax.bar_label(bars, fmt="%.2f", padding=3)
    ax.set_xlabel("Net payroll (₹ lakh)")
    ax.set_title(f"Net payroll by department - {pay['month'].max()}")
    ax.grid(axis="y", visible=False)
    ax.set_xlim(0, g.max() * 1.15)
    save(fig, "04_department_payroll.png")


def chart_weekday_absence(att):
    g = att.groupby("weekday", observed=True)["is_absent"].mean().mul(100)
    fig, ax = plt.subplots(figsize=(7, 4))
    bars = ax.bar(g.index.astype(str), g.values, color=C["red"], width=0.55)
    ax.bar_label(bars, fmt="%.1f%%", padding=3)
    ax.set_ylabel("Absent rate (%)")
    ax.set_title("Absenteeism by weekday")
    ax.grid(axis="x", visible=False)
    ax.set_ylim(0, g.max() * 1.2)
    save(fig, "05_weekday_absence.png")


def chart_leave_types(leave):
    g = leave[leave["status"] == "Approved"].groupby("leave_type")["days"].sum().sort_values()
    colors = {"Casual": C["blue"], "Sick": C["green"], "Earned": C["sky"], "Loss of Pay": C["orange"]}
    fig, ax = plt.subplots(figsize=(7, 3.8))
    bars = ax.barh(g.index, g.values, color=[colors[k] for k in g.index], height=0.6)
    ax.bar_label(bars, padding=3)
    ax.set_xlabel("Approved leave days")
    ax.set_title("Leave days by type")
    ax.grid(axis="y", visible=False)
    ax.set_xlim(0, g.max() * 1.15)
    save(fig, "06_leave_types.png")


def chart_hours(att):
    worked = att[att["working_hours"] > 0]
    fig, ax = plt.subplots(figsize=(7, 4))
    ax.hist(worked["working_hours"], bins=[x / 2 for x in range(7, 21)], color=C["blue"],
            edgecolor="white", linewidth=1.5)
    ax.axvline(worked["working_hours"].mean(), color=C["red"], linestyle="--", linewidth=1.5)
    ax.text(worked["working_hours"].mean() - 0.1, ax.get_ylim()[1] * 0.92,
            f"mean {worked['working_hours'].mean():.2f} h", color=C["red"], ha="right")
    ax.set_xlabel("Working hours per day")
    ax.set_ylabel("Employee-days")
    ax.set_title("Distribution of daily working hours")
    save(fig, "07_working_hours_distribution.png")


def chart_salary_vs_attendance(pay):
    latest = pay[pay["month"] == pay["month"].max()]
    fig, ax = plt.subplots(figsize=(7.5, 4.5))
    ax.scatter(latest["attendance_pct"], latest["lop_pct_of_gross"], s=60, color=C["blue"],
               edgecolor="white", linewidth=1.5, alpha=0.9)
    ax.set_xlabel("Payable days (% of working days)")
    ax.set_ylabel("Loss-of-pay deduction (% of gross)")
    ax.set_title(f"Attendance vs salary deduction - {pay['month'].max()}")
    save(fig, "08_attendance_vs_lop.png")


def main():
    lines = []

    def log(s=""):
        print(s)
        lines.append(s)

    emp, att, leave, pay = preprocess(*retrieve(DB), log)

    log("KEY FINDINGS")
    log("-" * 60)
    dept_abs = att.groupby("dept_name")["is_absent"].mean().mul(100).round(2).sort_values(ascending=False)
    log("Absent rate by department (%):\n" + dept_abs.to_string())
    top = att[att["status"] == "Absent"].groupby(["emp_id"]).size().sort_values(ascending=False).head(5)
    names = emp.set_index("emp_id")["emp_name"]
    log("\nTop 5 employees by absent days:")
    for eid, n in top.items():
        log(f"  {eid:>3} {names[eid]:<20} {n} days")
    m = pay.groupby("month")[["gross_salary", "net_salary", "lop_deduction"]].sum().round(2)
    log("\nMonthly payroll totals:\n" + m.to_string())
    log(f"\nAverage daily working hours (worked days): {att[att.working_hours > 0].working_hours.mean():.2f}")
    log(f"Weekday with highest absence: {att.groupby('weekday', observed=True)['is_absent'].mean().idxmax()}")
    log("")

    print("Generating charts ...")
    chart_status_mix(att)
    chart_dept_absence(att)
    chart_payroll_trend(pay)
    chart_dept_payroll(pay)
    chart_weekday_absence(att)
    chart_leave_types(leave)
    chart_hours(att)
    chart_salary_vs_attendance(pay)

    att.to_csv(OUT / "clean_attendance.csv", index=False)
    pay.to_csv(OUT / "clean_payroll.csv", index=False)
    (OUT / "analysis_summary.txt").write_text("\n".join(lines), encoding="utf-8")
    print("Done. Outputs in", OUT)


if __name__ == "__main__":
    main()
