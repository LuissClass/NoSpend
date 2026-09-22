package com.onlymymoney.adapter.out.persistence;
import jakarta.persistence.*; import java.math.BigDecimal;
@Entity @Table(name="categories") public class CategoryJpa {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id; @Column(name="account_id") Long accountId; String description;
 @Column(name="available_balance",precision=19,scale=2) BigDecimal availableBalance; @Column(name="general_expenses") boolean generalExpenses;
 public CategoryJpa(){} public CategoryJpa(Long id,Long a,String d,BigDecimal b,boolean g){this.id=id;accountId=a;description=d;availableBalance=b;generalExpenses=g;}
 public Long getId(){return id;} public Long getAccountId(){return accountId;} public String getDescription(){return description;} public BigDecimal getAvailableBalance(){return availableBalance;} public boolean isGeneralExpenses(){return generalExpenses;}
}
