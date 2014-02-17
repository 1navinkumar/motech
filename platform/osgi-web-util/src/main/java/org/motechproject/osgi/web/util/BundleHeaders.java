package org.motechproject.osgi.web.util;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.util.Dictionary;

public class BundleHeaders {
    private final Dictionary headers;

    public BundleHeaders(BundleContext bundleContext) {
        this(bundleContext.getBundle());
    }

    public BundleHeaders(Bundle bundle) {
        headers = bundle.getHeaders();
    }

    public Object get(Object key) {
        return headers.get(key);
    }

    public String getContextPath() {
        return getStringValue("Context-Path");
    }

    public String getResourcePath() {
        return getStringValue("Resource-Path");
    }

    public String getSymbolicName() {
        return getStringValue("Bundle-SymbolicName");
    }

    public String getName() {
        return getStringValue("Bundle-Name");
    }

    public String getVersion() {
        return getStringValue("Bundle-Version");
    }

    public String getStringValue(String key) {
        return (String) get(key);
    }

    public boolean isBluePrintEnabled() {
        return Boolean.valueOf(getStringValue("Blueprint-Enabled"));
    }
}
