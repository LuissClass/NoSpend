package com.onlymymoney.domain;
import org.junit.jupiter.api.Test; import java.math.*; import static org.junit.jupiter.api.Assertions.*;
class DomainRulesTest {
 @Test void moneyUsesDecimal(){assertEquals(new BigDecimal("0.30"),new BigDecimal("0.10").add(new BigDecimal("0.20")));}
 @Test void negativeIsExpense(){var m=new com.onlymymoney.domain.model.Movement(null,1L,"x",java.time.LocalDate.now(),new BigDecimal("-34.50"),new BigDecimal("100"),com.onlymymoney.domain.model.ImportStatus.CSV,null);assertTrue(m.isExpense());}
}
