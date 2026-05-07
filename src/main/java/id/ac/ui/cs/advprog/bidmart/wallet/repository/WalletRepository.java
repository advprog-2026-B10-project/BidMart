package id.ac.ui.cs.advprog.bidmart.wallet.repository;

import id.ac.ui.cs.advprog.bidmart.wallet.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserId(String userId);
}