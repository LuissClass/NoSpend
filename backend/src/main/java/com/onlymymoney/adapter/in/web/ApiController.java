package com.onlymymoney.adapter.in.web;
import com.onlymymoney.application.*; import com.onlymymoney.domain.model.*; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.util.*;
@RestController @RequestMapping("/api")
public class ApiController {
 private final AccountService accounts; private final CategoryService cats; private final ImportService imports; private final MovementRepositoryView movementView; private final MovementService movementService; private final ReportService reports; private final BudgetService budgets;
 public ApiController(AccountService a,CategoryService c,ImportService i,MovementRepositoryView mv,MovementService ms,ReportService r,BudgetService bs){accounts=a;cats=c;imports=i;movementView=mv;movementService=ms;reports=r;budgets=bs;}
 @GetMapping("/accounts") List<Account> accounts(){return accounts.all();}
 @PostMapping("/accounts") Account createAccount(@RequestBody CreateAccount body){return accounts.create(body.holder());}
 @GetMapping("/accounts/{id}") Account account(@PathVariable Long id){return accounts.get(id);}
 @GetMapping("/accounts/{id}/categories") List<Category> categories(@PathVariable Long id){return cats.list(id);}
 @PostMapping("/accounts/{id}/categories") Category createCategory(@PathVariable Long id,@RequestBody CreateCategory body){return cats.create(id,body.description());}
 @PostMapping("/accounts/{id}/categories/transfer") List<Category> transfer(@PathVariable Long id,@RequestBody Transfer body){return cats.transfer(id,body.fromCategoryId(),body.toCategoryId(),body.amount());}
 @PatchMapping("/accounts/{accountId}/movements/{movementId}/category") Movement assignCategory(@PathVariable Long accountId,@PathVariable Long movementId,@RequestBody AssignCategory body){return movementService.assignCategory(accountId,movementId,body.categoryId());}
 @PostMapping("/accounts/{accountId}/budgets") Budget createBudget(@PathVariable Long accountId,@RequestBody CreateBudget b){return budgets.create(accountId,b.categoryId(),b.periodStart(),b.periodEnd(),b.amount());}
 @GetMapping("/categories/{categoryId}/budgets") List<Budget> budgets(@PathVariable Long categoryId){return budgets.list(categoryId);}
 @GetMapping("/accounts/{id}/movements") List<Movement> movements(@PathVariable Long id,@RequestParam(defaultValue="50") int limit,@RequestParam(required=false) Long categoryId){return movementView.latest(id,Math.min(Math.max(limit,1),50),categoryId);}
 @PostMapping(value="/accounts/{id}/imports",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) ImportResult examine(@PathVariable Long id,@RequestPart("file") MultipartFile file)throws Exception{return imports.examine(file.getInputStream(),id);}
 @PostMapping("/accounts/{id}/imports/confirm") ImportResult confirm(@PathVariable Long id,@RequestBody ImportResult result){return imports.confirm(result,id);}
 @GetMapping("/accounts/{id}/reports/expenses") Map<String,BigDecimal> expenses(@PathVariable Long id){return reports.expenseByCategory(id);}
 public record CreateBudget(@NotNull Long categoryId,@NotNull java.time.LocalDate periodStart,@NotNull java.time.LocalDate periodEnd,@NotNull @DecimalMin("0.00") BigDecimal amount){} public record AssignCategory(@NotNull Long categoryId){} public record CreateAccount(@NotBlank String holder){} public record CreateCategory(@NotBlank String description){} public record Transfer(@NotNull Long fromCategoryId,@NotNull Long toCategoryId,@NotNull @DecimalMin("0.01") BigDecimal amount){}
}
interface MovementRepositoryView { List<Movement> latest(Long id,int limit,Long categoryId); }
