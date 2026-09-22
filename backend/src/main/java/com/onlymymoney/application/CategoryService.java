package com.onlymymoney.application;
import com.onlymymoney.domain.exception.DomainException; import com.onlymymoney.domain.model.*; import com.onlymymoney.domain.port.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal; import java.util.*;
@Service public class CategoryService {
 private final CategoryRepository cats;
 public CategoryService(CategoryRepository cats){this.cats=cats;}
 public List<Category> list(Long accountId){return cats.findByAccount(accountId);}
 public Category create(Long accountId,String name){if(name==null||name.isBlank())throw new DomainException("Category name is required"); return cats.save(new Category(null,accountId,name.trim(),BigDecimal.ZERO,false));}
 @Transactional public List<Category> transfer(Long accountId,Long fromId,Long toId,BigDecimal amount){
   if(amount==null||amount.signum()<=0)throw new DomainException("Transfer amount must be positive");
   Category from=cats.findById(fromId).orElseThrow(()->new DomainException("Source category not found"));
   Category to=cats.findById(toId).orElseThrow(()->new DomainException("Target category not found"));
   if(!from.accountId().equals(accountId)||!to.accountId().equals(accountId))throw new DomainException("Categories do not belong to account");
   if(from.availableBalance().compareTo(amount)<0)throw new DomainException("Insufficient available balance");
   cats.save(new Category(from.id(),from.accountId(),from.description(),from.availableBalance().subtract(amount),from.generalExpenses()));
   cats.save(new Category(to.id(),to.accountId(),to.description(),to.availableBalance().add(amount),to.generalExpenses()));
   return cats.findByAccount(accountId);
 }
}
