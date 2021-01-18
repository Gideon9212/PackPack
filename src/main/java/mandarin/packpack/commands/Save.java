package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.IDHolder;

public class Save extends ConstraintCommand {
    public Save(ROLE role, int lang, IDHolder holder) {
        super(role, lang, holder);
    }

    @Override
    public void doSomething(MessageCreateEvent event) {
        MessageChannel ch = getChannel(event);

        Message msg = ch.createMessage(LangID.getStringByID("save_save", lang)).block();

        if(msg != null) {
            StaticStore.saveServerInfo();

            msg.edit(e -> e.setContent(LangID.getStringByID("save_done", lang))).subscribe();
        } else {
            onFail(event, DEFAULT_ERROR);
        }
    }
}
