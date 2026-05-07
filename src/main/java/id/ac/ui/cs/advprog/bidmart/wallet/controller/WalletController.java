package id.ac.ui.cs.advprog.bidmart.wallet.controller;

import id.ac.ui.cs.advprog.bidmart.wallet.model.WalletEvent;
import id.ac.ui.cs.advprog.bidmart.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.ApplicationEventPublisher;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class WalletController {

    private final WalletService walletService;
    private final ApplicationEventPublisher eventPublisher;

    @GetMapping("/topup")
    public String topUp(@RequestParam String userId,
                        @RequestParam Double amount) {
        walletService.topUp(userId, amount);
        return "Top up successful";
    }

    @PostMapping("/withdraw")
    public String withdraw(@RequestParam String userId,
                           @RequestParam Double amount) {
        walletService.withdraw(userId, amount);
        return "Withdraw successful";
    }

    @PostMapping("/pay")
    public String pay(@RequestParam String userId,
                      @RequestParam Double amount) {
        walletService.payFromHeldBalance(userId, amount);
        return "Payment successful";
    }

    @PostMapping("/handle-win")
    public String handleWin(@RequestParam String userId, @RequestParam Long amount) {
        walletService.handleWin(userId, amount);
        return "Win handled successfully";
    }

    @PostMapping("/wallet/win")
    public ResponseEntity<String> winAuction(
            @RequestParam String userId,
            @RequestParam Long amount
    ) {
        walletService.handleWin(userId, amount);
        return ResponseEntity.ok("Dana ditahan → pembayaran berhasil");
    }

    @PostMapping("/wallet/test-event")
    public ResponseEntity<String> testEvent(
            @RequestParam String userId
    ) {
        eventPublisher.publishEvent(new WalletEvent(userId));
        return ResponseEntity.ok("Event published!");
    }

}