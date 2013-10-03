package org.motechproject.security.osgi;

import static org.osgi.framework.Bundle.ACTIVE;
import static org.osgi.framework.Bundle.UNINSTALLED;
import static org.osgi.framework.Bundle.RESOLVED;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.motechproject.security.domain.MotechSecurityConfiguration;
import org.motechproject.security.domain.MotechURLSecurityRule;
import org.motechproject.security.helper.MotechProxyManager;
import org.motechproject.security.model.PermissionDto;
import org.motechproject.security.model.RoleDto;
import org.motechproject.security.osgi.helper.SecurityTestConfigBuilder;
import org.motechproject.security.repository.AllMotechSecurityRules;
import org.motechproject.security.repository.AllMotechSecurityRulesCouchdbImpl;
import org.motechproject.security.service.MotechPermissionService;
import org.motechproject.security.service.MotechRoleService;
import org.motechproject.security.service.MotechUserService;
import org.motechproject.testing.osgi.BaseOsgiIT;
import org.motechproject.testing.utils.PollingHttpClient;
import org.motechproject.testing.utils.TestContext;
import org.motechproject.testing.utils.Wait;
import org.motechproject.testing.utils.WaitCondition;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.web.context.WebApplicationContext;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Test class that verifies the web security services
 * and dynamic security configuration. Stops and
 * starts the web security bundle and makes HTTP
 * requests with various credentials to test
 * different permutations of dynamic security.
 */
public class WebSecurityBundleIT extends BaseOsgiIT {
    private static final Integer TRIES_COUNT = 100;
    private static final String PERMISSION_NAME = "test-permission";
    private static final String ROLE_NAME = "test-role";
    private static final String USER_NAME = "test-username";
    private static final String USER_PASSWORD = "test-password";
    private static final String USER_EMAIL = "test@email.com";
    private static final String USER_EXTERNAL_ID = "test-externalId";
    private static final Locale USER_LOCALE = Locale.ENGLISH;
    private static final String SECURITY_BUNDLE_NAME = "motech-platform-web-security";
    private static final String QUERY_URL = "http://localhost:%d/websecurity/api/web-api/status";

    private PollingHttpClient httpClient = new PollingHttpClient(new DefaultHttpClient(), 60);

    public void testDynamicPermissionAccessSecurity() throws InterruptedException, IOException, BundleException {
        dynamicSecurity("motech", "motech", "addPermissionAccess", "stopBundle", null, HttpStatus.SC_OK);
        dynamicSecurity("motech", "motech", "addPermissionAccess", "fakeRole", null, HttpStatus.SC_FORBIDDEN);
        dynamicSecurity("motech2", "badpassword", "addPermissionAccess", "stopBundle", null, HttpStatus.SC_UNAUTHORIZED);
    }

    public void testDynamicUserAccessSecurity() throws InterruptedException, IOException, BundleException {
        dynamicSecurity("motech", "motech", "addUserAccess", "motech", null, HttpStatus.SC_OK);
        dynamicSecurity("motech", "motech", "addUserAccess", "not-motech", null, HttpStatus.SC_FORBIDDEN);
        dynamicSecurity("motech2", "badpassword", "addUserAccess", "motech", null, HttpStatus.SC_UNAUTHORIZED);
    }

    public void testMethodSpecificSecurity() throws InterruptedException, IOException, BundleException {
        dynamicSecurity("motech", "motech", "methodSpecific", "GET", "fakePermission", HttpStatus.SC_FORBIDDEN);
        dynamicSecurity("motech", "motech", "methodSpecific", "POST", "fakePermission", HttpStatus.SC_FORBIDDEN);
        dynamicSecurity("motech", "motech", "methodSpecific", "POST", "stopBundle", HttpStatus.SC_OK);
    }

    private void dynamicSecurity(String username, String password, String accessType, String accessValue, String accessValue2, int expectedResponseStatus) throws InterruptedException, IOException, BundleException {
        updateSecurity(SecurityTestConfigBuilder.buildConfig(accessType, accessValue, accessValue2));
        restartSecurityBundle();

        HttpUriRequest request = null;

        switch (accessValue) {
            case "POST" : request = new HttpPost(String.format(QUERY_URL, TestContext.getJettyPort())); break;
            default : request = new HttpGet(String.format(QUERY_URL, TestContext.getJettyPort()));
        }
        
        addAuthHeader(request, username, password);

        HttpResponse response = httpClient.execute(request);

        assertNotNull(response);
        assertEquals(expectedResponseStatus, response.getStatusLine().getStatusCode());
    }

    public void testWebSecurityServices() throws Exception {
        // given
        MotechPermissionService permissions = getService(MotechPermissionService.class);
        MotechRoleService roles = getService(MotechRoleService.class);
        MotechUserService users = getService(MotechUserService.class);

        PermissionDto permission = new PermissionDto(PERMISSION_NAME);
        RoleDto role = new RoleDto(ROLE_NAME, Arrays.asList(PERMISSION_NAME));

        // when
        permissions.addPermission(permission);
        roles.createRole(role);
        users.register(USER_NAME, USER_PASSWORD, USER_EMAIL, USER_EXTERNAL_ID, Arrays.asList(ROLE_NAME), USER_LOCALE);

        // then
        assertTrue(String.format("Permission %s has not been saved", PERMISSION_NAME), permissions.getPermissions().contains(permission));
        assertEquals(String.format("Role %s has not been saved properly", ROLE_NAME), role, roles.getRole(ROLE_NAME));
        assertNotNull(String.format("User %s has not been registered", USER_NAME), users.hasUser(USER_NAME));
        assertTrue(String.format("User doesn't have role %s", ROLE_NAME), users.getRoles(USER_NAME).contains(ROLE_NAME));
    }

    public void testProxyInitialization() throws Exception {
        WebApplicationContext theContext = getService(WebApplicationContext.class);
        MotechProxyManager manager = theContext.getBean(MotechProxyManager.class);
        FilterChainProxy proxy = manager.getFilterChainProxy();
        assertNotNull(proxy);
        assertNotNull(proxy.getFilterChains());
    }

    public void testUpdatingProxy() throws InterruptedException, BundleException, IOException, ClassNotFoundException {

        MotechSecurityConfiguration config = SecurityTestConfigBuilder.buildConfig("noSecurity", null, null);
        updateSecurity(config);

        restartSecurityBundle();

        MotechProxyManager manager = getProxyManager();
        assertTrue(manager.getFilterChainProxy().getFilterChains().size() == 1);

        MotechSecurityConfiguration updatedConfig = SecurityTestConfigBuilder.buildConfig("loginAccess", null, null);
        updateSecurity(updatedConfig);

        restartSecurityBundle();

        manager = getProxyManager();
        assertTrue(manager.getFilterChainProxy().getFilterChains().size() == 2);
    }

    private <T> T getService(Class<T> clazz) throws InterruptedException {
        T service = clazz.cast(bundleContext.getService(getServiceReference(clazz)));

        assertNotNull(String.format("Service %s is not available", clazz.getName()), service);

        return service;
    }

    private <T> ServiceReference getServiceReference(Class<T> clazz) throws InterruptedException {
        ServiceReference serviceReference;
        int tries = 0;

        do {
            serviceReference = bundleContext.getServiceReference(clazz.getName());
            ++tries;
            Thread.sleep(2000);
        } while (serviceReference == null && tries < TRIES_COUNT);

        assertNotNull(String.format("Not found service reference for %s", clazz.getName()), serviceReference);

        return serviceReference;
    }

    private void addAuthHeader(HttpUriRequest request, String userName, String password) {
        request.addHeader("Authorization", "Basic " + new String(Base64.encodeBase64((userName + ":" + password).getBytes())));
    }

    private Bundle getBundle(String symbolicName) {
        Bundle testBundle = null;
        for (Bundle bundle : bundleContext.getBundles()) {
            if (null != bundle.getSymbolicName() && bundle.getSymbolicName().contains(symbolicName)
                    && UNINSTALLED != bundle.getState()) {
                testBundle = bundle;
                break;
            }
        }
        assertNotNull(testBundle);
        return testBundle;
    }

    private void waitForBundleState(final Bundle bundle, final int state) throws InterruptedException {
        new Wait(new WaitCondition() {
            @Override
            public boolean needsToWait() {
                return state == bundle.getState();
            }
        }, 2000).start();
        assertEquals(state, bundle.getState());
    }

    private void updateSecurity(MotechSecurityConfiguration config) throws InterruptedException {
        WebApplicationContext theContext = getService(WebApplicationContext.class);
        AllMotechSecurityRules allSecurityRules = theContext.getBean(AllMotechSecurityRules.class);
        allSecurityRules.add(config);
    }
    
    private void deleteSecurityConfig() throws InterruptedException {
        WebApplicationContext theContext = getService(WebApplicationContext.class);
        AllMotechSecurityRules allSecurityRules = theContext.getBean(AllMotechSecurityRules.class);
        ((AllMotechSecurityRulesCouchdbImpl) allSecurityRules).removeAll();
    }

    private List<MotechURLSecurityRule> getRules() throws InterruptedException {
        WebApplicationContext theContext = getService(WebApplicationContext.class);
        AllMotechSecurityRules allSecurityRules = theContext.getBean(AllMotechSecurityRules.class);
        return allSecurityRules.getRules();
    }

    private MotechProxyManager getProxyManager() throws InterruptedException {
        WebApplicationContext theContext = getService(WebApplicationContext.class);
        return theContext.getBean(MotechProxyManager.class);
    }

    private void restartSecurityBundle() throws BundleException, InterruptedException, IOException {
        Bundle securityBundle = getBundle(SECURITY_BUNDLE_NAME);
        securityBundle.stop();
        waitForBundleState(securityBundle, RESOLVED);
        securityBundle.start();
        waitForBundleState(securityBundle, ACTIVE);
    }
    
    @After
    public void removeSecurityConfig() throws InterruptedException {
        deleteSecurityConfig();
    }
}
