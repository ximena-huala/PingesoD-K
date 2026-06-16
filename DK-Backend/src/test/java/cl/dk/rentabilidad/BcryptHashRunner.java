package cl.dk.rentabilidad;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Utilidad local para generar hashes BCrypt (solo desarrollo). */
public class BcryptHashRunner {

    public static void main(String[] args) {
        System.out.println(new BCryptPasswordEncoder(12).encode("changeme"));
    }
}
