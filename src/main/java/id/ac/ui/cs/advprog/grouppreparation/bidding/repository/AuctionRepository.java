package id.ac.ui.cs.advprog.grouppreparation.bidding.repository;

import id.ac.ui.cs.advprog.grouppreparation.bidding.model.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {
}
