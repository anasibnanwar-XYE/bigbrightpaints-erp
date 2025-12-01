-- Attendance tracking table

CREATE TABLE IF NOT EXISTS attendance (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    attendance_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ABSENT',
    check_in_time TIME,
    check_out_time TIME,
    marked_by VARCHAR(255),
    marked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remarks TEXT,
    is_holiday BOOLEAN DEFAULT FALSE,
    is_weekend BOOLEAN DEFAULT FALSE,
    CONSTRAINT uk_attendance_employee_date UNIQUE (company_id, employee_id, attendance_date)
);

CREATE INDEX idx_attendance_date ON attendance(company_id, attendance_date);
CREATE INDEX idx_attendance_employee ON attendance(employee_id, attendance_date);
