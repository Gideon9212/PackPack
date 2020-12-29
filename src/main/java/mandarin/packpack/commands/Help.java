package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;

public class Help implements Command {
    @Override
    public void doSomething(MessageCreateEvent event) {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        ch.createEmbed(emb -> {
            emb.setTitle("Command List");
            emb.setDescription("This embed text contains all commands with detailed explanations");
            emb.setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)]);
            emb.addField(StaticStore.serverPrefix+"bcustat","`Usage : "+StaticStore.serverPrefix+"bcustat`\nDisplay user statistics of BCU server with percentage data. This command will show number of human, members, pre-members, bcu-pc users, bcu-android users.", false);
            emb.addField(StaticStore.serverPrefix+"checkbcu","`Usage : "+StaticStore.serverPrefix+"checkbcu`\nDisplay users who have both member and pre-member roles or have none of them. Usually will be used for moderating server.",false);
            emb.addField(StaticStore.serverPrefix+"analyze","`Usage : "+StaticStore.serverPrefix+"analyze [Files]`\nMust be used with attached pack.bcuzip file. Under construction",false);
            emb.addField(StaticStore.serverPrefix+"prefix","`Usage : "+StaticStore.serverPrefix+"prefix [Prefix]`\nSet your own prefix to make bot recognize your command.", false);
            emb.addField(StaticStore.serverPrefix+"serverpre","`Usage : "+StaticStore.serverPrefix+"serverpre [Prefix]`\nSet server prefix to make bot recognize user's command. Only **moderators** of server can use this command.", false);
        }).subscribe();
    }
}
