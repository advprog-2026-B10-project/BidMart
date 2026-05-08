package id.ac.ui.cs.advprog.bidmart.notification.repository;

import id.ac.ui.cs.advprog.bidmart.notification.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
    Optional<NotificationPreference> findByUserId(String userId);
}
