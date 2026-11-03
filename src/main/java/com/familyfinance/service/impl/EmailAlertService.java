package com.familyfinance.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Month;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailAlertService {

    private final EmailService emailService;

    @Value("${app.alert.email-to}")
    private String emailTo;

    @Value("${app.alert.expense-threshold-pct:80}")
    private int thresholdPct;

    public boolean checkAndSendAlert(BigDecimal totalIncome, BigDecimal totalExpense,
                                     int month, int year) {
        if (totalIncome.compareTo(BigDecimal.ZERO) == 0) return false;

        BigDecimal expensePct = totalExpense
                .multiply(BigDecimal.valueOf(100))
                .divide(totalIncome, 2, RoundingMode.HALF_UP);

        if (expensePct.compareTo(BigDecimal.valueOf(thresholdPct)) >= 0) {
            sendBudgetAlert(totalIncome, totalExpense, expensePct, month, year);
            return true;
        }
        return false;
    }

    public void sendBudgetAlert(BigDecimal totalIncome, BigDecimal totalExpense,
                                BigDecimal expensePct, int month, int year) {
        String subject = String.format("⚠️ Alerta Buget — %s %d",
                Month.of(month).name(), year);
        String body = String.format(
                "Cheltuielile lunii %s %d au depasit %s%% din venituri.\n\n" +
                        "Venituri: %s RON\nCheltuieli: %s RON\nEconomii: %s RON",
                Month.of(month).name(), year, expensePct,
                totalIncome.setScale(2, RoundingMode.HALF_UP),
                totalExpense.setScale(2, RoundingMode.HALF_UP),
                totalIncome.subtract(totalExpense).setScale(2, RoundingMode.HALF_UP));

        emailService.sendAlertEmail(emailTo, subject, body);
        log.info("Email alerta trimis catre {}", emailTo);
    }

    public void sendTestEmail() {
        emailService.sendAlertEmail(emailTo,
                "✅ Test email — Family Finance",
                "Configurarea email functioneaza corect!");
    }
}