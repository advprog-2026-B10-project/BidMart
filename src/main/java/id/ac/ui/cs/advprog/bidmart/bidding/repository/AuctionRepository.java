package id.ac.ui.cs.advprog.bidmart.bidding.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import id.ac.ui.cs.advprog.bidmart.bidding.entity.Auction;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {
}