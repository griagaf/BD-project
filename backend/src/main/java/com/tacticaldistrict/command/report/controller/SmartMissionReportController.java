package com.tacticaldistrict.command.report.controller;

import com.tacticaldistrict.command.report.dto.SmartMissionReportDto;
import com.tacticaldistrict.command.report.dto.SmartMissionReportRequest;
import com.tacticaldistrict.command.report.service.ReportService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports/smart-mission")
@RequiredArgsConstructor
public class SmartMissionReportController {

    private final ReportService reportService;

    @PostMapping("/generate")
    @PreAuthorize("@userContextProvider.current().hasPermission('report:generate')")
    public SmartMissionReportDto generate(@Valid @RequestBody SmartMissionReportRequest request) {
        return reportService.generateSmartMissionReport(request);
    }

    @PostMapping("/export.csv")
    @PreAuthorize("@userContextProvider.current().hasPermission('report:generate')")
    public ResponseEntity<byte[]> exportCsv(@Valid @RequestBody SmartMissionReportRequest request) {
        byte[] csv = reportService.exportCsv(request);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("smart-mission-report-" + LocalDate.now() + ".csv")
                        .build()
                        .toString())
                .body(csv);
    }
}
