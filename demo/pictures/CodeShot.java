import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders the Math Lab's code pictures: each {@code <name>.txt} here (first line: the window title, then the code) as
 * {@code <name>.png}, a code-editor screenshot in the lab's colours, at twice the size for sharp text.
 * <pre>
 * javac CodeShot.java
 * java -Djava.awt.headless=true CodeShot formulas functions precision styles safe strict about
 * </pre>
 * Then move the PNGs to {@code ../mathlab/bundle/img/}.
 *
 * @author Mark-Marion Gondelman (Orchestration)
 * @author Code: Claude Code (agentic implementation)
 */
public class CodeShot {
    static final int S = 2;                       // pixel scale, for sharp text on dense screens
    static final Color BG = new Color(0x0d1526), BAR = new Color(0x0a101d), BORDER = new Color(56, 189, 248, 110);
    static final Color INK = new Color(0xdbe7f5), DIM = new Color(0x5b7a99), GUTTER = new Color(0x3b5068);
    static final Color TAG = new Color(0x38bdf8), ATTR = new Color(0xfbbf24), VALUE = new Color(0x4ade80),
            COMMENT = new Color(0x6b7f96);

    record Run(String text, Color color) {}

    public static void main(String[] args) throws Exception {
        File consolas = new File("C:/Windows/Fonts/consola.ttf");   // the pictures were made with Consolas
        Font font = consolas.isFile()
                ? Font.createFont(Font.TRUETYPE_FONT, consolas).deriveFont(14f * S)
                : new Font(Font.MONOSPACED, Font.PLAIN, 14 * S);
        Font titleFont = font.deriveFont(12f * S);
        for (String name : args) {
            List<String> lines = Files.readAllLines(Path.of(name + ".txt"), StandardCharsets.UTF_8);
            String title = lines.getFirst();
            List<String> code = lines.subList(1, lines.size());
            render(title, code, font, titleFont, new File(name + ".png"));
        }
    }

    static void render(String title, List<String> code, Font font, Font titleFont, File out) throws Exception {
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        FontMetrics fm = probe.createGraphics().getFontMetrics(font);
        int lineH = (int) (fm.getHeight() * 1.35);
        int gutter = fm.stringWidth("00") + 18 * S;
        int pad = 18 * S, bar = 34 * S;
        int textW = 0;
        for (String line : code) textW = Math.max(textW, fm.stringWidth(line));
        int w = Math.max(520 * S, pad + gutter + textW + pad);
        int h = bar + pad + code.size() * lineH + pad;

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        int r = 12 * S;
        Shape window = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, r, r);
        g.setColor(BG);
        g.fill(window);
        g.setClip(window);
        g.setColor(BAR);
        g.fillRect(0, 0, w, bar);
        g.setClip(null);
        int dotY = bar / 2;
        Color[] dots = {new Color(0xf87171), new Color(0xfbbf24), new Color(0x4ade80)};
        for (int i = 0; i < 3; i++) {
            g.setColor(dots[i]);
            g.fillOval(pad + i * 20 * S - 6 * S, dotY - 6 * S, 12 * S, 12 * S);
        }
        g.setFont(titleFont);
        g.setColor(DIM);
        FontMetrics tm = g.getFontMetrics();
        g.drawString(title, (w - tm.stringWidth(title)) / 2, dotY + tm.getAscent() / 2 - 2 * S);
        g.setColor(new Color(56, 189, 248, 60));
        g.drawLine(0, bar, w, bar);

        g.setFont(font);
        int y = bar + pad + fm.getAscent();
        for (int n = 0; n < code.size(); n++) {
            g.setColor(GUTTER);
            String num = String.valueOf(n + 1);
            g.drawString(num, pad + fm.stringWidth("00") - fm.stringWidth(num), y);
            int x = pad + gutter;
            for (Run run : tokenize(code.get(n))) {
                g.setColor(run.color());
                g.drawString(run.text(), x, y);
                x += fm.stringWidth(run.text());
            }
            y += lineH;
        }
        g.setColor(BORDER);
        g.setStroke(new BasicStroke(1.2f * S));
        g.draw(window);
        g.dispose();
        ImageIO.write(img, "png", out);
    }

    /** XML colouring: comments, tags, attribute names, values, text; "Key: value" lines like a manifest. */
    static List<Run> tokenize(String line) {
        List<Run> runs = new ArrayList<>();
        if (line.matches("^[A-Z][\\w-]+: .*")) {
            int colon = line.indexOf(':');
            runs.add(new Run(line.substring(0, colon + 1), ATTR));
            runs.add(new Run(line.substring(colon + 1), VALUE));
            return runs;
        }
        int i = 0;
        StringBuilder text = new StringBuilder();
        while (i < line.length()) {
            if (line.startsWith("<!--", i)) {
                flush(runs, text, INK);
                int end = line.indexOf("-->", i);
                end = end < 0 ? line.length() : end + 3;
                runs.add(new Run(line.substring(i, end), COMMENT));
                i = end;
            } else if (line.charAt(i) == '<' && i + 1 < line.length()
                    && (Character.isLetter(line.charAt(i + 1)) || line.charAt(i + 1) == '/')) {
                flush(runs, text, INK);
                int j = i + 1;
                if (line.charAt(j) == '/') j++;
                runs.add(new Run(line.substring(i, j), DIM));
                int nameEnd = j;
                while (nameEnd < line.length() && " />".indexOf(line.charAt(nameEnd)) < 0) nameEnd++;
                runs.add(new Run(line.substring(j, nameEnd), TAG));
                i = nameEnd;
                while (i < line.length() && line.charAt(i) != '>') {
                    char c = line.charAt(i);
                    if (c == '"') {
                        int close = line.indexOf('"', i + 1);
                        close = close < 0 ? line.length() : close + 1;
                        runs.add(new Run(line.substring(i, close), VALUE));
                        i = close;
                    } else if (Character.isLetter(c)) {
                        int k = i;
                        while (k < line.length() && (Character.isLetterOrDigit(line.charAt(k)) || line.charAt(k) == '-')) k++;
                        runs.add(new Run(line.substring(i, k), ATTR));
                        i = k;
                    } else {
                        runs.add(new Run(String.valueOf(c), DIM));
                        i++;
                    }
                }
                if (i < line.length()) {
                    runs.add(new Run(">", DIM));
                    i++;
                }
            } else {
                text.append(line.charAt(i));
                i++;
            }
        }
        flush(runs, text, INK);
        return runs;
    }

    static void flush(List<Run> runs, StringBuilder text, Color color) {
        if (!text.isEmpty()) {
            runs.add(new Run(text.toString(), color));
            text.setLength(0);
        }
    }
}
