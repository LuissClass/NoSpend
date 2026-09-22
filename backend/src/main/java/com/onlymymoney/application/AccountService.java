package com.onlymymoney.application;
import com.onlymymoney.domain.model.*; import com.onlymymoney.domain.port.*; import org.springframework.stereotype.Service; import java.math.BigDecimal; import java.time.Instant; import java.util.*;
@Service public class AccountService {
 private final AccountRepository accounts; private final CategoryRepository categories;
 public AccountService(AccountRepository a,CategoryRepository c){accounts=a;categories=c;}
 public List<Account> all(){return accounts.findAll();}
 public Account get(Long id){return accounts.findById(id).orElseThrow();}
 public Account create(String holder){Account a=accounts.save(new Account(null,holder,Instant.now(),BigDecimal.ZERO));categories.save(new Category(null,a.id(),"EXPENSES",BigDecimal.ZERO,true));return a;}
 public List<Category> categories(Long id){return categories.findByAccount(id);}
}
