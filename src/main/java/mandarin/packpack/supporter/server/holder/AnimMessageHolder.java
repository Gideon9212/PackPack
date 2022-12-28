package mandarin.packpack.supporter.server.holder;

import common.io.assets.UpdateCheck;
import common.system.files.VFile;
import common.util.anim.ImgCut;
import common.util.anim.MaAnim;
import common.util.anim.MaModel;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.AnimMixer;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class AnimMessageHolder extends MessageHolder<MessageReceivedEvent> {
    private final AnimMixer mixer;
    private final Message msg;
    private final int lang;
    private final String channelID;
    private final File container;

    private final boolean debug;
    private final boolean raw;
    private final boolean transparent;

    private boolean pngDone = false;
    private boolean cutDone = false;
    private boolean modelDone = false;

    private final AtomicReference<String> png = new AtomicReference<>("PNG : -");
    private final AtomicReference<String> imgcut = new AtomicReference<>("IMGCUT : -");
    private final AtomicReference<String> mamodel = new AtomicReference<>("MAMODEL : -");
    private final ArrayList<AtomicReference<String>> maanim = new ArrayList<>();

    public AnimMessageHolder(Message msg, Message target, int lang, String channelID, File container, boolean debug, MessageChannel ch, boolean raw, boolean transparent, int len) throws Exception {
        super(MessageReceivedEvent.class, msg);

        this.msg = target;
        this.lang = lang;
        this.channelID = channelID;
        this.container = container;

        this.debug = debug;
        this.raw = raw;
        this.transparent = transparent;

        mixer = new AnimMixer(len);

        for(int i = 0; i < len; i++) {
            maanim.add(new AtomicReference<>("MAANIM "+i+" : -"));
        }

        AtomicReference<Long> now = new AtomicReference<>(System.currentTimeMillis());

        if(!msg.getAttachments().isEmpty()) {
            for(Message.Attachment a : msg.getAttachments()) {
                if(a.getFileName().endsWith(".png") && mixer.png == null) {
                    UpdateCheck.Downloader down = StaticStore.getDownloader(a, container);

                    if(down != null) {
                        down.run(d -> {
                            String p = "PNG : ";

                            if(d == 1.0) {
                                p += "VALIDATING...";
                            } else {
                                p += DataToString.df.format(d * 100.0) + "%";
                            }

                            png.set(p);

                            if(System.currentTimeMillis() - now.get() >= 1500) {
                                now.set(System.currentTimeMillis());

                                edit();
                            }
                        });

                        File res = new File(container, a.getFileName());

                        if(res.exists()) {
                            if(AnimMixer.validPng(res)) {
                                png.set("PNG : SUCCESS");
                                pngDone = true;

                                mixer.png = ImageIO.read(res);
                            } else {
                                png.set("PNG : INVALID");
                            }
                        } else {
                            png.set("PNG : DOWN_FAIL");
                        }

                        edit();
                    }
                } else if((a.getFileName().endsWith(".imgcut") || a.getFileName().contains("imgcut.txt")) && mixer.imgCut == null) {
                    UpdateCheck.Downloader down = StaticStore.getDownloader(a, container);

                    if(down != null) {
                        down.run(d -> {
                            String p = "IMGCUT : ";

                            if(d == 1.0) {
                                p += "VALIDATING...";
                            } else {
                                p += DataToString.df.format(d * 100.0) + "%";
                            }

                            imgcut.set(p);

                            if(System.currentTimeMillis() - now.get() >= 1500) {
                                now.set(System.currentTimeMillis());

                                edit();
                            }
                        });

                        File res = new File(container, a.getFileName());

                        if(res.exists()) {
                            if(mixer.validImgCut(res)) {
                                imgcut.set("IMGCUT : SUCCESS");
                                cutDone = true;

                                VFile vf = VFile.getFile(res);

                                if(vf != null) {
                                    mixer.imgCut = ImgCut.newIns(vf.getData());
                                }
                            } else {
                                imgcut.set("IMGCUT : INVALID");
                            }
                        } else {
                            imgcut.set("IMGCUT : DOWN_FAIL");
                        }

                        edit();
                    }
                } else if((a.getFileName().endsWith(".mamodel") || a.getFileName().contains("mamodel.txt")) && mixer.model == null) {
                    UpdateCheck.Downloader down = StaticStore.getDownloader(a, container);

                    if(down != null) {
                        down.run(d -> {
                            String p = "MAMODEL : ";

                            if(d == 1.0) {
                                p += "VALIDATING...";
                            } else {
                                p += DataToString.df.format(d * 100.0) + "%";
                            }

                            mamodel.set(p);

                            if(System.currentTimeMillis() - now.get() >= 1500) {
                                now.set(System.currentTimeMillis());

                                edit();
                            }
                        });

                        File res = new File(container, a.getFileName());

                        if(res.exists()) {
                            if(mixer.validMamodel(res)) {
                                mamodel.set("MAMODEL : SUCCESS");
                                modelDone = true;

                                VFile vf = VFile.getFile(res);

                                if(vf != null) {
                                    mixer.model = MaModel.newIns(vf.getData());
                                }
                            } else {
                                mamodel.set("MAMODEL : INVALID");
                            }
                        } else {
                            mamodel.set("MAMODEL : DOWN_FAIL");
                        }

                        edit();
                    }
                } else if((a.getFileName().endsWith(".maanim") || (a.getFileName().startsWith("maanim") && a.getFileName().endsWith(".txt"))) && !animAllDone()) {
                    int ind = getLastIndex();

                    if(ind == -1)
                        continue;

                    UpdateCheck.Downloader down = StaticStore.getDownloader(a, container);

                    if(down != null) {
                        down.run(d -> {
                            String p = "MAANIM "+ind+" : ";

                            if(d == 1.0) {
                                p += "VALIDATING...";
                            } else {
                                p += DataToString.df.format(d * 100.0) + "%";
                            }

                            maanim.get(ind).set(p);

                            if(System.currentTimeMillis() - now.get() >= 1500) {
                                now.set(System.currentTimeMillis());

                                edit();
                            }
                        });

                        File res = new File(container, a.getFileName());

                        if(res.exists()) {
                            if(AnimMixer.validMaanim(res)) {
                                maanim.get(ind).set("MAANIM "+ind+" : SUCCESS");

                                VFile vf = VFile.getFile(res);

                                if(vf != null) {
                                    mixer.anim[ind] = MaAnim.newIns(vf.getData());
                                }
                            } else {
                                maanim.get(ind).set("MAANIM "+ind+" : INVALID");
                            }
                        } else {
                            maanim.get(ind).set("MAANIM "+ind+" : DOWN_FAIL");
                        }

                        edit();
                    }
                }
            }
        }

        if(pngDone && cutDone && modelDone && animAllDone()) {
            new Thread(() -> {
                Guild g;

                if(ch instanceof GuildChannel) {
                    g = msg.getGuild();
                } else {
                    g = null;
                }

                try {
                    for(int i = 0; i < mixer.anim.length; i++) {
                        EntityHandler.generateAnim(ch, mixer, g == null ? 0 : g.getBoostTier().getKey(), lang, debug, -1, raw, transparent, i);
                    }

                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            StaticStore.deleteFile(container, true);
                        }
                    }, 1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            StaticStore.putHolder(msg.getAuthor().getId(), this);

            registerAutoFinish(this, target, msg, lang, "animanalyze_expire", TimeUnit.MINUTES.toMillis(5));
        }
    }

    @Override
    public int handleEvent(MessageReceivedEvent event) {
        try {
            if(expired) {
                System.out.println("Expired!!");
                return RESULT_FAIL;
            }

            MessageChannel ch = event.getMessage().getChannel();

            if(!ch.getId().equals(channelID)) {
                return RESULT_STILL;
            }

            AtomicReference<Long> now = new AtomicReference<>(System.currentTimeMillis());

            Message m = event.getMessage();

            if(!m.getAttachments().isEmpty()) {
                for(Message.Attachment a : m.getAttachments()) {
                    if(a.getFileName().endsWith(".png") && mixer.png == null) {
                        UpdateCheck.Downloader down = StaticStore.getDownloader(a, container);

                        if(down != null) {
                            down.run(d -> {
                                String p = "PNG : ";

                                if(d == 1.0) {
                                    p += "VALIDATING...";
                                } else {
                                    p += DataToString.df.format(d * 100.0) + "%";
                                }

                                png.set(p);

                                if(System.currentTimeMillis() - now.get() >= 1500) {
                                    now.set(System.currentTimeMillis());

                                    edit();
                                }
                            });

                            File res = new File(container, a.getFileName());

                            if(res.exists()) {
                                if(AnimMixer.validPng(res)) {
                                    png.set("PNG : SUCCESS");
                                    pngDone = true;

                                    mixer.png = ImageIO.read(res);
                                } else {
                                    png.set("PNG : INVALID");
                                }
                            } else {
                                png.set("PNG : DOWN_FAIL");
                            }

                            edit();
                        }
                    } else if((a.getFileName().endsWith(".imgcut") || a.getFileName().contains("imgcut.txt")) && mixer.imgCut == null) {
                        UpdateCheck.Downloader down = StaticStore.getDownloader(a, container);

                        if(down != null) {
                            down.run(d -> {
                                String p = "IMGCUT : ";

                                if(d == 1.0) {
                                    p += "VALIDATING...";
                                } else {
                                    p += DataToString.df.format(d * 100.0) + "%";
                                }

                                imgcut.set(p);

                                if(System.currentTimeMillis() - now.get() >= 1500) {
                                    now.set(System.currentTimeMillis());

                                    edit();
                                }
                            });

                            File res = new File(container, a.getFileName());

                            if(res.exists()) {
                                if(mixer.validImgCut(res)) {
                                    imgcut.set("IMGCUT : SUCCESS");
                                    cutDone = true;

                                    VFile vf = VFile.getFile(res);

                                    if(vf != null) {
                                        mixer.imgCut = ImgCut.newIns(vf.getData());
                                    }
                                } else {
                                    imgcut.set("IMGCUT : INVALID");
                                }
                            } else {
                                imgcut.set("IMGCUT : DOWN_FAIL");
                            }

                            edit();
                        }
                    } else if((a.getFileName().endsWith(".mamodel") || a.getFileName().contains("mamodel.txt")) && mixer.model == null) {
                        UpdateCheck.Downloader down = StaticStore.getDownloader(a, container);

                        if(down != null) {
                            down.run(d -> {
                                String p = "MAMODEL : ";

                                if(d == 1.0) {
                                    p += "VALIDATING...";
                                } else {
                                    p += DataToString.df.format(d * 100.0) + "%";
                                }

                                mamodel.set(p);

                                if(System.currentTimeMillis() - now.get() >= 1500) {
                                    now.set(System.currentTimeMillis());

                                    edit();
                                }
                            });

                            File res = new File(container, a.getFileName());

                            if(res.exists()) {
                                if(mixer.validMamodel(res)) {
                                    mamodel.set("MAMODEL : SUCCESS");
                                    modelDone = true;

                                    VFile vf = VFile.getFile(res);

                                    if(vf != null) {
                                        mixer.model = MaModel.newIns(vf.getData());
                                    }
                                } else {
                                    mamodel.set("MAMODEL : INVALID");
                                }
                            } else {
                                mamodel.set("MAMODEL : DOWN_FAIL");
                            }

                            edit();
                        }
                    } else if((a.getFileName().endsWith(".maanim") || (a.getFileName().startsWith("maanim") && a.getFileName().endsWith(".txt"))) && !animAllDone()) {
                        int ind = getLastIndex();

                        if(ind == -1)
                            continue;

                        UpdateCheck.Downloader down = StaticStore.getDownloader(a, container);

                        if(down != null) {
                            down.run(d -> {
                                String p = "MAANIM "+ind+" : ";

                                if(d == 1.0) {
                                    p += "VALIDATING...";
                                } else {
                                    p += DataToString.df.format(d * 100.0) + "%";
                                }

                                maanim.get(ind).set(p);

                                if(System.currentTimeMillis() - now.get() >= 1500) {
                                    now.set(System.currentTimeMillis());

                                    edit();
                                }
                            });

                            File res = new File(container, a.getFileName());

                            if(res.exists()) {
                                if(AnimMixer.validMaanim(res)) {
                                    maanim.get(ind).set("MAANIM "+ind+" : SUCCESS");

                                    VFile vf = VFile.getFile(res);

                                    if(vf != null) {
                                        mixer.anim[ind] = MaAnim.newIns(vf.getData());
                                    }
                                } else {
                                    maanim.get(ind).set("MAANIM "+ind+" : INVALID");
                                }
                            } else {
                                maanim.get(ind).set("MAANIM "+ind+" : DOWN_FAIL");
                            }

                            edit();
                        }
                    }
                }

                m.delete().queue();

                if(pngDone && cutDone && modelDone && animAllDone()) {
                    new Thread(() -> {
                        Guild g;

                        if(ch instanceof GuildChannel) {
                            g = event.getGuild();
                        } else {
                            g = null;
                        }

                        try {
                            for(int i = 0; i < maanim.size(); i++) {
                                EntityHandler.generateAnim(ch, mixer, g == null ? 0 : g.getBoostTier().getKey(), lang, debug, -1, raw, transparent, i);
                            }

                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    StaticStore.deleteFile(container, true);
                                }
                            }, 1000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();

                    return RESULT_FINISH;
                }
            } else if(m.getContentRaw().equals("c")) {
                msg.editMessage(LangID.getStringByID("animanalyze_cancel", lang)).queue();

                StaticStore.deleteFile(container, true);

                return RESULT_FINISH;
            }

            return RESULT_STILL;
        } catch (Exception e) {
            return RESULT_STILL;
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void expire(String id) {
        if(expired)
            return;

        expired = true;

        StaticStore.removeHolder(id, this);

        msg.editMessage(LangID.getStringByID("formst_expire", lang))
                .mentionRepliedUser(false)
                .queue();
    }

    private void edit() {
        StringBuilder content = new StringBuilder(png.get()+"\n"+imgcut.get()+"\n"+mamodel.get()+"\n");

        for(int i = 0; i < maanim.size(); i++) {
            content.append(maanim.get(i));

            if(i < maanim.size() - 1)
                content.append("\n");
        }

        msg.editMessage(content.toString()).queue();
    }

    private int getLastIndex() {
        for(int i = 0; i < mixer.anim.length; i++) {
            if(mixer.anim[i] == null)
                return i;
        }

        return -1;
    }

    private boolean animAllDone() {
        for(int i = 0; i < mixer.anim.length; i++) {
            if(mixer.anim[i] == null)
                return false;
        }

        return true;
    }
}
