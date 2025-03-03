package mandarin.packpack.supporter.bc;

import common.CommonStatic;
import common.pack.UserProfile;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.MapColc;
import common.util.stage.SCDef;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import common.util.stage.info.DefStageInfo;
import common.util.unit.Combo;
import common.util.unit.Enemy;
import common.util.unit.Form;
import common.util.unit.Unit;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.KoreanSeparater;
import mandarin.packpack.supporter.server.data.AliasHolder;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class EntityFilter {
    private static final int TOLERANCE = 5;

    private static  final int[] storyChapterMonthly = {
            3, 4, 5, 6, 7, 8, 9
    };

    private static final int[] cycloneMonthly = {
            14, 15, 16, 39, 43, 66, 96, 122, 157
    };

    private static final int[] cycloneCatamin = {
            18, 19, 20, 21, 22, 23, 35, 36, 49
    };

    private static final String spaceRegex = "[\\-☆]";

    public static ArrayList<Form> findUnitWithName(String name, boolean trueForm, CommonStatic.Lang.Locale lang) {
        ArrayList<Form> res = new ArrayList<>();
        ArrayList<Form> clear = new ArrayList<>();

        for(Unit u : UserProfile.getBCData().units.getList()) {
            if(u == null)
                continue;

            for(Form f : u.forms) {
                boolean added = false;
                boolean cleared = false;

                for(CommonStatic.Lang.Locale locale : CommonStatic.Lang.supportedLanguage) {
                    String[] idFormats = {
                            Data.trio(u.id.id) + "-" + Data.trio(f.fid),
                            Data.trio(u.id.id) + " - " + Data.trio(f.fid),
                            u.id.id + "-" + f.fid,
                            u.id.id + " - " + f.fid,
                            Data.trio(u.id.id) + "-" + f.fid,
                            Data.trio(u.id.id) + " - " + f.fid,
                            u.id.id + "-" + Data.trio(f.fid),
                            u.id.id + " - " + Data.trio(f.fid)
                    };

                    for (int j = 0; j < idFormats.length; j++) {
                        if (idFormats[j].toLowerCase(Locale.ENGLISH).equals(name.toLowerCase(Locale.ENGLISH))) {
                            res.clear();

                            res.add(f);

                            return res;
                        } else if (idFormats[j].toLowerCase(Locale.ENGLISH).contains(name.toLowerCase(Locale.ENGLISH))) {
                            added = true;
                        }
                    }

                    //Special case
                    if (name.toLowerCase(Locale.ENGLISH).equals(Data.trio(u.id.id) + Data.trio(f.fid))) {
                        res.clear();

                        res.add(f);

                        return res;
                    }

                    String formName = null;

                    if(MultiLangCont.get(f, lang) != null) {
                        formName = StaticStore.safeMultiLangGet(f, locale);
                    }

                    if (formName == null || formName.isBlank()) {
                        formName = f.names.toString();

                        if (formName.isBlank())
                            formName = null;
                    }

                    if(formName != null && formName.replaceAll(spaceRegex, " ").toLowerCase(Locale.ENGLISH).contains(name.toLowerCase(Locale.ENGLISH))) {
                        added = true;
                    }

                    ArrayList<String> alias = AliasHolder.FALIAS.getCont(f, lang);

                    if(alias != null && !alias.isEmpty()) {
                        for(String a : alias) {
                            if(a.toLowerCase(Locale.ENGLISH).contains(name.toLowerCase(Locale.ENGLISH))) {
                                if(a.toLowerCase(Locale.ENGLISH).equals(name.toLowerCase(Locale.ENGLISH))) {
                                    cleared = true;
                                    added = true;

                                    break;
                                }

                                added = true;

                                break;
                            }
                        }

                        if(added)
                            break;
                    }
                }

                if(added) {
                    res.add(f);
                }

                if(cleared) {
                    clear.add(f);
                }
            }
        }

        if(!clear.isEmpty()) {
            if(trueForm) {
                ArrayList<Form> filtered = new ArrayList<>();

                for(int i = 0; i < clear.size(); i++) {
                    Form f = clear.get(i);

                    if(f.fid == 2 && !filtered.contains(f)) {
                        filtered.add(f);
                    } else if(f.fid != 2) {
                        Form finalForm = f.unit.forms[f.unit.forms.length - 1];

                        if(!filtered.contains(finalForm))
                            filtered.add(finalForm);
                    }
                }

                return filtered;
            }

            return clear;
        }

        if(res.isEmpty()) {
            ArrayList<Form> similar = new ArrayList<>();
            ArrayList<Integer> similarity = new ArrayList<>();

            name = name.toLowerCase(Locale.ENGLISH);

            if(lang == CommonStatic.Lang.Locale.KR)
                name = KoreanSeparater.separate(name);

            int sMin = 10;

            for(Unit u : UserProfile.getBCData().units.getList()) {
                if(u == null)
                    continue;

                for(Form f : u.forms) {
                    String fname = StaticStore.safeMultiLangGet(f, lang);

                    if(fname == null || fname.isBlank()) {
                        fname = f.names.toString();
                    }

                    boolean done = false;

                    if(!fname.isBlank()) {
                        fname = fname.toLowerCase(Locale.ENGLISH);

                        if(lang == CommonStatic.Lang.Locale.KR)
                            fname = KoreanSeparater.separate(fname);

                        int s = calculateOptimalDistance(name, fname);

                        if (s <= TOLERANCE) {
                            similarity.add(s);
                            similar.add(f);

                            sMin = Math.min(sMin, s);

                            done = true;
                        }
                    }

                    if(!done) {
                        ArrayList<String> alias = AliasHolder.FALIAS.getCont(f, lang);

                        if(alias != null && !alias.isEmpty()) {
                            for(String a : alias) {
                                boolean added = false;

                                a = a.toLowerCase(Locale.ENGLISH);

                                if(lang == CommonStatic.Lang.Locale.KR)
                                    a = KoreanSeparater.separate(a);

                                int s = calculateOptimalDistance(name, a);

                                if (s <= TOLERANCE) {
                                    similar.add(f);
                                    similarity.add(s);

                                    sMin = Math.min(sMin, s);

                                    added = true;
                                }

                                if(added)
                                    break;
                            }
                        }
                    }
                }
            }

            for(int i = 0; i < similar.size(); i++) {
                if(similarity.get(i) == sMin)
                    res.add(similar.get(i));
            }
        }

        if(trueForm) {
            ArrayList<Form> filtered = new ArrayList<>();

            for(int i = 0; i < res.size(); i++) {
                Form f = res.get(i);

                if(f.fid == 2 && !filtered.contains(f)) {
                    filtered.add(f);
                } else if(f.fid != 2) {
                    Form finalForm = f.unit.forms[f.unit.forms.length - 1];

                    if(!filtered.contains(finalForm))
                        filtered.add(finalForm);
                }
            }

            return filtered;
        }

        return res;
    }

    public static ArrayList<Enemy> findEnemyWithName(String name, CommonStatic.Lang.Locale lang) {
        ArrayList<Enemy> res = new ArrayList<>();
        ArrayList<Enemy> clear = new ArrayList<>();

        for(Enemy e : UserProfile.getBCData().enemies.getList()) {
            if(e == null)
                continue;

            if (name.toLowerCase(Locale.ENGLISH).equals(Data.trio(e.id.id).toLowerCase(Locale.ENGLISH))) {
                res.clear();

                res.add(e);

                return res;
            }

            boolean added = false;
            boolean cleared = false;

            for(CommonStatic.Lang.Locale locale : CommonStatic.Lang.supportedLanguage) {
                StringBuilder ename = new StringBuilder(Data.trio(e.id.id))
                        .append(" ");

                String enemyName = null;

                if(MultiLangCont.get(e, lang) != null) {
                    ename.append(StaticStore.safeMultiLangGet(e, locale));

                    enemyName = StaticStore.safeMultiLangGet(e, locale);
                }

                if(ename.toString().toLowerCase(Locale.ENGLISH).contains(name.toLowerCase(Locale.ENGLISH))) {
                    added = true;
                }

                if(enemyName != null && enemyName.replaceAll(spaceRegex, " ").toLowerCase(Locale.ENGLISH).contains(name.toLowerCase(Locale.ENGLISH))) {
                    added = true;
                }

                ArrayList<String> alias = AliasHolder.EALIAS.getCont(e, lang);

                if(alias != null && !alias.isEmpty()) {
                    for(String a : alias) {
                        if(a.toLowerCase(Locale.ENGLISH).contains(name.toLowerCase(Locale.ENGLISH))) {
                            if(a.toLowerCase(Locale.ENGLISH).equals(name.toLowerCase(Locale.ENGLISH))) {
                                added = true;
                                cleared = true;

                                break;
                            }

                            added = true;

                            break;
                        }
                    }

                    if(added)
                        break;
                }
            }

            if(added) {
                res.add(e);
            }

            if(cleared) {
                clear.add(e);
            }
        }

        if(!clear.isEmpty())
            return clear;

        if(res.isEmpty()) {
            ArrayList<Enemy> similar = new ArrayList<>();
            ArrayList<Integer> similarity = new ArrayList<>();

            name = name.toLowerCase(Locale.ENGLISH);

            int sMin = 10;

            if(lang == CommonStatic.Lang.Locale.KR)
                name = KoreanSeparater.separate(name);

            for(Enemy e : UserProfile.getBCData().enemies.getList()) {
                if(e == null)
                    continue;

                String ename = StaticStore.safeMultiLangGet(e, lang);

                if(ename == null || ename.isBlank())
                    ename = e.names.toString();

                boolean done = false;

                if(!ename.isBlank()) {
                    ename = ename.toLowerCase(Locale.ENGLISH);

                    if(lang == CommonStatic.Lang.Locale.KR)
                        ename = KoreanSeparater.separate(ename);

                    int s = calculateOptimalDistance(name, ename);

                    if (s <= TOLERANCE) {
                        similar.add(e);
                        similarity.add(s);

                        sMin = Math.min(sMin, s);

                        done = true;
                    }
                }

                if(!done) {
                    ArrayList<String> alias = AliasHolder.EALIAS.getCont(e, lang);

                    if(alias != null && !alias.isEmpty()) {
                        for(String a : alias) {
                            boolean added = false;

                            a = a.toLowerCase(Locale.ENGLISH);

                            if(lang == CommonStatic.Lang.Locale.KR)
                                a = KoreanSeparater.separate(a);

                            int s = calculateOptimalDistance(name, a);

                            if (s <= TOLERANCE) {
                                similar.add(e);
                                similarity.add(s);

                                sMin = Math.min(sMin, s);

                                added = true;
                            }

                            if(added)
                                break;
                        }
                    }
                }
            }

            if(similar.isEmpty())
                return similar;

            ArrayList<Enemy> finalResult = new ArrayList<>();

            for(int i = 0; i < similar.size(); i++) {
                if(similarity.get(i) == sMin)
                    finalResult.add(similar.get(i));
            }

            return finalResult;
        } else {
            return res;
        }
    }

    @SuppressWarnings("DataFlowIssue")
    public static ArrayList<Stage> findStageWithName(String[] names, CommonStatic.Lang.Locale lang) {
        ArrayList<Stage> result = new ArrayList<>();
        ArrayList<Stage> clear = new ArrayList<>();

        if(searchMapColc(names) && names[0] != null && !names[0].isBlank()) {
            //Search normally
            for(MapColc mc : MapColc.values()) {
                if(mc == null)
                    continue;

                for(CommonStatic.Lang.Locale locale : CommonStatic.Lang.supportedLanguage) {
                    String mcName = StaticStore.safeMultiLangGet(mc, locale);

                    if(mcName == null || mcName.isBlank())
                        continue;

                    boolean s0 = mcName.toLowerCase(Locale.ENGLISH).contains(names[0].toLowerCase(Locale.ENGLISH));

                    if(!s0) {
                        mcName = mcName.replace("-", " ");

                        if(mcName.toLowerCase(Locale.ENGLISH).contains(names[0].toLowerCase(Locale.ENGLISH))) {
                            s0 = true;
                        }
                    }

                    if (!s0) {
                        mcName = mcName.replace("\\.", "");

                        if(mcName.toLowerCase(Locale.ENGLISH).contains(names[0].toLowerCase(Locale.ENGLISH))) {
                            s0 = true;
                        }
                    }

                    if(s0) {
                        for(StageMap stm : mc.maps.getList()) {
                            if(stm == null)
                                continue;

                            for(Stage st : stm.list.getList()) {
                                if(st == null)
                                    continue;

                                result.add(st);
                            }
                        }

                        break;
                    }
                }
            }

            //Start autocorrect mode if no map colc found
            if(result.isEmpty()) {
                ArrayList<Integer> distances = getDistances(MapColc.values(), names[0], lang, MapColc.class);

                int disMin = Integer.MAX_VALUE;

                for(int d : distances) {
                    if(d != -1) {
                        disMin = Math.min(d, disMin);
                    }
                }

                if(disMin <= 5) {
                    int i = 0;

                    for(MapColc mc : MapColc.values()) {
                        if(distances.get(i) == disMin) {
                            for(StageMap stm : mc.maps.getList()) {
                                result.addAll(stm.list.getList());
                            }
                        }

                        i++;
                    }
                }
            }
        } else if(searchStageMap(names) && names[1] != null && !names[1].isBlank()) {
            boolean mcContain;

            if(names[0] != null && !names[0].isBlank())
                mcContain = needToPerformContainMode(MapColc.values(), names[0], MapColc.class);
            else
                mcContain = true;

            boolean stmContain = false;

            for(MapColc mc : MapColc.values()) {
                if(mc == null)
                    continue;

                stmContain |= needToPerformContainMode(mc.maps.getList(), names[1], StageMap.class);
            }

            if(mcContain) {
                for(MapColc mc : MapColc.values()) {
                    if(mc == null)
                        continue;

                    boolean s0 = false;

                    if(names[0] != null && !names[0].isBlank()) {
                        for(CommonStatic.Lang.Locale locale : CommonStatic.Lang.supportedLanguage) {
                            String mcName = StaticStore.safeMultiLangGet(mc, locale);

                            if(mcName == null || mcName.isBlank())
                                continue;

                            if(mcName.toLowerCase(Locale.ENGLISH).contains(names[0].toLowerCase(Locale.ENGLISH))) {
                                s0 = true;
                                break;
                            }

                            mcName = mcName.replace("-", " ");

                            if(mcName.toLowerCase(Locale.ENGLISH).contains(names[0].toLowerCase(Locale.ENGLISH))) {
                                s0 = true;
                                break;
                            }
                        }
                    } else {
                        s0 = true;
                    }

                    if(!s0)
                        continue;

                    if(stmContain) {
                        for(StageMap stm : mc.maps.getList()) {
                            if(stm == null)
                                continue;

                            boolean s1 = false;

                            for(CommonStatic.Lang.Locale locale : CommonStatic.Lang.supportedLanguage) {
                                String stmName = StaticStore.safeMultiLangGet(stm, locale);

                                if(stmName == null || stmName.isBlank())
                                    continue;

                                if(stmName.toLowerCase(Locale.ENGLISH).contains(names[1].toLowerCase(Locale.ENGLISH))) {
                                    s1 = true;
                                    break;
                                }

                                stmName = stmName.replace("-", " ");

                                if(stmName.toLowerCase(Locale.ENGLISH).contains(names[1].toLowerCase(Locale.ENGLISH))) {
                                    s1 = true;
                                    break;
                                }
                            }

                            if(s1) {
                                result.addAll(stm.list.getList());
                            }
                        }
                    } else {
                        ArrayList<Integer> stmDistances = getDistances(mc.maps.getList(), names[1], lang, StageMap.class);

                        int disMin = Integer.MAX_VALUE;

                        for(int d : stmDistances)
                            if(d != -1)
                                disMin = Math.min(d, disMin);

                        if(disMin <= 5) {
                            int i = 0;

                            for(StageMap stm : mc.maps.getList()) {
                                if(stmDistances.get(i) == disMin) {
                                    result.addAll(stm.list.getList());
                                }

                                i++;
                            }
                        }
                    }
                }
            } else {
                ArrayList<Integer> mcDistances = getDistances(MapColc.values(), names[0], lang, MapColc.class);

                int mcMin = Integer.MAX_VALUE;

                for(int d : mcDistances)
                    if(d != -1)
                        mcMin = Math.min(d, mcMin);

                if(mcMin <= 5) {
                    int i = 0;

                    for(MapColc mc : MapColc.values()) {
                        if(mcDistances.get(i) == mcMin) {
                            if(stmContain) {
                                for(StageMap stm : mc.maps.getList()) {
                                    if(stm == null)
                                        continue;

                                    boolean s1 = false;

                                    for(CommonStatic.Lang.Locale locale : CommonStatic.Lang.supportedLanguage) {
                                        String stmName = StaticStore.safeMultiLangGet(stm, locale);

                                        if(stmName == null || stmName.isBlank())
                                            continue;

                                        if(stmName.toLowerCase(Locale.ENGLISH).contains(names[1].toLowerCase(Locale.ENGLISH))) {
                                            s1 = true;
                                            break;
                                        }

                                        stmName = stmName.replace("-", " ");

                                        if(stmName.toLowerCase(Locale.ENGLISH).contains(names[1].toLowerCase(Locale.ENGLISH))) {
                                            s1 = true;
                                            break;
                                        }
                                    }

                                    if(s1) {
                                        result.addAll(stm.list.getList());
                                    }
                                }
                            } else {
                                ArrayList<Integer> stmDistances = getDistances(mc.maps.getList(), names[1], lang, StageMap.class);

                                int stmMin = Integer.MAX_VALUE;

                                for(int d : stmDistances)
                                    if(d != -1)
                                        stmMin = Math.min(d, stmMin);

                                if(stmMin <= 3) {
                                    int j = 0;

                                    for(StageMap stm : mc.maps.getList()) {
                                        if(stmDistances.get(j) == stmMin) {
                                            result.addAll(stm.list.getList());
                                        }

                                        j++;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if(names[2] != null && !names[2].isBlank()) {
            boolean mcContain;

            if(names[0] != null && !names[0].isBlank()) {
                mcContain = needToPerformContainMode(MapColc.values(), names[0], MapColc.class);
            } else {
                mcContain = true;
            }

            boolean stmContain = false;

            if(names[1] != null && !names[1].isBlank()) {
                for(MapColc mc : MapColc.values()) {
                    if(mc == null)
                        continue;

                    stmContain |= needToPerformContainMode(mc.maps.getList(), names[1], StageMap.class);
                }
            } else {
                stmContain = true;
            }

            boolean stContain = false;

            for(MapColc mc : MapColc.values()) {
                if(mc == null)
                    continue;

                for(StageMap stm : mc.maps.getList()) {
                    if(stm == null)
                        continue;

                    stContain |= needToPerformContainMode(stm.list.getList(), names[2], Stage.class);
                }
            }

            if(mcContain) {
                for(MapColc mc : MapColc.values()) {
                    if(mc == null)
                        continue;

                    boolean s0 = false;

                    if(names[0] != null && !names[0].isBlank()) {
                        for(CommonStatic.Lang.Locale locale : CommonStatic.Lang.supportedLanguage) {
                            String mcName = StaticStore.safeMultiLangGet(mc, locale);

                            if(mcName == null || mcName.isBlank())
                                continue;

                            if(mcName.toLowerCase(Locale.ENGLISH).contains(names[0].toLowerCase(Locale.ENGLISH))) {
                                s0 = true;
                                break;
                            }

                            mcName = mcName.replace("-", " ");

                            if(mcName.toLowerCase(Locale.ENGLISH).contains(names[0].toLowerCase(Locale.ENGLISH))) {
                                s0 = true;
                                break;
                            }
                        }
                    } else {
                        s0 = true;
                    }

                    if(!s0)
                        continue;

                    if(stmContain) {
                        for(StageMap stm : mc.maps.getList()) {
                            if(stm == null)
                                continue;

                            boolean s1 = false;

                            if(names[1] != null && !names[1].isBlank()) {
                                for(CommonStatic.Lang.Locale locale : CommonStatic.Lang.supportedLanguage) {
                                    String stmName = StaticStore.safeMultiLangGet(stm, locale);

                                    if(stmName == null || stmName.isBlank())
                                        continue;

                                    if(stmName.toLowerCase(Locale.ENGLISH).contains(names[1].toLowerCase(Locale.ENGLISH))) {
                                        s1 = true;
                                        break;
                                    }

                                    stmName = stmName.replace("-", " ");

                                    if(stmName.toLowerCase(Locale.ENGLISH).contains(names[1].toLowerCase(Locale.ENGLISH))) {
                                        s1 = true;
                                        break;
                                    }
                                }
                            } else {
                                s1 = true;
                            }

                            if(!s1)
                                continue;

                            if(stContain) {
                                for(Stage st : stm.list.getList()) {
                                    if(st == null)
                                        continue;

                                    boolean s2 = false;
                                    boolean cleared = false;

                                    for(CommonStatic.Lang.Locale locale : CommonStatic.Lang.supportedLanguage) {
                                        String stName = StaticStore.safeMultiLangGet(st, locale);

                                        if(stName != null && !stName.isBlank()) {
                                            if(stName.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                s2 = true;
                                            }

                                            stName = stName.replace("-", " ");

                                            if(stName.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                s2 = true;
                                            }
                                        }

                                        ArrayList<String> alias = AliasHolder.SALIAS.getCont(st, locale);

                                        if(alias != null && !alias.isEmpty()) {
                                            for(String a : alias) {
                                                if(a.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                    if(a.toLowerCase(Locale.ENGLISH).equals(names[2].toLowerCase(Locale.ENGLISH))) {
                                                        cleared = true;
                                                    }

                                                    s2 = true;

                                                    break;
                                                }
                                            }

                                            if(s2)
                                                break;
                                        }
                                    }

                                    String[] ids = {
                                            mc.getSID()+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                            mc.getSID()+"-"+stm.id.id+"-"+st.id.id,
                                            DataToString.getMapCode(mc)+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                            DataToString.getMapCode(mc)+"-"+stm.id.id+"-"+st.id.id
                                    };

                                    boolean s3 = false;

                                    for(String id : ids) {
                                        if(id.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                            s3 = true;
                                            break;
                                        }
                                    }

                                    if(s2 || s3) {
                                        result.add(st);
                                    }

                                    if(cleared) {
                                        clear.add(st);
                                    }
                                }
                            } else {
                                ArrayList<Integer> stDistances = getDistances(stm.list.getList(), names[2], lang, Stage.class);

                                int stMin = Integer.MAX_VALUE;

                                for(int d : stDistances)
                                    if(d != -1)
                                        stMin = Math.min(stMin, d);

                                if(stMin <= 3) {
                                    int stInd = 0;

                                    for(Stage st : stm.list.getList()) {
                                        if(stDistances.get(stInd) == stMin) {
                                            result.add(st);
                                        }

                                        stInd++;
                                    }
                                } else {
                                    for(Stage st : stm.list.getList()) {
                                        String[] ids = {
                                                mc.getSID()+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                                mc.getSID()+"-"+stm.id.id+"-"+st.id.id,
                                                DataToString.getMapCode(mc)+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                                DataToString.getMapCode(mc)+"-"+stm.id.id+"-"+st.id.id
                                        };

                                        boolean s3 = false;

                                        for(String id : ids) {
                                            if(id.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                s3 = true;
                                                break;
                                            }
                                        }

                                        if(s3)
                                            result.add(st);
                                    }
                                }
                            }
                        }
                    } else {
                        ArrayList<Integer> stmDistances = getDistances(mc.maps.getList(), names[1], lang, StageMap.class);

                        int stmMin = Integer.MAX_VALUE;

                        for(int d : stmDistances)
                            if(d != -1)
                                stmMin = Math.min(stmMin, d);

                        if(stmMin <= 3) {
                            int stmInd = 0;

                            for(StageMap stm : mc.maps.getList()) {
                                if(stmDistances.get(stmInd) == stmMin) {
                                    if(stContain) {
                                        for(Stage st : stm.list.getList()) {
                                            if(st == null)
                                                continue;

                                            boolean s2 = false;
                                            boolean cleared = false;

                                            for(CommonStatic.Lang.Locale locale : CommonStatic.Lang.supportedLanguage) {
                                                String stName = StaticStore.safeMultiLangGet(st, locale);

                                                if(stName != null && !stName.isBlank()) {
                                                    if(stName.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                        s2 = true;
                                                    }

                                                    stName = stName.replace("-", " ");

                                                    if(stName.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                        s2 = true;
                                                    }
                                                }

                                                ArrayList<String> alias = AliasHolder.SALIAS.getCont(st, locale);

                                                if(alias != null && !alias.isEmpty()) {
                                                    for(String a : alias) {
                                                        if(a.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                            if(a.toLowerCase(Locale.ENGLISH).equals(names[2].toLowerCase(Locale.ENGLISH))) {
                                                                cleared = true;
                                                            }

                                                            s2 = true;

                                                            break;
                                                        }
                                                    }

                                                    if(s2)
                                                        break;
                                                }
                                            }

                                            String[] ids = {
                                                    mc.getSID()+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                                    mc.getSID()+"-"+stm.id.id+"-"+st.id.id,
                                                    DataToString.getMapCode(mc)+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                                    DataToString.getMapCode(mc)+"-"+stm.id.id+"-"+st.id.id
                                            };

                                            boolean s3 = false;

                                            for(String id : ids) {
                                                if(id.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                    s3 = true;
                                                    break;
                                                }
                                            }

                                            if(s2 || s3) {
                                                result.add(st);
                                            }

                                            if(cleared) {
                                                clear.add(st);
                                            }
                                        }
                                    } else {
                                        ArrayList<Integer> stDistances = getDistances(stm.list.getList(), names[2], lang, Stage.class);

                                        int stMin = Integer.MAX_VALUE;

                                        for(int d : stDistances)
                                            if(d != -1)
                                                stMin = Math.min(stMin, d);

                                        if(stMin <= 3) {
                                            int stInd = 0;

                                            for(Stage st : stm.list.getList()) {
                                                if(stDistances.get(stInd) == stMin) {
                                                    result.add(st);
                                                }

                                                stInd++;
                                            }
                                        } else {
                                            for(Stage st : stm.list.getList()) {
                                                String[] ids = {
                                                        mc.getSID()+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                                        mc.getSID()+"-"+stm.id.id+"-"+st.id.id,
                                                        DataToString.getMapCode(mc)+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                                        DataToString.getMapCode(mc)+"-"+stm.id.id+"-"+st.id.id
                                                };

                                                boolean s3 = false;

                                                for(String id : ids) {
                                                    if(id.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                        s3 = true;
                                                        break;
                                                    }
                                                }

                                                if(s3)
                                                    result.add(st);
                                            }
                                        }
                                    }
                                }

                                stmInd++;
                            }
                        }
                    }
                }
            } else {
                ArrayList<Integer> mcDistances = getDistances(MapColc.values(), names[0], lang, MapColc.class);

                int mcMin = Integer.MAX_VALUE;

                for(int d : mcDistances)
                    if(d != -1)
                        mcMin = Math.min(d, mcMin);

                if(mcMin <= 5) {
                    int mcInd = 0;

                    for(MapColc mc : MapColc.values()) {
                        if(mcDistances.get(mcInd) == mcMin) {
                            if(stmContain) {
                                for(StageMap stm : mc.maps.getList()) {
                                    if(stm == null)
                                        continue;

                                    boolean s1 = false;

                                    if(names[1] != null && !names[1].isBlank()) {
                                        for(CommonStatic.Lang.Locale locale : CommonStatic.Lang.supportedLanguage) {
                                            String stmName = StaticStore.safeMultiLangGet(stm, locale);

                                            if(stmName == null || stmName.isBlank())
                                                continue;

                                            if(stmName.toLowerCase(Locale.ENGLISH).contains(names[1].toLowerCase(Locale.ENGLISH))) {
                                                s1 = true;
                                                break;
                                            }

                                            stmName = stmName.replace("-", " ");

                                            if(stmName.toLowerCase(Locale.ENGLISH).contains(names[1].toLowerCase(Locale.ENGLISH))) {
                                                s1 = true;
                                                break;
                                            }
                                        }
                                    } else {
                                        s1 = true;
                                    }

                                    if(!s1)
                                        continue;

                                    if(stContain) {
                                        for(Stage st : stm.list.getList()) {
                                            if(st == null)
                                                continue;

                                            boolean s2 = false;
                                            boolean cleared = false;

                                            for(CommonStatic.Lang.Locale locale : CommonStatic.Lang.supportedLanguage) {
                                                String stName = StaticStore.safeMultiLangGet(st, locale);

                                                if(stName != null && !stName.isBlank()) {
                                                    if(stName.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                        s2 = true;
                                                    }

                                                    stName = stName.replace("-", " ");

                                                    if(stName.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                        s2 = true;
                                                    }
                                                }

                                                ArrayList<String> alias = AliasHolder.SALIAS.getCont(st, locale);

                                                if(alias != null && !alias.isEmpty()) {
                                                    for(String a : alias) {
                                                        if(a.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                            if(a.toLowerCase(Locale.ENGLISH).equals(names[2].toLowerCase(Locale.ENGLISH))) {
                                                                cleared = true;
                                                            }

                                                            s2 = true;

                                                            break;
                                                        }
                                                    }

                                                    if(s2)
                                                        break;
                                                }
                                            }

                                            String[] ids = {
                                                    mc.getSID()+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                                    mc.getSID()+"-"+stm.id.id+"-"+st.id.id,
                                                    DataToString.getMapCode(mc)+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                                    DataToString.getMapCode(mc)+"-"+stm.id.id+"-"+st.id.id
                                            };

                                            boolean s3 = false;

                                            for(String id : ids) {
                                                if(id.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                    s3 = true;
                                                    break;
                                                }
                                            }

                                            if(s2 || s3) {
                                                result.add(st);
                                            }

                                            if(cleared) {
                                                clear.add(st);
                                            }
                                        }
                                    } else {
                                        ArrayList<Integer> stDistances = getDistances(stm.list.getList(), names[2], lang, Stage.class);

                                        int stMin = Integer.MAX_VALUE;

                                        for(int d : stDistances)
                                            if(d != -1)
                                                stMin = Math.min(stMin, d);

                                        if(stMin <= 3) {
                                            int stInd = 0;

                                            for(Stage st : stm.list.getList()) {
                                                if(stDistances.get(stInd) == stMin) {
                                                    result.add(st);
                                                }

                                                stInd++;
                                            }
                                        } else {
                                            for(Stage st : stm.list.getList()) {
                                                String[] ids = {
                                                        mc.getSID()+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                                        mc.getSID()+"-"+stm.id.id+"-"+st.id.id,
                                                        DataToString.getMapCode(mc)+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                                        DataToString.getMapCode(mc)+"-"+stm.id.id+"-"+st.id.id
                                                };

                                                boolean s3 = false;

                                                for(String id : ids) {
                                                    if(id.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                        s3 = true;
                                                        break;
                                                    }
                                                }

                                                if(s3)
                                                    result.add(st);
                                            }
                                        }
                                    }
                                }
                            } else {
                                ArrayList<Integer> stmDistances = getDistances(mc.maps.getList(), names[1], lang, StageMap.class);

                                int stmMin = Integer.MAX_VALUE;

                                for(int d : stmDistances)
                                    if(d != -1)
                                        stmMin = Math.min(stmMin, d);

                                if(stmMin <= 3) {
                                    int stmInd = 0;

                                    for(StageMap stm : mc.maps.getList()) {
                                        if(stmDistances.get(stmInd) == stmMin) {
                                            if(stContain) {
                                                for(Stage st : stm.list.getList()) {
                                                    if(st == null)
                                                        continue;

                                                    boolean s2 = false;
                                                    boolean cleared = false;

                                                    for(CommonStatic.Lang.Locale locale : CommonStatic.Lang.supportedLanguage) {
                                                        String stName = StaticStore.safeMultiLangGet(st, locale);

                                                        if(stName != null && !stName.isBlank()) {
                                                            if(stName.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                                s2 = true;
                                                            }

                                                            stName = stName.replace("-", " ");

                                                            if(stName.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                                s2 = true;
                                                            }
                                                        }

                                                        ArrayList<String> alias = AliasHolder.SALIAS.getCont(st, locale);

                                                        if(alias != null && !alias.isEmpty()) {
                                                            for(String a : alias) {
                                                                if(a.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                                    if(a.toLowerCase(Locale.ENGLISH).equals(names[2].toLowerCase(Locale.ENGLISH))) {
                                                                        cleared = true;
                                                                    }

                                                                    s2 = true;

                                                                    break;
                                                                }
                                                            }

                                                            if(s2)
                                                                break;
                                                        }
                                                    }

                                                    String[] ids = {
                                                            mc.getSID()+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                                            mc.getSID()+"-"+stm.id.id+"-"+st.id.id,
                                                            DataToString.getMapCode(mc)+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                                            DataToString.getMapCode(mc)+"-"+stm.id.id+"-"+st.id.id
                                                    };

                                                    boolean s3 = false;

                                                    for(String id : ids) {
                                                        if(id.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                            s3 = true;
                                                            break;
                                                        }
                                                    }

                                                    if(s2 || s3) {
                                                        result.add(st);
                                                    }

                                                    if(cleared) {
                                                        clear.add(st);
                                                    }
                                                }
                                            } else {
                                                ArrayList<Integer> stDistances = getDistances(stm.list.getList(), names[2], lang, Stage.class);

                                                int stMin = Integer.MAX_VALUE;

                                                for(int d : stDistances)
                                                    if(d != -1)
                                                        stMin = Math.min(stMin, d);

                                                if(stMin <= 3) {
                                                    int stInd = 0;

                                                    for(Stage st : stm.list.getList()) {
                                                        if(stDistances.get(stInd) == stMin) {
                                                            result.add(st);
                                                        }
                                                        stInd++;
                                                    }
                                                } else {
                                                    for(Stage st : stm.list.getList()) {
                                                        String[] ids = {
                                                                mc.getSID()+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                                                mc.getSID()+"-"+stm.id.id+"-"+st.id.id,
                                                                DataToString.getMapCode(mc)+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                                                DataToString.getMapCode(mc)+"-"+stm.id.id+"-"+st.id.id
                                                        };

                                                        boolean s3 = false;

                                                        for(String id : ids) {
                                                            if(id.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                                s3 = true;
                                                                break;
                                                            }
                                                        }

                                                        if(s3)
                                                            result.add(st);
                                                    }
                                                }
                                            }
                                        }

                                        stmInd++;
                                    }
                                }
                            }
                        }

                        mcInd++;
                    }
                }
            }
        }

        if(!clear.isEmpty()) {
            return clear;
        } else {
            return result;
        }
    }

    public static ArrayList<StageMap> findStageMapWithName(String[] names, CommonStatic.Lang.Locale lang) {
        ArrayList<StageMap> result = new ArrayList<>();

        if(searchMapColc(names) && names[0] != null && !names[0].isBlank()) {
            //Search normally
            for(MapColc mc : MapColc.values()) {
                if(mc == null)
                    continue;

                for(CommonStatic.Lang.Locale locale : CommonStatic.Lang.supportedLanguage) {
                    String mcName = StaticStore.safeMultiLangGet(mc, locale);

                    if(mcName == null || mcName.isBlank())
                        continue;

                    boolean s0 = mcName.toLowerCase(Locale.ENGLISH).contains(names[0].toLowerCase(Locale.ENGLISH));

                    if(!s0) {
                        mcName = mcName.replace("-", " ");

                        if(mcName.toLowerCase(Locale.ENGLISH).contains(names[0].toLowerCase(Locale.ENGLISH))) {
                            s0 = true;
                        }
                    }

                    if (!s0) {
                        mcName = mcName.replace("\\.", "");

                        if(mcName.toLowerCase(Locale.ENGLISH).contains(names[0].toLowerCase(Locale.ENGLISH))) {
                            s0 = true;
                        }
                    }

                    if(s0) {
                        for(StageMap stm : mc.maps.getList()) {
                            if(stm == null)
                                continue;

                            result.add(stm);
                        }

                        break;
                    }
                }
            }

            //Start autocorrect mode if no map colc found
            if(result.isEmpty()) {
                ArrayList<Integer> distances = getDistances(MapColc.values(), names[0], lang, MapColc.class);

                int disMin = Integer.MAX_VALUE;

                for(int d : distances) {
                    if(d != -1) {
                        disMin = Math.min(d, disMin);
                    }
                }

                if(disMin <= 5) {
                    int i = 0;

                    for(MapColc mc : MapColc.values()) {
                        if(distances.get(i) == disMin) {
                            result.addAll(mc.maps.getList());
                        }

                        i++;
                    }
                }
            }
        } else {
            boolean mcContain;

            if (names[0] != null && !names[0].isBlank())
                mcContain = needToPerformContainMode(MapColc.values(), names[0], MapColc.class);
            else
                mcContain = true;

            boolean stmContain = false;

            for (MapColc mc : MapColc.values()) {
                if (mc == null)
                    continue;

                stmContain |= needToPerformContainMode(mc.maps.getList(), names[1], StageMap.class);
            }

            if (mcContain) {
                for (MapColc mc : MapColc.values()) {
                    if (mc == null)
                        continue;

                    boolean s0 = false;

                    if (names[0] != null && !names[0].isBlank()) {
                        for (CommonStatic.Lang.Locale locale : CommonStatic.Lang.supportedLanguage) {
                            String mcName = StaticStore.safeMultiLangGet(mc, locale);

                            if (mcName == null || mcName.isBlank())
                                continue;

                            if (mcName.toLowerCase(Locale.ENGLISH).contains(names[0].toLowerCase(Locale.ENGLISH))) {
                                s0 = true;
                                break;
                            }

                            mcName = mcName.replace("-", " ");

                            if (mcName.toLowerCase(Locale.ENGLISH).contains(names[0].toLowerCase(Locale.ENGLISH))) {
                                s0 = true;
                                break;
                            }
                        }
                    } else {
                        s0 = true;
                    }

                    if (!s0)
                        continue;

                    if (stmContain) {
                        for (StageMap stm : mc.maps.getList()) {
                            if (stm == null)
                                continue;

                            result.add(stm);
                        }
                    } else {
                        ArrayList<Integer> stmDistances = getDistances(mc.maps.getList(), names[1], lang, StageMap.class);

                        int disMin = Integer.MAX_VALUE;

                        for (int d : stmDistances)
                            if (d != -1)
                                disMin = Math.min(d, disMin);

                        if (disMin <= 5) {
                            int i = 0;

                            for (StageMap stm : mc.maps.getList()) {
                                if (stmDistances.get(i) == disMin) {
                                    result.add(stm);
                                }

                                i++;
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    public static ArrayList<MapColc> findMapCollectionWithName(String name, CommonStatic.Lang.Locale lang) {
        ArrayList<MapColc> result = new ArrayList<>();

        if(name != null && !name.isBlank()) {
            //Search normally
            for (MapColc mc : MapColc.values()) {
                if (mc == null)
                    continue;

                for (CommonStatic.Lang.Locale locale : CommonStatic.Lang.supportedLanguage) {
                    String mcName = StaticStore.safeMultiLangGet(mc, locale);

                    if (mcName == null || mcName.isBlank())
                        continue;

                    boolean s0 = mcName.toLowerCase(Locale.ENGLISH).contains(name.toLowerCase(Locale.ENGLISH));

                    if (!s0) {
                        mcName = mcName.replace("-", " ");

                        if (mcName.toLowerCase(Locale.ENGLISH).contains(name.toLowerCase(Locale.ENGLISH))) {
                            s0 = true;
                        }
                    }

                    if (!s0) {
                        mcName = mcName.replace("\\.", "");

                        if (mcName.toLowerCase(Locale.ENGLISH).contains(name.toLowerCase(Locale.ENGLISH))) {
                            s0 = true;
                        }
                    }

                    if (s0) {
                        result.add(mc);

                        break;
                    }
                }
            }

            //Start autocorrect mode if no map colc found
            if (result.isEmpty()) {
                ArrayList<Integer> distances = getDistances(MapColc.values(), name, lang, MapColc.class);

                int disMin = Integer.MAX_VALUE;

                for (int d : distances) {
                    if (d != -1) {
                        disMin = Math.min(d, disMin);
                    }
                }

                if (disMin <= 5) {
                    int i = 0;

                    for (MapColc mc : MapColc.values()) {
                        if (distances.get(i) == disMin) {
                            result.add(mc);
                        }

                        i++;
                    }
                }
            }
        }

        return result;
    }

    public static ArrayList<Stage> findStageWithMapName(String name) {
        ArrayList<Stage> stmResult = new ArrayList<>();

        for(MapColc mc : MapColc.values()) {
            if(mc == null)
                continue;

            for(StageMap stm : mc.maps.getList()) {
                if(stm == null)
                    continue;

                boolean s1 = false;

                for(CommonStatic.Lang.Locale locale : CommonStatic.Lang.supportedLanguage) {
                    String stmName = StaticStore.safeMultiLangGet(stm, locale);

                    if(stmName == null || stmName.isBlank())
                        continue;

                    if(stmName.toLowerCase(Locale.ENGLISH).contains(name.toLowerCase(Locale.ENGLISH))) {
                        s1 = true;
                        break;
                    }
                }

                if(s1) {
                    stmResult.addAll(stm.list.getList());
                }
            }
        }

        return stmResult;
    }

    public static ArrayList<Stage> findStage(List<Enemy> enemies, int music, int background, int castle, boolean hasBoss, boolean orOperator, boolean monthly) {
        ArrayList<Stage> result = new ArrayList<>();

        for(MapColc mc : MapColc.values()) {
            if(mc == null)
                continue;

            for(StageMap stm : mc.maps.getList()) {
                if(stm == null)
                    continue;

                if (monthly && !isMonthly(mc, stm)) {
                    continue;
                }

                for(Stage st : stm.list.getList()) {
                    if(st == null)
                        continue;

                    if(music != -1) {
                        boolean mus = st.mus0 != null && st.mus0.id == music;

                        if(!mus && st.mus1 != null && st.mus1.id == music)
                            mus = true;

                        if(!mus)
                            continue;
                    }

                    if(background != -1 && (st.bg == null || st.bg.id != background))
                        continue;

                    if(castle != -1 && (st.castle == null || st.castle.id != castle))
                        continue;

                    if(enemies.isEmpty() || containsEnemies(st.data.datas, enemies, hasBoss, orOperator))
                        result.add(st);
                }
            }
        }

        return result;
    }

    public static ArrayList<Integer> findMedalByName(String name, CommonStatic.Lang.Locale lang) {
        ArrayList<Integer> result = new ArrayList<>();

        for(int i = 0; i < StaticStore.medalNumber; i++) {
            for(CommonStatic.Lang.Locale locale : CommonStatic.Lang.supportedLanguage) {
                String medalName = StaticStore.MEDNAME.getCont(i, locale);

                if(medalName == null || medalName.isBlank()) {
                    medalName = Data.trio(i);
                } else {
                    medalName += " " + Data.trio(i);
                }

                if(medalName.toLowerCase(Locale.ENGLISH).contains(name.toLowerCase(Locale.ENGLISH))) {
                    result.add(i);
                    break;
                }
            }
        }

        if(result.isEmpty()) {
            ArrayList<Integer> similar = new ArrayList<>();
            ArrayList<Integer> similarity = new ArrayList<>();

            int sMin = 10;

            name = name.toLowerCase(Locale.ENGLISH);

            if(lang == CommonStatic.Lang.Locale.KR)
                name = KoreanSeparater.separate(name);

            for(int i = 0; i < StaticStore.medalNumber; i++) {
                String medalName = StaticStore.MEDNAME.getCont(i, lang);

                if(medalName == null || medalName.isBlank())
                    continue;

                medalName = medalName.toLowerCase(Locale.ENGLISH);

                if(lang == CommonStatic.Lang.Locale.KR)
                    medalName = KoreanSeparater.separate(medalName);

                int s = calculateOptimalDistance(name, medalName);

                if (s <= TOLERANCE) {
                    similar.add(i);
                    similarity.add(s);

                    sMin = Math.min(s, sMin);
                }
            }

            if(similar.isEmpty())
                return similar;

            ArrayList<Integer> finalResult = new ArrayList<>();

            for(int i = 0; i < similar.size(); i++) {
                if (sMin == similarity.get(i)) {
                    finalResult.add(similar.get(i));
                }
            }

            return finalResult;
        } else {
            return result;
        }
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    public static ArrayList<Combo> filterComboWithUnit(Form f, String cName) {
        ArrayList<Combo> result = new ArrayList<>();

        List<Combo> fullCombo = UserProfile.getBCData().combos.getList();

        for(int i = 0; i < fullCombo.size(); i++) {
            Combo c = fullCombo.get(i);

            if(f == null) {
                if(cName != null) {
                    for(CommonStatic.Lang.Locale locale : CommonStatic.Lang.supportedLanguage) {
                        String comboName = MultiLangCont.getStatic().COMNAME.getCont(c, locale) + " | " + DataToString.getComboType(c, locale);

                        if(comboName.toLowerCase(Locale.ENGLISH).contains(cName.toLowerCase(Locale.ENGLISH))) {
                            result.add(c);
                            break;
                        }
                    }
                } else {
                    result.add(c);
                }
            } else {
                for(int k = 0; k < 5; k++) {
                    boolean added = false;

                    if(k >= c.forms.length || c.forms[k] == null || c.forms[k].unit == null)
                        continue;

                    if(c.forms[k].unit.id.id == f.unit.id.id && c.forms[k].fid <= f.fid) {
                        if(cName != null) {
                            for(CommonStatic.Lang.Locale locale : CommonStatic.Lang.supportedLanguage) {
                                String comboName = MultiLangCont.getStatic().COMNAME.getCont(c, locale) + " | " + DataToString.getComboType(c, locale);

                                if(comboName.toLowerCase(Locale.ENGLISH).contains(cName.toLowerCase(Locale.ENGLISH))) {
                                    result.add(c);
                                    added = true;
                                    break;
                                }
                            }
                        } else {
                            result.add(c);
                            added = true;
                        }
                    }

                    if(added)
                        break;
                }
            }
        }

        result.sort(Comparator.comparingInt(c -> c.id.id));

        return result;
    }

    public static List<Integer> findRewardByName(String name, CommonStatic.Lang.Locale lang) {
        List<Integer> result = new ArrayList<>();

        for(int i = 0; i < StaticStore.existingRewards.size(); i++) {
            for(CommonStatic.Lang.Locale locale : CommonStatic.Lang.supportedLanguage) {
                String rewardName = MultiLangCont.getStatic().RWNAME.getCont(StaticStore.existingRewards.get(i), locale);

                if (rewardName == null || rewardName.isBlank()) {
                    rewardName = Data.trio(StaticStore.existingRewards.get(i));
                } else {
                    rewardName = Data.trio(StaticStore.existingRewards.get(i)) + " " + rewardName;
                }

                if (rewardName.toLowerCase(Locale.ENGLISH).contains(name.toLowerCase(Locale.ENGLISH))) {
                    result.add(StaticStore.existingRewards.get(i));

                    break;
                }
            }
        }

        if(result.isEmpty()) {
            ArrayList<Integer> similar = new ArrayList<>();
            ArrayList<Integer> similarity = new ArrayList<>();

            int sMin = 10;

            name = name.toLowerCase(Locale.ENGLISH);

            if(lang == CommonStatic.Lang.Locale.KR)
                name = KoreanSeparater.separate(name);

            for(int i = 0; i < StaticStore.existingRewards.size(); i++) {
                String rewardName = MultiLangCont.getStatic().RWNAME.getCont(StaticStore.existingRewards.get(i), lang);

                if(rewardName == null || rewardName.isBlank())
                    continue;

                rewardName = rewardName.toLowerCase(Locale.ENGLISH);

                if(lang == CommonStatic.Lang.Locale.KR)
                    rewardName = KoreanSeparater.separate(rewardName);

                int s = calculateOptimalDistance(name, rewardName);

                if (s <= TOLERANCE) {
                    similar.add(StaticStore.existingRewards.get(i));
                    similarity.add(s);

                    sMin = Math.min(sMin, s);
                }
            }

            if(similar.isEmpty())
                return similar;

            ArrayList<Integer> finalResult = new ArrayList<>();

            for(int i = 0; i < similar.size(); i++) {
                if (sMin == similarity.get(i)) {
                    finalResult.add(similar.get(i));
                }
            }

            return finalResult;
        } else {
            return result;
        }
    }

    public static List<Stage> findStageByReward(int reward, double chance, int amount) {
        List<Stage> result = new ArrayList<>();

        for(MapColc mc : MapColc.values()) {
            if(mc == null)
                continue;

            for(StageMap map : mc.maps) {
                if(map == null)
                    continue;

                for(Stage st : map.list) {
                    if(st == null || !(st.info instanceof DefStageInfo) || (((DefStageInfo) st.info).drop == null && ((DefStageInfo) st.info).time == null))
                        continue;

                    if(chance == -1) {
                        boolean added = false;

                        if(((DefStageInfo) st.info).drop != null) {
                            for(int[] data : ((DefStageInfo) st.info).drop) {
                                if(data[1] == reward && (amount == -1 || data[2] >= amount)) {
                                    added = true;

                                    result.add(st);

                                    break;
                                }
                            }
                        }

                        if(added)
                            continue;

                        if(((DefStageInfo) st.info).time != null) {
                            for(int[] data : ((DefStageInfo) st.info).time) {
                                if(data[1] == reward && (amount == -1 || data[2] >= amount)) {
                                    result.add(st);

                                    break;
                                }
                            }
                        }
                    } else {
                        if(((DefStageInfo) st.info).drop == null)
                            continue;

                        List<Double> chances = DataToString.getDropChances(st);

                        if(chances == null) {
                            continue;
                        }

                        if(chances.isEmpty()) {
                            double ch = 100.0 / ((DefStageInfo) st.info).drop.length;

                            for(int i = 0; i < ((DefStageInfo) st.info).drop.length; i++) {
                                int[] data = ((DefStageInfo) st.info).drop[i];

                                if(data[1] == reward && ch >= chance && (amount == -1 || data[2] >= amount)) {
                                    result.add(st);

                                    break;
                                }
                            }
                        } else {
                            for(int i = 0; i < ((DefStageInfo) st.info).drop.length; i++) {
                                int[] data = ((DefStageInfo) st.info).drop[i];

                                if(data[1] == reward && chances.get(i) >= chance && (amount == -1 || data[2] >= amount)) {
                                    result.add(st);

                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    private static boolean searchMapColc(String[] names) {
        return names[1] == null && names[2] == null;
    }

    private static boolean searchStageMap(String[] names) {
        return names[2] == null;
    }

    private static int damerauLevenshteinDistance(String src, String compare) {
        int[][] table = new int[src.length() + 1][compare.length() + 1];

        for(int i = 0; i < src.length() + 1; i++) {
            table[i][0] = i;
        }

        for(int i  = 0; i < compare.length() + 1; i++) {
            table[0][i] = i;
        }

        for(int i = 1; i < src.length() + 1; i++) {
            for(int j = 1; j < compare.length() + 1; j++) {
                int cost;

                if(src.charAt(i-1) == compare.charAt(j-1))
                    cost = 0;
                else
                    cost = 1;

                table[i][j] = Math.min(Math.min(table[i-1][j] + 1, table[i][j-1] + 1), table[i-1][j-1] + cost);

                if(i > 1 && j > 1 && src.charAt(i-1) == compare.charAt(j-2) && src.charAt(i-2) == compare.charAt(j-1)) {
                    table[i][j] = Math.min(table[i][j], table[i-2][j-2] + 1);
                }
            }
        }

        return table[src.length()][compare.length()];
    }

    /**
     * Get word list by separated per number of words in search keywords<br>
     * <br>
     * For example, if target text is `abc def ghi`, and search keyword was `abc def`, then this
     * method will slice target text by 2 each words. Result will be : <br>
     * {@code
     * { abc def, def ghi }
     * }
     * @param src Target text separated by spaces
     * @param numberOfWords Number of words in search keywords
     * @return Array of sliced words
     */
    private static String[] getWords(String[] src, int numberOfWords) {
        int length = Math.max(1, src.length - numberOfWords + 1);

        String[] result = new String[length];

        if (src.length < numberOfWords) {
            result[0] = String.join(" ", src);
        } else {
            for (int i = 0; i < length; i++) {
                StringBuilder builder = new StringBuilder();

                for (int j = i; j < i + numberOfWords; j++) {
                    builder.append(src[j]);

                    if (j < i + numberOfWords - 1) {
                        builder.append(" ");
                    }
                }

                result[i] = builder.toString();
            }
        }

        return result;
    }

    private static <T> boolean needToPerformContainMode(Iterable<T> set, String keyword, Class<T> cls) {
        for(T t : set) {
            if(t == null)
                continue;

            keyword = keyword.toLowerCase(Locale.ENGLISH);

            for(CommonStatic.Lang.Locale locale : CommonStatic.Lang.supportedLanguage) {
                String name = StaticStore.safeMultiLangGet(t, locale);

                if(name != null && !name.isBlank()) {
                    if(name.toLowerCase(Locale.ENGLISH).contains(keyword))
                        return true;

                    name = name.replace("-", " ");

                    if(name.toLowerCase(Locale.ENGLISH).contains(keyword))
                        return true;

                    name = name.replace("\\.", "");

                    if (name.toLowerCase(Locale.ENGLISH).contains(keyword))
                        return true;

                    ArrayList<String> alias;

                    if(cls == Form.class) {
                        alias = AliasHolder.FALIAS.getCont((Form) t, locale);
                    } else if(cls == Enemy.class) {
                        alias = AliasHolder.EALIAS.getCont((Enemy) t, locale);
                    } else if(cls == Stage.class) {
                        alias = AliasHolder.SALIAS.getCont((Stage) t, locale);
                    } else {
                        continue;
                    }

                    if(alias != null && !alias.isEmpty()) {
                        for(String a : alias) {
                            if(a.toLowerCase(Locale.ENGLISH).contains(keyword))
                                return true;
                        }
                    }
                } else {
                    ArrayList<String> alias;

                    if(cls == Form.class) {
                        alias = AliasHolder.FALIAS.getCont((Form) t, locale);
                    } else if(cls == Enemy.class) {
                        alias = AliasHolder.EALIAS.getCont((Enemy) t, locale);
                    } else if(cls == Stage.class) {
                        alias = AliasHolder.SALIAS.getCont((Stage) t, locale);
                    } else {
                        continue;
                    }

                    if(alias != null && !alias.isEmpty()) {
                        for(String a : alias) {
                            if(a.toLowerCase(Locale.ENGLISH).contains(keyword))
                                return true;
                        }
                    }
                }
            }

            if(cls == Stage.class) {
                Stage st = (Stage) t;

                StageMap stm = st.getCont();

                if(stm != null) {
                    MapColc mc = stm.getCont();

                    if(mc != null) {
                        String[] ids = {
                                mc.getSID()+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                mc.getSID()+"-"+stm.id.id+"-"+st.id.id,
                                DataToString.getMapCode(mc)+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                DataToString.getMapCode(mc)+"-"+stm.id.id+"-"+st.id.id
                        };

                        for(String id : ids) {
                            if(id.toLowerCase(Locale.ENGLISH).contains(keyword)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    private static <T> ArrayList<Integer> getDistances(Iterable<T> set, String keyword, CommonStatic.Lang.Locale lang, Class<T> cls) {
        ArrayList<Integer> distances = new ArrayList<>();
        int allMin = Integer.MAX_VALUE;

        keyword = keyword.toLowerCase(Locale.ENGLISH);

        if(lang == CommonStatic.Lang.Locale.KR)
            keyword = KoreanSeparater.separate(keyword);

        for(T t : set) {
            if(t == null) {
                distances.add(-1);
                continue;
            }

            String name = StaticStore.safeMultiLangGet(t, lang);

            if(name == null || name.isBlank()) {
                distances.add(-1);
                continue;
            }

            name = name.toLowerCase(Locale.ENGLISH);

            if(lang == CommonStatic.Lang.Locale.KR) {
                name = KoreanSeparater.separate(name);
            }

            int s = calculateOptimalDistance(name, keyword);

            int disMin = s;
            allMin = Math.min(allMin, s);

            if(disMin > allMin) {
                ArrayList<String> alias;

                if(cls == Form.class) {
                    alias = AliasHolder.FALIAS.getCont((Form) t, lang);
                } else if(cls == Enemy.class) {
                    alias = AliasHolder.EALIAS.getCont((Enemy) t, lang);
                } else if(cls == Stage.class) {
                    alias = AliasHolder.SALIAS.getCont((Stage) t, lang);
                } else {
                    distances.add(disMin);
                    continue;
                }

                if(alias != null && !alias.isEmpty()) {
                    for(String a : alias) {
                        a = a.toLowerCase(Locale.ENGLISH);

                        if(lang == CommonStatic.Lang.Locale.KR)
                            a = KoreanSeparater.separate(a);

                        s = calculateOptimalDistance(keyword, a);

                        disMin = Math.min(disMin, s);
                        allMin = Math.min(allMin, s);
                    }
                }
            }

            distances.add(disMin);
        }

        return distances;
    }

    private static int calculateOptimalDistance(String src, String target) {
        int distance = calculateRawDistance(src, target);

        if (target.contains("-") && target.contains(".")) {
            distance = Math.min(distance, calculateRawDistance(src, target.replaceAll(spaceRegex, " ")));
            distance = Math.min(distance, calculateRawDistance(src, target.replaceAll("\\.", "")));
            distance = Math.min(distance, calculateRawDistance(src, target.replaceAll(spaceRegex, " ").replaceAll("\\.", "")));
        } else if (target.contains("-")) {
            distance = Math.min(distance, calculateRawDistance(src, target.replaceAll(spaceRegex, " ")));
        } else if (target.contains(".")) {
            distance = Math.min(distance, calculateRawDistance(src, target.replaceAll("\\.", "")));
        }

        return distance;
    }

    private static int calculateRawDistance(String src, String target) {
        int distance = Integer.MAX_VALUE;

        int wordNumber = StringUtils.countMatches(src, ' ') + 1;

        for (int i = 1; i <= wordNumber; i++) {
            String[] words;

            if(wordNumber != 1) {
                words = getWords(target.split(" "), i);
            } else {
                words = target.split(" ");
            }

            for (String word : words) {
                distance = Math.min(distance, damerauLevenshteinDistance(word, src));
            }
        }

        distance = Math.min(distance, damerauLevenshteinDistance(target, src));

        return distance;
    }

    private static boolean containsEnemies(SCDef.Line[] lines, List<Enemy> enemies, boolean hasBoss, boolean or) {
        boolean b = !hasBoss;

        if(!b) {
            for(SCDef.Line l : lines) {
                if(l.boss != 0) {
                    b = true;
                    break;
                }
            }
        }

        boolean c = false;

        for(Enemy e : enemies) {
            boolean contain = false;

            for(SCDef.Line l : lines) {
                if(l.enemy != null && l.enemy.equals(e.id)) {
                    contain = true;
                    break;
                }
            }

            if(!or && !contain)
                return false;
            else if(or && contain) {
                c = true;
                break;
            }
        }

        return b && (!or || c);
    }

    private static boolean isMonthly(MapColc mc, StageMap map) {
        switch (mc.getSID()) {
            case "000003" -> {
                int id = map.id.id;

                for (int i = 0; i < storyChapterMonthly.length; i++) {
                    if (id == storyChapterMonthly[i])
                        return true;
                }

            }
            case "000001" -> {
                int id = map.id.id;

                for (int i = 0; i < cycloneMonthly.length; i++) {
                    if (id == cycloneMonthly[i])
                        return true;
                }

            }
            case "000014" -> {
                int id = map.id.id;

                for (int i = 0; i < cycloneCatamin.length; i++) {
                    if (id == cycloneCatamin[i])
                        return true;
                }

            }
        }

        return mc.getSID().equals("000000");
    }
}
