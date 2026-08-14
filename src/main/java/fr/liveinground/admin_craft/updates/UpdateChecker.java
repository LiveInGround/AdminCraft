package fr.liveinground.admin_craft.updates;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import static fr.liveinground.admin_craft.AdminCraft.LOGGER;
import static fr.liveinground.admin_craft.AdminCraft._MCVERSION;
import static fr.liveinground.admin_craft.AdminCraft._MODLOADER;
import static fr.liveinground.admin_craft.AdminCraft._VERSION;

public class UpdateChecker {
    public static final String DOWNLOAD_LINK = "https://modrinth.com/mod/admincraft";
    private static final String _SLUG = "admincraft";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    public static final Version currentVersion = new Version(_VERSION, _MCVERSION, _MODLOADER, "No update check was done yet, could not retrieve changelogs.");

    public static VersionResult updateResult = new VersionResult(true, currentVersion);

    private static CompletableFuture<Version[]> retrieveUpdates() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "https://api.modrinth.com/v2/project/"
                                + _SLUG
                                + "/version"
                                + "?loaders=%5B%22"
                                + _MODLOADER
                                + "%22%5D&game_versions=%5B%22"
                                + _MCVERSION
                                + "%22%5D&featured=true"
                                + "&include_changelog=true"
                ))
                .GET()
                .header("User-Agent", "AdminCraft/" + _VERSION)
                .build();

        return CLIENT
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(UpdateChecker::parseVersions);
    }

    private static Version[] parseVersions(String json) {
        JsonArray versions = JsonParser.parseString(json).getAsJsonArray();

        Version[] result = new Version[versions.size()];
        for (int i = 0; i < versions.size(); i++) {
            JsonObject version = versions.get(i).getAsJsonObject();

            String versionNumber = version.get("version_number")
                    .getAsString();
            String mcVersion = version.getAsJsonArray("game_versions")
                    .get(0).getAsString();
            String modloader = version.getAsJsonArray("loaders")
                    .get(0).getAsString();
            String changelog = version.get("changelog").getAsString();

            result[i] = new Version(versionNumber, mcVersion, modloader, changelog);
        }

        return result;
    }

    public static void checkForUpdates() {
        LOGGER.info("Checking for updates...");
        retrieveUpdates().thenAccept(versions -> {
            Version highestVersion = currentVersion;
            boolean utd = true;
            for (Version v: versions) {
                if (v.higherThan(highestVersion)) {
                    highestVersion = v;
                    if (utd) utd = false;
                }
            }
            updateResult = new VersionResult(utd, highestVersion);
            LOGGER.info("Update checking finished.");
            if (utd) LOGGER.info("AdminCraft is UP-TO-DATE!");
            else {
                LOGGER.warn("------------------------------------------------------------------------");
                LOGGER.warn("                        NEW COMPATIBLE UPDATE");
                LOGGER.warn("  A new compatible version of AdminCraft has been published: {}", highestVersion.version());
                LOGGER.warn("  Please download it on modrinth: " + DOWNLOAD_LINK);
                LOGGER.warn("  Next version's changelogs:");
                LOGGER.warn(highestVersion.changelogs());
                LOGGER.warn("------------------------------------------------------------------------");

            }
        });
    }
}
