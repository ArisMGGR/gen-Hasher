import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws the Hasher "#" icon and writes the window icons and the exe icon.
 * Run from the project root: java tools/IconGen.java
 */
public class IconGen {

    static final int[] SIZES = {16, 24, 32, 48, 64, 256};

    public static void main(String[] args) throws IOException {
        Path resources = Path.of("src/main/resources/hasher");
        Path packaging = Path.of("src/main/packaging");
        Files.createDirectories(resources);
        Files.createDirectories(packaging);

        List<byte[]> pngs = new ArrayList<>();
        for (int size : SIZES) {
            byte[] png = toPng(draw(size));
            Files.write(resources.resolve("icon-" + size + ".png"), png);
            pngs.add(png);
        }
        Files.write(packaging.resolve("hasher.ico"), toIco(pngs));
    }

    static BufferedImage draw(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        double s = size;
        g.setPaint(new GradientPaint(0, 0, new Color(0x5B8DEF), 0, size, new Color(0x3A5FC8)));
        g.fill(new RoundRectangle2D.Double(0, 0, s, s, s * 0.36, s * 0.36));

        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke((float) Math.max(1.5, s * 0.095), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double lo = s * 0.24, hi = s * 0.76, slant = s * 0.05;
        for (double x : new double[] {s * 0.40, s * 0.62}) {
            g.draw(new Line2D.Double(x + slant, lo, x - slant, hi));
        }
        for (double y : new double[] {s * 0.39, s * 0.61}) {
            g.draw(new Line2D.Double(lo, y, hi, y));
        }
        g.dispose();
        return img;
    }

    static byte[] toPng(BufferedImage img) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    /** An .ico file whose entries are PNG images (supported since Windows Vista). */
    static byte[] toIco(List<byte[]> pngs) {
        int headerSize = 6 + 16 * pngs.size();
        int total = headerSize + pngs.stream().mapToInt(p -> p.length).sum();
        ByteBuffer buf = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);
        buf.putShort((short) 0).putShort((short) 1).putShort((short) pngs.size());
        int offset = headerSize;
        for (int i = 0; i < pngs.size(); i++) {
            int size = SIZES[i];
            buf.put((byte) (size >= 256 ? 0 : size)).put((byte) (size >= 256 ? 0 : size));
            buf.put((byte) 0).put((byte) 0);
            buf.putShort((short) 1).putShort((short) 32);
            buf.putInt(pngs.get(i).length).putInt(offset);
            offset += pngs.get(i).length;
        }
        pngs.forEach(buf::put);
        return buf.array();
    }
}
