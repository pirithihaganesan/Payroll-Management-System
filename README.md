\# Employee Attendance and Payroll Management System



\## 1. Project Overview



The Employee Attendance and Payroll Management System is a software application developed using Java, SQLite, and Python.



The system manages employee details, daily attendance, leave records, and monthly payroll calculations. Python is used to analyse attendance and payroll data and generate visual reports.



\## 2. Technologies Used



\- Java 21 – Application development

\- SQLite3 – Database management

\- SQLite JDBC – Database connectivity

\- Python 3 – Data analysis

\- Matplotlib – Data visualization



\## 3. Project Modules



\### Employee Management

\- Add employee details.

\- Store employee ID, name, department, designation, and basic salary.

\- View employee records.



\### Attendance Management

\- Mark daily attendance.

\- Store attendance date and status.

\- View attendance records.



\### Leave Management

\- Apply for leave.

\- Store leave date and reason.

\- View leave records.

\- Count leave days.



\### Payroll Management

\- Calculate monthly salary.

\- Retrieve employee attendance details.

\- Calculate absent-day deductions.

\- Generate monthly payroll reports.



\### Python Data Analysis

\- Retrieve information from SQLite.

\- Generate employee-wise attendance summaries.

\- Generate department-wise attendance reports.

\- Display monthly payroll reports.

\- Generate charts using Matplotlib.



\## 4. Payroll Calculation Formula



Daily Salary = Basic Salary / Working Days



Deduction = Daily Salary × Recorded Absent Days



Net Salary = Basic Salary − Deduction



Note: This is a sample payroll calculation rule used for demonstration.



\## 5. Project Folder Structure



```text

Payroll-Management-System/

│

├── database/

│   └── payroll.db

│

├── lib/

│   └── sqlite-jdbc-3.54.1.0.jar

│

├── python/

│   └── payroll\_analysis.py

│

├── reports/

│   ├── attendance\_chart.png

│   ├── department\_attendance.png

│   └── salary\_chart.png

│

├── src/

│   ├── DatabaseConnection.java

│   ├── EmployeeManagement.java

│   ├── AttendanceManagement.java

│   ├── LeaveManagement.java

│   └── PayrollManagement.java

│

└── README.md

```



\## 6. Requirements



\- Java JDK 21

\- Python 3

\- SQLite JDBC driver

\- Matplotlib library



\## 7. How to Execute the Project



Open CMD inside the project directory.



\### Compile Java Files



```cmd

javac -cp "lib\\sqlite-jdbc-3.54.1.0.jar" -d src src\\\*.java

```



\### Initialize Database



```cmd

java -cp "src;lib\\sqlite-jdbc-3.54.1.0.jar" DatabaseConnection

```



\### Run Employee Management



```cmd

java -cp "src;lib\\sqlite-jdbc-3.54.1.0.jar" EmployeeManagement

```



\### Run Attendance Management



```cmd

java -cp "src;lib\\sqlite-jdbc-3.54.1.0.jar" AttendanceManagement

```



\### Run Leave Management



```cmd

java -cp "src;lib\\sqlite-jdbc-3.54.1.0.jar" LeaveManagement

```



\### Run Payroll Management



```cmd

java -cp "src;lib\\sqlite-jdbc-3.54.1.0.jar" PayrollManagement

```



\### Install Matplotlib



```cmd

python -m pip install matplotlib

```



\### Run Python Analysis



```cmd

python python\\payroll\_analysis.py

```



\## 8. Generated Reports



The Python analysis program generates the following charts inside the reports folder:



1\. attendance\_chart.png

2\. department\_attendance.png

3\. salary\_chart.png



\## 9. Conclusion



The Employee Attendance and Payroll Management System demonstrates the integration of Java, SQLite, and Python. It provides employee management, attendance tracking, leave management, payroll calculation, data analysis, and graphical reports.

