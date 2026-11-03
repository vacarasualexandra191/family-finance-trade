package com.familyfinance.repository;

import com.familyfinance.entity.SavingsAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface SavingsAllocationRepository extends JpaRepository<SavingsAllocation, Long> {

    @Query("SELECT COALESCE(SUM(s.amount), 0) FROM SavingsAllocation s " +
            "WHERE s.userId = :userId " +
            "AND EXTRACT(MONTH FROM s.allocationDate) = :month " +
            "AND EXTRACT(YEAR FROM s.allocationDate) = :year")
    BigDecimal sumByUserIdAndMonth(@Param("userId") Long userId,
                                   @Param("month") int month,
                                   @Param("year") int year);
}