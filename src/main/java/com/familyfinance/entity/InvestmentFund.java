package com.familyfinance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "ff_investment_funds")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvestmentFund {

    public enum FundType { ETF, ACTIUNE_BVB }

    public enum RiskLevel { SCAZUT, MEDIU, RIDICAT }

    public enum TimeHorizon { SCURT_1_3_ANI, MEDIU_3_7_ANI, LUNG_7_ANI_PLUS }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 20)
    private String symbol;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "fund_type", nullable = false, length = 20)
    private FundType fundType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "time_horizon", nullable = false, length = 20)
    private TimeHorizon timeHorizon;

    @Column(name = "current_price", precision = 12, scale = 4)
    private BigDecimal currentPrice;

    @Column(length = 5)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "last_price_update")
    private LocalDateTime lastPriceUpdate;

    @Column(length = 500)
    private String description;


    @Column(name = "annual_return_pct", precision = 6, scale = 2)
    private BigDecimal annualReturnPct;
}
