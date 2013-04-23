package org.motechproject.scheduletracking.api.it;

import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.motechproject.model.Time;
import org.motechproject.scheduler.MotechSchedulerService;
import org.motechproject.scheduletracking.api.repository.AllEnrollments;
import org.motechproject.scheduletracking.api.repository.AllSchedules;
import org.motechproject.scheduletracking.api.service.EnrollmentRequest;
import org.motechproject.scheduletracking.api.service.ScheduleTrackingService;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static junit.framework.Assert.assertEquals;
import static org.motechproject.testing.utils.TimeFaker.fakeNow;
import static org.motechproject.testing.utils.TimeFaker.stopFakingTime;
import static org.motechproject.util.DateUtil.newDate;
import static org.motechproject.util.DateUtil.newDateTime;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:testApplicationSchedulerTrackingAPI.xml")
public class SchedulingWithPreferredTimeIT {

    @Autowired
    private ScheduleTrackingService scheduleTrackingService;

    @Autowired
    MotechSchedulerService schedulerService;

    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;

    @Autowired
    private AllEnrollments allEnrollments;

    Scheduler scheduler;

    @Before
    public void setup() {
        scheduler = schedulerFactoryBean.getScheduler();
    }

    @After
    public void teardown() {
        schedulerService.unscheduleAllJobs("org.motechproject.scheduletracking");
        allEnrollments.removeAll();
    }

    @Test
    public void shouldScheduleAlertsAtPreferredAlertTime() throws SchedulerException, URISyntaxException, IOException {

        String enrollmentId = scheduleTrackingService.enroll(new EnrollmentRequest(
                "abcde",
                "schedule",
                new Time(8, 20),
                newDate(2050, 5, 10), new Time(0, 0),
                newDate(2050, 5, 10), new Time(0, 0),
                "milestone1",
                null));

        List<DateTime> fireTimes = getFireTimes(format("org.motechproject.scheduletracking.api.milestone.alert-%s.0-repeat", enrollmentId)) ;
        assertEquals(asList(
                newDateTime(2050, 5, 15, 8, 20, 0),
                newDateTime(2050, 5, 16, 8, 20, 0),
                newDateTime(2050, 5, 17, 8, 20, 0),
                newDateTime(2050, 5, 18, 8, 20, 0),
                newDateTime(2050, 5, 19, 8, 20, 0)),
                fireTimes);
    }

    @Test
    public void shouldNotScheduleAlertsInThePastForDelayedEnrollment() throws SchedulerException, URISyntaxException, IOException {

        try {
            fakeNow(newDateTime(2050, 5, 17, 9, 0, 0));

            String enrollmentId = scheduleTrackingService.enroll(new EnrollmentRequest(
                    "abcde",
                    "schedule",
                    new Time(10, 0),
                    newDate(2050, 5, 10), new Time(0, 0),
                    newDate(2050, 5, 17), new Time(0, 0),
                    "milestone1",
                    null));

            List<DateTime> fireTimes = getFireTimes(format("org.motechproject.scheduletracking.api.milestone.alert-%s.0-repeat", enrollmentId)) ;
            assertEquals(asList(
                    newDateTime(2050, 5, 17, 10, 0, 0),
                    newDateTime(2050, 5, 18, 10, 0, 0),
                    newDateTime(2050, 5, 19, 10, 0, 0)),
                    fireTimes);
        } finally {
            stopFakingTime();
        }
    }

    @Test
    public void shouldNotScheduleAlertForTheDayIfPreferredTimeIsPassed() throws SchedulerException, URISyntaxException, IOException {

        try {
            fakeNow(newDateTime(2050, 5, 17, 11, 0, 0));
                String enrollmentId = scheduleTrackingService.enroll(new EnrollmentRequest(
                    "abcde",
                    "schedule",
                    new Time(8, 0),
                    newDate(2050, 5, 10), new Time(0, 0),
                    newDate(2050, 5, 17), new Time(0, 0),
                    "milestone1",
                    null));

            List<DateTime> fireTimes = getFireTimes(format("org.motechproject.scheduletracking.api.milestone.alert-%s.0-repeat", enrollmentId)) ;
            assertEquals(asList(
                    newDateTime(2050, 5, 18, 8, 0, 0),
                    newDateTime(2050, 5, 19, 8, 0, 0)),
                    fireTimes);
        } finally {
            stopFakingTime();
        }
    }

    @Test
    public void shouldScheduleSecondMilestoneAlerts() throws IOException, URISyntaxException, SchedulerException {

        String enrollmentId = scheduleTrackingService.enroll(new EnrollmentRequest(
                "abcde",
                "schedule",
                new Time(10, 0),
                newDate(2050, 5, 10), new Time(0, 0),
                newDate(2050, 5, 10), new Time(0, 0),
                "milestone1",
                null));
        scheduleTrackingService.fulfillCurrentMilestone("abcde", "schedule", newDate(2050, 5, 20), new Time(9, 0));

        List<DateTime> fireTimes = getFireTimes(format("org.motechproject.scheduletracking.api.milestone.alert-%s.1-repeat", enrollmentId)) ;
        assertEquals(asList(
                newDateTime(2050, 5, 20, 10, 0, 0),
                newDateTime(2050, 5, 21, 10, 0, 0),
                newDateTime(2050, 5, 22, 10, 0, 0)),
                fireTimes);
    }


    private List<DateTime> getFireTimes(String key) throws SchedulerException {
        Trigger trigger = scheduler.getTrigger(key, "default");
        List<DateTime> fireTimes = new ArrayList<DateTime>();
        Date nextFireTime = trigger.getNextFireTime();
        while (nextFireTime != null) {
            fireTimes.add(newDateTime(nextFireTime));
            nextFireTime = trigger.getFireTimeAfter(nextFireTime);
        }
        return fireTimes;
    }
}