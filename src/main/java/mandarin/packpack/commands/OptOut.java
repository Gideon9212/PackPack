package mandarin.packpack.commands;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.ConfirmButtonHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class OptOut extends ConstraintCommand {
    public OptOut(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        User u = getUser(event);

        if(u != null) {
            String id = u.getId();

            Message m = getRepliedMessageSafely(ch, LangID.getStringByID("optout_warn", lang), getMessage(event), a -> registerConfirmButtons(a, lang));

            StaticStore.putHolder(id, new ConfirmButtonHolder(getMessage(event), m, ch.getId(), () -> {
                StaticStore.optoutMembers.add(id);

                StaticStore.spamData.remove(id);
                StaticStore.prefix.remove(id);
                StaticStore.timeZones.remove(id);

                replyToMessageSafely(ch, LangID.getStringByID("optout_success", lang), getMessage(event), a -> a);
            }, lang));
        } else {
            replyToMessageSafely(ch, LangID.getStringByID("optout_nomem", lang), getMessage(event), a -> a);
        }
    }
}
