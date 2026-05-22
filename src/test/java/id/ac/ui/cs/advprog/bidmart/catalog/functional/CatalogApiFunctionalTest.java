package id.ac.ui.cs.advprog.bidmart.catalog.functional;

import id.ac.ui.cs.advprog.bidmart.catalog.entity.Catalog;
import id.ac.ui.cs.advprog.bidmart.catalog.repository.CatalogRepository;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import net.serenitybdd.rest.SerenityRest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@ExtendWith(SerenityJUnit5Extension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CatalogApiFunctionalTest {

    @LocalServerPort
    private int port;

    @Autowired
    private CatalogRepository catalogRepository;

    @MockBean
    private JavaMailSender javaMailSender;

    @BeforeEach
    public void setup() {
        SerenityRest.setDefaultPort(port);
        // Ensure there's at least one data
        catalogRepository.deleteAll(); // optional: clear previous tests
        Catalog catalog = new Catalog();
        catalog.setJudul("Test Serenity Catalog");
        catalog.setDeskripsi("Functional Test Description");
        catalog.setHargaAwal(1000.0);
        catalog.setSellerId("seller-123");
        catalogRepository.save(catalog);
    }

    @Test
    public void testGetAllKatalogReturnsSuccessAndData() {
        // Serenity BDD step-by-step reporting
        SerenityRest.given()
                .log().all()
                .when()
                .get("/api/katalog")
                .then()
                .log().all()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    public void testGetKatalogByIdReturnsCorrectData() {
        Catalog saved = catalogRepository.findAll().get(0);
        
        SerenityRest.given()
                .pathParam("id", saved.getId())
                .when()
                .get("/api/katalog/{id}")
                .then()
                .statusCode(200)
                .body("judul", equalTo(saved.getJudul()));
    }
}
