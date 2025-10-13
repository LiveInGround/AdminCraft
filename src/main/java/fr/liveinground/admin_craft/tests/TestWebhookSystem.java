package fr.liveinground.admin_craft.tests;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class TestWebhookSystem {
    public static void main(String[] args) {
        String webhook = args[0];

        try {
            URL url = new URL(webhook);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            String json = """
            {
              "username": "Report system",
              "avatar_url": "https://images-ext-1.discordapp.net/external/UinHcMDrxmO4hwH3wSq1EHAxDA2wZYrsdDQPmKqUHuE/https/cdn.discordapp.com/icons/1420500702811390014/38b64cae66eb4cdb642ff18432e8876b.png?format=webp&quality=lossless",
              "embeds": [
                {
                  "title": "A new report was issued by player",
                  "description": "<testPlayer01234> reported <testCheater01234>: 'a test reason'",
                  "color": 16711680,
                  "fields": [
                    { "name": "<testPlayer01234> health", "value": "<health> H.P.", "inline": false },
                    { "name": "<testCheater01234> health", "value": "<health> H.P.", "inline": false },

                    { "name": "<testPlayer01234> location", "value": "<Level>, <x>, <y>, <z>", "inline": false },
                    { "name": "<testCheater01234> location", "value": "<Level>, <x>, <y>, <z>", "inline": false },
                    { "name": "Distance", "value": "<Distance>", "inline": false }

                  ],
                  "footer": { "text": "AdminCraft - Report system" },
                  "timestamp": "%s"
                }
              ]
            }
            """.formatted(java.time.Instant.now().toString());

            try (OutputStream os = connection.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();
            System.out.println("Discord answer: " + responseCode);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
