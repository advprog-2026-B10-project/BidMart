package id.ac.ui.cs.advprog.bidmart.bidding.repository;

import id.ac.ui.cs.advprog.bidmart.bidding.entity.AuctionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import id.ac.ui.cs.advprog.bidmart.bidding.entity.Auction;

import java.util.List;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {

    List<Auction> findByStatusIn(List<AuctionStatus> statuses);
}