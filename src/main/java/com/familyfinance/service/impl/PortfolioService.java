package com.familyfinance.service.impl;

import com.familyfinance.dto.InvestmentTransactionForm;
import com.familyfinance.dto.PortfolioItemDto;
import com.familyfinance.entity.InvestmentFund;
import com.familyfinance.entity.InvestmentTransaction;
import com.familyfinance.entity.User;
import com.familyfinance.repository.InvestmentFundRepository;
import com.familyfinance.repository.InvestmentTransactionRepository;
import com.familyfinance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioService {

    private final InvestmentFundRepository fundRepository;
    private final InvestmentTransactionRepository investmentTransactionRepository;
    private final YahooFinanceService yahooFinanceService;
    private final UserRepository userRepository;

    private static final BigDecimal USD_TO_RON = BigDecimal.valueOf(4.8);
    private static final BigDecimal EUR_TO_RON = BigDecimal.valueOf(5.0);

    public List<PortfolioItemDto> getPortfolio(Long userId) {
        List<PortfolioItemDto> portfolio = new ArrayList<>();
        List<InvestmentFund> funds = fundRepository.findAll();

        for (InvestmentFund fund : funds) {
            BigDecimal netUnits = investmentTransactionRepository
                    .getNetUnitsForFundAndUser(fund.getId(), userId);
            if (netUnits == null || netUnits.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal netInvested = investmentTransactionRepository
                    .getNetInvestedValueForFundAndUser(fund.getId(), userId);
            BigDecimal currentPrice = fund.getCurrentPrice() != null ?
                    fund.getCurrentPrice() : BigDecimal.ZERO;
            BigDecimal currentValue = netUnits.multiply(currentPrice)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal profitLoss = currentValue.subtract(
                    netInvested != null ? netInvested : BigDecimal.ZERO);

            portfolio.add(PortfolioItemDto.builder()
                    .fund(fund)
                    .netUnits(netUnits)
                    .netInvestedValue(netInvested != null ? netInvested : BigDecimal.ZERO)
                    .currentValue(currentValue)
                    .profitLoss(profitLoss)
                    .build());
        }
        return portfolio;
    }

    @Transactional
    public InvestmentTransaction executeTransaction(InvestmentTransactionForm form, Long userId) {
        log.info("executeTransaction: fundId={}, op={}, units={}, price={}",
                form.getFundId(), form.getOperationType(),
                form.getUnits(), form.getPricePerUnit());

        InvestmentFund fund = fundRepository.findById(form.getFundId())
                .orElseThrow(() -> new RuntimeException("Fond inexistent: " + form.getFundId()));

        // Verificare unitati disponibile la SELL
        if (form.getOperationType() == InvestmentTransaction.OperationType.SELL) {
            BigDecimal unitatiDetinute = investmentTransactionRepository
                    .getNetUnitsForFundAndUser(fund.getId(), userId);

            if (unitatiDetinute == null || unitatiDetinute.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException(
                        "Nu detii unitati din fondul " + fund.getSymbol() + "!");
            }

            if (form.getUnits().compareTo(unitatiDetinute) > 0) {
                throw new RuntimeException(
                        "Nu poti vinde " + form.getUnits() + " unitati! " +
                                "Detii doar " + unitatiDetinute.setScale(4, RoundingMode.HALF_UP) +
                                " unitati din " + fund.getSymbol() + ".");
            }
        }

        // Preia pretul curent daca nu e completat
        BigDecimal price = form.getPricePerUnit();
        if (price == null || price.compareTo(BigDecimal.ZERO) == 0) {
            yahooFinanceService.updatePrice(fund);
            price = fund.getCurrentPrice() != null ? fund.getCurrentPrice() : BigDecimal.ONE;
        }

        BigDecimal totalValue = form.getUnits().multiply(price)
                .setScale(2, RoundingMode.HALF_UP);

        String currency = form.getCurrency() != null ? form.getCurrency() : "USD";


        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilizator inexistent: " + userId));

        if ("USD".equals(currency)) {
            BigDecimal balanceUsd = user.getBalanceUsd() != null ? user.getBalanceUsd() : BigDecimal.ZERO;
            if (form.getOperationType() == InvestmentTransaction.OperationType.BUY) {
                if (totalValue.compareTo(balanceUsd) > 0) {
                    throw new RuntimeException(
                            "Sold USD insuficient! Necesar: " + totalValue + " USD, "
                                    + "disponibil: " + balanceUsd + " USD. "
                                    + "Foloseste mai intai Schimb Valutar pentru a converti economii in USD.");
                }
                user.setBalanceUsd(balanceUsd.subtract(totalValue));
            } else {
                user.setBalanceUsd(balanceUsd.add(totalValue));
            }
        } else {
            // RON (sau alta moneda locala) - se scade direct din economii.
            BigDecimal savingsBalance = user.getSavingsBalance() != null ? user.getSavingsBalance() : BigDecimal.ZERO;
            if (form.getOperationType() == InvestmentTransaction.OperationType.BUY) {
                if (totalValue.compareTo(savingsBalance) > 0) {
                    throw new RuntimeException(
                            "Economii insuficiente! Necesar: " + totalValue + " " + currency + ", "
                                    + "disponibil: " + savingsBalance + " " + currency + ".");
                }
                user.setSavingsBalance(savingsBalance.subtract(totalValue));
            } else {
                user.setSavingsBalance(savingsBalance.add(totalValue));
            }
        }
        userRepository.save(user);

        // Salveaza tranzactia de investitie - singura inregistrare persistata
        // pentru aceasta operatiune.
        InvestmentTransaction tx = InvestmentTransaction.builder()
                .fund(fund)
                .operationType(form.getOperationType())
                .units(form.getUnits())
                .pricePerUnit(price)
                .totalValue(totalValue)
                .transactionDate(LocalDate.now())
                .currency(currency)
                .userId(userId)
                .build();

        InvestmentTransaction saved = investmentTransactionRepository.save(tx);

        log.info("Tranzactie investitie salvata: id={}, {} {} unitati {} la {} {}",
                saved.getId(), form.getOperationType(), form.getUnits(),
                fund.getSymbol(), price, currency);
        return saved;
    }


    public List<InvestmentFund> suggestFunds(InvestmentFund.RiskLevel risk,
                                             InvestmentFund.TimeHorizon horizon) {
        if (risk != null && horizon != null) {
            return fundRepository.findByRiskLevelAndTimeHorizon(risk, horizon);
        } else if (risk != null) {
            return fundRepository.findByRiskLevel(risk);
        } else if (horizon != null) {
            return fundRepository.findByTimeHorizon(horizon);
        }
        return fundRepository.findAll();
    }

    public BigDecimal getTotalPortfolioValue(Long userId) {
        return getPortfolio(userId).stream()
                .map(PortfolioItemDto::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Valoarea totala a portofoliului convertita in RON
     */
    public BigDecimal getTotalPortfolioValueRon(Long userId) {
        List<PortfolioItemDto> portfolio = getPortfolio(userId);
        BigDecimal totalRon = BigDecimal.ZERO;
        for (PortfolioItemDto item : portfolio) {
            String currency = item.getFund().getCurrency();
            BigDecimal valueRon;
            if ("USD".equals(currency)) {
                valueRon = item.getCurrentValue().multiply(USD_TO_RON);
            } else if ("EUR".equals(currency)) {
                valueRon = item.getCurrentValue().multiply(EUR_TO_RON);
            } else {
                valueRon = item.getCurrentValue();
            }
            totalRon = totalRon.add(valueRon);
        }
        return totalRon.setScale(2, RoundingMode.HALF_UP);
    }
}