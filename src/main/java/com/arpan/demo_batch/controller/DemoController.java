package com.arpan.demo_batch.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class DemoController {

    @GetMapping("/hello")
    public String hello() {
        log.info("Hello World....................");
        return "Hello World";
    }
}
