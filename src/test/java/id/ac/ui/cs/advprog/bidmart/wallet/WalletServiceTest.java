package id.ac.ui.cs.advprog.bidmart.wallet;

import id.ac.ui.cs.advprog.bidmart.wallet.model.Wallet;
import id.ac.ui.cs.advprog.bidmart.wallet.repository.TransactionRepository;
import id.ac.ui.cs.advprog.bidmart.wallet.repository.WalletRepository;
import id.ac.ui.cs.advprog.bidmart.wallet.service.WalletServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private WalletServiceImpl walletService;

    private Wallet mockWallet;
    private final String testUserId = "user-123";

    @BeforeEach
    void setUp() {
        mockWallet = Wallet.builder()
                .userId(testUserId)
                .balance(1000.0)
                .heldBalance(0.0)
                .build();
    }

    @Test
    void testTopUp_Success() {
        // Arrange
        when(walletRepository.findByUserId(testUserId)).thenReturn(Optional.of(mockWallet));
        when(transactionRepository.existsByIdempotencyKey("key-1")).thenReturn(false);

        // Act
        walletService.topUp(testUserId, 500.0, "key-1");

        // Assert
        assertEquals(1500.0, mockWallet.getBalance());
        verify(walletRepository, times(1)).save(mockWallet);
        verify(transactionRepository, times(1)).save(any());
    }

    @Test
    void testTopUp_IdempotencyPreventsDuplicate() {
        // Arrange
        when(transactionRepository.existsByIdempotencyKey("dup-key")).thenReturn(true);

        // Act
        walletService.topUp(testUserId, 500.0, "dup-key");

        // Assert
        verify(walletRepository, never()).save(any()); // Pastikan tidak ada aksi penyimpanan dompet
    }

    @Test
    void testWithdraw_Success() {
        // Arrange
        when(walletRepository.findByUserId(testUserId)).thenReturn(Optional.of(mockWallet));

        // Act
        walletService.withdraw(testUserId, 300.0, "key-2");

        // Assert
        assertEquals(700.0, mockWallet.getBalance());
        verify(walletRepository, times(1)).save(mockWallet);
    }

    @Test
    void testWithdraw_InsufficientBalance() {
        // Arrange
        when(walletRepository.findByUserId(testUserId)).thenReturn(Optional.of(mockWallet));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            walletService.withdraw(testUserId, 2000.0, "key-3");
        });

        // Saldo tidak boleh berkurang
        assertEquals(1000.0, mockWallet.getBalance());
        // Verifikasi transaksi gagal tetap dicatat (Audit Trail)
        verify(transactionRepository, times(1)).save(any());
    }

    @Test
    void testHoldBalance_Success() {
        // Arrange
        when(walletRepository.findByUserId(testUserId)).thenReturn(Optional.of(mockWallet));

        // Act
        walletService.holdBalance(testUserId, 400.0);

        // Assert
        assertEquals(600.0, mockWallet.getBalance());
        assertEquals(400.0, mockWallet.getHeldBalance());
        verify(walletRepository, times(1)).save(mockWallet);
    }
}