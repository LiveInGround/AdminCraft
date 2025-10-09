package fr.liveinground.admin_craft.storage.types.reports;


import java.util.Date;

public record ReportData(String targetUUID, String sourceUUID, String reason, Date date) {
}
