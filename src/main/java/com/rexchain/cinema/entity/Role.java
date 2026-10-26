package com.rexchain.cinema.entity;

public enum Role {
    CUSTOMER, // Khách hàng
    STAFF, // Nhân viên soát vé tại rạp
    SUPERVISOR, // Giám sát rạp (tối đa 3 người / rạp)
    CINEMA_MANAGER, // Tổng quản trị vận hành chuỗi
    ADMIN // Quản trị viên hệ thống kỹ thuật
}
