package mandarin.packpack.supporter.server.holder;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Enemy;
import mandarin.packpack.commands.bc.EnemyGif;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.TimeBoolean;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class EnemyAnimMessageHolder extends SearchHolder {
    private final ArrayList<Enemy> enemy;

    private final int mode;
    private final int frame;
    private final boolean transparent;
    private final boolean debug;
    private final boolean gif;
    private final boolean raw;
    private final boolean gifMode;

    public EnemyAnimMessageHolder(ArrayList<Enemy> enemy, Message author, Message msg, String channelID, int mode, int frame, boolean transparent, boolean debug, int lang, boolean isGif, boolean raw, boolean gifMode) {
        super(msg, author, channelID, lang);

        this.enemy = enemy;

        this.mode = mode;
        this.frame = frame;
        this.transparent = transparent;
        this.debug = debug;
        this.gif = isGif;
        this.raw = raw;
        this.gifMode = gifMode;

        registerAutoFinish(this, msg, author, lang, FIVE_MIN);
    }

    @Override
    public List<String> accumulateListData(boolean onText) {
        List<String> data = new ArrayList<>();

        for (int i = PAGE_CHUNK * page; i < PAGE_CHUNK * (page + 1); i++) {
            if (i >= enemy.size())
                break;

            Enemy e = enemy.get(i);

            String ename = Data.trio(e.id.id) + " ";

            int oldConfig = CommonStatic.getConfig().lang;
            CommonStatic.getConfig().lang = lang;

            if (MultiLangCont.get(e) != null)
                ename += MultiLangCont.get(e);

            CommonStatic.getConfig().lang = oldConfig;

            data.add(ename);
        }

        return data;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event) {
        MessageChannel ch = event.getChannel();
        Guild g = event.getGuild();

        if (g == null)
            return;

        int id = parseDataToInt(event);

        try {
            Enemy e = enemy.get(id);

            if(EnemyGif.forbidden.contains(e.id.id)) {
                ch.sendMessage(LangID.getStringByID("gif_dummy", lang)).queue();

                msg.delete().queue();

                return;
            }

            if (gif) {
                TimeBoolean timeBoolean = StaticStore.canDo.get("gif");

                if (timeBoolean == null || timeBoolean.canDo) {
                    new Thread(() -> {

                        try {
                            boolean result = EntityHandler.generateEnemyAnim(e, ch, g.getBoostTier().getKey(), mode, debug, frame, lang, raw, gifMode);

                            if (result) {
                                long time = raw ? TimeUnit.MINUTES.toMillis(1) : TimeUnit.SECONDS.toMillis(30);

                                StaticStore.canDo.put("gif", new TimeBoolean(false, time));

                                Timer timer = new Timer();

                                timer.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        System.out.println("Remove Process : gif");
                                        StaticStore.canDo.put("gif", new TimeBoolean(true));
                                    }
                                }, time);
                            }
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    }).start();
                } else {
                    ch.sendMessage(LangID.getStringByID("single_wait", lang).replace("_", DataToString.df.format((timeBoolean.totalTime - (System.currentTimeMillis() - StaticStore.canDo.get("gif").time)) / 1000.0))).queue();
                }
            } else {
                Member m = event.getMember();

                if (m != null) {
                    try {
                        if (StaticStore.timeLimit.containsKey(m.getId()) && StaticStore.timeLimit.get(m.getId()).containsKey(StaticStore.COMMAND_ENEMYIMAGE_ID)) {
                            long time = StaticStore.timeLimit.get(m.getId()).get(StaticStore.COMMAND_ENEMYIMAGE_ID);

                            if (System.currentTimeMillis() - time > 10000) {
                                EntityHandler.generateEnemyImage(e, ch, mode, frame, transparent, debug, lang);

                                StaticStore.timeLimit.get(m.getId()).put(StaticStore.COMMAND_ENEMYIMAGE_ID, System.currentTimeMillis());
                            } else {
                                ch.sendMessage(LangID.getStringByID("command_timelimit", lang).replace("_", DataToString.df.format((System.currentTimeMillis() - time) / 1000.0))).queue();
                            }
                        } else if (StaticStore.timeLimit.containsKey(m.getId())) {
                            EntityHandler.generateEnemyImage(e, ch, mode, frame, transparent, debug, lang);

                            StaticStore.timeLimit.get(m.getId()).put(StaticStore.COMMAND_ENEMYIMAGE_ID, System.currentTimeMillis());
                        } else {
                            EntityHandler.generateEnemyImage(e, ch, mode, frame, transparent, debug, lang);

                            Map<String, Long> memberLimit = new HashMap<>();

                            memberLimit.put(StaticStore.COMMAND_ENEMYIMAGE_ID, System.currentTimeMillis());

                            StaticStore.timeLimit.put(m.getId(), memberLimit);
                        }
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        msg.delete().queue();
    }

    @Override
    public int getDataSize() {
        return enemy.size();
    }
}
