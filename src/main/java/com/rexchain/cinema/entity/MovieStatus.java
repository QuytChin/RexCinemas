package com.rexchain.cinema.entity;

public enum MovieStatus {
    NOW_SHOWING("Đang chiếu"),
    COMING_SOON("Sắp chiếu"),
    ENDED("Đã kết thúc");

    private final String displayName;

    MovieStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
