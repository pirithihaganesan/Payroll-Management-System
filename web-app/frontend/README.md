
# PayTrack – Employee Attendance and Payroll Management System

## Project Overview

PayTrack is an Employee Attendance and Payroll Management System developed to simplify employee information management, attendance tracking, leave management, and salary processing.

The system provides a web-based dashboard where administrators can view employee details, monitor attendance, manage leave records, and view payroll information.

The application uses Java for backend processing, SQLite for database management, and React for the frontend interface.

## Features

### 1. Dashboard
- View total number of employees.
- View active employees.
- View department details.
- View total basic salary information.

### 2. Employee Management
- View employee records.
- Display employee ID, name, department, designation, salary, joining date, and status.

### 3. Attendance Management
- View employee attendance records.
- Filter attendance records by month.
- Display attendance status, check-in time, check-out time, and working hours.

### 4. Leave Management
- View employee leave records.
- Filter leave records based on leave status.
- Display leave details and approval status.

### 5. Payroll Management
- View monthly payroll records.
- Display employee salary information.
- View payroll details.

### 6. Reports
- Access monthly employee, attendance, and payroll information.

## Technologies Used

| Component | Technology |
|---|---|
| Frontend | React.js |
| Build Tool | Vite |
| Programming Language | Java |
| Backend API | Java HttpServer |
| Database | SQLite |
| Database Connectivity | JDBC |
| Styling | CSS |
| Version Control | Git and GitHub |

## Project Structure

```text
Payroll-Management-System/
│
├── java-app/
│   ├── src/
│   ├── lib/
│   └── out/
│
├── database/
│
├── web-app/
│   ├── WebServer.java
│   ├── payroll-web.db
│   │
│   └── frontend/
│       ├── public/
│       ├── src/
│       │   ├── assets/
│       │   ├── App.jsx
│       │   ├── App.css
│       │   ├── index.css
│       │   └── main.jsx
│       │
│       ├── package.json
│       └── vite.config.js
│
└── README.md
```

## Requirements

- Java JDK 21 or later
- Node.js and npm
- SQLite JDBC Driver
- Visual Studio Code or any Java-compatible editor
- Web browser

## How to Run the Project

### Step 1: Start the Backend

Open CMD in the project root directory.

Compile the backend:

```cmd
javac -cp "java-app\out;java-app\lib\sqlite-jdbc-3.46.1.3.jar" -d web-app web-app\WebServer.java
```

Run the backend:

```cmd
java -cp "web-app;java-app\out;java-app\lib\sqlite-jdbc-3.46.1.3.jar" WebServer
```

The backend API runs at:

```text
http://localhost:8080
```

### Step 2: Start the Frontend

Open another CMD window.

Navigate to the frontend folder:

```cmd
cd /d "C:\Users\dell user\Documents\GitHub\Payroll-Management-System-17Nov2026\web-app\frontend"
```

Install dependencies:

```cmd
npm install
```

Start the React application:

```cmd
npm run dev
```

Open the frontend in your browser using the URL displayed by Vite, usually:

```text
http://localhost:5173
```

## Backend API Endpoints

The application currently provides the following GET API endpoints:

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/employees` | Retrieve employee records |
| GET | `/api/attendance` | Retrieve attendance records |
| GET | `/api/leaves` | Retrieve leave records |
| GET | `/api/payroll` | Retrieve payroll records |
| GET | `/api/reports` | Retrieve monthly reports |

Optional filters are supported for attendance, leaves, payroll, and reports.

## Database

The application uses SQLite to store employee, attendance, leave, and payroll information.

The web application database is:

```text
web-app/payroll-web.db
```

The database contains fictional sample employee records for demonstration and testing purposes.

## Project Objective

The main objective of PayTrack is to reduce manual work in employee attendance and payroll administration by providing a centralized system for managing employee-related information.

## Future Enhancements

- Employee login and administrator authentication.
- Add, update, and delete operations through the web interface.
- Attendance marking through the web application.
- Leave application and approval through the web application.
- Automated monthly payroll generation.
- Export payroll and attendance reports.


## Author

Developed as an academic project.

**Project:** Employee Attendance and Payroll Management System

**Application Name:** PayTrack
