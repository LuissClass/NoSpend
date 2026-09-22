package com.onlymymoney.domain.port;
import com.onlymymoney.domain.model.Budget; import java.util.*;
public interface BudgetRepository { Budget save(Budget b); List<Budget> findByCategory(Long categoryId); }
