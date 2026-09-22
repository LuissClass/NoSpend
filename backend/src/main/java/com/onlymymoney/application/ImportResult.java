package com.onlymymoney.application;
import com.onlymymoney.domain.model.Movement; import java.util.*;
public record ImportResult(List<Movement> accepted,List<Movement> ignored,List<String> errors,java.math.BigDecimal totalBalance) {}
