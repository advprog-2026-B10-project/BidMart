package id.ac.ui.cs.advprog.bidmart.auth.repository;

import id.ac.ui.cs.advprog.bidmart.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    
    Optional<RefreshToken> findByToken(String token);
    
    Optional<RefreshToken> findByEmail(String email);

    @Query("SELECT r FROM RefreshToken r WHERE r.email = ?1 AND r.revoked = false AND r.expiresAt > CURRENT_TIMESTAMP ORDER BY r.createdAt ASC")
    List<RefreshToken> findActiveSessionsByEmail(String email);

    @Query("SELECT COUNT(r) FROM RefreshToken r WHERE r.email = ?1 AND r.revoked = false AND r.expiresAt > CURRENT_TIMESTAMP")
    long countActiveSessionsByEmail(String email);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.email = ?1 AND r.revoked = false AND r.expiresAt > CURRENT_TIMESTAMP")
    int revokeActiveSessionsByEmail(String email);
    
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.email = ?1")
    void deleteByEmail(String email);
}
