package com.chargeslot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.time.Clock;

@SpringBootApplication
public class ChargeSlotApplication {
    public static void main(String[] args) { SpringApplication.run(ChargeSlotApplication.class, args); }
    @Bean Clock applicationClock() { return Clock.systemUTC(); }
}
