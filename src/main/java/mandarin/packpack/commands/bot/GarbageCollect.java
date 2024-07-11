package mandarin.packpack.commands.bot;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;

public class GarbageCollect extends ConstraintCommand {
    public GarbageCollect(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        System.gc();

        createMessageWithNoPings(ch, "Vurp!");
    }
}
