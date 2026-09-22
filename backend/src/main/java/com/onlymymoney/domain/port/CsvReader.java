package com.onlymymoney.domain.port;
import com.onlymymoney.domain.model.Movement; import java.io.InputStream; import java.util.*;
public interface CsvReader { List<Movement> read(InputStream input, Long accountId); }
