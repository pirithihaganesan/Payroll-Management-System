\# Database Normalization – PayTrack



\## Introduction

Normalization is the process of organizing data into tables to reduce redundancy and improve data integrity.



The PayTrack Employee Attendance and Payroll Management System uses a relational database structure organized according to normalization principles.



\## First Normal Form (1NF)

A table is in 1NF when each column contains atomic values and there are no repeating groups.



In PayTrack, each attendance record stores one employee's attendance for one date. Multiple attendance records are stored as separate rows.



\## Second Normal Form (2NF)

A table is in 2NF when it is in 1NF and every non-key attribute depends on the whole primary key.



PayTrack separates employee information from attendance, leave, and payroll records. Each table has its own primary key, such as emp\_id, att\_id, leave\_id, and payroll\_id.



\## Third Normal Form (3NF)

A table is in 3NF when it is in 2NF and non-key attributes depend only on the primary key.



PayTrack stores department and designation information in separate tables. The employee table references them using dept\_id and desig\_id foreign keys, reducing repeated data.



\## Tables Used



| Table | Primary Key | Purpose |

|---|---|---|

| department | dept\_id | Department information |

| designation | desig\_id | Designation information |

| employee | emp\_id | Employee information |

| attendance | att\_id | Attendance records |

| leave\_record | leave\_id | Leave records |

| payroll | payroll\_id | Payroll records |



\## Relationships

\- Department to Employee: One-to-Many

\- Designation to Employee: One-to-Many

\- Employee to Attendance: One-to-Many

\- Employee to Leave Record: One-to-Many

\- Employee to Payroll: One-to-Many



\## Benefits

\- Reduces data redundancy.

\- Avoids insertion, update, and deletion anomalies.

\- Improves data consistency.

\- Maintains referential integrity.

\- Makes database maintenance easier.



\## Conclusion

The PayTrack database is organized using normalization principles up to 3NF, with separate related tables, primary keys, and foreign keys to improve data organization and consistency.



