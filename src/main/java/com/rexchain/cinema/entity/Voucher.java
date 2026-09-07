package com.rexchain.cinema.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="vouchers", uniqueConstraints=@UniqueConstraint(columnNames="code"))
public class Voucher {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Version private Long version;
    @Column(nullable=false, length=40) private String code;
    @Column(nullable=false, length=160) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private DiscountType discountType=DiscountType.PERCENT;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal discountValue=BigDecimal.ZERO;
    @Column(precision=12, scale=2) private BigDecimal minOrderAmount=BigDecimal.ZERO;
    @Column(precision=12, scale=2) private BigDecimal maxDiscountAmount;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Integer usageLimit;
    @Column(nullable=false) private int usedCount=0;
    @Column(nullable=false) private boolean active=true;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getVersion(){return version;} public void setVersion(Long v){version=v;}
    public String getCode(){return code;} public void setCode(String v){code=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public DiscountType getDiscountType(){return discountType;} public void setDiscountType(DiscountType v){discountType=v;}
    public BigDecimal getDiscountValue(){return discountValue;} public void setDiscountValue(BigDecimal v){discountValue=v;}
    public BigDecimal getMinOrderAmount(){return minOrderAmount;} public void setMinOrderAmount(BigDecimal v){minOrderAmount=v;}
    public BigDecimal getMaxDiscountAmount(){return maxDiscountAmount;} public void setMaxDiscountAmount(BigDecimal v){maxDiscountAmount=v;}
    public LocalDateTime getValidFrom(){return validFrom;} public void setValidFrom(LocalDateTime v){validFrom=v;}
    public LocalDateTime getValidUntil(){return validUntil;} public void setValidUntil(LocalDateTime v){validUntil=v;}
    public Integer getUsageLimit(){return usageLimit;} public void setUsageLimit(Integer v){usageLimit=v;}
    public int getUsedCount(){return usedCount;} public void setUsedCount(int v){usedCount=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
}
