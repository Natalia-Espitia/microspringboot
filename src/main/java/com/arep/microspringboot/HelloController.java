package com.arep.microspringboot;

@RestController
public class HelloController {

    @GetMapping("/")
    public String index(){
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>MicroSpringBoot</title></head>"
                + "<body><h1>MicroSpringBoot</h1><p>Servidor HTTP con IoC minimo usando reflexion.</p>"
                + "<ul><li><a href=\"/hello\">/hello</a></li><li><a href=\"/pi\">/pi</a></li>"
                + "<li><a href=\"/greeting?name=AREP\">/greeting?name=AREP</a></li>"
                + "<li><a href=\"/index.html\">/index.html</a></li></ul></body></html>";
    }

    @GetMapping("/pi")
    public String getPI(){
        return "PI= " + Math.PI;
    }

    @GetMapping("/hello")
    public String hello(){
        return "Hello World!";
    }
}
