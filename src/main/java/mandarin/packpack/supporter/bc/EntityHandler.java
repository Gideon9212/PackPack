package mandarin.packpack.supporter.bc;

import com.google.gson.JsonObject;
import common.CommonStatic;
import common.battle.BasisSet;
import common.battle.data.*;
import common.pack.PackData;
import common.pack.UserProfile;
import common.system.fake.FakeImage;
import common.system.fake.ImageBuilder;
import common.system.files.VFile;
import common.util.Data;
import common.util.anim.AnimU;
import common.util.anim.EAnimD;
import common.util.lang.MultiLangCont;
import common.util.pack.Background;
import common.util.pack.Soul;
import common.util.stage.MapColc;
import common.util.stage.SCDef;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import common.util.stage.info.DefStageInfo;
import common.util.unit.*;
import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.cell.*;
import mandarin.packpack.supporter.calculation.Equation;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.lwjgl.GLGraphics;
import mandarin.packpack.supporter.lwjgl.opengl.model.FontModel;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import mandarin.packpack.supporter.server.holder.component.EnemyButtonHolder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.ArrayUtils;
import org.jcodec.common.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class EntityHandler {
    private static final DecimalFormat df;

    private static final float catFruitTextGap = 20f;

    private static FontModel font;

    static {
        NumberFormat nf = NumberFormat.getInstance(Locale.US);

        df = (DecimalFormat) nf;
        df.applyPattern("#.##");
    }

    public static void initialize() {
        File fon = new File("./data/ForceFont.otf");

        try {
            CountDownLatch waiter = new CountDownLatch(1);

            StaticStore.renderManager.queueGL(() -> {
                font = new FontModel(24f, fon, FontModel.Type.FILL, 0f);

                waiter.countDown();
            });

            waiter.await();
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::initialize - Failed to initialize font file");
        }
    }

    public static void performUnitEmb(Form f, GenericCommandInteractionEvent event, ConfigHolder config, boolean isFrame, boolean talent, boolean extra, Level lv, boolean treasure, TreasureHolder holder, CommonStatic.Lang.Locale lang, Consumer<Message> onSuccess) throws Exception {
        ReplyCallbackAction action = event.deferReply();

        int level = lv.getLv();
        int levelp = lv.getPlusLv();

        if(level <= 0) {
            if(f.unit.rarity == 0)
                level = 110;
            else {
                if(config == null)
                    level = 30;
                else
                    level = config.defLevel;
            }
        }

        if(level > f.unit.max) {
            levelp = level - f.unit.max;
            level = f.unit.max;

            if(levelp > f.unit.maxp)
                levelp = f.unit.maxp;

            if(levelp < 0)
                levelp = 0;
        }

        lv.setLevel(level);
        lv.setPlusLevel(levelp);

        String l;

        if(levelp == 0)
            l = String.valueOf(level);
        else
            l = level + " + " + levelp;

        File img = generateIcon(f);
        File cf;

        if(extra)
            cf = generateCatfruit(f, lang);
        else
            cf = null;

        EmbedBuilder spec = new EmbedBuilder();

        int c;

        if(f.fid == 0)
            c = StaticStore.rainbow[4];
        else if(f.fid == 1)
            c = StaticStore.rainbow[3];
        else
            c = StaticStore.rainbow[2];

        int[] t;

        if(talent && f.du.getPCoin() != null) {
            t = f.du.getPCoin().max.clone();
        } else
            t = null;

        if(t != null) {
            t = handleTalent(f, lv, t);

            lv.setTalents(t);
        }

        spec.setTitle(DataToString.getTitle(f, lang));

        String desc = "";

        if(talent && f.du.getPCoin() != null && t != null && talentExists(t)) {
            desc += LangID.getStringByID("data.unit.talent.info", lang) + "\n";
        }

        if(holder.differentFromGlobal()) {
            desc += LangID.getStringByID("data.unit.treasure", lang);
        }

        if(!desc.isBlank()) {
            spec.setDescription(desc);
        }

        spec.setColor(c);
        spec.setThumbnail("attachment://icon.png");
        spec.addField(LangID.getStringByID("data.id", lang), DataToString.getID(f.uid.id, f.fid), true);
        spec.addField(LangID.getStringByID("data.unit.level", lang), l, true);

        String hpNormal = DataToString.getHP(f.du, f.unit.lv, talent, lv, false, holder);
        String hpWithTreasure;

        if(treasure) {
            hpWithTreasure = DataToString.getHP(f.du, f.unit.lv, talent, lv, true, holder);
        } else {
            hpWithTreasure = "";
        }

        if(hpWithTreasure.isBlank() || hpNormal.equals(hpWithTreasure)) {
            spec.addField(LangID.getStringByID("data.hp", lang), hpNormal, true);
        } else {
            spec.addField(LangID.getStringByID("data.hp", lang), hpNormal + " <" + hpWithTreasure + ">", true);
        }

        spec.addField(LangID.getStringByID("data.kb", lang), DataToString.getHitback(f.du, talent, lv), true);
        spec.addField(LangID.getStringByID("data.unit.cooldown", lang), DataToString.getCD(f.du,isFrame, talent, lv, holder), true);
        spec.addField(LangID.getStringByID("data.speed", lang), DataToString.getSpeed(f.du, talent, lv), true);
        spec.addField(LangID.getStringByID("data.unit.cost", lang), DataToString.getCost(f.du, talent, lv), true);
        spec.addField(DataToString.getRangeTitle(f.du, lang), DataToString.getRange(f.du), true);
        spec.addField(LangID.getStringByID("data.attackTime", lang), DataToString.getAtkTime(f.du, talent, isFrame, lv), true);
        spec.addField(LangID.getStringByID("data.foreswing", lang), DataToString.getPre(f.du, isFrame), true);
        spec.addField(LangID.getStringByID("data.backswing", lang), DataToString.getPost(f.du, isFrame), true);
        spec.addField(LangID.getStringByID("data.tba", lang), DataToString.getTBA(f.du, talent, lv, isFrame), true);
        spec.addField(LangID.getStringByID("data.attackType", lang), DataToString.getSiMu(f.du, lang), true);

        String dpsNormal = DataToString.getDPS(f.du, f.unit.lv, talent, lv, false, holder);
        String dpsWithTreasure;

        if(treasure) {
            dpsWithTreasure = DataToString.getDPS(f.du, f.unit.lv, talent, lv, true, holder);
        } else {
            dpsWithTreasure = "";
        }

        if(dpsWithTreasure.isBlank() || dpsNormal.equals(dpsWithTreasure)) {
            spec.addField(LangID.getStringByID("data.dps", lang), dpsNormal, true);
        } else {
            spec.addField(LangID.getStringByID("data.dps", lang), dpsNormal + " <" + dpsWithTreasure + ">", true);
        }

        spec.addField(LangID.getStringByID("data.useAbility", lang), DataToString.getAbilT(f.du, lang), true);
        spec.addField(LangID.getStringByID("data.damage", lang), DataToString.getAtk(f.du, f.unit.lv, talent, lv, treasure, holder), true);
        spec.addField(LangID.getStringByID("data.trait", lang), DataToString.getTrait(f.du, talent, lv, true, lang), true);

        MaskUnit du;

        if(f.du.getPCoin() != null)
            if(talent && t != null)
                du = f.du.getPCoin().improve(t);
            else
                du = f.du;
        else
            du = f.du;

        List<String> abis = Interpret.getAbi(du, true, lang, treasure ? du.getTraits() : null, holder);
        abis.addAll(Interpret.getProc(du, !isFrame, true, lang, 1.0, 1.0, treasure, du.getTraits(), holder::getAbilityMultiplier));

        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < abis.size(); i++) {
            if(i == abis.size() - 1)
                sb.append("- ").append(abis.get(i));
            else
                sb.append("- ").append(abis.get(i)).append("\n");
        }

        String res = sb.toString();

        if(res.isBlank())
            res = LangID.getStringByID("data.none", lang);
        else if(res.length() > 1024) {
            abis = Interpret.getAbi(du, false, lang, treasure ? du.getTraits() : null, holder);
            abis.addAll(Interpret.getProc(du, !isFrame, false, lang, 1.0, 1.0, treasure, du.getTraits(), holder::getAbilityMultiplier));

            sb = new StringBuilder();

            for(int i = 0; i < abis.size(); i++) {
                if(i == abis.size() - 1)
                    sb.append("- ").append(abis.get(i));
                else
                    sb.append("- ").append(abis.get(i)).append("\n");
            }

            res = sb.toString();
        }

        spec.addField(LangID.getStringByID("data.ability", lang), res, false);

        if(extra) {
            String explanation = DataToString.getDescription(f, lang);

            if(explanation != null)
                spec.addField(LangID.getStringByID("data.unit.description", lang), explanation, false);

            String catfruit = DataToString.getCatFruitEvolve(f, lang);

            if(catfruit != null)
                spec.addField(LangID.getStringByID("data.unit.evolve", lang), catfruit, false);

            spec.setImage("attachment://cf.png");
        }

        if(t != null && talentExists(t))
            spec.setFooter(DataToString.getTalent(f.du, lv, lang), null);

        action = action.addEmbeds(spec.build());

        if(img != null)
            action = action.addFiles(FileUpload.fromData(img, "icon.png"));

        if(cf != null)
            action = action.addFiles(FileUpload.fromData(cf, "cf.png"));

        action.queue(hook -> {
            if (hook == null)
                return;

            hook.retrieveOriginal().queue(msg -> {
                MessageChannel ch = msg.getChannel();

                Guild g;

                if(ch instanceof GuildChannel) {
                    g = msg.getGuild();
                } else {
                    g = null;
                }

                if(!(ch instanceof GuildChannel) || g.getSelfMember().hasPermission((GuildChannel) ch, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_MANAGE)) {
                    if(canFirstForm(f)) {
                        msg.addReaction(EmojiStore.TWO_PREVIOUS).queue();
                    }

                    if(canPreviousForm(f)) {
                        msg.addReaction(EmojiStore.PREVIOUS).queue();
                    }

                    if(canNextForm(f)) {
                        msg.addReaction(EmojiStore.NEXT).queue();
                    }

                    if(canFinalForm(f)) {
                        msg.addReaction(EmojiStore.TWO_NEXT).queue();
                    }
                }

                StaticStore.executorHandler.postDelayed(5000, () -> {
                    if(img != null && img.exists() && !img.delete()) {
                        StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                    }

                    if(cf != null && cf.exists() && !cf.delete()) {
                        StaticStore.logger.uploadLog("Failed to delete file : "+cf.getAbsolutePath());
                    }
                });

                onSuccess.accept(msg);
            });
        });
    }

    public static void showUnitEmb(Form f, MessageChannel ch, @Nullable Message reference, ConfigHolder config, boolean isFrame, boolean talent, boolean extra, boolean isTrueForm, boolean trueFormPossible, Level lv, boolean treasure, TreasureHolder holder, CommonStatic.Lang.Locale lang, boolean addEmoji, boolean compact, Consumer<Message> onSuccess) throws Exception {
        int level = lv.getLv();
        int levelp = lv.getPlusLv();

        if(level <= 0) {
            if(f.unit.rarity == 0)
                level = 110;
            else {
                if(config == null)
                    level = 30;
                else
                    level = config.defLevel;
            }
        }

        if(level > f.unit.max) {
            levelp = level - f.unit.max;
            level = f.unit.max;

            if(levelp > f.unit.maxp)
                levelp = f.unit.maxp;

            if(levelp < 0)
                levelp = 0;
        }

        lv.setLevel(level);
        lv.setPlusLevel(levelp);

        String l;

        if(levelp == 0)
            l = String.valueOf(level);
        else
            l = level + " + " + levelp;

        File img = generateIcon(f);

        File cf;

        if(extra)
            cf = generateCatfruit(f, lang);
        else
            cf = null;

        EmbedBuilder spec = new EmbedBuilder();

        int c;

        if(f.fid == 0)
            c = StaticStore.rainbow[4];
        else if(f.fid == 1)
            c = StaticStore.rainbow[3];
        else
            c = StaticStore.rainbow[2];

        String desc = "";

        int[] t;

        if(talent && f.du.getPCoin() != null) {
            t = f.du.getPCoin().max.clone();
        } else
            t = null;

        if(t != null) {
            t = handleTalent(f, lv, t);

            lv.setTalents(t);
        }

        if(talent && f.du.getPCoin() != null && t != null && talentExists(t)) {
            desc += LangID.getStringByID("data.unit.talent.info", lang) + "\n";
        } else if(talent && f.du.getPCoin() == null) {
            desc += LangID.getStringByID("data.unit.talent.noTalent", lang) + "\n";
        }

        if(isTrueForm && !trueFormPossible) {
            desc += LangID.getStringByID("data.unit.noTrueForm", lang) + "\n";
        }

        if(holder.differentFromGlobal()) {
            desc += LangID.getStringByID("data.unit.treasure", lang);
        }

        if(!desc.isBlank()) {
            spec.setDescription(desc);
        }

        spec.setColor(c);
        spec.setThumbnail("attachment://icon.png");

        if(compact) {
            spec.setTitle(DataToString.getCompactTitle(f, lang));

            spec.addField(LangID.getStringByID("data.unit.level", lang), l, false);
            spec.addField(LangID.getStringByID("data.compact.healthKb", lang), DataToString.getHealthHitback(f.du, f.unit.lv, talent, lv, treasure, holder), false);
            spec.addField(LangID.getStringByID("data.compact.costCooldownSpeed", lang), DataToString.getCostCooldownSpeed(f.du, isFrame, talent, lv, holder), true);
            spec.addField(DataToString.getRangeTitle(f.du, lang), DataToString.getRange(f.du), true);
            spec.addField(LangID.getStringByID("data.compact.attackTimings", lang), DataToString.getCompactAtkTimings(f.du, talent, lv, isFrame), false);
            spec.addField(LangID.getStringByID("data.compact.damageDPS", lang).replace("_TTT_", DataToString.getSiMu(f.du, lang)), DataToString.getCompactAtk(f.du, talent, f.unit.lv, lv, treasure, holder), false);
        } else {
            spec.setTitle(DataToString.getTitle(f, lang));

            spec.addField(LangID.getStringByID("data.id", lang), DataToString.getID(f.uid.id, f.fid), true);
            spec.addField(LangID.getStringByID("data.unit.level", lang), l, true);

            String hpNormal = DataToString.getHP(f.du, f.unit.lv, talent, lv, false, holder);
            String hpWithTreasure;

            if(treasure) {
                hpWithTreasure = DataToString.getHP(f.du, f.unit.lv, talent, lv, true, holder);
            } else {
                hpWithTreasure = "";
            }

            if(hpWithTreasure.isBlank() || hpNormal.equals(hpWithTreasure)) {
                spec.addField(LangID.getStringByID("data.hp", lang), hpNormal, true);
            } else {
                spec.addField(LangID.getStringByID("data.hp", lang), hpNormal + " <" + hpWithTreasure + ">", true);
            }

            spec.addField(LangID.getStringByID("data.kb", lang), DataToString.getHitback(f.du, talent, lv), true);
            spec.addField(LangID.getStringByID("data.unit.cooldown", lang), DataToString.getCD(f.du,isFrame, talent, lv, holder), true);
            spec.addField(LangID.getStringByID("data.speed", lang), DataToString.getSpeed(f.du, talent, lv), true);
            spec.addField(LangID.getStringByID("data.unit.cost", lang), DataToString.getCost(f.du, talent, lv), true);
            spec.addField(DataToString.getRangeTitle(f.du, lang), DataToString.getRange(f.du), true);
            spec.addField(LangID.getStringByID("data.attackTime", lang), DataToString.getAtkTime(f.du, talent, isFrame, lv), true);
            spec.addField(LangID.getStringByID("data.foreswing", lang), DataToString.getPre(f.du, isFrame), true);
            spec.addField(LangID.getStringByID("data.backswing", lang), DataToString.getPost(f.du, isFrame), true);
            spec.addField(LangID.getStringByID("data.tba", lang), DataToString.getTBA(f.du, talent, lv, isFrame), true);
            spec.addField(LangID.getStringByID("data.attackType", lang), DataToString.getSiMu(f.du, lang), true);

            String dpsNormal = DataToString.getDPS(f.du, f.unit.lv, talent, lv, false, holder);
            String dpsWithTreasure;

            if(treasure) {
                dpsWithTreasure = DataToString.getDPS(f.du, f.unit.lv, talent, lv, true, holder);
            } else {
                dpsWithTreasure = "";
            }

            if(dpsWithTreasure.isBlank() || dpsNormal.equals(dpsWithTreasure)) {
                spec.addField(LangID.getStringByID("data.dps", lang), dpsNormal, true);
            } else {
                spec.addField(LangID.getStringByID("data.dps", lang), dpsNormal + " <" + dpsWithTreasure + ">", true);
            }

            spec.addField(LangID.getStringByID("data.useAbility", lang), DataToString.getAbilT(f.du, lang), true);
            spec.addField(LangID.getStringByID("data.damage", lang), DataToString.getAtk(f.du, f.unit.lv, talent, lv, treasure, holder), true);
        }

        spec.addField(LangID.getStringByID("data.trait", lang), DataToString.getTrait(f.du, talent, lv, true, lang), true);

        MaskUnit du;

        if(f.du.getPCoin() != null)
            if(talent && t != null)
                du = f.du.getPCoin().improve(t);
            else
                du = f.du;
        else
            du = f.du;

        List<String> abis = Interpret.getAbi(du, true, lang, treasure ? du.getTraits() : null, holder);
        abis.addAll(Interpret.getProc(du, !isFrame, true, lang, 1.0, 1.0, treasure, du.getTraits(), holder::getAbilityMultiplier));

        if(compact) {
            abis = mergeImmune(abis, lang);
        }

        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < abis.size(); i++) {
            if(i == abis.size() - 1)
                sb.append("- ").append(abis.get(i));
            else
                sb.append("- ").append(abis.get(i)).append("\n");
        }

        String res = sb.toString();

        if(res.isBlank())
            res = LangID.getStringByID("data.none", lang);
        else if(res.length() > 1024) {
            abis = Interpret.getAbi(du, false, lang, treasure ? du.getTraits() : null, holder);
            abis.addAll(Interpret.getProc(du, !isFrame, false, lang, 1.0, 1.0, treasure, du.getTraits(), holder::getAbilityMultiplier));

            if(compact) {
                abis = mergeImmune(abis, lang);
            }

            sb = new StringBuilder();

            for(int i = 0; i < abis.size(); i++) {
                if(i == abis.size() - 1)
                    sb.append("- ").append(abis.get(i));
                else
                    sb.append("- ").append(abis.get(i)).append("\n");
            }

            res = sb.toString();
        }

        spec.addField(LangID.getStringByID("data.ability", lang), res, false);

        if(extra) {
            String explanation = DataToString.getDescription(f, lang);

            if(explanation != null)
                spec.addField(LangID.getStringByID("data.unit.description", lang), explanation, false);

            String catfruit = DataToString.getCatFruitEvolve(f, lang);

            if(catfruit != null)
                spec.addField(LangID.getStringByID("data.unit.evolve", lang), catfruit, false);

            spec.setImage("attachment://cf.png");
        }

        if(t != null && talentExists(t))
            spec.setFooter(DataToString.getTalent(f.du, lv, lang));

        Command.replyToMessageSafely(ch, "", reference, a -> {
            MessageCreateAction action = a.setEmbeds(spec.build());

            if(img != null)
                action = action.addFiles(FileUpload.fromData(img, "icon.png"));

            if(cf != null)
                action = action.addFiles(FileUpload.fromData(cf, "cf.png"));

            ArrayList<ActionComponent> forms = new ArrayList<>();
            ArrayList<ActionComponent> misc = new ArrayList<>();

            if(addEmoji) {
                if (f.fid - 3 >= 0) {
                    forms.add(Button.secondary("first", LangID.getStringByID("formStat.button.firstForm", lang)).withEmoji(EmojiStore.THREE_PREVIOUS));
                }

                if (f.fid - 2 >= 0) {
                    forms.add(Button.secondary("twoPre", LangID.getStringByID("formStat.button.twoPreviousForm", lang)).withEmoji(EmojiStore.TWO_PREVIOUS));
                }

                if (f.fid - 1 >= 0) {
                    forms.add(Button.secondary("pre", LangID.getStringByID("formStat.button.previousForm", lang)).withEmoji(EmojiStore.PREVIOUS));
                }

                if (f.fid + 1 < f.unit.forms.length) {
                    forms.add(Button.secondary("next", LangID.getStringByID("formStat.button.nextForm", lang)).withEmoji(EmojiStore.NEXT));
                }

                if (f.fid + 2 < f.unit.forms.length) {
                    forms.add(Button.secondary("twoNext", LangID.getStringByID("formStat.button.twoNextForm", lang)).withEmoji(EmojiStore.TWO_NEXT));
                }

                if (f.fid + 3 < f.unit.forms.length) {
                    forms.add(Button.secondary("final", LangID.getStringByID("formStat.button.finalForm", lang)).withEmoji(EmojiStore.THREE_NEXT));
                }

                if(talent && f.du.getPCoin() != null) {
                    misc.add(Button.secondary("talent", LangID.getStringByID("formStat.button.talentInfo", lang)).withEmoji(EmojiStore.NP));
                }

                misc.add(Button.secondary("dps", LangID.getStringByID("ui.button.dps", lang)).withEmoji(Emoji.fromUnicode("\uD83D\uDCC8")));
            }

            if(StaticStore.availableUDP.contains(f.unit.id.id)) {
                misc.add(Button.link("https://thanksfeanor.pythonanywhere.com/UDP/"+Data.trio(f.unit.id.id), "UDP").withEmoji(EmojiStore.UDP));
            }

            ArrayList<LayoutComponent> components = new ArrayList<>();

            if (!forms.isEmpty()) {
                components.add(ActionRow.of(forms));
            }

            if (!misc.isEmpty()) {
                components.add(ActionRow.of(misc));
            }

            if (!components.isEmpty()) {
                action.addComponents(components);
            }

            return action;
        }, msg -> {
            StaticStore.executorHandler.postDelayed(5000, () -> {
                if(img != null && img.exists() && !img.delete()) {
                    StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                }

                if(cf != null && cf.exists() && !cf.delete()) {
                    StaticStore.logger.uploadLog("Failed to delete file : "+cf.getAbsolutePath());
                }
            });

            f.anim.unload();

            onSuccess.accept(msg);
        });
    }

    public static void showTalentEmbed(MessageChannel ch, Message reference, Form unit, boolean isFrame, CommonStatic.Lang.Locale lang) throws Exception {
        if(unit.du == null || unit.du.getPCoin() == null)
            throw new IllegalStateException("E/EntityHandler::showTalentEmbed - Unit which has no talent has been passed");

        Level levels = new Level(unit.du.getPCoin().max.length);

        for(int i = 0; i < unit.du.getPCoin().max.length; i++) {
            levels.getTalents()[i] = 1;
        }

        MaskUnit improved = unit.du.getPCoin().improve(levels.getTalents());

        EmbedBuilder spec = new EmbedBuilder();

        PCoin talent = unit.du.getPCoin();

        File img = generateIcon(unit);

        String unitName = MultiLangCont.get(unit, lang);

        if(unitName == null)
            unitName = Data.trio(unit.unit.id.id);

        spec.setTitle(LangID.getStringByID("data.talent.embed.title", lang).replace("_", unitName));

        for(int i = 0; i < talent.info.size(); i++) {
            if(talent.info.get(i)[13] == 1) {
                spec.setDescription(LangID.getStringByID("data.talent.superTalent.description", lang));

                break;
            }
        }

        for(int i = 0; i < talent.info.size(); i++) {
            String title = DataToString.getTalentTitle(unit.du, i, lang);
            String desc = DataToString.getTalentExplanation(unit.du, improved, i, isFrame, lang);

            if(title.isBlank() || desc.isBlank())
                continue;

            spec.addField(title, desc, false);
        }

        spec.setColor(StaticStore.rainbow[2]);

        if(img != null)
            spec.setThumbnail("attachment://icon.png");

        spec.setFooter(DataToString.accumulateNpCost(unit.du.getPCoin(), lang));

        Command.replyToMessageSafely(ch, "", reference, a -> {
            MessageCreateAction action = a.setEmbeds(spec.build());

            if(img != null)
                action = action.addFiles(FileUpload.fromData(img, "icon.png"));

            return action;
        });
    }

    private static int[] handleTalent(Form f, Level lv, int[] t) {
        int[] res = new int[t.length];
        int[] talents = lv.getTalents();

        for(int i = 0; i < t.length; i++) {
            if(i >= talents.length && talents.length == 0)
                res[i] = t[i];
            else if(i < talents.length)
                res[i] = Math.min(t[i], talents[i]);
        }

        if(f.du.getPCoin() != null) {
            PCoin pc = f.du.getPCoin();

            for(int i = 0; i < res.length; i++) {
                if(pc.info.get(i)[13] == 1 && lv.getLv() + lv.getPlusLv() < 60) {
                    res[i] = 0;
                }
            }
        }

        return res;
    }

    private static boolean talentExists(int[] t) {
        for(int i = 0; i < t.length; i++) {
            if (t[i] > 0)
                return true;
        }

        return false;
    }

    public static void showEnemyEmb(Enemy e, MessageChannel ch, Message reference, boolean isFrame, boolean extra, boolean compact, int[] magnification, TreasureHolder holder, CommonStatic.Lang.Locale lang) throws Exception {
        File img = generateIcon(e);

        EmbedBuilder spec = new EmbedBuilder();

        int c = StaticStore.rainbow[0];

        int[] mag;

        if(magnification.length == 1) {
            mag = new int[2];
            if(magnification[0] <= 0) {
                mag[0] = mag[1] = 100;
            } else {
                mag[0] = mag[1] = magnification[0];
            }
        } else if(magnification.length == 2) {
            mag = magnification;

            if(mag[0] <= 0)
                mag[0] = 100;

            if(mag[1] < 0)
                mag[1] = 0;
        } else {
            mag = new int[2];
        }

        if (e.de.getTraits().contains(TreasureHolder.fullTraits.get(Data.TRAIT_ALIEN))) {
            for(int i = 0; i < mag.length; i++)
                mag[i] = (int) Math.round(mag[i] * (e.de.getStar() == 0 ? holder.getAlienMultiplier() : holder.getStarredAlienMultiplier()));
        }

        spec.setColor(c);
        spec.setThumbnail("attachment://icon.png");

        if(holder.differentFromGlobal()) {
            spec.setDescription(LangID.getStringByID("data.unit.treasure", lang));
        }

        if(compact) {
            spec.setTitle(DataToString.getCompactTitle(e, lang));

            spec.addField(LangID.getStringByID("data.enemy.magnification", lang), DataToString.getMagnification(mag, 100), false);
            spec.addField(LangID.getStringByID("data.compact.healthKb", lang), DataToString.getHealthHitback(e.de, mag[0]), false);
            spec.addField(LangID.getStringByID("data.compact.dropBarrierSpeed", lang), DataToString.getDropBarrierSpeed(e.de, holder, lang), true);
            spec.addField(DataToString.getRangeTitle(e.de, lang), DataToString.getRange(e.de), true);
            spec.addField(LangID.getStringByID("data.compact.attackTimings", lang), DataToString.getCompactAtkTimings(e.de, isFrame), false);
            spec.addField(LangID.getStringByID("data.compact.damageDPS", lang).replace("_TTT_", DataToString.getSiMu(e.de, lang)), DataToString.getCompactAtk(e.de, mag[1]), false);
            spec.addField(LangID.getStringByID("data.trait", lang), DataToString.getTrait(e.de, true, lang), false);
        } else {
            spec.setTitle(DataToString.getTitle(e, lang));

            spec.addField(LangID.getStringByID("data.id", lang), DataToString.getID(e.id.id), true);
            spec.addField(LangID.getStringByID("data.enemy.magnification", lang), DataToString.getMagnification(mag, 100), true);
            spec.addField(LangID.getStringByID("data.hp", lang), DataToString.getHP(e.de, mag[0]), true);
            spec.addField(LangID.getStringByID("data.kb", lang), DataToString.getHitback(e.de), true);
            spec.addField(LangID.getStringByID("data.enemy.barrier", lang), DataToString.getBarrier(e.de, lang), true);
            spec.addField(LangID.getStringByID("data.speed", lang), DataToString.getSpeed(e.de), true);
            spec.addField(LangID.getStringByID("data.attackTime", lang), DataToString.getAtkTime(e.de, isFrame), true);
            spec.addField(LangID.getStringByID("data.foreswing", lang), DataToString.getPre(e.de, isFrame), true);
            spec.addField(LangID.getStringByID("data.backswing", lang), DataToString.getPost(e.de, isFrame), true);
            spec.addField(LangID.getStringByID("data.tba", lang), DataToString.getTBA(e.de, isFrame), true);
            spec.addField(LangID.getStringByID("data.enemy.drop", lang), DataToString.getDrop(e.de, holder), true);
            spec.addField(DataToString.getRangeTitle(e.de, lang), DataToString.getRange(e.de), true);
            spec.addField(LangID.getStringByID("data.attackType", lang), DataToString.getSiMu(e.de, lang), true);
            spec.addField(LangID.getStringByID("data.dps", lang), DataToString.getDPS(e.de, mag[1]), true);
            spec.addField(LangID.getStringByID("data.useAbility", lang), DataToString.getAbilT(e.de, lang), true);
            spec.addField(LangID.getStringByID("data.damage", lang), DataToString.getAtk(e.de, mag[1]), true);
            spec.addField(LangID.getStringByID("data.trait", lang), DataToString.getTrait(e.de, true, lang), true);
        }

        List<String> abis = Interpret.getAbi(e.de, true, lang, null, null);
        abis.addAll(Interpret.getProc(e.de, !isFrame, true, lang, mag[0] / 100.0, mag[1] / 100.0, false, null, null));

        if(compact) {
            abis = mergeImmune(abis, lang);
        }

        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < abis.size(); i++) {
            if(i == abis.size() - 1)
                sb.append(abis.get(i));
            else
                sb.append(abis.get(i)).append("\n");
        }

        String res = sb.toString();

        if(res.isBlank())
            res = LangID.getStringByID("data.none", lang);
        else if(res.length() > 1024) {
            abis = Interpret.getAbi(e.de, false, lang, null, null);
            abis.addAll(Interpret.getProc(e.de, !isFrame, false, lang, mag[0] / 100.0, mag[1] / 100.0, false, null, null));

            if(compact) {
                abis = mergeImmune(abis, lang);
            }

            sb = new StringBuilder();

            for(int i = 0; i < abis.size(); i++) {
                if(i == abis.size() - 1)
                    sb.append(abis.get(i));
                else
                    sb.append(abis.get(i)).append("\n");
            }

            res = sb.toString();
        }

        spec.addField(LangID.getStringByID("data.ability", lang), res, false);

        if(extra) {
            String explanation = DataToString.getDescription(e, lang);

            if(explanation != null) {
                spec.addField(LangID.getStringByID("data.enemy.description", lang), explanation, false);
            }
        }

        spec.setFooter(LangID.getStringByID("enemyStat.source", lang));

        Command.replyToMessageSafely(ch, "", reference, a -> {
            a = a.setEmbeds(spec.build());

            if (img != null) {
                a = a.addFiles(FileUpload.fromData(img, "icon.png"));
            }

            return a.addComponents(ActionRow.of(Button.secondary("dps", LangID.getStringByID("ui.button.dps", lang)).withEmoji(Emoji.fromUnicode("\uD83D\uDCC8"))));
        }, msg -> {
            if (img != null && img.exists() && !img.delete()) {
                StaticStore.logger.uploadLog("Failed to delete file : " + img.getAbsolutePath());
            }

            StaticStore.putHolder(reference.getAuthor().getId(), new EnemyButtonHolder(e, reference, msg, holder, mag, compact, lang, ch.getId()));

            e.anim.unload();
        });
    }

    public static void performEnemyEmb(Enemy e, GenericCommandInteractionEvent event, boolean isFrame, boolean extra, int[] magnification, TreasureHolder holder, CommonStatic.Lang.Locale lang) throws Exception {
        File img = generateIcon(e);

        EmbedBuilder spec = new EmbedBuilder();

        int c = StaticStore.rainbow[0];

        int[] mag = new int[2];

        if(magnification.length == 1) {
            if(magnification[0] <= 0) {
                mag[0] = mag[1] = 100;
            } else {
                mag[0] = mag[1] = magnification[0];
            }
        } else if(magnification.length == 2) {
            mag = magnification;

            if(mag[0] <= 0)
                mag[0] = 100;

            if(mag[1] < 0)
                mag[1] = 0;
        }

        if (e.de.getTraits().contains(TreasureHolder.fullTraits.get(Data.TRAIT_ALIEN))) {
            for(int i = 0; i < mag.length; i++)
                mag[i] = (int) Math.round(mag[i] * (e.de.getStar() == 0 ? holder.getAlienMultiplier() : holder.getStarredAlienMultiplier()));
        }

        if(holder.differentFromGlobal()) {
            spec.setDescription(LangID.getStringByID("data.unit.treasure", lang));
        }

        spec.setTitle(DataToString.getTitle(e, lang));
        spec.setColor(c);
        spec.setThumbnail("attachment://icon.png");
        spec.addField(LangID.getStringByID("data.id", lang), DataToString.getID(e.id.id), true);
        spec.addField(LangID.getStringByID("data.enemy.magnification", lang), DataToString.getMagnification(mag, 100), true);
        spec.addField(LangID.getStringByID("data.hp", lang), DataToString.getHP(e.de, mag[0]), true);
        spec.addField(LangID.getStringByID("data.kb", lang), DataToString.getHitback(e.de), true);
        spec.addField(LangID.getStringByID("data.enemy.barrier", lang), DataToString.getBarrier(e.de, lang), true);
        spec.addField(LangID.getStringByID("data.speed", lang), DataToString.getSpeed(e.de), true);
        spec.addField(LangID.getStringByID("data.attackTime", lang), DataToString.getAtkTime(e.de, isFrame), true);
        spec.addField(LangID.getStringByID("data.foreswing", lang), DataToString.getPre(e.de, isFrame), true);
        spec.addField(LangID.getStringByID("data.backswing", lang), DataToString.getPost(e.de, isFrame), true);
        spec.addField(LangID.getStringByID("data.tba", lang), DataToString.getTBA(e.de, isFrame), true);
        spec.addField(LangID.getStringByID("data.enemy.drop", lang), DataToString.getDrop(e.de, holder), true);
        spec.addField(DataToString.getRangeTitle(e.de, lang), DataToString.getRange(e.de), true);
        spec.addField(LangID.getStringByID("data.attackType", lang), DataToString.getSiMu(e.de, lang), true);
        spec.addField(LangID.getStringByID("data.dps", lang), DataToString.getDPS(e.de, mag[1]), true);
        spec.addField(LangID.getStringByID("data.useAbility", lang), DataToString.getAbilT(e.de, lang), true);
        spec.addField(LangID.getStringByID("data.damage", lang), DataToString.getAtk(e.de, mag[1]), true);
        spec.addField(LangID.getStringByID("data.trait", lang), DataToString.getTrait(e.de, true, lang), true);

        List<String> abis = Interpret.getAbi(e.de, true, lang, null, null);
        abis.addAll(Interpret.getProc(e.de, !isFrame, true, lang, mag[0] / 100.0, mag[1] / 100.0, false, null, null));

        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < abis.size(); i++) {
            if(i == abis.size() - 1)
                sb.append(abis.get(i));
            else
                sb.append(abis.get(i)).append("\n");
        }

        String res = sb.toString();

        if(res.isBlank())
            res = LangID.getStringByID("data.none", lang);
        else if(res.length() > 1024) {
            abis = Interpret.getAbi(e.de, false, lang, null, null);
            abis.addAll(Interpret.getProc(e.de, !isFrame, false, lang, mag[0] / 100.0, mag[1] / 100.0, false, null, null));

            sb = new StringBuilder();

            for(int i = 0; i < abis.size(); i++) {
                if(i == abis.size() - 1)
                    sb.append(abis.get(i));
                else
                    sb.append(abis.get(i)).append("\n");
            }

            res = sb.toString();
        }

        spec.addField(LangID.getStringByID("data.ability", lang), res, false);

        if(extra) {
            String explanation = DataToString.getDescription(e, lang);

            if(explanation != null) {
                spec.addField(LangID.getStringByID("data.enemy.description", lang), explanation, false);
            }
        }

        spec.setFooter(LangID.getStringByID("enemyStat.source", lang), null);

        ReplyCallbackAction action = event.deferReply().addEmbeds(spec.build());

        if(img != null)
            action = action.addFiles(FileUpload.fromData(img, "icon.png"));

        action.queue(h -> {
            if(img != null && img.exists() && !img.delete()) {
                StaticStore.logger.uploadLog("Failed to remove file : "+img.getAbsolutePath());
            }
        }, err -> {
            StaticStore.logger.uploadErrorLog(err, "E/EntityHandler::performEnemyEmb - Error happened while trying to dispaly enemy embed");

            if(img != null && img.exists() && !img.delete()) {
                StaticStore.logger.uploadLog("Failed to remove file : "+img.getAbsolutePath());
            }
        });

        e.anim.unload();
    }

    private static File generateIcon(Enemy e) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+temp.getAbsolutePath());
                return null;
            }
        }

        File img = StaticStore.generateTempFile(temp, "result", ".png", false);

        if(img == null)
            return null;

        FakeImage image;

        if(e.anim.getEdi() != null && e.anim.getEdi().getImg() != null)
            image = e.anim.getEdi().getImg();
        else
            return null;

        CountDownLatch waiter = new CountDownLatch(1);

        StaticStore.renderManager.createRenderer(image.getWidth(), image.getHeight(), temp, connector -> {
            connector.queue(g -> {
                g.drawImage(image, 0f, 0f);

                return null;
            });

            return null;
        }, progress -> img, () -> {
            waiter.countDown();

            return null;
        });

        waiter.await();

        return img;
    }

    private static File generateIcon(Form f) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+temp.getAbsolutePath());
                return null;
            }
        }

        File img = StaticStore.generateTempFile(temp, "result", ".png", false);

        if(img == null) {
            return null;
        }

        FakeImage image;

        if(f.anim.getUni() != null)
            image = f.anim.getUni().getImg();
        else if(f.anim.getEdi() != null)
            image = f.anim.getEdi().getImg();
        else
            return null;
        
        CountDownLatch waiter = new CountDownLatch(1);
        
        StaticStore.renderManager.createRenderer(image.getWidth(), image.getHeight(), temp, connector -> {
            connector.queue(g -> {
                g.drawImage(image, 0f, 0f);
                
                return null;
            });
            
            return null;
        }, progress -> img, () -> {
            waiter.countDown();
            
            return null;
        });
        
        waiter.await();

        return img;
    }

    private static File generateCatfruit(Form f, CommonStatic.Lang.Locale lang) throws Exception {
        if(f.unit == null)
            return null;

        if(f.unit.info.evo == null)
            return null;

        File tmp = new File("./temp");

        if(!tmp.exists()) {
            boolean res = tmp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+tmp.getAbsolutePath());
                return null;
            }
        }

        File img = StaticStore.generateTempFile(tmp, "result", ".png", false);

        if(img == null) {
            return null;
        }

        CountDownLatch waiter = new CountDownLatch(1);

        float[] trueFormText = font.measureDimension(LangID.getStringByID("data.unit.trueForm", lang));
        float[] ultraFormText = font.measureDimension(LangID.getStringByID("data.unit.ultraForm", lang));

        int w = Math.round(Math.max(600f, trueFormText[2]));
        float th = catFruitTextGap + trueFormText[3] + catFruitTextGap + 150f;

        if (f.unit.info.zeroEvo != null) {
            th += catFruitTextGap + ultraFormText[3] + catFruitTextGap + 150f;
        }

        int h = Math.round(th);

        StaticStore.renderManager.createRenderer(w, h, tmp, connector -> {
            connector.queue(g -> {
                g.setFontModel(font);

                g.setStroke(2f, GLGraphics.LineEndMode.VERTICAL);
                g.setColor(47, 49, 54, 255);

                g.fillRect(0, 0, w, h);

                g.translate(0f, catFruitTextGap);

                g.setColor(238, 238, 238, 255);
                g.drawText(LangID.getStringByID("data.unit.trueForm", lang), 0f, 0f, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.TOP);

                g.translate(0f, trueFormText[3] + catFruitTextGap);

                g.setColor(238, 238, 238, 128);

                g.drawRect(0, 0, 600, 150);

                for(int i = 1; i < 6; i++) {
                    g.drawLine(100 * i, 0, 100* i , 150);
                }

                g.drawLine(0, 100, 600, 100);

                g.setColor(238, 238, 238, 255);

                for(int i = 0; i < 6; i++) {
                    if(i == 0) {
                        VFile vf = VFile.get("./org/page/catfruit/xp.png");

                        if(vf != null) {
                            FakeImage icon = vf.getData().getImg();

                            g.drawImage(icon, 510, 10, 80, 80);
                            g.drawText(String.valueOf(f.unit.info.xp), 550, 125, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.MIDDLE);
                        }
                    } else {
                        if(f.unit.info.evo[i - 1][0] != 0) {
                            VFile vf = VFile.get("./org/page/catfruit/gatyaitemD_"+f.unit.info.evo[i - 1][0]+"_f.png");

                            if(vf != null) {
                                FakeImage icon = vf.getData().getImg();

                                g.drawImage(icon, 100 * (i-1)+5, 10, 80, 80);
                                g.drawText(String.valueOf(f.unit.info.evo[i - 1][1]), 100 * (i-1) + 50, 125, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.MIDDLE);
                            }
                        }
                    }
                }

                if (f.unit.info.zeroEvo != null) {
                    g.translate(0f, 150 + catFruitTextGap);

                    g.setColor(238, 238, 238, 255);
                    g.drawText(LangID.getStringByID("data.unit.ultraForm", lang), 0f, 0f, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.TOP);

                    g.translate(0f, ultraFormText[3] + catFruitTextGap);

                    g.setColor(238, 238, 238, 128);

                    g.drawRect(0, 0, 600, 150);

                    for(int i = 1; i < 6; i++) {
                        g.drawLine(100 * i, 0, 100* i , 150);
                    }

                    g.drawLine(0, 100, 600, 100);

                    g.setColor(238, 238, 238, 255);

                    for(int i = 0; i < 6; i++) {
                        if(i == 0) {
                            VFile vf = VFile.get("./org/page/catfruit/xp.png");

                            if(vf != null) {
                                FakeImage icon = vf.getData().getImg();

                                g.drawImage(icon, 510, 10, 80, 80);
                                g.drawText(String.valueOf(f.unit.info.zeroXp), 550, 125, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.MIDDLE);
                            }
                        } else {
                            if(f.unit.info.zeroEvo[i - 1][0] != 0) {
                                VFile vf = VFile.get("./org/page/catfruit/gatyaitemD_"+f.unit.info.zeroEvo[i - 1][0]+"_f.png");

                                if(vf != null) {
                                    FakeImage icon = vf.getData().getImg();

                                    g.drawImage(icon, 100 * (i-1)+5, 10, 80, 80);
                                    g.drawText(String.valueOf(f.unit.info.zeroEvo[i - 1][1]), 100 * (i-1) + 50, 125, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.MIDDLE);
                                }
                            }
                        }
                    }
                }

                return null;
            });

            return null;
        }, progress -> img, () -> {
            waiter.countDown();

            return null;
        });

        waiter.await();

        return img;
    }

    public static void showStageEmb(Stage st, MessageChannel ch, Message reference, boolean isFrame, boolean isExtra, boolean isCompact, int level, TreasureHolder holder, CommonStatic.Lang.Locale lang, Consumer<Message> onSuccess) throws Exception {
        StageMap stm = st.getCont();

        int sta;
        int stmMagnification;

        if(stm == null) {
            sta = 0;
            stmMagnification = 100;
        } else {
            MapColc mc = stm.getCont();

            if(mc != null && mc.getSID().equals("000003") && stm.id.id == 9) {
                if(st.id.id == 49) {
                    sta = 1;
                } else if(st.id.id == 50) {
                    sta = 2;
                } else {
                    sta = Math.min(Math.max(level-1, 0), st.getCont().stars.length-1);
                }
            } else {
                sta = Math.min(Math.max(level-1, 0), st.getCont().stars.length-1);
            }

            stmMagnification = stm.stars[sta];
        }

        File img = generateScheme(st, isFrame, lang, stmMagnification, holder);

        EmbedBuilder spec = new EmbedBuilder();

        if(!(st.info instanceof DefStageInfo) || ((DefStageInfo) st.info).diff == -1)
            spec.setColor(new Color(217, 217, 217).getRGB());
        else
            spec.setColor(DataToString.getDifficultyColor(((DefStageInfo) st.info).diff));

        String name = "";

        if(stm == null) {
            ch.sendMessageEmbeds(spec.build()).queue();

            return;
        }

        MapColc mc = stm.getCont();

        String mcName = MultiLangCont.get(mc, lang);

        if(mcName == null || mcName.isBlank())
            mcName = mc.getSID();

        name += mcName+" - ";

        String stmName = MultiLangCont.get(stm, lang);

        if(stmName == null || stmName.isBlank())
            if(stm.id != null)
                stmName = Data.trio(stm.id.id);
            else
                stmName = "Unknown";

        name += stmName + " - ";

        String stName = MultiLangCont.get(st, lang);

        if(stName == null || stName.isBlank())
            if(st.id != null)
                stName = Data.trio(st.id.id);
            else
                stName = "Unknown";

        name += stName;

        spec.setTitle(name);

        if(holder.differentFromGlobal()) {
            spec.setDescription(LangID.getStringByID("data.unit.treasure", lang));
        }

        if(isCompact) {
            spec.addField(LangID.getStringByID("data.compact.idDifficultyLevel", lang), DataToString.getIdDifficultyLevel(st, sta, lang), false);

            String secondField = DataToString.getEnergyBaseXP(st, holder, lang);

            if(secondField.contains("!!drink!!")) {
                secondField = secondField.replace("!!drink!!", "");

                spec.addField(LangID.getStringByID("data.compact.cataminBaseXP", lang), secondField, false);
            } else {
                spec.addField(LangID.getStringByID("data.compact.energyBaseXP", lang), secondField, false);
            }

            spec.addField(LangID.getStringByID("data.compact.limitContinuableLength", lang), DataToString.getEnemyContinuableLength(st, lang), false);
            spec.addField(LangID.getStringByID("data.compact.musicBackgroundCastle", lang).replace("_BBB_", String.valueOf(st.mush)), DataToString.getMusciBackgroundCastle(st, lang), false);
            spec.addField(LangID.getStringByID("data.stage.guardBarrier", lang), DataToString.getBossGuard(st, lang), false);
            spec.setFooter(LangID.getStringByID("data.compact.minimumRespawn", lang).replace("_RRR_", DataToString.getMinSpawn(st, isFrame)));
        } else {
            spec.addField(LangID.getStringByID("data.id", lang), DataToString.getStageCode(st), true);
            spec.addField(LangID.getStringByID("data.unit.level", lang), DataToString.getStar(st, sta), true);

            String energy = DataToString.getEnergy(st, lang);

            if(energy.endsWith("!!drink!!")) {
                spec.addField(LangID.getStringByID("data.stage.catamin.title", lang), energy.replace("!!drink!!", ""), true);
            } else {
                spec.addField(LangID.getStringByID("data.stage.energy", lang), energy, true);
            }

            spec.addField(LangID.getStringByID("data.stage.baseHealth", lang), DataToString.getBaseHealth(st), true);
            spec.addField(LangID.getStringByID("data.stage.xp", lang), DataToString.getXP(st, holder), true);
            spec.addField(LangID.getStringByID("data.stage.difficulty", lang), DataToString.getDifficulty(st, lang), true);
            spec.addField(LangID.getStringByID("data.stage.continuable", lang), DataToString.getContinuable(st, lang), true);
            spec.addField(LangID.getStringByID("data.stage.music", lang), DataToString.getMusic(st, lang), true);
            spec.addField(DataToString.getMusicChange(st), DataToString.getMusic1(st, lang) , true);
            spec.addField(LangID.getStringByID("data.stage.enemyLimit", lang), DataToString.getMaxEnemy(st), true);
            spec.addField(LangID.getStringByID("data.stage.background", lang), DataToString.getBackground(st, lang),true);
            spec.addField(LangID.getStringByID("data.stage.castle", lang), DataToString.getCastle(st, lang), true);
            spec.addField(LangID.getStringByID("data.stage.length", lang), DataToString.getLength(st), true);
            spec.addField(LangID.getStringByID("data.stage.minimumRespawn", lang), DataToString.getMinSpawn(st, isFrame), true);
            spec.addField(LangID.getStringByID("data.stage.guardBarrier", lang), DataToString.getBossGuard(st, lang), true);
        }

        ArrayList<String> limit = DataToString.getLimit(st.getLim(sta), isFrame, lang);

        if(!limit.isEmpty()) {
            StringBuilder sb = new StringBuilder();

            for(int i = 0; i < limit.size(); i ++) {
                sb.append(limit.get(i));

                if(i < limit.size()-1)
                    sb.append("\n");
            }

            spec.addField(LangID.getStringByID("data.stage.limit.title", lang), sb.toString(), false);
        }

        if(isExtra) {
            List<String> misc = DataToString.getMiscellaneous(st, lang);

            if(!misc.isEmpty()) {
                StringBuilder sbuilder = new StringBuilder();

                for(int i = 0; i < misc.size(); i++) {
                    sbuilder.append("- ").append(misc.get(i));

                    if(i < misc.size() - 1) {
                        sbuilder.append("\n");
                    }
                }

                spec.addField(LangID.getStringByID("data.stage.misc.title", lang), sbuilder.toString(), false);
            }

            String exData = DataToString.getEXStage(st, lang);

            if(exData != null) {
                spec.addField(LangID.getStringByID("data.stage.misc.exStage", lang), exData, false);
            }

            String materials = DataToString.getMaterialDrop(st, sta, lang);

            if(materials != null) {
                spec.addField(LangID.getStringByID("data.stage.material.title", lang), materials, false);
            }
        }

        String drops = DataToString.getRewards(st, lang);

        if(drops != null) {
            if(drops.endsWith("!!number!!")) {
                spec.addField(LangID.getStringByID("data.stage.reward.type.number", lang), drops.replace("!!number!!", ""), false);
            } else if(drops.endsWith("!!nofail!!")) {
                spec.addField(LangID.getStringByID("data.stage.reward.type.chance.guaranteed", lang), drops.replace("!!nofail!!", ""), false);
            } else {
                spec.addField(LangID.getStringByID("data.stage.reward.type.chance.normal", lang), drops, false);
            }
        }

        String score = DataToString.getScoreDrops(st, lang);

        if(score != null) {
            spec.addField(LangID.getStringByID("data.stage.reward.type.score", lang), score, false);
        }

        if(img != null) {
            spec.addField(LangID.getStringByID("data.stage.scheme", lang), "** **", false);
            spec.setImage("attachment://scheme.png");
        }

        Command.replyToMessageSafely(ch, "", reference, a -> {
            MessageCreateAction action = a.setEmbeds(spec.build());

            if(img != null)
                action = action.addFiles(FileUpload.fromData(img, "scheme.png"));

            ArrayList<Button> buttons = new ArrayList<>();

            buttons.add(Button.secondary("castle", LangID.getStringByID("stageInfo.button.castle", lang)).withEmoji(EmojiStore.CASTLE));
            buttons.add(Button.secondary("bg", LangID.getStringByID("stageInfo.button.background", lang)).withEmoji(EmojiStore.BACKGROUND));

            if(st.mus0 != null) {
                buttons.add(Button.secondary("music", LangID.getStringByID("stageInfo.button.music", lang)).withEmoji(EmojiStore.MUSIC));
            }

            if(hasTwoMusic(st)) {
                buttons.add(Button.secondary("music2", LangID.getStringByID("stageInfo.button.secondMusic", lang)).withEmoji(EmojiStore.MUSIC_BOSS));
            }

            action = action.setComponents(ActionRow.of(buttons));

            return action;
        }, msg -> {
            StaticStore.executorHandler.postDelayed(5000, () -> {
                if(img != null && img.exists() && !img.delete()) {
                    StaticStore.logger.uploadLog("Can't delete file : "+img.getAbsolutePath());
                }
            });

            onSuccess.accept(msg);
        });
    }

    public static void performStageEmb(Stage st, GenericCommandInteractionEvent event, boolean isFrame, boolean isExtra, int level, CommonStatic.Lang.Locale lang, TreasureHolder holder, Consumer<Message> onSuccess) throws Exception {
        StageMap stm = st.getCont();

        int sta;
        int stmMagnification;

        if(stm == null) {
            sta = 0;
            stmMagnification = 100;
        } else {
            MapColc mc = stm.getCont();

            if(mc != null && mc.getSID().equals("000003") && stm.id.id == 9) {
                if(st.id.id == 49) {
                    sta = 1;
                } else if(st.id.id == 50) {
                    sta = 2;
                } else {
                    sta = Math.min(Math.max(level-1, 0), st.getCont().stars.length-1);
                }
            } else {
                sta = Math.min(Math.max(level-1, 0), st.getCont().stars.length-1);
            }

            stmMagnification = stm.stars[sta];
        }

        File img = generateScheme(st, isFrame, lang, stmMagnification, holder);

        EmbedBuilder spec = new EmbedBuilder();

        if(!(st.info instanceof DefStageInfo) || ((DefStageInfo) st.info).diff == -1)
            spec.setColor(new Color(217, 217, 217).getRGB());
        else
            spec.setColor(DataToString.getDifficultyColor(((DefStageInfo) st.info).diff));

        String name = "";

        if(stm == null)
            return;

        MapColc mc = stm.getCont();

        String mcName = MultiLangCont.get(mc, lang);

        if(mcName == null || mcName.isBlank())
            mcName = mc.getSID();

        name += mcName+" - ";

        String stmName = MultiLangCont.get(stm, lang);

        if(stmName == null || stmName.isBlank())
            if(stm.id != null)
                stmName = Data.trio(stm.id.id);
            else
                stmName = "Unknown";

        name += stmName + " - ";

        String stName = MultiLangCont.get(st, lang);

        if(stName == null || stName.isBlank())
            if(st.id != null)
                stName = Data.trio(st.id.id);
            else
                stName = "Unknown";

        name += stName;

        spec.setTitle(name);
        spec.addField(LangID.getStringByID("data.id", lang), DataToString.getStageCode(st), true);
        spec.addField(LangID.getStringByID("data.unit.level", lang), DataToString.getStar(st, sta), true);

        String energy = DataToString.getEnergy(st, lang);

        if(energy.endsWith("!!drink!!")) {
            spec.addField(LangID.getStringByID("data.stage.catamin.title", lang), energy.replace("!!drink!!", ""), true);
        } else {
            spec.addField(LangID.getStringByID("data.stage.energy", lang), energy, true);
        }

        spec.addField(LangID.getStringByID("data.stage.baseHealth", lang), DataToString.getBaseHealth(st), true);
        spec.addField(LangID.getStringByID("data.stage.xp", lang), DataToString.getXP(st, holder), true);
        spec.addField(LangID.getStringByID("data.stage.difficulty", lang), DataToString.getDifficulty(st, lang), true);
        spec.addField(LangID.getStringByID("data.stage.continuable", lang), DataToString.getContinuable(st, lang), true);
        spec.addField(LangID.getStringByID("data.stage.music", lang), DataToString.getMusic(st, lang), true);
        spec.addField(DataToString.getMusicChange(st), DataToString.getMusic1(st, lang) , true);
        spec.addField(LangID.getStringByID("data.stage.enemyLimit", lang), DataToString.getMaxEnemy(st), true);
        spec.addField(LangID.getStringByID("data.stage.background", lang), DataToString.getBackground(st, lang),true);
        spec.addField(LangID.getStringByID("data.stage.castle", lang), DataToString.getCastle(st, lang), true);
        spec.addField(LangID.getStringByID("data.stage.length", lang), DataToString.getLength(st), true);
        spec.addField(LangID.getStringByID("data.stage.minimumRespawn", lang), DataToString.getMinSpawn(st, isFrame), true);

        ArrayList<String> limit = DataToString.getLimit(st.getLim(sta), isFrame, lang);

        if(!limit.isEmpty()) {
            StringBuilder sb = new StringBuilder();

            for(int i = 0; i < limit.size(); i ++) {
                sb.append(limit.get(i));

                if(i < limit.size()-1)
                    sb.append("\n");
            }

            spec.addField(LangID.getStringByID("data.stage.limit.title", lang), sb.toString(), false);
        }

        if(isExtra) {
            List<String> misc = DataToString.getMiscellaneous(st, lang);

            if(!misc.isEmpty()) {
                StringBuilder sbuilder = new StringBuilder();

                for(int i = 0; i < misc.size(); i++) {
                    sbuilder.append("- ").append(misc.get(i));

                    if(i < misc.size() - 1) {
                        sbuilder.append("\n");
                    }
                }

                spec.addField(LangID.getStringByID("data.stage.misc.title", lang), sbuilder.toString(), false);
            }

            String exData = DataToString.getEXStage(st, lang);

            if(exData != null) {
                spec.addField(LangID.getStringByID("data.stage.misc.exStage", lang), exData, false);
            }
        }

        String drops = DataToString.getRewards(st, lang);

        if(drops != null) {
            if(drops.endsWith("!!number!!")) {
                spec.addField(LangID.getStringByID("data.stage.reward.type.number", lang), drops.replace("!!number!!", ""), false);
            } else if(drops.endsWith("!!nofail!!")) {
                spec.addField(LangID.getStringByID("data.stage.reward.type.chance.guaranteed", lang), drops.replace("!!nofail!!", ""), false);
            } else {
                spec.addField(LangID.getStringByID("data.stage.reward.type.chance.normal", lang), drops, false);
            }
        }

        String score = DataToString.getScoreDrops(st, lang);

        if(score != null) {
            spec.addField(LangID.getStringByID("data.stage.reward.type.score", lang), score, false);
        }

        if(img != null) {
            spec.addField(LangID.getStringByID("data.stage.scheme", lang), "** **", false);
            spec.setImage("attachment://scheme.png");
        }

        ReplyCallbackAction action = event.deferReply().addEmbeds(spec.build());

        if(img != null)
            action = action.addFiles(FileUpload.fromData(img, "scheme.png"));

        action.queue(hook ->
            hook.retrieveOriginal().queue(msg -> {
                if (msg == null)
                    return;

                MessageChannel ch = msg.getChannel();

                Guild g;

                if(ch instanceof GuildChannel) {
                    g = msg.getGuild();
                } else {
                    g = null;
                }

                if(!(ch instanceof GuildChannel) || g.getSelfMember().hasPermission((GuildChannel) ch, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_MANAGE)) {
                    msg.addReaction(EmojiStore.CASTLE).queue();
                    msg.addReaction(EmojiStore.BACKGROUND).queue();

                    if(st.mus0 != null) {
                        msg.addReaction(EmojiStore.MUSIC).queue();
                    }

                    if(hasTwoMusic(st)) {
                        msg.addReaction(EmojiStore.MUSIC_BOSS).queue();
                    }
                }

                StaticStore.executorHandler.postDelayed(5000, () -> {
                    if(img != null && img.exists() && !img.delete()) {
                        StaticStore.logger.uploadLog("Can't delete file : "+img.getAbsolutePath());
                    }
                });

                onSuccess.accept(msg);
            })
        );
    }

    private static File generateScheme(Stage st, boolean isFrame, CommonStatic.Lang.Locale lang, int star, TreasureHolder holder) throws Exception {
        File temp = new File("./temp/");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+temp.getAbsolutePath());
                return null;
            }
        }

        File img = StaticStore.generateTempFile(temp, "scheme", ".png", false);

        if(img == null) {
            return null;
        }

        boolean needBoss = false;
        boolean needRespect = false;
        boolean needCount = false;

        for(int i = st.data.datas.length - 1; i >= 0; i--) {
            SCDef.Line line = st.data.datas[i];

            if(!needBoss && line.boss != 0)
                needBoss = true;

            if(!needRespect && (line.spawn_0 < 0 || line.spawn_1 < 0))
                needRespect = true;

            if(!needCount && line.kill_count != 0)
                needCount = true;
        }

        ArrayList<String> enemies = new ArrayList<>();
        ArrayList<String> numbers = new ArrayList<>();
        ArrayList<String> magnifs = new ArrayList<>();
        ArrayList<String> isBoss = new ArrayList<>();
        ArrayList<String> baseHealth = new ArrayList<>();
        ArrayList<String> startRespawn = new ArrayList<>();
        ArrayList<String> layers = new ArrayList<>();
        ArrayList<String> respects = new ArrayList<>();
        ArrayList<String> killCounts = new ArrayList<>();

        for(int i = st.data.datas.length - 1; i >= 0; i--) {
            SCDef.Line line = st.data.datas[i];

            AbEnemy enemy = line.enemy.get();

            if(enemy == null)
                continue;

            int hp = line.multiple;
            int atk = line.mult_atk;

            if(enemy instanceof Enemy) {
                String eName = MultiLangCont.get(enemy, lang);

                if(eName == null || eName.isBlank())
                    eName = ((Enemy) enemy).names.toString();

                if(eName.isBlank())
                    eName = DataToString.getPackName(((Enemy) enemy).id.pack, lang)+" - "+Data.trio(((Enemy) enemy).id.id);

                enemies.add(eName);

                if(((Enemy) enemy).de.getTraits().contains(UserProfile.getBCData().traits.get(Data.TRAIT_ALIEN)) && ((Enemy) enemy).de.getStar() == 0) {
                    hp = (int) Math.round(hp * holder.getAlienMultiplier());
                    atk = (int) Math.round(atk * holder.getAlienMultiplier());
                } else if(((Enemy) enemy).de.getStar() == 1) {
                    hp = (int) Math.round(hp * holder.getStarredAlienMultiplier());
                    atk = (int) Math.round(atk * holder.getStarredAlienMultiplier());
                }
            } else if(enemy instanceof EneRand) {
                String name = ((EneRand) enemy).name;

                if(name == null || name.isBlank()) {
                    name = DataToString.getPackName(((EneRand) enemy).id.pack, lang)+"-"+Data.trio(((EneRand) enemy).id.id);
                }

                enemies.add(name);
            } else
                continue;

            String number;

            if(line.number == 0)
                number = LangID.getStringByID("data.stage.infinite", lang);
            else
                number = String.valueOf(line.number);

            numbers.add(number);

            int[] magnification;

            if(st.getCont() != null && st.getCont().getCont() != null && st.getCont().getCont().getSID().equals("000003") && st.getCont().id.id == 9) {
                magnification = new int[] {100, 100};
            } else {

                magnification = new int[] {hp, atk};
            }

            String magnif = DataToString.getMagnification(magnification, star);

            magnifs.add(magnif);

            String start;

            if(line.spawn_1 == 0)
                if(isFrame)
                    start = Math.abs(line.spawn_0)+"f";
                else
                    start = df.format(Math.abs(line.spawn_0) / 30.0)+"s";
            else {
                int minSpawn = Math.abs(Math.min(line.spawn_0, line.spawn_1));
                int maxSpawn = Math.abs(Math.max(line.spawn_0, line.spawn_1));

                if(isFrame)
                    start = minSpawn+"f ~ "+maxSpawn+"f";
                else
                    start = df.format(minSpawn/30.0)+"s ~ "+df.format(maxSpawn/30.0)+"s";
            }

            String respawn;

            if(line.respawn_0 == line.respawn_1)
                if(isFrame)
                    respawn = line.respawn_0+"f";
                else
                    respawn = df.format(line.respawn_0/30.0)+"s";
            else {
                int minSpawn = Math.min(line.respawn_0, line.respawn_1);
                int maxSpawn = Math.max(line.respawn_0, line.respawn_1);

                if(isFrame)
                    respawn = minSpawn+"f ~ "+maxSpawn+"f";
                else
                    respawn = df.format(minSpawn/30.0)+"s ~ "+df.format(maxSpawn/30.0)+"s";
            }

            String startResp = start+" ("+respawn+")";

            startRespawn.add(startResp);

            String baseHP;

            if(st.trail) {
                baseHP = String.valueOf(line.castle_0);
            } else {
                if(line.castle_0 == line.castle_1 || line.castle_1 == 0)
                    baseHP = line.castle_0+"%";
                else {
                    int minHealth = Math.min(line.castle_0, line.castle_1);
                    int maxHealth = Math.max(line.castle_0, line.castle_1);

                    baseHP = minHealth + " ~ " + maxHealth + "%";
                }
            }

            baseHealth.add(baseHP);

            String layer;

            if(line.layer_0 != line.layer_1) {
                int minLayer = Math.min(line.layer_0, line.layer_1);
                int maxLayer = Math.max(line.layer_0, line.layer_1);

                layer = minLayer + " ~ " + maxLayer;
            } else {
                layer = String.valueOf(line.layer_0);
            }

            layers.add(layer);

            if(needBoss) {
                String boss;

                if(line.boss == 0)
                    boss = "";
                else if(line.boss == 1)
                    boss = LangID.getStringByID("data.stage.boss.normal", lang);
                else
                    boss = LangID.getStringByID("data.stage.boss.shake", lang);

                isBoss.add(boss);
            }

            if(needRespect) {
                String respect = (line.spawn_0 < 0 || line.spawn_1 < 0) ? LangID.getStringByID("data.true", lang) : "";

                respects.add(respect);
            }

            if(needCount) {
                killCounts.add(String.valueOf(line.kill_count));
            }
        }

        double eMax = font.textWidth(LangID.getStringByID("data.stage.enemy", lang));
        double nMax = font.textWidth(LangID.getStringByID("data.stage.number", lang));
        double mMax = font.textWidth(LangID.getStringByID("data.enemy.magnification", lang));
        double iMax = font.textWidth(LangID.getStringByID("data.stage.isBoss", lang));
        double bMax = font.textWidth(LangID.getStringByID(st.trail ? "data.stage.totalDamage" : "data.stage.basePercentage", lang));
        double sMax = font.textWidth(LangID.getStringByID("data.stage.start", lang));
        double lMax = font.textWidth(LangID.getStringByID("data.stage.layer", lang));
        double rMax = font.textWidth(LangID.getStringByID("data.stage.respectStart", lang));
        double kMax = font.textWidth(LangID.getStringByID("data.stage.killCount", lang));

        for(int i = 0; i < enemies.size(); i++) {
            eMax = Math.max(eMax, font.textWidth(enemies.get(i)));

            nMax = Math.max(nMax, font.textWidth(numbers.get(i)));

            mMax = Math.max(mMax, font.textWidth(magnifs.get(i)));

            bMax = Math.max(bMax, font.textWidth(baseHealth.get(i)));

            sMax = Math.max(sMax, font.textWidth(startRespawn.get(i)));

            lMax = Math.max(lMax, font.textWidth(layers.get(i)));

            if(needBoss)
                iMax = Math.max(iMax, font.textWidth(isBoss.get(i)));

            if(needRespect)
                rMax = Math.max(rMax, font.textWidth(respects.get(i)));

            if(needCount)
                kMax = Math.max(kMax, font.textWidth(killCounts.get(i)));
        }

        int xGap = 16;
        int yGap = 10;

        eMax += xGap + 93;
        nMax += xGap;
        mMax += xGap;
        bMax += xGap;
        sMax += xGap;
        lMax += xGap;
        rMax += xGap;
        kMax += xGap;
        iMax += xGap;

        int ySeg = Math.round(Math.max(font.getMaxHeight() + yGap, 32 + yGap));

        double dw = eMax + nMax + mMax + bMax + sMax + lMax;

        if(needBoss)
            dw += iMax;

        if(needRespect)
            dw += rMax;

        if(needCount)
            dw += kMax;

        int w = (int) Math.round(dw);
        int h = ySeg * (enemies.size() + 1);

        CountDownLatch waiter = new CountDownLatch(1);

        double finalEMax = eMax;
        double finalNMax = nMax;
        double finalBMax = bMax;
        double finalMMax = mMax;
        double finalSMax = sMax;
        double finalLMax = lMax;
        double finalRMax = rMax;
        double finalKMax = kMax;
        double finalIMax = iMax;

        boolean finalNeedRespect = needRespect;
        boolean finalNeedCount = needCount;
        boolean finalNeedBoss = needBoss;

        StaticStore.renderManager.createRenderer(w, h, temp, connector -> {
            connector.queue(g -> {
                g.setFontModel(font);

                g.setColor(47, 49, 54, 255);
                g.fillRect(0, 0, w, h);

                g.setColor(54, 57, 63, 255);
                g.fillRect(0, 0, w, ySeg);

                g.setColor(32, 34, 37, 255);
                g.setStroke(4f, GLGraphics.LineEndMode.VERTICAL);

                g.drawRect(0, 0, w, h);

                g.setStroke(2f, GLGraphics.LineEndMode.VERTICAL);

                for(int i = 1; i < enemies.size() + 1; i++) {
                    g.drawLine(0, ySeg * i, w, ySeg * i);
                }

                int x = (int) finalEMax;

                g.drawLine(x, 0, x, h);

                x += (int) finalNMax;

                g.drawLine(x, 0, x, h);

                x += (int) finalBMax;

                g.drawLine(x, 0, x, h);

                x += (int) finalMMax;

                g.drawLine(x, 0, x, h);

                x += (int) finalSMax;

                g.drawLine(x, 0, x, h);

                x += (int) finalLMax;

                g.drawLine(x, 0, x, h);

                if(finalNeedRespect) {
                    x += (int) finalRMax;

                    g.drawLine(x, 0, x, h);
                }

                if(finalNeedCount) {
                    x += (int) finalKMax;

                    g.drawLine(x, 0, x, h);
                }

                if(finalNeedBoss) {
                    x += (int) finalIMax;

                    g.drawLine(x, 0, x, h);
                }

                g.setColor(238, 238, 238, 255);

                int initX = (int) (finalEMax / 2);

                g.drawText(LangID.getStringByID("data.stage.enemy", lang), initX, ySeg / 2f, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.MIDDLE);

                initX += (int) (finalEMax / 2 + finalNMax / 2);

                g.drawText(LangID.getStringByID("data.stage.number", lang), initX, ySeg / 2f, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.MIDDLE);

                initX += (int) (finalNMax / 2 + finalBMax / 2);

                g.drawText(LangID.getStringByID(st.trail ? "data.stage.totalDamage" : "data.stage.basePercentage", lang), initX, ySeg / 2f, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.MIDDLE);

                initX += (int) (finalBMax / 2 + finalMMax / 2);

                g.drawText(LangID.getStringByID("data.enemy.magnification", lang), initX, ySeg / 2f, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.MIDDLE);

                initX += (int) (finalMMax / 2 + finalSMax / 2);

                g.drawText(LangID.getStringByID("data.stage.start", lang), initX, ySeg / 2f, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.MIDDLE);

                initX += (int) (finalSMax / 2 + finalLMax / 2);

                g.drawText(LangID.getStringByID("data.stage.layer", lang), initX, ySeg / 2f, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.MIDDLE);

                initX += (int) (finalLMax / 2);

                if(finalNeedRespect) {
                    initX += (int) (finalRMax / 2);

                    g.drawText(LangID.getStringByID("data.stage.respectStart", lang), initX, ySeg / 2f, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.MIDDLE);

                    initX += (int) (finalRMax / 2);
                }

                if(finalNeedCount) {
                    initX += (int) (finalKMax / 2);

                    g.drawText(LangID.getStringByID("data.stage.killCount", lang), initX, ySeg / 2f, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.MIDDLE);

                    initX += (int) (finalKMax / 2);
                }

                if(finalNeedBoss) {
                    initX += (int) (finalIMax / 2);

                    g.drawText(LangID.getStringByID("data.stage.isBoss", lang), initX, ySeg / 2f, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.MIDDLE);
                }

                for(int i = 0; i < enemies.size(); i++) {
                    AbEnemy e = st.data.datas[st.data.datas.length - 1 - i].enemy.get();

                    if(e != null) {
                        FakeImage edi;

                        if(e instanceof Enemy) {
                            if(((Enemy) e).anim.getEdi() != null) {
                                edi = ((Enemy) e).anim.getEdi().getImg();
                            } else {
                                edi = CommonStatic.getBCAssets().ico[0][0].getImg();
                            }
                        } else {
                            edi = CommonStatic.getBCAssets().ico[0][0].getImg();
                        }

                        g.drawImage(edi, xGap / 2f, ySeg * (i + 1) + yGap / 2f);
                    } else
                        continue;

                    int px = 93 + xGap / 2;
                    int py = ySeg * (i + 2) - ySeg / 2;

                    g.drawText(enemies.get(i), px, py, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.MIDDLE);

                    px = (int) finalEMax + xGap / 2;

                    g.drawText(numbers.get(i), px, py, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.MIDDLE);

                    px += (int) finalNMax;

                    g.drawText(baseHealth.get(i), px, py, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.MIDDLE);

                    px += (int) finalBMax;

                    g.drawText(magnifs.get(i), px, py, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.MIDDLE);

                    px += (int) finalMMax;

                    g.drawText(startRespawn.get(i), px, py, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.MIDDLE);

                    px += (int) finalSMax;

                    g.drawText(layers.get(i), px, py, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.MIDDLE);

                    px += (int) finalLMax;

                    if(finalNeedRespect) {
                        g.drawText(respects.get(i), px, py, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.MIDDLE);

                        px += (int) finalRMax;
                    }

                    if(finalNeedCount) {
                        g.drawText(killCounts.get(i), px, py, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.MIDDLE);

                        px += (int) finalKMax;
                    }

                    if(finalNeedBoss) {
                        g.drawText(isBoss.get(i), px, py, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.MIDDLE);
                    }
                }

                return null;
            });

            return null;
        }, progress -> img, () -> {
            waiter.countDown();

            return null;
        });

        waiter.await();

        return img;
    }

    public static void generateFormImage(Form f, MessageChannel ch, Message reference, int mode, int frame, boolean transparent, boolean debug, CommonStatic.Lang.Locale lang) throws Exception {
        f.anim.load();

        if(mode >= f.anim.anims.length)
            mode = 0;

        EAnimD<?> anim = f.anim.getEAnim(ImageDrawing.getAnimType(mode, f.anim.anims.length));

        File img = ImageDrawing.drawAnimImage(anim, frame, 1f, transparent, debug);

        f.anim.unload();

        if(img != null) {
            String fName = MultiLangCont.get(f, lang);

            if(fName == null || fName.isBlank())
                fName = f.names.toString();

            if(fName.isBlank())
                fName = LangID.getStringByID("data.stage.limit.unit", lang)+" "+ Data.trio(f.uid.id)+" "+Data.trio(f.fid);

            Command.sendMessageWithFile(ch, LangID.getStringByID("formImage.result", lang).replace("_", fName).replace(":::", getModeName(mode, f.anim.anims.length, lang)).replace("=", String.valueOf(frame)), img, "result.png", reference);
        }
    }

    public static void generateEnemyImage(Enemy en, MessageChannel ch, Message reference, int mode, int frame, boolean transparent, boolean debug, CommonStatic.Lang.Locale lang) throws Exception {
        en.anim.load();

        if(mode >= en.anim.anims.length)
            mode = 0;

        EAnimD<?> anim = en.anim.getEAnim(ImageDrawing.getAnimType(mode, en.anim.anims.length));

        File img = ImageDrawing.drawAnimImage(anim, frame, 1f, transparent, debug);

        en.anim.unload();

        if(img != null) {
            String eName = MultiLangCont.get(en, lang);

            if(eName == null || eName.isBlank())
                eName = en.names.toString();

            if(eName.isBlank())
                eName = LangID.getStringByID("data.stage.enemy", lang)+" "+ Data.trio(en.id.id);

            Command.sendMessageWithFile(ch, LangID.getStringByID("formImage.result", lang).replace("_", eName).replace(":::", getModeName(mode, en.anim.anims.length, lang)).replace("=", String.valueOf(frame)), img, "result.png", reference);
        }
    }

    private static String getModeName(int mode, int max, CommonStatic.Lang.Locale lang) {
        switch (mode) {
            case 1 -> {
                return LangID.getStringByID("data.animation.mode.idle", lang);
            }
            case 2 -> {
                return LangID.getStringByID("data.animation.mode.attack", lang);
            }
            case 3 -> {
                return LangID.getStringByID("formImage.mode.kb", lang);
            }
            case 4 -> {
                if (max == 5)
                    return LangID.getStringByID("data.animation.mode.enter", lang);
                else
                    return LangID.getStringByID("formImage.mode.burrowDown", lang);
            }
            case 5 -> {
                return LangID.getStringByID("formImage.mode.burrowMove", lang);
            }
            case 6 -> {
                return LangID.getStringByID("formImage.mode.burrowUp", lang);
            }
            default -> {
                return LangID.getStringByID("data.animation.mode.walk", lang);
            }
        }
    }

    public static void generateFormAnim(Form f, MessageChannel ch, Message reference, int booster, int mode, boolean debug, int limit, CommonStatic.Lang.Locale lang, boolean raw, boolean gif, Runnable onSuccess, Runnable onFail) {
        if(f.unit == null || f.unit.id == null) {
            onFail.run();

            return;
        }

        f.anim.load();

        if(mode >= f.anim.anims.length)
            mode = 0;

        if(!debug && limit <= 0) {
            String id = generateID(f, mode);

            String link = StaticStore.imgur.get(id, gif, raw);

            if(link != null) {
                Command.replyToMessageSafely(ch, LangID.getStringByID("data.animation.gif.cached", lang).replace("_", link), reference, a -> a);

                onFail.run();

                return;
            }
        }

        ShardManager client = ch.getJDA().getShardManager();

        if (client == null)
            return;

        if(limit > 0)  {
            ch.sendMessage(LangID.getStringByID("data.animation.gif.length.withLimit", lang).replace("_", String.valueOf(f.anim.len(getAnimType(mode, f.anim.anims.length)))).replace("-", String.valueOf(limit))).queue();
        } else if(!raw && f.anim.len(getAnimType(mode, f.anim.anims.length)) >= 300) {
            ch.sendMessage(LangID.getStringByID("data.animation.gif.length.withLimit", lang).replace("_", String.valueOf(f.anim.len(getAnimType(mode, f.anim.anims.length)))).replace("-", 300+"")).queue();
        } else {
            ch.sendMessage(LangID.getStringByID("data.animation.gif.length.default", lang).replace("_", String.valueOf(f.anim.len(getAnimType(mode, f.anim.anims.length))))).queue();
        }

        CommonStatic.getConfig().ref = false;

        int finalMode = mode;

        ch.sendMessage(LangID.getStringByID("data.animation.gif.analyzingBox", lang)).queue(msg -> {
            try {
                if(msg == null) {
                    onFail.run();

                    return;
                }

                long start = System.currentTimeMillis();

                EAnimD<?> anim = f.getEAnim(getAnimType(finalMode, f.anim.anims.length));

                File img;

                if(raw) {
                    img = ImageDrawing.drawAnimMp4(anim, msg, 1f, false, debug, limit, lang);
                } else {
                    img = ImageDrawing.drawAnimGif(anim, msg, 1f, false, debug, limit, lang);
                }

                f.anim.unload();

                long end = System.currentTimeMillis();

                String time = DataToString.df.format((end - start)/1000.0);

                long max;

                if(debug || limit > 0)
                    max = getBoosterFileLimit(booster) * 1024L * 1024;
                else
                    max = 8 * 1024 * 1024;

                if (img == null) {
                    ch.sendMessage(LangID.getStringByID("data.animation.gif.failed.gif", lang)).queue();

                    onFail.run();
                } else if (img.length() >= max) {
                    if(img.length() < (raw ? 200 * 1024 * 1024 : 10 * 1024 * 1024)) {
                        Command.replyToMessageSafely(ch, LangID.getStringByID("data.animation.gif.alternative.imgur", lang), reference, a -> a, m -> {
                            if(m == null) {
                                ch.sendMessage(LangID.getStringByID("data.animation.gif.failed.unknown", lang)).queue(message -> {
                                    if(img.exists() && !img.delete()) {
                                        StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                    }
                                }, e -> {
                                    StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateFormAnim - Failed to display form anim");

                                    if(img.exists() && !img.delete()) {
                                        StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                    }
                                });

                                onFail.run();

                                return;
                            }

                            String link;

                            try {
                                link = StaticStore.imgur.uploadFile(img);
                            } catch (Exception e) {
                                StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateFormAnim - Failed to upload file to website");

                                return;
                            }

                            if(link == null) {
                                m.editMessage(LangID.getStringByID("data.animation.gif.failed.imgur", lang))
                                        .setAllowedMentions(new ArrayList<>())
                                        .queue(message -> {
                                            if(img.exists() && !img.delete()) {
                                                StaticStore.logger.uploadLog("Failed to delete file : " + img.getAbsolutePath());
                                            }
                                        }, e -> {
                                            StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateFormAnim - Failed to display form anim");

                                            if(img.exists() && !img.delete()) {
                                                StaticStore.logger.uploadLog("Failed to delete file : " + img.getAbsolutePath());
                                            }
                                        });
                            } else {
                                if(!debug && limit <= 0) {
                                    String id = generateID(f, finalMode);

                                    StaticStore.imgur.put(id, link, raw);
                                }

                                long finalEnd = System.currentTimeMillis();

                                m.editMessage(LangID.getStringByID("data.animation.gif.uploaded.imgur", lang).replace("_FFF_", getFileSize(img)).replace("_TTT_", DataToString.df.format((end-start) / 1000.0)).replace("_ttt_", DataToString.df.format((finalEnd-start) / 1000.0))+"\n"+link)
                                        .setAllowedMentions(new ArrayList<>())
                                        .queue(message -> {
                                            if(img.exists() && !img.delete()) {
                                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                            }
                                        }, e -> {
                                            StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateFormAnim - Failed to display form anim");

                                            if(img.exists() && !img.delete()) {
                                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                            }
                                        });

                                GuildChannel chan = client.getGuildChannelById(StaticStore.UNITARCHIVE);

                                if(chan instanceof GuildMessageChannel) {
                                    ((GuildMessageChannel) chan).sendMessage(generateID(f, finalMode)+"\n\n"+link).queue();
                                }
                            }

                            onSuccess.run();
                        });
                    } else if(img.length() < 200 * 1024 * 1024) {
                        Command.replyToMessageSafely(ch, LangID.getStringByID("data.animation.gif.alternative.catbox", lang), reference, a -> a, m -> {
                            if(m == null) {
                                ch.sendMessage(LangID.getStringByID("data.animation.gif.failed.unknown", lang)).queue(message -> {
                                    if(img.exists() && !img.delete()) {
                                        StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                    }
                                }, e -> {
                                    StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateFormAnim - Failed to display form anim");

                                    if(img.exists() && !img.delete()) {
                                        StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                    }
                                });

                                onFail.run();

                                return;
                            }

                            String link;

                            try {
                                link = StaticStore.imgur.uploadCatbox(img);
                            } catch (Exception e) {
                                StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateFormAnim - Failed to upload file to catbox");

                                return;
                            }

                            if(link == null) {
                                m.editMessage(LangID.getStringByID("data.animation.gif.failed.catbox", lang))
                                        .setAllowedMentions(new ArrayList<>())
                                        .queue(message -> {
                                            if(img.exists() && !img.delete()) {
                                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                            }
                                        }, e -> {
                                            StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateFormAnim - Failed to display form anim");

                                            if(img.exists() && !img.delete()) {
                                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                            }
                                        });
                            } else {
                                if(!debug && limit <= 0) {
                                    String id = generateID(f, finalMode);

                                    StaticStore.imgur.put(id, link, raw);
                                }

                                long finalEnd = System.currentTimeMillis();

                                m.editMessage(String.format(LangID.getStringByID("data.animation.gif.uploaded.catbox", lang), getFileSize(img), (end-start) / 1000.0, (finalEnd-start) / 1000.0)+"\n"+link)
                                        .setAllowedMentions(new ArrayList<>())
                                        .queue(message -> {
                                            if(img.exists() && !img.delete()) {
                                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                            }
                                        }, e -> {
                                            StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateFormAnim - Failed to display form anim");

                                            if(img.exists() && !img.delete()) {
                                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                            }
                                        });

                                GuildChannel chan = client.getGuildChannelById(StaticStore.UNITARCHIVE);

                                if(chan instanceof GuildMessageChannel) {
                                    ((GuildMessageChannel) chan).sendMessage(generateID(f, finalMode)+"\n\n"+link).queue();
                                }
                            }

                            onSuccess.run();
                        });
                    } else {
                        ch.sendMessage(LangID.getStringByID("data.animation.gif.failed.tooBig", lang)).queue();

                        onSuccess.run();
                    }
                } else if(img.length() < max) {
                    if(debug || limit > 0) {
                        Command.sendMessageWithFile(ch, LangID.getStringByID("data.animation.gif.uploaded.default", lang).replace("_TTT_", time).replace("_FFF_", getFileSize(img)), img, raw ? "result.mp4" : "result.gif", reference);
                    } else {
                        GuildChannel chan = client.getGuildChannelById(StaticStore.UNITARCHIVE);

                        if(chan instanceof GuildMessageChannel) {
                            String siz = getFileSize(img);

                            ((GuildMessageChannel) chan).sendMessage(generateID(f, finalMode))
                                    .addFiles(FileUpload.fromData(img, raw ? "result.mp4" : "result.gif"))
                                    .queue(m -> {
                                        if(img.exists() && !img.delete()) {
                                            StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                        }

                                        for(int i = 0; i < m.getAttachments().size(); i++) {
                                            Message.Attachment at = m.getAttachments().get(i);

                                            if(at.getFileName().startsWith("result.")) {
                                                Command.replyToMessageSafely(ch, LangID.getStringByID("data.animation.gif.uploaded.default", lang).replace("_TTT_", time).replace("_FFF_", siz)+"\n\n"+at.getUrl(), reference, a -> a);
                                                break;
                                            }
                                        }

                                        cacheImage(f, finalMode, m);
                                    }, e -> {
                                        StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateFormAnim - Failed to display form anim");

                                        if(img.exists() && !img.delete()) {
                                            StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                        }
                                    });
                        }
                    }

                    onSuccess.run();
                }
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateFormAnim - Failed to generate form animation");
            }
        });
    }

    public static void generateEnemyAnim(Enemy en, MessageChannel ch, Message reference, int booster, int mode, boolean debug, int limit, CommonStatic.Lang.Locale lang, boolean raw, boolean gif, Runnable onSuccess, Runnable onFail) {
        if(en.id == null) {
            onFail.run();

            return;
        }

        en.anim.load();

        if(mode >= en.anim.anims.length)
            mode = 0;

        if(!debug && limit <= 0) {
            String id = generateID(en, mode);

            String link = StaticStore.imgur.get(id, gif, raw);

            if(link != null) {
                Command.replyToMessageSafely(ch, LangID.getStringByID("data.animation.gif.cached", lang).replace("_", link), reference, a -> a);

                onFail.run();

                return;
            }
        }

        ShardManager client = ch.getJDA().getShardManager();

        if (client == null)
            return;

        EAnimD<?> anim = en.getEAnim(getAnimType(mode, en.anim.anims.length));

        if(limit > 0)  {
            ch.sendMessage(LangID.getStringByID("data.animation.gif.length.withLimit", lang).replace("_", String.valueOf(anim.len())).replace("-", String.valueOf(limit))).queue();
        } else if(!raw && anim.len() >= 300) {
            ch.sendMessage(LangID.getStringByID("data.animation.gif.length.withLimit", lang).replace("_", String.valueOf(anim.len())).replace("-", 300+"")).queue();
        } else {
            ch.sendMessage(LangID.getStringByID("data.animation.gif.length.default", lang).replace("_", String.valueOf(anim.len()))).queue();
        }

        CommonStatic.getConfig().ref = false;
        int finalMode = mode;

        ch.sendMessage(LangID.getStringByID("data.animation.gif.analyzingBox", lang)).queue(msg -> {
            try {
                if(msg == null) {
                    onFail.run();

                    return;
                }

                long start = System.currentTimeMillis();

                File img;

                long max;

                if(debug || limit > 0)
                    max = getBoosterFileLimit(booster) * 1024L * 1024;
                else
                    max = 8 * 1024 * 1024;

                if(raw) {
                    img = ImageDrawing.drawAnimMp4(anim, msg, 1f, false, debug, limit, lang);
                } else {
                    img = ImageDrawing.drawAnimGif(anim, msg, 1f, false, debug, limit, lang);
                }

                en.anim.unload();

                long end = System.currentTimeMillis();

                String time = DataToString.df.format((end - start)/1000.0);

                if(img == null) {
                    Command.replyToMessageSafely(ch, LangID.getStringByID("data.animation.gif.failed.gif", lang), reference, a -> a);

                    onFail.run();
                } else if(img.length() >= max) {
                    if(img.length() < (raw ? 200 * 1024 * 1024 : 10 * 1024 * 1024)) {
                        Command.replyToMessageSafely(ch, LangID.getStringByID("data.animation.gif.alternative.imgur", lang), reference, a -> a, m -> {
                            if(m == null) {
                                ch.sendMessage(LangID.getStringByID("data.animation.gif.failed.unknown", lang)).queue(message -> {
                                    if(img.exists() && !img.delete()) {
                                        StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                    }
                                }, e -> {
                                    StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateFormAnim - Failed to display form anim");

                                    if(img.exists() && !img.delete()) {
                                        StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                    }
                                });

                                onFail.run();

                                return;
                            }

                            String link;

                            try {
                                link = StaticStore.imgur.uploadFile(img);
                            } catch (Exception e) {
                                StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateFormAnim - Failed to upload to file to imgur");

                                return;
                            }

                            if(link == null) {
                                m.editMessage(LangID.getStringByID("data.animation.gif.failed.imgur", lang))
                                        .setAllowedMentions(new ArrayList<>())
                                        .queue(message -> {
                                            if(img.exists() && !img.delete()) {
                                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                            }
                                        }, e -> {
                                            StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateFormAnim - Failed to display form anim");

                                            if(img.exists() && !img.delete()) {
                                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                            }
                                        });
                            } else {
                                if(!debug && limit <= 0) {
                                    String id = generateID(en, finalMode);

                                    StaticStore.imgur.put(id, link, raw);
                                }

                                long finalEnd = System.currentTimeMillis();

                                m.editMessage(LangID.getStringByID("data.animation.gif.uploaded.imgur", lang).replace("_FFF_", getFileSize(img)).replace("_TTT_", DataToString.df.format((end-start) / 1000.0)).replace("_ttt_", DataToString.df.format((finalEnd-start) / 1000.0))+"\n"+link)
                                        .setAllowedMentions(new ArrayList<>())
                                        .queue(message -> {
                                            if(img.exists() && !img.delete()) {
                                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                            }
                                        }, e -> {
                                            StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateFormAnim - Failed to display form anim");

                                            if(img.exists() && !img.delete()) {
                                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                            }
                                        });

                                GuildChannel chan = client.getGuildChannelById(StaticStore.ENEMYARCHIVE);

                                if(chan instanceof GuildMessageChannel) {
                                    ((GuildMessageChannel) chan).sendMessage(generateID(en, finalMode)+"\n\n"+link).queue();
                                }
                            }

                            onSuccess.run();
                        });
                    } else if(img.length() < 200 * 1024 * 1024) {
                        Command.replyToMessageSafely(ch, LangID.getStringByID("data.animation.gif.alternative.catbox", lang), reference, a -> a, m -> {
                            if(m == null) {
                                ch.sendMessage(LangID.getStringByID("data.animation.gif.failed.unknown", lang)).queue(message -> {
                                    if(img.exists() && !img.delete()) {
                                        StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                    }
                                }, e -> {
                                    StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateFormAnim - Failed to display form anim");

                                    if(img.exists() && !img.delete()) {
                                        StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                    }
                                });

                                onFail.run();

                                return;
                            }

                            String link;

                            try {
                                link = StaticStore.imgur.uploadCatbox(img);
                            } catch (Exception e) {
                                StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateEnemyAnim - Failed to upload animation to cat box");

                                return;
                            }

                            if(link == null) {
                                m.editMessage(LangID.getStringByID("data.animation.gif.failed.catbox", lang))
                                        .setAllowedMentions(new ArrayList<>())
                                        .queue(message -> {
                                            if(img.exists() && !img.delete()) {
                                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                            }
                                        }, e -> {
                                            StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateFormAnim - Failed to display form anim");

                                            if(img.exists() && !img.delete()) {
                                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                            }
                                        });
                            } else {
                                if(!debug && limit <= 0) {
                                    String id = generateID(en, finalMode);

                                    StaticStore.imgur.put(id, link, raw);
                                }

                                long finalEnd = System.currentTimeMillis();

                                m.editMessage(String.format(LangID.getStringByID("data.animation.gif.uploaded.catbox", lang), getFileSize(img), (end-start) / 1000.0, (finalEnd-start) / 1000.0)+"\n"+link)
                                        .setAllowedMentions(new ArrayList<>())
                                        .queue(message -> {
                                            if(img.exists() && !img.delete()) {
                                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                            }
                                        }, e -> {
                                            StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateFormAnim - Failed to display form anim");

                                            if(img.exists() && !img.delete()) {
                                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                            }
                                        });

                                GuildChannel chan = client.getGuildChannelById(StaticStore.ENEMYARCHIVE);

                                if(chan instanceof GuildMessageChannel) {
                                    ((GuildMessageChannel) chan).sendMessage(generateID(en, finalMode)+"\n\n"+link).queue();
                                }
                            }

                            onSuccess.run();
                        });
                    } else {
                        ch.sendMessage(LangID.getStringByID("data.animation.gif.failed.tooBig", lang)).queue();
                        onSuccess.run();
                    }
                } else if(img.length() < max) {
                    if(debug || limit > 0) {
                        Command.sendMessageWithFile(ch, LangID.getStringByID("data.animation.gif.uploaded.default", lang).replace("_TTT_", time).replace("_FFF_", getFileSize(img)), img, raw ? "result.mp4" : "result.gif", reference);

                        onSuccess.run();
                    } else {
                        GuildChannel chan = client.getGuildChannelById(StaticStore.ENEMYARCHIVE);

                        if(chan instanceof GuildMessageChannel) {
                            String siz = getFileSize(img);

                            ((GuildMessageChannel) chan).sendMessage(generateID(en, finalMode))
                                    .addFiles(FileUpload.fromData(img, raw ? "result.mp4" : "result.gif"))
                                    .queue(m -> {
                                        if(img.exists() && !img.delete()) {
                                            StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                        }

                                        for(int i = 0; i < m.getAttachments().size(); i++) {
                                            Message.Attachment at = m.getAttachments().get(i);

                                            if(at.getFileName().startsWith("result.")) {
                                                Command.replyToMessageSafely(ch, LangID.getStringByID("data.animation.gif.uploaded.default", lang).replace("_TTT_", time).replace("_FFF_", siz)+"\n\n"+at.getUrl(), reference, a -> a);
                                            }
                                        }

                                        cacheImage(en, finalMode, m);

                                        onSuccess.run();
                                    }, e -> {
                                        StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateEnemyAnim - Failed to display enemy anim");

                                        if(img.exists() && !img.delete()) {
                                            StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                        }
                                    });
                        }
                    }
                }
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateEnemyAnim - Failed to generate enemy animation");
            }
        });
    }

    public static void generateAnim(MessageChannel ch, AnimMixer mixer, int booster, boolean performance, CommonStatic.Lang.Locale lang, boolean debug, int limit, boolean raw, int index) {
        boolean mix = mixer.mix();

        if(!mix) {
            ch.sendMessage("Failed to mix Anim").queue();
            return;
        }

        EAnimD<?> anim = mixer.getAnim(index);

        if(anim == null) {
            ch.sendMessage("Failed to generate anim instance").queue();
            return;
        }

        ch.sendMessage(LangID.getStringByID("data.animation.gif.length.default", lang).replace("_", String.valueOf(anim.len()))).queue();

        CommonStatic.getConfig().ref = false;

        ch.sendMessage(LangID.getStringByID("data.animation.gif.analyzingBox", lang)).queue(msg -> {
            try {
                if(msg == null)
                    return;

                long start = System.currentTimeMillis();

                File img;

                if(raw) {
                    img = ImageDrawing.drawAnimMp4(anim, msg, 1f, performance, debug, limit, lang);
                } else {
                    img = ImageDrawing.drawAnimGif(anim, msg, 1f, performance, debug, limit, lang);
                }

                long end = System.currentTimeMillis();

                String time = DataToString.df.format((end - start)/1000.0);

                if(img == null) {
                    ch.sendMessage(LangID.getStringByID("data.animation.gif.failed.gif", lang)).queue();
                } else if(img.length() >= (long) getBoosterFileLimit(booster) * 1024 * 1024) {
                    if(img.length() < (raw ? 200 * 1024 * 1024 : 10 * 1024 * 1024)) {
                        ch.sendMessage(LangID.getStringByID("data.animation.gif.alternative.imgur", lang)).queue(m -> {
                            if(m == null) {
                                ch.sendMessage(LangID.getStringByID("data.animation.gif.failed.unknown", lang)).queue(message -> {
                                    if(img.exists() && !img.delete()) {
                                        StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                    }
                                }, e -> {
                                    StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateFormAnim - Failed to display form anim");

                                    if(img.exists() && !img.delete()) {
                                        StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                    }
                                });

                                return;
                            }

                            String link;

                            try {
                                link = StaticStore.imgur.uploadFile(img);
                            } catch (Exception e) {
                                StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateAnim - Failed to upload animation file to imgur");

                                return;
                            }

                            if(link == null) {
                                m.editMessage(LangID.getStringByID("data.animation.gif.failed.imgur", lang))
                                        .setAllowedMentions(new ArrayList<>())
                                        .queue(message -> {
                                            if(img.exists() && !img.delete()) {
                                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                            }
                                        }, e -> {
                                            StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateFormAnim - Failed to display form anim");

                                            if(img.exists() && !img.delete()) {
                                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                            }
                                        });
                            } else {
                                long finalEnd = System.currentTimeMillis();

                                m.editMessage(LangID.getStringByID("data.animation.gif.uploaded.imgur", lang).replace("_FFF_", getFileSize(img)).replace("_TTT_", DataToString.df.format((end-start) / 1000.0)).replace("_ttt_", DataToString.df.format((finalEnd-start) / 1000.0))+"\n"+link)
                                        .setAllowedMentions(new ArrayList<>())
                                        .queue(message -> {
                                            if(img.exists() && !img.delete()) {
                                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                            }
                                        }, e -> {
                                            StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateFormAnim - Failed to display form anim");

                                            if(img.exists() && !img.delete()) {
                                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                            }
                                        });
                            }
                        });
                    } else if(img.length() < 200 * 1024 * 1024) {
                        ch.sendMessage(LangID.getStringByID("data.animation.gif.alternative.catbox", lang)).queue(m -> {
                            if(m == null) {
                                ch.sendMessage(LangID.getStringByID("data.animation.gif.failed.unknown", lang)).queue(message -> {
                                    if(img.exists() && !img.delete()) {
                                        StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                    }
                                }, e -> {
                                    StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateFormAnim - Failed to display form anim");

                                    if(img.exists() && !img.delete()) {
                                        StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                    }
                                });

                                return;
                            }

                            String link;

                            try {
                                link = StaticStore.imgur.uploadCatbox(img);
                            } catch (Exception e) {
                                StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateAnim - Failed to upload animation file to cat box");

                                return;
                            }

                            if(link == null) {
                                m.editMessage(LangID.getStringByID("data.animation.gif.failed.catbox", lang))
                                        .setAllowedMentions(new ArrayList<>())
                                        .queue(message -> {
                                            if(img.exists() && !img.delete()) {
                                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                            }
                                        }, e -> {
                                            StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateFormAnim - Failed to display form anim");

                                            if(img.exists() && !img.delete()) {
                                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                            }
                                        });
                            } else {
                                long finalEnd = System.currentTimeMillis();

                                m.editMessage(LangID.getStringByID("data.animation.gif.uploaded.catbox", lang).replace("_FFF_", getFileSize(img)).replace("_TTT_", DataToString.df.format((end-start) / 1000.0)).replace("_ttt_", DataToString.df.format((finalEnd-start) / 1000.0))+"\n"+link)
                                        .setAllowedMentions(new ArrayList<>())
                                        .queue(message -> {
                                            if(img.exists() && !img.delete()) {
                                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                            }
                                        }, e -> {
                                            StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateFormAnim - Failed to display form anim");

                                            if(img.exists() && !img.delete()) {
                                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                            }
                                        });
                            }
                        });
                    }
                } else if(img.length() < (long) getBoosterFileLimit(booster) * 1024 * 1024) {
                    ch.sendMessage(LangID.getStringByID("data.animation.gif.uploaded.default", lang).replace("_TTT_", time).replace("_FFF_", getFileSize(img)))
                            .addFiles(FileUpload.fromData(img, raw ? "result.mp4" : "result.gif"))
                            .queue(message -> {
                                if(img.exists() && !img.delete()) {
                                    StaticStore.logger.uploadLog("W/EntityHandlerAnim | Can't delete file : "+img.getAbsolutePath());
                                }
                            }, e -> {
                                StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateAnim - Failed to generate mixed anim");

                                if(img.exists() && !img.delete()) {
                                    StaticStore.logger.uploadLog("W/EntityHandlerAnim | Can't delete file : "+img.getAbsolutePath());
                                }
                            });
                }
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateAnim - Failed to generate animation");
            }
        });
    }

    public static void generateBCAnim(MessageChannel ch, int booster, AnimMixer mixer, boolean performance, CommonStatic.Lang.Locale lang, Runnable onFail, Runnable onSuccess) {
        boolean mix = mixer.mix();

        if(!mix) {
            ch.sendMessage("Failed to mix Anim").queue();

            onFail.run();

            return;
        }

        CommonStatic.getConfig().ref = false;

        ch.sendMessage(LangID.getStringByID("data.animation.gif.analyzingBox", lang)).queue(msg -> {
            if(msg == null) {
                onFail.run();

                return;
            }

            long start = System.currentTimeMillis();

            File img;

            try {
                img = ImageDrawing.drawBCAnim(mixer, msg, 1f, performance, lang);
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateBCAnim - Failed to generate BC animation");

                return;
            }

            long end = System.currentTimeMillis();

            String time = DataToString.df.format((end - start) / 1000.0);

            if(img == null) {
                ch.sendMessage(LangID.getStringByID("data.animation.gif.failed.gif", lang)).queue();
            } else if(img.length() >= (long) getBoosterFileLimit(booster) * 1024 * 1024 && img.length() < 200 * 1024 * 1024) {
                ch.sendMessage(LangID.getStringByID("data.animation.gif.alternative.imgur", lang)).queue(m -> {
                    if(m == null) {
                        ch.sendMessage(LangID.getStringByID("data.animation.gif.failed.unknown", lang))
                                .queue(message -> {
                                    if(img.exists() && !img.delete()) {
                                        StaticStore.logger.uploadLog("W/EntityHandlerBCAnim | Can't delete file : "+img.getAbsolutePath());
                                    }
                                }, e -> {
                                    StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateBCAnim - Failed to generate BC anim");

                                    if(img.exists() && !img.delete()) {
                                        StaticStore.logger.uploadLog("W/EntityHandlerBCAnim | Can't delete file : "+img.getAbsolutePath());
                                    }
                                });

                        onSuccess.run();

                        return;
                    }

                    String link;

                    try {
                        link = StaticStore.imgur.uploadFile(img);
                    } catch (Exception e) {
                        StaticStore.logger.uploadErrorLog(e, "EntityHandler::generateAnim - Failed to upload aimation to imgur");

                        return;
                    }

                    if(link == null) {
                        m.editMessage(LangID.getStringByID("data.animation.gif.failed.imgur", lang))
                                .queue(message -> {
                                    if(img.exists() && !img.delete()) {
                                        StaticStore.logger.uploadLog("W/EntityHandlerBCAnim | Can't delete file : "+img.getAbsolutePath());
                                    }
                                }, e -> {
                                    StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateBCAnim - Failed to generate BC anim");

                                    if(img.exists() && !img.delete()) {
                                        StaticStore.logger.uploadLog("W/EntityHandlerBCAnim | Can't delete file : "+img.getAbsolutePath());
                                    }
                                });
                    } else {
                        long finalEnd = System.currentTimeMillis();

                        m.editMessage(LangID.getStringByID("data.animation.gif.uploaded.imgur", lang).replace("_FFF_", getFileSize(img)).replace("_TTT_", DataToString.df.format((end-start) / 1000.0)).replace("_ttt_", DataToString.df.format((finalEnd-start) / 1000.0))+"\n"+link)
                                .queue(message -> {
                                    if(img.exists() && !img.delete()) {
                                        StaticStore.logger.uploadLog("W/EntityHandlerBCAnim | Can't delete file : "+img.getAbsolutePath());
                                    }
                                }, e -> {
                                    StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateBCAnim - Failed to generate BC anim");

                                    if(img.exists() && !img.delete()) {
                                        StaticStore.logger.uploadLog("W/EntityHandlerBCAnim | Can't delete file : "+img.getAbsolutePath());
                                    }
                                });
                    }

                    onSuccess.run();
                });
            } else if(img.length() < (long) getBoosterFileLimit(booster) * 1024 * 1024) {
                ch.sendMessage(LangID.getStringByID("data.animation.gif.uploaded.default", lang).replace("_TTT_", time).replace("_FFF_", getFileSize(img)))
                        .addFiles(FileUpload.fromData(img, "result.mp4"))
                        .queue(message -> {
                            if(img.exists() && !img.delete()) {
                                StaticStore.logger.uploadLog("W/EntityHandlerBCAnim | Can't delete file : "+img.getAbsolutePath());
                            }
                        }, e -> {
                            StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateBCAnim - Failed to generate BC anim");

                            if(img.exists() && !img.delete()) {
                                StaticStore.logger.uploadLog("W/EntityHandlerBCAnim | Can't delete file : "+img.getAbsolutePath());
                            }
                        });

                onSuccess.run();
            }
        });
    }

    public static void generateBGAnim(MessageChannel ch, Message reference, Background bg, CommonStatic.Lang.Locale lang) {
        ch.sendMessage(LangID.getStringByID("data.animation.background.prepare", lang)).queue(message -> {
            if(message == null)
                return;

            ShardManager client = ch.getJDA().getShardManager();

            if (client == null)
                return;

            long start = System.currentTimeMillis();

            File result;

            try {
                result = ImageDrawing.drawBGAnimEffect(bg, message, lang);
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateBGAnim - Failed to generate bg animation");

                return;
            }

            long end = System.currentTimeMillis();

            if(result == null) {
                Command.replyToMessageSafely(ch, LangID.getStringByID("data.animation.background.failed", lang), reference, a -> a);
            } else if(result.length() >= 8 * 1024 * 1024) {
                Command.replyToMessageSafely(ch, LangID.getStringByID("data.animation.background.fileTooBig", lang).replace("_SSS_", getFileSize(result)), reference, a -> a);
            } else {
                GuildChannel chan = client.getGuildChannelById(StaticStore.MISCARCHIVE);

                if(chan instanceof MessageChannel) {
                    String siz = getFileSize(result);

                    ((MessageChannel) chan).sendMessage("BG - "+Data.trio(bg.id.id))
                            .addFiles(FileUpload.fromData(result, "result.mp4"))
                            .queue(m -> {
                                if(result.exists() && !result.delete()) {
                                    StaticStore.logger.uploadLog("W/EntityHandlerBGAnim | Can't delete file : "+result.getAbsolutePath());
                                }

                                for(int i = 0; i < m.getAttachments().size(); i++) {
                                    Message.Attachment at = m.getAttachments().get(i);

                                    if(at.getFileName().startsWith("result.")) {
                                        Command.replyToMessageSafely(ch, LangID.getStringByID("data.animation.background.result", lang).replace("_SSS_", siz).replace("_TTT_", DataToString.df.format((end - start) / 1000.0))+"\n\n"+at.getUrl(), reference, a -> a);

                                        StaticStore.imgur.put("BG - "+Data.trio(bg.id.id), at.getUrl(), true);
                                    }
                                }
                            }, e -> {
                                StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateBGAnim - Failed to generate BG anim");

                                if(result.exists() && !result.delete()) {
                                    StaticStore.logger.uploadLog("W/EntityHandlerBGAnim | Can't delete file : "+result.getAbsolutePath());
                                }
                            });
                }
            }
        });
    }

    public static void generateSoulAnim(Soul s, MessageChannel ch, Message reference, int booster, boolean debug, int limit, CommonStatic.Lang.Locale lang, boolean raw, boolean gif, Runnable onSuccess, Runnable onFail) {
        if(s.getID() == null) {
            onFail.run();

            return;
        }

        else if(!debug && limit <= 0) {
            String id = "SOUL - " + Data.trio(s.getID().id);

            String link = StaticStore.imgur.get(id, gif, raw);

            if(link != null) {
                ch.sendMessage(LangID.getStringByID("data.animation.gif.cached", lang).replace("_", link)).queue();

                onFail.run();

                return;
            }
        }

        ShardManager client = ch.getJDA().getShardManager();

        if (client == null)
            return;

        s.anim.load();

        EAnimD<?> anim = s.anim.getEAnim(AnimU.UType.SOUL);

        if(limit > 0)  {
            ch.sendMessage(LangID.getStringByID("data.animation.gif.length.withLimit", lang).replace("_", String.valueOf(anim.len())).replace("-", String.valueOf(limit))).queue();
        } else if(!raw && anim.len() >= 300) {
            ch.sendMessage(LangID.getStringByID("data.animation.gif.length.withLimit", lang).replace("_", String.valueOf(anim.len())).replace("-", 300+"")).queue();
        } else {
            ch.sendMessage(LangID.getStringByID("data.animation.gif.length.default", lang).replace("_", String.valueOf(anim.len()))).queue();
        }

        CommonStatic.getConfig().ref = false;

        ch.sendMessage(LangID.getStringByID("data.animation.gif.analyzingBox", lang)).queue(msg -> {
            if(msg == null) {
                onFail.run();

                return;
            }

            long start = System.currentTimeMillis();

            File img;

            long max;

            if(debug || limit > 0)
                max = getBoosterFileLimit(booster) * 1024L * 1024L;
            else
                max = 8 * 1024 * 1024;

            try {
                if(raw) {
                    img = ImageDrawing.drawAnimMp4(anim, msg, 1f, false, debug, limit, lang);
                } else {
                    img = ImageDrawing.drawAnimGif(anim, msg, 1f, false, debug, limit, lang);
                }
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateSoulAnim - Failed to generate soul animaiton");

                return;
            }

            s.anim.unload();

            long end = System.currentTimeMillis();

            String time = DataToString.df.format((end - start) / 1000.0);

            if(img == null) {
                ch.sendMessage(LangID.getStringByID("data.animation.gif.failed.gif", lang)).queue();

                onFail.run();
            } else if(img.length() >= max && img.length() < (raw ? 200 * 1024 * 1024 : 10 * 1024 * 1024)) {
                Command.replyToMessageSafely(ch, LangID.getStringByID("data.animation.gif.alternative.imgur", lang), reference, a -> a, m -> {
                    if(m == null) {
                        ch.sendMessage(LangID.getStringByID("data.animation.gif.failed.unknown", lang)).queue(message -> {
                            if(img.exists() && !img.delete()) {
                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                            }
                        }, e -> {
                            StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateSoulAnim - Failed to display enemy anim");

                            if(img.exists() && !img.delete()) {
                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                            }
                        });

                        onFail.run();

                        return;
                    }

                    String link;

                    try {
                        link = StaticStore.imgur.uploadFile(img);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    if(link == null) {
                        m.editMessage(LangID.getStringByID("data.animation.gif.failed.imgur", lang)).queue(message -> {
                            if(img.exists() && !img.delete()) {
                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                            }
                        }, e -> {
                            StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateSoulAnim - Failed to display enemy anim");

                            if(img.exists() && !img.delete()) {
                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                            }
                        });
                    } else {
                        if(!debug && limit <= 0) {
                            String id = "SOUL - " + Data.trio(s.getID().id);

                            StaticStore.imgur.put(id, link, raw);
                        }

                        long finalEnd = System.currentTimeMillis();

                        m.editMessage(LangID.getStringByID("data.animation.gif.uploaded.imgur", lang).replace("_FFF_", getFileSize(img)).replace("_TTT_", DataToString.df.format((end-start) / 1000.0)).replace("_ttt_", DataToString.df.format((finalEnd-start) / 1000.0))+"\n"+link)
                                .queue(message -> {
                                    if(img.exists() && !img.delete()) {
                                        StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                    }
                                }, e -> {
                                    StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateSoulAnim - Failed to display enemy anim");

                                    if(img.exists() && !img.delete()) {
                                        StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                    }
                                });

                        GuildChannel chan = client.getGuildChannelById(StaticStore.MISCARCHIVE);

                        if(chan instanceof GuildMessageChannel) {
                            ((GuildMessageChannel) chan).sendMessage("SOUL - " + Data.trio(s.getID().id) + "\n\n"+link).queue();
                        }
                    }

                    onSuccess.run();
                });
            } else if(img.length() < max) {
                if(debug || limit > 0) {
                    Command.sendMessageWithFile(ch, LangID.getStringByID("data.animation.gif.uploaded.default", lang).replace("_TTT_", time).replace("_FFF_", getFileSize(img)), img, raw ? "result.mp4" : "result.gif", reference);
                } else {
                    GuildChannel chan = client.getGuildChannelById(StaticStore.MISCARCHIVE);

                    if(chan instanceof GuildMessageChannel) {
                        String siz = getFileSize(img);

                        ((GuildMessageChannel) chan).sendMessage("SOUL - " + Data.trio(s.getID().id))
                                .addFiles(FileUpload.fromData(img, raw ? "result.mp4" : "result.gif"))
                                .queue(m -> {
                                    if(img.exists() && !img.delete()) {
                                        StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                    }

                                    for(int i = 0; i < m.getAttachments().size(); i++) {
                                        Message.Attachment at = m.getAttachments().get(i);

                                        if(at.getFileName().startsWith("result.")) {
                                            Command.replyToMessageSafely(ch, LangID.getStringByID("data.animation.gif.uploaded.default", lang).replace("_TTT_", time).replace("_FFF_", siz)+"\n\n"+at.getUrl(), reference, a -> a);

                                            StaticStore.imgur.put("SOUL - " + Data.trio(s.getID().id), at.getUrl(), raw);
                                        }
                                    }
                                }, e -> {
                                    StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateEnemyAnim - Failed to display enemy anim");

                                    if(img.exists() && !img.delete()) {
                                        StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                    }
                                });
                    }
                }

                onSuccess.run();
            }
        });
    }

    private static String getFileSize(File f) {
        String[] unit = {"B", "KB", "MB"};

        double size = f.length();

        for (String s : unit) {
            if (size < 1024) {
                return DataToString.df.format(size) + s;
            } else {
                size /= 1024.0;
            }
        }

        return DataToString.df.format(size)+unit[2];
    }

    private static void cacheImage(Form f, int mode, Message msg) {
        if(f.unit == null || f.unit.id == null)
            return;

        String id = generateID(f, mode);

        List<Message.Attachment> att = msg.getAttachments();

        if(att.isEmpty())
            return;

        for(Message.Attachment a : att) {
            if (a.getFileName().equals("result.gif")) {
                String link = a.getUrl();

                StaticStore.imgur.put(id, link, false);

                return;
            } else if(a.getFileName().equals("result.mp4")) {
                String link = a.getUrl();

                StaticStore.imgur.put(id, link, true);

                return;
            }
        }
    }

    public static void getFormSprite(Form f, MessageChannel ch, Message reference, int mode, CommonStatic.Lang.Locale lang) throws Exception {
        if(f.unit == null || f.unit.id == null) {
            ch.sendMessage(LangID.getStringByID("formSprite.failed.invalidUnit", lang)).queue();
            return;
        }

        File temp = new File("./temp");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+temp.getAbsolutePath());
                return;
            }
        }

        File image = StaticStore.generateTempFile(temp, "result", ".png", false);

        if(image == null) {
            return;
        }

        f.anim.load();

        FakeImage img;

        switch (mode) {
            case 0 -> img = f.anim.getNum();
            case 1 -> img = f.anim.getUni().getImg();
            case 2 -> {
                if (f.unit.getCont() instanceof PackData.DefPack) {
                    String code;

                    if (f.fid == 0)
                        code = "f";
                    else if (f.fid == 1)
                        code = "c";
                    else if (f.fid == 2)
                        code = "s";
                    else
                        code = "u";

                    VFile vf = VFile.get("./org/unit/" + Data.trio(f.unit.id.id) + "/" + code + "/udi" + Data.trio(f.unit.id.id) + "_" + code + ".png");

                    if (vf != null) {
                        img = vf.getData().getImg();
                    } else {
                        img = null;
                    }
                } else {
                    img = null;
                }
            }
            case 3 -> img = f.anim.getEdi().getImg();
            default -> throw new IllegalStateException("Mode in sprite getter is incorrect : " + mode);
        }

        if(img == null) {
            Command.replyToMessageSafely(ch, LangID.getStringByID("formSprite.failed.invalidMode", lang).replace("_", getIconName(mode, lang)), reference, a -> a);
            return;
        }

        FakeImage result = img;

        CountDownLatch waiter = new CountDownLatch(1);

        StaticStore.renderManager.createRenderer(result.getWidth(), result.getHeight(), temp, connector -> {
            connector.queue(g -> {
                g.drawImage(result, 0f, 0f);

                return null;
            });

            return null;
        }, progress -> image, () -> {
            waiter.countDown();

            return null;
        });

        waiter.await();

        String fName = StaticStore.safeMultiLangGet(f, lang);

        if(fName == null || fName.isBlank()) {
            fName = Data.trio(f.unit.id.id)+"-"+Data.trio(f.fid);
        }

        Command.sendMessageWithFile(ch, LangID.getStringByID("formSprite.uploaded", lang).replace("_", fName).replace("===", getIconName(mode, lang)), image, "result.png", reference);

        f.anim.unload();
    }

    public static void getEnemySprite(Enemy e, MessageChannel ch, Message reference, int mode, CommonStatic.Lang.Locale lang) throws Exception {
        if(e.id == null) {
            Command.replyToMessageSafely(ch, LangID.getStringByID("enemySprite.failed.invalidEnemy", lang), reference, a -> a);
            return;
        }

        File temp = new File("./temp");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+temp.getAbsolutePath());
                return;
            }
        }

        File image = StaticStore.generateTempFile(temp, "result", ".png", false);

        if(image == null) {
            return;
        }

        e.anim.load();

        FakeImage img = switch (mode) {
            case 0 -> e.anim.getNum();
            case 1 -> e.anim.getUni().getImg();
            case 3 -> e.anim.getEdi().getImg();
            default -> throw new IllegalStateException("Mode in sprite getter is incorrect : " + mode);
        };

        if(img == null) {
            Command.replyToMessageSafely(ch, LangID.getStringByID("formSprite.failed.invalidMode", lang).replace("_", getIconName(mode, lang)), reference, a -> a);
            return;
        }

        CountDownLatch waiter = new CountDownLatch(1);

        StaticStore.renderManager.createRenderer(img.getWidth(), img.getHeight(), temp, connector -> {
            connector.queue(g -> {
                g.drawImage(img, 0f, 0f);

                return null;
            });

            return null;
        }, progress -> image, () -> {
            waiter.countDown();

            return null;
        });

        waiter.await();

        String fName = StaticStore.safeMultiLangGet(e, lang);

        if(fName == null || fName.isBlank()) {
            fName = Data.trio(e.id.id);
        }

        Command.sendMessageWithFile(ch, LangID.getStringByID("formSprite.uploaded", lang).replace("_", fName).replace("===", getIconName(mode, lang)), image, "result.png", reference);

        e.anim.unload();
    }

    public static void getSoulSprite(Soul s, MessageChannel ch, Message reference, CommonStatic.Lang.Locale lang) throws Exception {
        if(s.getID() == null) {
            ch.sendMessage(LangID.getStringByID("soul.failed.noSoul", lang)).queue();

            return;
        }

        File temp = new File("./temp");

        if(!temp.exists() && !temp.mkdirs()) {
            StaticStore.logger.uploadLog("W/EntityHandler::getSoulSprite - Failed to create folder : " + temp.getAbsolutePath());

            return;
        }

        File image = StaticStore.generateTempFile(temp, "result", ".png", false);

        if(image == null)
            return;

        s.anim.load();

        FakeImage img = s.anim.getNum();

        if(img == null) {
            Command.replyToMessageSafely(ch, LangID.getStringByID("soul.failed.noSoul", lang), reference, a -> a);

            return;
        }

        CountDownLatch waiter = new CountDownLatch(1);

        StaticStore.renderManager.createRenderer(img.getWidth(), img.getHeight(), temp, connector -> {
            connector.queue(g -> {
                g.drawImage(img, 0f, 0f);

                return null;
            });

            return null;
        }, progress -> image, () -> {
            waiter.countDown();

            return null;
        });

        waiter.await();

        Command.sendMessageWithFile(
                ch,
                LangID.getStringByID("soulImage.success", lang).replace("_", Data.trio(s.getID().id)),
                image,
                reference
        );

        s.anim.unload();
    }

    public static void showMedalEmbed(int id, MessageChannel ch, Message reference, CommonStatic.Lang.Locale lang) throws  Exception {
        File temp = new File("./temp");

        if(!temp.exists() && !temp.mkdirs()) {
            StaticStore.logger.uploadLog("Can't create folder : "+temp.getAbsolutePath());
            return;
        }

        File image = StaticStore.generateTempFile(temp, "result", ".png", false);

        if(image == null) {
            return;
        }

        String medalName = "./org/page/medal/medal_"+Data.trio(id);

        if(id <= 13 && lang != CommonStatic.Lang.Locale.JP) {
            medalName += "_"+getLocaleName(lang);
        } else if(id == 90 && lang != CommonStatic.Lang.Locale.JP) {
            medalName += "_en";
        }

        medalName += ".png";

        VFile vf = VFile.get(medalName);

        if(vf == null)
            Command.replyToMessageSafely(ch, LangID.getStringByID("medal.failed.noImage", lang), reference, a -> a);
        else {
            FakeImage img = vf.getData().getImg();

            CountDownLatch waiter = new CountDownLatch(1);

            StaticStore.renderManager.createRenderer(img.getWidth(), img.getHeight(), temp, connector -> {
                connector.queue(g -> {
                    g.drawImage(img, 0f, 0f);

                    return null;
                });

                return null;
            }, progress -> image, () -> {
                waiter.countDown();

                return null;
            });

            waiter.await();

            EmbedBuilder e = new EmbedBuilder();

            String name = StaticStore.MEDNAME.getCont(id, lang);
            String desc = StaticStore.MEDEXP.getCont(id, lang);

            if(StaticStore.medalData != null) {
                JsonObject obj = StaticStore.medalData.getAsJsonArray().get(id).getAsJsonObject();

                int grade = obj.get("grade").getAsInt();

                e.setColor(StaticStore.grade[grade]);
            }

            e.addField(name, desc, false);
            e.setImage("attachment://medal.png");

            Command.sendMessageWithFile(ch, "", e.build(), image, "medal.png", reference);
        }
    }

    public static void showComboEmbed(MessageChannel ch, Message reference, Combo c, CommonStatic.Lang.Locale lang) throws Exception {
        File icon = generateComboImage(c);

        EmbedBuilder e = new EmbedBuilder();

        String comboName = MultiLangCont.getStatic().COMNAME.getCont(c, lang);

        if (comboName == null || comboName.isBlank()) {
            comboName = "Combo " + c.name;
        }

        e.setTitle(comboName);

        if (c.lv == 0) {
            e.setColor(StaticStore.rainbow[4]);
        } else if (c.lv == 1) {
            e.setColor(StaticStore.rainbow[3]);
        } else if (c.lv == 2) {
            e.setColor(StaticStore.rainbow[2]);
        } else {
            e.setColor(StaticStore.rainbow[0]);
        }

        e.addField(DataToString.getComboType(c, lang), DataToString.getComboDescription(c, lang), false);

        if (icon != null) {
            e.setImage("attachment://combo.png");
        }

        if (icon != null)
            Command.sendMessageWithFile(ch, "", e.build(), icon, "combo.png", reference);
    }

    public static void showFormDPS(MessageChannel ch, Message authorMessage, Form f, TreasureHolder treasureSetting, Level lv, ConfigHolder config, boolean talent, boolean treasure, CommonStatic.Lang.Locale lang) throws Exception {
        int level = lv.getLv();
        int levelp = lv.getPlusLv();

        if(level <= 0) {
            if(f.unit.rarity == 0)
                level = 110;
            else {
                if(config == null)
                    level = 30;
                else
                    level = config.defLevel;
            }
        }

        if(level > f.unit.max) {
            levelp = level - f.unit.max;
            level = f.unit.max;

            if(levelp > f.unit.maxp)
                levelp = f.unit.maxp;

            if(levelp < 0)
                levelp = 0;
        }

        lv.setLevel(level);
        lv.setPlusLevel(levelp);

        int[] t;

        if(talent && f.du.getPCoin() != null) {
            t = f.du.getPCoin().max.clone();
        } else
            t = null;

        if(t != null) {
            t = handleTalent(f, lv, t);

            lv.setTalents(t);
        }

        if (f.du == null)
            return;

        MaskUnit du;

        if (talent && f.du.getPCoin() != null) {
            du = f.du.getPCoin().improve(lv.getTalents());
        } else {
            du = f.du;
        }

        List<DPSNode> dpsNodes = new ArrayList<>();
        List<DPSNode> treasureNodes = new ArrayList<>();

        // Damage Calculation
        for (int i = 0; i < du.getAtkCount(); i++) {
            if (!du.isLD() && !du.isOmni()) {
                dpsNodes.add(new DPSNode(BigDecimal.valueOf(-320), BigDecimal.ZERO, getAttack(i, du, f.unit.lv, lv, treasureSetting, talent, false)));
                dpsNodes.add(new DPSNode(BigDecimal.valueOf(du.getRange()), BigDecimal.ZERO, getAttack(i, du, f.unit.lv, lv, treasureSetting, talent, false).negate()));

                if (treasure) {
                    treasureNodes.add(new DPSNode(BigDecimal.valueOf(-320), BigDecimal.ZERO, getAttack(i, du, f.unit.lv, lv, treasureSetting, talent, true)));
                    treasureNodes.add(new DPSNode(BigDecimal.valueOf(du.getRange()), BigDecimal.ZERO, getAttack(i, du, f.unit.lv, lv, treasureSetting, talent, true).negate()));
                }
            } else {
                MaskAtk attack = du.getAtkModel(i);

                int shortPoint = attack.getShortPoint();
                int width = attack.getLongPoint() - attack.getShortPoint();

                dpsNodes.add(new DPSNode(BigDecimal.valueOf(Math.min(shortPoint, shortPoint + width)), BigDecimal.ZERO, getAttack(i, du, f.unit.lv, lv, treasureSetting, talent, false)));
                dpsNodes.add(new DPSNode(BigDecimal.valueOf(Math.max(shortPoint, shortPoint + width)), BigDecimal.ZERO, getAttack(i, du, f.unit.lv, lv, treasureSetting, talent, false).negate()));

                if (treasure) {
                    treasureNodes.add(new DPSNode(BigDecimal.valueOf(Math.min(shortPoint, shortPoint + width)), BigDecimal.ZERO, getAttack(i, du, f.unit.lv, lv, treasureSetting, talent, true)));
                    treasureNodes.add(new DPSNode(BigDecimal.valueOf(Math.max(shortPoint, shortPoint + width)), BigDecimal.ZERO, getAttack(i, du, f.unit.lv, lv, treasureSetting, talent, true).negate()));
                }
            }
        }

        MaskAtk representativeAttack = du.getRepAtk();

        Data.Proc.VOLC surgeAbility = representativeAttack.getProc().VOLC;
        Data.Proc.MINIVOLC miniSurgeAbility = representativeAttack.getProc().MINIVOLC;

        Data.Proc.WAVE waveAbility = representativeAttack.getProc().WAVE;
        Data.Proc.MINIWAVE miniWaveAbility = representativeAttack.getProc().MINIWAVE;

        Data.Proc.BLAST blastAbility = representativeAttack.getProc().BLAST;

        // Handling surge
        if (surgeAbility.exists() || miniSurgeAbility.exists()) {
            BigDecimal shortSurgeDistance;
            BigDecimal longSurgeDistance;
            BigDecimal surgeLevel;
            BigDecimal surgeChance;
            BigDecimal surgeMultiplier;

            if (surgeAbility.exists()) {
                shortSurgeDistance = BigDecimal.valueOf(surgeAbility.dis_0);
                longSurgeDistance = BigDecimal.valueOf(surgeAbility.dis_1);
                surgeLevel = BigDecimal.valueOf(surgeAbility.time).divide(BigDecimal.valueOf(Data.VOLC_ITV), Equation.context);
                surgeChance = BigDecimal.valueOf(surgeAbility.prob).divide(new BigDecimal("100"), Equation.context);
                surgeMultiplier = BigDecimal.ONE;
            } else {
                shortSurgeDistance = BigDecimal.valueOf(miniSurgeAbility.dis_0);
                longSurgeDistance = BigDecimal.valueOf(miniSurgeAbility.dis_1);
                surgeLevel = BigDecimal.valueOf(miniSurgeAbility.time).divide(new BigDecimal("20"), Equation.context);
                surgeChance = BigDecimal.valueOf(miniSurgeAbility.prob).divide(new BigDecimal("100"), Equation.context);
                surgeMultiplier = BigDecimal.valueOf(miniSurgeAbility.mult).divide(new BigDecimal("100"), Equation.context);
            }

            BigDecimal minimumDistance = shortSurgeDistance.min(longSurgeDistance);
            BigDecimal maximumDistance = shortSurgeDistance.max(longSurgeDistance);

            BigDecimal minimumRange = minimumDistance.subtract(BigDecimal.valueOf(Data.W_VOLC_INNER));
            BigDecimal maximumRange = maximumDistance.add(BigDecimal.valueOf(Data.W_VOLC_PIERCE));

            BigDecimal minimumPierce = minimumDistance.add(BigDecimal.valueOf(Data.W_VOLC_PIERCE));
            BigDecimal maximumInner = maximumDistance.subtract(BigDecimal.valueOf(Data.W_VOLC_INNER));

            BigDecimal hitChance;

            if (minimumPierce.subtract(maximumInner).compareTo(BigDecimal.ZERO) > 0) {
                hitChance = BigDecimal.ONE;
            } else {
                hitChance = BigDecimal.valueOf(Data.W_VOLC_INNER + Data.W_VOLC_PIERCE)
                        .divide(maximumDistance.subtract(minimumDistance), Equation.context);
            }

            BigDecimal surgeDamage = getTotalAbilityAttack(du, f.unit.lv, lv, treasureSetting, talent, false)
                    .multiply(hitChance)
                    .multiply(surgeChance)
                    .multiply(surgeLevel)
                    .multiply(surgeMultiplier);

            BigDecimal valueDifference = minimumPierce.min(maximumInner).subtract(minimumRange);

            if (valueDifference.compareTo(BigDecimal.ZERO) == 0) {
                dpsNodes.add(new DPSNode(minimumRange, BigDecimal.ZERO, surgeDamage));
                dpsNodes.add(new DPSNode(maximumRange, BigDecimal.ZERO, surgeDamage.negate(), true));
            } else {
                BigDecimal slope = surgeDamage.divide(minimumPierce.min(maximumInner).subtract(minimumRange), Equation.context);

                dpsNodes.add(new DPSNode(minimumRange, slope, BigDecimal.ZERO));
                dpsNodes.add(new DPSNode(minimumPierce.min(maximumInner), slope.negate(), BigDecimal.ZERO));
                dpsNodes.add(new DPSNode(minimumPierce.max(maximumInner), slope.negate(), BigDecimal.ZERO));
                dpsNodes.add(new DPSNode(maximumRange, slope, BigDecimal.ZERO));
            }

            if (treasure) {
                surgeDamage = getTotalAbilityAttack(du, f.unit.lv, lv, treasureSetting, talent, true)
                        .multiply(hitChance)
                        .multiply(surgeChance)
                        .multiply(surgeLevel)
                        .multiply(surgeMultiplier);

                if (valueDifference.compareTo(BigDecimal.ZERO) == 0) {
                    dpsNodes.add(new DPSNode(minimumRange, BigDecimal.ZERO, surgeDamage));
                    dpsNodes.add(new DPSNode(maximumRange, BigDecimal.ZERO, surgeDamage.negate(), true));
                } else {
                    BigDecimal slope = surgeDamage.divide(minimumPierce.min(maximumInner).subtract(minimumRange), Equation.context);

                    dpsNodes.add(new DPSNode(minimumRange, slope, BigDecimal.ZERO));
                    dpsNodes.add(new DPSNode(minimumPierce.min(maximumInner), slope.negate(), BigDecimal.ZERO));
                    dpsNodes.add(new DPSNode(minimumPierce.max(maximumInner), slope.negate(), BigDecimal.ZERO));
                    dpsNodes.add(new DPSNode(maximumRange, slope, BigDecimal.ZERO));
                }
            }
        }

        // Handling wave
        if (waveAbility.exists() || miniWaveAbility.exists()) {
            BigDecimal waveChance;
            BigDecimal waveLevel;
            BigDecimal waveMultiplier;

            if (waveAbility.exists()) {
                waveChance = BigDecimal.valueOf(waveAbility.prob).divide(new BigDecimal("100"), Equation.context);
                waveLevel = BigDecimal.valueOf(waveAbility.lv);
                waveMultiplier = BigDecimal.ONE;
            } else {
                waveChance = BigDecimal.valueOf(miniWaveAbility.prob).divide(new BigDecimal("100"), Equation.context);
                waveLevel = BigDecimal.valueOf(miniWaveAbility.lv);
                waveMultiplier = BigDecimal.valueOf(miniWaveAbility.multi).divide(new BigDecimal("100"), Equation.context);
            }

            BigDecimal waveDamage = getTotalAbilityAttack(du, f.unit.lv, lv, treasureSetting, talent, false)
                    .multiply(waveChance)
                    .multiply(waveMultiplier);

            BigDecimal position = BigDecimal.ZERO;

            //Initial Position
            BigDecimal width = BigDecimal.valueOf(Data.W_U_WID);
            BigDecimal offset = BigDecimal.valueOf(Data.W_U_INI);

            position = position.add(offset).add(width.divide(new BigDecimal("2"), Equation.context));

            BigDecimal halfWidth = BigDecimal.valueOf(Data.W_E_WID).divide(new BigDecimal("2"), Equation.context);

            for (BigDecimal wv = waveLevel; wv.compareTo(BigDecimal.ZERO) > 0; wv = wv.subtract(BigDecimal.ONE)) {
                dpsNodes.add(new DPSNode(position.subtract(halfWidth), BigDecimal.ZERO, waveDamage));
                dpsNodes.add(new DPSNode(position.add(halfWidth), BigDecimal.ZERO, waveDamage));

                position = position.add(BigDecimal.valueOf(Data.W_PROG));
            }

            if (treasure) {
                waveDamage = getTotalAbilityAttack(du, f.unit.lv, lv, treasureSetting, talent, true)
                        .multiply(waveChance)
                        .multiply(waveMultiplier);

                position = BigDecimal.ZERO;

                width = BigDecimal.valueOf(Data.W_U_WID);
                offset = BigDecimal.valueOf(Data.W_U_INI);

                position = position.add(offset).add(width.divide(new BigDecimal("2"), Equation.context));

                for (BigDecimal wv = waveLevel; wv.compareTo(BigDecimal.ZERO) > 0; wv = wv.subtract(BigDecimal.ONE)) {
                    treasureNodes.add(new DPSNode(position.subtract(halfWidth), BigDecimal.ZERO, waveDamage));
                    treasureNodes.add(new DPSNode(position.add(halfWidth), BigDecimal.ZERO, waveDamage));

                    position = position.add(BigDecimal.valueOf(Data.W_PROG));
                }
            }
        }

        if (blastAbility.exists()) {
            BigDecimal blastChance = BigDecimal.valueOf(blastAbility.prob).divide(BigDecimal.valueOf(100), Equation.context);

            for (int i = 0; i < 5; i++) {
                BigDecimal blastWidth;
                BigDecimal blastOffset;
                BigDecimal blastMultiplier;

                switch (i) {
                    case 0, 4 -> {
                        blastWidth = BigDecimal.valueOf(Data.BLAST_RANGE[2]);

                        if (i == 0) {
                            blastOffset = BigDecimal.valueOf(-Data.BLAST_RANGE[0] / 2 - Data.BLAST_RANGE[1] - Data.BLAST_RANGE[2] / 2);
                        } else {
                            blastOffset = BigDecimal.valueOf(Data.BLAST_RANGE[0] / 2 + Data.BLAST_RANGE[1] + Data.BLAST_RANGE[2] / 2);
                        }

                        blastMultiplier = BigDecimal.valueOf(Data.BLAST_MULTIPLIER[2]).divide(BigDecimal.valueOf(100), Equation.context);
                    }
                    case 1, 3 -> {
                        blastWidth = BigDecimal.valueOf(Data.BLAST_RANGE[1]);

                        if (i == 1) {
                            blastOffset = BigDecimal.valueOf(-Data.BLAST_RANGE[0] / 2 - Data.BLAST_RANGE[1] / 2);
                        } else {
                            blastOffset = BigDecimal.valueOf(Data.BLAST_RANGE[0] / 2 + Data.BLAST_RANGE[1] / 2);
                        }

                        blastMultiplier = BigDecimal.valueOf(Data.BLAST_MULTIPLIER[1]).divide(BigDecimal.valueOf(100), Equation.context);
                    }
                    case 2 -> {
                        blastWidth = BigDecimal.valueOf(Data.BLAST_RANGE[0]);
                        blastOffset = BigDecimal.ZERO;
                        blastMultiplier = BigDecimal.valueOf(Data.BLAST_MULTIPLIER[0]).divide(BigDecimal.valueOf(100), Equation.context);
                    }
                    default -> throw new IllegalStateException("E/EntityHandler::showEnemyDPS - Invalid blast index %d".formatted(i));
                }

                BigDecimal longBlastDistance = BigDecimal.valueOf(blastAbility.dis_1);
                BigDecimal shortBlastDistance = BigDecimal.valueOf(blastAbility.dis_0);

                BigDecimal halfWidth = blastWidth.divide(BigDecimal.valueOf(2), Equation.context);

                BigDecimal minimumDistance = shortBlastDistance.min(longBlastDistance).add(blastOffset);
                BigDecimal maximumDistance = shortBlastDistance.max(longBlastDistance).add(blastOffset);

                BigDecimal minimumRange = minimumDistance.subtract(halfWidth);
                BigDecimal maximumRange = maximumDistance.add(halfWidth);

                BigDecimal minimumPierce = minimumDistance.add(halfWidth);
                BigDecimal maximumInner = maximumDistance.subtract(halfWidth);

                BigDecimal hitChance;

                if (minimumPierce.subtract(maximumInner).compareTo(BigDecimal.ZERO) > 0) {
                    hitChance = BigDecimal.ONE;
                } else {
                    hitChance = blastWidth.divide(maximumDistance.subtract(minimumDistance), Equation.context);
                }

                BigDecimal blastDamage = getTotalAbilityAttack(du, f.unit.lv, lv, treasureSetting, talent, false)
                        .multiply(hitChance)
                        .multiply(blastMultiplier)
                        .multiply(blastChance);

                BigDecimal valueDifference = minimumPierce.min(maximumInner).subtract(minimumRange);

                if (valueDifference.compareTo(BigDecimal.ZERO) == 0) {
                    dpsNodes.add(new DPSNode(minimumRange, BigDecimal.ZERO, blastDamage));
                    dpsNodes.add(new DPSNode(maximumRange, BigDecimal.ZERO, blastDamage.negate(), true));
                } else {
                    BigDecimal slope = blastDamage.divide(minimumPierce.min(maximumInner).subtract(minimumRange), Equation.context);

                    dpsNodes.add(new DPSNode(minimumRange, slope, BigDecimal.ZERO));
                    dpsNodes.add(new DPSNode(minimumPierce.min(maximumInner), slope.negate(), BigDecimal.ZERO));
                    dpsNodes.add(new DPSNode(minimumPierce.max(maximumInner), slope.negate(), BigDecimal.ZERO));
                    dpsNodes.add(new DPSNode(maximumRange, slope, BigDecimal.ZERO));
                }

                if (treasure) {
                    blastDamage = getTotalAbilityAttack(du, f.unit.lv, lv, treasureSetting, talent, true)
                            .multiply(hitChance)
                            .multiply(blastMultiplier)
                            .multiply(blastChance);

                    if (valueDifference.compareTo(BigDecimal.ZERO) == 0) {
                        treasureNodes.add(new DPSNode(minimumRange, BigDecimal.ZERO, blastDamage));
                        treasureNodes.add(new DPSNode(maximumRange, BigDecimal.ZERO, blastDamage.negate(), true));
                    } else {
                        BigDecimal slope = blastDamage.divide(minimumPierce.min(maximumInner).subtract(minimumRange), Equation.context);

                        treasureNodes.add(new DPSNode(minimumRange, slope, BigDecimal.ZERO));
                        treasureNodes.add(new DPSNode(minimumPierce.min(maximumInner), slope.negate(), BigDecimal.ZERO));
                        treasureNodes.add(new DPSNode(minimumPierce.max(maximumInner), slope.negate(), BigDecimal.ZERO));
                        treasureNodes.add(new DPSNode(maximumRange, slope, BigDecimal.ZERO));
                    }
                }
            }
        }

        dpsNodes.sort((n0, n1) -> {
            if (n0 == null && n1 == null) {
                return 0;
            }

            if (n0 == null) {
                return -1;
            }

            if (n1 == null) {
                return 1;
            }

            int xCoordinateCompare = n0.xCoordinate.compareTo(n1.xCoordinate);

            if (xCoordinateCompare == 0) {
                if (n0.ignoreValue) {
                    return -1;
                }

                if (n1.ignoreValue) {
                    return 1;
                }

                return -n0.valueChange.compareTo(n1.valueChange);
            } else {
                return xCoordinateCompare;
            }
        });

        treasureNodes.sort((n0, n1) -> {
            if (n0 == null && n1 == null) {
                return 0;
            }

            if (n0 == null) {
                return -1;
            }

            if (n1 == null) {
                return 1;
            }

            int xCoordinateCompare = n0.xCoordinate.compareTo(n1.xCoordinate);

            if (xCoordinateCompare == 0) {
                if (n0.ignoreValue) {
                    return -1;
                }

                if (n1.ignoreValue) {
                    return 1;
                }

                return -n0.valueChange.compareTo(n1.valueChange);
            } else {
                return xCoordinateCompare;
            }
        });

        dpsNodes.addFirst(new DPSNode(dpsNodes.getFirst().xCoordinate.subtract(BigDecimal.valueOf(100)).min(BigDecimal.valueOf(-320)), BigDecimal.ZERO, BigDecimal.ZERO));
        dpsNodes.addLast(new DPSNode(dpsNodes.getLast().xCoordinate.add(BigDecimal.valueOf(100)), BigDecimal.ZERO, BigDecimal.ZERO));

        if (treasure) {
            treasureNodes.addFirst(new DPSNode(dpsNodes.getFirst().xCoordinate.subtract(BigDecimal.valueOf(100)).min(BigDecimal.valueOf(-320)), BigDecimal.ZERO, BigDecimal.ZERO));
            treasureNodes.addLast(new DPSNode(dpsNodes.getLast().xCoordinate.add(BigDecimal.valueOf(100)), BigDecimal.ZERO, BigDecimal.ZERO));
        }

        // Ignore value must be false if there's only that node on each X point
        for (int i = 0; i < dpsNodes.size(); i++) {
            if (i < dpsNodes.size() - 1) {
                DPSNode currentNode = dpsNodes.get(i);
                DPSNode nextNode = dpsNodes.get(i + 1);

                if (currentNode.ignoreValue && currentNode.xCoordinate.compareTo(nextNode.xCoordinate) != 0) {
                    currentNode.ignoreValue = false;
                }
            }
        }

        if (treasure) {
            for (int i = 0; i < treasureNodes.size(); i++) {
                if (i < treasureNodes.size() - 1) {
                    DPSNode currentNode = treasureNodes.get(i);
                    DPSNode nextNode = treasureNodes.get(i + 1);

                    if (currentNode.ignoreValue && currentNode.xCoordinate.compareTo(nextNode.xCoordinate) != 0) {
                        currentNode.ignoreValue = false;
                    }
                }
            }
        }

        List<BigDecimal[]> coordinates = new ArrayList<>();
        List<BigDecimal[]> withTreasure = new ArrayList<>();

        BigDecimal currentYValue = BigDecimal.ZERO;
        BigDecimal currentSlope = BigDecimal.ZERO;

        for (int i = 0; i < dpsNodes.size(); i++) {
            DPSNode currentNode = dpsNodes.get(i);

            if (i == 0) {
                coordinates.add(new BigDecimal[]{ currentNode.xCoordinate, BigDecimal.ZERO });

                continue;
            }

            DPSNode previousNode = dpsNodes.get(i - 1);

            BigDecimal dx = currentNode.xCoordinate.subtract(previousNode.xCoordinate);

            if (dx.compareTo(BigDecimal.ZERO) != 0) {
                currentYValue = currentYValue.add(dx.multiply(currentSlope));

                coordinates.add(new BigDecimal[]{ currentNode.xCoordinate, currentYValue });
            }

            if (currentNode.valueChange.compareTo(BigDecimal.ZERO) != 0) {
                currentYValue = currentYValue.add(currentNode.valueChange);

                if (!currentNode.ignoreValue) {
                    coordinates.add(new BigDecimal[]{ currentNode.xCoordinate, currentYValue });
                }
            }

            if (currentNode.slopeChange.compareTo(BigDecimal.ZERO) != 0) {
                currentSlope = currentSlope.add(currentNode.slopeChange);
            }
        }

        if (treasure) {
            currentYValue = BigDecimal.ZERO;
            currentSlope = BigDecimal.ZERO;

            for (int i = 0; i < treasureNodes.size(); i++) {
                DPSNode currentNode = treasureNodes.get(i);

                if (i == 0) {
                    withTreasure.add(new BigDecimal[]{ currentNode.xCoordinate, BigDecimal.ZERO });

                    continue;
                }

                DPSNode previousNode = treasureNodes.get(i - 1);

                BigDecimal dx = currentNode.xCoordinate.subtract(previousNode.xCoordinate);

                if (dx.compareTo(BigDecimal.ZERO) != 0) {
                    currentYValue = currentYValue.add(dx.multiply(currentSlope));

                    withTreasure.add(new BigDecimal[]{ currentNode.xCoordinate, currentYValue });
                }

                if (currentNode.valueChange.compareTo(BigDecimal.ZERO) != 0) {
                    currentYValue = currentYValue.add(currentNode.valueChange);

                    if (!currentNode.ignoreValue) {
                        withTreasure.add(new BigDecimal[]{ currentNode.xCoordinate, currentYValue });
                    }
                }

                if (currentNode.slopeChange.compareTo(BigDecimal.ZERO) != 0) {
                    currentSlope = currentSlope.add(currentNode.slopeChange);
                }
            }
        }

        for (int i = 0; i < coordinates.size(); i++) {
            coordinates.get(i)[1] = coordinates.get(i)[1].divide(BigDecimal.valueOf(du.getItv()).divide(BigDecimal.valueOf(30), Equation.context), Equation.context);
        }

        BigDecimal maximumDamage = BigDecimal.ZERO;
        BigDecimal minimumX = new BigDecimal("-320");
        BigDecimal maximumX = new BigDecimal("-320");

        for (int i = 0; i < coordinates.size(); i++) {
            maximumDamage = maximumDamage.max(coordinates.get(i)[1]);

            minimumX = minimumX.min(coordinates.get(i)[0]);
            maximumX = maximumX.max(coordinates.get(i)[0]);
        }

        if (treasure) {
            for (int i = 0; i < withTreasure.size(); i++) {
                maximumDamage = maximumDamage.max(withTreasure.get(i)[1]);

                minimumX = minimumX.min(withTreasure.get(i)[0]);
                maximumX = maximumX.max(withTreasure.get(i)[0]);
            }
        }

        if (maximumDamage.compareTo(BigDecimal.ZERO) == 0) {
            maximumDamage = BigDecimal.TEN;
        }

        File result;

        boolean identical = allCoordinatesSame(coordinates, withTreasure);

        if (treasure && !identical) {
            result = ImageDrawing.plotDPSGraph(coordinates.toArray(new BigDecimal[0][0]), withTreasure.toArray(new BigDecimal[0][0]), new BigDecimal[] { minimumX, maximumX }, new BigDecimal[] { BigDecimal.ZERO, maximumDamage.multiply(new BigDecimal("1.1")) }, lang);
        } else {
            result = ImageDrawing.plotDPSGraph(coordinates.toArray(new BigDecimal[0][0]), null, new BigDecimal[] { minimumX, maximumX }, new BigDecimal[] { BigDecimal.ZERO, maximumDamage.multiply(new BigDecimal("1.1")) }, lang);
        }

        if (result == null) {
            Command.replyToMessageSafely(ch, LangID.getStringByID("formDPS.failed.unknown", lang), authorMessage, a -> a);
        } else {
            EmbedBuilder spec = new EmbedBuilder();

            String name = MultiLangCont.get(f, lang);

            if(name == null || name.isBlank())
                name = Data.trio(f.unit.id.id) + "-" + Data.trio(f.fid);

            String desc;

            if (lv.getPlusLv() == 0) {
                desc = String.format(LangID.getStringByID("formDPS.graph.description.default", lang), lv.getLv());
            } else {
                desc = String.format(LangID.getStringByID("formDPS.graph.description.withPlus", lang), lv.getLv(), lv.getPlusLv());
            }

            if (talent && f.du.getPCoin() != null) {
                desc += "\n" + String.format(LangID.getStringByID("formDPS.graph.description.talent", lang), StringUtils.joinS(ArrayUtils.toObject(lv.getTalents()), ", "));
            }

            if (treasureSetting.differentFromGlobal()) {
                desc = "\n\n" + LangID.getStringByID("data.unit.treasure", lang);
            }

            if (talent && f.du.getPCoin() != null) {
                desc += "\n\n" + LangID.getStringByID("data.unit.talent.embed", lang);
            }

            if (treasure && !identical) {
                desc += "\n\n" + String.format(LangID.getStringByID("formDPS.graph.legend", lang), EmojiStore.GREENLINE.getFormatted(), EmojiStore.REDDASHEDLINE.getFormatted());
            }

            int c;

            if(f.fid == 0)
                c = StaticStore.rainbow[4];
            else if(f.fid == 1)
                c = StaticStore.rainbow[3];
            else
                c = StaticStore.rainbow[2];

            spec.setTitle(String.format(LangID.getStringByID("formDPS.graph.title", lang), name));

            if (!desc.isBlank()) {
                spec.setDescription(desc);
            }

            spec.setColor(c);

            spec.setImage("attachment://graph.png");
            spec.setThumbnail("attachment://icon.png");

            if (talent && f.du.getPCoin() != null) {
                spec.setFooter(DataToString.getTalent(f.du, lv, lang));
            }

            File icon = generateIcon(f);

            Command.replyToMessageSafely(ch, "", authorMessage, a -> {
                a = a.setEmbeds(spec.build())
                        .addFiles(FileUpload.fromData(result, "graph.png"));

                if (icon != null)
                    a = a.addFiles(FileUpload.fromData(icon, "icon.png"));

                return a;
            }, m -> {
                if(result.exists() && !result.delete()) {
                    StaticStore.logger.uploadLog("Failed to delete file : "+result.getAbsolutePath());
                }

                if (icon != null && icon.exists() && !icon.delete()) {
                    StaticStore.logger.uploadLog("Failed to delete file : " + icon.getAbsolutePath());
                }
            });
        }
    }

    public static void showEnemyDPS(MessageChannel ch, Message authorMessage, Enemy e, TreasureHolder treasureSetting, int magnification, CommonStatic.Lang.Locale lang) throws Exception {
        int adjustedMagnification;

        if (magnification <= 0) {
            adjustedMagnification = 100;
        } else {
            adjustedMagnification = magnification;
        }

        if (e.de.getTraits().contains(TreasureHolder.fullTraits.get(Data.TRAIT_ALIEN))) {
            adjustedMagnification = (int) Math.round(adjustedMagnification * (e.de.getStar() == 0 ? treasureSetting.getAlienMultiplier() : treasureSetting.getStarredAlienMultiplier()));
        }

        List<DPSNode> dpsNodes = new ArrayList<>();

        MaskEnemy de = e.de;

        // Damage Calculation
        for (int i = 0; i < de.getAtkCount(); i++) {
            if (!de.isLD() && !de.isOmni()) {
                dpsNodes.add(new DPSNode(BigDecimal.valueOf(-320), BigDecimal.ZERO, getAttack(i, de, adjustedMagnification)));
                dpsNodes.add(new DPSNode(BigDecimal.valueOf(de.getRange()), BigDecimal.ZERO, getAttack(i, de, adjustedMagnification).negate()));
            } else {
                MaskAtk attack = de.getAtkModel(i);

                int shortPoint = attack.getShortPoint();
                int width = attack.getLongPoint() - attack.getShortPoint();

                dpsNodes.add(new DPSNode(BigDecimal.valueOf(Math.min(shortPoint, shortPoint + width)), BigDecimal.ZERO, getAttack(i, de, adjustedMagnification)));
                dpsNodes.add(new DPSNode(BigDecimal.valueOf(Math.max(shortPoint, shortPoint + width)), BigDecimal.ZERO, getAttack(i, de, adjustedMagnification).negate()));
            }
        }

        MaskAtk representativeAttack = de.getRepAtk();

        Data.Proc.VOLC surgeAbility = representativeAttack.getProc().VOLC;
        Data.Proc.MINIVOLC miniSurgeAbility = representativeAttack.getProc().MINIVOLC;

        Data.Proc.WAVE waveAbility = representativeAttack.getProc().WAVE;
        Data.Proc.MINIWAVE miniWaveAbility = representativeAttack.getProc().MINIWAVE;

        Data.Proc.BLAST blastAbility = representativeAttack.getProc().BLAST;

        // Handling surge
        if (surgeAbility.exists() || miniSurgeAbility.exists()) {
            BigDecimal shortSurgeDistance;
            BigDecimal longSurgeDistance;
            BigDecimal surgeLevel;
            BigDecimal surgeChance;
            BigDecimal surgeMultiplier;

            if (surgeAbility.exists()) {
                shortSurgeDistance = BigDecimal.valueOf(surgeAbility.dis_0);
                longSurgeDistance = BigDecimal.valueOf(surgeAbility.dis_1);
                surgeLevel = BigDecimal.valueOf(surgeAbility.time).divide(BigDecimal.valueOf(Data.VOLC_ITV), Equation.context);
                surgeChance = BigDecimal.valueOf(surgeAbility.prob).divide(new BigDecimal("100"), Equation.context);
                surgeMultiplier = BigDecimal.ONE;
            } else {
                shortSurgeDistance = BigDecimal.valueOf(miniSurgeAbility.dis_0);
                longSurgeDistance = BigDecimal.valueOf(miniSurgeAbility.dis_1);
                surgeLevel = BigDecimal.valueOf(miniSurgeAbility.time).divide(new BigDecimal("20"), Equation.context);
                surgeChance = BigDecimal.valueOf(miniSurgeAbility.prob).divide(new BigDecimal("100"), Equation.context);
                surgeMultiplier = BigDecimal.valueOf(miniSurgeAbility.mult).divide(new BigDecimal("100"), Equation.context);
            }

            BigDecimal minimumDistance = shortSurgeDistance.min(longSurgeDistance);
            BigDecimal maximumDistance = shortSurgeDistance.max(longSurgeDistance);

            BigDecimal minimumRange = minimumDistance.subtract(BigDecimal.valueOf(Data.W_VOLC_INNER));
            BigDecimal maximumRange = maximumDistance.add(BigDecimal.valueOf(Data.W_VOLC_PIERCE));

            BigDecimal minimumPierce = minimumDistance.add(BigDecimal.valueOf(Data.W_VOLC_PIERCE));
            BigDecimal maximumInner = maximumDistance.subtract(BigDecimal.valueOf(Data.W_VOLC_INNER));

            BigDecimal hitChance;

            if (minimumPierce.subtract(maximumInner).compareTo(BigDecimal.ZERO) > 0) {
                hitChance = BigDecimal.ONE;
            } else {
                hitChance = BigDecimal.valueOf(Data.W_VOLC_INNER + Data.W_VOLC_PIERCE)
                        .divide(maximumDistance.subtract(minimumDistance), Equation.context);
            }

            BigDecimal surgeDamage = getTotalAbilityAttack(de, adjustedMagnification)
                    .multiply(hitChance)
                    .multiply(surgeChance)
                    .multiply(surgeLevel)
                    .multiply(surgeMultiplier);

            BigDecimal valueDifference = minimumPierce.min(maximumInner).subtract(minimumRange);

            if (valueDifference.compareTo(BigDecimal.ZERO) == 0) {
                dpsNodes.add(new DPSNode(minimumRange, BigDecimal.ZERO, surgeDamage));
                dpsNodes.add(new DPSNode(maximumRange, BigDecimal.ZERO, surgeDamage.negate(), true));
            } else {
                BigDecimal slope = surgeDamage.divide(minimumPierce.min(maximumInner).subtract(minimumRange), Equation.context);

                dpsNodes.add(new DPSNode(minimumRange, slope, BigDecimal.ZERO));
                dpsNodes.add(new DPSNode(minimumPierce.min(maximumInner), slope.negate(), BigDecimal.ZERO));
                dpsNodes.add(new DPSNode(minimumPierce.max(maximumInner), slope.negate(), BigDecimal.ZERO));
                dpsNodes.add(new DPSNode(maximumRange, slope, BigDecimal.ZERO));
            }
        }

        // Handling wave
        if (waveAbility.exists() || miniWaveAbility.exists()) {
            BigDecimal waveChance;
            BigDecimal waveLevel;
            BigDecimal waveMultiplier;

            if (waveAbility.exists()) {
                waveChance = BigDecimal.valueOf(waveAbility.prob).divide(new BigDecimal("100"), Equation.context);
                waveLevel = BigDecimal.valueOf(waveAbility.lv);
                waveMultiplier = BigDecimal.ONE;
            } else {
                waveChance = BigDecimal.valueOf(miniWaveAbility.prob).divide(new BigDecimal("100"), Equation.context);
                waveLevel = BigDecimal.valueOf(miniWaveAbility.lv);
                waveMultiplier = BigDecimal.valueOf(miniWaveAbility.multi).divide(new BigDecimal("100"), Equation.context);
            }

            BigDecimal waveDamage = getTotalAbilityAttack(de, adjustedMagnification)
                    .multiply(waveChance)
                    .multiply(waveMultiplier);

            BigDecimal position = BigDecimal.ZERO;

            //Initial Position
            BigDecimal width = BigDecimal.valueOf(Data.W_E_WID);
            BigDecimal offset = BigDecimal.valueOf(Data.W_E_INI);

            position = position.add(offset).add(width.divide(new BigDecimal("2"), Equation.context));

            BigDecimal halfWidth = BigDecimal.valueOf(Data.W_E_WID).divide(new BigDecimal("2"), Equation.context);

            for (BigDecimal wv = waveLevel; wv.compareTo(BigDecimal.ZERO) > 0; wv = wv.subtract(BigDecimal.ONE)) {
                dpsNodes.add(new DPSNode(position.subtract(halfWidth), BigDecimal.ZERO, waveDamage));
                dpsNodes.add(new DPSNode(position.add(halfWidth), BigDecimal.ZERO, waveDamage));

                position = position.add(BigDecimal.valueOf(Data.W_PROG));
            }
        }

        // Handling blast
        if (blastAbility.exists()) {
            BigDecimal blastChance = BigDecimal.valueOf(blastAbility.prob).divide(BigDecimal.valueOf(100), Equation.context);

            for (int i = 0; i < 5; i++) {
                BigDecimal blastWidth;
                BigDecimal blastOffset;
                BigDecimal blastMultiplier;

                switch (i) {
                    case 0, 4 -> {
                        blastWidth = BigDecimal.valueOf(Data.BLAST_RANGE[2]);

                        if (i == 0) {
                            blastOffset = BigDecimal.valueOf(-Data.BLAST_RANGE[0] / 2 - Data.BLAST_RANGE[1] - Data.BLAST_RANGE[2] / 2);
                        } else {
                            blastOffset = BigDecimal.valueOf(Data.BLAST_RANGE[0] / 2 + Data.BLAST_RANGE[1] + Data.BLAST_RANGE[2] / 2);
                        }

                        blastMultiplier = BigDecimal.valueOf(Data.BLAST_MULTIPLIER[2]).divide(BigDecimal.valueOf(100), Equation.context);
                    }
                    case 1, 3 -> {
                        blastWidth = BigDecimal.valueOf(Data.BLAST_RANGE[1]);

                        if (i == 1) {
                            blastOffset = BigDecimal.valueOf(-Data.BLAST_RANGE[0] / 2 - Data.BLAST_RANGE[1] / 2);
                        } else {
                            blastOffset = BigDecimal.valueOf(Data.BLAST_RANGE[0] / 2 + Data.BLAST_RANGE[1] / 2);
                        }

                        blastMultiplier = BigDecimal.valueOf(Data.BLAST_MULTIPLIER[1]).divide(BigDecimal.valueOf(100), Equation.context);
                    }
                    case 2 -> {
                        blastWidth = BigDecimal.valueOf(Data.BLAST_RANGE[0]);
                        blastOffset = BigDecimal.ZERO;
                        blastMultiplier = BigDecimal.valueOf(Data.BLAST_MULTIPLIER[0]).divide(BigDecimal.valueOf(100), Equation.context);
                    }
                    default -> throw new IllegalStateException("E/EntityHandler::showEnemyDPS - Invalid blast index %d".formatted(i));
                }

                BigDecimal longBlastDistance = BigDecimal.valueOf(blastAbility.dis_1);
                BigDecimal shortBlastDistance = BigDecimal.valueOf(blastAbility.dis_0);

                BigDecimal halfWidth = blastWidth.divide(BigDecimal.valueOf(2), Equation.context);

                BigDecimal minimumDistance = shortBlastDistance.min(longBlastDistance).add(blastOffset);
                BigDecimal maximumDistance = shortBlastDistance.max(longBlastDistance).add(blastOffset);

                BigDecimal minimumRange = minimumDistance.subtract(halfWidth);
                BigDecimal maximumRange = maximumDistance.add(halfWidth);

                BigDecimal minimumPierce = minimumDistance.add(halfWidth);
                BigDecimal maximumInner = maximumDistance.subtract(halfWidth);

                BigDecimal hitChance;

                if (minimumPierce.subtract(maximumInner).compareTo(BigDecimal.ZERO) > 0) {
                    hitChance = BigDecimal.ONE;
                } else {
                    hitChance = blastWidth.divide(maximumDistance.subtract(minimumDistance), Equation.context);
                }

                BigDecimal blastDamage = getTotalAbilityAttack(de, adjustedMagnification)
                        .multiply(hitChance)
                        .multiply(blastMultiplier)
                        .multiply(blastChance);

                BigDecimal valueDifference = minimumPierce.min(maximumInner).subtract(minimumRange);

                if (valueDifference.compareTo(BigDecimal.ZERO) == 0) {
                    dpsNodes.add(new DPSNode(minimumRange, BigDecimal.ZERO, blastDamage));
                    dpsNodes.add(new DPSNode(maximumRange, BigDecimal.ZERO, blastDamage.negate(), true));
                } else {
                    BigDecimal slope = blastDamage.divide(minimumPierce.min(maximumInner).subtract(minimumRange), Equation.context);

                    dpsNodes.add(new DPSNode(minimumRange, slope, BigDecimal.ZERO));
                    dpsNodes.add(new DPSNode(minimumPierce.min(maximumInner), slope.negate(), BigDecimal.ZERO));
                    dpsNodes.add(new DPSNode(minimumPierce.max(maximumInner), slope.negate(), BigDecimal.ZERO));
                    dpsNodes.add(new DPSNode(maximumRange, slope, BigDecimal.ZERO));
                }
            }
        }

        dpsNodes.sort((n0, n1) -> {
            if (n0 == null && n1 == null) {
                return 0;
            }

            if (n0 == null) {
                return -1;
            }

            if (n1 == null) {
                return 1;
            }

            int xCoordinateCompare = n0.xCoordinate.compareTo(n1.xCoordinate);

            if (xCoordinateCompare == 0) {
                if (n0.ignoreValue) {
                    return -1;
                }

                if (n1.ignoreValue) {
                    return 1;
                }

                return -n0.valueChange.compareTo(n1.valueChange);
            } else {
                return xCoordinateCompare;
            }
        });

        dpsNodes.addFirst(new DPSNode(dpsNodes.getFirst().xCoordinate.subtract(BigDecimal.valueOf(100)).min(BigDecimal.valueOf(-320)), BigDecimal.ZERO, BigDecimal.ZERO));

        dpsNodes.addLast(new DPSNode(dpsNodes.getLast().xCoordinate.add(BigDecimal.valueOf(100)), BigDecimal.ZERO, BigDecimal.ZERO));

        // Ignore value must be false if there's only that node on each X point
        for (int i = 0; i < dpsNodes.size(); i++) {
            if (i < dpsNodes.size() - 1) {
                DPSNode currentNode = dpsNodes.get(i);
                DPSNode nextNode = dpsNodes.get(i + 1);

                if (currentNode.ignoreValue && currentNode.xCoordinate.compareTo(nextNode.xCoordinate) != 0) {
                    currentNode.ignoreValue = false;
                }
            }
        }

        List<BigDecimal[]> coordinates = new ArrayList<>();

        BigDecimal currentYValue = BigDecimal.ZERO;
        BigDecimal currentSlope = BigDecimal.ZERO;

        for (int i = 0; i < dpsNodes.size(); i++) {
            DPSNode currentNode = dpsNodes.get(i);

            if (i == 0) {
                coordinates.add(new BigDecimal[]{ currentNode.xCoordinate, BigDecimal.ZERO });

                continue;
            }

            DPSNode previousNode = dpsNodes.get(i - 1);

            BigDecimal dx = currentNode.xCoordinate.subtract(previousNode.xCoordinate);

            if (dx.compareTo(BigDecimal.ZERO) != 0) {
                currentYValue = currentYValue.add(dx.multiply(currentSlope));

                coordinates.add(new BigDecimal[]{ currentNode.xCoordinate, currentYValue });
            }

            if (currentNode.valueChange.compareTo(BigDecimal.ZERO) != 0) {
                currentYValue = currentYValue.add(currentNode.valueChange);

                if (!currentNode.ignoreValue) {
                    coordinates.add(new BigDecimal[]{ currentNode.xCoordinate, currentYValue });
                }
            }

            if (currentNode.slopeChange.compareTo(BigDecimal.ZERO) != 0) {
                currentSlope = currentSlope.add(currentNode.slopeChange);
            }
        }

        for (int i = 0; i < coordinates.size(); i++) {
            coordinates.get(i)[1] = coordinates.get(i)[1].divide(BigDecimal.valueOf(de.getItv()).divide(BigDecimal.valueOf(30), Equation.context), Equation.context);
        }

        BigDecimal maximumDamage = BigDecimal.ZERO;
        BigDecimal minimumX = new BigDecimal("-320");
        BigDecimal maximumX = new BigDecimal("-320");

        for (int i = 0; i < coordinates.size(); i++) {
            maximumDamage = maximumDamage.max(coordinates.get(i)[1]);

            minimumX = minimumX.min(coordinates.get(i)[0]);
            maximumX = maximumX.max(coordinates.get(i)[0]);
        }

        if (maximumDamage.compareTo(BigDecimal.ZERO) == 0) {
            maximumDamage = BigDecimal.TEN;
        }

        File result = ImageDrawing.plotDPSGraph(coordinates.toArray(new BigDecimal[0][0]), null, new BigDecimal[] { minimumX, maximumX }, new BigDecimal[] { BigDecimal.ZERO, maximumDamage.multiply(new BigDecimal("1.1")) }, lang);

        if (result == null) {
            Command.replyToMessageSafely(ch, LangID.getStringByID("formDPS.failed.unknown", lang), authorMessage, a -> a);
        } else {
            EmbedBuilder spec = new EmbedBuilder();

            String name = MultiLangCont.get(e, lang);

            if(name == null || name.isBlank())
                name = Data.trio(e.id.id);

            String desc = String.format(LangID.getStringByID("enemyDPS.graph.magnification", lang), adjustedMagnification);

            if (treasureSetting.differentFromGlobal()) {
                desc += "\n\n" + LangID.getStringByID("data.unit.treasure", lang);
            }

            spec.setTitle(String.format(LangID.getStringByID("formDPS.graph.title", lang), name));

            if (!desc.isBlank()) {
                spec.setDescription(desc);
            }

            spec.setColor(StaticStore.rainbow[0]);

            spec.setImage("attachment://graph.png");
            spec.setThumbnail("attachment://icon.png");

            File icon = generateIcon(e);

            Command.replyToMessageSafely(ch, "", authorMessage, a -> {
                a = a.setEmbeds(spec.build())
                        .addFiles(FileUpload.fromData(result, "graph.png"));

                if (icon != null) {
                    a = a.addFiles(FileUpload.fromData(icon, "icon.png"));
                }

                return a;
            }, m -> {
                if(result.exists() && !result.delete()) {
                    StaticStore.logger.uploadLog("Failed to delete file : "+result.getAbsolutePath());
                }

                if (icon != null && icon.exists() && !icon.delete()) {
                    StaticStore.logger.uploadLog("Failed to delete file : " + icon.getAbsolutePath());
                }
            });
        }
    }

    private static BigDecimal getTotalAbilityAttack(MaskUnit data, UnitLevel levelCurve, Level lv, TreasureHolder t, boolean talent, boolean treasure) {
        BigDecimal result = BigDecimal.ZERO;

        for (int i = 0; i < data.getAtkCount(); i++) {
            boolean abilityApplied = data.rawAtkData()[i][2] == 1;

            if (abilityApplied) {
                result = result.add(getAttack(i, data, levelCurve, lv, t, talent, treasure));
            }
        }

        return result;
    }

    private static BigDecimal getTotalAbilityAttack(MaskEnemy data, int magnification) {
        BigDecimal result = BigDecimal.ZERO;

        for (int i = 0; i < data.getAtkCount(); i++) {
            boolean abilityApplied = data.rawAtkData()[i][2] == 1;

            if (abilityApplied) {
                result = result.add(getAttack(i, data, magnification));
            }
        }

        return result;
    }

    private static BigDecimal getAttack(int index, MaskUnit data, UnitLevel levelCurve, Level lv, TreasureHolder t, boolean talent, boolean treasure) {
        MaskAtk attack = data.getAtkModel(index);

        BigDecimal damageTotalMultiplier = BigDecimal.ONE;

        if (data.rawAtkData()[index][2] == 1) {
            Data.Proc.PM savageBlow = attack.getProc().SATK;
            Data.Proc.PM critical = attack.getProc().CRIT;

            if (savageBlow.exists()) {
                damageTotalMultiplier = damageTotalMultiplier.multiply(BigDecimal.ONE.add(BigDecimal.valueOf(savageBlow.mult).divide(new BigDecimal("100"), Equation.context).multiply(BigDecimal.valueOf(savageBlow.prob).divide(new BigDecimal("100"), Equation.context))));
            }

            if (critical.exists()) {
                damageTotalMultiplier = damageTotalMultiplier.multiply(BigDecimal.ONE.add(BigDecimal.ONE.multiply(BigDecimal.valueOf(critical.prob).divide(new BigDecimal("100"), Equation.context))));
            }
        }

        int result;

        if(data.getPCoin() != null && talent) {
            result = (int) ((int) (Math.round(attack.getAtk() * levelCurve.getMult(lv.getLv() + lv.getPlusLv())) * t.getAtkMultiplier()) * data.getPCoin().getAtkMultiplication(lv.getTalents()));
        } else {
            result = (int) (Math.round(attack.getAtk() * levelCurve.getMult(lv.getLv() + lv.getPlusLv())) * t.getAtkMultiplier());
        }

        if(treasure) {
            List<Trait> traits = data.getTraits();

            if((data.getAbi() & Data.AB_GOOD) > 0) {
                result = (int) (result * t.getStrongAttackMultiplier(traits));
            }

            if((data.getAbi() & Data.AB_MASSIVE) > 0) {
                result = (int) (result * t.getMassiveAttackMultiplier(traits));
            }

            if((data.getAbi() & Data.AB_MASSIVES) > 0) {
                result = (int) (result * t.getInsaneMassiveAttackMultiplier(traits));
            }
        }

        return BigDecimal.valueOf(result).multiply(damageTotalMultiplier);
    }

    private static BigDecimal getAttack(int index, MaskEnemy data, int magnification) {
        MaskAtk attack = data.getAtkModel(index);

        BigDecimal damageTotalMultiplier = BigDecimal.ONE;

        if (data.rawAtkData()[index][2] == 1) {
            Data.Proc.PM savageBlow = attack.getProc().SATK;
            Data.Proc.PM critical = attack.getProc().CRIT;

            if (savageBlow.exists()) {
                damageTotalMultiplier = damageTotalMultiplier.multiply(BigDecimal.ONE.add(BigDecimal.valueOf(savageBlow.mult).divide(new BigDecimal("100"), Equation.context).multiply(BigDecimal.valueOf(savageBlow.prob).divide(new BigDecimal("100"), Equation.context))));
            }

            if (critical.exists()) {
                damageTotalMultiplier = damageTotalMultiplier.multiply(BigDecimal.ONE.add(BigDecimal.ONE.multiply(BigDecimal.valueOf(critical.prob).divide(new BigDecimal("100"), Equation.context))));
            }
        }

        int result = (int) ((attack.getAtk() * data.multi(BasisSet.current()) * magnification / 100.0));

        return BigDecimal.valueOf(result).multiply(damageTotalMultiplier);
    }

    private static boolean allCoordinatesSame(List<BigDecimal[]> normal, List<BigDecimal[]> withTreasure) {
        if (normal.size() != withTreasure.size())
            return false;

        for (int i = 0; i < normal.size(); i++) {
            BigDecimal[] normalCoordinate = normal.get(i);
            BigDecimal[] withTreasureCoordinate = withTreasure.get(i);

            if (normalCoordinate.length != withTreasureCoordinate.length)
                return false;

            for (int j = 0; j < normalCoordinate.length; j++) {
                if (normalCoordinate[j].compareTo(withTreasureCoordinate[j]) != 0)
                    return false;
            }
        }

        return true;
    }

    public static void generateStatImage(MessageChannel ch, List<CellData> data, List<AbilityData> procData, List<FlagCellData> abilData, List<FlagCellData> traitData, CustomMaskUnit[] units, String[] name, File container, File itemContainer, int lv, boolean isFrame, int[] egg, int[][] trueForm, ImageDrawing.Mode mode, int uid, CommonStatic.Lang.Locale lang) throws Exception {
        List<List<CellDrawer>> cellGroup = new ArrayList<>();

        for(int i = 0; i < units.length; i++) {
            cellGroup.add(addCell(data, procData, abilData, traitData, units[i], lang, lv, isFrame));
        }

        String type = DataToString.getRarity(units[0].rarity, lang);

        File result = ImageDrawing.drawStatImage(units, cellGroup, lv, name, type, container, itemContainer, mode, uid, egg, trueForm);

        if(result == null) {
            ch.sendMessage(LangID.getStringByID("statAnalyzer.failed.unknown", lang)).queue();
        } else {
            ch.sendMessage(LangID.getStringByID("statAnalyzer.success", lang))
                    .addFiles(FileUpload.fromData(result, "stat.png"))
                    .queue(m -> {
                        if(result.exists() && !result.delete()) {
                            StaticStore.logger.uploadLog("Failed to delete file : "+result.getAbsolutePath());
                        }
                    }, e -> {
                        StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateStatImage - Failed to create stat image");

                        if(result.exists() && !result.delete()) {
                            StaticStore.logger.uploadLog("Failed to delete file : "+result.getAbsolutePath());
                        }
                    });
        }
    }

    public static void generateEnemyStatImage(MessageChannel ch, List<CellData> data, List<AbilityData> procData, List<FlagCellData> abilData, List<FlagCellData> traitData, CustomMaskEnemy enemy, String name, File container, int m, boolean isFrame, int eid, CommonStatic.Lang.Locale lang) throws Exception {
        List<CellDrawer> cellGroup = getEnemyCell(data, procData, abilData, traitData, enemy, lang, m, isFrame);

        File result = ImageDrawing.drawEnemyStatImage(cellGroup, LangID.getStringByID("statAnalyzer.magnification", lang).replace("_", String.valueOf(m)), name, container, eid);

        if(result == null) {
            ch.sendMessage(LangID.getStringByID("statAnalyzer.failed.unknown", lang)).queue();
        } else {
            ch.sendMessage(LangID.getStringByID("statAnalyzer.success", lang))
                    .addFiles(FileUpload.fromData(result, "stat.png"))
                    .queue(msg -> {
                        if(result.exists() && !result.delete()) {
                            StaticStore.logger.uploadLog("Failed to delete file : "+result.getAbsolutePath());
                        }
                    }, e -> {
                        StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateEnemyStatImage - Failed to create stat image");

                        if(result.exists() && !result.delete()) {
                            StaticStore.logger.uploadLog("Failed to delete file : "+result.getAbsolutePath());
                        }
                    });
        }
    }

    public static void generateStageStatImage(MessageChannel ch, CustomStageMap map, int lv, boolean isFrame, CommonStatic.Lang.Locale lang, String[] name, String code) {
        List<List<CellDrawer>> cellGroups = new ArrayList<>();

        if(map.customIndex.isEmpty()) {
            for(int i = 0; i < map.list.size(); i++) {
                cellGroups.add(getStageCell(map, i, lang, lv, isFrame));
            }
        } else {
            for(int i = 0; i < map.customIndex.size(); i++) {
                cellGroups.add(getStageCell(map, map.customIndex.get(i), lang, lv, isFrame));
            }
        }

        List<File> results = new ArrayList<>();

        long start = System.currentTimeMillis();

        if(map.customIndex.isEmpty()) {
            Command.replyToMessageSafely(ch, String.format(LangID.getStringByID("statAnalyzer.analyzingStages", lang), 0, map.list.size()), null, a -> a, msg -> {
                for(int i = 0; i < map.list.size(); i++) {
                    File result;

                    try {
                        result = ImageDrawing.drawStageStatImage(map, cellGroups.get(i), isFrame, lv, name[i], code + " - " + Data.trio(map.mapID % 1000) + " - " + Data.trio(i), i, lang);
                    } catch (Exception e) {
                        StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateStageStatImage - Failed to generate stage stat image");

                        continue;
                    }

                    if(result != null) {
                        results.add(result);
                    }

                    if(System.currentTimeMillis() - start > 1000) {
                        msg.editMessage(String.format(LangID.getStringByID("statAnalyzer.analyzingStages", lang), i + 1, map.list.size())).queue();
                    }
                }

                msg.editMessage(String.format(LangID.getStringByID("statAnalyzer.analyzingStages", lang), map.list.size(), map.list.size())).queue();

                sendMultipleFiles(ch, results);
            });
        } else {
            Command.replyToMessageSafely(ch, String.format(LangID.getStringByID("statAnalyzer.analyzingStages", lang), 0, map.customIndex.size()), null, a -> a, msg -> {
                for(int i = 0; i < map.customIndex.size(); i++) {
                    File result;

                    try {
                        result = ImageDrawing.drawStageStatImage(map, cellGroups.get(i), isFrame, lv, name[i], code + " - " + Data.trio(map.mapID % 1000) + " - " + Data.trio(map.customIndex.get(i)), map.customIndex.get(i), lang);
                    } catch (Exception e) {
                        StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateStageStatImage - Failed to generate stage stat image");

                        continue;
                    }

                    if(result != null) {
                        results.add(result);
                    }

                    if(System.currentTimeMillis() - start > 1000) {
                        msg.editMessage(String.format(LangID.getStringByID("statAnalyzer.analyzingStages", lang), i + 1, map.customIndex.size())).queue();
                    }
                }

                msg.editMessage(String.format(LangID.getStringByID("statAnalyzer.analyzingStages", lang), map.customIndex.size(), map.customIndex.size())).queue();

                sendMultipleFiles(ch, results);
            });
        }
    }

    private static void sendMultipleFiles(MessageChannel ch, List<File> results) {
        int i = 0;

        Queue<File> done = new ArrayDeque<>();

        while(i < results.size()) {
            MessageCreateAction action = ch.sendMessage("Analyzed stage image");

            long fileSize = 0;

            while (i < results.size() && fileSize + results.get(i).length() < 8 * 1024 * 1024) {
                action = action.addFiles(FileUpload.fromData(results.get(i)));
                done.add(results.get(i));

                fileSize += results.get(i).length();
                i++;
            }

            action.queue(m -> {
                while(!done.isEmpty()) {
                    File target = done.poll();

                    if(target != null && target.exists() && !target.delete()) {
                        StaticStore.logger.uploadLog("W/EntityHandler::generateStageStatImage - Failed to delete file : " + target.getAbsolutePath());
                    }
                }
            }, e -> {
                StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateStageStatImage - Error happened while trying to upload analyzed stage image");

                while(!done.isEmpty()) {
                    File target = done.poll();

                    if(target != null && target.exists() && !target.delete()) {
                        StaticStore.logger.uploadLog("W/EntityHandler::generateStageStatImage - Failed to delete file : " + target.getAbsolutePath());
                    }
                }
            });
        }
    }

    private static List<CellDrawer> addCell(List<CellData> data, List<AbilityData> procData, List<FlagCellData> abilData, List<FlagCellData> traitData, CustomMaskUnit u, CommonStatic.Lang.Locale lang, int lv, boolean isFrame) {
        List<CellDrawer> cells = new ArrayList<>();

        Level lvs = new Level(0);

        lvs.setLevel(lv);

        cells.add(new NormalCellDrawer(
                new String[] {LangID.getStringByID("data.hp", lang), LangID.getStringByID("data.kb", lang), LangID.getStringByID("data.speed", lang)},
                new String[] {DataToString.getHP(u, u.curve, false, lvs, false, TreasureHolder.global), DataToString.getHitback(u, false, lvs), DataToString.getSpeed(u, false , lvs)}
        ));

        cells.add(new NormalCellDrawer(new String[] {LangID.getStringByID("data.damage", lang)}, new String[] {DataToString.getAtk(u, u.curve, false, lvs, false, TreasureHolder.global)}));

        cells.add(new NormalCellDrawer(
                new String[] {LangID.getStringByID("data.dps", lang), LangID.getStringByID("data.attackTime", lang), LangID.getStringByID("data.useAbility", lang)},
                new String[] {DataToString.getDPS(u, u.curve, false, lvs, false, TreasureHolder.global), DataToString.getAtkTime(u, false, isFrame, lvs), DataToString.getAbilT(u, lang)}
        ));

        cells.add(new NormalCellDrawer(
                new String[] {LangID.getStringByID("data.foreswing", lang), LangID.getStringByID("data.backswing", lang), LangID.getStringByID("data.tba", lang)},
                new String[] {DataToString.getPre(u, isFrame), DataToString.getPost(u, isFrame), DataToString.getTBA(u, false, lvs, isFrame)}
        ));

        StringBuilder trait = new StringBuilder(DataToString.getTrait(u, false, lvs, false, lang));

        for(int i = 0; i < traitData.size(); i++) {
            String t = traitData.get(i).dataToString(u.data);

            if(!t.isBlank()) {
                trait.append(", ").append(t);
            }
        }

        cells.add(new NormalCellDrawer(
                new String[] {LangID.getStringByID("data.trait", lang)},
                new String[] {trait.toString()}
        ));

        cells.add(new NormalCellDrawer(
                new String[] {LangID.getStringByID("data.attackType", lang), LangID.getStringByID("data.unit.cost", lang), LangID.getStringByID("data.range", lang)},
                new String[] {DataToString.getSiMu(u, lang), DataToString.getCost(u, false, lvs), DataToString.getRange(u)}
        ));

        cells.add(new NormalCellDrawer(
                new String[] {LangID.getStringByID("data.unit.cooldown", lang)},
                new String[] {DataToString.getCD(u, isFrame, false, lvs, TreasureHolder.global)}
        ));

        List<List<CellData>> cellGroup = new ArrayList<>();

        for(int i = 0; i < data.size(); i++) {
            CellData d = data.get(i);

            List<CellData> group = new ArrayList<>();

            if(d.isOneLine()) {
                group.add(d);

                cellGroup.add(group);
            } else {
                int j = i;

                while(group.size() < 3 && !data.get(j).isOneLine()) {
                    group.add(data.get(j));

                    j++;

                    if(j >= data.size()) {
                        break;
                    }
                }

                j--;

                cellGroup.add(group);

                if(j > i) {
                    i = j;
                }
            }
        }

        for(int i = 0; i < cellGroup.size(); i++) {
            List<CellData> group = cellGroup.get(i);

            String[] names = new String[group.size()];
            String[] contents = new String[group.size()];

            for(int j = 0; j < group.size(); j++) {
                names[j] = group.get(j).getName();
                String c = group.get(j).dataToString(u.data, isFrame);

                if(c.isBlank()) {
                    contents[j] = LangID.getStringByID("data.none", lang);
                } else {
                    contents[j] = c;
                }
            }

            cells.add(new NormalCellDrawer(names, contents));
        }

        List<String> abil = Interpret.getAbi(u, false, lang, null, null);

        for(int i = 0; i < abilData.size(); i++) {
            String a = abilData.get(i).dataToString(u.data);

            if(!a.isBlank()) {
                abil.add(a);
            }
        }

        abil.addAll(Interpret.getProc(u, !isFrame, false, lang, 1.0, 1.0, false, null, null));

        for(int i = 0; i < procData.size(); i++) {
            String p = procData.get(i).beautify(u.data, isFrame);

            if(!p.isBlank()) {
                abil.add(p);
            }
        }

        if(abil.isEmpty()) {
            cells.add(new AbilityCellDrawer(LangID.getStringByID("data.ability", lang), new String[] {LangID.getStringByID("data.none", lang)}));
        } else {
            List<String> finalAbil = new ArrayList<>();

            for(int i = 0; i < abil.size(); i++) {
                finalAbil.add(" · " + abil.get(i));
            }

            cells.add(new AbilityCellDrawer(LangID.getStringByID("data.ability", lang), finalAbil.toArray(new String[0])));
        }

        return cells;
    }

    private static List<CellDrawer> getEnemyCell(List<CellData> data, List<AbilityData> procData, List<FlagCellData> abilData, List<FlagCellData> traitData, CustomMaskEnemy e, CommonStatic.Lang.Locale lang, int m, boolean isFrame) {
        List<CellDrawer> cells = new ArrayList<>();

        cells.add(new NormalCellDrawer(
                new String[] {LangID.getStringByID("data.hp", lang), LangID.getStringByID("data.kb", lang), LangID.getStringByID("data.speed", lang)},
                new String[] {DataToString.getHP(e, m), DataToString.getHitback(e), DataToString.getSpeed(e)}
        ));

        cells.add(new NormalCellDrawer(new String[] {LangID.getStringByID("data.damage", lang)}, new String[] {DataToString.getAtk(e, m)}));

        cells.add(new NormalCellDrawer(
                new String[] {LangID.getStringByID("data.dps", lang), LangID.getStringByID("data.attackTime", lang), LangID.getStringByID("data.useAbility", lang)},
                new String[] {DataToString.getDPS(e, m), DataToString.getAtkTime(e, isFrame), DataToString.getAbilT(e, lang)}
        ));

        cells.add(new NormalCellDrawer(
                new String[] {LangID.getStringByID("data.foreswing", lang), LangID.getStringByID("data.backswing", lang), LangID.getStringByID("data.tba", lang)},
                new String[] {DataToString.getPre(e, isFrame), DataToString.getPost(e, isFrame), DataToString.getTBA(e, isFrame)}
        ));

        StringBuilder trait = new StringBuilder(DataToString.getTrait(e, false, lang));

        for(int i = 0; i < traitData.size(); i++) {
            String t = traitData.get(i).dataToString(e.data);

            if(!t.isBlank()) {
                trait.append(", ").append(t);
            }
        }

        cells.add(new NormalCellDrawer(
                new String[] {LangID.getStringByID("data.trait", lang)},
                new String[] {trait.toString()}
        ));

        cells.add(new NormalCellDrawer(
                new String[] {LangID.getStringByID("data.attackType", lang), LangID.getStringByID("data.enemy.drop", lang), LangID.getStringByID("data.range", lang)},
                new String[] {DataToString.getSiMu(e, lang), DataToString.getDrop(e, TreasureHolder.global), DataToString.getRange(e)}
        ));

        cells.add(new NormalCellDrawer(
                new String[] {LangID.getStringByID("data.enemy.barrier", lang)},
                new String[] {DataToString.getBarrier(e, lang)}
        ));

        List<List<CellData>> cellGroup = new ArrayList<>();

        for(int i = 0; i < data.size(); i++) {
            CellData d = data.get(i);

            List<CellData> group = new ArrayList<>();

            if(d.isOneLine()) {
                group.add(d);

                cellGroup.add(group);
            } else {
                int j = i;

                while(group.size() < 3 && !data.get(j).isOneLine()) {
                    group.add(data.get(j));

                    j++;

                    if(j >= data.size()) {
                        break;
                    }
                }

                j--;

                cellGroup.add(group);

                if(j > i) {
                    i = j;
                }
            }
        }

        for(int i = 0; i < cellGroup.size(); i++) {
            List<CellData> group = cellGroup.get(i);

            String[] names = new String[group.size()];
            String[] contents = new String[group.size()];

            for(int j = 0; j < group.size(); j++) {
                names[j] = group.get(j).getName();
                String c = group.get(j).dataToString(e.data, isFrame);

                if(c.isBlank()) {
                    contents[j] = LangID.getStringByID("data.none", lang);
                } else {
                    contents[j] = c;
                }
            }

            cells.add(new NormalCellDrawer(names, contents));
        }

        List<String> abil = Interpret.getAbi(e, false, lang, null, null);

        for(int i = 0; i < abilData.size(); i++) {
            String a = abilData.get(i).dataToString(e.data);

            if(!a.isBlank()) {
                abil.add(a);
            }
        }

        abil.addAll(Interpret.getProc(e, !isFrame, false, lang, 1.0, 1.0, false, null, null));

        for(int i = 0; i < procData.size(); i++) {
            String p = procData.get(i).beautify(e.data, isFrame);

            if(!p.isBlank()) {
                abil.add(p);
            }
        }

        if(abil.isEmpty()) {
            cells.add(new AbilityCellDrawer(LangID.getStringByID("data.ability", lang), new String[] {LangID.getStringByID("data.none", lang)}));
        } else {
            List<String> finalAbil = new ArrayList<>();

            for(int i = 0; i < abil.size(); i++) {
                finalAbil.add(" · " + abil.get(i));
            }

            cells.add(new AbilityCellDrawer(LangID.getStringByID("data.ability", lang), finalAbil.toArray(new String[0])));
        }

        return cells;
    }

    private static List<CellDrawer> getStageCell(CustomStageMap map, int index, CommonStatic.Lang.Locale lang, int lv, boolean isFrame) {
        List<CellDrawer> cells = new ArrayList<>();

        Stage st = map.list.get(index);

        cells.add(new NormalCellDrawer(
                new String[] {LangID.getStringByID("data.stage.energy", lang), LangID.getStringByID("data.stage.baseHealth", lang), LangID.getStringByID("data.stage.xp", lang), LangID.getStringByID("data.unit.level", lang)},
                new String[] {DataToString.getEnergy(st, lang), DataToString.getBaseHealth(st), DataToString.getXP(st, TreasureHolder.global), DataToString.getLevelMagnification(map)},
                new FakeImage[] {null, null, null, drawLevelImage(map.stars.length, lv)},
                new boolean[] {false, false ,false, true}
        ));

        cells.add(new NormalCellDrawer(
                new String[] {LangID.getStringByID("data.stage.music", lang), DataToString.getMusicChange(st), LangID.getStringByID("data.stage.background", lang), LangID.getStringByID("data.stage.castle", lang)},
                new String[] {DataToString.getMusic(st, lang), DataToString.getMusic1(st, lang), DataToString.getBackground(st, lang), DataToString.getCastle(st, lang)}
        ));

        cells.add(new NormalCellDrawer(
                new String[] {LangID.getStringByID("data.stage.continuable", lang), LangID.getStringByID("data.stage.enemyLimit", lang), LangID.getStringByID("data.stage.minimumRespawn", lang), LangID.getStringByID("data.stage.length", lang)},
                new String[] {DataToString.getContinuable(st, lang), DataToString.getMaxEnemy(st), DataToString.getMinSpawn(st, isFrame), DataToString.getLength(st)}
        ));

        List<String> limits = DataToString.getLimit(st.lim, map, isFrame, lang);

        if(limits.isEmpty())
            limits.add(LangID.getStringByID("data.none", lang));

        cells.add(new AbilityCellDrawer(
                LangID.getStringByID("data.stage.limit.title", lang),
                limits.toArray(new String[0])
        ));

        List<String> misc = DataToString.getMiscellaneous(st, lang);

        misc.replaceAll(s -> " - " + s);

        if(misc.isEmpty())
            misc.add(LangID.getStringByID("data.none", lang));

        cells.add(new AbilityCellDrawer(
                LangID.getStringByID("data.stage.misc.title", lang),
                misc.toArray(new String[0])
        ));

        return cells;
    }

    private static File generateComboImage(Combo c) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : " + temp.getAbsolutePath());
                return null;
            }
        }

        File image = StaticStore.generateTempFile(temp, "combo", ".png", false);

        if(image == null) {
            return null;
        }

        CountDownLatch waiter = new CountDownLatch(1);

        StaticStore.renderManager.createRenderer(600, 95, temp, connector -> {
            connector.queue(g -> {
                g.setColor(47, 49, 54, 255);
                g.fillRect(0, 0, 650, 105);

                g.setStroke(2f, GLGraphics.LineEndMode.VERTICAL);
                g.setColor(230, 230, 230, 255);
                g.drawRect(0, 0, 600, 95);

                for(int i = 1; i < 5; i++) {
                    g.drawLine(120 * i, 0, 120 * i, 95);
                }

                for(int i = 0; i < 5; i++) {
                    if(i >= c.forms.length || c.forms[i] == null)
                        continue;

                    Unit u = c.forms[i].unit;

                    if(u == null)
                        continue;

                    Form f = c.forms[i];

                    f.anim.load();

                    FakeImage icon = f.anim.getUni().getImg();

                    g.drawImage(icon, 120 * i + 5, 5);

                    f.anim.unload();
                }

                return null;
            });

            return null;
        }, progress -> image, () -> {
            waiter.countDown();

            return null;
        });

        waiter.await();

        return image;
    }

    private static String getIconName(int mode, CommonStatic.Lang.Locale lang) {
        if(mode == 0)
            return LangID.getStringByID("formSprite.spriteSheet", lang);
        else if(mode == 1)
            return LangID.getStringByID("formSprite.icon.unitIcon", lang);
        else if(mode == 2)
            return LangID.getStringByID("formSprite.icon.unitDisplay", lang);
        else
            return LangID.getStringByID("formSprite.icon.enemyDisplay", lang);
    }

    private static void cacheImage(Enemy e, int mode, Message msg) {
        if(e.id == null)
            return;

        String id = generateID(e, mode);

        List<Message.Attachment> att = msg.getAttachments();

        if(att.isEmpty())
            return;

        for(Message.Attachment a : att) {
            if (a.getFileName().equals("result.gif")) {
                String link = a.getUrl();

                StaticStore.imgur.put(id, link, false);

                return;
            } else if(a.getFileName().equals("result.mp4")) {
                String link = a.getUrl();

                StaticStore.imgur.put(id, link, true);

                return;
            }
        }
    }

    private static String generateID(Enemy e, int mode) {
        if(e.id == null)
            return "";

        return "E - "+e.id.pack+" - "+Data.trio(e.id.id)+" - "+Data.trio(mode);
    }

    private static String generateID(Form f, int mode) {
        if(f.unit == null || f.unit.id == null)
            return "";

        return "F - "+f.unit.id.pack+" - "+ Data.trio(f.unit.id.id)+" - "+Data.trio(f.fid) + " - " + Data.trio(mode);
    }

    private static AnimU.UType getAnimType(int mode, int max) {
        switch (mode) {
            case 1 -> {
                return AnimU.UType.IDLE;
            }
            case 2 -> {
                return AnimU.UType.ATK;
            }
            case 3 -> {
                return AnimU.UType.HB;
            }
            case 4 -> {
                if (max == 5)
                    return AnimU.UType.ENTER;
                else
                    return AnimU.UType.BURROW_DOWN;
            }
            case 5 -> {
                return AnimU.UType.BURROW_MOVE;
            }
            case 6 -> {
                return AnimU.UType.BURROW_UP;
            }
            default -> {
                return AnimU.UType.WALK;
            }
        }
    }

    private static String getLocaleName(CommonStatic.Lang.Locale lang) {
        if(lang == CommonStatic.Lang.Locale.EN)
            return "en";
        else if(lang == CommonStatic.Lang.Locale.ZH)
            return "tw";
        else if(lang == CommonStatic.Lang.Locale.KR)
            return "kr";
        else
            return "";
    }

    private static boolean hasTwoMusic(Stage st) {
        return st.mush != 0 && st.mush != 100 && st.mus1 != null && st.mus0 != null && st.mus1.id != st.mus0.id;
    }

    private static boolean canFirstForm(Form f) {
        return f.unit != null && f.fid - 2 >= 0;
    }

    private static boolean canPreviousForm(Form f) {
        return f.unit != null && f.fid - 1 >= 0;
    }

    private static boolean canNextForm(Form f) {
        return f.unit != null && f.fid + 1 < f.unit.forms.length;
    }

    private static boolean canFinalForm(Form f) {
        return f.unit != null && f.fid + 2 < f.unit.forms.length;
    }

    private static int getBoosterFileLimit(int level) {
        return switch (level) {
            case 2 -> 50;
            case 3 -> 100;
            default -> 8;
        };
    }

    private static List<String> mergeImmune(List<String> abilities, CommonStatic.Lang.Locale lang) {
        List<String> result = new ArrayList<>();
        List<String> immunes = new ArrayList<>();

        for(int i = 0; i < abilities.size(); i++) {
            String actualName = abilities.get(i).replaceAll("<:.+:\\d+> ", "");

            switch (lang) {
                case KR, JP -> {
                    if (actualName.endsWith(LangID.getStringByID("data.abilities.immuneTo", lang))) {
                        immunes.add(actualName);
                    } else {
                        result.add(abilities.get(i));
                    }
                }
                case EN -> {
                    if (actualName.startsWith(LangID.getStringByID("data.abilities.immuneTo", lang))) {
                        immunes.add(actualName);
                    } else {
                        result.add(abilities.get(i));
                    }
                }
                default -> {
                    return abilities;
                }
            }
        }

        if(immunes.size() < 2)
            return abilities;

        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < immunes.size(); i++) {
            String segment = immunes.get(i).replace(LangID.getStringByID("data.abilities.immuneTo", lang), "");

            sb.append(segment);

            if(i < immunes.size() - 1)
                sb.append(LangID.getStringByID("data.comma", lang));
        }

        Emoji emoji = EmojiStore.ABILITY.get("IMMUNITY");

        String e = emoji == null ? "" : emoji.getFormatted() + " ";

        switch (lang) {
            case KR, JP -> result.add(e + sb + LangID.getStringByID("data.abilities.immuneTo", lang));
            default -> result.add(e + LangID.getStringByID("data.abilities.immuneTo", lang) + sb);
        }

        return result;
    }

    private static FakeImage drawLevelImage(int max, int lv) {
        File temp = new File("./temp");

        if (!temp.exists() && !temp.mkdirs()) {
            StaticStore.logger.uploadLog("W/EntityHandler::drawLevelImage - Failed to create folder : " + temp.getAbsolutePath());
        }

        File image = StaticStore.generateTempFile(temp, "level", ".png", false);

        if (image != null && !image.exists())
            return null;

        try {
            FakeImage crownOn = ImageBuilder.builder.build(new File("./data/bot/icons/crownOn.png"));
            FakeImage crownOff = ImageBuilder.builder.build(new File("./data/bot/icons/crownOff.png"));

            CountDownLatch waiter = new CountDownLatch(1);

            StaticStore.renderManager.createRenderer(crownOn.getWidth() * max + 10 * (max - 1), crownOn.getHeight(), temp, connector -> {
                connector.queue(g -> {
                    int x = 0;

                    for(int i = 0; i < max; i++) {
                        if(i > lv) {
                            g.drawImage(crownOff, x, 0);
                        } else {
                            g.drawImage(crownOn, x, 0);
                        }

                        x += crownOn.getWidth() + 10;
                    }

                    return null;
                });

                return null;
            }, progress -> image, () -> {
                waiter.countDown();

                return null;
            });

            waiter.await();

            return ImageBuilder.builder.build(image);
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::drawLevelImage - Failed to generate level image");
        }

        return null;
    }
}
