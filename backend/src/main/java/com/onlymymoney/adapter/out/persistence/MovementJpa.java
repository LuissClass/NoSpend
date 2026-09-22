package com.onlymymoney.adapter.out.persistence;
import com.onlymymoney.domain.model.ImportStatus; import jakarta.persistence.*; import java.math.BigDecimal; import java.time.*;
@Entity @Table(name="movements") public class MovementJpa {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id; @Column(name="account_id") Long accountId; String concept;
 @Column(name="movement_date") LocalDate movementDate; @Column(precision=19,scale=2) BigDecimal amount;
 @Column(name="available_balance",precision=19,scale=2) BigDecimal availableBalance; @Enumerated(EnumType.STRING) @Column(name="import_status") ImportStatus importStatus;
 @Column(name="category_id") Long categoryId; @Column(name="created_at") Instant createdAt;
 public MovementJpa(){} public MovementJpa(Long id,Long a,String c,LocalDate d,BigDecimal am,BigDecimal b,ImportStatus s,Long cat,Instant t){this.id=id;accountId=a;concept=c;movementDate=d;amount=am;availableBalance=b;importStatus=s;categoryId=cat;createdAt=t;}
 public Long getId(){return id;} public Long getAccountId(){return accountId;} public String getConcept(){return concept;} public LocalDate getMovementDate(){return movementDate;} public BigDecimal getAmount(){return amount;} public BigDecimal getAvailableBalance(){return availableBalance;} public ImportStatus getImportStatus(){return importStatus;} public Long getCategoryId(){return categoryId;} public Instant getCreatedAt(){return createdAt;}
}
