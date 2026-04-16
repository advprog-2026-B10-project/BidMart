package id.ac.ui.cs.advprog.bidmart.wallet.model;

import id.ac.ui.cs.advprog.bidmart.wallet.model.Transaction;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TransactionCreatedEvent extends ApplicationEvent {

    private final Transaction transaction;

    public TransactionCreatedEvent(Object source, Transaction transaction) {
        super(source);
        this.transaction = transaction;
    }
}

