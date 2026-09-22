package com.onlymymoney.application;
import com.onlymymoney.domain.exception.DomainException; import com.onlymymoney.domain.model.*;
import com.onlymymoney.domain.port.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.io.InputStream; import java.math.BigDecimal; import java.util.*;
@Service
public class ImportService {
 private final CsvReader csv; private final MovementRepository movements; private final AccountRepository accounts; private final CategoryRepository categories;
 public ImportService(CsvReader csv,MovementRepository movements,AccountRepository accounts,CategoryRepository categories){this.csv=csv;this.movements=movements;this.accounts=accounts;this.categories=categories;}
 public ImportResult examine(InputStream in,Long accountId){
   accounts.findById(accountId).orElseThrow(()->new DomainException("Account not found"));
   List<Movement> rows=csv.read(in,accountId); List<Movement> accepted=new ArrayList<>(), ignored=new ArrayList<>(); List<String> errors=new ArrayList<>();
   Set<BigDecimal> batchBalances=new HashSet<>();
   for(Movement r:rows){
     if(r.amount()==null || r.availableBalance()==null || r.date()==null || r.concept()==null || r.concept().isBlank()){ignored.add(new Movement(r.id(),r.accountId(),r.concept(),r.date(),r.amount(),r.availableBalance(),ImportStatus.IGNORED,null)); errors.add("Invalid movement: "+r.concept()); continue;}
     if(movements.existsByAvailableBalance(accountId,r.availableBalance()) || !batchBalances.add(r.availableBalance())){ignored.add(new Movement(r.id(),r.accountId(),r.concept(),r.date(),r.amount(),r.availableBalance(),ImportStatus.IGNORED,null)); errors.add("Duplicate by available balance: "+r.availableBalance()); continue;}
     accepted.add(new Movement(null,accountId,r.concept(),r.date(),r.amount(),r.availableBalance(),ImportStatus.EXAMINING,null));
   }
   BigDecimal last=rows.isEmpty()?accounts.findById(accountId).orElseThrow().totalBalance():rows.get(rows.size()-1).availableBalance();
   return new ImportResult(accepted,ignored,errors,last);
 }
 @Transactional public ImportResult confirm(ImportResult result,Long accountId){
   Account a=accounts.findById(accountId).orElseThrow(()->new DomainException("Account not found"));
   for(Movement m:result.accepted()) movements.save(new Movement(null,accountId,m.concept(),m.date(),m.amount(),m.availableBalance(),ImportStatus.ADDED_TO_MAIN_CSV,null));
   accounts.save(new Account(a.id(),a.holder(),java.time.Instant.now(),result.totalBalance()));
   Category expenses=categories.findGeneral(accountId).orElseThrow(()->new DomainException("EXPENSES category missing"));
   BigDecimal newExpenses=expenses.availableBalance();
   for(Movement m:result.accepted()) newExpenses=newExpenses.add(m.amount());
   categories.save(new Category(expenses.id(),expenses.accountId(),expenses.description(),newExpenses,true));
   return result;
 }
}
