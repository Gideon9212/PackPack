package mandarin.packpack.commands;

import common.system.fake.FakeImage;
import common.system.fake.ImageBuilder;
import common.system.files.FDFile;
import common.util.anim.ImgCut;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.ImageGenerator;
import mandarin.packpack.supporter.FontStageImageGenerator;
import mandarin.packpack.supporter.StageImageGenerator;
import mandarin.packpack.supporter.StaticStore;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Locale;

public class StmImage extends ConstraintCommand {
    private static final int PARAM_REAL = 2;
    private static final int PARAM_JP = 4;

    public StmImage(ROLE role) {
        super(role);
    }

    private int startIndex = 1;

    @Override
    public void doSomething(MessageCreateEvent event) {
        try {
            MessageChannel ch = getChannel(event);

            String [] list = getMessage(event).split(" ");

            if(list.length >= 2) {
                File temp = new File("./temp");

                if(!temp.exists()) {
                    boolean res = temp.mkdirs();

                    if(!res) {
                        System.out.println("Can't create folder : "+temp.getAbsolutePath());
                        return;
                    }
                }

                int param = checkParameters(getMessage(event));

                ImageGenerator generator;
                File f;
                String message = getMessage(event).split(" ", startIndex+1)[startIndex];

                if((param & PARAM_JP) > 0) {
                    File fon = new File("./data/Font.otf");
                    Font font = Font.createFont(Font.TRUETYPE_FONT, fon).deriveFont(102f);

                    generator = new FontStageImageGenerator(font, 13.5f);
                } else {
                    ImgCut ic = ImgCut.newIns(new FDFile(new File("./data/stage/stm/Font.imgcut")));
                    FakeImage img = ImageBuilder.builder.build(new File("./data/stage/stm/Font.png"));

                    generator = new StageImageGenerator(ic.cut(img));
                }

                if((param & PARAM_REAL) > 0) {
                    f = generator.generateRealImage(message, false);
                } else {
                    f = generator.generateImage(message, false);
                }

                handleLast(message, f, ch, generator);
            } else {
                ch.createMessage("You need one more argument!\nUsage : `"+ StaticStore.serverPrefix+"stageimg [Text]`").subscribe();
            }
        } catch (Exception e) {
            e.printStackTrace();
            onFail(event, DEFAULT_ERROR);
        }
    }

    private void handleLast(String message, File f, MessageChannel ch, ImageGenerator generator) {
        if(f != null) {
            ch.createMessage(m -> {
                try {
                    m.addFile(f.getName(), new FileInputStream(f));
                    m.setContent("Result : ");
                } catch (FileNotFoundException e) {
                    m.setContent("Can't send file!");
                    e.printStackTrace();
                }
            }).subscribe();
        } else {
            ArrayList<String> invalid = generator.getInvalids(message);

            if(invalid.isEmpty()) {
                ch.createMessage("Something went wrong while generating image").subscribe();
            } else {
                StringBuilder builder = new StringBuilder("Your message has invalid letters : ");

                for(int i = 0; i < invalid.size(); i++) {
                    if(i == invalid.size() -1) {
                        builder.append(invalid.get(i));
                    } else {
                        builder.append(invalid.get(i)).append(", ");
                    }
                }

                ch.createMessage(builder.toString()).subscribe();
            }
        }
    }

    private int checkParameters(String message) {
        String[] msg = message.split(" ");

        int result =  1;

        if(msg.length >= 2) {
            String pureMessage = message.split(" ", 2)[1].toLowerCase(Locale.ROOT).replace(" ", "");

            for(int i = 0; i < 2; i++) {
                if(pureMessage.startsWith("-r")) {
                    result |= PARAM_REAL;
                    startIndex++;

                    pureMessage = pureMessage.substring(2);
                } else if(pureMessage.startsWith("-jp")) {
                    result |= PARAM_JP;
                    startIndex++;

                    pureMessage = pureMessage.substring(3);
                }
            }
        }

        return result;
    }
}
