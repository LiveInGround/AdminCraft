package fr.liveinground.admin_craft.mutes;

import org.jetbrains.annotations.Nullable;

import java.util.Date;

public class PlayerMuteData {
    public String name;
    public String uuid;
    public String reason;
    public Date expiresOn;

    public PlayerMuteData() {}

    public PlayerMuteData(String name, String uuid, String reason, @Nullable Date expiresOn) {
        this.name = name;
        this.uuid = uuid;
        this.reason = reason;
        this.expiresOn = expiresOn;
    }
}
