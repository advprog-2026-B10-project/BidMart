package id.ac.ui.cs.advprog.bidmart.notification.listener;

import id.ac.ui.cs.advprog.bidmart.wallet.model.TransactionCreatedEvent;
import id.ac.ui.cs.advprog.bidmart.wallet.model.Transaction;
import id.ac.ui.cs.advprog.bidmart.notification.entity.Notification;
import id.ac.ui.cs.advprog.bidmart.notification.entity.NotificationChannel;
import id.ac.ui.cs.advprog.bidmart.notification.entity.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionEventListener {

    private final NotificationRepository notificationRepository;

    @EventListener
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        Transaction tx = event.getTransaction();

        String message = buildMessage(tx);

        Notification notification = Notification.builder()
                .userId(tx.getUserId())
                .title("Transaksi Dompet")
                .message(message)
                .type(NotificationType.TRANSACTION)
                .channel(NotificationChannel.IN_APP)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }

    private String buildMessage(Transaction tx) {
        switch (tx.getType()) {
            case "TOP_UP":
                return "Top up berhasil Rp " + tx.getAmount();
            case "WITHDRAW":
                return "Withdraw Rp " + tx.getAmount();
            case "PAYMENT":
                return "Pembayaran berhasil Rp " + tx.getAmount();
            case "HOLD":
                return "Dana ditahan Rp " + tx.getAmount();
            case "RELEASE":
                return "Dana dilepas Rp " + tx.getAmount();
            default:
                return "Transaksi Rp " + tx.getAmount();
        }
    }
}
