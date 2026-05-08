package id.ac.ui.cs.advprog.bidmart.notification.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationPreferenceRequest {
    private Boolean inAppEnabled;
    private Boolean auctionStatusEnabled;
    private Boolean orderUpdateEnabled;
    private Boolean transactionEnabled;
    private Boolean systemEnabled;
}
