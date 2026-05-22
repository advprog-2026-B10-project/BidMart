package id.ac.ui.cs.advprog.bidmart.wallet.model;

public class WalletEvent {
    private final String userId;
    public WalletEvent(String userId) {
        this.userId = userId;
    }
    public String getUserId() {
        return userId;
    }
}