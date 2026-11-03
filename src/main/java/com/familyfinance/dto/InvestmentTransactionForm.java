package com.familyfinance.dto;

import com.familyfinance.entity.InvestmentTransaction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class InvestmentTransactionForm {

    @NotNull(message = "Selecteaza un fond")
    private Long fundId;

    @NotNull(message = "Selecteaza tipul operatiei")
    private InvestmentTransaction.OperationType operationType = InvestmentTransaction.OperationType.BUY;

    @NotNull(message = "Numarul de unitati este obligatoriu")
    @Positive(message = "Numarul de unitati trebuie sa fie pozitiv")
    private BigDecimal units;

    private BigDecimal pricePerUnit;

    @NotNull(message = "Data este obligatorie")
    private LocalDate transactionDate = LocalDate.now();

    private String currency = "USD";
}