package id.ac.ui.cs.advprog.bidmart.notification.service;

import id.ac.ui.cs.advprog.bidmart.notification.entity.NotificationPreference;
import id.ac.ui.cs.advprog.bidmart.notification.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository repository;

    public NotificationPreference getOrDefault(String userId) {
        return repository.findByUserId(userId).orElseGet(() ->
                NotificationPreference.builder()
                        .userId(userId)
                        .inAppEnabled(true)
                        .auctionStatusEnabled(true)
                        .orderUpdateEnabled(true)
                        .transactionEnabled(true)
                        .systemEnabled(true)
                        .build());
    }

    public NotificationPreference upsert(String userId,
                                         boolean inApp,
                                         boolean auctionStatus,
                                         boolean orderUpdate,
                                         boolean transaction,
                                         boolean system) {
        NotificationPreference pref = repository.findByUserId(userId)
                .orElseGet(() -> NotificationPreference.builder().userId(userId).build());
        pref.setInAppEnabled(inApp);
        pref.setAuctionStatusEnabled(auctionStatus);
        pref.setOrderUpdateEnabled(orderUpdate);
        pref.setTransactionEnabled(transaction);
        pref.setSystemEnabled(system);
        return repository.save(pref);
    }
}
