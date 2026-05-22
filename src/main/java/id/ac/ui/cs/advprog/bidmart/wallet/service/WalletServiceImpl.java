package id.ac.ui.cs.advprog.bidmart.wallet.service;

import id.ac.ui.cs.advprog.bidmart.wallet.model.Transaction;
import id.ac.ui.cs.advprog.bidmart.wallet.model.Wallet;
import id.ac.ui.cs.advprog.bidmart.wallet.model.TransactionCreatedEvent;
import id.ac.ui.cs.advprog.bidmart.wallet.model.WalletEvent;
import id.ac.ui.cs.advprog.bidmart.wallet.repository.TransactionRepository;
import id.ac.ui.cs.advprog.bidmart.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Wallet getOrCreateWallet(String userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> walletRepository.save(
                        Wallet.builder()
                                .userId(userId)
                                .balance(0.0)
                                .heldBalance(0.0)
                                .build()
                ));
    }

    @Override
    @Transactional
    public void topUp(String userId, Double amount, String idempotencyKey) {
        if (idempotencyKey != null && transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }

        Wallet wallet = getOrCreateWallet(userId);
        wallet.setBalance(wallet.getBalance() + amount);
        walletRepository.save(wallet);

        saveTransaction(userId, amount, "TOP_UP", idempotencyKey, "SUCCESS");
    }

    @Override
    @Transactional
    public void withdraw(String userId, Double amount, String idempotencyKey) {
        if (idempotencyKey != null && transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }

        Wallet wallet = getOrCreateWallet(userId);

        if (wallet.getBalance() < amount) {
            saveTransaction(userId, amount, "WITHDRAW", idempotencyKey, "FAILED");
            throw new RuntimeException("Insufficient balance");
        }

        wallet.setBalance(wallet.getBalance() - amount);
        walletRepository.save(wallet);

        saveTransaction(userId, amount, "WITHDRAW", idempotencyKey, "SUCCESS");
    }

    @Override
    @Transactional
    public void holdBalance(String userId, Double amount) {
        Wallet wallet = getOrCreateWallet(userId);
        if (wallet.getBalance() < amount) {
            throw new RuntimeException("Insufficient balance to hold");
        }
        wallet.setBalance(wallet.getBalance() - amount);
        wallet.setHeldBalance(wallet.getHeldBalance() + amount);
        walletRepository.save(wallet);
        
        saveTransaction(userId, amount, "HOLD", null, "SUCCESS");
    }

    @Override
    @Transactional
    public void releaseHeldBalance(String userId, Double amount) {
        Wallet wallet = getOrCreateWallet(userId);
        if (wallet.getHeldBalance() < amount) {
            throw new RuntimeException("Insufficient held balance to release");
        }
        wallet.setHeldBalance(wallet.getHeldBalance() - amount);
        wallet.setBalance(wallet.getBalance() + amount);
        walletRepository.save(wallet);
        
        saveTransaction(userId, amount, "RELEASE_HOLD", null, "SUCCESS");
    }

    @Override
    @Transactional
    public void deductHeldBalance(String userId, Double amount) {
        Wallet wallet = getOrCreateWallet(userId);
        if (wallet.getHeldBalance() < amount) {
            throw new RuntimeException("Insufficient held balance");
        }
        wallet.setHeldBalance(wallet.getHeldBalance() - amount);
        walletRepository.save(wallet);
        
        saveTransaction(userId, amount, "DEDUCT_HOLD", null, "SUCCESS");
    }

    @Override
    @Transactional
    public void payFromHeldBalance(String userId, Double amount) {
        Wallet wallet = getOrCreateWallet(userId);
        if (wallet.getHeldBalance() < amount) {
            throw new RuntimeException("Insufficient held balance");
        }
        wallet.setHeldBalance(wallet.getHeldBalance() - amount);
        walletRepository.save(wallet);
        
        saveTransaction(userId, amount, "PAYMENT", null, "SUCCESS");
    }

    @Override
    @Transactional
    public void handleWin(String userId, Long amount) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        Double amountDouble = amount.doubleValue();

        if (wallet.getHeldBalance() < amountDouble) {
            throw new RuntimeException("Insufficient held balance");
        }

        wallet.setHeldBalance(wallet.getHeldBalance() - amountDouble);
        walletRepository.save(wallet);

        eventPublisher.publishEvent(new WalletEvent(userId));

        saveTransaction(userId, amountDouble, "WIN_PAYMENT", null, "SUCCESS");

        System.out.println("WIN AUCTION processed for " + userId);
    }

    @Override
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    private void saveTransaction(String userId, Double amount, String type, String idempotencyKey, String status) {
        Transaction transaction = transactionRepository.save(
                Transaction.builder()
                        .userId(userId)
                        .amount(amount)
                        .type(type)
                        .idempotencyKey(idempotencyKey)
                        .status(status)
                        .build()
        );
        eventPublisher.publishEvent(new TransactionCreatedEvent(this, transaction));
    }
}