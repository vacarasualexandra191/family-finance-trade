package com.familyfinance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "ff_savings_allocations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SavingsAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Column(name = "allocation_date", nullable = false)
    private LocalDate allocationDate;

    @Column(name = "user_id", nullable = false)
    private Long userId;
}