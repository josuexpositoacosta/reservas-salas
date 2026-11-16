package com.josue.reservas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ReservasSalasApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReservasSalasApplication.class, args);
	}

}
