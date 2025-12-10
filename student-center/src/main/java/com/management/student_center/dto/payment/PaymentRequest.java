package com.management.student_center.dto.payment;

public class PaymentRequest {
    private Long teacherId;
    private int month;
    private int year;

    public PaymentRequest() {
    }

    public PaymentRequest(Long teacherId, int month, int year) {
        this.teacherId = teacherId;
        this.month = month;
        this.year = year;
    }

    // Getters & Setters
    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
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