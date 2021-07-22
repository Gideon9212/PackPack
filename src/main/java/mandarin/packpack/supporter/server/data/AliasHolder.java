package mandarin.packpack.supporter.server.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import common.CommonStatic;
import common.io.json.JsonDecoder;
import common.io.json.JsonEncoder;
import common.pack.Identifier;
import common.util.lang.MultiLangCont;
import common.util.stage.Stage;
import common.util.unit.AbEnemy;
import common.util.unit.Enemy;
import common.util.unit.Form;
import common.util.unit.Unit;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;

import java.util.ArrayList;
import java.util.Map;

@SuppressWarnings({"unchecked", "CastCanBeRemovedNarrowingVariableType"})
public class AliasHolder {
    public enum TYPE {
        FORM,
        ENEMY,
        STAGE,
        UNSPECIFIED
    }

    public enum MODE {
        ADD,
        REMOVE,
        RESET,
        GET
    }

    public static final int SUCCESS = 0;
    public static final int ERR_NO_SUCH_ALIAS = -1;
    public static final int ERR_ALIAS_EXISTS = -2;
    public static final int ERR_WRONG_DATA = -3;

    public static final MultiLangCont<Form, ArrayList<String>> FALIAS = new MultiLangCont<>();
    public static final MultiLangCont<Enemy, ArrayList<String>> EALIAS = new MultiLangCont<>();
    public static final MultiLangCont<Stage, ArrayList<String>> SALIAS = new MultiLangCont<>();

    public static JsonObject jsonfy() {
        JsonObject result = new JsonObject();

        JsonObject formAlias = new JsonObject();
        JsonObject enemyAlias = new JsonObject();
        JsonObject stageAlias = new JsonObject();

        for(int i = 0; i < 4; i++) {
            Map<Form, ArrayList<String>> formMap = FALIAS.getMap(getLangCode(i));

            if(formMap != null && !formMap.isEmpty()) {
                JsonArray segment = new JsonArray();

                for(Form key : formMap.keySet()) {
                    ArrayList<String> alias = formMap.get(key);

                    if(alias == null || alias.isEmpty())
                        continue;

                    JsonArray arr = StaticStore.listToJsonString(alias);
                    JsonObject id = JsonEncoder.encode(key.unit.id).getAsJsonObject();

                    JsonObject container = new JsonObject();

                    container.add("val", arr);
                    container.add("key", id);
                    container.addProperty("fid", key.fid);

                    segment.add(container);
                }

                formAlias.add(getLangCode(i), segment);
            }

            Map<Enemy, ArrayList<String>> enemyMap = EALIAS.getMap(getLangCode(i));

            if(enemyMap != null && !enemyMap.isEmpty()) {
                JsonArray segment = new JsonArray();

                for(Enemy key : enemyMap.keySet()) {
                    ArrayList<String> alias = EALIAS.getCont(key);

                    if(alias == null || alias.isEmpty())
                        continue;

                    JsonArray arr = StaticStore.listToJsonString(alias);
                    JsonObject id = JsonEncoder.encode(key.id).getAsJsonObject();

                    JsonObject container = new JsonObject();

                    container.add("val", arr);
                    container.add("key", id);

                    segment.add(container);
                }

                enemyAlias.add(getLangCode(i), segment);
            }

            Map<Stage, ArrayList<String>> stageMap = SALIAS.getMap(getLangCode(i));

            if(stageMap != null && !stageMap.isEmpty()) {
                JsonArray segment = new JsonArray();

                for(Stage key : stageMap.keySet()) {
                    ArrayList<String> alias = SALIAS.getCont(key);

                    if(alias == null || alias.isEmpty())
                        continue;

                    JsonArray arr = StaticStore.listToJsonString(alias);
                    JsonObject id = JsonEncoder.encode(key.id).getAsJsonObject();

                    JsonObject container = new JsonObject();

                    container.add("val", arr);
                    container.add("key", id);

                    segment.add(container);
                }

                stageAlias.add(getLangCode(i), segment);
            }
        }

        result.add("form", formAlias);
        result.add("enemy", enemyAlias);
        result.add("stage", stageAlias);

        return result;
    }

    public static void parseJson(JsonObject obj) {
        if(obj.has("form")) {
            JsonObject formAlias = obj.getAsJsonObject("form");

            for(int i = 0; i < 4; i++) {
                if(formAlias.has(getLangCode(i))) {
                    JsonArray segment = formAlias.getAsJsonArray(getLangCode(i));

                    for(int j = 0; j < segment.size(); j++) {
                        JsonObject container = segment.get(j).getAsJsonObject();

                        if(container.has("key") && container.has("val") && container.has("fid")) {
                            Identifier<?> id = JsonDecoder.decode(container.get("key"), Identifier.class);

                            if(id.cls != Unit.class)
                                continue;

                            Unit u = Identifier.get((Identifier<Unit>) id);

                            if(u == null)
                                continue;

                            int fid = container.get("fid").getAsInt();

                            if(fid < 0 || fid >= u.forms.length)
                                continue;

                            Form f = u.forms[fid];

                            ArrayList<String> arr = StaticStore.jsonToListString(container.getAsJsonArray("val"));

                            FALIAS.put(getLangCode(i), f, arr);
                        }
                    }
                }
            }
        }

        if(obj.has("enemy")) {
            JsonObject enemyAlias = obj.getAsJsonObject("enemy");

            for(int i = 0; i < 4; i++) {
                if(enemyAlias.has(getLangCode(i))) {
                    JsonArray segment = enemyAlias.getAsJsonArray(getLangCode(i));

                    for(int j = 0; j < segment.size(); j++) {
                        JsonObject container = segment.get(j).getAsJsonObject();

                        if(container.has("key") && container.has("val")) {
                            Identifier<?> id = JsonDecoder.decode(container.get("key"), Identifier.class);

                            if(!id.cls.isAssignableFrom(AbEnemy.class))
                                continue;

                            AbEnemy ae = Identifier.get((Identifier<AbEnemy>) id);

                            if(!(ae instanceof Enemy))
                                continue;

                            ArrayList<String> arr = StaticStore.jsonToListString(container.getAsJsonArray("val"));

                            EALIAS.put(getLangCode(i), (Enemy) ae, arr);
                        }
                    }
                }
            }
        }

        if(obj.has("stage")) {
            JsonObject enemyAlias = obj.getAsJsonObject("stage");

            for(int i = 0; i < 4; i++) {
                if(enemyAlias.has(getLangCode(i))) {
                    JsonArray segment = enemyAlias.getAsJsonArray(getLangCode(i));

                    for(int j = 0; j < segment.size(); j++) {
                        JsonObject container = segment.get(j).getAsJsonObject();

                        if(container.has("key") && container.has("val")) {
                            Identifier<?> id = JsonDecoder.decode(container.get("key"), Identifier.class);

                            if(id.cls != Stage.class)
                                continue;

                            Stage s = Identifier.get((Identifier<Stage>) id);

                            ArrayList<String> arr = StaticStore.jsonToListString(container.getAsJsonArray("val"));

                            SALIAS.put(getLangCode(i), s, arr);
                        }
                    }
                }
            }
        }
    }

    public static ArrayList<String> getAlias(TYPE type, int lang, Object data) {
        ArrayList<String> aliases;

        switch (type) {
            case FORM:
                if(!(data instanceof Form)) {
                    return null;
                }

                Map<Form, ArrayList<String>> fMap = FALIAS.getMap(getLangCode(lang));

                aliases = fMap.get((Form) data);
                break;
            case ENEMY:
                if(!(data instanceof Enemy)) {
                    return null;
                }

                Map<Enemy, ArrayList<String>> eMap = EALIAS.getMap(getLangCode(lang));

                aliases = eMap.get((Enemy) data);

                break;
            case STAGE:
                if(!(data instanceof Stage)) {
                    return null;
                }

                Map<Stage, ArrayList<String>> sMap = SALIAS.getMap(getLangCode(lang));

                aliases = sMap.get((Stage) data);

                break;
            default:
                return null;
        }

        return aliases;
    }

    public static void resetAlias(TYPE type, int lang, Object data) {
        switch (type) {
            case FORM:
                if(!(data instanceof Form)) {
                    return;
                }

                FALIAS.put(getLangCode(lang), (Form) data, null);

                break;
            case ENEMY:
                if(!(data instanceof Enemy)) {
                    return;
                }

                EALIAS.put(getLangCode(lang), (Enemy) data, null);

                break;
            case STAGE:
                if(!(data instanceof Stage)) {
                    return;
                }

                SALIAS.put(getLangCode(lang), (Stage) data, null);

                break;
        }
    }

    public static int putAlias(TYPE type, int lang, Object data, String alias) {
        ArrayList<String> aliases;

        int oldConfig = CommonStatic.getConfig().lang;
        CommonStatic.getConfig().lang = lang;

        switch (type) {
            case FORM:
                if(!(data instanceof Form)) {
                    return ERR_WRONG_DATA;
                }

                aliases = FALIAS.getCont((Form) data);
                break;
            case ENEMY:
                if(!(data instanceof Enemy)) {
                    return ERR_WRONG_DATA;
                }

                aliases = EALIAS.getCont((Enemy) data);
                break;
            case STAGE:
                if(!(data instanceof Stage)) {
                    return ERR_WRONG_DATA;
                }

                aliases = SALIAS.getCont((Stage) data);
                break;
            default:
                return ERR_WRONG_DATA;
        }

        CommonStatic.getConfig().lang = oldConfig;

        if(aliases == null)
            aliases = new ArrayList<>();

        if(aliases.contains(alias))
            return ERR_ALIAS_EXISTS;

        aliases.add(alias);

        switch (type) {
            case FORM:
                FALIAS.put(getLangCode(lang), (Form) data, aliases);
                break;
            case ENEMY:
                EALIAS.put(getLangCode(lang), (Enemy) data, aliases);
                break;
            case STAGE:
                SALIAS.put(getLangCode(lang), (Stage) data, aliases);
                break;
        }

        return SUCCESS;
    }

    public static int removeAlias(TYPE type, int lang, Object data, String alias) {
        ArrayList<String> aliases;

        int oldConfig = CommonStatic.getConfig().lang;
        CommonStatic.getConfig().lang = lang;

        switch (type) {
            case FORM:
                if(!(data instanceof Form)) {
                    return ERR_WRONG_DATA;
                }

                aliases = FALIAS.getCont((Form) data);
                break;
            case ENEMY:
                if(!(data instanceof Enemy)) {
                    return ERR_WRONG_DATA;
                }

                aliases = EALIAS.getCont((Enemy) data);
                break;
            case STAGE:
                if(!(data instanceof Stage)) {
                    return ERR_WRONG_DATA;
                }

                aliases = SALIAS.getCont((Stage) data);
                break;
            default:
                return ERR_WRONG_DATA;
        }

        CommonStatic.getConfig().lang = oldConfig;

        if(aliases == null || !aliases.contains(alias))
            return ERR_NO_SUCH_ALIAS;

        aliases.remove(alias);

        switch (type) {
            case FORM:
                FALIAS.put(getLangCode(lang), (Form) data, aliases);
                break;
            case ENEMY:
                EALIAS.put(getLangCode(lang), (Enemy) data, aliases);
                break;
            case STAGE:
                SALIAS.put(getLangCode(lang), (Stage) data, aliases);
                break;
        }

        return SUCCESS;
    }

    public static int replaceAlias(TYPE type, int lang, Object data, String alias, String replace) {
        ArrayList<String> aliases;

        int oldConfig = CommonStatic.getConfig().lang;
        CommonStatic.getConfig().lang = lang;

        switch (type) {
            case FORM:
                if(!(data instanceof Form)) {
                    return ERR_WRONG_DATA;
                }

                aliases = FALIAS.getCont((Form) data);
                break;
            case ENEMY:
                if(!(data instanceof Enemy)) {
                    return ERR_WRONG_DATA;
                }

                aliases = EALIAS.getCont((Enemy) data);
                break;
            case STAGE:
                if(!(data instanceof Stage)) {
                    return ERR_WRONG_DATA;
                }

                aliases = SALIAS.getCont((Stage) data);
                break;
            default:
                return ERR_WRONG_DATA;
        }

        CommonStatic.getConfig().lang = oldConfig;

        if(aliases == null || !aliases.contains(alias))
            return ERR_NO_SUCH_ALIAS;

        if(aliases.contains(replace))
            return ERR_ALIAS_EXISTS;

        aliases.remove(alias);
        aliases.add(replace);

        switch (type) {
            case FORM:
                FALIAS.put(getLangCode(lang), (Form) data, aliases);
                break;
            case ENEMY:
                EALIAS.put(getLangCode(lang), (Enemy) data, aliases);
                break;
            case STAGE:
                SALIAS.put(getLangCode(lang), (Stage) data, aliases);
                break;
        }

        return SUCCESS;
    }

    public static String getLangCode(int lang) {
        switch (lang) {
            case LangID.ZH:
                return "zh";
            case LangID.KR:
                return "kr";
            case LangID.JP:
                return "jp";
            default:
                return "en";
        }
    }
}
