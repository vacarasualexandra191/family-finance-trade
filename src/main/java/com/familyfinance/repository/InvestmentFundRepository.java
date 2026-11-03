package com.familyfinance.repository;

import com.familyfinance.entity.InvestmentFund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvestmentFundRepository extends JpaRepository<InvestmentFund, Long> {
    Optional<InvestmentFund> findBySymbol(String symbol);
    List<InvestmentFund> findByFundType(InvestmentFund.FundType fundType);
    List<InvestmentFund> findByRiskLevelAndTimeHorizon(InvestmentFund.RiskLevel riskLevel, InvestmentFund.TimeHorizon timeHorizon);
    List<InvestmentFund> findByRiskLevel(InvestmentFund.RiskLevel riskLevel);
    List<InvestmentFund> findByTimeHorizon(InvestmentFund.TimeHorizon timeHorizon);
}