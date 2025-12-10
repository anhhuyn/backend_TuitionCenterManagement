package com.management.student_center.dto.tuition;

public class TuitionPaymentRequest {
    private Long studentId;
    private int month;
    private int year;

    // Constructor mặc định (Bắt buộc để Jackson deserialize JSON)
    public TuitionPaymentRequest() {
    }

    // Constructor đầy đủ (Tùy chọn, tiện cho test)
    public TuitionPaymentRequest(Long studentId, int month, int year) {
        this.studentId = studentId;
        this.month = month;
        this.year = year;
    }

    // Getters & Setters
    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }
}