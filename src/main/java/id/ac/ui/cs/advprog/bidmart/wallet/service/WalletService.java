package id.ac.ui.cs.advprog.bidmart.wallet.service;

import id.ac.ui.cs.advprog.bidmart.wallet.model.Wallet;
import id.ac.ui.cs.advprog.bidmart.wallet.model.Transaction;
import java.util.List;

public interface WalletService {
    Wallet getOrCreateWallet(String userId);
    void topUp(String userId, Double amount, String idempotencyKey);
    void withdraw(String userId, Double amount, String idempotencyKey);
    void holdBalance(String userId, Double amount);
    void releaseHeldBalance(String userId, Double amount);
    void deductHeldBalance(String userId, Double amount);
    void payFromHeldBalance(String userId, Double amount);
    void handleWin(String userId, Long amount);
    List<Transaction> getAllTransactions();
}