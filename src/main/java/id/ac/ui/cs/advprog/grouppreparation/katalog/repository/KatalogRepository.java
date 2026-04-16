package id.ac.ui.cs.advprog.grouppreparation.katalog.repository;

import id.ac.ui.cs.advprog.grouppreparation.katalog.model.Katalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface KatalogRepository extends JpaRepository<Katalog, Long>, JpaSpecificationExecutor<Katalog> {
    @Query(value = """
    SELECT COUNT(*) > 0 
    FROM bids b 
    JOIN auctions a ON b.auction_id = a.id 
    WHERE a.listing_id = :id
    """, nativeQuery = true)
    boolean hasBids(@Param("id") Long id);
}
