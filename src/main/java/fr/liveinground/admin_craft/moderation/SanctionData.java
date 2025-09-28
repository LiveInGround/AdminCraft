package fr.liveinground.admin_craft.moderation;

import org.jetbrains.annotations.Nullable;

import java.util.Date;

public class SanctionData {
    public Sanction sanctionType;
    public String reason;
    public Date date;
    @Nullable Date expiresOn;

    public SanctionData(Sanction type, String reason, Date date, @Nullable Date expiresOn) {
        this.sanctionType = type;
        this.reason = reason;
        this.date = date;
        this.expiresOn = expiresOn;
    }
}
