package id.ac.ui.cs.advprog.bidmart.wallet.repository;

import id.ac.ui.cs.advprog.bidmart.wallet.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}