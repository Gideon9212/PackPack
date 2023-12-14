package mandarin.packpack.commands.math;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.calculation.Equation;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class Calculator extends ConstraintCommand {

    public Calculator(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        String[] equation = loader.getContent().split(" ", 2);

        if(equation.length < 2) {
            replyToMessageSafely(ch, LangID.getStringByID("calc_eq", lang), loader.getMessage(), a -> a);

            return;
        }

        BigDecimal result = Equation.calculate(equation[1].replace(" ", ""), null, false, lang);

        if(Equation.error.isEmpty()) {
            DecimalFormat df = new DecimalFormat("#.########");

            String value = df.format(result);

            if(value.length() > 1500) {
                value = Equation.formatNumber(result);
            }

            replyToMessageSafely(ch, String.format(LangID.getStringByID("calc_result", lang), value), loader.getMessage(), a -> a);
        } else {
            replyToMessageSafely(ch, Equation.getErrorMessage(), loader.getMessage(), a -> a);
        }
    }
}
