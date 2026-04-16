package id.ac.ui.cs.advprog.bidmart.notification.dto;

import id.ac.ui.cs.advprog.bidmart.notification.entity.NotificationPreference;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationPreferenceResponse {
    private Boolean inAppEnabled;
    private Boolean auctionStatusEnabled;
    private Boolean orderUpdateEnabled;
    private Boolean transactionEnabled;
    private Boolean systemEnabled;

    public static NotificationPreferenceResponse from(NotificationPreference p) {
        return new NotificationPreferenceResponse(
                p.getInAppEnabled(),
                p.getAuctionStatusEnabled(),
                p.getOrderUpdateEnabled(),
                p.getTransactionEnabled(),
                p.getSystemEnabled());
    }
}
