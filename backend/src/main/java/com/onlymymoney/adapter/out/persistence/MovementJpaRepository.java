package com.onlymymoney.adapter.out.persistence; import org.springframework.data.jpa.repository.JpaRepository; import java.math.BigDecimal; import java.util.*;
public interface MovementJpaRepository extends JpaRepository<MovementJpa,Long>{
 boolean existsByAccountIdAndAvailableBalance(Long accountId,BigDecimal balance);
 List<MovementJpa> findTop50ByAccountIdAndCategoryIdOrderByMovementDateDescIdDesc(Long accountId,Long categoryId);
 List<MovementJpa> findTop50ByAccountIdOrderByMovementDateDescIdDesc(Long accountId);
 List<MovementJpa> findByAccountIdAndCategoryIdOrderByMovementDateDescIdDesc(Long accountId,Long categoryId);
 long countByAccountId(Long accountId);
}
