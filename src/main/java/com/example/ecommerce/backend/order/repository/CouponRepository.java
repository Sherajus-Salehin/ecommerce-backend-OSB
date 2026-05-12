package com.example.ecommerce.backend.order.repository;

import com.example.ecommerce.backend.order.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon,Long> {
    Optional<Coupon> findByCode(String code);
}
