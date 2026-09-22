package com.onlymymoney.domain.model;
import java.math.BigDecimal;
public record Category(Long id, Long accountId, String description, BigDecimal availableBalance, boolean generalExpenses) {}
