package mandarin.packpack.supporter.awt;

import common.system.fake.FakeGraphics;
import common.system.fake.FakeImage;
import common.system.fake.FakeTransform;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lzw.AnimatedGifEncoder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.RenderingHints.Key;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;

import static java.awt.AlphaComposite.SRC_OVER;

public class FG2D implements FakeGraphics {
	private static final Object KAS = RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED;
	private static final Object KAD = RenderingHints.VALUE_ALPHA_INTERPOLATION_DEFAULT;
	private static final Object KAQ = RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY;
	private static final Object KCS = RenderingHints.VALUE_COLOR_RENDER_SPEED;
	private static final Object KCD = RenderingHints.VALUE_COLOR_RENDER_DEFAULT;
	private static final Object KCQ = RenderingHints.VALUE_COLOR_RENDER_QUALITY;
	private static final Object KFS = RenderingHints.VALUE_FRACTIONALMETRICS_OFF;
	private static final Object KFD = RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT;
	private static final Object KFQ = RenderingHints.VALUE_FRACTIONALMETRICS_ON;
	private static final Object KIS = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
	private static final Object KID = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
	private static final Object KIQ = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
	private static final Key KA = RenderingHints.KEY_ALPHA_INTERPOLATION;
	private static final Key KC = RenderingHints.KEY_COLOR_RENDERING;
	private static final Key KF = RenderingHints.KEY_FRACTIONALMETRICS;
	private static final Key KI = RenderingHints.KEY_INTERPOLATION;
	private static final Key[] KEYS = new Key[] { KA, KC, KF, KI };
	private static final Object[][] VALS = new Object[][] { { KAS, KAD, KAQ }, { KCS, KCD, KCQ }, { KFS, KFD, KFQ },
			{ KIS, KID, KIQ } };

	private final Graphics2D g;
	private final Composite comp;
	private final BufferedImage original;

	private final File progress;

	public FG2D(Graphics graphics) {
		g = (Graphics2D) graphics;
		comp = g.getComposite();
		original = null;
		progress = null;
	}

	public FG2D(Graphics graphics, BufferedImage original) {
		g = (Graphics2D) graphics;
		comp = g.getComposite();
		this.original = original;

		File temp = new File("./temp");

		if(temp.exists()) {
			progress = StaticStore.generateTempFile(temp, "progress", "", true);
		} else {
			progress = null;
		}
	}

	@Override
	public void colRect(int x, int y, int w, int h, int r, int gr, int b, int a) {
		int al = a >= 0 ? a : 255;
		Color c = new Color(r, gr, b, al);
		g.setColor(c);
		g.fillRect(x, y, w, h);

		exportProgress();
	}

	@Override
	public void drawImage(FakeImage bimg, double i, double j) {
		g.drawImage((Image) bimg.bimg(), (int) i, (int) j, null);

		exportProgress();
	}

	public void drawImage(BufferedImage bimg, double i, double j) {
		g.drawImage(bimg, (int) i, (int) j, null);

		exportProgress();
	}

	@Override
	public void drawImage(FakeImage bimg, double ix, double iy, double iw, double ih) {
		g.drawImage((Image) bimg.bimg(), (int) ix, (int) iy, (int) iw, (int) ih, null);

		exportProgress();
	}

	public void drawImage(BufferedImage bimg, double ix, double iy, double iw, double ih) {
		g.drawImage(bimg, (int) ix, (int) iy, (int) iw, (int) ih, null);

		exportProgress();
	}

	@Override
	public void drawLine(int i, int j, int x, int y) {
		g.drawLine(i, j, x, y);

		exportProgress();
	}

	@Override
	public void drawOval(int i, int j, int k, int l) {
		g.drawOval(i, j, k, l);

		exportProgress();
	}

	@Override
	public void drawRect(int x, int y, int x2, int y2) {
		g.drawRect(x, y, x2, y2);

		exportProgress();
	}

	@Override
	public void fillOval(int i, int j, int k, int l) {
		g.fillOval(i, j, k, l);

		exportProgress();
	}

	@Override
	public void fillRect(int x, int y, int w, int h) {
		g.fillRect(x, y, w, h);

		exportProgress();
	}

	@Override
	public FakeTransform getTransform() {
		return new FTAT(g.getTransform());
	}

	@Override
	public void gradRect(int x, int y, int w, int h, int a, int b, int[] c, int d, int e, int[] f) {
		g.setPaint(new GradientPaint(a, b, new Color(c[0], c[1], c[2]), d, e, new Color(f[0], f[1], f[2])));
		g.fillRect(x, y, w, h);

		exportProgress();
	}

	@Override
	public void gradRectAlpha(int x, int y, int w, int h, int a, int b, int al, int[] c, int d, int e, int al2, int[] f) {
		g.setPaint(new GradientPaint(a, b, new Color(c[0], c[1], c[2], al), d, e, new Color(f[0], f[1], f[2], al2)));
		g.fillRect(x, y, w, h);

		exportProgress();
	}

	@Override
	public void rotate(double d) {
		g.rotate(d);
	}

	@Override
	public void scale(int hf, int vf) {
		g.scale(hf, vf);
	}

	public void scaleDouble(double sx, double sy) {
		g.scale(sx, sy);
	}

	@Override
	public void setColor(int c) {
		if (c == RED)
			g.setColor(Color.RED);
		if (c == YELLOW)
			g.setColor(Color.YELLOW);
		if (c == BLACK)
			g.setColor(Color.BLACK);
		if (c == MAGENTA)
			g.setColor(Color.MAGENTA);
		if (c == BLUE)
			g.setColor(Color.BLUE);
		if (c == CYAN)
			g.setColor(Color.CYAN);
		if (c == WHITE)
			g.setColor(Color.WHITE);
	}

	@Override
	public void setColor(int r, int g, int b) {
		this.g.setColor(new Color(r, g, b));
	}

	public void setColor(int r, int g, int b, int a) {
		this.g.setColor(new Color(r, g, b, a));
	}

	public void drawText(String text, int x, int y) {
		g.drawString(text, x, y);

		exportProgress();
	}

	public void changeFontSize(float pt) {
		g.setFont(g.getFont().deriveFont(pt));
	}

	public void setFont(Font font) {
		g.setFont(font);
	}

	public void drawCenteredText(String text, int x, int y) {
		FontMetrics fm = g.getFontMetrics();

		Rectangle2D rect = fm.getStringBounds(text, g);

		g.drawString(text, (int) (x - rect.getWidth() / 2 - rect.getX()), (int) (y - rect.getHeight() / 2 - rect.getY()));

		exportProgress();
	}

	public void drawVerticalCenteredText(String text, int x, int y) {
		FontMetrics fm = g.getFontMetrics();

		Rectangle2D rect = fm.getStringBounds(text, g);

		g.drawString(text, x, (int) (y + (rect.getHeight() - fm.getDescent()) / 2));

		exportProgress();
	}

	public void setStroke(float f) {
		g.setStroke(new BasicStroke(f,BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
	}

	public void setStroke(float f, int mode, int joinMode) {
		g.setStroke(new BasicStroke(f, mode, joinMode));
	}

	public void roundRect(int x, int y, int w, int h, int aw, int ah) {
		g.drawRoundRect(x, y, w, h, aw, ah);

		exportProgress();
	}

	public void fillRoundRect(int x, int y, int w, int h, int aw, int ah) {
		g.fillRoundRect(x, y, w, h, aw, ah);

		exportProgress();
	}

	@Override
	public void setComposite(int mode, int p0, int p1) {
		if (mode == DEF)
			g.setComposite(comp);
		if (mode == TRANS)
			g.setComposite(AlphaComposite.getInstance(SRC_OVER, (float) (p0 / 256.0)));
		if (mode == BLEND)
			g.setComposite(new Blender(p0, p1));
		if (mode == GRAY)
			g.setComposite(new Converter(p0));

	}

	@Override
	public void setRenderingHint(int key, int val) {
		g.setRenderingHint(KEYS[key], VALS[key][val]);
	}

	@Override
	public void setTransform(FakeTransform at) {
		g.setTransform((AffineTransform) at.getAT());
	}

	@Override
	public void translate(double x, double y) {
		g.translate(x, y);
	}

	public void enableAntialiasing() {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	}

	public void setGradient(int x, int y, int x2, int y2, Color c, Color c2, float ratio) {
		LinearGradientPaint gra = new LinearGradientPaint(x, y, x2, y2, new float[] {0.4f, 1f}, new Color[] {c, c2});

		g.setPaint(gra);
	}

	public void drawFontOutline(Path2D path, float width) {
		Stroke st = g.getStroke();
		Color c = g.getColor();

		Stroke round = new BasicStroke(width,BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

		setColor(BLACK);

		g.setStroke(round);
		g.draw(path);

		g.setStroke(st);
		g.setColor(c);
	}

	public void fillPath2D(Path2D path) {
		g.fill(path);
	}

	private void exportProgress() {
		try {
			if(progress != null) {
				File image = StaticStore.generateTempFile(progress, "p", ".png", false);

				if(image != null && image.exists()) {
					ImageIO.write(original, "PNG", image);
				}
			}
		} catch (Exception ignored) {

		}
	}

	public void export() throws Exception {
		if(progress == null)
			return;

		AnimatedGifEncoder encoder = new AnimatedGifEncoder();

		encoder.setSize(original.getWidth(), original.getHeight());
		encoder.setFrameRate(10f);
		encoder.setRepeat(1);

		File result = StaticStore.generateTempFile(progress, "progression", ".gif", false);

		if(result == null || !result.exists())
			return;

		FileOutputStream fos = new FileOutputStream(result);

		encoder.start(fos);

		int i = 0;

		while(true) {
			File image = new File(progress, "p" + (i == 0 ? "" : "_" + i) + ".png");

			if(!image.exists())
				break;

			BufferedImage img = ImageIO.read(image);

			encoder.addFrame(img);

			i++;
		}

		encoder.finish();
		fos.close();
	}
}
