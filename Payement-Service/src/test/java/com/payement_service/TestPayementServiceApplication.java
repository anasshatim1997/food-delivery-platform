package com.payement_service;

import org.springframework.boot.SpringApplication;

public class TestPayementServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(PayementServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
