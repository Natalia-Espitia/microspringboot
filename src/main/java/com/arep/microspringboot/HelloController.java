package com.arep.microspringboot;

@RestController
public class HelloController {

    @GetMapping("/")
    public static String index(){
        return "Greetings from SpringBoot";
    }

    @GetMapping("/pi")
    public static String getPI(){
        return "PI= " + Math.PI;
    }

    @GetMapping("/hello")
    public static String hello(){
        return "Hello World!";
    }
}
