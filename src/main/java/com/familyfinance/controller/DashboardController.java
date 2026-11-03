package com.familyfinance.controller;

import com.familyfinance.dto.MonthlyReportDto;
import com.familyfinance.entity.SavingsAllocation;
import com.familyfinance.entity.Transaction;
import com.familyfinance.entity.User;
import com.familyfinance.repository.SavingsAllocationRepository;
import com.familyfinance.repository.TransactionRepository;
import com.familyfinance.repository.UserRepository;
import com.familyfinance.service.impl.PdfReportService;
import com.familyfinance.service.impl.PortfolioService;
import com.familyfinance.service.impl.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final ReportService reportService;
    private final PortfolioService portfolioService;
    private final UserRepository userRepository;
    private final SavingsAllocationRepository savingsAllocationRepository;
    private final PdfReportService pdfReportService;
    private final TransactionRepository transactionRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow();
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model, jakarta.servlet.http.HttpSession session) {
        User currentUser = getCurrentUser();
        Long userId = currentUser.getId();

        LocalDate now = LocalDate.now();
        MonthlyReportDto report = reportService.generateMonthlyReport(
                now.getMonthValue(), now.getYear(), userId);

        BigDecimal portfolioValue = portfolioService.getTotalPortfolioValue(userId);
        BigDecimal portfolioValueRon = portfolioValue.multiply(BigDecimal.valueOf(4.8));
        BigDecimal savingsAfterInvestments = report.getSavings().subtract(portfolioValueRon);

        // Suma deja alocata in economii luna aceasta (din SavingsAllocation, nu tranzactii).
        BigDecimal allocatedThisMonth = savingsAllocationRepository.sumByUserIdAndMonth(
                userId, now.getMonthValue(), now.getYear());

        // Venit disponibil de alocat = venituri lunii curente - ce ai alocat deja.
        BigDecimal totalIncome = report.getTotalIncome() != null ? report.getTotalIncome() : BigDecimal.ZERO;
        BigDecimal availableToAllocate = totalIncome.subtract(allocatedThisMonth);

        // Economiile alocate manual, total acumulat (folosit la Exchange/Trade).
        BigDecimal savingsBalance = currentUser.getSavingsBalance() != null ?
                currentUser.getSavingsBalance() : BigDecimal.ZERO;

        model.addAttribute("report", report);
        model.addAttribute("portfolioValue", portfolioValue);
        model.addAttribute("portfolioValueRon", portfolioValueRon);
        model.addAttribute("savingsAfterInvestments", savingsAfterInvestments);
        model.addAttribute("portfolio", portfolioService.getPortfolio(userId));
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("savingsBalance", savingsBalance);
        model.addAttribute("availableToAllocate", availableToAllocate);

        session.setAttribute("familyRole", currentUser.getFamilyRole());
        return "dashboard/index";
    }

    @PostMapping("/dashboard/allocate-savings")
    @Transactional
    public String allocateSavings(@RequestParam BigDecimal amount,
                                  RedirectAttributes ra) {
        User currentUser = getCurrentUser();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            ra.addFlashAttribute("errorMessage", "Suma trebuie sa fie pozitiva.");
            return "redirect:/dashboard";
        }

        LocalDate now = LocalDate.now();
        MonthlyReportDto report = reportService.generateMonthlyReport(
                now.getMonthValue(), now.getYear(), currentUser.getId());
        BigDecimal totalIncome = report.getTotalIncome() != null ?
                report.getTotalIncome() : BigDecimal.ZERO;

        BigDecimal allocatedThisMonth = savingsAllocationRepository.sumByUserIdAndMonth(
                currentUser.getId(), now.getMonthValue(), now.getYear());
        BigDecimal availableToAllocate = totalIncome.subtract(allocatedThisMonth);

        if (amount.compareTo(availableToAllocate) > 0) {
            ra.addFlashAttribute("errorMessage",
                    "Suma depaseste venitul disponibil (" + availableToAllocate + " RON).");
            return "redirect:/dashboard";
        }

        // Inregistram alocarea in tabela separata - NU e o cheltuiala reala.
        SavingsAllocation allocation = SavingsAllocation.builder()
                .amount(amount)
                .allocationDate(LocalDate.now())
                .userId(currentUser.getId())
                .build();
        savingsAllocationRepository.save(allocation);

        // Adaugam suma in savingsBalance - total acumulat, folosit la Exchange/Trade.
        BigDecimal newSavingsBalance = (currentUser.getSavingsBalance() != null ?
                currentUser.getSavingsBalance() : BigDecimal.ZERO).add(amount);
        currentUser.setSavingsBalance(newSavingsBalance);
        userRepository.save(currentUser);

        ra.addFlashAttribute("successMessage",
                "Au fost mutati " + amount + " RON in economii. Total economii: " + newSavingsBalance + " RON.");
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard/report-pdf")
    public ResponseEntity<byte[]> downloadReportPdf() {
        User currentUser = getCurrentUser();
        LocalDate now = LocalDate.now();
        MonthlyReportDto report = reportService.generateMonthlyReport(
                now.getMonthValue(), now.getYear(), currentUser.getId());


        BigDecimal childrenExpenses = null;
        if ("PARINTE".equals(currentUser.getFamilyRole())) {
            childrenExpenses = transactionRepository.sumByUserIdAndSubCategoryAndMonth(
                    currentUser.getId(), Transaction.SubCategory.ALTE_VENITURI,
                    now.getMonthValue(), now.getYear());
        }

        byte[] pdfBytes = pdfReportService.generateMonthlyReportPdf(
                report, currentUser.getFullName(), childrenExpenses);

        String filename = "raport-" + now.getMonthValue() + "-" + now.getYear() + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}