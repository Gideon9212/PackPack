package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.IDHolder;

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class BCUStat implements Command {

    final int lang;
    final IDHolder holder;

    public BCUStat(int lang, IDHolder holder) {
        this.lang = lang;
        this.holder = holder;
    }

    @Override
    public void doSomething(MessageCreateEvent event) {
        Message msg = event.getMessage();
        MessageChannel ch = msg.getChannel().block();

        if(ch == null)
            return;

        DecimalFormat df = new DecimalFormat("#.##");

        AtomicBoolean error = new AtomicBoolean(false);

        AtomicReference<StringBuilder> result = new AtomicReference<>(new StringBuilder());

        AtomicReference<Long> allUsers = new AtomicReference<>(0L);

        event.getGuild().subscribe(g -> {
            g.getMembers()
                    .filter(m -> !m.isBot())
                    .count()
                    .subscribe(l -> {
                        result.get().append(LangID.getStringByID("bcustat_human", lang).replace("_", Long.toString(l)));
                        allUsers.set(l);
                    });

            g.getMembers()
                    .filter(m -> !m.isBot())
                    .filter(m -> StaticStore.rolesToString(m.getRoleIds()).contains(holder.PRE_MEMBER))
                    .count()
                    .subscribe(l -> result.get().append(LangID.getStringByID("bcustat_prem", lang).replace("_", String.valueOf(l)).replace("-", df.format(l * 100.0 / allUsers.get()))));

            g.getMembers()
                    .filter(m -> !m.isBot())
                    .filter(m -> StaticStore.rolesToString(m.getRoleIds()).contains(holder.MEMBER))
                    .count()
                    .subscribe(l -> result.get().append(LangID.getStringByID("bcustat_mem", lang).replace("_", String.valueOf(l)).replace("-", df.format(l * 100.0 / allUsers.get()))));

            g.getMembers()
                    .filter(m -> !m.isBot())
                    .filter(m -> StaticStore.rolesToString(m.getRoleIds()).contains(holder.BCU_PC_USER))
                    .count()
                    .subscribe(l -> result.get().append(LangID.getStringByID("bcustat_pc", lang).replace("_", String.valueOf(l)).replace("-", df.format(l * 100.0 / allUsers.get()))));

            g.getMembers()
                    .filter(m -> !m.isBot())
                    .filter(m -> StaticStore.rolesToString(m.getRoleIds()).contains(holder.BCU_ANDROID))
                    .count()
                    .subscribe(l -> result.get().append(LangID.getStringByID("bcustat_and", lang).replace("_", String.valueOf(l)).replace("-",df.format(l * 100.0 / allUsers.get()))));
        }, e -> {
            ch.createMessage(StaticStore.ERROR_MSG).subscribe();
            error.set(true);
        }, pause::resume);

        pause.pause(() -> {
            ch.createMessage(StaticStore.ERROR_MSG).subscribe();
            error.set(true);
        });

        if(!error.get())
            ch.createMessage(result.get().toString()).subscribe();
    }
}
