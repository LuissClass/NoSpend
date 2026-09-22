package com.onlymymoney.adapter.in.web;
import com.onlymymoney.domain.model.Movement; import com.onlymymoney.domain.port.MovementRepository; import org.springframework.stereotype.Component; import java.util.*;
@Component public class MovementViewAdapter implements MovementRepositoryView {
 private final MovementRepository r; public MovementViewAdapter(MovementRepository r){this.r=r;}
 public List<Movement> latest(Long id,int limit,Long categoryId){return r.findLatest(id,limit,categoryId);}
}
