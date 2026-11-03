package com.familyfinance.controller;

import com.familyfinance.dto.InvestmentTransactionForm;
import com.familyfinance.entity.InvestmentFund;
import com.familyfinance.entity.InvestmentTransaction;
import com.familyfinance.entity.User;
import com.familyfinance.repository.InvestmentFundRepository;
import com.familyfinance.repository.InvestmentTransactionRepository;
import com.familyfinance.repository.UserRepository;
import com.familyfinance.service.impl.PortfolioService;
import com.familyfinance.service.impl.ReportService;
import com.familyfinance.service.impl.YahooFinanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Controller
@RequestMapping("/investments")
@RequiredArgsConstructor
@Slf4j
public class InvestmentController {

    private final InvestmentFundRepository fundRepository;
    private final InvestmentTransactionRepository txRepository;
    private final PortfolioService portfolioService;
    private final YahooFinanceService yahooFinanceService;
    private final UserRepository userRepository;
    private final ReportService reportService;

    private static final BigDecimal USD_TO_RON = BigDecimal.valueOf(4.8);
    private static final BigDecimal EUR_TO_RON = BigDecimal.valueOf(5.0);

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow();
    }

    @GetMapping
    public String portfolio(Model model) {
        User currentUser = getCurrentUser();
        if ("COPIL".equals(currentUser.getFamilyRole())) {
            return "redirect:/dashboard";
        }
        Long userId = currentUser.getId();

        model.addAttribute("portfolio", portfolioService.getPortfolio(userId));
        model.addAttribute("totalValue", portfolioService.getTotalPortfolioValue(userId));
        model.addAttribute("recentTx", txRepository.findByUserIdOrderByTransactionDateDesc(userId));
        return "investments/portfolio";
    }

    @GetMapping("/funds")
    public String funds(Model model) {
        model.addAttribute("funds", fundRepository.findAll());
        return "investments/funds";
    }

    @GetMapping("/funds/new")
    public String newFundForm(Model model) {
        model.addAttribute("fund", new InvestmentFund());
        model.addAttribute("fundTypes", InvestmentFund.FundType.values());
        model.addAttribute("riskLevels", InvestmentFund.RiskLevel.values());
        model.addAttribute("timeHorizons", InvestmentFund.TimeHorizon.values());
        return "investments/fund-form";
    }

    @PostMapping("/funds/new")
    public String saveFund(@ModelAttribute InvestmentFund fund, RedirectAttributes ra) {
        fundRepository.save(fund);
        yahooFinanceService.updatePrice(fund);
        ra.addFlashAttribute("successMessage", "Fond adaugat! Pret preluat de pe Yahoo Finance.");
        return "redirect:/investments/funds";
    }

    @GetMapping("/funds/{id}/refresh")
    public String refreshPrice(@PathVariable Long id, RedirectAttributes ra) {
        fundRepository.findById(id).ifPresent(fund -> {
            yahooFinanceService.updatePrice(fund);
            ra.addFlashAttribute("successMessage",
                    "Pret actualizat: " + fund.getSymbol() + " = " + fund.getCurrentPrice());
        });
        return "redirect:/investments/funds";
    }

    @GetMapping("/funds/{id}/edit")
    public String editFundForm(@PathVariable Long id, Model model) {
        InvestmentFund fund = fundRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fond inexistent"));
        model.addAttribute("fund", fund);
        model.addAttribute("fundTypes", InvestmentFund.FundType.values());
        model.addAttribute("riskLevels", InvestmentFund.RiskLevel.values());
        model.addAttribute("timeHorizons", InvestmentFund.TimeHorizon.values());
        return "investments/fund-edit";
    }

    @PostMapping("/funds/{id}/edit")
    public String editFund(@PathVariable Long id,
                           @ModelAttribute InvestmentFund fund,
                           RedirectAttributes ra) {
        fund.setId(id);
        fundRepository.save(fund);
        yahooFinanceService.updatePrice(fund);
        ra.addFlashAttribute("successMessage", "Fond actualizat!");
        return "redirect:/investments/funds";
    }

    @Transactional
    @PostMapping("/funds/{id}/delete")
    public String deleteFund(@PathVariable Long id, RedirectAttributes ra) {
        try {
            txRepository.deleteByFundId(id);
            fundRepository.deleteById(id);
            ra.addFlashAttribute("successMessage", "Fond sters cu succes!");
        } catch (Exception ex) {
            log.error("Eroare stergere fond: {}", ex.getMessage());
            ra.addFlashAttribute("errorMessage", "Nu se poate sterge fondul: " + ex.getMessage());
        }
        return "redirect:/investments/funds";
    }

    @GetMapping("/trade")
    public String tradeForm(@RequestParam(required = false) Long fundId, Model model) {
        User currentUser = getCurrentUser();
        LocalDate now = LocalDate.now();
        var report = reportService.generateMonthlyReport(
                now.getMonthValue(), now.getYear(), currentUser.getId());

        var portfolio = portfolioService.getPortfolio(currentUser.getId());

        // Actualizeaza pretul live (Yahoo Finance) pentru fiecare fond inainte de afisare.
        var funds = fundRepository.findAll();
        for (InvestmentFund fund : funds) {
            yahooFinanceService.updatePrice(fund);
        }

        // Daca s-a venit din pagina de Recomandari cu un fond specific (fundId),
        // precompletam formularul, ca acel fond sa apara deja selectat in
        // dropdown, fara ca utilizatorul sa trebuiasca sa-l caute din nou.
        InvestmentTransactionForm form = new InvestmentTransactionForm();
        if (fundId != null) {
            form.setFundId(fundId);
        }

        model.addAttribute("form", form);
        model.addAttribute("funds", funds.stream()
                .filter(f -> f.getCurrentPrice() != null &&
                        f.getCurrentPrice().compareTo(BigDecimal.ZERO) > 0)
                .collect(java.util.stream.Collectors.toList()));
        model.addAttribute("portfolio", portfolio);
        model.addAttribute("operationTypes", InvestmentTransaction.OperationType.values());
        model.addAttribute("monthlySavings", report.getSavings());
        model.addAttribute("usdToRon", 4.8);
        model.addAttribute("eurToRon", 5.0);
        return "investments/trade";
    }

    @PostMapping("/trade")
    public String executeTrade(@Valid @ModelAttribute("form") InvestmentTransactionForm form,
                               BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            log.warn("Erori validare: {}", result.getAllErrors());
            model.addAttribute("funds", fundRepository.findAll().stream()
                    .filter(f -> f.getCurrentPrice() != null &&
                            f.getCurrentPrice().compareTo(BigDecimal.ZERO) > 0)
                    .collect(java.util.stream.Collectors.toList()));
            model.addAttribute("operationTypes", InvestmentTransaction.OperationType.values());
            LocalDate now = LocalDate.now();
            User currentUser = getCurrentUser();
            var report = reportService.generateMonthlyReport(
                    now.getMonthValue(), now.getYear(), currentUser.getId());
            model.addAttribute("monthlySavings", report.getSavings());
            model.addAttribute("usdToRon", 4.8);
            model.addAttribute("eurToRon", 5.0);
            return "investments/trade";
        }

        User currentUser = getCurrentUser();

        InvestmentFund fund = fundRepository.findById(form.getFundId())
                .orElse(null);
        if (fund == null) {
            ra.addFlashAttribute("errorMessage", "Fond invalid.");
            return "redirect:/investments/trade";
        }

        yahooFinanceService.updatePrice(fund);
        BigDecimal currentPrice = fund.getCurrentPrice();

        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            ra.addFlashAttribute("errorMessage", "Pretul fondului nu este disponibil momentan. Incearca din nou.");
            return "redirect:/investments/trade";
        }

        if (form.getUnits() == null || form.getUnits().compareTo(BigDecimal.ZERO) <= 0) {
            ra.addFlashAttribute("errorMessage", "Numarul de unitati trebuie sa fie pozitiv.");
            return "redirect:/investments/trade";
        }

        BigDecimal total = form.getUnits().multiply(currentPrice);
        String currency = fund.getCurrency();
        boolean isBuy = InvestmentTransaction.OperationType.BUY.equals(form.getOperationType());

        if (isBuy) {
            LocalDate now = LocalDate.now();
            var report = reportService.generateMonthlyReport(
                    now.getMonthValue(), now.getYear(), currentUser.getId());
            BigDecimal monthlySavings = report.getSavings() != null ?
                    report.getSavings() : BigDecimal.ZERO;
            BigDecimal totalRon = convertToRon(total, currency);

            if (totalRon.compareTo(monthlySavings) > 0) {
                ra.addFlashAttribute("errorMessage",
                        "Economii insuficiente! Necesar: " + totalRon.setScale(2, RoundingMode.HALF_UP)
                                + " RON, disponibil: " + monthlySavings + " RON");
                return "redirect:/investments/trade";
            }
        } else {
            BigDecimal unitsHeld = portfolioService.getPortfolio(currentUser.getId()).stream()
                    .filter(p -> p.getFund().getId().equals(fund.getId()))
                    .map(p -> p.getNetUnits())
                    .findFirst()
                    .orElse(BigDecimal.ZERO);

            if (form.getUnits().compareTo(unitsHeld) > 0) {
                ra.addFlashAttribute("errorMessage",
                        "Nu detii suficiente unitati! Detii: " + unitsHeld
                                + ", incerci sa vinzi: " + form.getUnits());
                return "redirect:/investments/trade";
            }
        }

        form.setPricePerUnit(currentPrice);

        log.info("Executare tranzactie: fundId={}, op={}, units={}, price={}",
                form.getFundId(), form.getOperationType(),
                form.getUnits(), form.getPricePerUnit());

        try {
            portfolioService.executeTransaction(form, currentUser.getId());
            ra.addFlashAttribute("successMessage", "Tranzactie de investitie executata!");
        } catch (Exception ex) {
            log.error("Eroare executare tranzactie: {}", ex.getMessage(), ex);
            ra.addFlashAttribute("errorMessage", "Eroare: " + ex.getMessage());
        }
        return "redirect:/investments";
    }

    @GetMapping("/suggest")
    public String suggestFunds(@RequestParam(required = false) InvestmentFund.RiskLevel risk,
                               @RequestParam(required = false) InvestmentFund.TimeHorizon horizon,
                               Model model) {
        model.addAttribute("riskLevels", InvestmentFund.RiskLevel.values());
        model.addAttribute("timeHorizons", InvestmentFund.TimeHorizon.values());

        if (risk != null || horizon != null) {
            model.addAttribute("suggestions", portfolioService.suggestFunds(risk, horizon));
            model.addAttribute("selectedRisk", risk);
            model.addAttribute("selectedHorizon", horizon);
        }
        return "investments/suggest";
    }

    private BigDecimal convertToRon(BigDecimal amount, String currency) {
        if ("USD".equals(currency)) {
            return amount.multiply(USD_TO_RON);
        } else if ("EUR".equals(currency)) {
            return amount.multiply(EUR_TO_RON);
        }
        return amount;
    }
}