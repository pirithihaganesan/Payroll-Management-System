
import { useEffect, useMemo, useState } from "react";
import "./App.css";

const API = "http://localhost:8080/api";

function money(value) {
  return `₹${Number(value || 0).toLocaleString("en-IN", {
    maximumFractionDigits: 2
  })}`;
}

function App() {
  const [employees, setEmployees] = useState([]);
  const [attendance, setAttendance] = useState([]);
  const [leaves, setLeaves] = useState([]);
  const [payroll, setPayroll] = useState([]);
  const [reports, setReports] = useState(null);

  const [search, setSearch] = useState("");
  const [activePage, setActivePage] = useState("Dashboard");
  const [month, setMonth] = useState(
    new Date().toISOString().slice(0, 7)
  );

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;

    async function loadData() {
      setLoading(true);
      setError("");

      try {
        const urls = [
          `${API}/employees`,
          `${API}/attendance?month=${month}`,
          `${API}/leaves`,
          `${API}/payroll?month=${month}`,
          `${API}/reports?month=${month}`
        ];

        const responses = await Promise.all(
          urls.map((url) => fetch(url))
        );

        if (responses.some((response) => !response.ok)) {
          throw new Error("One or more API requests failed.");
        }

        const data = await Promise.all(
          responses.map((response) => response.json())
        );

        if (!cancelled) {
          setEmployees(data[0]);
          setAttendance(data[1]);
          setLeaves(data[2]);
          setPayroll(data[3]);
          setReports(data[4]);
        }
      } catch {
        if (!cancelled) {
          setError(
            "Unable to load data. Check that the Java backend is running."
          );
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    loadData();

    return () => {
      cancelled = true;
    };
  }, [month]);

  const activeEmployees = employees.filter(
    (employee) => employee.status === "Active"
  );

  const departments = new Set(
    employees.map((employee) => employee.department)
  ).size;

  const totalSalary = employees.reduce(
    (total, employee) => total + Number(employee.salary || 0),
    0
  );

  const filteredEmployees = useMemo(
    () =>
      employees.filter((employee) =>
        `${employee.name} ${employee.department} ${employee.designation}`
          .toLowerCase()
          .includes(search.toLowerCase())
      ),
    [employees, search]
  );

  const navigation = [
    { name: "Dashboard", icon: "▦" },
    { name: "Employees", icon: "♙" },
    { name: "Attendance", icon: "◷" },
    { name: "Leave Management", icon: "▤" },
    { name: "Payroll", icon: "₹" },
    { name: "Reports", icon: "▥" }
  ];

  const reportAttendance = reports?.attendance?.[0] || {};
  const reportPayroll = reports?.payroll?.[0] || {};

  function renderTable(headers, rows, emptyMessage) {
    return (
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              {headers.map((header) => (
                <th key={header}>{header}</th>
              ))}
            </tr>
          </thead>
          <tbody>{rows}</tbody>
        </table>
        {rows.length === 0 && (
          <p className="message">{emptyMessage}</p>
        )}
      </div>
    );
  }

  function renderEmployees() {
    return (
      <section className="panel">
        <div className="panel-heading">
          <div>
            <h2>Employee Details</h2>
            <p>Registered employee directory</p>
          </div>
          <input
            className="search"
            type="text"
            placeholder="Search employee..."
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>

        {renderTable(
          [
            "ID",
            "EMPLOYEE NAME",
            "DEPARTMENT",
            "DESIGNATION",
            "BASIC SALARY",
            "JOIN DATE",
            "STATUS"
          ],
          filteredEmployees.map((employee) => (
            <tr key={employee.id}>
              <td>#{employee.id}</td>
              <td><strong>{employee.name}</strong></td>
              <td>{employee.department}</td>
              <td>{employee.designation}</td>
              <td>{money(employee.salary)}</td>
              <td>{employee.joinDate}</td>
              <td>
                <span className="status">{employee.status}</span>
              </td>
            </tr>
          )),
          "No employees found."
        )}

        <div className="table-footer">
          Showing {filteredEmployees.length} of {employees.length} employees
        </div>
      </section>
    );
  }

  function renderAttendance() {
    return (
      <section className="panel">
        <div className="panel-heading">
          <div>
            <h2>Attendance Records</h2>
            <p>Employee attendance for the selected month</p>
          </div>
          <input
            className="search"
            type="month"
            value={month}
            onChange={(event) => setMonth(event.target.value)}
          />
        </div>

        {renderTable(
          [
            "RECORD ID",
            "EMPLOYEE",
            "DATE",
            "STATUS",
            "CHECK IN",
            "CHECK OUT",
            "WORKING HOURS"
          ],
          attendance.map((item) => (
            <tr key={item.att_id}>
              <td>#{item.att_id}</td>
              <td>{item.emp_name} (#{item.emp_id})</td>
              <td>{item.att_date}</td>
              <td><span className="status">{item.status}</span></td>
              <td>{item.check_in || "-"}</td>
              <td>{item.check_out || "-"}</td>
              <td>{item.working_hours ?? "-"}</td>
            </tr>
          )),
          "No attendance records found for this month."
        )}
      </section>
    );
  }

  function renderLeaves() {
    return (
      <section className="panel">
        <div className="panel-heading">
          <div>
            <h2>Leave Management</h2>
            <p>Employee leave applications and status</p>
          </div>
        </div>

        {renderTable(
          [
            "LEAVE ID",
            "EMPLOYEE",
            "LEAVE TYPE",
            "START DATE",
            "END DATE",
            "DAYS",
            "REASON",
            "STATUS"
          ],
          leaves.map((item) => (
            <tr key={item.leave_id}>
              <td>#{item.leave_id}</td>
              <td>{item.emp_name} (#{item.emp_id})</td>
              <td>{item.leave_type}</td>
              <td>{item.start_date}</td>
              <td>{item.end_date}</td>
              <td>{item.days}</td>
              <td>{item.reason || "-"}</td>
              <td><span className="status">{item.status}</span></td>
            </tr>
          )),
          "No leave records found."
        )}
      </section>
    );
  }

  function renderPayroll() {
    return (
      <section className="panel">
        <div className="panel-heading">
          <div>
            <h2>Payroll Records</h2>
            <p>Employee salary details for the selected month</p>
          </div>
          <input
            className="search"
            type="month"
            value={month}
            onChange={(event) => setMonth(event.target.value)}
          />
        </div>

        {renderTable(
          [
            "EMPLOYEE",
            "MONTH",
            "WORKING DAYS",
            "PAYABLE DAYS",
            "GROSS SALARY",
            "DEDUCTIONS",
            "NET SALARY"
          ],
          payroll.map((item) => (
            <tr key={item.payroll_id}>
              <td>{item.emp_name} (#{item.emp_id})</td>
              <td>
                {item.pay_year}-{String(item.pay_month).padStart(2, "0")}
              </td>
              <td>{item.working_days}</td>
              <td>{item.payable_days}</td>
              <td>{money(item.gross_salary)}</td>
              <td>{money(item.total_deductions)}</td>
              <td><strong>{money(item.net_salary)}</strong></td>
            </tr>
          )),
          "No payroll records found for this month."
        )}
      </section>
    );
  }

  function renderReports() {
    return (
      <section className="panel">
        <div className="panel-heading">
          <div>
            <h2>Monthly Reports</h2>
            <p>Attendance and payroll summary for {month}</p>
          </div>
          <input
            className="search"
            type="month"
            value={month}
            onChange={(event) => setMonth(event.target.value)}
          />
        </div>

        <section className="stats">
          <div className="stat-card">
            <p>Total Attendance Records</p>
            <h2>{reportAttendance.totalRecords || 0}</h2>
          </div>
          <div className="stat-card">
            <p>Present</p>
            <h2>{reportAttendance.present || 0}</h2>
          </div>
          <div className="stat-card">
            <p>Absent</p>
            <h2>{reportAttendance.absent || 0}</h2>
          </div>
          <div className="stat-card">
            <p>Payroll Count</p>
            <h2>{reportPayroll.payrollCount || 0}</h2>
          </div>
        </section>

        <h3>Attendance Summary</h3>
        <p>Half Days: {reportAttendance.halfDay || 0}</p>
        <p>Paid Leave: {reportAttendance.paidLeave || 0}</p>
        <p>Unpaid Leave: {reportAttendance.unpaidLeave || 0}</p>

        <h3>Payroll Summary</h3>
        <p>Total Gross Salary: {money(reportPayroll.totalGross)}</p>
        <p>Total Deductions: {money(reportPayroll.totalDeductions)}</p>
        <p>Total Net Salary: {money(reportPayroll.totalNet)}</p>
      </section>
    );
  }

  function renderDashboard() {
    return (
      <>
        <section className="welcome">
          <div>
            <p className="welcome-small">EMPLOYEE MANAGEMENT</p>
            <h2>Welcome to your dashboard</h2>
            <p>
              Manage your workforce, attendance and payroll in one place.
            </p>
          </div>
          <div className="welcome-art">👥</div>
        </section>

        <section className="stats">
          <div className="stat-card">
            <div className="stat-icon blue">♙</div>
            <p>Total Employees</p>
            <h2>{employees.length}</h2>
            <span>Registered employees</span>
          </div>

          <div className="stat-card">
            <div className="stat-icon green">✓</div>
            <p>Active Employees</p>
            <h2>{activeEmployees.length}</h2>
            <span>Currently active</span>
          </div>

          <div className="stat-card">
            <div className="stat-icon purple">▦</div>
            <p>Departments</p>
            <h2>{departments}</h2>
            <span>Across the organization</span>
          </div>

          <div className="stat-card">
            <div className="stat-icon orange">₹</div>
            <p>Total Basic Salary</p>
            <h2>{money(totalSalary)}</h2>
            <span>Monthly basic salary total</span>
          </div>
        </section>

        {renderEmployees()}
      </>
    );
  }

  function renderPage() {
    switch (activePage) {
      case "Dashboard":
        return renderDashboard();
      case "Employees":
        return renderEmployees();
      case "Attendance":
        return renderAttendance();
      case "Leave Management":
        return renderLeaves();
      case "Payroll":
        return renderPayroll();
      case "Reports":
        return renderReports();
      default:
        return renderDashboard();
    }
  }

  return (
    <div className="app">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-icon">P</div>
          <div>
            <h2>PayTrack</h2>
            <span>Management System</span>
          </div>
        </div>

        <p className="menu-label">MAIN MENU</p>

        <nav>
          {navigation.map((item) => (
            <button
              key={item.name}
              className={`nav-item ${
                activePage === item.name ? "selected" : ""
              }`}
              onClick={() => setActivePage(item.name)}
            >
              <span className="nav-icon">{item.icon}</span>
              {item.name}
            </button>
          ))}
        </nav>

        <div className="sidebar-bottom">
          <div className="profile-avatar">AD</div>
          <div>
            <strong>Administrator</strong>
            <span>HR Department</span>
          </div>
        </div>
      </aside>

      <main className="main">
        <header className="topbar">
          <div>
            <p className="breadcrumb">Pages / {activePage}</p>
            <h1>{activePage}</h1>
          </div>

          <div className="topbar-right">
            <span className="online-dot"></span>
            <span>System Online</span>
            <div className="top-avatar">AD</div>
          </div>
        </header>

        {loading && <p className="message">Loading project data...</p>}
        {error && <p className="error">{error}</p>}

        {!loading && !error && renderPage()}

        <footer>
          © 2026 PayTrack Employee Attendance &amp; Payroll Management System
        </footer>
      </main>
    </div>
  );
}

export default App;
