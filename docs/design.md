# System Design

Diagrams are written in Mermaid; GitHub renders them automatically.

## 1. Use Case Diagram

```mermaid
flowchart LR
    Admin(["HR / Payroll Administrator"])
    Mgmt(["Management / Analyst"])

    subgraph SYS["Employee Attendance & Payroll System"]
        UC1(["Manage employees"])
        UC2(["Record attendance"])
        UC3(["Apply for leave"])
        UC4(["Approve / reject leave"])
        UC5(["Generate payroll"])
        UC6(["View / print payslip"])
        UC7(["Generate reports"])
        UC8(["Analyse attendance & payroll (Python)"])
        V1(["Validate input"])
        V2(["Check duplicate / weekend / future date"])
    end

    Admin --> UC1
    Admin --> UC2
    Admin --> UC3
    Admin --> UC4
    Admin --> UC5
    Admin --> UC6
    Admin --> UC7
    Mgmt --> UC7
    Mgmt --> UC8
    UC1 -. include .-> V1
    UC2 -. include .-> V1
    UC2 -. include .-> V2
    UC4 -. extend: writes attendance .-> UC2
    UC5 -. include .-> UC2
```

| Use case | Description |
|---|---|
| Manage employees | Add, view, search, update salary/department/designation, deactivate |
| Record attendance | Present / Absent / Half-Day with check-in and check-out |
| Apply for leave | Casual, Sick, Earned or Loss of Pay for a date range |
| Approve / reject leave | Approval creates Paid/Unpaid Leave attendance rows |
| Generate payroll | One employee or all, for a month, using salary rules |
| Generate reports | Attendance summary, department attendance, payroll, absentees, leave |
| Analyse (Python) | Retrieve, preprocess and visualize data |

## 2. Class Diagram

```mermaid
classDiagram
    class Employee {
        -int empId
        -String name
        -String department
        -String designation
        -double basicSalary
        -LocalDate joinDate
        -String status
        +isActive() boolean
    }
    class AttendanceRecord {
        -int empId
        -LocalDate date
        -AttendanceStatus status
        -String checkIn
        -String checkOut
        -double workingHours
    }
    class LeaveRecord {
        -int leaveId
        -int empId
        -LeaveType type
        -LocalDate startDate
        -LocalDate endDate
        -int days
        -String reason
        -String status
    }
    class PayrollRecord {
        -int empId
        -YearMonth period
        -int workingDays
        -double payableDays
        -double lopDays
        -double gross
        -double pf
        -double profTax
        -double lopDeduction
        -double net
        +toPayslip() String
    }
    class AttendanceStatus {
        <<enumeration>>
        PRESENT
        ABSENT
        HALF_DAY
        PAID_LEAVE
        UNPAID_LEAVE
    }
    class LeaveType {
        <<enumeration>>
        CASUAL
        SICK
        EARNED
        LOSS_OF_PAY
        +isPaid() boolean
    }
    class SalaryRules {
        <<utility>>
        +HRA_RATE
        +DA_RATE
        +PF_RATE
        +PROF_TAX
    }
    class PayrollCalculator {
        <<utility>>
        +workingDays(YearMonth) int
        +workingDaysBetween(from,to) int
        +calculate(...) PayrollRecord
        +round2(double) double
    }
    class Validator {
        <<utility>>
        +name(String) String
        +salary(double) double
        +date(String) LocalDate
        +month(String) YearMonth
        +hoursBetween(in,out) double
    }
    class Database {
        +getConnection() Connection
        +runScript(Path)
        +isInitialised() boolean
    }
    class EmployeeDAO {
        +add(...) int
        +findById(int) Employee
        +listAll(boolean) List
        +updateSalary(int,double) boolean
        +deactivate(int) boolean
    }
    class AttendanceDAO {
        +mark(Employee,date,status,in,out)
        +monthCounts(int,YearMonth) int[]
    }
    class LeaveDAO {
        +apply(...) int
        +approve(int)
        +reject(int)
    }
    class PayrollDAO {
        +generate(int,YearMonth) PayrollRecord
        +generateAll(YearMonth) int
        +find(int,YearMonth) PayrollRecord
    }
    class ReportService {
        +attendanceSummary(YearMonth)
        +departmentAttendance(YearMonth)
        +payrollReport(YearMonth)
        +frequentAbsentees(int)
    }
    class Console
    class Main

    Employee "1" --> "*" AttendanceRecord : has
    Employee "1" --> "*" LeaveRecord : applies
    Employee "1" --> "*" PayrollRecord : paid
    AttendanceRecord --> AttendanceStatus
    LeaveRecord --> LeaveType
    PayrollCalculator ..> SalaryRules
    PayrollCalculator ..> PayrollRecord : creates
    PayrollDAO ..> PayrollCalculator
    PayrollDAO --> AttendanceDAO
    PayrollDAO --> EmployeeDAO
    LeaveDAO --> AttendanceDAO
    AttendanceDAO ..> Validator
    EmployeeDAO --> Database
    AttendanceDAO --> Database
    LeaveDAO --> Database
    PayrollDAO --> Database
    ReportService --> PayrollDAO
    Main --> Console
    Main --> EmployeeDAO
    Main --> AttendanceDAO
    Main --> LeaveDAO
    Main --> PayrollDAO
    Main --> ReportService
```

## 3. System Architecture

```mermaid
flowchart TB
    subgraph UI["Presentation layer"]
        M["Main (menus)"]
        C["Console (validated input)"]
    end
    subgraph BL["Business logic layer"]
        PC["PayrollCalculator"]
        V["Validator"]
        SR["SalaryRules"]
    end
    subgraph DL["Data access layer (JDBC)"]
        E["EmployeeDAO"]
        A["AttendanceDAO"]
        L["LeaveDAO"]
        P["PayrollDAO"]
        R["ReportService"]
    end
    DB[("SQLite database\npayroll.db")]
    subgraph PY["Analytics layer (Python)"]
        RET["Retrieve: SQL + pandas"]
        PRE["Preprocess"]
        VIZ["Visualize: matplotlib"]
        OUT["PNG charts, CSV, summary"]
    end

    M --> C
    M --> E & A & L & P & R
    A --> V
    P --> PC --> SR
    E & A & L & P & R --> DB
    DB --> RET --> PRE --> VIZ --> OUT
```

**Layers:** presentation (console menus and validated input), business logic (pure, unit-tested payroll and validation), data access (one DAO per aggregate, prepared statements only), database (SQLite with constraints), analytics (independent Python pipeline reading the same database).

## 4. Payroll generation flow

```mermaid
flowchart TD
    S([Start: employee + month]) --> A{Employee active?}
    A -- no --> X1[Reject]
    A -- yes --> B{Month valid? not future, after joining}
    B -- no --> X1
    B -- yes --> C{Payslip already exists?}
    C -- yes --> X1
    C -- no --> D{Attendance recorded this month?}
    D -- no --> X1
    D -- yes --> E[Count present, half-day, paid-leave days]
    E --> F[payable = present + 0.5 x half + paid leave\ncap at working days]
    F --> G[LOP days = working days - payable]
    G --> H[HRA, DA, gross, PF, professional tax]
    H --> I[LOP deduction = gross / working days x LOP days]
    I --> J[net = gross - deductions, floor 0]
    J --> K[(Save payslip snapshot)]
    K --> Z([Print payslip])
```

## 5. Leave approval sequence

```mermaid
sequenceDiagram
    actor Admin
    participant Main
    participant LeaveDAO
    participant AttendanceDAO
    participant DB
    Admin->>Main: Approve leave #id
    Main->>LeaveDAO: approve(id)
    LeaveDAO->>DB: load leave, check status = Pending
    LeaveDAO->>DB: BEGIN
    loop each working day in range
        LeaveDAO->>AttendanceDAO: exists(emp, day)?
        AttendanceDAO->>DB: SELECT
        LeaveDAO->>AttendanceDAO: insert Paid/Unpaid Leave
        AttendanceDAO->>DB: INSERT
    end
    LeaveDAO->>DB: UPDATE leave status = Approved
    LeaveDAO->>DB: COMMIT (ROLLBACK on any failure)
    LeaveDAO-->>Main: done
    Main-->>Admin: "Leave approved; attendance updated"
```
