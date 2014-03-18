package org.motechproject.admin.osgi;

import org.motechproject.osgi.web.ModuleRegistrationData;
import org.motechproject.osgi.web.UIFrameworkService;
import org.osgi.framework.Bundle;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class MockUIFrameworkService implements UIFrameworkService {

    @Override
    public void registerModule(ModuleRegistrationData module) {
    }

    @Override
    public void unregisterModule(String moduleName) {
    }

    @Override
    public Map<String, Collection<ModuleRegistrationData>> getRegisteredModules() {
        return Collections.EMPTY_MAP;
    }

    @Override
    public ModuleRegistrationData getModuleData(String moduleName) {
        return null;
    }

    @Override
    public ModuleRegistrationData getModuleDataByBundle(Bundle bundle) {
        return null;
    }

    @Override
    public boolean isModuleRegistered(String moduleName) {
        return false;
    }

    @Override
    public void moduleNeedsAttention(String moduleName, String message) {
    }

    @Override
    public void moduleBackToNormal(String moduleName) {
    }

    @Override
    public void moduleNeedsAttention(String moduleName, String message, String submenu) {
    }

    @Override
    public void moduleBackToNormal(String moduleName, String submenu) {
    }

    @Override
    public ModuleRegistrationData getModuleDataByAngular(String angularModule) {
        return null;
    }
}
