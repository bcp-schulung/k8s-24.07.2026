package de.bcpeducation.jokes.gateway.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final String instanceName;

    public DashboardController(
            @Value(
                    "${joke-gateway.instance-name:${HOSTNAME:local}}"
            )
            String instanceName
    ) {
        this.instanceName = instanceName;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute(
                "gatewayInstance",
                instanceName
        );

        return "dashboard";
    }
}
