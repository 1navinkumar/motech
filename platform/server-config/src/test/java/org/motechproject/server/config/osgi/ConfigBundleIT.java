package org.motechproject.server.config.osgi;

import org.junit.Test;
import org.motechproject.server.config.domain.MotechSettings;
import org.motechproject.config.service.ConfigurationService;
import org.motechproject.testing.osgi.BaseOsgiIT;
import org.osgi.framework.ServiceReference;
import org.springframework.test.context.ContextConfiguration;

import java.util.Arrays;
import java.util.List;

@ContextConfiguration(locations = {"classpath*:testApplicationPlatformConfig.xml"})
public class ConfigBundleIT extends BaseOsgiIT {

    @Test
    public void testConfigBundle() throws Exception {
        ServiceReference settingsReference = bundleContext.getServiceReference(ConfigurationService.class.getName());
        assertNotNull(settingsReference);
        ConfigurationService settings = (ConfigurationService) bundleContext.getService(settingsReference);
        assertNotNull(settings);
        settings.setPlatformSetting("jms.call.delay", "5000");

        final MotechSettings platformSettings = settings.getPlatformSettings();
        final String delay = platformSettings.getActivemqProperties().getProperty("jms.call.delay");
        assertEquals("5000", delay);
    }

    @Override
    protected List<String> getImports() {
        return Arrays.asList("org.motechproject.server.config", "org.motechproject.commons.couchdb.service",
                "org.motechproject.server.config.domain");
    }
}
