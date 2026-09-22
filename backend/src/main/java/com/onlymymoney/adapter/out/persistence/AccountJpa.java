package com.onlymymoney.adapter.out.persistence;
import jakarta.persistence.*; import java.math.BigDecimal; import java.time.Instant;
@Entity @Table(name="accounts") public class AccountJpa {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id; String holder; @Column(name="total_balance",precision=19,scale=2) BigDecimal totalBalance;
 @Column(name="last_modification") Instant lastModification;
 public AccountJpa(){} public AccountJpa(Long id,String h,BigDecimal b,Instant l){this.id=id;holder=h;totalBalance=b;lastModification=l;}
 public Long getId(){return id;} public String getHolder(){return holder;} public BigDecimal getTotalBalance(){return totalBalance;} public Instant getLastModification(){return lastModification;}
}
