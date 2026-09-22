package com.onlymymoney.application;
import com.onlymymoney.domain.exception.DomainException; import com.onlymymoney.domain.model.*; import com.onlymymoney.domain.port.*; import org.springframework.stereotype.Service;
@Service public class MovementService {
 private final MovementRepository movements; private final CategoryRepository categories;
 public MovementService(MovementRepository m,CategoryRepository c){movements=m;categories=c;}
 public Movement assignCategory(Long accountId,Long movementId,Long categoryId){
  Movement m=movements.findById(movementId).orElseThrow(()->new DomainException("Movement not found"));
  Category c=categories.findById(categoryId).orElseThrow(()->new DomainException("Category not found"));
  if(!m.accountId().equals(accountId)||!c.accountId().equals(accountId))throw new DomainException("Resource does not belong to account");
  return movements.save(new Movement(m.id(),m.accountId(),m.concept(),m.date(),m.amount(),m.availableBalance(),m.status(),categoryId));
 }
}
