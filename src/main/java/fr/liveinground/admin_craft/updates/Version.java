package fr.liveinground.admin_craft.updates;

public record Version(String version, String mc_version, String modloader, String changelogs) {
    public boolean higherThan(Version compare) {
        String[] self = version.split("\\.");
        String[] other = compare.version().split("\\.");

        if (self.length != 3 || other.length != 3) {
            throw new IllegalStateException("Version ID does not follow semantic versioning format");
        }

        for (int i = 0; i < 3; i++) {
            int selfPart = Integer.parseInt(self[i]);
            int otherPart = Integer.parseInt(other[i]);

            if (selfPart > otherPart) {
                return true;
            }

            if (selfPart < otherPart) {
                return false;
            }
        }

        return false; // identic versions
    }
}