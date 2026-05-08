package id.ac.ui.cs.advprog.bidmart.wallet.controller;

import id.ac.ui.cs.advprog.bidmart.wallet.model.WalletEvent;
import id.ac.ui.cs.advprog.bidmart.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.ApplicationEventPublisher;
import id.ac.ui.cs.advprog.bidmart.wallet.model.Transaction;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class WalletController {

    private final WalletService walletService;
    private final ApplicationEventPublisher eventPublisher;

    @GetMapping("/balance")
    public ResponseEntity<Map<String, Double>> getBalance(@RequestParam String userId) {
        var wallet = walletService.getOrCreateWallet(userId);
        return ResponseEntity.ok(Map.of(
                "balance", wallet.getBalance(),
                "heldBalance", wallet.getHeldBalance()
        ));
    }

    @GetMapping("/admin/transactions")
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(walletService.getAllTransactions());
    }

    @PostMapping("/topup")
    public String topUp(@RequestParam String userId,
                        @RequestParam Double amount,
                        @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        walletService.topUp(userId, amount, idempotencyKey);
        return "Top up successful";
    }

    @PostMapping("/withdraw")
    public String withdraw(@RequestParam String userId,
                           @RequestParam Double amount,
                           @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        walletService.withdraw(userId, amount, idempotencyKey);
        return "Withdraw successful";
    }

    @PostMapping("/pay")
    public String pay(@RequestParam String userId,
                      @RequestParam Double amount) {
        walletService.payFromHeldBalance(userId, amount);
        return "Payment successful";
    }

    @PostMapping("/test-event")
    public ResponseEntity<String> testEvent(
            @RequestParam String userId
    ) {
        eventPublisher.publishEvent(new WalletEvent(userId));
        return ResponseEntity.ok("Event published!");
    }
}