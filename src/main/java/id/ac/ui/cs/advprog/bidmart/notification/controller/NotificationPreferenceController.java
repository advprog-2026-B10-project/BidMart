package id.ac.ui.cs.advprog.bidmart.notification.controller;

import id.ac.ui.cs.advprog.bidmart.notification.dto.NotificationPreferenceRequest;
import id.ac.ui.cs.advprog.bidmart.notification.dto.NotificationPreferenceResponse;
import id.ac.ui.cs.advprog.bidmart.notification.entity.NotificationPreference;
import id.ac.ui.cs.advprog.bidmart.notification.service.NotificationPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications/preferences")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class NotificationPreferenceController {

    private final NotificationPreferenceService service;

    @GetMapping
    public ResponseEntity<NotificationPreferenceResponse> get(Authentication authentication) {
        NotificationPreference pref = service.getOrDefault(authentication.getName());
        return ResponseEntity.ok(NotificationPreferenceResponse.from(pref));
    }

    @PutMapping
    public ResponseEntity<NotificationPreferenceResponse> put(
            Authentication authentication,
            @RequestBody NotificationPreferenceRequest request) {
        NotificationPreference saved = service.upsert(
                authentication.getName(),
                Boolean.TRUE.equals(request.getInAppEnabled()),
                Boolean.TRUE.equals(request.getAuctionStatusEnabled()),
                Boolean.TRUE.equals(request.getOrderUpdateEnabled()),
                Boolean.TRUE.equals(request.getTransactionEnabled()),
                Boolean.TRUE.equals(request.getSystemEnabled()));
        return ResponseEntity.ok(NotificationPreferenceResponse.from(saved));
    }
}
