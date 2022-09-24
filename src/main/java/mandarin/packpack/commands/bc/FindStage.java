package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.pack.UserProfile;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.CastleList;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import common.util.unit.Enemy;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.SearchHolder;
import mandarin.packpack.supporter.server.holder.StageEnemyMessageHolder;
import mandarin.packpack.supporter.server.holder.StageInfoButtonHolder;
import mandarin.packpack.supporter.server.holder.StageInfoMessageHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.util.ArrayList;
import java.util.List;

public class FindStage extends TimedConstraintCommand {
    private static final int PARAM_SECOND = 2;
    private static final int PARAM_EXTRA = 4;
    private static final int PARAM_COMPACT = 8;
    private static final int PARAM_OR = 16;
    private static final int PARAM_AND = 32;
    private static final int PARAM_BOSS = 64;

    private final ConfigHolder config;

    public FindStage(ConstraintCommand.ROLE role, int lang, IDHolder id, ConfigHolder config, long time) {
        super(role, lang, id, time, StaticStore.COMMAND_FINDSTAGE_ID);

        if(config == null)
            this.config = id.config;
        else
            this.config = config;
    }

    @Override
    public void prepare() throws Exception {
        registerRequiredPermission(Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EMBED_LINKS);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String enemyName = getEnemyName(getContent(event));

        int param = checkParameters(getContent(event));
        int star = getLevel(getContent(event));
        int music = getMusic(getContent(event));
        int castle = getCastle(getContent(event));
        int background = getBackground(getContent(event));

        boolean isFrame = (param & PARAM_SECOND) == 0 && config.useFrame;
        boolean isExtra = (param & PARAM_EXTRA) > 0 || config.extra;
        boolean isCompact = (param & PARAM_COMPACT) > 0 || (holder.forceCompact ? holder.config.compact : config.compact);
        boolean orOperate = (param & PARAM_OR) > 0 && (param & PARAM_AND) == 0;
        boolean hasBoss = (param & PARAM_BOSS) > 0;

        if(enemyName.isBlank() && music < 0 && castle < 0 && background < 0 && !hasBoss) {
            ch.sendMessage(LangID.getStringByID("fstage_noparam", lang)).queue();

            return;
        }

        if(background >= 0 && UserProfile.getBCData().bgs.get(background) == null) {
            ch.sendMessage(LangID.getStringByID("fstage_bg", lang)).queue();

            return;
        }

        if(music >= 0 && UserProfile.getBCData().musics.get(music) == null) {
            ch.sendMessage(LangID.getStringByID("fstage_music", lang)).queue();

            return;
        }

        ArrayList<CastleList> castleLists = new ArrayList<>(CastleList.defset());

        if(castle >= 0 && castle >= castleLists.get(0).size()) {
            ch.sendMessage(LangID.getStringByID("fstage_castle", lang)).queue();

            return;
        }

        List<List<Enemy>> enemySequences = new ArrayList<>();
        List<Enemy> filterEnemy = new ArrayList<>();
        StringBuilder enemyList = new StringBuilder();

        String[] names = enemyName.split("/");

        if(names.length > 5) {
            ch.sendMessage(LangID.getStringByID("fstage_toomany", lang)).queue();
            disableTimer();

            return;
        }

        if(!enemyName.isBlank()) {
            for(int i = 0; i < names.length; i++) {
                if(names[i].trim().isBlank()) {
                    createMessageWithNoPings(ch, LangID.getStringByID("fstage_noname", lang));
                    disableTimer();

                    return;
                }

                ArrayList<Enemy> enemies = EntityFilter.findEnemyWithName(names[i].trim(), lang);

                if(enemies.isEmpty()) {
                    createMessageWithNoPings(ch, LangID.getStringByID("enemyst_noenemy", lang).replace("_", names[i].trim()));
                    disableTimer();

                    return;
                } else if(enemies.size() == 1) {
                    filterEnemy.add(enemies.get(0));

                    String n = StaticStore.safeMultiLangGet(enemies.get(0), lang);

                    if(n == null || n.isBlank()) {
                        n = Data.trio(enemies.get(0).id.id);
                    }

                    enemyList.append(n).append(", ");
                } else {
                    enemySequences.add(enemies);
                }
            }
        }

        if(enemySequences.isEmpty()) {
            ArrayList<Stage> stages = EntityFilter.findStage(filterEnemy, music, background, castle, hasBoss, orOperate);

            if(stages.isEmpty()) {
                createMessageWithNoPings(ch, LangID.getStringByID("fstage_nost", lang));

                disableTimer();
            } else if(stages.size() == 1) {
                Message result = EntityHandler.showStageEmb(stages.get(0), ch, isFrame, isExtra, isCompact, star, lang);

                Member m = getMember(event);

                if(m != null) {
                    Message msg = getMessage(event);

                    if(msg != null) {
                        StaticStore.putHolder(m.getId(), new StageInfoButtonHolder(stages.get(0), msg, result, ch.getId()));
                    }
                }
            } else {
                StringBuilder sb = new StringBuilder(LangID.getStringByID("fstage_several", lang)).append("```md\n");

                List<String> data = accumulateStage(stages, true);

                for(int i = 0; i < data.size(); i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(stages.size() > SearchHolder.PAGE_CHUNK) {
                    int totalPage = stages.size() / SearchHolder.PAGE_CHUNK;

                    if(stages.size() % SearchHolder.PAGE_CHUNK != 0)
                        totalPage++;

                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(1)).replace("-", String.valueOf(totalPage))).append("\n");
                }

                sb.append("```");

                Message res = registerSearchComponents(ch.sendMessage(sb.toString()).setAllowedMentions(new ArrayList<>()), stages.size(), accumulateStage(stages, false), lang).complete();

                if(res != null) {
                    Member m = getMember(event);

                    if(m != null) {
                        Message msg = getMessage(event);

                        if(msg != null) {
                            StaticStore.putHolder(m.getId(), new StageInfoMessageHolder(stages, msg, res, ch.getId(), star, isFrame, isExtra, isCompact, lang));
                        }
                        disableTimer();
                    }
                }
            }
        } else {
            StringBuilder sb = new StringBuilder();

            if(enemyList.length() != 0) {
                sb.append(LangID.getStringByID("fstage_selected", lang).replace("_", enemyList.toString().replaceAll(", $", "")));
            }

            sb.append("```md\n").append(LangID.getStringByID("formst_pick", lang));

            List<Enemy> enemies = enemySequences.get(0);

            List<String> data = accumulateEnemy(enemies);

            for(int i = 0; i < data.size(); i++) {
                sb.append(i+1).append(". ").append(data.get(i)).append("\n");
            }

            if(enemies.size() > SearchHolder.PAGE_CHUNK) {
                int totalPage = enemies.size() / SearchHolder.PAGE_CHUNK;

                if(enemies.size() % SearchHolder.PAGE_CHUNK != 0)
                    totalPage++;

                sb.append(LangID.getStringByID("formst_page", lang).replace("_", "1").replace("-", String.valueOf(totalPage))).append("\n");
            }

            sb.append("```");

            Message res = registerSearchComponents(ch.sendMessage(sb.toString()).setAllowedMentions(new ArrayList<>()), enemies.size(), data, lang).complete();

            if(res != null) {
                Member m = getMember(event);

                if(m != null) {
                    Message msg = getMessage(event);

                    if(msg != null)
                        StaticStore.putHolder(m.getId(), new StageEnemyMessageHolder(enemySequences, filterEnemy, enemyList, msg, res, ch.getId(), isFrame, isExtra, isCompact, orOperate, hasBoss, star, background, castle, music, lang));
                }
            }
        }
    }

    private String getEnemyName(String message) {
        String[] contents = message.split(" ");

        if(contents.length < 2)
            return "";

        StringBuilder result = new StringBuilder();

        boolean second = false;
        boolean level = false;
        boolean background = false;
        boolean or = false;
        boolean and = false;
        boolean castle = false;
        boolean music = false;
        boolean boss = false;

        for(int i = 1; i < contents.length; i++) {
            if(contents[i].equals("-lv") && !level) {
                if(i < contents.length - 1 && StaticStore.isNumeric(contents[i+1])) {
                    level = true;
                    i++;
                } else {
                    result.append(contents[i]);

                    if(i < contents.length - 1) {
                        result.append(" ");
                    }
                }
            } else if(!second && contents[i].equals("-s")) {
                second = true;
            } else if(!background && (contents[i].equals("-bg") || contents[i].equals("-background")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                background = true;
                i++;
            } else if(!and && (contents[i].equals("-a") || contents[i].equals("-and"))) {
                and = true;
            } else if(!or && (contents[i].equals("-o") || contents[i].equals("-or"))) {
                or = true;
            } else if(!castle && (contents[i].equals("-cs") || contents[i].equals("-castle")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                castle = true;
                i++;
            } else if(!music && (contents[i].equals("-ms") || contents[i].equals("-music")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                music = true;
                i++;
            } else if(!boss && (contents[i].equals("-b") || contents[i].equals("-boss"))) {
                boss = true;
            } else {
                result.append(contents[i]);

                if(i < contents.length - 1) {
                    result.append(" ");
                }
            }
        }

        return result.toString();
    }

    private int checkParameters(String message) {
        String[] msg = message.split(" ");

        int result = 1;

        if(msg.length >= 2) {
            String[] pureMessage = message.split(" ", 2)[1].split(" ");

            label:
            for(String str : pureMessage) {
                switch (str) {
                    case "-s":
                        if ((result & PARAM_SECOND) == 0) {
                            result |= PARAM_SECOND;
                        } else
                            break label;
                        break;
                    case "-e":
                    case "-extra":
                        if ((result & PARAM_EXTRA) == 0) {
                            result |= PARAM_EXTRA;
                        } else
                            break label;
                        break;
                    case "-c":
                    case "-compact":
                        if ((result & PARAM_COMPACT) == 0) {
                            result |= PARAM_COMPACT;
                        } else
                            break label;
                        break;
                    case "-o":
                    case "-or":
                        if ((result & PARAM_OR) == 0) {
                            result |= PARAM_OR;
                        } else
                            break label;
                        break;
                    case "-a":
                    case "-and":
                        if ((result & PARAM_AND) == 0) {
                            result |= PARAM_AND;
                        } else
                            break label;
                        break;
                    case "-b":
                    case "-boss":
                        if ((result & PARAM_BOSS) == 0) {
                            result |= PARAM_BOSS;
                        } else {
                            break label;
                        }
                        break;
                }
            }
        }

        return result;
    }

    private int getLevel(String command) {
        int level = 0;

        if(command.contains("-lv")) {
            String[] contents = command.split(" ");

            for(int i = 0; i < contents.length; i++) {
                if(contents[i].equals("-lv") && i < contents.length - 1 && StaticStore.isNumeric(contents[i+1])) {
                    level = StaticStore.safeParseInt(contents[i+1]);
                    break;
                }
            }
        }

        return level;
    }

    private int getBackground(String command) {
        int bg = -1;

        if (command.contains("-bg") || command.contains("-background")) {
            String[] contents = command.split(" ");

            for(int i = 0; i < contents.length; i++) {
                if((contents[i].equals("-bg") || contents[i].equals("-background")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                    bg = StaticStore.safeParseInt(contents[i + 1]);
                    break;
                }
            }
        }

        return bg;
    }

    private int getCastle(String command) {
        int castle = -1;

        if (command.contains("-cs")) {
            String[] contents = command.split(" ");

            for(int i = 0; i < contents.length; i++) {
                if((contents[i].equals("-cs") || contents[i].equals("-castle")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                    castle = StaticStore.safeParseInt(contents[i + 1]);
                    break;
                }
            }
        }

        return castle;
    }

    private int getMusic(String command) {
        int music = -1;

        if (command.contains("-ms")) {
            String[] contents = command.split(" ");

            for(int i = 0; i < contents.length; i++) {
                if((contents[i].equals("-ms") || contents[i].equals("-music")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                    music = StaticStore.safeParseInt(contents[i + 1]);
                    break;
                }
            }
        }

        return music;
    }
    
    private List<String> accumulateEnemy(List<Enemy> enemies) {
        List<String> data = new ArrayList<>();
        
        for(int i = 0; i < SearchHolder.PAGE_CHUNK; i++) {
            if(i >= enemies.size())
                break;

            Enemy e = enemies.get(i);

            String ename = e.id == null ? "UNKNOWN " : Data.trio(e.id.id)+" ";

            int oldConfig = CommonStatic.getConfig().lang;
            CommonStatic.getConfig().lang = lang;

            if(MultiLangCont.get(e) != null)
                ename += MultiLangCont.get(e);

            CommonStatic.getConfig().lang = oldConfig;

            data.add(ename);
        }
        
        return data;
    }
    
    private List<String> accumulateStage(List<Stage> stages, boolean full) {
        List<String> data = new ArrayList<>();
        
        for(int i = 0; i < SearchHolder.PAGE_CHUNK; i++) {
            if(i >= stages.size())
                break;

            Stage st = stages.get(i);
            StageMap stm = st.getCont();
            MapColc mc = stm.getCont();

            String name = "";

            if(full) {
                if(mc != null)
                    name = mc.getSID()+"/";
                else
                    name = "Unknown/";

                if(stm.id != null)
                    name += Data.trio(stm.id.id)+"/";
                else
                    name += "Unknown/";

                if(st.id != null)
                    name += Data.trio(st.id.id)+" | ";
                else
                    name += "Unknown | ";

                if(mc != null) {
                    int oldConfig = CommonStatic.getConfig().lang;
                    CommonStatic.getConfig().lang = lang;

                    String mcn = MultiLangCont.get(mc);

                    CommonStatic.getConfig().lang = oldConfig;

                    if(mcn == null || mcn.isBlank())
                        mcn = mc.getSID();

                    name += mcn+" - ";
                } else {
                    name += "Unknown - ";
                }
            }

            int oldConfig = CommonStatic.getConfig().lang;
            CommonStatic.getConfig().lang = lang;

            String stmn = MultiLangCont.get(stm);

            CommonStatic.getConfig().lang = oldConfig;

            if(stm.id != null) {
                if(stmn == null || stmn.isBlank())
                    stmn = Data.trio(stm.id.id);
            } else {
                if(stmn == null || stmn.isBlank())
                    stmn = "Unknown";
            }

            name += stmn+" - ";

            CommonStatic.getConfig().lang = lang;

            String stn = MultiLangCont.get(st);

            CommonStatic.getConfig().lang = oldConfig;

            if(st.id != null) {
                if(stn == null || stn.isBlank())
                    stn = Data.trio(st.id.id);
            } else {
                if(stn == null || stn.isBlank())
                    stn = "Unknown";
            }

            name += stn;

            data.add(name);
        }
        
        return data;
    }
}
