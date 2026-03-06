package id.ac.ui.cs.advprog.bidmart.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import id.ac.ui.cs.advprog.bidmart.catalog.entity.Catalog;

@Repository
public interface CatalogRepository extends JpaRepository<Catalog, Long> {

}
