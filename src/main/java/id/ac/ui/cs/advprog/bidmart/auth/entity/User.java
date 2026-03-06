package id.ac.ui.cs.advprog.bidmart.auth.entity;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email; 

    @Column(nullable = false)
    private String password; 

    private String displayName;
    
    @Builder.Default
    private boolean isEnabled = false; 

    @Column(name = "mfa_enabled", nullable = false)
    @Builder.Default
    private boolean mfaEnabled = false;

    private String phoneNumber;

    private String provider; 

    @Builder.Default
    private boolean mfaSecretSet = false;

    @Column(name = "verification_token")
    private String verificationToken;

    @Enumerated(EnumType.STRING)
    private Role role;


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return this.password; 
    }

    @Override
    public String getUsername() {
        return this.email; 
    }

    @Override
    public boolean isEnabled() {
        return this.isEnabled; 
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
}