package hasher;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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

/** The Hasher window: type a string, read and copy the digests as they update. */
public final class HasherApp {

    private static final String PREF_CHECKED = "checked";
    private static final int[] ICON_SIZES = {16, 24, 32, 48, 64, 256};
    /** The window may shrink to this width; the digest fields scroll instead. */
    private static final int MIN_WIDTH = 640;
    /** The window may shrink to this height; the digest rows scroll instead. */
    private static final int MIN_HEIGHT = 300;

    private final Preferences prefs = Preferences.userNodeForPackage(HasherApp.class);
    /** Multi-line so pasted line breaks are hashed, not turned into spaces like a JTextField does. */
    private final JTextArea input = new JTextArea(3, 40);
    private final List<Row> rows = new ArrayList<>();
    /** The text the results were computed from; null while the text box is empty. */
    private String hashedText;

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        SwingUtilities.invokeLater(() -> new HasherApp().show());
    }

    private void show() {
        JFrame frame = new JFrame("Hasher");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setIconImages(loadIcons());

        // Hash as the text changes; Tab moves focus instead of typing a tab.
        input.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                hashNow();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                hashNow();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // Attribute changes only; plain text has none.
            }
        });
        input.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        input.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
        input.setLineWrap(true);

        JLabel label = new JLabel("Text");
        label.setVerticalAlignment(SwingConstants.TOP);
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.add(label, BorderLayout.WEST);
        top.add(new JScrollPane(input), BorderLayout.CENTER);

        Set<String> checked = loadChecked();
        JPanel grid = new RowsPanel(new GridBagLayout());
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
        // An empty last row takes any extra height, so the rows stay at the top of a taller window.
        c.gridy = rows.size(); c.gridx = 0; c.weighty = 1;
        c.insets = new Insets(0, 0, 0, 0);
        grid.add(Box.createGlue(), c);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        content.add(top, BorderLayout.NORTH);
        JScrollPane gridScroll = new JScrollPane(grid, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        gridScroll.setBorder(BorderFactory.createEmptyBorder());
        gridScroll.getVerticalScrollBar().setUnitIncrement(16);
        content.add(gridScroll, BorderLayout.CENTER);

        frame.setContentPane(content);
        frame.pack();
        // Fit narrow or scaled screens: below the packed width the digest fields shrink and scroll,
        // below the packed height the rows scroll.
        Rectangle screen = usableScreenBounds(frame.getGraphicsConfiguration());
        Dimension fit = new Dimension(Math.min(frame.getWidth(), screen.width), Math.min(frame.getHeight(), screen.height));
        frame.setMinimumSize(new Dimension(Math.min(fit.width, MIN_WIDTH), Math.min(fit.height, MIN_HEIGHT)));
        frame.setSize(fit);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        input.requestFocusInWindow();
    }

    private void hashNow() {
        String text = input.getText();
        hashedText = text.isEmpty() ? null : text;
        rows.forEach(Row::refresh);
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

    /** The screen's bounds minus the taskbar and other insets. */
    private static Rectangle usableScreenBounds(GraphicsConfiguration gc) {
        Rectangle bounds = gc.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        return new Rectangle(bounds.x + insets.left, bounds.y + insets.top,
                bounds.width - insets.left - insets.right, bounds.height - insets.top - insets.bottom);
    }

    private static Font monoFont() {
        Font consolas = new Font("Consolas", Font.PLAIN, 14);
        return consolas.getFamily().equals("Consolas") ? consolas : new Font(Font.MONOSPACED, Font.PLAIN, 13);
    }

    /** The digest rows: as wide as the scroll pane, so the fields shrink, and as tall as the rows need, so they scroll. */
    private static final class RowsPanel extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;

        RowsPanel(LayoutManager layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return orientation == SwingConstants.VERTICAL ? visible.height : visible.width;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            // Fill a viewport taller than the rows; scroll in a shorter one.
            return getParent() instanceof JViewport v && v.getHeight() > getPreferredSize().height;
        }
    }

    /** One algorithm: its checkbox, its digest and its Copy button. */
    private final class Row {
        final String algo;
        final JCheckBox box;
        final JTextField output = new JTextField(128); // wide enough for SHA-512
        final JButton copy = new JButton("Copy");
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

            // Size the button for its widest label so the label swaps don't shift the layout.
            Dimension size = copy.getPreferredSize();
            for (String label : new String[] {"Copied", "Failed"}) {
                copy.setText(label);
                size.width = Math.max(size.width, copy.getPreferredSize().width);
            }
            copy.setPreferredSize(size);
            copy.setText("Copy");
            resetCopy.setRepeats(false);
            copy.addActionListener(e -> {
                try {
                    Toolkit.getDefaultToolkit().getSystemClipboard()
                            .setContents(new StringSelection(output.getText()), null);
                    copy.setText("Copied");
                } catch (IllegalStateException ex) {
                    // Another program has the clipboard open.
                    copy.setText("Failed");
                }
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
