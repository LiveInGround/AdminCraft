package fr.liveinground.admin_craft.storage.types.reports;


import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.UUID;

public record ReportData(int id, ReportStatus status, UUID sourceUUID, UUID targetUUID, String reason, Date date, @Nullable UUID claimedUUID) {
}
