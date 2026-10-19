package com.rexchain.cinema.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings", indexes = {
    @Index(name = "idx_bookings_status_created_at", columnList = "status, created_at"),
    @Index(name = "idx_bookings_user_created_at", columnList = "user_id, created_at"),
    @Index(name = "idx_bookings_payment_reference", columnList = "payment_method, payment_reference")
})
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version @Column(nullable = false)
    private Long version;

    @Column(nullable = false, unique = true, length = 30)
    private String bookingCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "showtime_id")
    private Showtime showtime;

    @Column(precision = 12, scale = 2)
    private BigDecimal seatAmount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal comboAmount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(length = 40)
    private String voucherCode;

    @Column(length = 120)
    private String paymentReference;

    @Column(length = 80, unique = true)
    private String sepayTransactionId;

    private LocalDateTime paidAt;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BookingStatus status = BookingStatus.CONFIRMED;

    @Column(nullable = false)
    private boolean pointsAwarded = false;

    @Column(nullable = false)
    private int earnedPoints = 0;

    @Column(nullable = false)
    private boolean pointsReversed = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private MembershipTier membershipTierSnapshot;

    @Column(precision = 7, scale = 4)
    private BigDecimal membershipDiscountPercent = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal membershipDiscountAmount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal voucherDiscountAmount = BigDecimal.ZERO;

    @Column(precision = 7, scale = 4)
    private BigDecimal pointMultiplier = BigDecimal.ONE;

    /** Secret random token encoded into the QR. Never show it as the human booking code. */
    @Column(unique = true, length = 80)
    private String qrToken;

    /** Cinema gate check-in audit fields. */
    private LocalDateTime checkedInAt;

    @Column(length = 180)
    private String checkedInBy;

    /** Refund audit fields. */
    private LocalDateTime refundedAt;

    @Column(length = 180)
    private String refundedBy;

    @Column(length = 255)
    private String refundNote;

    /** Manual bank-transfer reconciliation evidence. Set only once the refund is verified. */
    @Column(length = 120)
    private String refundReference;

    @Column(precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column(length = 600)
    private String refundEvidence;

    private Long refundedByUserId;

    /** When this PENDING booking should be auto-expired by the scheduled job. */
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @org.hibernate.annotations.BatchSize(size = 50)
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingSeat> seats = new ArrayList<>();

    @org.hibernate.annotations.BatchSize(size = 50)
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingCombo> combos = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Showtime getShowtime() { return showtime; }
    public void setShowtime(Showtime showtime) { this.showtime = showtime; }
    public BigDecimal getSeatAmount() { return seatAmount == null ? BigDecimal.ZERO : seatAmount; }
    public void setSeatAmount(BigDecimal seatAmount) { this.seatAmount = seatAmount; }
    public BigDecimal getComboAmount() { return comboAmount == null ? BigDecimal.ZERO : comboAmount; }
    public void setComboAmount(BigDecimal comboAmount) { this.comboAmount = comboAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount == null ? BigDecimal.ZERO : discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
    public String getSepayTransactionId() { return sepayTransactionId; }
    public void setSepayTransactionId(String sepayTransactionId) { this.sepayTransactionId = sepayTransactionId; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public boolean isPointsAwarded() { return pointsAwarded; }
    public void setPointsAwarded(boolean pointsAwarded) { this.pointsAwarded = pointsAwarded; }
    public int getEarnedPoints() { return earnedPoints; }
    public void setEarnedPoints(int earnedPoints) { this.earnedPoints = Math.max(0, earnedPoints); }
    public boolean isPointsReversed() { return pointsReversed; }
    public void setPointsReversed(boolean pointsReversed) { this.pointsReversed = pointsReversed; }
    public MembershipTier getMembershipTierSnapshot() { return membershipTierSnapshot; }
    public void setMembershipTierSnapshot(MembershipTier membershipTierSnapshot) { this.membershipTierSnapshot = membershipTierSnapshot; }
    public BigDecimal getMembershipDiscountPercent() { return membershipDiscountPercent == null ? BigDecimal.ZERO : membershipDiscountPercent; }
    public void setMembershipDiscountPercent(BigDecimal value) { this.membershipDiscountPercent = value == null ? BigDecimal.ZERO : value; }
    public BigDecimal getMembershipDiscountAmount() { return membershipDiscountAmount == null ? BigDecimal.ZERO : membershipDiscountAmount; }
    public void setMembershipDiscountAmount(BigDecimal value) { this.membershipDiscountAmount = value == null ? BigDecimal.ZERO : value; }
    public BigDecimal getVoucherDiscountAmount() { return voucherDiscountAmount == null ? BigDecimal.ZERO : voucherDiscountAmount; }
    public void setVoucherDiscountAmount(BigDecimal value) { this.voucherDiscountAmount = value == null ? BigDecimal.ZERO : value; }
    public BigDecimal getPointMultiplier() { return pointMultiplier == null ? BigDecimal.ONE : pointMultiplier; }
    public void setPointMultiplier(BigDecimal value) { this.pointMultiplier = value == null ? BigDecimal.ONE : value; }
    public String getQrToken() { return qrToken; }
    public void setQrToken(String qrToken) { this.qrToken = qrToken; }
    public LocalDateTime getCheckedInAt() { return checkedInAt; }
    public void setCheckedInAt(LocalDateTime checkedInAt) { this.checkedInAt = checkedInAt; }
    public String getCheckedInBy() { return checkedInBy; }
    public void setCheckedInBy(String checkedInBy) { this.checkedInBy = checkedInBy; }
    public boolean isCheckedIn() { return checkedInAt != null; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<BookingSeat> getSeats() { return seats; }
    public void setSeats(List<BookingSeat> seats) { this.seats = seats; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public void setRefundedAt(LocalDateTime refundedAt) { this.refundedAt = refundedAt; }
    public String getRefundedBy() { return refundedBy; }
    public void setRefundedBy(String refundedBy) { this.refundedBy = refundedBy; }
    public String getRefundNote() { return refundNote; }
    public void setRefundNote(String refundNote) { this.refundNote = refundNote; }
    public String getRefundReference() { return refundReference; }
    public void setRefundReference(String refundReference) { this.refundReference = refundReference; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
    public String getRefundEvidence() { return refundEvidence; }
    public void setRefundEvidence(String refundEvidence) { this.refundEvidence = refundEvidence; }
    public Long getRefundedByUserId() { return refundedByUserId; }
    public void setRefundedByUserId(Long refundedByUserId) { this.refundedByUserId = refundedByUserId; }
    public List<BookingCombo> getCombos() { return combos; }
    public void setCombos(List<BookingCombo> combos) { this.combos = combos; }
}
