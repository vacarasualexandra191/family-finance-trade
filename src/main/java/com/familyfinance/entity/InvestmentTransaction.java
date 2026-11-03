package com.familyfinance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;


@Entity
@Table(name = "ff_investment_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvestmentTransaction {

    public enum OperationType { BUY, SELL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fund_id", nullable = false)
    private InvestmentFund fund;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OperationType operationType;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal units;

    @NotNull
    @Positive
    @Column(name = "price_per_unit", nullable = false, precision = 12, scale = 4)
    private BigDecimal pricePerUnit;

    @NotNull
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;


    @Column(name = "total_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalValue;

    @Column(length = 5)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "user_id")
    private Long userId;
}
