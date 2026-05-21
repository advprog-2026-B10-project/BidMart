package id.ac.ui.cs.advprog.bidmart;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import id.ac.ui.cs.advprog.bidmart.auth.entity.Role;
import id.ac.ui.cs.advprog.bidmart.auth.entity.User;
import id.ac.ui.cs.advprog.bidmart.auth.repository.UserRepository;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class BidMartApplication {
    public static void main(String[] args) {
        SpringApplication.run(BidMartApplication.class, args);
    }

    @Bean
    @org.springframework.context.annotation.Profile("!test")
    CommandLineRunner initAdmin(UserRepository repository, PasswordEncoder encoder) {
        return args -> {
            if (repository.findByEmail("admin@bidmart.com").isEmpty()) {
                User admin = User.builder()
                    .email("admin@bidmart.com")
                    .password(encoder.encode("AdminPass123"))
                    .displayName("System Admin")
                    .role(Role.ADMIN)
                    .isEnabled(true)
                    .build();
                repository.save(admin);
            }
        };
    }
}
