package mandarin.packpack.supporter.server.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import discord4j.common.util.Snowflake;
import mandarin.packpack.supporter.lang.LangID;

import java.util.*;

public class IDHolder {
    public static IDHolder jsonToIDHolder(JsonObject obj) {
        IDHolder id = new IDHolder();

        if(obj.has("server")) {
            id.serverPrefix = id.setOr(obj.get("server").getAsString());
        }

        if(obj.has("locale")) {
            id.serverLocale = obj.get("locale").getAsInt();
        }

        if (obj.has("publish")) {
            id.publish = obj.get("publish").getAsBoolean();
        }

        if(obj.has("mod")) {
            id.MOD = id.setOrNull(obj.get("mod").getAsString());
        }

        if(obj.has("mem")) {
            id.MEMBER = id.setOrNull(obj.get("mem").getAsString());
        }

        if(obj.has("acc")) {
            id.GET_ACCESS = id.setOrNull(obj.get("acc").getAsString());
        }

        if(obj.has("ann")) {
            id.ANNOUNCE = id.setOrNull(obj.get("ann").getAsString());
        }

        if(obj.has("bo")) {
            id.BOOSTER = id.setOrNull(obj.get("bo").getAsString());
        }

        if(obj.has("channel")) {
            id.channel = id.toMap(obj.getAsJsonObject("channel"));
        }

        if(obj.has("id")) {
            id.ID = id.toIDMap(obj.getAsJsonObject("id"));
        }

        if(obj.has("logDM")) {
            id.logDM = id.setOrNull(obj.get("logDM").getAsString());
        }

        if(obj.has("event")) {
            id.event = id.setOrNull(obj.get("event").getAsString());
        }

        if(obj.has("eventLocale")) {
            id.eventLocale = id.jsonObjectToListInteger(obj.getAsJsonArray("eventLocale"));
        }

        return id;
    }

    public String serverPrefix = "p!";
    public int serverLocale = LangID.EN;
    public boolean publish = false;
    public String logDM = null;
    public String event = null;

    public String MOD;
    public String MEMBER;
    public String BOOSTER;

    public String GET_ACCESS;
    public String ANNOUNCE;

    public Map<String, String> ID = new TreeMap<>();
    public Map<String, List<String>> channel = new TreeMap<>();
    public List<Integer> eventLocale = new ArrayList<>();

    public IDHolder(String m, String me, String bo, String acc) {
        this.MOD = m;
        this.MEMBER = me;
        this.GET_ACCESS = acc;
        this.BOOSTER = bo;
    }

    public IDHolder() {

    }

    public JsonObject jsonfy() {
        JsonObject obj = new JsonObject();

        obj.addProperty("server", getOrNull(serverPrefix));
        obj.addProperty("locale", serverLocale);
        obj.addProperty("publish", publish);
        obj.addProperty("mod", getOrNull(MOD));
        obj.addProperty("mem", getOrNull(MEMBER));
        obj.addProperty("acc", getOrNull(GET_ACCESS));
        obj.addProperty("ann", getOrNull(ANNOUNCE));
        obj.addProperty("bo", getOrNull(BOOSTER));
        obj.add("channel", jsonfyMap());
        obj.add("id", jsonfyIDs());
        obj.addProperty("logDM", getOrNull(logDM));
        obj.addProperty("event", getOrNull(event));
        obj.add("eventLocale", listIntegerToJsonObject(eventLocale));

        return obj;
    }

    public ArrayList<String> getAllAllowedChannels(Set<Snowflake> ids) {
        ArrayList<String> result = new ArrayList<>();

        for(Snowflake id : ids) {
            if(isSetAsRole(id.asString())) {
                List<String> channels = channel.get(id.asString());

                if(channels == null)
                    return null;

                result.addAll(channels);
            }
        }

        return result;
    }

    private boolean hasIDasRole(String id) {
        for(String i : ID.values()) {
            if(id.equals(i))
                return true;
        }

        return false;
    }

    private boolean isSetAsRole(String id) {
        return id.equals(MOD) || id.equals(MEMBER) || id.equals(BOOSTER) || hasIDasRole(id);
    }

    private String getOrNull(String id) {
        return id == null ? "null" : id;
    }

    private String setOrNull(String id) {
        return id.equals("null") ? null : id;
    }

    private String setOr(String id) {
        return id.equals("null") ? "p!" : id;
    }

    private JsonElement listStringToJsonObject(List<String> arr) {
        if(arr == null) {
            return JsonNull.INSTANCE;
        }

        JsonArray array = new JsonArray();

        for (String s : arr) {
            array.add(s);
        }

        return array;
    }

    private JsonElement listIntegerToJsonObject(List<Integer> arr) {
        if(arr == null) {
            return JsonNull.INSTANCE;
        }

        JsonArray array = new JsonArray();

        for(int i : arr) {
            array.add(i);
        }

        return array;
    }

    private List<String> jsonObjectToListString(JsonElement obj) {
        if(obj.isJsonArray()) {
            JsonArray ele = obj.getAsJsonArray();

            ArrayList<String> arr = new ArrayList<>();

            for(int i = 0; i < ele.size(); i++) {
                arr.add(ele.get(i).getAsString());
            }

            return arr;
        }

        return null;
    }

    private List<Integer> jsonObjectToListInteger(JsonElement obj) {
        if(obj.isJsonArray()) {
            JsonArray ele = obj.getAsJsonArray();

            List<Integer> arr = new ArrayList<>();

            for(int i = 0; i < ele.size(); i++) {
                arr.add(ele.get(i).getAsInt());
            }

            return arr;
        }

        return null;
    }

    private JsonObject jsonfyMap() {
        JsonObject obj = new JsonObject();

        Set<String> keys = channel.keySet();

        int i = 0;

        for(String key : keys) {
            List<String> arr = channel.get(key);

            if(arr == null)
                continue;

            JsonObject container = new JsonObject();

            container.addProperty("key", key);
            container.add("val" , listStringToJsonObject(arr));

            obj.add(Integer.toString(i), container);

            i++;
        }

        return obj;
    }

    private JsonObject jsonfyIDs() {
        JsonObject obj = new JsonObject();

        Set<String> keys = ID.keySet();

        int i = 0;

        for(String key : keys) {
            String id = ID.get(key);

            if(id == null)
                continue;

            JsonObject container = new JsonObject();

            container.addProperty("key", key);
            container.addProperty("val", id);

            obj.add(Integer.toString(i), container);

            i++;
        }

        return obj;
    }

    private TreeMap<String, List<String>> toMap(JsonObject obj) {
        TreeMap<String, List<String>> map = new TreeMap<>();

        int i = 0;

        while(true) {
            if(obj.has(Integer.toString(i))) {
                JsonObject container = obj.getAsJsonObject(Integer.toString(i));

                String key = container.get("key").getAsString();
                List<String> arr = jsonObjectToListString(container.get("val"));

                map.put(key, arr);

                i++;
            } else {
                break;
            }
        }

        return map;
    }

    private TreeMap<String, String> toIDMap(JsonObject obj) {
        TreeMap<String, String> map = new TreeMap<>();

        int i = 0;

        while(true) {
            if (obj.has(Integer.toString(i))) {
                JsonObject container = obj.getAsJsonObject(Integer.toString(i));

                String key = container.get("key").getAsString();
                String val = container.get("val").getAsString();

                map.put(key, val);

                i++;
            } else {
                break;
            }
        }

        return map;
    }

    @Override
    public String toString() {
        return "IDHolder{" +
                "serverPrefix='" + serverPrefix + '\'' +
                ", serverLocale=" + serverLocale +
                ", publish=" + publish +
                ", MOD='" + MOD + '\'' +
                ", MEMBER='" + MEMBER + '\'' +
                ", BOOSTER='" + BOOSTER + '\'' +
                ", GET_ACCESS='" + GET_ACCESS + '\'' +
                ", ANNOUNCE='" + ANNOUNCE + '\'' +
                ", ID=" + ID +
                ", channel=" + channel +
                '}';
    }
}