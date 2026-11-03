package com.familyfinance.service.impl;

import com.familyfinance.dto.MonthlyReportDto;
import com.familyfinance.entity.Transaction;
import com.familyfinance.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final TransactionRepository transactionRepository;

    public MonthlyReportDto generateMonthlyReport(int month, int year, Long userId) {
        log.info("Generare raport lunar: {}/{} pentru userId={}", month, year, userId);

        BigDecimal totalIncome = transactionRepository.sumByUserIdAndTypeAndMonth(
                userId, Transaction.Type.INCOME, month, year);
        BigDecimal totalExpense = transactionRepository.sumByUserIdAndTypeAndMonth(
                userId, Transaction.Type.EXPENSE, month, year);
        BigDecimal savings = totalIncome.subtract(totalExpense);

        List<Transaction> transactions = transactionRepository
                .findByUserIdAndMonthAndYear(userId, month, year);

        return MonthlyReportDto.builder()
                .month(month)
                .year(year)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .savings(savings)
                .transactions(transactions)
                .chirie(transactionRepository.sumByUserIdAndSubCategoryAndMonth(
                        userId, Transaction.SubCategory.CHIRIE, month, year))
                .utilitati(transactionRepository.sumByUserIdAndSubCategoryAndMonth(
                        userId, Transaction.SubCategory.UTILITATI, month, year))
                .alimente(transactionRepository.sumByUserIdAndSubCategoryAndMonth(
                        userId, Transaction.SubCategory.ALIMENTE, month, year))
                .abonamente(transactionRepository.sumByUserIdAndSubCategoryAndMonth(
                        userId, Transaction.SubCategory.ABONAMENTE, month, year))
                .medical(transactionRepository.sumByUserIdAndSubCategoryAndMonth(
                        userId, Transaction.SubCategory.MEDICAL, month, year))
                .asigurare(transactionRepository.sumByUserIdAndSubCategoryAndMonth(
                        userId, Transaction.SubCategory.ASIGURARE, month, year))
                .concediu(transactionRepository.sumByUserIdAndSubCategoryAndMonth(
                        userId, Transaction.SubCategory.CONCEDIU, month, year))
                .evenimentSpecial(transactionRepository.sumByUserIdAndSubCategoryAndMonth(
                        userId, Transaction.SubCategory.EVENIMENT_SPECIAL, month, year))
                .build();
    }


    public BigDecimal getTotalSavings(Long userId) {
        BigDecimal totalIncome = transactionRepository.sumByUserIdAndType(
                userId, Transaction.Type.INCOME);
        BigDecimal totalExpense = transactionRepository.sumByUserIdAndType(
                userId, Transaction.Type.EXPENSE);
        BigDecimal income = totalIncome != null ? totalIncome : BigDecimal.ZERO;
        BigDecimal expense = totalExpense != null ? totalExpense : BigDecimal.ZERO;
        return income.subtract(expense);
    }
}