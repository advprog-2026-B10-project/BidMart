package id.ac.ui.cs.advprog.bidmart.catalog.repository;

import id.ac.ui.cs.advprog.bidmart.catalog.entity.Catalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CatalogRepository extends JpaRepository<Catalog, Long>, JpaSpecificationExecutor<Catalog> {
    @Query(value = """
    SELECT COUNT(*) > 0 
    FROM bids b 
    JOIN auctions a ON b.auction_id = a.id 
    WHERE a.listing_id = :id
    """, nativeQuery = true)
    boolean hasBids(@Param("id") Long id);
}
