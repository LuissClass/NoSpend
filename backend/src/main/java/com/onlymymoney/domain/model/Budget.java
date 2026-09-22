package com.onlymymoney.domain.model;
import java.math.BigDecimal; import java.time.LocalDate;
public record Budget(Long id,Long categoryId,LocalDate periodStart,LocalDate periodEnd,BigDecimal amount) {}
