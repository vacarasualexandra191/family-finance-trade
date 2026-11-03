package com.familyfinance.controller;

import com.familyfinance.entity.SavingsGoal;
import com.familyfinance.entity.User;
import com.familyfinance.repository.SavingsGoalRepository;
import com.familyfinance.repository.UserRepository;
import com.familyfinance.service.impl.EmailAlertService;
import com.familyfinance.service.impl.PortfolioService;
import com.familyfinance.service.impl.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final SavingsGoalRepository savingsGoalRepository;
    private final EmailAlertService emailAlertService;
    private final PortfolioService portfolioService;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow();
    }

    @GetMapping
    public String report(@RequestParam(required = false) Integer month,
                         @RequestParam(required = false) Integer year,
                         Model model) {
        LocalDate now = LocalDate.now();
        int m = month != null ? month : now.getMonthValue();
        int y = year != null ? year : now.getYear();

        User currentUser = getCurrentUser();
        Long userId = currentUser.getId();

        var report = reportService.generateMonthlyReport(m, y, userId);

        BigDecimal portfolioValueRon = portfolioService.getTotalPortfolioValue(userId)
                .multiply(BigDecimal.valueOf(4.8));
        BigDecimal savingsAfterInvestments = report.getSavings().subtract(portfolioValueRon);

        model.addAttribute("report", report);
        model.addAttribute("selectedMonth", m);
        model.addAttribute("selectedYear", y);
        model.addAttribute("portfolioValueRon", portfolioValueRon);
        model.addAttribute("savingsAfterInvestments", savingsAfterInvestments);
        return "reports/monthly";
    }

    @PostMapping("/check-alert")
    public String checkAlert(@RequestParam int month,
                             @RequestParam int year,
                             RedirectAttributes ra) {
        User currentUser = getCurrentUser();
        var report = reportService.generateMonthlyReport(month, year, currentUser.getId());
        boolean sent = emailAlertService.checkAndSendAlert(
                report.getTotalIncome(), report.getTotalExpense(), month, year);
        if (sent) {
            ra.addFlashAttribute("successMessage",
                    "Alerta trimisa pe email! Cheltuielile depasesc pragul setat.");
        } else {
            ra.addFlashAttribute("successMessage",
                    "Cheltuielile sunt sub prag — nicio alerta necesara.");
        }
        return "redirect:/reports?month=" + month + "&year=" + year;
    }

    @GetMapping("/test-email")
    public String testEmail(RedirectAttributes ra) {
        try {
            emailAlertService.sendTestEmail();
            ra.addFlashAttribute("successMessage", "✅ Email de test trimis cu succes!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "❌ Eroare SMTP: " + ex.getMessage());
        }
        return "redirect:/reports";
    }

    @GetMapping("/savings-goal")
    public String savingsGoalForm(Model model) {
        LocalDate now = LocalDate.now();
        SavingsGoal goal = savingsGoalRepository
                .findByYearAndMonth(now.getYear(), now.getMonthValue())
                .orElse(SavingsGoal.builder()
                        .year(now.getYear())
                        .month(now.getMonthValue())
                        .savingsPercentage(20)
                        .build());
        model.addAttribute("savingsGoal", goal);
        return "savings/goal-form";
    }

    @PostMapping("/savings-goal")
    public String saveSavingsGoal(@Valid @ModelAttribute("savingsGoal") SavingsGoal goal,
                                  BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) return "savings/goal-form";
        savingsGoalRepository.findByYearAndMonth(goal.getYear(), goal.getMonth())
                .ifPresent(existing -> goal.setId(existing.getId()));
        savingsGoalRepository.save(goal);
        ra.addFlashAttribute("successMessage", "Tinta de economisire salvata!");
        return "redirect:/reports?month=" + goal.getMonth() + "&year=" + goal.getYear();
    }
}