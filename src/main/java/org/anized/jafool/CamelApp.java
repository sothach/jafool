package org.anized.jafool;

import io.vavr.control.Try;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class CamelApp {
    private static final Logger logger = LoggerFactory.getLogger(CamelApp.class);

    public static void main(final String[] args) throws Exception {
        logger.info("\uD83D\uDC2B Starting Camel journey...");

        final Main route = new Main();
        route.addRoutesBuilder(new CamelRoute(args[0]));

        final CompletableFuture<Void> camel = startCamel(route);
        final ProducerTemplate producer = route.getCamelContext().createProducerTemplate();
        producer.start();

        final Object result = producer.requestBody("direct:worldclock", "cet");

        logger.info("Route produced: " + result);
        camel.cancel(true);
    }

    private static CompletableFuture<Void> startCamel(final Main route) throws InterruptedException {
        final CompletableFuture<Void> camel =
                CompletableFuture.runAsync(() -> Try.of(() -> {
                    route.run();
                    return null;
                }));
        Thread.sleep(2000);
        return camel;
    }


}
