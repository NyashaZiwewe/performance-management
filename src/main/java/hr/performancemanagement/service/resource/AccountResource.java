package hr.performancemanagement.service.resource;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.exception.ResourceNotFoundException;
import hr.performancemanagement.service.api.AccountService;
import hr.performancemanagement.utils.dto.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountResource {

    private final AccountService accountService;

    @GetMapping("/client/{clientId}")
    public ResponseEntity<CommonResponse<List<Account>>> listAccountsByClientId(@PathVariable long clientId) {
        List<Account> accounts = accountService.listAllAccountsByClientId(clientId);
        return ResponseEntity.ok(CommonResponse.<List<Account>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Accounts retrieved successfully")
                .data(accounts)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<Account>> getAccount(@PathVariable long id) {
        Account account = accountService.getAccountById(id);
        if (account == null) {
            throw new ResourceNotFoundException("Account not found with id " + id);
        }
        return ResponseEntity.ok(CommonResponse.<Account>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Account retrieved successfully")
                .data(account)
                .build());
    }

    @PostMapping
    public ResponseEntity<CommonResponse<Account>> saveAccount(@RequestBody Account account) {
        Account savedAccount = accountService.saveAccount(account);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.<Account>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Account saved successfully")
                .data(savedAccount)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResponse<Void>> deleteAccount(@PathVariable long id) {
        Account account = accountService.getAccountById(id);
        if (account == null) {
            throw new ResourceNotFoundException("Account not found with id " + id);
        }
        accountService.deleteAccount(account);
        return ResponseEntity.ok(CommonResponse.<Void>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Account deleted successfully")
                .data(null)
                .build());
    }
}
