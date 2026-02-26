package gov.meghalaya.meghaconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MeghaConnectApplication {
    public static void main(String[] args) {
        SpringApplication.run(MeghaConnectApplication.class, args);
    }
}
