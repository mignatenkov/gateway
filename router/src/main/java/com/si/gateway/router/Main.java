package com.si.gateway.router;

import com.si.gateway.common.properties_reader.PropertiesReaderSingleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration.CompositeConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

/**
 * The Main App class.
 */
@Slf4j
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
        CompositeConfiguration config = PropertiesReaderSingleton.getInstance().getConfig();

        URI baseUri = UriBuilder.fromUri("http"+ (config.getBoolean("core.ssl.enabled")?"s":"") +"://"+
                config.getString("core.host") +"/").port(config.getInteger("core.port", 8080)).build();
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.packages("com.si.gateway");

        Server server;
        if (config.getBoolean("core.ssl.enabled")) {
            SslContextFactory contextFactory = new SslContextFactory();
            contextFactory.setKeyStorePath(config.getString("gateway.keystore.path"));
            contextFactory.setKeyStorePassword(config.getString("gateway.keystore.password"));

            server = JettyHttpContainerFactory.createServer(baseUri, contextFactory, resourceConfig);
        } else {
            server = JettyHttpContainerFactory.createServer(baseUri, resourceConfig);
        }
        server.start();
        log.debug("Core started!");
        server.join();
    }
}
