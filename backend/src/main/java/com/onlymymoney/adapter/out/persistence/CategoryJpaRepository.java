package com.onlymymoney.adapter.out.persistence; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface CategoryJpaRepository extends JpaRepository<CategoryJpa,Long>{List<CategoryJpa> findByAccountId(Long accountId); Optional<CategoryJpa> findByAccountIdAndGeneralExpensesTrue(Long accountId);}
