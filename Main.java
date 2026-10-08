import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Main {
    private static final Path DATA_FILE = getDataFile();
    private static final Path LEGACY_DATA_FILE = Paths.get("estrazione-studenti-data.txt")
            .toAbsolutePath().normalize();
    private static final Color BACKGROUND = new Color(244, 246, 250);
    private static final Color SURFACE = new Color(255, 255, 255);
    private static final Color ACCENT = new Color(88, 81, 232);
    private static final Color ACCENT_DARK = new Color(70, 62, 211);
    private static final Color TEXT = new Color(28, 35, 53);
    private static final Color MUTED = new Color(119, 128, 148);
    private static final Color BORDER = new Color(231, 234, 241);
    private static final Color GREEN = new Color(34, 148, 111);

    private final Map<String, List<String>> classes = new LinkedHashMap<String, List<String>>();
    private final Map<String, Map<String, List<String>>> events =
            new LinkedHashMap<String, Map<String, List<String>>>();
    private final Random random = new Random();
    private final JComboBox<String> classSelector = new JComboBox<String>();
    private final JComboBox<String> eventSelector = new JComboBox<String>();
    private final JLabel studentCount = new JLabel("0");
    private final JLabel drawnCount = new JLabel("0");
    private final JLabel remainingCount = new JLabel("0");
    private final JLabel result = new JLabel("Ready for the first draw", SwingConstants.CENTER);
    private final JLabel activeContext = new JLabel("Select a class and an event");
    private final JPanel roster = new JPanel();
    private final RoundedPanel resultCard = new RoundedPanel(new Color(39, 39, 82), 24);
    private final JButton drawButton = new RoundedButton("Draw the next student", ACCENT, Color.WHITE);
    private boolean updatingSelectors;

    private Main() throws IOException {
        prepareDataDirectory();
        loadState();
    }

    private static Path getDataFile() {
        String appData = System.getenv("APPDATA");
        Path dataDirectory = appData == null || appData.trim().isEmpty()
                ? Paths.get(System.getProperty("user.home"), ".student-draw")
                : Paths.get(appData, "Student Draw");
        return dataDirectory.resolve("student-draw-data.txt");
    }

    private void prepareDataDirectory() throws IOException {
        Files.createDirectories(DATA_FILE.getParent());
        if (!Files.exists(DATA_FILE) && Files.exists(LEGACY_DATA_FILE)
                && !LEGACY_DATA_FILE.equals(DATA_FILE)) {
            Files.copy(LEGACY_DATA_FILE, DATA_FILE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                         | UnsupportedLookAndFeelException exception) {
                    showStartupError(exception);
                }
                try {
                    new Main().showWindow();
                } catch (IOException exception) {
                    JOptionPane.showMessageDialog(null,
                            "Could not load saved data from "
                                    + DATA_FILE.toAbsolutePath() + ":\n" + exception.getMessage(),
                            "Loading error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private static void showStartupError(Exception exception) {
        JOptionPane.showMessageDialog(null,
                "Could not apply the system appearance: " + exception.getMessage(),
                "Warning", JOptionPane.WARNING_MESSAGE);
    }

    private void showWindow() {
        JFrame frame = new JFrame("Student Draw");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(900, 620));
        frame.setSize(1120, 760);
        frame.setLocationRelativeTo(null);
        frame.setContentPane(buildContent());
        refreshClasses(null);
        frame.setVisible(true);
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(22, 22));
        root.setBackground(BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(26, 30, 18, 30));

        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        JPanel brand = new JPanel(new BorderLayout(13, 0));
        brand.setOpaque(false);
        JLabel mark = new JLabel("E", SwingConstants.CENTER);
        mark.setFont(new Font("SansSerif", Font.BOLD, 23));
        mark.setForeground(Color.WHITE);
        mark.setOpaque(true);
        mark.setBackground(ACCENT);
        mark.setPreferredSize(new Dimension(48, 48));
        JPanel brandText = new JPanel(new GridLayout(2, 1, 0, 3));
        brandText.setOpaque(false);
        JLabel title = new JLabel("Student Draw");
        title.setFont(new Font("SansSerif", Font.BOLD, 25));
        title.setForeground(TEXT);
        JLabel subtitle = new JLabel("A simple, clear way to draw students without repeats.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(MUTED);
        brandText.add(title);
        brandText.add(subtitle);
        brand.add(mark, BorderLayout.WEST);
        brand.add(brandText, BorderLayout.CENTER);
        header.add(brand, BorderLayout.WEST);

        JPanel saveBadge = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        saveBadge.setOpaque(false);
        JLabel savedDot = new JLabel("\u25cf");
        savedDot.setForeground(GREEN);
        JLabel savedText = new JLabel("AUTOMATICALLY SAVED");
        savedText.setFont(new Font("SansSerif", Font.BOLD, 10));
        savedText.setForeground(new Color(82, 117, 105));
        RoundedPanel badge = new RoundedPanel(new Color(232, 246, 240), 18);
        badge.setLayout(new FlowLayout(FlowLayout.CENTER, 7, 5));
        badge.add(savedDot);
        badge.add(savedText);
        saveBadge.add(badge);
        header.add(saveBadge, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        JPanel workspace = new JPanel(new BorderLayout(20, 0));
        workspace.setOpaque(false);
        workspace.add(buildSidebar(), BorderLayout.WEST);
        workspace.add(buildMainPanel(), BorderLayout.CENTER);
        root.add(workspace, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JLabel dataLocation = new JLabel("Data saved to  " + DATA_FILE.toAbsolutePath());
        dataLocation.setFont(new Font("SansSerif", Font.PLAIN, 11));
        dataLocation.setForeground(MUTED);
        footer.add(dataLocation, BorderLayout.WEST);
        root.add(footer, BorderLayout.SOUTH);

        classSelector.addActionListener(event -> {
            if (!updatingSelectors) {
                refreshEvents(null);
            }
        });
        eventSelector.addActionListener(event -> {
            if (!updatingSelectors) {
                updateClassDetails();
            }
        });
        drawButton.addActionListener(event -> drawStudent());
        updateDrawButton();
        return root;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setOpaque(false);
        sidebar.setPreferredSize(new Dimension(278, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        RoundedPanel selection = new RoundedPanel(SURFACE, 20);
        selection.setLayout(new BoxLayout(selection, BoxLayout.Y_AXIS));
        selection.setBorder(BorderFactory.createEmptyBorder(20, 18, 19, 18));
        selection.setAlignmentX(Component.LEFT_ALIGNMENT);
        selection.add(sectionTitle("YOUR SESSION"));
        selection.add(Box.createVerticalStrut(19));
        selection.add(fieldLabel("CLASS"));
        selection.add(Box.createVerticalStrut(6));
        styleSelector(classSelector);
        selection.add(classSelector);
        selection.add(Box.createVerticalStrut(14));
        selection.add(fieldLabel("EVENT"));
        selection.add(Box.createVerticalStrut(6));
        styleSelector(eventSelector);
        selection.add(eventSelector);
        selection.add(Box.createVerticalStrut(11));
        activeContext.setFont(new Font("SansSerif", Font.PLAIN, 11));
        activeContext.setForeground(MUTED);
        activeContext.setAlignmentX(Component.LEFT_ALIGNMENT);
        selection.add(activeContext);
        sidebar.add(selection);
        sidebar.add(Box.createVerticalStrut(17));

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        actions.add(sectionTitle("MANAGE"));
        actions.add(Box.createVerticalStrut(10));
        actions.add(actionButton("＋   Create a class", false, () -> addClass()));
        actions.add(Box.createVerticalStrut(8));
        actions.add(actionButton("＋   Create an event", false, () -> addEvent()));
        actions.add(Box.createVerticalStrut(8));
        actions.add(actionButton("＋   Add a student", false, () -> addStudent()));
        actions.add(Box.createVerticalStrut(8));
        actions.add(actionButton("⇧   Import a .txt list", false, () -> importFile()));
        sidebar.add(actions);
        sidebar.add(Box.createVerticalGlue());
        return sidebar;
    }

    private JPanel buildMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 16));
        mainPanel.setOpaque(false);

        resultCard.setOpaque(false);
        resultCard.setBackground(new Color(39, 39, 82));
        resultCard.setLayout(new BorderLayout(12, 12));
        resultCard.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        resultCard.setPreferredSize(new Dimension(0, 268));

        JPanel resultHeading = new JPanel(new BorderLayout());
        resultHeading.setOpaque(false);
        JLabel eyebrow = new JLabel("  STUDENT DRAW");
        eyebrow.setOpaque(true);
        eyebrow.setBackground(new Color(58, 58, 108));
        eyebrow.setForeground(new Color(211, 208, 255));
        eyebrow.setFont(new Font("SansSerif", Font.BOLD, 10));
        eyebrow.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
        resultHeading.add(eyebrow, BorderLayout.WEST);
        JLabel randomIcon = new JLabel("\u2726");
        randomIcon.setFont(new Font("SansSerif", Font.PLAIN, 23));
        randomIcon.setForeground(new Color(185, 180, 255));
        resultHeading.add(randomIcon, BorderLayout.EAST);
        resultCard.add(resultHeading, BorderLayout.NORTH);

        result.setFont(new Font("SansSerif", Font.BOLD, 32));
        result.setForeground(Color.WHITE);
        result.setBorder(BorderFactory.createEmptyBorder(9, 0, 0, 0));
        resultCard.add(result, BorderLayout.CENTER);

        JPanel drawFooter = new JPanel(new BorderLayout());
        drawFooter.setOpaque(false);
        drawFooter.add(drawButton, BorderLayout.WEST);
        JLabel drawHint = new JLabel("  Students won't be drawn twice in the same event");
        drawHint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        drawHint.setForeground(new Color(194, 194, 218));
        drawFooter.add(drawHint, BorderLayout.CENTER);
        resultCard.add(drawFooter, BorderLayout.SOUTH);
        mainPanel.add(resultCard, BorderLayout.NORTH);

        RoundedPanel rosterCard = new RoundedPanel(SURFACE, 20);
        rosterCard.setLayout(new BorderLayout(0, 14));
        rosterCard.setBorder(BorderFactory.createEmptyBorder(20, 22, 18, 22));

        JPanel rosterHeader = new JPanel(new BorderLayout(8, 10));
        rosterHeader.setOpaque(false);
        JPanel rosterTitle = new JPanel(new GridLayout(2, 1, 0, 4));
        rosterTitle.setOpaque(false);
        JLabel registerTitle = new JLabel("Class roster");
        registerTitle.setFont(new Font("SansSerif", Font.BOLD, 17));
        registerTitle.setForeground(TEXT);
        JLabel registerSubtitle = new JLabel("Track progress for the selected event.");
        registerSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        registerSubtitle.setForeground(MUTED);
        rosterTitle.add(registerTitle);
        rosterTitle.add(registerSubtitle);
        rosterHeader.add(rosterTitle, BorderLayout.WEST);

        JPanel statistics = new JPanel(new GridLayout(1, 3, 8, 0));
        statistics.setOpaque(false);
        statistics.add(statCard("STUDENTS", studentCount, TEXT));
        statistics.add(statCard("DRAWN", drawnCount, ACCENT));
        statistics.add(statCard("REMAINING", remainingCount, GREEN));
        rosterHeader.add(statistics, BorderLayout.EAST);
        rosterCard.add(rosterHeader, BorderLayout.NORTH);

        roster.setOpaque(false);
        roster.setLayout(new BoxLayout(roster, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(roster);
        scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        rosterCard.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(rosterCard, BorderLayout.CENTER);

        classSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        eventSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        return mainPanel;
    }

    private JPanel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 10));
        label.setForeground(MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(label, BorderLayout.WEST);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        return wrapper;
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 10));
        label.setForeground(MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 15));
        return label;
    }

    private void styleSelector(JComboBox<String> selector) {
        selector.setFont(new Font("SansSerif", Font.PLAIN, 13));
        selector.setForeground(TEXT);
        selector.setBackground(SURFACE);
        selector.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        selector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                label.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
                if (isSelected) {
                    label.setBackground(new Color(237, 236, 255));
                    label.setForeground(ACCENT_DARK);
                } else {
                    label.setForeground(TEXT);
                }
                return label;
            }
        });
    }

    private RoundedPanel statCard(String caption, JLabel value, Color color) {
        RoundedPanel card = new RoundedPanel(new Color(247, 248, 251), 12);
        card.setLayout(new GridLayout(2, 1, 0, 1));
        card.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
        JLabel label = new JLabel(caption);
        label.setFont(new Font("SansSerif", Font.BOLD, 8));
        label.setForeground(MUTED);
        value.setFont(new Font("SansSerif", Font.BOLD, 17));
        value.setForeground(color);
        card.add(label);
        card.add(value);
        card.setPreferredSize(new Dimension(82, 54));
        return card;
    }

    private JButton actionButton(String text, boolean primary, Runnable action) {
        Color background = primary ? ACCENT : SURFACE;
        Color foreground = primary ? Color.WHITE : TEXT;
        RoundedButton button = new RoundedButton(text, background, foreground);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setPreferredSize(new Dimension(240, 42));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.addActionListener(event -> action.run());
        return button;
    }

    private void addClass() {
        String name = JOptionPane.showInputDialog(null, "Enter the new class name:",
                "Create a class", JOptionPane.PLAIN_MESSAGE);
        if (name == null) {
            return;
        }
        name = name.trim();
        if (name.isEmpty()) {
            showError("The class name cannot be empty.");
            return;
        }
        if (classes.containsKey(name)) {
            showError("A class with this name already exists.");
            return;
        }
        classes.put(name, new ArrayList<String>());
        saveAfterChange();
        refreshClasses(name);
    }

    private void addEvent() {
        String className = (String) classSelector.getSelectedItem();
        if (className == null) {
            showError("Create a class or import a file first.");
            return;
        }
        String name = JOptionPane.showInputDialog(null, "Enter an event name (e.g. Oral exams round 1):",
                "Create an event", JOptionPane.PLAIN_MESSAGE);
        if (name == null) {
            return;
        }
        name = name.trim();
        if (name.isEmpty()) {
            showError("The event name cannot be empty.");
            return;
        }
        Map<String, List<String>> classEvents = events.get(className);
        if (classEvents == null) {
            classEvents = new LinkedHashMap<String, List<String>>();
            events.put(className, classEvents);
        }
        if (classEvents.containsKey(name)) {
            showError("An event with this name already exists for the selected class.");
            return;
        }
        classEvents.put(name, new ArrayList<String>());
        saveAfterChange();
        refreshEvents(name);
    }

    private void addStudent() {
        String className = (String) classSelector.getSelectedItem();
        if (className == null) {
            showError("Create a class or import a file first.");
            return;
        }
        String name = JOptionPane.showInputDialog(null, "Enter the student's full name:",
                "Add a student", JOptionPane.PLAIN_MESSAGE);
        if (name == null) {
            return;
        }
        name = name.trim();
        if (name.isEmpty()) {
            showError("The student's name cannot be empty.");
            return;
        }
        classes.get(className).add(name);
        saveAfterChange();
        updateClassDetails();
    }

    private void drawStudent() {
        String className = (String) classSelector.getSelectedItem();
        if (className == null) {
            showError("Create a class or import a file first.");
            return;
        }
        String eventName = (String) eventSelector.getSelectedItem();
        if (eventName == null) {
            showError("Create an event for the selected class first.");
            return;
        }
        List<String> students = classes.get(className);
        Set<String> drawn = new HashSet<String>(events.get(className).get(eventName));
        List<String> available = new ArrayList<String>();
        Set<String> availableNames = new HashSet<String>();
        for (String student : students) {
            if (!drawn.contains(student) && availableNames.add(student)) {
                available.add(student);
            }
        }
        if (available.isEmpty()) {
            showError(students.isEmpty()
                    ? "The selected class has no students."
                    : "All students have already been drawn for this event.");
            return;
        }
        String student = available.get(random.nextInt(available.size()));
        events.get(className).get(eventName).add(student);
        saveAfterChange();
        updateClassDetails();
        result.setText(student);
    }

    private void importFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select the class list file");
        chooser.setFileFilter(new FileNameExtensionFilter("File di testo (*.txt)", "txt"));
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path file = chooser.getSelectedFile().toPath();
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            Map<String, List<String>> imported = parseClasses(lines);
            for (Map.Entry<String, List<String>> entry : imported.entrySet()) {
                List<String> students = classes.get(entry.getKey());
                if (students == null) {
                    students = new ArrayList<String>();
                    classes.put(entry.getKey(), students);
                }
                students.addAll(entry.getValue());
            }
            String selected = imported.isEmpty() ? null : imported.keySet().iterator().next();
            boolean saved = saveAfterChange();
            refreshClasses(selected);
            if (saved) {
                JOptionPane.showMessageDialog(null,
                        "Import complete: " + imported.size() + " classes loaded.",
                        "Import complete", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException exception) {
            showError("Could not read the file:\n" + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        }
    }

    private Map<String, List<String>> parseClasses(List<String> lines) {
        Map<String, List<String>> imported = new LinkedHashMap<String, List<String>>();
        String currentClass = null;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (i == 0 && line.startsWith("\uFEFF")) {
                line = line.substring(1).trim();
            }
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                String className = line.substring(1, line.length() - 1).trim();
                if (className.isEmpty()) {
                    throw new IllegalArgumentException("Empty class name on line " + (i + 1) + ".");
                }
                currentClass = className;
                if (!imported.containsKey(currentClass)) {
                    imported.put(currentClass, new ArrayList<String>());
                }
            } else {
                if (line.startsWith("[") || line.endsWith("]")) {
                    throw new IllegalArgumentException("Invalid class heading on line "
                            + (i + 1) + ". Use the format [ClassName].");
                }
                if (currentClass == null) {
                    throw new IllegalArgumentException(
                            "Student without a class on line " + (i + 1)
                                    + ". Each class must start with [ClassName].");
                }
                imported.get(currentClass).add(line);
            }
        }
        if (imported.isEmpty()) {
            throw new IllegalArgumentException("The file contains no classes to import.");
        }
        return imported;
    }

    private void refreshClasses(String selectedClass) {
        updatingSelectors = true;
        classSelector.removeAllItems();
        for (String className : classes.keySet()) {
            classSelector.addItem(className);
        }
        if (selectedClass != null) {
            classSelector.setSelectedItem(selectedClass);
        }
        updatingSelectors = false;
        refreshEvents(null);
    }

    private void refreshEvents(String selectedEvent) {
        updatingSelectors = true;
        String className = (String) classSelector.getSelectedItem();
        eventSelector.removeAllItems();
        if (className != null && events.containsKey(className)) {
            for (String eventName : events.get(className).keySet()) {
                eventSelector.addItem(eventName);
            }
        }
        if (selectedEvent != null) {
            eventSelector.setSelectedItem(selectedEvent);
        }
        updatingSelectors = false;
        updateClassDetails();
    }

    private void updateClassDetails() {
        String className = (String) classSelector.getSelectedItem();
        roster.removeAll();
        if (className == null) {
            roster.add(emptyState("No classes yet",
                    "Create a class or import a list to get started."));
            studentCount.setText("0");
            drawnCount.setText("0");
            remainingCount.setText("0");
            activeContext.setText("Select a class and an event");
            result.setText("Ready to draw");
            updateDrawButton();
            roster.revalidate();
            roster.repaint();
            return;
        }
        List<String> students = classes.get(className);
        String eventName = (String) eventSelector.getSelectedItem();
        Set<String> drawn = new HashSet<String>();
        if (eventName != null && events.containsKey(className)) {
            List<String> drawnStudents = events.get(className).get(eventName);
            if (drawnStudents != null) {
                drawn.addAll(drawnStudents);
            }
        }
        Set<String> drawnSet = drawn;
        for (String student : students) {
            boolean alreadyDrawn = drawnSet.contains(student);
            roster.add(studentRow(student, alreadyDrawn));
            roster.add(Box.createVerticalStrut(7));
        }
        if (students.isEmpty()) {
            roster.add(emptyState("This class is empty",
                    "Add students or import a text file."));
        }
        int remaining = 0;
        for (String student : students) {
            if (!drawnSet.contains(student)) {
                remaining++;
            }
        }
        studentCount.setText(String.valueOf(students.size()));
        drawnCount.setText(String.valueOf(drawn.size()));
        remainingCount.setText(String.valueOf(remaining));
        if (eventName == null) {
            activeContext.setText("Create an event to start drawing");
            result.setText("Select or create an event");
        } else {
            activeContext.setText(className + "  \u00b7  " + eventName);
            if (remaining == 0 && !students.isEmpty()) {
                result.setText("Everyone has been drawn!");
            } else {
                result.setText("Ready for the next draw");
            }
        }
        updateDrawButton();
        roster.revalidate();
        roster.repaint();
    }

    private JPanel studentRow(String name, boolean alreadyDrawn) {
        RoundedPanel row = new RoundedPanel(
                alreadyDrawn ? new Color(240, 249, 246) : new Color(248, 249, 252), 12);
        row.setLayout(new BorderLayout(12, 0));
        row.setBorder(BorderFactory.createEmptyBorder(10, 13, 10, 13));
        JLabel marker = new JLabel(alreadyDrawn ? "\u2713" : "\u2022", SwingConstants.CENTER);
        marker.setFont(new Font("SansSerif", Font.BOLD, alreadyDrawn ? 14 : 19));
        marker.setForeground(alreadyDrawn ? GREEN : new Color(160, 167, 183));
        marker.setPreferredSize(new Dimension(22, 22));
        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("SansSerif", alreadyDrawn ? Font.PLAIN : Font.BOLD, 13));
        nameLabel.setForeground(alreadyDrawn ? MUTED : TEXT);
        row.add(marker, BorderLayout.WEST);
        row.add(nameLabel, BorderLayout.CENTER);
        if (alreadyDrawn) {
            JLabel badge = new JLabel("DRAWN");
            badge.setFont(new Font("SansSerif", Font.BOLD, 9));
            badge.setForeground(GREEN);
            row.add(badge, BorderLayout.EAST);
        }
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private JPanel emptyState(String title, String detail) {
        JPanel state = new JPanel(new GridLayout(2, 1, 0, 5));
        state.setOpaque(false);
        state.setBorder(BorderFactory.createEmptyBorder(30, 8, 30, 8));
        JLabel heading = new JLabel(title, SwingConstants.CENTER);
        heading.setFont(new Font("SansSerif", Font.BOLD, 14));
        heading.setForeground(TEXT);
        JLabel message = new JLabel(detail, SwingConstants.CENTER);
        message.setFont(new Font("SansSerif", Font.PLAIN, 12));
        message.setForeground(MUTED);
        state.add(heading);
        state.add(message);
        state.setAlignmentX(Component.LEFT_ALIGNMENT);
        return state;
    }

    private void updateDrawButton() {
        String className = (String) classSelector.getSelectedItem();
        String eventName = (String) eventSelector.getSelectedItem();
        boolean canDraw = className != null && eventName != null
                && classes.containsKey(className)
                && events.containsKey(className)
                && events.get(className).containsKey(eventName);
        if (canDraw) {
            Set<String> drawn = new HashSet<String>(events.get(className).get(eventName));
            canDraw = false;
            for (String student : classes.get(className)) {
                if (!drawn.contains(student)) {
                    canDraw = true;
                    break;
                }
            }
        }
        drawButton.setEnabled(canDraw);
        drawButton.setToolTipText(canDraw
                ? "Draw a student who has not been drawn yet"
                : "Select a class with students and an event with available students");
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(null, message, "Attention", JOptionPane.WARNING_MESSAGE);
    }

    private void loadState() throws IOException {
        if (!Files.exists(DATA_FILE)) {
            return;
        }
        List<String> lines = Files.readAllLines(DATA_FILE, StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isEmpty()) {
                continue;
            }
            String[] fields = line.split("\t", -1);
            try {
                if ("STUDENT".equals(fields[0]) && fields.length == 3) {
                    String className = decode(fields[1]);
                    String student = decode(fields[2]);
                    if (className.isEmpty() || student.isEmpty()) {
                        throw new IllegalArgumentException("Class name or student name is empty.");
                    }
                    if (!classes.containsKey(className)) {
                        classes.put(className, new ArrayList<String>());
                    }
                    classes.get(className).add(student);
                } else if ("EVENT".equals(fields[0]) && fields.length == 3) {
                    String className = decode(fields[1]);
                    String eventName = decode(fields[2]);
                    if (className.isEmpty() || eventName.isEmpty()) {
                        throw new IllegalArgumentException("Class name or event name is empty.");
                    }
                    getOrCreateEvent(className, eventName);
                } else if ("DRAWN".equals(fields[0]) && fields.length == 4) {
                    String className = decode(fields[1]);
                    String eventName = decode(fields[2]);
                    String student = decode(fields[3]);
                    if (className.isEmpty() || eventName.isEmpty() || student.isEmpty()) {
                        throw new IllegalArgumentException("Class, event, or student name is empty.");
                    }
                    getOrCreateEvent(className, eventName).add(student);
                } else {
                    throw new IllegalArgumentException("Unrecognized data format.");
                }
            } catch (IllegalArgumentException exception) {
                throw new IOException("Error on line " + (i + 1) + ": " + exception.getMessage(),
                        exception);
            }
        }
    }

    private List<String> getOrCreateEvent(String className, String eventName) {
        if (!classes.containsKey(className)) {
            classes.put(className, new ArrayList<String>());
        }
        Map<String, List<String>> classEvents = events.get(className);
        if (classEvents == null) {
            classEvents = new LinkedHashMap<String, List<String>>();
            events.put(className, classEvents);
        }
        List<String> drawnStudents = classEvents.get(eventName);
        if (drawnStudents == null) {
            drawnStudents = new ArrayList<String>();
            classEvents.put(eventName, drawnStudents);
        }
        return drawnStudents;
    }

    private boolean saveAfterChange() {
        try {
            saveState();
            return true;
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(null,
                    "The change will remain active while the app is open, but could not be saved to "
                            + DATA_FILE.toAbsolutePath() + ":\n" + exception.getMessage(),
                    "Save error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void saveState() throws IOException {
        List<String> lines = new ArrayList<String>();
        for (Map.Entry<String, List<String>> classEntry : classes.entrySet()) {
            for (String student : classEntry.getValue()) {
                lines.add("STUDENT\t" + encode(classEntry.getKey()) + "\t" + encode(student));
            }
        }
        for (Map.Entry<String, Map<String, List<String>>> classEntry : events.entrySet()) {
            for (Map.Entry<String, List<String>> eventEntry : classEntry.getValue().entrySet()) {
                lines.add("EVENT\t" + encode(classEntry.getKey()) + "\t" + encode(eventEntry.getKey()));
                for (String student : eventEntry.getValue()) {
                    lines.add("DRAWN\t" + encode(classEntry.getKey()) + "\t"
                            + encode(eventEntry.getKey()) + "\t" + encode(student));
                }
            }
        }
        Path temporaryFile = DATA_FILE.resolveSibling(DATA_FILE.getFileName().toString() + ".tmp");
        Files.write(temporaryFile, lines, StandardCharsets.UTF_8);
        try {
            Files.move(temporaryFile, DATA_FILE,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryFile, DATA_FILE, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static class RoundedPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        private final Color fill;
        private final int radius;

        RoundedPanel(Color fill, int radius) {
            this.fill = fill;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(fill);
            g.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class RoundedButton extends JButton {
        private static final long serialVersionUID = 1L;

        private final Color baseColor;
        private final Color textColor;

        RoundedButton(String text, Color background, Color foreground) {
            super(text);
            baseColor = background;
            textColor = foreground;
            setForeground(foreground);
            setFont(new Font("SansSerif", Font.BOLD, 13));
            setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
            setFocusPainted(false);
            setFocusable(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = baseColor;
            if (!isEnabled()) {
                fill = new Color(221, 222, 234);
            } else if (getModel().isPressed()) {
                fill = ACCENT_DARK;
            } else if (getModel().isRollover() && baseColor.equals(ACCENT)) {
                fill = ACCENT_DARK;
            } else if (getModel().isRollover()) {
                fill = new Color(239, 241, 247);
            }
            setForeground(isEnabled() ? textColor : new Color(117, 119, 140));
            g.setColor(fill);
            g.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
