package com.analytic_service;

import org.springframework.boot.SpringApplication;

public class TestAnalyticsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(AnalyticsServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
