package id.ac.ui.cs.advprog.bidmart;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class BidMartApplicationTests {

	@MockitoBean
    private JavaMailSender javaMailSender;

	@Test
	void contextLoads() {
	}

}
