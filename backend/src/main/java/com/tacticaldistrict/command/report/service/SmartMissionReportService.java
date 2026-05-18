package com.tacticaldistrict.command.report.service;

import com.tacticaldistrict.command.report.dto.SmartMissionReportDto;
import com.tacticaldistrict.command.report.dto.SmartMissionReportRequest;
import com.tacticaldistrict.command.security.access.PermissionService;
import com.tacticaldistrict.command.user.service.UserContext;
import com.tacticaldistrict.command.user.service.UserContextProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SmartMissionReportService implements ReportService {

    private final UserContextProvider userContextProvider;
    private final PermissionService permissionService;
    private final ReportGenerator reportGenerator;
    private final ReportExportService reportExportService;

    @Override
    @Transactional(readOnly = true)
    public SmartMissionReportDto generateSmartMissionReport(SmartMissionReportRequest request) {
        UserContext user = userContextProvider.current();
        checkReportPermission(user);
        permissionService.checkRead(user, request.objectType().toObjectType(), request.objectId());
        return reportGenerator.generate(user, request);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportCsv(SmartMissionReportRequest request) {
        SmartMissionReportDto report = generateSmartMissionReport(request);
        return reportExportService.toCsv(report);
    }

    private void checkReportPermission(UserContext user) {
        if (!user.hasPermission("report:generate")) {
            throw new AccessDeniedException("Report generation is not allowed");
        }
    }
}
