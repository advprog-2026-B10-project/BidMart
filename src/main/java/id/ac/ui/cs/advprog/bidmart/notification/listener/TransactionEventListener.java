package id.ac.ui.cs.advprog.bidmart.notification.listener;

import id.ac.ui.cs.advprog.bidmart.notification.entity.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notification.service.NotificationService;
import id.ac.ui.cs.advprog.bidmart.wallet.model.Transaction;
import id.ac.ui.cs.advprog.bidmart.wallet.model.TransactionCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionEventListener {

    private final NotificationService notificationService;

    @EventListener
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        Transaction tx = event.getTransaction();
        notificationService.send(
                tx.getUserId(),
                NotificationType.TRANSACTION,
                "Transaksi Dompet",
                buildMessage(tx),
                null);
    }

    private String buildMessage(Transaction tx) {
        return switch (tx.getType()) {
            case "TOP_UP"   -> "Top up berhasil Rp " + tx.getAmount();
            case "WITHDRAW" -> "Withdraw Rp " + tx.getAmount();
            case "PAYMENT"  -> "Pembayaran berhasil Rp " + tx.getAmount();
            case "HOLD"     -> "Dana ditahan Rp " + tx.getAmount();
            case "RELEASE"  -> "Dana dilepas Rp " + tx.getAmount();
            default         -> "Transaksi Rp " + tx.getAmount();
        };
    }
}
