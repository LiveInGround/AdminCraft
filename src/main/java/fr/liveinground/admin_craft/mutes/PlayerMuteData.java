package fr.liveinground.admin_craft.mutes;

public class PlayerMuteData {
    public String name;
    public String uuid;
    public String reason;

    public PlayerMuteData() {}

    public PlayerMuteData(String name, String uuid, String reason) {
        this.name = name;
        this.uuid = uuid;
        this.reason = reason;
    }
}
