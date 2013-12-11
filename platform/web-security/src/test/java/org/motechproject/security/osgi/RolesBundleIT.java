package org.motechproject.security.osgi;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.motechproject.security.model.PermissionDto;
import org.motechproject.security.model.RoleDto;
import org.motechproject.security.service.MotechPermissionService;
import org.motechproject.security.service.MotechRoleService;
import org.motechproject.security.service.MotechUserService;
import org.motechproject.testing.osgi.BaseOsgiIT;
import org.motechproject.testing.utils.PollingHttpClient;
import org.motechproject.testing.utils.TestContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static java.util.Arrays.asList;

public class RolesBundleIT extends BaseOsgiIT {


    private static final int TRIES_COUNT = 100;
    private PollingHttpClient httpClient = new PollingHttpClient(new DefaultHttpClient(), 60);
    private static final String BUNDLE_NAME = "bundle";

    private static final String PERMISSION_NAME = "test-permission";
    private static final String SOME_ROLE = "test-role";
    private static final String USER_NOT_AUTHORISED_TO_MANAGE_ROLES = "test-user-cannot-manage-roles";
    private static final String USER_PASSWORD = "test-password";
    private static final String USER_EXTERNAL_ID = "test-externalId";
    private static final Locale USER_LOCALE = Locale.ENGLISH;
    private static final String USER_AUTHORISED_TO_MANAGE_ROLES = "test-user-can-manage-roles";

    private static final String ROLES_ADMIN = "Roles Admin";

    public static final String MANAGE_ROLE = "manageRole";
    private static final String POST_DATA = "{\"roleName\":\"fooRole\",\"originalRoleName\":\"\",\"permissionNames\":[],\"deletable\":true}";


    public void testThatRoleThatAllowsRoleManagementIsPresent() throws InterruptedException {
        MotechRoleService motechRoleService = getService(MotechRoleService.class);
        RoleDto role = motechRoleService.getRole(ROLES_ADMIN);
        assertNotNull(role);
        assertTrue(role.getPermissionNames().contains(MANAGE_ROLE));
    }

    public void testThatAccessIsDeniedForUnAuthorisedUserTryingToViewRoles() throws Exception {
        HttpResponse httpResponse = get("http://localhost:%s/websecurity/api/web-api/roles", USER_NOT_AUTHORISED_TO_MANAGE_ROLES, USER_PASSWORD);
        assertEquals(HttpStatus.SC_FORBIDDEN, httpResponse.getStatusLine().getStatusCode());
    }

    public void testThatAccessIsDeniedForUnAuthorisedUserTryingToCreateRoles() throws Exception {
        HttpResponse httpResponse = post("http://localhost:%s/websecurity/api/web-api/roles/create", USER_NOT_AUTHORISED_TO_MANAGE_ROLES, USER_PASSWORD, POST_DATA);
        assertEquals(HttpStatus.SC_FORBIDDEN, httpResponse.getStatusLine().getStatusCode());
    }

    public void testThatAccessIsDeniedForUnAuthorisedUserTryingToUpdateRoles() throws Exception {
        HttpResponse httpResponse = post("http://localhost:%s/websecurity/api/web-api/roles/update", USER_NOT_AUTHORISED_TO_MANAGE_ROLES, USER_PASSWORD, POST_DATA);
        assertEquals(HttpStatus.SC_FORBIDDEN, httpResponse.getStatusLine().getStatusCode());
    }

    public void testThatAccessIsDeniedForUnAuthorisedUserTryingToDeleteRoles() throws Exception {
        HttpResponse httpResponse = post("http://localhost:%s/websecurity/api/web-api/roles/delete", USER_NOT_AUTHORISED_TO_MANAGE_ROLES, USER_PASSWORD, POST_DATA);
        assertEquals(HttpStatus.SC_FORBIDDEN, httpResponse.getStatusLine().getStatusCode());
    }

    public void testThatAuthorisedUserCanViewRoles() throws Exception {
        HttpResponse httpResponse = get("http://localhost:%s/websecurity/api/web-api/roles", USER_AUTHORISED_TO_MANAGE_ROLES, USER_PASSWORD);
        assertEquals(HttpStatus.SC_OK, httpResponse.getStatusLine().getStatusCode());
    }

    public void testThatAccessIsDeniedForUnAuthorisedUserTryingToViewPermissions() throws Exception {
        HttpResponse httpResponse = get("http://localhost:%s/websecurity/api/web-api/permissions", USER_NOT_AUTHORISED_TO_MANAGE_ROLES, USER_PASSWORD);
        assertEquals(HttpStatus.SC_FORBIDDEN, httpResponse.getStatusLine().getStatusCode());
    }

    public void testThatAuthorisedUserCanViewPermissions() throws Exception {
        HttpResponse httpResponse = get("http://localhost:%s/websecurity/api/web-api/permissions", USER_AUTHORISED_TO_MANAGE_ROLES, USER_PASSWORD);
        assertEquals(HttpStatus.SC_OK, httpResponse.getStatusLine().getStatusCode());
    }

    public void testThatAccessIsDeniedForUnAuthorisedUserTryingToCreatePermission() throws Exception {
        HttpResponse httpResponse = post("http://localhost:%s/websecurity/api/web-api/permissions/foo-permission", USER_NOT_AUTHORISED_TO_MANAGE_ROLES, USER_PASSWORD, "{}");
        assertEquals(HttpStatus.SC_FORBIDDEN, httpResponse.getStatusLine().getStatusCode());
    }

    public void testThatAccessIsDeniedForUnAuthorisedUserTryingToDeletePermission() throws Exception {
        HttpResponse httpResponse = delete("http://localhost:%s/websecurity/api/web-api/permissions/foo-permission", USER_NOT_AUTHORISED_TO_MANAGE_ROLES, USER_PASSWORD);
        assertEquals(HttpStatus.SC_FORBIDDEN, httpResponse.getStatusLine().getStatusCode());
    }


    @Override
    public void onSetUp() throws InterruptedException {
        MotechPermissionService permissions = getService(MotechPermissionService.class);
        MotechRoleService roles = getService(MotechRoleService.class);
        MotechUserService users = getService(MotechUserService.class);


        PermissionDto someOtherPermission = new PermissionDto(PERMISSION_NAME, BUNDLE_NAME);
        RoleDto someOtherRole = new RoleDto(SOME_ROLE, Arrays.asList(PERMISSION_NAME));

        // when
        permissions.addPermission(someOtherPermission);
        roles.createRole(someOtherRole);

        if (!users.hasUser(USER_AUTHORISED_TO_MANAGE_ROLES)) {
            users.register(USER_AUTHORISED_TO_MANAGE_ROLES, USER_PASSWORD, "test-user-can-manage-roles@mail.com", USER_EXTERNAL_ID, Arrays.asList(ROLES_ADMIN), USER_LOCALE);
        }
        if (!users.hasUser(USER_NOT_AUTHORISED_TO_MANAGE_ROLES)) {
            users.register(USER_NOT_AUTHORISED_TO_MANAGE_ROLES, USER_PASSWORD, "test-user-cannot-manage-roles@mail.com", USER_EXTERNAL_ID, Arrays.asList(SOME_ROLE), USER_LOCALE);
        }
    }


    private <T> T getService(Class<T> clazz) throws InterruptedException {
        ServiceReference serviceReference = getServiceReference(clazz);
        assertNotNull(serviceReference);
        return clazz.cast(bundleContext.getService(serviceReference));
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

    private HttpResponse get(String urlTemplate, String username, String password) throws Exception {
        String url = String.format(urlTemplate, TestContext.getJettyPort());
        System.out.println(url);
        HttpGet httpGet = new HttpGet(url);
        return request(httpGet, username, password);
    }

    private HttpResponse post(String urlTemplate, String username, String password, String postData) throws Exception {
        String url = String.format(urlTemplate, TestContext.getJettyPort());
        HttpPost httpPost = new HttpPost(url);
        StringEntity entity = new StringEntity(postData);
        httpPost.setEntity(entity);
        httpPost.setHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return request(httpPost, username, password);
    }

    private HttpResponse delete(String urlTemplate, String username, String password) throws Exception {
        String url = String.format(urlTemplate, TestContext.getJettyPort());
        System.out.println(url);
        HttpDelete httpDelete = new HttpDelete(url);
        return request(httpDelete, username, password);
    }

    private HttpResponse request(HttpUriRequest request, String username, String password) throws InterruptedException, IOException, BundleException {
        addAuthHeader(request, username, password);
        HttpResponse response = httpClient.execute(request);
        assertNotNull(response);
        return response;
    }

    private void addAuthHeader(HttpUriRequest request, String userName, String password) {
        request.addHeader("Authorization", "Basic " + new String(Base64.encodeBase64((userName + ":" + password).getBytes())));
    }

    @Override
    protected List<String> getImports() {
        return asList(
                "org.motechproject.security.domain", "org.motechproject.security.service", "org.motechproject.security.repository"
        );
    }

}
