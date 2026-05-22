package id.ac.ui.cs.advprog.bidmart.bidding.strategy;

import id.ac.ui.cs.advprog.bidmart.bidding.entity.AuctionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuctionStrategyFactoryTest {

    @Mock
    private EnglishAuctionStrategy englishAuctionStrategy;

    @InjectMocks
    private AuctionStrategyFactory strategyFactory;

    @Test
    void getStrategy_english_shouldReturnEnglishAuctionStrategy() {
        AuctionStrategy result = strategyFactory.getStrategy(AuctionType.ENGLISH);

        assertNotNull(result);
        assertEquals(englishAuctionStrategy, result);
    }

    @Test
    void getStrategy_scholarship_shouldReturnDefaultEnglishStrategy() {
        AuctionStrategy result = strategyFactory.getStrategy(AuctionType.SCHOLARSHIP);

        assertNotNull(result);
        assertEquals(englishAuctionStrategy, result);
    }

    @Test
    void getStrategy_corporate_shouldReturnDefaultEnglishStrategy() {
        AuctionStrategy result = strategyFactory.getStrategy(AuctionType.CORPORATE);

        assertNotNull(result);
        assertEquals(englishAuctionStrategy, result);
    }
}