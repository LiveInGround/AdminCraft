package fr.liveinground.admin_craft.storage.types.tools;

public class PlayerIPSData {
    public String name;
    public String uuid;
    public String ip;

    public PlayerIPSData(String name, String uuid, String ips) {
        this.name = name;
        this.uuid = uuid;
        this.ip = ips;
    }
}
