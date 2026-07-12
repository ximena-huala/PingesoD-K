package cl.dk.rentabilidad.integration.bsale;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BsaleManualImportServiceTest {

    @Test
    void parseaCostoChilenoConSimboloPeso() {
        assertEquals(new BigDecimal("10690.00"), BsaleManualImportService.parsearNumero("$ 10.690"));
    }

    @Test
    void parseaStockConDecimalChileno() {
        assertEquals(new BigDecimal("2672.00"), BsaleManualImportService.parsearNumero("2.672,0"));
    }

    @Test
    void estadoActivoPorDefecto() {
        assertTrue(BsaleManualImportService.parsearActivo(""));
        assertTrue(BsaleManualImportService.parsearActivo("Activo"));
    }

    @Test
    void estadoInactivo() {
        assertFalse(BsaleManualImportService.parsearActivo("Inactivo"));
    }
}
