package com.onlymymoney.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link AccountJpa} entities, keyed by {@link Long}.
 * <p>
 * It only inherits the standard CRUD operations from {@link JpaRepository}.
 */
public interface AccountJpaRepository extends JpaRepository<AccountJpa, Long> {
}