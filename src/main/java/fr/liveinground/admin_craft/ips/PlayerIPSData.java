package fr.liveinground.admin_craft.ips;

import fr.liveinground.admin_craft.PlayerDataManager;

import java.util.List;

public class PlayerIPSData {
    public String uuid;
    public List<String> ips;

    public PlayerIPSData(String uuid, List<String> ips) {
        this.uuid = uuid;
        this.ips = ips;
    }
}
