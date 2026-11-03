package com.familyfinance.dto;

import com.familyfinance.entity.InvestmentFund;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortfolioItemDto {

    private InvestmentFund fund;
    private BigDecimal netUnits;            // unitati detinute (buy - sell)
    private BigDecimal netInvestedValue;    // suma investita net
    private BigDecimal currentValue;        // netUnits * currentPrice
    private BigDecimal profitLoss;          // currentValue - netInvestedValue

    public BigDecimal getProfitLossPct() {
        if (netInvestedValue == null || netInvestedValue.compareTo(BigDecimal.ZERO) == 0)
            return BigDecimal.ZERO;
        return profitLoss.divide(netInvestedValue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
