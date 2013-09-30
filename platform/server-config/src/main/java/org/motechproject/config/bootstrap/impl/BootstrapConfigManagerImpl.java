package org.motechproject.config.bootstrap.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.motechproject.config.MotechConfigurationException;
import org.motechproject.config.bootstrap.BootstrapConfigManager;
import org.motechproject.config.bootstrap.ConfigFileReader;
import org.motechproject.config.bootstrap.Environment;
import org.motechproject.config.bootstrap.mapper.BootstrapConfigPropertyMapper;
import org.motechproject.config.domain.BootstrapConfig;
import org.motechproject.config.domain.ConfigLocation;
import org.motechproject.config.domain.ConfigSource;
import org.motechproject.config.domain.DBConfig;
import org.motechproject.config.filestore.ConfigLocationFileStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

import static org.motechproject.config.domain.ConfigLocation.FileAccessType;
import static org.motechproject.config.domain.ConfigLocation.FileAccessType.READABLE;
import static org.motechproject.config.domain.ConfigLocation.FileAccessType.WRITEABLE;

/**
 * Implementation of {@link org.motechproject.config.bootstrap.BootstrapConfigManager}.
 * <p/>
 * This class is concerned with managing the Bootstrap configuration.
 */
@Component
public class BootstrapConfigManagerImpl implements BootstrapConfigManager {

    private static Logger logger = Logger.getLogger(BootstrapConfigManagerImpl.class);

    static final String BOOTSTRAP_PROPERTIES = "bootstrap.properties";

    private Environment environment;
    private ConfigFileReader configFileReader;
    private ConfigLocationFileStore configLocationFileStore;

    @Autowired
    public BootstrapConfigManagerImpl(ConfigFileReader configFileReader, Environment environment, ConfigLocationFileStore configLocationFileStore) {
        this.environment = environment;
        this.configFileReader = configFileReader;
        this.configLocationFileStore = configLocationFileStore;
    }

    /**
     * This method is used to return the bootstrap configuration
     * <p/>
     * Try to Load and return the bootstrap configuration in the following order:
     * - Environment Variable: MOTECH_CONFIG_DIR - bootstrap props are in MOTECH_CONFIG_DIR/bootstrap.properties
     * - Environment Variable : MOTECH_DB_INSTANCE - Database config is loaded from one or more environment variables.  In this case we will use the MOTECH_TENANT_ID variable, to derive the complete database name. If tenant id is not specified, “DEFAULT” will be used as the tenant id.
     * - Default config location - bootstrap props in bootstrap.properties file from the default location. Default location of bootstrap.properties file is specified in config-locations.properties.
     * <p/>
     * Returns the Bootstrapconfig if bootstrap config is defined in any of the above locations or returns null.
     *
     * @return BootstrapConfig object
     */
    @Override
    public BootstrapConfig loadBootstrapConfig() {
        String configLocation = environment.getConfigDir();

        if (configLocation != null) {
            final String errorMessage = String.format("specified by '%s' environment variable.", Environment.MOTECH_CONFIG_DIR);
            return readBootstrapConfigFromFile(new File(getConfigFile(configLocation)), errorMessage);
        }

        try {
            return readBootstrapConfigFromEnvironment();
        } catch (MotechConfigurationException e) {
            logger.info("Could not find bootstrap configuration values from environment variables. So, trying to load " +
                    "from default location.", e);
            return readBootstrapConfigFromDefaultLocation();
        }
    }

    /**
     * Saves the bootstrap configuration provided, to the default Bootstrap file location.
     *
     * @param bootstrapConfig
     */
    @Override
    public void saveBootstrapConfig(BootstrapConfig bootstrapConfig) {
        File defaultBootstrapFile = getDefaultBootstrapFile(WRITEABLE);
        try {
            defaultBootstrapFile.getParentFile().mkdirs();
            defaultBootstrapFile.createNewFile();

            Properties bootstrapProperties = BootstrapConfigPropertyMapper.toProperties(bootstrapConfig);
            Writer writer = null;
            try {
                writer = new FileWriter(defaultBootstrapFile);
                bootstrapProperties.store(writer, "MOTECH bootstrap properties");
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }

        } catch (IOException e) {
            String errorMessage = "Error saving bootstrap properties to file";
            logger.error(errorMessage + " " + e.getMessage());
            throw new MotechConfigurationException(errorMessage, e);
        }
    }

    Iterable<ConfigLocation> getDefaultBootstrapConfigDir() {
        return configLocationFileStore.getAll();
    }

    File getDefaultBootstrapFile(FileAccessType accessType) {
        Iterable<ConfigLocation> configLocations = configLocationFileStore.getAll();

        for (ConfigLocation configLocation : configLocations) {
            try {
                return configLocation.getFile(BOOTSTRAP_PROPERTIES, accessType);
            } catch (MotechConfigurationException e) {
                logger.warn(e.getMessage());
                continue;
            }
        }

        throw new MotechConfigurationException(String.format("%s file is not %s from any of the default locations.", BOOTSTRAP_PROPERTIES, accessType.toString()));
    }

    private BootstrapConfig readBootstrapConfigFromDefaultLocation() {
        File defaultBootstrapFile;
        try {
            defaultBootstrapFile = getDefaultBootstrapFile(READABLE);
        } catch (MotechConfigurationException ex) {
            logger.warn(ex.getMessage());
            return null;
        }

        return readBootstrapConfigFromFile(defaultBootstrapFile, StringUtils.EMPTY);
    }

    private String getConfigFile(String configLocation) {
        return configLocation + File.separator + BOOTSTRAP_PROPERTIES;
    }

    private BootstrapConfig readBootstrapConfigFromEnvironment() {
        String dbUrl = environment.getDBUrl();
        String username = environment.getDBUsername();
        String password = environment.getDBPassword();
        String tenantId = environment.getTenantId();
        String configSource = environment.getConfigSource();

        return new BootstrapConfig(new DBConfig(dbUrl, username, password), tenantId, ConfigSource.valueOf(configSource));
    }

    private BootstrapConfig readBootstrapConfigFromFile(File configFile, String errorMessage) {
        try {
            logger.debug("Trying to load bootstrap configuration from " + configFile.getAbsolutePath());

            Properties properties = configFileReader.getProperties(configFile);
            return BootstrapConfigPropertyMapper.fromProperties(properties);
        } catch (IOException e) {
            final String message = "Error loading bootstrap properties from config file " + configFile + " " + errorMessage;
            logger.warn(message);
            throw new MotechConfigurationException(message, e);
        }
    }
}
