SELECT employee_id, attendance_date, status, regular_hours, overtime_hours
FROM attendance
ORDER BY attendance_date DESC
LIMIT 10;
