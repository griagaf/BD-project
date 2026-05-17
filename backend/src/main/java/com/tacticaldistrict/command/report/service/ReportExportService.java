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
        StringBuilder csv = new StringBuilder("Раздел,Показатель,Значение\n");

        append(csv, "Объект", "Тип", report.object().type().name());
        append(csv, "Объект", "ID", report.object().id());
        append(csv, "Объект", "Название", report.object().name());
        append(csv, "Объект", "Родительский объект", report.object().parentName());
        append(csv, "Объект", "Статус", report.object().status());
        append(csv, "Объект", "Дислокация", report.object().location());
        append(csv, "Готовность", "Общая", report.readiness().overall());
        append(csv, "Готовность", "Личный состав", report.readiness().personnel());
        append(csv, "Готовность", "Техника", report.readiness().equipment());
        append(csv, "Готовность", "Вооружение", report.readiness().weapons());
        append(csv, "Готовность", "Специалисты", report.readiness().specialists());
        append(csv, "Готовность", "Инфраструктура", report.readiness().infrastructure());
        append(csv, "Готовность", "Статус", report.readiness().status());

        append(csv, "Личный состав", "Всего", report.personnel().total());
        append(csv, "Личный состав", "Офицеры", report.personnel().officers());
        append(csv, "Личный состав", "Сержанты и рядовые", report.personnel().enlisted());
        append(csv, "Личный состав", "Командиры", report.personnel().commanders());
        appendMap(csv, "Личный состав по званиям", report.personnel().byRank());
        appendMap(csv, "Личный состав по подразделениям", report.personnel().bySubdivision());

        append(csv, "Техника", "Общее количество", report.equipment().totalQuantity());
        append(csv, "Техника", "Типов техники", report.equipment().typesCount());
        append(csv, "Техника", "Части без техники", report.equipment().unitsWithoutEquipment());
        for (ResourceQuantityDto item : report.equipment().topEquipment()) {
            append(csv, "Позиция техники", item.typeName(), item.quantity() + " / " + item.categoryName());
        }

        append(csv, "Вооружение", "Общее количество", report.weapons().totalQuantity());
        append(csv, "Вооружение", "Типов вооружения", report.weapons().typesCount());
        append(csv, "Вооружение", "Части без вооружения", report.weapons().unitsWithoutWeapons());
        for (ResourceQuantityDto item : report.weapons().topWeapons()) {
            append(csv, "Позиция вооружения", item.typeName(), item.quantity() + " / " + item.categoryName());
        }

        append(csv, "Сооружения", "Всего", report.buildings().total());
        append(csv, "Сооружения", "Не используются", report.buildings().unused());
        append(csv, "Сооружения", "Перегружены", report.buildings().overloaded());
        for (BuildingUsageDto building : report.buildings().problemBuildings()) {
            append(csv, "Проблемное сооружение", building.buildingName(), building.subdivisionsCount() + " подразделений / " + building.unitName());
        }

        append(csv, "Специальности", "Всего", report.specialties().totalSpecialties());
        append(csv, "Специальности", "Закрыты", report.specialties().coveredSpecialties());
        append(csv, "Специальности", "Отсутствуют", report.specialties().missingSpecialties());
        appendMap(csv, "Ключевые специальности", report.specialties().topSpecialties());

        for (ReportCommanderDto commander : report.commanders()) {
            append(csv, "Командир", commander.position(), commander.fullName() + " / " + commander.rankName() + " / " + commander.objectName());
        }
        for (ReportAlertDto alert : report.alerts()) {
            append(csv, "Предупреждение " + alert.severity(), alert.type(), alert.title());
        }
        for (ReportRecommendationDto recommendation : report.recommendations()) {
            append(csv, "Рекомендация " + recommendation.severity().name(), recommendation.code(), recommendation.title());
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
