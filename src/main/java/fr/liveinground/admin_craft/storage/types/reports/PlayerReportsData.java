package fr.liveinground.admin_craft.storage.types.reports;

import java.util.List;

public record PlayerReportsData(String playerUUID, List<ReportData> reports) {
}
