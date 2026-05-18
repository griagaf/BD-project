package com.tacticaldistrict.command.report.service;

import com.tacticaldistrict.command.report.dto.SmartMissionReportDto;
import com.tacticaldistrict.command.report.dto.SmartMissionReportRequest;

public interface ReportService {

    SmartMissionReportDto generateSmartMissionReport(SmartMissionReportRequest request);

    byte[] exportCsv(SmartMissionReportRequest request);
}
