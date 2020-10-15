package example.spring.minimal;

import kamon.Kamon;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringMinimalExample {
    public static void main(String[] args) {
        Kamon.loadModules();
        SpringApplication.run(SpringMinimalExample.class, args);
    }
}
