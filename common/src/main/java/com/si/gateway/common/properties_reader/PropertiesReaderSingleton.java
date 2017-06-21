package com.si.gateway.common.properties_reader;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration.*;

import java.io.File;

/**
 * The properties reader singleton.
 * Stores the set of configs and options for this app
 */
@Slf4j
public class PropertiesReaderSingleton {

    private static PropertiesReaderSingleton instance;

    private CompositeConfiguration config;

    /**
     * Gets an instance of properties reader singleton.
     * Instantiates a new instance of properties reader singleton if needed.
     *
     * @return the instance of properties reader singleton
     */
    public static PropertiesReaderSingleton getInstance() {
        if (instance == null) {
            instance = new PropertiesReaderSingleton();
        }
        return instance;
    }

    /**
     * Instantiates a new instance of properties reader singleton.
     */
    private PropertiesReaderSingleton() {
        config = new CompositeConfiguration();

        try {
            String propertiesPath = System.getProperty("propertiesPath");
            if ((propertiesPath != null) && !propertiesPath.isEmpty() && (new File(propertiesPath)).exists()) {
                config.addConfiguration(new PropertiesConfiguration(propertiesPath));
            }
        } catch (ConfigurationException e) {
            log.error("Error loading properties file by \"propertiesPath\"");
        }

        try {
            config.addConfiguration(new PropertiesConfiguration("gateway.properties"));
        } catch (ConfigurationException e) {
            log.error("Error loading gateway.properties file");
        }

        config.addConfiguration(new EnvironmentConfiguration());

        config.addConfiguration(new SystemConfiguration());
    }

    /**
     * Gets config.
     *
     * @return the config, that consists of external properties(enlisted at app start), internal(default) properties, EnvVars and SystemVars
     */
    public CompositeConfiguration getConfig() {
        return config;
    }

}
