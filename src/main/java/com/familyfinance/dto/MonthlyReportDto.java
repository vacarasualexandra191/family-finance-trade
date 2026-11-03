package com.familyfinance.dto;

import com.familyfinance.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MonthlyReportDto {

    private int month;
    private int year;

    // Totaluri principale
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal savings;          // venituri - cheltuieli

    // Cheltuieli pe subcategorii
    private BigDecimal chirie;
    private BigDecimal utilitati;
    private BigDecimal alimente;
    private BigDecimal abonamente;
    private BigDecimal medical;
    private BigDecimal asigurare;
    private BigDecimal concediu;
    private BigDecimal evenimentSpecial;

    // Tinta de economisire
    private BigDecimal savingsTarget;        // suma tinta
    private Integer savingsPercentage;       // procentul setat
    private BigDecimal savingsDifference;    // savings - savingsTarget (pozitiv = bine)

    // Lista tranzactii ale lunii
    private List<Transaction> transactions;

    // Rata de economisire efectiva (%)
    public BigDecimal getSavingsRate() {
        if (totalIncome == null || totalIncome.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return savings.divide(totalIncome, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);
    }

    // Procentul cheltuielilor curente din total venituri
    public BigDecimal getCurrentExpensesRatio() {
        BigDecimal current = sum(chirie, utilitati, alimente, abonamente);
        if (totalIncome == null || totalIncome.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return current.divide(totalIncome, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal sum(BigDecimal... values) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal v : values) if (v != null) total = total.add(v);
        return total;
    }
}
