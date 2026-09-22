package com.onlymymoney.application;
import com.onlymymoney.domain.exception.DomainException; import com.onlymymoney.domain.model.*; import com.onlymymoney.domain.port.*; import org.springframework.stereotype.Service; import java.math.BigDecimal; import java.time.LocalDate; import java.util.*;
@Service public class BudgetService {
 private final BudgetRepository budgets; private final CategoryRepository categories;
 public BudgetService(BudgetRepository b,CategoryRepository c){budgets=b;categories=c;}
 public Budget create(Long accountId,Long categoryId,LocalDate start,LocalDate end,BigDecimal amount){
  Category c=categories.findById(categoryId).orElseThrow(()->new DomainException("Category not found"));
  if(!c.accountId().equals(accountId)||end.isBefore(start)||amount.signum()<0)throw new DomainException("Invalid budget");
  return budgets.save(new Budget(null,categoryId,start,end,amount));
 }
 public List<Budget> list(Long categoryId){return budgets.findByCategory(categoryId);}
}
