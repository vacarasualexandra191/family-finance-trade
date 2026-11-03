package com.familyfinance.controller;

import com.familyfinance.entity.Transaction;
import com.familyfinance.entity.User;
import com.familyfinance.repository.TransactionRepository;
import com.familyfinance.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow();
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        User currentUser = getCurrentUser();

        var transactions = transactionRepository.findByUserIdOrderByTransactionDateDesc(
                currentUser.getId(), PageRequest.of(page, 20));
        model.addAttribute("transactions", transactions);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", transactions.getTotalPages());
        return "dashboard/transactions";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("transaction", new Transaction());
        model.addAttribute("types", Transaction.Type.values());
        model.addAttribute("subCategories", Transaction.SubCategory.values());
        return "dashboard/transaction-form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("transaction") Transaction transaction,
                         BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("types", Transaction.Type.values());
            model.addAttribute("subCategories", Transaction.SubCategory.values());
            return "dashboard/transaction-form";
        }

        User currentUser = getCurrentUser();
        transaction.setUserId(currentUser.getId());

        if ("COPIL".equals(currentUser.getFamilyRole()) &&
                Transaction.Type.EXPENSE.equals(transaction.getType())) {

            BigDecimal soldCurent = currentUser.getBalance() != null ?
                    currentUser.getBalance() : BigDecimal.ZERO;

            if (soldCurent.compareTo(transaction.getAmount()) < 0) {
                model.addAttribute("types", Transaction.Type.values());
                model.addAttribute("subCategories", Transaction.SubCategory.values());
                model.addAttribute("errorMessage",
                        "⚠️ Sold insuficient! Sold disponibil: " +
                                soldCurent.setScale(2, java.math.RoundingMode.HALF_UP) +
                                " RON, cheltuiala: " +
                                transaction.getAmount() +
                                " RON. Contacteaza parintele pentru alimentarea contului!");
                return "dashboard/transaction-form";
            }

            currentUser.setBalance(soldCurent.subtract(transaction.getAmount()));
            userRepository.save(currentUser);
        }

        transactionRepository.save(transaction);
        ra.addFlashAttribute("successMessage", "Tranzactie salvata!");
        return "redirect:/transactions";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        User currentUser = getCurrentUser();

        Transaction transaction = transactionRepository.findById(id).orElse(null);
        if (transaction == null || !transaction.getUserId().equals(currentUser.getId())) {
            ra.addFlashAttribute("errorMessage", "Tranzactia nu a fost gasita.");
            return "redirect:/transactions";
        }

        model.addAttribute("transaction", transaction);
        model.addAttribute("types", Transaction.Type.values());
        model.addAttribute("subCategories", Transaction.SubCategory.values());
        model.addAttribute("isEdit", true);
        return "dashboard/transaction-form";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @Valid @ModelAttribute("transaction") Transaction transaction,
                       BindingResult result, Model model, RedirectAttributes ra) {

        User currentUser = getCurrentUser();

        Transaction existing = transactionRepository.findById(id).orElse(null);
        if (existing == null || !existing.getUserId().equals(currentUser.getId())) {
            ra.addFlashAttribute("errorMessage", "Tranzactia nu a fost gasita.");
            return "redirect:/transactions";
        }

        if (result.hasErrors()) {
            model.addAttribute("types", Transaction.Type.values());
            model.addAttribute("subCategories", Transaction.SubCategory.values());
            model.addAttribute("isEdit", true);
            return "dashboard/transaction-form";
        }

        // Pentru copil, la o cheltuiala, recalculam soldul: intai "restituim"
        // suma veche, apoi verificam si scadem noua suma - ca editarea sa nu
        // dubleze sau piarda din soldul disponibil.
        if ("COPIL".equals(currentUser.getFamilyRole()) &&
                Transaction.Type.EXPENSE.equals(existing.getType())) {

            BigDecimal soldCurent = currentUser.getBalance() != null ?
                    currentUser.getBalance() : BigDecimal.ZERO;
            BigDecimal soldRestituit = soldCurent.add(existing.getAmount());

            if (Transaction.Type.EXPENSE.equals(transaction.getType())
                    && soldRestituit.compareTo(transaction.getAmount()) < 0) {
                model.addAttribute("types", Transaction.Type.values());
                model.addAttribute("subCategories", Transaction.SubCategory.values());
                model.addAttribute("isEdit", true);
                model.addAttribute("errorMessage",
                        "⚠️ Sold insuficient pentru aceasta modificare! Sold disponibil: "
                                + soldRestituit.setScale(2, java.math.RoundingMode.HALF_UP) + " RON.");
                return "dashboard/transaction-form";
            }

            BigDecimal soldNou = Transaction.Type.EXPENSE.equals(transaction.getType())
                    ? soldRestituit.subtract(transaction.getAmount())
                    : soldRestituit;
            currentUser.setBalance(soldNou);
            userRepository.save(currentUser);
        }

        existing.setType(transaction.getType());
        existing.setSubCategory(transaction.getSubCategory());
        existing.setAmount(transaction.getAmount());
        existing.setTransactionDate(transaction.getTransactionDate());
        existing.setDescription(transaction.getDescription());
        existing.setCurrency(transaction.getCurrency() != null ? transaction.getCurrency() : "RON");

        transactionRepository.save(existing);
        ra.addFlashAttribute("successMessage", "Tranzactie actualizata!");
        return "redirect:/transactions";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        transactionRepository.deleteById(id);
        ra.addFlashAttribute("successMessage", "Tranzactie stearsa!");
        return "redirect:/transactions";
    }
}