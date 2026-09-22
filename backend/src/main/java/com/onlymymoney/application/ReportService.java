package com.onlymymoney.application;
import com.onlymymoney.domain.port.*; import org.springframework.stereotype.Service; import java.math.BigDecimal; import java.util.*;
@Service public class ReportService {
 private final MovementRepository movements; private final CategoryRepository cats;
 public ReportService(MovementRepository m,CategoryRepository c){movements=m;cats=c;}
 public Map<String,BigDecimal> expenseByCategory(Long accountId){
   Map<String,BigDecimal> out=new LinkedHashMap<>();
   for(var c:cats.findByAccount(accountId)) out.put(c.description(),BigDecimal.ZERO);
   // Transaction category metadata is optional; uncategorized expenses are intentionally not assigned.
   for(var c:cats.findByAccount(accountId)){
     BigDecimal sum=movements.findAllByCategory(accountId,c.id()).stream().filter(m->m.amount().signum()<0).map(m->m.amount().abs()).reduce(BigDecimal.ZERO,BigDecimal::add);
     out.put(c.description(),sum);
   }
   return out;
 }
}
