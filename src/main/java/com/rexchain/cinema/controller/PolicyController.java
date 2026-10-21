package com.rexchain.cinema.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PolicyController {
    @GetMapping("/policies")
    String policies() {
        return "policies";
    }
}
