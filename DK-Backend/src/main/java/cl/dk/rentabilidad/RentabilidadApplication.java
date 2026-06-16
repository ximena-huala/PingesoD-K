package cl.dk.rentabilidad;

import cl.dk.rentabilidad.config.JwtProperties;
import cl.dk.rentabilidad.integration.bsale.BsaleProperties;
import cl.dk.rentabilidad.integration.falabella.FalabellaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Punto de entrada de la API D&K Integrador.
 *
 * @see <a href="classpath:../../../../README.md">DK-Backend/README.md</a> para guía de instalación y uso
 */
@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, BsaleProperties.class, FalabellaProperties.class})
public class RentabilidadApplication {

	public static void main(String[] args) {
		SpringApplication.run(RentabilidadApplication.class, args);
	}

}
