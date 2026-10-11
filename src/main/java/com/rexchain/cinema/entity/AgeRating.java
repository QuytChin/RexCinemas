package com.rexchain.cinema.entity;

public enum AgeRating {
    P("P", "P - Phổ biến cho mọi lứa tuổi"),
    K("K", "K - Khán giả dưới 13 tuổi có người giám hộ"),
    C13("C13", "C13 - Cấm khán giả dưới 13 tuổi"),
    C16("C16", "C16 - Cấm khán giả dưới 16 tuổi"),
    C18("C18", "C18 - Cấm khán giả dưới 18 tuổi"),
    // Aliases hỗ trợ tương thích ngược dữ liệu cũ
    T13("C13", "C13 - Cấm khán giả dưới 13 tuổi"),
    T16("C16", "C16 - Cấm khán giả dưới 16 tuổi"),
    T18("C18", "C18 - Cấm khán giả dưới 18 tuổi");

    private final String code;
    private final String description;

    AgeRating(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return code;
    }

    public static AgeRating fromString(String val) {
        if (val == null || val.isBlank()) return P;
        String clean = val.trim().toUpperCase();
        for (AgeRating r : values()) {
            if (r.name().equalsIgnoreCase(clean) || r.code.equalsIgnoreCase(clean)) {
                return r;
            }
        }
        return P;
    }
}
