package com.onlymymoney.domain.port;
import com.onlymymoney.domain.model.Movement; import java.util.*;
public interface MovementRepository {
 boolean existsByAvailableBalance(Long accountId, java.math.BigDecimal balance);
 Movement save(Movement m);
 List<Movement> findLatest(Long accountId,int limit,Long categoryId);
 List<Movement> findAllByCategory(Long accountId,Long categoryId);
 Optional<Movement> findById(Long id);
 long count(Long accountId);
}
