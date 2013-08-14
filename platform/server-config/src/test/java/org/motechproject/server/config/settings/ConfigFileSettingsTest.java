package org.motechproject.server.config.settings;

import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.Properties;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class ConfigFileSettingsTest {

    @Test
    public void shouldReturnUrlWithHttpProtocolIfProtocolNotPresentInitially() {
        Properties motechSettings = new Properties();
        motechSettings.put(MotechSettings.SERVER_URL_PROP,"some.url");
        ConfigFileSettings settings = new ConfigFileSettings(motechSettings, new Properties());
        assertThat(settings.getServerUrl(), Is.is("http://some.url"));
    }


    @Test
    public void blankUrlShouldBeReturnedAsBlank() {
        ConfigFileSettings settingsWithNullUrl = new ConfigFileSettings();
        assertThat(settingsWithNullUrl.getServerUrl(), nullValue());

        Properties motechSettings = new Properties();
        motechSettings.put(MotechSettings.SERVER_URL_PROP,"  ");

        ConfigFileSettings settingsWithEmptyUrl = new ConfigFileSettings(motechSettings,new Properties());
        assertThat(settingsWithEmptyUrl.getServerUrl(), Is.is(EMPTY));
    }


    @Test
    public void urlWithProtocolShouldBeReturnedWithoutModification() {
        Properties motechSettings = new Properties();
        motechSettings.put(MotechSettings.SERVER_URL_PROP,"ftp://some.other.url");

        ConfigFileSettings settingsRecord = new ConfigFileSettings(motechSettings,new Properties());
        assertThat(settingsRecord.getServerUrl(), Is.is("ftp://some.other.url"));
    }


}
