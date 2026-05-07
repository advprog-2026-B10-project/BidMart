package id.ac.ui.cs.advprog.bidmart.wallet.model;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class WalletEventListener {

    @EventListener
    public void handleWalletEvent(WalletEvent event) {
        System.out.println("EVENT RECEIVED for user: " + event.getUserId());
    }
}