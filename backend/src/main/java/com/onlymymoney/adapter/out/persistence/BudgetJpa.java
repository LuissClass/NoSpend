package com.onlymymoney.adapter.out.persistence;
import jakarta.persistence.*; import java.math.BigDecimal; import java.time.LocalDate;
@Entity @Table(name="budgets") public class BudgetJpa {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id; @Column(name="category_id") Long categoryId; @Column(name="period_start") LocalDate periodStart; @Column(name="period_end") LocalDate periodEnd; BigDecimal amount;
 public BudgetJpa(){} public BudgetJpa(Long id,Long c,LocalDate s,LocalDate e,BigDecimal a){this.id=id;categoryId=c;periodStart=s;periodEnd=e;amount=a;}
 public Long getId(){return id;} public Long getCategoryId(){return categoryId;} public LocalDate getPeriodStart(){return periodStart;} public LocalDate getPeriodEnd(){return periodEnd;} public BigDecimal getAmount(){return amount;}
}
