package hasher;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

/** The Hasher window: type a string, press Hash, read and copy the digests. */
public final class HasherApp {

    private static final String PREF_CHECKED = "checked";
    private static final int[] ICON_SIZES = {16, 24, 32, 48, 64, 256};

    private final Preferences prefs = Preferences.userNodeForPackage(HasherApp.class);
    private final JTextField input = new JTextField(40);
    private final List<Row> rows = new ArrayList<>();
    /** The text the results were computed from; null until the first Hash. */
    private String hashedText;

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        SwingUtilities.invokeLater(() -> new HasherApp().show());
    }

    private void show() {
        JFrame frame = new JFrame("Hasher");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setIconImages(loadIcons());

        JButton hashButton = new JButton("Hash");
        hashButton.addActionListener(e -> hashNow());
        frame.getRootPane().setDefaultButton(hashButton); // Enter hashes

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.add(new JLabel("Text"), BorderLayout.WEST);
        top.add(input, BorderLayout.CENTER);
        top.add(hashButton, BorderLayout.EAST);

        Set<String> checked = loadChecked();
        JPanel grid = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 0, 3, 8);
        c.anchor = GridBagConstraints.WEST;
        for (String algo : Hasher.ALGORITHMS) {
            Row row = new Row(algo, checked.contains(algo));
            rows.add(row);
            c.gridy = rows.size() - 1;
            c.gridx = 0; c.weightx = 0; c.fill = GridBagConstraints.NONE;
            grid.add(row.box, c);
            c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
            grid.add(row.output, c);
            c.gridx = 2; c.weightx = 0; c.fill = GridBagConstraints.NONE;
            c.insets.right = 0;
            grid.add(row.copy, c);
            c.insets.right = 8;
        }

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        content.add(top, BorderLayout.NORTH);
        content.add(grid, BorderLayout.CENTER);

        frame.setContentPane(content);
        frame.pack();
        frame.setMinimumSize(frame.getSize());
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        input.requestFocusInWindow();
    }

    private void hashNow() {
        hashedText = input.getText();
        rows.forEach(Row::refresh);
        input.selectAll();
    }

    private Set<String> loadChecked() {
        String saved = prefs.get(PREF_CHECKED, null);
        if (saved == null) return new HashSet<>(Hasher.DEFAULTS);
        return new HashSet<>(Arrays.asList(saved.split(",")));
    }

    private void saveChecked() {
        List<String> names = rows.stream().filter(r -> r.box.isSelected()).map(r -> r.algo).toList();
        prefs.put(PREF_CHECKED, String.join(",", names));
    }

    private static List<Image> loadIcons() {
        List<Image> icons = new ArrayList<>();
        for (int size : ICON_SIZES) {
            try (InputStream in = HasherApp.class.getResourceAsStream("icon-" + size + ".png")) {
                if (in != null) icons.add(ImageIO.read(in));
            } catch (IOException ignored) {
                // A missing icon just falls back to the default one.
            }
        }
        return icons;
    }

    private static Font monoFont() {
        Font consolas = new Font("Consolas", Font.PLAIN, 14);
        return consolas.getFamily().equals("Consolas") ? consolas : new Font(Font.MONOSPACED, Font.PLAIN, 13);
    }

    /** One algorithm: its checkbox, its digest and its Copy button. */
    private final class Row {
        final String algo;
        final JCheckBox box;
        final JTextField output = new JTextField(128); // wide enough for SHA-512
        final JButton copy = new JButton("Copied");
        private final Timer resetCopy = new Timer(1200, e -> copy.setText("Copy"));

        Row(String algo, boolean checked) {
            this.algo = algo;
            box = new JCheckBox(algo, checked);
            box.addActionListener(e -> {
                saveChecked();
                refresh();
            });

            output.setEditable(false);
            output.setFont(monoFont());

            // Size the button for "Copied" so the label swap doesn't shift the layout.
            copy.setPreferredSize(copy.getPreferredSize());
            copy.setText("Copy");
            resetCopy.setRepeats(false);
            copy.addActionListener(e -> {
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(output.getText()), null);
                copy.setText("Copied");
                resetCopy.restart();
            });
            refresh();
        }

        void refresh() {
            boolean ok = false;
            if (!box.isSelected() || hashedText == null) {
                output.setText("");
            } else {
                try {
                    output.setText(Hasher.hash(hashedText, algo));
                    ok = true;
                } catch (NoSuchAlgorithmException ex) {
                    output.setText("error: " + algo + " is not available in this Java runtime");
                }
            }
            output.setCaretPosition(0);
            output.setEnabled(box.isSelected());
            copy.setEnabled(ok);
        }
    }
}
