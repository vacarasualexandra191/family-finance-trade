package com.familyfinance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "ff_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "full_name")
    private String fullName;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String username;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id")
    private Family family;

    @Column(name = "family_role")
    private String familyRole; // "PARINTE" / "COPIL" etc.

    // Rolurile de securitate (ROLE_USER, ROLE_ADMIN etc), folosite de Spring Security.
    // Nota: numele tabelei/coloanelor de join e o presupunere - ajusteaza daca
    // difera la tine (verifica FF_USER_ROLES vazut in schema Oracle).
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "ff_user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new java.util.HashSet<>();

    @Column(precision = 19, scale = 2)
    private BigDecimal balance; // sold folosit pentru dashboard-ul copilului

    @Column(name = "balance_usd", precision = 19, scale = 2)
    private BigDecimal balanceUsd; // sold USD pentru investitii

    // Economii alocate manual de utilizator, separate de calculul automat
    // (venituri - cheltuieli) facut de ReportService.getTotalSavings().
    // Alimentat prin formularul "Muta in economii" de pe Dashboard.
    @Column(name = "savings_balance", precision = 19, scale = 2)
    private BigDecimal savingsBalance;

    // Seteaza automat createdAt la orice inserare noua, indiferent de unde
    // este creat obiectul (inregistrare normala, FamilyController.addMember,
    // etc.), ca sa nu mai poata aparea eroarea ORA-01400 (coloana NOT NULL
    // omisa la construirea manuala a unui User).
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}