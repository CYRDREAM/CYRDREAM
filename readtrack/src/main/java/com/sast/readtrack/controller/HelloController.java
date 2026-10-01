package com.sast.readtrack.controller;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
@RestController
public class HelloController {
    @GetMapping("/hello") public Map<String,String> hello() { return Map.of("message","Hello, World!"); }
}
