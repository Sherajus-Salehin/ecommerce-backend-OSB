package com.example.ecommerce.backend.order.entity;

import com.example.ecommerce.backend.common.entity.Auditable;
import com.example.ecommerce.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name="coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Coupon extends BaseEntity implements Auditable {
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;
    @Column(name = "discount", nullable = false)
    private Double discount;
    @Column(name="max_discount")
    private Double cap;//limit
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "modified_at", nullable = false)
    private LocalDateTime updatedAt;
    @Column(name = "created_by")
    private Long createdBy;
    @Column(name = "modified_by")
    private Long modifiedBy;
    @Column(name = "is_active", nullable = false)
    private Boolean isActive=true;

    @Override
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public LocalDateTime getModifiedAt() {
        return updatedAt;
    }

    @Override
    public void setModifiedAt(LocalDateTime modifiedAt) {
        this.updatedAt = modifiedAt;
    }

    @Override
    public Long getCreatedBy() {
        return createdBy;
    }

    @Override
    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public Long getModifiedBy() {
        return modifiedBy;
    }

    @Override
    public void setModifiedBy(Long modifiedBy) {
        this.modifiedBy = modifiedBy;
    }
}
