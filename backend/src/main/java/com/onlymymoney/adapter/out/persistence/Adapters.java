package com.onlymymoney.adapter.out.persistence;
import com.onlymymoney.domain.model.*; import com.onlymymoney.domain.port.*; import org.springframework.stereotype.Component; import java.util.*;
@Component class AccountPersistenceAdapter implements AccountRepository {
 private final AccountJpaRepository r; AccountPersistenceAdapter(AccountJpaRepository r){this.r=r;}
 public Optional<Account> findById(Long id){return r.findById(id).map(this::m);} public List<Account> findAll(){return r.findAll().stream().map(this::m).toList();}
 public Account save(Account a){return m(r.save(new AccountJpa(a.id(),a.holder(),a.totalBalance(),a.lastModification())));}
 private Account m(AccountJpa x){return new Account(x.getId(),x.getHolder(),x.getLastModification(),x.getTotalBalance());}
}
@Component class CategoryPersistenceAdapter implements CategoryRepository {
 private final CategoryJpaRepository r; CategoryPersistenceAdapter(CategoryJpaRepository r){this.r=r;}
 public List<Category> findByAccount(Long id){return r.findByAccountId(id).stream().map(this::m).toList();}
 public Optional<Category> findById(Long id){return r.findById(id).map(this::m);}
 public Optional<Category> findGeneral(Long id){return r.findByAccountIdAndGeneralExpensesTrue(id).map(this::m);}
 public Category save(Category c){return m(r.save(new CategoryJpa(c.id(),c.accountId(),c.description(),c.availableBalance(),c.generalExpenses())));}
 private Category m(CategoryJpa x){return new Category(x.getId(),x.getAccountId(),x.getDescription(),x.getAvailableBalance(),x.isGeneralExpenses());}
}
@Component class MovementPersistenceAdapter implements MovementRepository {
 private final MovementJpaRepository r; MovementPersistenceAdapter(MovementJpaRepository r){this.r=r;}
 public boolean existsByAvailableBalance(Long a,java.math.BigDecimal b){return r.existsByAccountIdAndAvailableBalance(a,b);}
 public Movement save(Movement m){return m(r.save(new MovementJpa(m.id(),m.accountId(),m.concept(),m.date(),m.amount(),m.availableBalance(),m.status(),m.categoryId(),java.time.Instant.now())));}
 public List<Movement> findLatest(Long a,int limit,Long c){return (c==null?r.findTop50ByAccountIdOrderByMovementDateDescIdDesc(a):r.findTop50ByAccountIdAndCategoryIdOrderByMovementDateDescIdDesc(a,c)).stream().limit(limit).map(this::m).toList();}
 public List<Movement> findAllByCategory(Long a,Long c){return r.findByAccountIdAndCategoryIdOrderByMovementDateDescIdDesc(a,c).stream().map(this::m).toList();}
 public Optional<Movement> findById(Long id){return r.findById(id).map(this::m);} public long count(Long id){return r.countByAccountId(id);}
 private Movement m(MovementJpa x){return new Movement(x.getId(),x.getAccountId(),x.getConcept(),x.getMovementDate(),x.getAmount(),x.getAvailableBalance(),x.getImportStatus(),x.getCategoryId());}
}
@Component class BudgetPersistenceAdapter implements BudgetRepository {
 private final BudgetJpaRepository r; BudgetPersistenceAdapter(BudgetJpaRepository r){this.r=r;}
 public Budget save(Budget b){var x=r.save(new BudgetJpa(b.id(),b.categoryId(),b.periodStart(),b.periodEnd(),b.amount()));return new Budget(x.getId(),x.getCategoryId(),x.getPeriodStart(),x.getPeriodEnd(),x.getAmount());}
 public List<Budget> findByCategory(Long c){return r.findByCategoryId(c).stream().map(x->new Budget(x.getId(),x.getCategoryId(),x.getPeriodStart(),x.getPeriodEnd(),x.getAmount())).toList();}
}
