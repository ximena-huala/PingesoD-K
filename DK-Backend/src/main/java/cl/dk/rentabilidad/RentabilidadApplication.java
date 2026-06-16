package cl.dk.rentabilidad;

import cl.dk.rentabilidad.integration.falabella.FalabellaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(FalabellaProperties.class)
public class RentabilidadApplication {

	public static void main(String[] args) {
		SpringApplication.run(RentabilidadApplication.class, args);
	}

}
