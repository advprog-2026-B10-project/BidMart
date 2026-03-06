package id.ac.ui.cs.advprog.bidding.repository;

import id.ac.ui.cs.advprog.bidding.entity.Auction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionRepository extends JpaRepository<Auction, Long> {

}
