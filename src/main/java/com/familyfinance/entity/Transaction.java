package com.familyfinance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;


@Entity
@Table(name = "ff_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {

    public enum Type { INCOME, EXPENSE }

    public enum SubCategory {

        CHIRIE, UTILITATI, ALIMENTE, ABONAMENTE,
        MEDICAL, ASIGURARE, CONCEDIU, EVENIMENT_SPECIAL,
        SALARIU, FREELANCE, DIVIDENDE, ALTE_VENITURI,
        ALOCARE_COPIL, SCHIMB_VALUTAR_OUT,
        SCHIMB_VALUTAR_IN, ALOCARE_ECONOMII }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubCategory subCategory;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String description;

    @Column(length = 3)
    @Builder.Default
    private String currency = "RON";

    @Column(name = "user_id")
    private Long userId;
}