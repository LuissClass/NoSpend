package com.onlymymoney.domain.model;
import java.math.BigDecimal;
import java.time.LocalDate;
public record Movement(Long id, Long accountId, String concept, LocalDate date, BigDecimal amount,
                       BigDecimal availableBalance, ImportStatus status, Long categoryId) {
 public boolean isExpense(){ return amount.signum()<0; }
}
