package com.si.gateway.entry_point.http;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.SpringCamelContext;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * The Main App class.
 */
@Slf4j
@SpringBootApplication(scanBasePackages = "com.si.gateway")
public class Main {

    /**
     * The entry point of application.
     *
     * Instantiates connection to ActiveMQ and all routes, that defined in com.si.gateway packages of module
     *
     * @param args the input arguments
     * @throws Exception the exception
     */
    public static void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(Main.class);
        CamelContext camelContext = new SpringCamelContext(applicationContext);
        for (RouteBuilder rb : applicationContext.getBeansOfType(RouteBuilder.class).values()) {
            camelContext.addRoutes(rb);
        }

        camelContext.start();
    }
}
