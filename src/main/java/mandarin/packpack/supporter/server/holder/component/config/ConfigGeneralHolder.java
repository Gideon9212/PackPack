package mandarin.packpack.supporter.server.holder.component.config;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder;
import mandarin.packpack.supporter.server.holder.modal.ServerPrefixModalHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConfigGeneralHolder extends ServerConfigHolder {

    public ConfigGeneralHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull IDHolder holder, @NotNull IDHolder backup, CommonStatic.Lang.Locale lang) {
        super(author, channelID, message, holder, backup, lang);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "language" -> {
                if (!(event instanceof StringSelectInteractionEvent se))
                    return;

                holder.config.lang = CommonStatic.Lang.Locale.valueOf(se.getValues().getFirst());

                String languageName = switch (holder.config.lang) {
                    case EN -> LangID.getStringByID("locale.language.en", lang);
                    case ZH -> LangID.getStringByID("locale.language.zh", lang);
                    case KR -> LangID.getStringByID("locale.language.kr", lang);
                    case JP -> LangID.getStringByID("locale.language.jp", lang);
                    case FR -> LangID.getStringByID("locale.language.fr", lang);
                    case IT -> LangID.getStringByID("locale.language.it", lang);
                    case ES -> LangID.getStringByID("locale.language.es", lang);
                    case DE -> LangID.getStringByID("locale.language.de", lang);
                    case TH -> LangID.getStringByID("locale.language.th", lang);
                    case RU -> LangID.getStringByID("locale.language.ru", lang);
                };

                event.deferReply()
                        .setContent(LangID.getStringByID("serverConfig.general.languageSet", lang).formatted(languageName))
                        .setAllowedMentions(new ArrayList<>())
                        .setEphemeral(true)
                        .queue();

                applyResult();
            }
            case "prefix" -> {
                TextInput input = TextInput.create("prefix", LangID.getStringByID("serverConfig.general.prefix", lang), TextInputStyle.SHORT)
                        .setPlaceholder(LangID.getStringByID("serverConfig.general.typePrefix", lang))
                        .setRequired(true)
                        .build();

                Modal modal = Modal.create("prefix", LangID.getStringByID("serverConfig.general.serverPrefix", lang))
                        .addActionRow(input)
                        .build();

                event.replyModal(modal).queue();

                connectTo(new ServerPrefixModalHolder(getAuthorMessage(), channelID, message, holder.config, lang));
            }
            case "role" -> connectTo(event, new ConfigRoleRegistrationHolder(getAuthorMessage(), channelID, message, holder, backup, lang));
            case "confirm" -> {
                event.deferEdit()
                        .setContent(LangID.getStringByID("serverConfig.applied", lang))
                        .setComponents()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();

                expired = true;
            }
            case "cancel" -> {
                registerPopUp(event, LangID.getStringByID("serverConfig.cancelConfirm", lang));

                connectTo(new ConfirmPopUpHolder(getAuthorMessage(), channelID, message, e -> {
                    e.deferEdit()
                            .setContent(LangID.getStringByID("serverConfig.canceled", lang))
                            .setComponents()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();

                    holder.inject(backup);

                    expired = true;
                }, lang));
            }
            case "back" -> goBack(event);
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onConnected(@NotNull GenericComponentInteractionCreateEvent event) {
        applyResult(event);
    }

    @Override
    public void onBack(@NotNull GenericComponentInteractionCreateEvent event, @NotNull Holder child) {
        applyResult(event);
    }

    @Override
    public void onBack(@NotNull Holder child) {
        applyResult();
    }

    private void applyResult(GenericComponentInteractionCreateEvent event) {
        event.deferEdit()
                .setContent(getContents())
                .setComponents(getComponents())
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private void applyResult() {
        message.editMessage(getContents())
                .setComponents(getComponents())
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private String getContents() {
        CommonStatic.Lang.Locale locale = Objects.requireNonNull(holder.config.lang);

        Emoji e = switch (locale) {
            case EN -> Emoji.fromUnicode("🇺🇸");
            case ZH -> Emoji.fromUnicode("🇹🇼");
            case KR -> Emoji.fromUnicode("🇰🇷");
            case JP -> Emoji.fromUnicode("🇯🇵");
            case FR -> Emoji.fromUnicode("🇫🇷");
            case IT -> Emoji.fromUnicode("🇮🇹");
            case ES -> Emoji.fromUnicode("🇪🇸");
            case DE -> Emoji.fromUnicode("🇩🇪");
            case TH -> Emoji.fromUnicode("🇹🇭");
            case RU -> Emoji.fromUnicode("🇷🇺");
        };

        String languageName = switch (locale) {
            case EN -> LangID.getStringByID("locale.language.en", lang);
            case ZH -> LangID.getStringByID("locale.language.zh", lang);
            case KR -> LangID.getStringByID("locale.language.kr", lang);
            case JP -> LangID.getStringByID("locale.language.jp", lang);
            case FR -> LangID.getStringByID("locale.language.fr", lang);
            case IT -> LangID.getStringByID("locale.language.it", lang);
            case ES -> LangID.getStringByID("locale.language.es", lang);
            case DE -> LangID.getStringByID("locale.language.de", lang);
            case TH -> LangID.getStringByID("locale.language.th", lang);
            case RU -> LangID.getStringByID("locale.language.ru", lang);
        };

        return LangID.getStringByID("serverConfig.general.documentation.title", lang) + "\n" +
                LangID.getStringByID("serverConfig.general.documentation.language.title", lang).formatted(EmojiStore.LANGUAGE.getFormatted(), e.getFormatted(), languageName) + "\n" +
                LangID.getStringByID("serverConfig.general.documentation.language.description", lang).formatted(languageName) + "\n" +
                LangID.getStringByID("serverConfig.general.documentation.prefix.title", lang).formatted(Emoji.fromUnicode("🔗").getFormatted(), holder.config.prefix) + "\n" +
                LangID.getStringByID("serverConfig.general.documentation.prefix.description", lang).formatted(StaticStore.globalPrefix, StaticStore.globalPrefix);
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

        List<SelectOption> languageOptions = new ArrayList<>();

        for (CommonStatic.Lang.Locale locale : CommonStatic.Lang.Locale.values()) {
            String l = LangID.getStringByID("locale.language." + locale.code, lang);
            Emoji e = Emoji.fromUnicode(StaticStore.langUnicode[locale.ordinal()]);

            languageOptions.add(
                    SelectOption.of(
                                    LangID.getStringByID("config.locale.title", lang).replace("_", l),
                                    locale.name()
                            )
                            .withDefault(holder.config.lang == locale)
                            .withEmoji(e)
            );
        }

        result.add(ActionRow.of(StringSelectMenu.create("language").addOptions(languageOptions).setPlaceholder(LangID.getStringByID("serverConfig.general.selectLanguage", lang)).build()));
        result.add(ActionRow.of(Button.secondary("prefix", LangID.getStringByID("serverConfig.general.prefixSet", lang)).withEmoji(Emoji.fromUnicode("🔗"))));
        result.add(ActionRow.of(Button.secondary("role", LangID.getStringByID("serverConfig.general.role.button", lang)).withEmoji(EmojiStore.ROLE)));
        result.add(ActionRow.of(
                Button.secondary("back", LangID.getStringByID("ui.button.back", lang)).withEmoji(EmojiStore.BACK),
                Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)).withEmoji(EmojiStore.CHECK),
                Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)).withEmoji(EmojiStore.CROSS)
        ));

        return result;
    }
}
