package com.familyfinance.repository;

import com.familyfinance.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByTokenAndTokenType(String token, VerificationToken.TokenType type);


    @Transactional
    void deleteByUserId(Long userId);

}