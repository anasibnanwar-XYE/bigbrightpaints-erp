package com.bigbrightpaints.erp.modules.hr.domain;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Daily attendance record for employees and labourers.
 * Staff marks their own attendance, Factory marks labourers.
 */
@Entity
@Table(name = "attendance", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "employee_id", "attendance_date"}),
       indexes = {
           @Index(name = "idx_attendance_date", columnList = "company_id, attendance_date"),
           @Index(name = "idx_attendance_employee", columnList = "employee_id, attendance_date")
       })
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status = AttendanceStatus.ABSENT;

    @Column(name = "check_in_time")
    private LocalTime checkInTime;

    @Column(name = "check_out_time")
    private LocalTime checkOutTime;

    @Column(name = "marked_by")
    private String markedBy; // Who marked this attendance (username)

    @Column(name = "marked_at")
    private Instant markedAt;

    private String remarks;

    @Column(name = "is_holiday")
    private boolean holiday = false;

    @Column(name = "is_weekend")
    private boolean weekend = false;

    @PrePersist
    public void prePersist() {
        if (markedAt == null) {
            markedAt = Instant.now();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }
    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }
    public LocalTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalTime checkInTime) { this.checkInTime = checkInTime; }
    public LocalTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalTime checkOutTime) { this.checkOutTime = checkOutTime; }
    public String getMarkedBy() { return markedBy; }
    public void setMarkedBy(String markedBy) { this.markedBy = markedBy; }
    public Instant getMarkedAt() { return markedAt; }
    public void setMarkedAt(Instant markedAt) { this.markedAt = markedAt; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public boolean isHoliday() { return holiday; }
    public void setHoliday(boolean holiday) { this.holiday = holiday; }
    public boolean isWeekend() { return weekend; }
    public void setWeekend(boolean weekend) { this.weekend = weekend; }

    public enum AttendanceStatus {
        PRESENT,      // Full day
        HALF_DAY,     // Half day (0.5 day pay)
        ABSENT,       // No pay
        LEAVE,        // Approved leave (may or may not have pay)
        HOLIDAY,      // Paid holiday
        WEEKEND       // Weekend (typically no work)
    }
}
