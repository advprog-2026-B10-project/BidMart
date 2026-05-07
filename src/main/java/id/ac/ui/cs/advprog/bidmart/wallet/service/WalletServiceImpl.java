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

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final WalletRepository WalletRepository;
    private final TransactionRepository TransactionRepository;
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
    public void topUp(String userId, Double amount) {
        Wallet wallet = getOrCreateWallet(userId);
        wallet.setBalance(wallet.getBalance() + amount);
        walletRepository.save(wallet);

        saveTransaction(userId, amount, "TOP_UP");
    }

    @Override
    @Transactional
    public void withdraw(String userId, Double amount) {
        Wallet wallet = getOrCreateWallet(userId);

        if (wallet.getBalance() < amount) {
            throw new RuntimeException("Insufficient balance");
        }

        wallet.setBalance(wallet.getBalance() - amount);
        walletRepository.save(wallet);

        saveTransaction(userId, amount, "WITHDRAW");
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
        saveTransaction(userId, amount, "HOLD");
    }

    @Override
    @Transactional
    public void releaseHeldBalance(String userId, Double amount) {
        Wallet wallet = getOrCreateWallet(userId);

        wallet.setHeldBalance(wallet.getHeldBalance() - amount);
        wallet.setBalance(wallet.getBalance() + amount);

        walletRepository.save(wallet);
        saveTransaction(userId, amount, "RELEASE");
    }

    @Override
    @Transactional
    public void deductHeldBalance(String userId, Double amount) {
        Wallet wallet = getOrCreateWallet(userId);

        wallet.setHeldBalance(wallet.getHeldBalance() - amount);

        walletRepository.save(wallet);
        saveTransaction(userId, amount, "PAYMENT");
    }

    private void saveTransaction(String userId, Double amount, String type) {
        Transaction transaction = transactionRepository.save(
                Transaction.builder()
                        .userId(userId)
                        .amount(amount)
                        .type(type)
                        .build()
        );
        // Publikasi event transaksi
        eventPublisher.publishEvent(new TransactionCreatedEvent(this, transaction));
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

        saveTransaction(userId, amount, "PAYMENT");
    }

    @Override
    @Transactional
    public void handleWin(String userId, Long amount) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        // KOREKSI: Potong dari heldBalance, karena dana sudah di-hold saat bid
        if (wallet.getHeldBalance() < amount) {
            throw new RuntimeException("Insufficient held balance");
        }

        wallet.setHeldBalance(wallet.getHeldBalance() - amount.doubleValue());
        walletRepository.save(wallet);

        // TODO 2: Publikasi Event (Menggunakan WalletEvent sesuai file kamu)
        eventPublisher.publishEvent(new WalletEvent(userId));

        // Simpan juga ke history transaksi
        saveTransaction(userId, amount.doubleValue(), "WIN_PAYMENT");

        System.out.println("WIN AUCTION processed for " + userId);
    }
}