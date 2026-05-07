package id.ac.ui.cs.advprog.bidmart.wallet.service;

import id.ac.ui.cs.advprog.bidmart.wallet.model.Wallet;

public interface WalletService {
    Wallet getOrCreateWallet(String userId);
    void topUp(String userId, Double amount);
    void withdraw(String userId, Double amount);
    void holdBalance(String userId, Double amount);
    void releaseHeldBalance(String userId, Double amount);
    void deductHeldBalance(String userId, Double amount);
    void payFromHeldBalance(String userId, Double amount);
    void handleWin(String userId, Long amount);
}