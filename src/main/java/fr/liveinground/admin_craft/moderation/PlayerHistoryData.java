package fr.liveinground.admin_craft.moderation;

import java.util.ArrayList;
import java.util.List;

public class PlayerHistoryData {
    public String uuid;
    public List<SanctionData> sanctionList = new ArrayList<>();

    public PlayerHistoryData(String playerUUID, List<SanctionData> sanctionList) {
        this.uuid = playerUUID;
        this.sanctionList = sanctionList;
    }
}
