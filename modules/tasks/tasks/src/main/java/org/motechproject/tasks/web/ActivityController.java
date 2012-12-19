package org.motechproject.tasks.web;

import org.motechproject.tasks.domain.TaskActivity;
import org.motechproject.tasks.service.TaskActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class ActivityController {
    private TaskActivityService messageService;

    @Autowired
    public ActivityController(final TaskActivityService messageService) {
        this.messageService = messageService;
    }

    @RequestMapping(value = "/activity", method = RequestMethod.GET)
    @ResponseBody
    public List<TaskActivity> getAllActivities() {
        return messageService.getAllActivities();
    }
}
