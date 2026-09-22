package com.onlymymoney.domain.model;
import java.math.BigDecimal; import java.time.Instant;
public record Account(Long id,String holder,Instant lastModification,BigDecimal totalBalance) {}
