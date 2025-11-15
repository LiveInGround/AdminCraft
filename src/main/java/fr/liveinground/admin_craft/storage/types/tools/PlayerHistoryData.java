package fr.liveinground.admin_craft.storage.types.tools;

import fr.liveinground.admin_craft.storage.types.sanction.SanctionData;

import java.util.List;

public class PlayerHistoryData {
    public String uuid;
    public List<SanctionData> sanctionList;

    public PlayerHistoryData(String playerUUID, List<SanctionData> sanctionList) {
        this.uuid = playerUUID;
        this.sanctionList = sanctionList;
    }
}
