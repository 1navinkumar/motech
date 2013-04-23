package org.motechproject.scheduletracking.api.events;

import org.motechproject.model.MotechEvent;
import org.motechproject.scheduler.MotechSchedulerService;
import org.motechproject.scheduletracking.api.events.constants.EventDataKeys;
import org.motechproject.scheduletracking.api.events.constants.EventSubjects;

import java.util.HashMap;

public class DefaultmentCaptureEvent {
    private String jobId;
    private String enrollmentId;

    public DefaultmentCaptureEvent(String enrollmentId, String jobId) {
        this.jobId = jobId;
        this.enrollmentId = enrollmentId;
    }

    public DefaultmentCaptureEvent(MotechEvent event) {
        this.jobId = (String) event.getParameters().get(MotechSchedulerService.JOB_ID_KEY);
        this.enrollmentId = (String) event.getParameters().get(EventDataKeys.ENROLLMENT_ID);
    }

    public MotechEvent toMotechEvent() {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put(EventDataKeys.ENROLLMENT_ID, enrollmentId);
        data.put(MotechSchedulerService.JOB_ID_KEY, jobId);
        return new MotechEvent(EventSubjects.DEFAULTMENT_CAPTURE, data);
    }

    public String getEnrollmentId() {
        return enrollmentId;
    }

    public String getJobId() {
        return jobId;
    }
}