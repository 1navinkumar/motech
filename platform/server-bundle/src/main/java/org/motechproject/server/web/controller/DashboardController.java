package org.motechproject.server.web.controller;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.motechproject.osgi.web.ModuleRegistrationData;
import org.motechproject.osgi.web.UIFrameworkService;
import org.motechproject.security.service.MotechRoleService;
import org.motechproject.security.service.MotechUserService;
import org.motechproject.server.startup.MotechPlatformState;
import org.motechproject.server.startup.StartupManager;
import org.motechproject.server.ui.LocaleSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.motechproject.osgi.web.UIFrameworkService.MODULES_WITHOUT_SUBMENU;
import static org.motechproject.osgi.web.UIFrameworkService.MODULES_WITH_SUBMENU;

@Controller
public class DashboardController {

    private StartupManager startupManager = StartupManager.getInstance();

    @Autowired
    private UIFrameworkService uiFrameworkService;

    @Autowired
    private LocaleSettings localeSettings;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private MotechUserService userService;

    @Autowired
    private MotechRoleService roleService;


    @RequestMapping({"/index", "/", "/home"})
    public ModelAndView index(@RequestParam(required = false) String moduleName, final HttpServletRequest request) {

        ModelAndView mav = null;

        // check if this is the first run
        if (startupManager.getPlatformState() == MotechPlatformState.NEED_CONFIG) {
            mav = new ModelAndView("redirect:startup.do");
        } else {
            mav = new ModelAndView("index");
            if (request.getUserPrincipal() != null) {
                mav.addObject("userName", request.getUserPrincipal().getName());
                mav.addObject("securityLaunch", true);
            } else {
                mav.addObject("securityLaunch", false);
                mav.addObject("userName", "Admin Mode");
            }
            if (StringUtils.isNotBlank(request.getSession().getServletContext().getContextPath()) && !"/".equals(request.getSession().getServletContext().getContextPath())) {
                mav.addObject("contextPath", request.getSession().getServletContext().getContextPath().substring(1) + "/");
            } else if (StringUtils.isBlank(request.getSession().getServletContext().getContextPath()) || "/".equals(request.getSession().getServletContext().getContextPath())) {
                mav.addObject("contextPath", "");
            }
            mav.addObject("uptime", getUptime(request));

            Map<String, Collection<ModuleRegistrationData>> modules = uiFrameworkService.getRegisteredModules();

            mav.addObject(MODULES_WITH_SUBMENU, modules.get(MODULES_WITH_SUBMENU));


            List<ModuleRegistrationData> allowedModules = filterPermittedModules(request, modules);
            mav.addObject(MODULES_WITHOUT_SUBMENU, allowedModules);

            if (moduleName != null) {
                ModuleRegistrationData currentModule = uiFrameworkService.getModuleData(moduleName);
                if (currentModule != null) {
                    mav.addObject("currentModule", currentModule);
                }
            }

            mav.addObject("pageLang", localeSettings.getUserLocale(request));
        }

        return mav;
    }

    private List<ModuleRegistrationData> filterPermittedModules(HttpServletRequest request, Map<String, Collection<ModuleRegistrationData>> modules) {
        Collection<ModuleRegistrationData> modulesWithoutSubmenu = modules.get(MODULES_WITHOUT_SUBMENU);
        List<ModuleRegistrationData> allowedModules = new ArrayList<>();

        for (ModuleRegistrationData registrationData : modulesWithoutSubmenu) {

            List<String> userRoles = userService.getUser(request.getUserPrincipal().getName()).getRoles();
            String requiredPermissionForAccess = registrationData.getRoleForAccess();

            if (requiredPermissionForAccess == null) {
                allowedModules.add(registrationData);
            } else {

                for (String userRole : userRoles) {
                    boolean userHasPermission = roleService.getRole(userRole).getPermissionNames().contains(requiredPermissionForAccess);
                    if (userHasPermission) {
                        allowedModules.add(registrationData);
                        break;
                    }
                }
            }

        }
        return allowedModules;
    }

    @RequestMapping(value = "/gettime", method = RequestMethod.POST)
    @ResponseBody
    public String getTime(HttpServletRequest request) {
        Locale locale = localeSettings.getUserLocale(request);
        DateTimeFormatter format = DateTimeFormat.forPattern("EEE MMM dd, h:mm a, z yyyy").withLocale(locale);
        return new DateTime().toString(format);
    }

    private String getUptime(HttpServletRequest request) {
        RuntimeMXBean mx = ManagementFactory.getRuntimeMXBean();
        Period uptime = new Duration(mx.getUptime()).toPeriod();
        Locale locale = localeSettings.getUserLocale(request);

        PeriodFormatter formatter = new PeriodFormatterBuilder()
                .appendDays()
                .appendSuffix(" " + messageSource.getMessage("day", null, locale), " " + messageSource.getMessage("days", null, locale))
                .appendSeparator(" " + messageSource.getMessage("and", null, locale) + " ")
                .appendHours()
                .appendSuffix(" " + messageSource.getMessage("hour", null, locale), " " + messageSource.getMessage("hours", null, locale))
                .appendSeparator(" " + messageSource.getMessage("and", null, locale) + " ")
                .appendMinutes()
                .appendSuffix(" " + messageSource.getMessage("minute", null, locale), " " + messageSource.getMessage("minutes", null, locale))
                .toFormatter();

        return formatter.print(uptime.normalizedStandard());
    }

}
