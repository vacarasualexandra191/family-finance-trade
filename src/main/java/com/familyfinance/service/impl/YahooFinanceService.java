package com.familyfinance.service.impl;

import com.familyfinance.entity.InvestmentFund;
import com.familyfinance.repository.InvestmentFundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
@Slf4j
public class YahooFinanceService {

    private final InvestmentFundRepository fundRepository;
    private final WebClient webClient;

    /**
     * URL: https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?interval=1d&range=1d
     */
    public BigDecimal fetchCurrentPrice(String symbol) {
        try {
            String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + symbol + "?interval=1d&range=1d";

            Map response = webClient.get()
                    .uri(url)
                    .header("User-Agent", "Mozilla/5.0")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null) {
                Map chart = (Map) response.get("chart");
                List result = (List) chart.get("result");
                if (result != null && !result.isEmpty()) {
                    Map firstResult = (Map) result.get(0);
                    Map meta = (Map) firstResult.get("meta");
                    Object price = meta.get("regularMarketPrice");
                    if (price != null) {
                        return new BigDecimal(price.toString());
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Nu s-a putut prelua pretul pentru {}: {}", symbol, ex.getMessage());
        }
        return null;
    }

    /**
     * Actualizeaza preturile tuturor fondurilor din baza de date.
     * Ruleaza automat la fiecare ora (cron = "0 0 * * * *").
     */
    @Scheduled(cron = "0 0 * * * *")
    public void updateAllPrices() {
        List<InvestmentFund> funds = fundRepository.findAll();
        log.info("Actualizare preturi pentru {} fonduri...", funds.size());

        for (InvestmentFund fund : funds) {
            BigDecimal price = fetchCurrentPrice(fund.getSymbol());
            if (price != null) {
                fund.setCurrentPrice(price);
                fund.setLastPriceUpdate(LocalDateTime.now());
                fundRepository.save(fund);
                log.info("Pret actualizat: {} = {} {}", fund.getSymbol(), price, fund.getCurrency());
            }
        }
    }

    /**
     * Actualizeaza pretul pentru un singur fond (apelat manual la cerere).
     */
    public void updatePrice(InvestmentFund fund) {
        BigDecimal price = fetchCurrentPrice(fund.getSymbol());
        if (price != null) {
            fund.setCurrentPrice(price);
            fund.setLastPriceUpdate(LocalDateTime.now());
            fundRepository.save(fund);
        }
    }
}
