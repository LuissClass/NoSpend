package com.onlymymoney.domain.port;
import com.onlymymoney.domain.model.Category; import java.util.*;
public interface CategoryRepository {
 List<Category> findByAccount(Long accountId); Optional<Category> findById(Long id);
 Optional<Category> findGeneral(Long accountId); Category save(Category c);
}
