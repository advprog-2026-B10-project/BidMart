package id.ac.ui.cs.advprog.grouppreparation.katalog.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "katalog")
@Getter
@Setter
public class Katalog {

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
    private Kategori kategori;

    @Column(nullable = false)
    private Double hargaAwal;

    private Double hargaCadangan;

    private Integer durasiLelang;

    public Katalog() {}

    public Katalog(String judul, String deskripsi, String gambar, Kategori kategori, Double hargaAwal, Double hargaCadangan, Integer durasiLelang) {
        this.judul = judul;
        this.deskripsi = deskripsi;
        this.gambar = gambar;
        this.kategori = kategori;
        this.hargaAwal = hargaAwal;
        this.hargaCadangan = hargaCadangan;
        this.durasiLelang = durasiLelang;
    }
}