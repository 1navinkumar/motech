package org.motechproject.security.helper;

import java.util.ArrayList;
import java.util.List;
import org.motechproject.security.domain.MotechURLSecurityRule;
import org.motechproject.security.repository.AllMotechSecurityRules;
import org.motechproject.security.service.MotechURLSecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

/**
 * The MotechProxyManager acts as a wrapper around Spring's FilterChainProxy.
 * The FilterChainProxy contains a list of immutable SecurityFilterChain objects
 * which Spring's security consults for filters when handling requests. In order
 * to dynamically define new secure, a new FilterChainProxy is constructed and the 
 * reference is updated. The MotechProxyManager acts as a customized delegate 
 * in MotechDelegatingFilterProxy.
 *
 */
@Component
public class MotechProxyManager {

    @Autowired
    private FilterChainProxy proxy;

    @Autowired
    private SecurityRuleBuilder securityRuleBuilder;

    @Autowired
    private MotechURLSecurityService motechSecurityService;
    
    @Autowired
    private AllMotechSecurityRules securityRulesDAO;

    /**
     * Method to invoke to dynamically re-define the Spring security.
     * All rules converted into security filter chains in order
     * to create a new FilterChainProxy. The order of the rules in the 
     * list matters for filtering purposes.
     */
    public synchronized void rebuildProxyChain() {
        List<MotechURLSecurityRule> allSecurityRules = motechSecurityService.findAllSecurityRules();

        List<SecurityFilterChain> newFilterChains = new ArrayList<SecurityFilterChain>();

        for (MotechURLSecurityRule securityRule : allSecurityRules) {
            for (String method: securityRule.getMethodsRequired()) {
                newFilterChains.add(securityRuleBuilder.buildSecurityChain(securityRule, method));

            }
        }

        proxy = new FilterChainProxy(newFilterChains);
    }

    public FilterChainProxy getFilterChainProxy() {
        return proxy;
    }

    public void setFilterChainProxy(FilterChainProxy proxy) {
        this.proxy = proxy;
    }

    /**
     * This method serves the same purpose of rebuild proxy chain, but does not required
     * any kind of security authentication. So it should only ever be used by the activator,
     * which does not have an authentication object.
     */
    public void initializeProxyChain() {
        List<MotechURLSecurityRule> securityRules = securityRulesDAO.getRules();

        //Security rules have not been configured in the DB, use the default spring security chain from the security context
        if (securityRules.size() == 0) {
            return;
        }

        List<SecurityFilterChain> newFilterChains = new ArrayList<SecurityFilterChain>();

        for (MotechURLSecurityRule securityRule : securityRules) {
            for (String method: securityRule.getMethodsRequired()) {
                newFilterChains.add(securityRuleBuilder.buildSecurityChain(securityRule, method));

            }
        }

        proxy = new FilterChainProxy(newFilterChains);
    }
}
