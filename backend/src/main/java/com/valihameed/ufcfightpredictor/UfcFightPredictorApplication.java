package com.valihameed.ufcfightpredictor;

import com.valihameed.ufcfightpredictor.repository.userRepository;
import com.valihameed.ufcfightpredictor.users.userService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UfcFightPredictorApplication {

    public static void main(String[] args) {
        SpringApplication.run(UfcFightPredictorApplication.class, args);
        System.out.println("UfcFightPredictorApplication started");

    }

}
