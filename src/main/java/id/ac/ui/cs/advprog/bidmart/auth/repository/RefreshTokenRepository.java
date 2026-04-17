package id.ac.ui.cs.advprog.bidmart.auth.repository;

import id.ac.ui.cs.advprog.bidmart.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    
    Optional<RefreshToken> findByToken(String token);
    
    Optional<RefreshToken> findByEmail(String email);
    
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.email = ?1")
    void deleteByEmail(String email);
}
