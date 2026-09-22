package com.onlymymoney.domain.port;
import com.onlymymoney.domain.model.Account; import java.util.*;
public interface AccountRepository { Optional<Account> findById(Long id); List<Account> findAll(); Account save(Account a); }
