package id.ac.ui.cs.advprog.bidmart.catalog.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "katalog")
@Getter
@Setter
public class Catalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String judul;

    @Column(columnDefinition = "TEXT")
    private String deskripsi;

    private String gambar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kategori_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Category category;

    @Column(nullable = false)
    private Double hargaAwal;

    private Double hargaCadangan;

    private Integer durasiLelang;

    public Catalog() {}

    public Catalog(String judul, String deskripsi, String gambar, Category category, Double hargaAwal, Double hargaCadangan, Integer durasiLelang) {
        this.judul = judul;
        this.deskripsi = deskripsi;
        this.gambar = gambar;
        this.category = category;
        this.hargaAwal = hargaAwal;
        this.hargaCadangan = hargaCadangan;
        this.durasiLelang = durasiLelang;
    }
}