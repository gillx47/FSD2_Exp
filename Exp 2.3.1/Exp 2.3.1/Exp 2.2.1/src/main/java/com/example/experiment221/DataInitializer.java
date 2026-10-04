package com.example.experiment221;

import com.example.experiment221.model.Product;
import com.example.experiment221.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(ProductRepository repository) {
        return args -> {
            repository.save(new Product("Laptop", 1200.00));
            repository.save(new Product("Smartphone", 800.00));
            repository.save(new Product("Headphones", 150.00));
            repository.save(new Product("Monitor", 300.00));
            repository.save(new Product("Keyboard", 50.00));
            repository.save(new Product("Mouse", 30.00));
            repository.save(new Product("Printer", 150.00));
        };
    }
}
