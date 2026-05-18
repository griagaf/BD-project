package com.tacticaldistrict.command.report.service;

import com.tacticaldistrict.command.report.dto.BuildingUsageDto;
import com.tacticaldistrict.command.report.dto.ReportAlertDto;
import com.tacticaldistrict.command.report.dto.ReportCommanderDto;
import com.tacticaldistrict.command.report.dto.ReportRecommendationDto;
import com.tacticaldistrict.command.report.dto.ResourceQuantityDto;
import com.tacticaldistrict.command.report.dto.SmartMissionReportDto;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ReportExportService {

    public byte[] toCsv(SmartMissionReportDto report) {
        StringBuilder csv = new StringBuilder("Section,Key,Value\n");

        append(csv, "Object", "Type", report.object().type().name());
        append(csv, "Object", "Id", report.object().id());
        append(csv, "Object", "Name", report.object().name());
        append(csv, "Object", "Parent", report.object().parentName());
        append(csv, "Object", "Status", report.object().status());
        append(csv, "Object", "Location", report.object().location());
        append(csv, "Readiness", "Overall", report.readiness().overall());
        append(csv, "Readiness", "Personnel", report.readiness().personnel());
        append(csv, "Readiness", "Equipment", report.readiness().equipment());
        append(csv, "Readiness", "Weapons", report.readiness().weapons());
        append(csv, "Readiness", "Specialists", report.readiness().specialists());
        append(csv, "Readiness", "Infrastructure", report.readiness().infrastructure());
        append(csv, "Readiness", "Status", report.readiness().status());

        append(csv, "Personnel", "Total", report.personnel().total());
        append(csv, "Personnel", "Officers", report.personnel().officers());
        append(csv, "Personnel", "Enlisted", report.personnel().enlisted());
        append(csv, "Personnel", "Commanders", report.personnel().commanders());
        appendMap(csv, "Personnel by rank", report.personnel().byRank());
        appendMap(csv, "Personnel by subdivision", report.personnel().bySubdivision());

        append(csv, "Equipment", "Total quantity", report.equipment().totalQuantity());
        append(csv, "Equipment", "Types count", report.equipment().typesCount());
        append(csv, "Equipment", "Units without equipment", report.equipment().unitsWithoutEquipment());
        for (ResourceQuantityDto item : report.equipment().topEquipment()) {
            append(csv, "Equipment item", item.typeName(), item.quantity() + " / " + item.categoryName());
        }

        append(csv, "Weapons", "Total quantity", report.weapons().totalQuantity());
        append(csv, "Weapons", "Types count", report.weapons().typesCount());
        append(csv, "Weapons", "Units without weapons", report.weapons().unitsWithoutWeapons());
        for (ResourceQuantityDto item : report.weapons().topWeapons()) {
            append(csv, "Weapon item", item.typeName(), item.quantity() + " / " + item.categoryName());
        }

        append(csv, "Buildings", "Total", report.buildings().total());
        append(csv, "Buildings", "Unused", report.buildings().unused());
        append(csv, "Buildings", "Overloaded", report.buildings().overloaded());
        for (BuildingUsageDto building : report.buildings().problemBuildings()) {
            append(csv, "Building issue", building.buildingName(), building.subdivisionsCount() + " subdivisions / " + building.unitName());
        }

        append(csv, "Specialties", "Total", report.specialties().totalSpecialties());
        append(csv, "Specialties", "Covered", report.specialties().coveredSpecialties());
        append(csv, "Specialties", "Missing", report.specialties().missingSpecialties());
        appendMap(csv, "Top specialties", report.specialties().topSpecialties());

        for (ReportCommanderDto commander : report.commanders()) {
            append(csv, "Commander", commander.position(), commander.fullName() + " / " + commander.rankName() + " / " + commander.objectName());
        }
        for (ReportAlertDto alert : report.alerts()) {
            append(csv, "Alert " + alert.severity(), alert.type(), alert.title());
        }
        for (ReportRecommendationDto recommendation : report.recommendations()) {
            append(csv, "Recommendation " + recommendation.severity().name(), recommendation.code(), recommendation.title());
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void appendMap(StringBuilder csv, String section, Map<String, Long> values) {
        for (Map.Entry<String, Long> entry : values.entrySet()) {
            append(csv, section, entry.getKey(), entry.getValue());
        }
    }

    private void append(StringBuilder csv, String section, String key, Object value) {
        csv.append(escape(section))
                .append(",")
                .append(escape(key))
                .append(",")
                .append(escape(value))
                .append("\n");
    }

    private String escape(Object value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.toString().replace("\"", "\"\"") + "\"";
    }
}
