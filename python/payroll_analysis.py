
import sqlite3
import matplotlib.pyplot as plt
import os

# Connect to the existing Java SQLite database
DB_PATH = "database/payroll.db"

REPORT_FOLDER = "reports"
os.makedirs(REPORT_FOLDER, exist_ok=True)

try:
    connection = sqlite3.connect(DB_PATH)
    cursor = connection.cursor()

    print("\n===== PAYROLL DATA ANALYSIS SYSTEM =====")

    # 1. Employee-wise attendance summary
    print("\n===== EMPLOYEE ATTENDANCE SUMMARY =====")

    cursor.execute("""
        SELECT e.name,
               COUNT(DISTINCT CASE
                   WHEN a.status = 'Present' THEN a.attendance_date
               END) AS present_days,
               COUNT(DISTINCT CASE
                   WHEN a.status = 'Absent' THEN a.attendance_date
               END) AS absent_days
        FROM employee e
        LEFT JOIN attendance a
        ON e.employee_id = a.employee_id
        GROUP BY e.employee_id, e.name
    """)

    attendance_data = cursor.fetchall()

    for row in attendance_data:
        print(
            f"Employee: {row[0]} | "
            f"Present: {row[1]} | "
            f"Absent: {row[2]}"
        )

    # 2. Department-wise attendance report
    print("\n===== DEPARTMENT-WISE ATTENDANCE =====")

    cursor.execute("""
        SELECT e.department,
               COUNT(DISTINCT a.employee_id) AS employees,
               COUNT(CASE WHEN a.status = 'Present' THEN 1 END)
                    AS present_records,
               COUNT(CASE WHEN a.status = 'Absent' THEN 1 END)
                    AS absent_records
        FROM employee e
        LEFT JOIN attendance a
        ON e.employee_id = a.employee_id
        GROUP BY e.department
    """)

    department_data = cursor.fetchall()

    for row in department_data:
        print(
            f"Department: {row[0]} | "
            f"Employees: {row[1]} | "
            f"Present records: {row[2]} | "
            f"Absent records: {row[3]}"
        )

    # 3. Monthly payroll report
    print("\n===== MONTHLY PAYROLL REPORT =====")

    cursor.execute("""
        SELECT e.name, p.payroll_month,
               p.basic_salary, p.deduction, p.net_salary
        FROM payroll p
        JOIN employee e
        ON p.employee_id = e.employee_id
        ORDER BY p.payroll_month
    """)

    payroll_data = cursor.fetchall()

    for row in payroll_data:
        print(
            f"Employee: {row[0]} | "
            f"Month: {row[1]} | "
            f"Basic: Rs.{row[2]:.2f} | "
            f"Deduction: Rs.{row[3]:.2f} | "
            f"Net Salary: Rs.{row[4]:.2f}"
        )

    # 4. Attendance chart
    if attendance_data:
        names = [row[0] for row in attendance_data]
        present = [row[1] for row in attendance_data]
        absent = [row[2] for row in attendance_data]

        x = range(len(names))
        width = 0.35

        plt.figure(figsize=(8, 5))
        plt.bar(
            [i - width / 2 for i in x],
            present,
            width,
            label="Present"
        )
        plt.bar(
            [i + width / 2 for i in x],
            absent,
            width,
            label="Absent"
        )

        plt.xticks(list(x), names)
        plt.xlabel("Employees")
        plt.ylabel("Number of Days")
        plt.title("Employee Attendance Summary")
        plt.legend()
        plt.tight_layout()

        plt.savefig(os.path.join(
            REPORT_FOLDER, "attendance_chart.png"
        ))
        plt.close()

    # 5. Department-wise attendance chart
    if department_data:
        departments = [row[0] for row in department_data]
        present_records = [row[2] for row in department_data]
        absent_records = [row[3] for row in department_data]

        x = range(len(departments))
        width = 0.35

        plt.figure(figsize=(8, 5))
        plt.bar(
            [i - width / 2 for i in x],
            present_records,
            width,
            label="Present"
        )
        plt.bar(
            [i + width / 2 for i in x],
            absent_records,
            width,
            label="Absent"
        )

        plt.xticks(list(x), departments)
        plt.xlabel("Department")
        plt.ylabel("Attendance Records")
        plt.title("Department-wise Attendance")
        plt.legend()
        plt.tight_layout()

        plt.savefig(os.path.join(
            REPORT_FOLDER, "department_attendance.png"
        ))
        plt.show()

    # 6. Salary comparison chart
    if payroll_data:
        employee_names = [row[0] for row in payroll_data]
        salaries = [row[4] for row in payroll_data]

        plt.figure(figsize=(8, 5))
        plt.bar(employee_names, salaries)
        plt.xlabel("Employees")
        plt.ylabel("Net Salary (Rs.)")
        plt.title("Employee Monthly Salary Comparison")
        plt.tight_layout()

        plt.savefig(os.path.join(
            REPORT_FOLDER, "salary_chart.png"
        ))
        plt.show()

    print("\nAnalysis completed successfully!")
    print("Charts saved inside the reports folder.")

except sqlite3.Error as error:
    print("Database error:", error)

finally:
    if 'connection' in locals():
        connection.close()
