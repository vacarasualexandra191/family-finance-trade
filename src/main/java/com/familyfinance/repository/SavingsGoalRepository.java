package com.familyfinance.repository;

import com.familyfinance.entity.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {
    Optional<SavingsGoal> findByYearAndMonth(int year, int month);
    List<SavingsGoal> findByYearOrderByMonthAsc(int year);
}
