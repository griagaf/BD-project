package com.tacticaldistrict.command.common.web;

import com.tacticaldistrict.command.common.dto.ApiStatusResponse;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    @GetMapping("/status")
    public ApiStatusResponse status() {
        return new ApiStatusResponse("TACTICAL DISTRICT COMMAND", "READY", Instant.now());
    }
}

