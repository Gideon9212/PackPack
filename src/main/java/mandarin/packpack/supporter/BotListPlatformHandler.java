package mandarin.packpack.supporter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BotListPlatformHandler {
    private static String topGGToken = null;
    private static String discordBotListToken = null;
    private static String koreanDiscordListToken = null;

    private static final String topGGDomain = "https://top.gg/api/";
    private static final String discordBotListDomain = "https://discordbotlist.com/api/v1/";
    private static final String koreanDiscordListDomain = "https://koreanbots.dev/api/v2/";

    public static void initialize() {
        File jsonFile = new File("./data/botListPlatformTokens.json");

        if (!jsonFile.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(jsonFile)) {
            JsonElement element = JsonParser.parseReader(reader);

            if (!element.isJsonObject())
                return;

            JsonObject tokenObject = element.getAsJsonObject();

            if (tokenObject.has("topGG")) {
                topGGToken = tokenObject.get("topGG").getAsString();
            }

            if (tokenObject.has("discordBotList")) {
                discordBotListToken = tokenObject.get("discordBotList").getAsString();
            }

            if (tokenObject.has("koreanDiscordList")) {
                koreanDiscordListToken = tokenObject.get("koreanDiscordList").getAsString();
            }
        } catch (IOException e) {
            StaticStore.logger.uploadErrorLog(e, "E/BotListPlatformHandler::initialize - Failed to open reader for json file");
        }
    }

    public static void handleUpdatingBotStatus(@Nonnull ShardManager manager) {
        String botID = getBotID(manager);

        if (botID == null)
            return;

        handleTopGG(manager, botID);
        handleDiscordBotList(manager, botID);
        handleKoreanDiscordList(manager, botID);
    }

    private static void handleTopGG(@Nonnull ShardManager manager, String botID) {
        if (topGGToken == null)
            return;

        String requestLink = topGGDomain + "bots/" + botID + "/stats";

        JsonObject obj = new JsonObject();

        obj.addProperty("server_count", getGuildNumbers(manager));

        JsonArray shards = new JsonArray();

        for (long guildNumber : getShardGuildNumbers(manager)) {
            shards.add(guildNumber);
        }

        obj.add("shards", shards);
        obj.addProperty("shard_count", getShardCount(manager));

        HttpPost post = new HttpPost(requestLink);

        post.addHeader("Authorization", topGGToken);
        post.addHeader("Content-Type", "application/json");
        post.setEntity(new StringEntity(obj.toString()));

        try (
                CloseableHttpClient httpClient = HttpClientBuilder.create().build();
                ClassicHttpResponse response = httpClient.executeOpen(null, post, null)
        ) {
            if (response.getCode() != 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
                    StringBuilder result = new StringBuilder();

                    String line;

                    while((line = reader.readLine()) != null) {
                        result.append(line).append("\n");
                    }

                    StaticStore.logger.uploadLog("W/BotListPlatformHandler::handleTopGG - Got non-200 code from top.gg\nStatus Code = %d\nReason Phrase = %s\nBody = %s".formatted(response.getCode(), response.getReasonPhrase(), result.toString()));
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/BotListPlatformHandler::handleTopGG - Failed to read body from response");
                }
            }
        } catch (IOException e) {
            StaticStore.logger.uploadErrorLog(e, "E/BotListPlatformHandler::handleTopGG - Failed to open http client");
        }
    }

    private static void handleDiscordBotList(@Nonnull ShardManager manager, String botID) {
        if (discordBotListToken == null)
            return;

        String requestLink = discordBotListDomain + "bots/" + botID + "/stats";

        long[] guildNumbers = getShardGuildNumbers(manager);

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            for (int i = 0; i < guildNumbers.length; i++) {
                HttpPost post = new HttpPost(requestLink);

                post.addHeader("Authorization", discordBotListToken);
                post.addHeader("Content-Type", "application/json");

                JsonObject bodyObject = new JsonObject();

                bodyObject.addProperty("guilds", guildNumbers[i]);
                bodyObject.addProperty("shard_id", i);

                post.setEntity(new StringEntity(bodyObject.toString()));

                try (
                        ClassicHttpResponse response = httpClient.executeOpen(null, post, null)
                ) {
                    if (response.getCode() != 200) {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
                            StringBuilder result = new StringBuilder();

                            String line;

                            while((line = reader.readLine()) != null) {
                                result.append(line).append("\n");
                            }

                            StaticStore.logger.uploadLog("W/BotListPlatformHandler::handleDiscordBotList - Got non-200 code from discordbotlist.com\nStatus Code = %d\nReason Phrase = %s\nBody = %s".formatted(response.getCode(), response.getReasonPhrase(), result.toString()));
                        } catch (Exception e) {
                            StaticStore.logger.uploadErrorLog(e, "E/BotListPlatformHandler::handleDiscordBotList - Failed to read body from response");
                        }
                    }
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/BotListPlatformHandler::handleDiscordBotList - Failed to execute http post");
                }
            }
        } catch (IOException e) {
            StaticStore.logger.uploadErrorLog(e, "E/BotListPlatformHandler::handleTopGG - Failed to open http client");
        }
    }

    private static void handleKoreanDiscordList(@Nonnull ShardManager manager, String botID) {
        if (koreanDiscordListToken == null)
            return;

        String requestLink = koreanDiscordListDomain + "bots/" + botID + "/stats";

        JsonObject bodyObject = new JsonObject();

        bodyObject.addProperty("servers", getGuildNumbers(manager));
        bodyObject.addProperty("shards", getShardCount(manager));

        HttpPost post = new HttpPost(requestLink);

        post.addHeader("Authorization", koreanDiscordListToken);
        post.addHeader("Content-Type", "application/json");

        post.setEntity(new StringEntity(bodyObject.toString()));

        try (
                CloseableHttpClient client = HttpClientBuilder.create().build();
                ClassicHttpResponse response = client.executeOpen(null, post, null)
        ) {
            if (response.getCode() != 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
                    StringBuilder result = new StringBuilder();

                    String line;

                    while((line = reader.readLine()) != null) {
                        result.append(line).append("\n");
                    }

                    StaticStore.logger.uploadLog("W/BotListPlatformHandler::handleKoreanDiscordList - Got non-200 code from koreanbots.com\nStatus Code = %d\nReason Phrase = %s\nBody = %s".formatted(response.getCode(), response.getReasonPhrase(), result.toString()));
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/BotListPlatformHandler::handleKoreanDiscordList - Failed to read body from response");
                }
            }
        } catch (IOException e) {
            StaticStore.logger.uploadErrorLog(e, "E/BotListPlatformHandler::handleKoreanDiscordList - Failed to open http client");
        }
    }

    private static long getGuildNumbers(@Nonnull ShardManager manager) {
        List<JDA> shardList = new ArrayList<>(manager.getShards());
        shardList.sort(Comparator.comparingInt(shard -> shard.getShardInfo().getShardId()));

        long totalSize = 0;

        for (JDA shard : shardList) {
            totalSize += shard.getGuilds().size();
        }

        return totalSize;
    }

    private static long[] getShardGuildNumbers(@Nonnull ShardManager manager) {
        List<JDA> shardList = new ArrayList<>(manager.getShards());
        shardList.sort(Comparator.comparingInt(shard -> shard.getShardInfo().getShardId()));

        long[] guildNumbers = new long[shardList.size()];

        for (int i = 0; i < shardList.size(); i++) {
            guildNumbers[i] = shardList.get(i).getGuilds().size();
        }

        return guildNumbers;
    }

    private static long getShardCount(@Nonnull ShardManager manager) {
        return manager.getShards().size();
    }

    private static String getBotID(@Nonnull ShardManager manager) {
        List<JDA> shards = manager.getShards();

        if (shards.isEmpty()) {
            return null;
        } else {
            return shards.getFirst().getSelfUser().getId();
        }
    }
}
