package org.motechproject.security.service;

import org.motechproject.commons.api.MotechException;
import org.motechproject.commons.api.json.MotechJsonReader;
import org.motechproject.security.builder.SecurityRuleBuilder;
import org.motechproject.security.domain.MotechSecurityConfiguration;
import org.motechproject.security.domain.MotechURLSecurityRule;
import org.motechproject.security.repository.AllMotechSecurityRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The MotechProxyManager acts as a wrapper around Spring's FilterChainProxy.
 * The FilterChainProxy contains a list of immutable SecurityFilterChain objects
 * which Spring's security consults for filters when handling requests. In order
 * to dynamically define new secure, a new FilterChainProxy is constructed and the
 * reference is updated. The MotechProxyManager acts as a customized delegate
 * in MotechDelegatingFilterProxy.
 */
@Component
public class MotechProxyManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MotechProxyManager.class);
    private static final String DEFAULT_SECURITY_CONFIG_FILE = "defaultSecurityConfig.json";

    @Autowired
    private FilterChainProxy proxy;

    @Autowired
    private SecurityRuleBuilder securityRuleBuilder;

    @Autowired
    private MotechURLSecurityService motechSecurityService;

    @Autowired
    private AllMotechSecurityRules securityRulesDAO;

    private MotechJsonReader motechJsonReader = new MotechJsonReader();

    /**
     * Method to invoke to dynamically re-define the Spring security.
     * All rules converted into security filter chains in order
     * to create a new FilterChainProxy. The order of the rules in the
     * list matters for filtering purposes.
     */
    public synchronized void rebuildProxyChain() {
        LOGGER.info("Rebuilding proxy chain");
        updateSecurityChain(motechSecurityService.findAllSecurityRules());
        LOGGER.info("Rebuilt proxy chain");
    }

    /**
     * This method serves the same purpose of rebuildProxyChain, but does not require
     * any kind of security authentication so it should only ever be used by the activator,
     * which does not have an authentication object.
     */
    public void initializeProxyChain() {
        LOGGER.info("Initializing proxy chain");
        List<MotechURLSecurityRule> securityRules = securityRulesDAO.getRules();

        MotechSecurityConfiguration securityConfig = securityRulesDAO.getMotechSecurityConfiguration();
        //Security rules have not been configured in the DB, load from default security config
        if (securityConfig == null) {
            securityConfig = loadSecurityConfigFile();
            securityRulesDAO.addOrUpdate(securityConfig);
            securityRules = securityConfig.getSecurityRules();
        }

        updateSecurityChain(securityRules);
        LOGGER.info("Initialized proxy chain");
    }

    public FilterChainProxy getFilterChainProxy() {
        return proxy;
    }

    public void setFilterChainProxy(FilterChainProxy proxy) {
        this.proxy = proxy;
    }

    private void updateSecurityChain(List<MotechURLSecurityRule> securityRules) {
        LOGGER.debug("Updating security chain");
        List<SecurityFilterChain> newFilterChains = new ArrayList<>();

        for (MotechURLSecurityRule securityRule : securityRules) {
            if (securityRule.isActive()) {
                LOGGER.debug("Creating SecurityFilterChain for: {}", securityRule.getPattern());
                for (String method : securityRule.getMethodsRequired()) {
                    newFilterChains.add(securityRuleBuilder.buildSecurityChain(securityRule, method));
                }
                LOGGER.debug("Created SecurityFilterChain for: {}", securityRule.getPattern());
            }
        }

        LOGGER.debug("Updated security chain.");

        proxy = new FilterChainProxy(newFilterChains);
    }

    private MotechSecurityConfiguration loadSecurityConfigFile() {
        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(DEFAULT_SECURITY_CONFIG_FILE)) {
            LOGGER.debug("Load default security rules from: {}", DEFAULT_SECURITY_CONFIG_FILE);
            return (MotechSecurityConfiguration) motechJsonReader.readFromStream(in, MotechSecurityConfiguration.class);
        } catch (IOException e) {
            throw new MotechException("Error while loading json file", e);
        }
    }
}
