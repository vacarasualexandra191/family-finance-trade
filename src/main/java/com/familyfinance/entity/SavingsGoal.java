package com.familyfinance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

/**
 * Tinta de economisire — suma recomandata sau setata de utilizator per luna.
 * Calculul efectiv al economiilor se face din raportul lunar (venituri - cheltuieli).
 */
@Entity
@Table(name = "ff_savings_goals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SavingsGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private Integer year;

    @NotNull
    @Min(1) @Max(12)
    @Column(nullable = false)
    private Integer month;

    @NotNull
    @Positive
    @Column(name = "monthly_income", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyIncome;

    /**
     * Procentul recomandat din venit de economisit (ex: 20 = 20%).
     * Suma recomandata = monthlyIncome * savingsPercentage / 100
     */
    @NotNull
    @Min(1) @Max(100)
    @Column(name = "savings_percentage", nullable = false)
    private Integer savingsPercentage;

    @Column(name = "notes", length = 255)
    private String notes;
}
