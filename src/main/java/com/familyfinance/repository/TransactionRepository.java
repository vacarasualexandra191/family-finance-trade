package com.familyfinance.repository;

import com.familyfinance.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND EXTRACT(MONTH FROM t.transactionDate) = :month AND EXTRACT(YEAR FROM t.transactionDate) = :year ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserIdAndMonthAndYear(@Param("userId") Long userId, @Param("month") int month, @Param("year") int year);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.userId = :userId AND t.type = :type AND EXTRACT(MONTH FROM t.transactionDate) = :month AND EXTRACT(YEAR FROM t.transactionDate) = :year")
    BigDecimal sumByUserIdAndTypeAndMonth(@Param("userId") Long userId, @Param("type") Transaction.Type type, @Param("month") int month, @Param("year") int year);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.userId = :userId AND t.subCategory = :subCategory AND EXTRACT(MONTH FROM t.transactionDate) = :month AND EXTRACT(YEAR FROM t.transactionDate) = :year")
    BigDecimal sumByUserIdAndSubCategoryAndMonth(@Param("userId") Long userId, @Param("subCategory") Transaction.SubCategory subCategory, @Param("month") int month, @Param("year") int year);

    Page<Transaction> findByUserIdOrderByTransactionDateDesc(Long userId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.userId = :userId AND t.type = :type")
    BigDecimal sumByUserIdAndType(@Param("userId") Long userId, @Param("type") Transaction.Type type);


    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.userId IN :userIds AND t.type = :type " +
            "AND EXTRACT(MONTH FROM t.transactionDate) = :month " +
            "AND EXTRACT(YEAR FROM t.transactionDate) = :year")
    BigDecimal sumByUserIdsAndTypeAndMonth(@Param("userIds") List<Long> userIds,
                                           @Param("type") Transaction.Type type,
                                           @Param("month") int month,
                                           @Param("year") int year);
}