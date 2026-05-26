# Readiness And Alert Architecture

## Readiness

Readiness вычисляется из агрегированных показателей:

- личный состав;
- техника;
- вооружение;
- специалисты;
- инфраструктура;
- активные предупреждения.

Application service собирает данные через `DashboardRepositoryPort`, calculator применяет правила и thresholds, API возвращает DTO для dashboard и reports.

## Alerts

Alert flow разделён на два слоя:

- `AlertRepositoryPort` описывает required data access;
- `AlertQueryRepository` реализует порт и получает кандидатов из БД;
- `AlertService` формирует `TacticalAlertDto`, action flow и применяет access filtering.

SQL не находится в alert service, а предметные правила остаются рядом с генерацией alert DTO.

## Сооружения

Сооружения с `assignable = false` не считаются пустыми для размещения подразделений, не ухудшают infrastructure readiness и не создают предупреждение “сооружение не используется”. Для них возвращается нейтральный статус: “Размещение подразделений не предусмотрено”.

## Thresholds

Порог перегрузки сооружения, превышения количества техники и вооружения оформлен именованными константами. Это исключает неочевидные magic numbers в логике генерации предупреждений.
