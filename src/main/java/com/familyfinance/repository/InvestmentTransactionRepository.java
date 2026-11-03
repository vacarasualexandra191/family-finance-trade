package com.familyfinance.repository;

import com.familyfinance.entity.InvestmentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface InvestmentTransactionRepository extends JpaRepository<InvestmentTransaction, Long> {

    List<InvestmentTransaction> findByFundIdOrderByTransactionDateDesc(Long fundId);

    List<InvestmentTransaction> findAllByOrderByTransactionDateDesc();

    @Query("SELECT COALESCE(SUM(CASE WHEN it.operationType = 'BUY' THEN it.units ELSE -it.units END), 0) FROM InvestmentTransaction it WHERE it.fund.id = :fundId")
    BigDecimal getNetUnitsForFund(@Param("fundId") Long fundId);

    @Query("SELECT COALESCE(SUM(CASE WHEN it.operationType = 'BUY' THEN it.totalValue ELSE -it.totalValue END), 0) FROM InvestmentTransaction it WHERE it.fund.id = :fundId")
    BigDecimal getNetInvestedValueForFund(@Param("fundId") Long fundId);

    @Transactional
    void deleteByFundId(Long fundId);

    List<InvestmentTransaction> findByUserIdOrderByTransactionDateDesc(Long userId);

    @Query("SELECT COALESCE(SUM(CASE WHEN it.operationType = 'BUY' THEN it.units ELSE -it.units END), 0) FROM InvestmentTransaction it WHERE it.fund.id = :fundId AND it.userId = :userId")
    BigDecimal getNetUnitsForFundAndUser(@Param("fundId") Long fundId, @Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(CASE WHEN it.operationType = 'BUY' THEN it.totalValue ELSE -it.totalValue END), 0) FROM InvestmentTransaction it WHERE it.fund.id = :fundId AND it.userId = :userId")
    BigDecimal getNetInvestedValueForFundAndUser(@Param("fundId") Long fundId, @Param("userId") Long userId);
}
