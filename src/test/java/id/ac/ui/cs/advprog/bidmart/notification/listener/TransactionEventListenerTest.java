package id.ac.ui.cs.advprog.bidmart.notification.listener;

import id.ac.ui.cs.advprog.bidmart.notification.entity.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notification.service.NotificationService;
import id.ac.ui.cs.advprog.bidmart.wallet.model.Transaction;
import id.ac.ui.cs.advprog.bidmart.wallet.model.TransactionCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionEventListenerTest {

    @Mock NotificationService notificationService;
    @InjectMocks TransactionEventListener listener;

    @Test
    void handleTransactionCreated_delegatesToNotificationService() {
        Transaction tx = new Transaction();
        tx.setUserId("alice@x");
        tx.setType("TOP_UP");
        tx.setAmount(50000.0);

        listener.handleTransactionCreated(new TransactionCreatedEvent(this, tx));

        verify(notificationService).send(
                eq("alice@x"),
                eq(NotificationType.TRANSACTION),
                anyString(),
                contains("Top up"),
                isNull());
    }
}
