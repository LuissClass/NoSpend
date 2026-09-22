package com.onlymymoney.adapter.out.persistence; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface BudgetJpaRepository extends JpaRepository<BudgetJpa,Long>{List<BudgetJpa> findByCategoryId(Long categoryId);}
