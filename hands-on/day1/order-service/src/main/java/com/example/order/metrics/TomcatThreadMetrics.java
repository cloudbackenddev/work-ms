package com.example.order.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.AbstractProtocol;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

/**
 * Registers Tomcat thread-pool gauges after the web server is fully started.
 *
 * Uses ApplicationReadyEvent so Tomcat connectors are guaranteed to exist.
 * Exposes:
 *   order.tomcat.threads.busy  — threads currently blocked on a request
 *   order.tomcat.threads.max   — configured pool maximum
 */
@Component
public class TomcatThreadMetrics implements ApplicationListener<ApplicationReadyEvent> {

    private final MeterRegistry registry;

    public TomcatThreadMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        var ctx = event.getApplicationContext();
        if (!(ctx instanceof ServletWebServerApplicationContext webCtx)) return;
        if (!(webCtx.getWebServer() instanceof TomcatWebServer tomcat)) return;

        for (Connector connector : tomcat.getTomcat().getService().findConnectors()) {
            if (!(connector.getProtocolHandler() instanceof AbstractProtocol<?> protocol)) continue;

            Executor executor = protocol.getExecutor();
            if (!(executor instanceof ThreadPoolExecutor pool)) continue;

            Gauge.builder("order.tomcat.threads.busy", pool, ThreadPoolExecutor::getActiveCount)
                    .description("Tomcat threads currently blocked handling a request")
                    .register(registry);

            Gauge.builder("order.tomcat.threads.max", pool, ThreadPoolExecutor::getMaximumPoolSize)
                    .description("Tomcat maximum thread pool size")
                    .register(registry);

            // Log confirmation so you can see it in the startup output
            org.slf4j.LoggerFactory.getLogger(TomcatThreadMetrics.class)
                    .info("Registered Tomcat thread metrics — max pool size: {}", pool.getMaximumPoolSize());
        }
    }
}
