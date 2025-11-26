import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.*;
import java.util.List;

public class DatabaseGUI extends JFrame {
    private Database database;
    private File currentFile;
    private String databaseName;

    //цвета
    private final Color BACKGROUND = Color.decode("#B8B3B1");
    private final Color ACCENT = Color.decode("#716361");
    private final Color TEXT = Color.decode("#362F32");
    private final Color DANGER_BUTTON = Color.decode("#5D4459");
    private final Color PRIMARY_BUTTON = Color.decode("#586144");

    //компоненты
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> searchFieldComboBox;
    private JLabel statusLabel;
    private JLabel databaseNameLabel;
    private JButton showAllButton;

    private boolean isSearchMode = false;

    public DatabaseGUI() {
        initializeGUI();
        database = new Database();
        databaseName = "База не создана";
        updateTable();
        updateDatabaseNameDisplay();
        updateSearchFields();
    }

    //инициализация всех панелей
    private void initializeGUI() {
        setTitle("Файловая База Данных");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 850);
        setLocationRelativeTo(null);

        //главная
        JPanel mainPanel = getJPanel(BACKGROUND, ACCENT);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        databaseNameLabel = new JLabel(databaseName, SwingConstants.CENTER);
        databaseNameLabel.setFont(new Font("Georgia", Font.BOLD, 16));
        databaseNameLabel.setForeground(TEXT);
        databaseNameLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        topPanel.add(databaseNameLabel, BorderLayout.NORTH);

        //поиск
        JPanel searchPanel = createSearchPanel();
        topPanel.add(searchPanel, BorderLayout.CENTER);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        //таблица
        table = new JTable();
        tableModel = new DefaultTableModel();
        table.setModel(tableModel);
        table.setFont(new Font("Georgia", Font.PLAIN, 12));
        table.setForeground(TEXT);
        table.setBackground(Color.WHITE);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        //панель кнопок для работы с бд
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.WEST);

        //статус бд
        statusLabel = new JLabel("База данных не загружена");
        statusLabel.setFont(new Font("Georgia", Font.PLAIN, 12));
        statusLabel.setForeground(TEXT);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private static JPanel getJPanel(Color BACKGROUND, Color ACCENT) {
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gradient = new GradientPaint(
                        0, 0, BACKGROUND,
                        getWidth(), getHeight(), ACCENT
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setLayout(new BorderLayout(15, 15));
        return mainPanel;
    }

    //панель поиска
    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panel.setOpaque(false);

        JLabel searchLabel = new JLabel("Поиск по:");
        searchLabel.setFont(new Font("Georgia", Font.BOLD, 14));
        searchLabel.setForeground(TEXT);

        searchFieldComboBox = new JComboBox<>();
        searchFieldComboBox.setFont(new Font("Georgia", Font.PLAIN, 12));
        searchFieldComboBox.setPreferredSize(new Dimension(120, 30));

        searchField = new JTextField(15);
        searchField.setFont(new Font("Georgia", Font.PLAIN, 12));
        searchField.setPreferredSize(new Dimension(150, 30));

        JButton searchButton = createButton("Найти", DANGER_BUTTON);
        searchButton.addActionListener(this::searchRecords);

        showAllButton = createButton("Показать все", PRIMARY_BUTTON);
        showAllButton.addActionListener(e -> showAllRecords());
        showAllButton.setVisible(false);

        panel.add(searchLabel);
        panel.add(searchFieldComboBox);
        panel.add(searchField);
        panel.add(searchButton);
        panel.add(showAllButton);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(createButton("Создать БД", PRIMARY_BUTTON, this::createDatabase));
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(createButton("Открыть БД", PRIMARY_BUTTON, this::openDatabase));
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(createButton("Сохранить", PRIMARY_BUTTON, this::saveDatabase));
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(createButton("Импорт БД", PRIMARY_BUTTON, this::importDatabase));
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(createButton("Создать Backup", PRIMARY_BUTTON, this::createBackup));
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(createButton("Восстановить", PRIMARY_BUTTON, this::restoreBackup));

        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        panel.add(createButton("Добавить запись", PRIMARY_BUTTON, this::addRecord));
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(createButton("Изменить запись", PRIMARY_BUTTON, this::editRecord));
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(createButton("Удалить запись", DANGER_BUTTON, this::deleteRecord));

        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        panel.add(createButton("Очистить БД", DANGER_BUTTON, this::clearDatabase));
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(createButton("Удалить БД", DANGER_BUTTON, this::deleteDatabase));

        return panel;
    }

    private JButton createButton(String text, Color color) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

                super.paintComponent(g);
            }
        };

        button.setFont(new Font("Georgia", Font.BOLD, 12));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(180, 45));
        button.setMinimumSize(new Dimension(180, 45));
        button.setMaximumSize(new Dimension(180, 45));

        return button;
    }

    private JButton createButton(String text, Color color, java.awt.event.ActionListener listener) {
        JButton button = createButton(text, color);
        button.addActionListener(listener);
        return button;
    }

    //обновление при поиске
    private void updateSearchFields() {
        searchFieldComboBox.removeAllItems();

        if (database.getColumns().isEmpty()) {
            searchFieldComboBox.addItem("Нет полей");
            searchFieldComboBox.setEnabled(false);
            searchField.setEnabled(false);
        } else {
            searchFieldComboBox.setEnabled(true);
            searchField.setEnabled(true);

            searchFieldComboBox.addItem("Все поля");

            for (DatabaseColumn column : database.getColumns()) {
                searchFieldComboBox.addItem(column.getName());
            }
        }
    }

    //поиск записей
    private void searchRecords(ActionEvent e) {
        if (database.getColumns().isEmpty()) {
            JOptionPane.showMessageDialog(this, "База данных не загружена");
            return;
        }

        String searchValue = searchField.getText().trim();
        if (searchValue.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Введите значение для поиска");
            return;
        }

        String selectedField = (String) searchFieldComboBox.getSelectedItem();
        if (selectedField == null || selectedField.equals("Нет полей")) {
            return;
        }

        List<Map<String, Object>> searchResults;

        if (selectedField.equals("Все поля")) {
            searchResults = database.search("ANY_FIELD", searchValue, true);
        } else {
            Class<?> fieldType = getColumnType(selectedField);
            boolean partialMatch = fieldType.equals(String.class);

            try {
                Object convertedValue = partialMatch ? searchValue : convertValue(searchValue, fieldType);
                searchResults = database.search(selectedField, convertedValue, partialMatch);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Неверный формат значения для поля '" + selectedField +
                                "'. Ожидается: " + getTypeName(fieldType));
                return;
            }
        }

        DefaultTableModel searchModel = new DefaultTableModel();

        for (DatabaseColumn column : database.getColumns()) {
            searchModel.addColumn(column.getName());
        }

        for (Map<String, Object> record : searchResults) {
            addRecordToTable(searchModel, record);
        }

        table.setModel(searchModel);
        isSearchMode = true;
        showAllButton.setVisible(true);

        updateStatus("Найдено записей: " + searchResults.size() + " (поиск по '" + selectedField + "')");
    }

    private void addRecordToTable(DefaultTableModel model, Map<String, Object> record) {
        Object[] rowData = new Object[database.getColumns().size()];
        for (int i = 0; i < database.getColumns().size(); i++) {
            rowData[i] = record.get(database.getColumns().get(i).getName());
        }
        model.addRow(rowData);
    }

    private void showAllRecords() {
        updateTable();
        isSearchMode = false;
        showAllButton.setVisible(false);
        searchField.setText("");
        updateStatus("Показаны все записи: " + database.getRecordCount() + " записей");
    }

    //создание бд
    private void createDatabase(ActionEvent e) {
        try {
            String name = JOptionPane.showInputDialog(this, "Введите название базы данных:");
            if (name == null || name.trim().isEmpty()) return;

            database = new Database();
            currentFile = null;
            databaseName = name;

            while (true) {
                String columnName = JOptionPane.showInputDialog(this,
                        "Введите название колонки (или Отмена для завершения):");
                if (columnName == null) break;

                if (columnName.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Название колонки не может быть пустым");
                    continue;
                }

                String[] types = {"Текст", "Целое число", "Дробное число", "Логическое"};
                String type = (String) JOptionPane.showInputDialog(this,
                        "Выберите тип колонки:", "Тип колонки",
                        JOptionPane.QUESTION_MESSAGE, null, types, types[0]);

                if (type == null) break;

                Class<?> columnType = switch (type) {
                    case "Целое число" -> Integer.class;
                    case "Дробное число" -> Double.class;
                    case "Логическое" -> Boolean.class;
                    default -> String.class;
                };

                boolean isPrimaryKey = database.getPrimaryKey() == null &&
                        JOptionPane.showConfirmDialog(this,
                                "Сделать первичным ключом?", "Первичный ключ",
                                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;

                database.addColumn(columnName, columnType, isPrimaryKey);

                if (isPrimaryKey) {
                    JOptionPane.showMessageDialog(this,
                            "Колонка '" + columnName + "' установлена как первичный ключ");
                }
            }

            if (database.getColumns().isEmpty()) {
                JOptionPane.showMessageDialog(this, "База данных должна иметь хотя бы одну колонку");
                databaseName = "База не создана";
                return;
            }

            updateTable();
            updateSearchFields();
            updateDatabaseNameDisplay();
            showAllButton.setVisible(false);
            isSearchMode = false;
            updateStatus("База данных '" + databaseName + "' создана. Добавьте первую запись.");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ошибка создания базы данных: " + ex.getMessage());
        }
    }

    //открытие бд
    private void openDatabase(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Файлы базы данных (*.fdb)", "fdb"));
        fileChooser.setDialogTitle("Открыть базу данных");

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                database = Database.loadFromFile(fileChooser.getSelectedFile());
                currentFile = fileChooser.getSelectedFile();
                databaseName = currentFile.getName().replace(".fdb", "");
                updateTable();
                updateSearchFields();
                updateDatabaseNameDisplay();
                showAllButton.setVisible(false);
                isSearchMode = false;
                updateStatus("База данных загружена: " + currentFile.getName() +
                        " (" + database.getRecordCount() + " записей)");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Ошибка открытия базы данных: " + ex.getMessage());
            }
        }
    }

    //сохранение
    private void saveDatabase(ActionEvent e) {
        if (database.getColumns().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Нет данных для сохранения");
            return;
        }

        if (currentFile == null) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Файлы базы данных (*.fdb)", "fdb"));
            fileChooser.setDialogTitle("Сохранить базу данных");
            fileChooser.setSelectedFile(new File(databaseName + ".fdb"));

            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                currentFile = fileChooser.getSelectedFile();
                if (!currentFile.getName().toLowerCase().endsWith(".fdb")) {
                    currentFile = new File(currentFile.getAbsolutePath() + ".fdb");
                }
                databaseName = currentFile.getName().replace(".fdb", "");
            } else {
                return;
            }
        }

        try {
            database.saveToFile(currentFile);
            updateDatabaseNameDisplay();
            updateStatus("База данных сохранена: " + currentFile.getName() +
                    " (" + database.getRecordCount() + " записей)");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ошибка сохранения базы данных: " + ex.getMessage());
        }
    }

    private void saveAfterOperation() {
        if (currentFile != null) {
            try {
                database.saveToFile(currentFile);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка сохранения изменений: " + ex.getMessage());
            }
        }
    }

    //импорт
    private void importDatabase(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Файлы баз данных (*.fdb, *.dat, *.db, *.csv)", "fdb", "dat", "db", "csv"));
        fileChooser.setDialogTitle("Импорт базы данных");

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String fileName = selectedFile.getName().toLowerCase();

            try {
                if (fileName.endsWith(".csv")) {
                    importFromCSV(selectedFile);
                } else {
                    importFromDatabaseFile(selectedFile);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Ошибка импорта: " + ex.getMessage() +
                                "\nУбедитесь, что файл имеет корректный формат",
                        "Ошибка импорта", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    //импорт из формата CSV
    private void importFromCSV(File csvFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line;
            List<String> headers = new ArrayList<>();
            List<Map<String, String>> csvData = new ArrayList<>();
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;

                List<String> row = parseCSVLine(line);

                if (headers.isEmpty()) {
                    headers = row;
                    if (headers.isEmpty()) {
                        throw new IOException("CSV файл не содержит заголовков");
                    }
                } else {
                    if (row.size() != headers.size()) {
                        System.out.println("Предупреждение: строка " + lineNumber + " имеет " +
                                row.size() + " колонок вместо " + headers.size());
                        while (row.size() < headers.size()) {
                            row.add("");
                        }
                        if (row.size() > headers.size()) {
                            row = row.subList(0, headers.size());
                        }
                    }

                    Map<String, String> record = new HashMap<>();
                    for (int i = 0; i < headers.size(); i++) {
                        record.put(headers.get(i), row.get(i));
                    }
                    csvData.add(record);
                }
            }

            if (headers.isEmpty()) {
                throw new IOException("CSV файл пуст или не содержит данных");
            }

            int result = showCSVImportDialog(headers, csvData.size());
            if (result == JOptionPane.CANCEL_OPTION) return;

            if (database.getColumns().isEmpty() || result == JOptionPane.YES_OPTION) {
                database = new Database();
                databaseName = "Импорт из " + csvFile.getName().replace(".csv", "");

                Map<String, Class<?>> columnTypes = detectColumnTypes(headers, csvData);

                String primaryKey = selectPrimaryKey(headers);

                for (String header : headers) {
                    Class<?> type = columnTypes.get(header);
                    boolean isPK = header.equals(primaryKey);
                    database.addColumn(header, type, isPK);
                }

                importCSVData(csvData, headers, columnTypes);

            } else {
                if (!validateCSVForExistingDB(headers)) {
                    return;
                }
                importCSVData(csvData, headers, null);
            }

            updateTable();
            updateSearchFields();
            updateDatabaseNameDisplay();
            showAllButton.setVisible(false);
            isSearchMode = false;
            updateStatus("Импорт из CSV завершен: " + csvFile.getName() +
                    " (" + csvData.size() + " записей)");

        } catch (Exception ex) {
            throw new RuntimeException("Ошибка чтения CSV файла: " + ex.getMessage(), ex);
        }
    }

    private List<String> parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        result.add(current.toString().trim());

        for (int i = 0; i < result.size(); i++) {
            String field = result.get(i);
            if (field.startsWith("\"") && field.endsWith("\"")) {
                result.set(i, field.substring(1, field.length() - 1));
            }
        }

        return result;
    }

    private Map<String, Class<?>> detectColumnTypes(List<String> headers, List<Map<String, String>> data) {
        Map<String, Class<?>> types = new HashMap<>();

        for (String header : headers) {
            Class<?> type = String.class;

            int samples = Math.min(10, data.size());
            boolean allIntegers = samples > 0;
            boolean allDoubles = samples > 0;
            boolean allBooleans = samples > 0;

            for (int i = 0; i < samples; i++) {
                String value = data.get(i).get(header);
                if (value == null || value.trim().isEmpty()) continue;

                try {
                    Integer.parseInt(value.trim());
                } catch (NumberFormatException e) {
                    allIntegers = false;
                }

                try {
                    Double.parseDouble(value.trim());
                } catch (NumberFormatException e) {
                    allDoubles = false;
                }

                String lowerValue = value.trim().toLowerCase();
                if (!lowerValue.equals("true") && !lowerValue.equals("false") &&
                        !lowerValue.equals("1") && !lowerValue.equals("0") &&
                        !lowerValue.equals("да") && !lowerValue.equals("нет")) {
                    allBooleans = false;
                }
            }

            if (allIntegers) {
                type = Integer.class;
            } else if (allDoubles) {
                type = Double.class;
            } else if (allBooleans) {
                type = Boolean.class;
            }

            types.put(header, type);
        }

        return types;
    }

    private int showCSVImportDialog(List<String> headers, int recordCount) {
        //если не существует изначальной бд и импортируем
        if (database.getColumns().isEmpty()) {
            String message = "Найдено в CSV:\n" +
                    "• Колонки: " + headers + "\n" +
                    "• Записей: " + recordCount + "\n\n" +
                    "Создать новую базу данных?";
            return JOptionPane.showConfirmDialog(this, message, "Импорт CSV",
                    JOptionPane.YES_NO_OPTION);
        }
        //если существует
        else {
            String message = "Найдено в CSV:\n" +
                    "• Колонки: " + headers + "\n" +
                    "• Записей: " + recordCount + "\n\n" +
                    "Текущая база данных:\n" +
                    "• Колонки: " + getColumnNames() + "\n" +
                    "• Записей: " + database.getRecordCount() + "\n\n" +
                    "Выберите действие:";

            Object[] options = {"Добавить к существующим", "Заменить данными из CSV", "Отмена"};
            int result = JOptionPane.showOptionDialog(this, message, "Импорт CSV",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, options, options[0]);

            if (result == 0) return JOptionPane.NO_OPTION;  //добавляем записи
            if (result == 1) return JOptionPane.YES_OPTION; //заменяем записи
            return JOptionPane.CANCEL_OPTION;
        }
    }

    private String selectPrimaryKey(List<String> headers) {
        if (headers.size() == 1) {
            return headers.getFirst();
        }

        String[] options = new String[headers.size() + 1];
        options[0] = "Без первичного ключа";
        for (int i = 0; i < headers.size(); i++) {
            options[i + 1] = headers.get(i);
        }

        String choice = (String) JOptionPane.showInputDialog(this,
                "Выберите колонку для первичного ключа:\n" +
                        "(должна содержать уникальные значения)",
                "Первичный ключ",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        return (choice != null && !choice.equals("Без первичного ключа")) ? choice : null;
    }

    private boolean validateCSVForExistingDB(List<String> csvHeaders) {
        List<String> dbHeaders = getColumnNames();

        if (!dbHeaders.equals(csvHeaders)) {
            String message = "Колонки в CSV не совпадают с текущей базой данных:\n" +
                    "CSV: " + csvHeaders + "\n" +
                    "БД: " + dbHeaders + "\n\n" +
                    "Продолжить? (Могут быть ошибки)";
            return JOptionPane.showConfirmDialog(this, message, "Несовпадение колонок",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
        }

        return true;
    }

    private void importCSVData(List<Map<String, String>> csvData, List<String> headers,
                               Map<String, Class<?>> columnTypes) {
        int successCount = 0;
        int errorCount = 0;
        StringBuilder errors = new StringBuilder();

        for (int i = 0; i < csvData.size(); i++) {
            Map<String, String> csvRecord = csvData.get(i);
            Map<String, Object> record = new HashMap<>();

            try {
                for (String header : headers) {
                    String value = csvRecord.get(header);
                    if (value == null) value = "";

                    Object convertedValue;
                    if (columnTypes != null) {
                        Class<?> type = columnTypes.get(header);
                        convertedValue = convertValue(value, type);
                    } else {
                        Class<?> type = getColumnType(header);
                        convertedValue = convertValue(value, type);
                    }

                    record.put(header, convertedValue);
                }

                if (database.addRecord(record)) {
                    successCount++;
                } else {
                    errorCount++;
                    errors.append("Строка ").append(i + 2).append(": Дублирование первичного ключа\n");
                }

            } catch (Exception e) {
                errorCount++;
                errors.append("Строка ").append(i + 2).append(": ").append(e.getMessage()).append("\n");
            }
        }

        if (errorCount > 0) {
            String message = "Импорт завершен:\n" +
                    "• Успешно: " + successCount + "\n" +
                    "• Ошибок: " + errorCount + "\n\n" +
                    "Ошибки:\n" + errors;
            JOptionPane.showMessageDialog(this, message, "Результаты импорта",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private List<String> getColumnNames() {
        List<String> names = new ArrayList<>();
        for (DatabaseColumn column : database.getColumns()) {
            names.add(column.getName());
        }
        return names;
    }

    private Class<?> getColumnType(String columnName) {
        for (DatabaseColumn column : database.getColumns()) {
            if (column.getName().equals(columnName)) {
                return column.getType();
            }
        }
        return String.class;
    }

    private void importFromDatabaseFile(File file) {
        //импорт обычных файлов
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            this.database = (Database) ois.readObject();
            currentFile = null;
            databaseName = "Импортированная БД (" + file.getName() + ")";
            updateTable();
            updateSearchFields();
            updateDatabaseNameDisplay();
            showAllButton.setVisible(false);
            isSearchMode = false;
            updateStatus("База данных импортирована: " + file.getName() +
                    " (" + database.getRecordCount() + " записей)");
        } catch (Exception ex) {
            throw new RuntimeException("Ошибка импорта базы данных: " + ex.getMessage(), ex);
        }
    }

    //создание backup
    private void createBackup(ActionEvent e) {
        if (database.getColumns().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Нет загруженной базы данных");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(databaseName + "_backup_" +
                System.currentTimeMillis() + ".fdb"));
        fileChooser.setDialogTitle("Создать резервную копию");

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(fileChooser.getSelectedFile()))) {
                oos.writeObject(database);
                updateStatus("Резервная копия создана: " + fileChooser.getSelectedFile().getName());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Ошибка создания резервной копии: " + ex.getMessage());
            }
        }
    }

    private void restoreBackup(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Резервные копии (*.fdb)", "fdb"));
        fileChooser.setDialogTitle("Восстановить из резервной копии");

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            int result = JOptionPane.showConfirmDialog(this,
                    "Текущие данные будут потеряны. Продолжить?",
                    "Восстановление из backup", JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.YES_OPTION) {
                try (ObjectInputStream ois = new ObjectInputStream(
                        new FileInputStream(fileChooser.getSelectedFile()))) {
                    database = (Database) ois.readObject();
                    currentFile = null;
                    databaseName = "Восстановленная БД (" + fileChooser.getSelectedFile().getName() + ")";
                    updateTable();
                    updateSearchFields();
                    updateDatabaseNameDisplay();
                    showAllButton.setVisible(false);
                    isSearchMode = false;
                    updateStatus("База данных восстановлена из резервной копии: " +
                            fileChooser.getSelectedFile().getName() + " (" + database.getRecordCount() + " записей)");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Ошибка восстановления: " + ex.getMessage());
                }
            }
        }
    }

    //добавление записи
    private void addRecord(ActionEvent e) {
        if (database.getColumns().isEmpty()) {
            JOptionPane.showMessageDialog(this, "База данных не загружена");
            return;
        }

        Map<String, Object> record = new HashMap<>();
        for (DatabaseColumn column : database.getColumns()) {
            String value = JOptionPane.showInputDialog(this,
                    "Введите значение для '" + column.getName() + "' (" +
                            getTypeName(column.getType()) + "):");
            if (value == null) return;

            try {
                Object convertedValue = convertValue(value, column.getType());
                record.put(column.getName(), convertedValue);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Неверное значение для '" + column.getName() + "'. Ожидается: " +
                                getTypeName(column.getType()));
                return;
            }
        }

        if (database.addRecord(record)) {
            saveAfterOperation();
            if (!isSearchMode) {
                updateTable();
            } else {
                updateStatus("Запись добавлена. Всего записей: " + database.getRecordCount() +
                        " (в режиме поиска - нажмите 'Показать все' чтобы увидеть все записи)");
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "Не удалось добавить запись. Первичный ключ должен быть уникальным.");
        }
    }

    private void editRecord(ActionEvent e) {
        if (database.getPrimaryKey() == null) {
            JOptionPane.showMessageDialog(this, "Первичный ключ не определен");
            return;
        }

        Class<?> primaryKeyType = null;
        for (DatabaseColumn column : database.getColumns()) {
            if (column.getName().equals(database.getPrimaryKey())) {
                primaryKeyType = column.getType();
                break;
            }
        }

        if (primaryKeyType == null) {
            JOptionPane.showMessageDialog(this, "Ошибка: не удалось определить тип первичного ключа");
            return;
        }

        JOptionPane.showMessageDialog(this,
                "Для изменения записи введите значение первичного ключа.\n" +
                        "Текущий первичный ключ: '" + database.getPrimaryKey() + "'\n" +
                        "Тип ключа: " + getTypeName(primaryKeyType) + "\n" +
                        "Это значение уникально идентифицирует запись.",
                "Изменение записи", JOptionPane.INFORMATION_MESSAGE);

        String keyValueInput = JOptionPane.showInputDialog(this,
                "Введите значение первичного ключа для изменения (" + getTypeName(primaryKeyType) + "):");
        if (keyValueInput == null || keyValueInput.trim().isEmpty()) return;

        Object keyValue;
        try {
            keyValue = convertValue(keyValueInput, primaryKeyType);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Неверный формат значения! Ожидается: " + getTypeName(primaryKeyType) +
                            "\nВведено: " + keyValueInput);
            return;
        }

        List<Map<String, Object>> records = database.search(database.getPrimaryKey(), keyValue);
        if (records.isEmpty()) {
            StringBuilder existingKeys = new StringBuilder("Существующие значения первичного ключа:\n");
            int count = 0;
            for (Map<String, Object> record : database.getRecords()) {
                Object existingKey = record.get(database.getPrimaryKey());
                if (existingKey != null) {
                    existingKeys.append("- ").append(existingKey).append("\n");
                    count++;
                    if (count >= 10) {
                        existingKeys.append("... и еще ").append(database.getRecordCount() - 10).append(" значений");
                        break;
                    }
                }
            }

            JOptionPane.showMessageDialog(this,
                    "Запись с первичным ключом '" + keyValue + "' не найдена.\n\n" +
                            existingKeys,
                    "Запись не найдена", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Map<String, Object> oldRecord = records.getFirst();
        Map<String, Object> newData = new HashMap<>();

        for (DatabaseColumn column : database.getColumns()) {
            if (column.getName().equals(database.getPrimaryKey())) {
                newData.put(column.getName(), oldRecord.get(column.getName()));
                continue;
            }

            Object currentValue = oldRecord.get(column.getName());
            String currentValueStr = (currentValue != null) ? currentValue.toString() : "";

            String newValue = JOptionPane.showInputDialog(this,
                    "Измените '" + column.getName() + "' (" + getTypeName(column.getType()) + "):",
                    currentValueStr);

            if (newValue == null) return;

            try {
                Object convertedValue = convertValue(newValue, column.getType());
                newData.put(column.getName(), convertedValue);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Неверное значение для '" + column.getName() + "'. Ожидается: " +
                                getTypeName(column.getType()) + "\nВведено: " + newValue);
                return;
            }
        }

        if (database.updateRecord(keyValue, newData)) {
            saveAfterOperation();
            if (!isSearchMode) {
                updateTable();
            } else {
                updateStatus("Запись изменена (в режиме поиска - нажмите 'Показать все' чтобы увидеть все записи)");
            }
            updateStatus("Запись с ключом '" + keyValue + "' успешно изменена");
        } else {
            JOptionPane.showMessageDialog(this, "Ошибка изменения записи");
        }
    }

    private void deleteRecord(ActionEvent e) {
        if (database.getColumns().isEmpty()) {
            JOptionPane.showMessageDialog(this, "База данных не загружена");
            return;
        }

        String[] options = {"Удалить по первичному ключу", "Удалить по значению в поле", "Удалить по значению в любом поле"};
        int choice = JOptionPane.showOptionDialog(this,
                "Выберите тип удаления:",
                "Удаление записей",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        if (choice == -1) return;

        int removedCount;
        String message = "";
        String fieldName = "";
        Object searchValue = null;
        boolean partialMatch = false;

        switch (choice) {
            case 0:
                if (database.getPrimaryKey() == null) {
                    JOptionPane.showMessageDialog(this, "Первичный ключ не определен");
                    return;
                }

                fieldName = database.getPrimaryKey();
                Class<?> primaryKeyType = getColumnType(fieldName);
                String keyValueInput = JOptionPane.showInputDialog(this,
                        "Введите значение первичного ключа для удаления (" + getTypeName(primaryKeyType) + "):");
                if (keyValueInput == null || keyValueInput.trim().isEmpty()) return;

                try {
                    searchValue = convertValue(keyValueInput, primaryKeyType);
                    message = "Запись с первичным ключом '" + searchValue + "' удалена";
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "Неверный формат значения! Ожидается: " + getTypeName(primaryKeyType));
                    return;
                }
                break;

            case 1: //по значению в конкретном поле
                String[] fieldOptions = new String[database.getColumns().size()];
                for (int i = 0; i < database.getColumns().size(); i++) {
                    fieldOptions[i] = database.getColumns().get(i).getName();
                }

                fieldName = (String) JOptionPane.showInputDialog(this,
                        "Выберите поле для поиска:",
                        "Выбор поля",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        fieldOptions,
                        fieldOptions[0]);

                if (fieldName == null) return;

                Class<?> fieldType = getColumnType(fieldName);
                String fieldValueInput = JOptionPane.showInputDialog(this,
                        "Введите значение для поиска в поле '" + fieldName + "' (" + getTypeName(fieldType) + "):\n" +
                                "(для текстовых полей можно ввести часть значения)");
                if (fieldValueInput == null || fieldValueInput.trim().isEmpty()) return;

                try {
                    searchValue = fieldType.equals(String.class) ? fieldValueInput : convertValue(fieldValueInput, fieldType);
                    partialMatch = fieldType.equals(String.class);
                    message = "Удалено записей: " + "?placeholder?" + " (по полю '" + fieldName + "')";
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "Неверный формат значения! Ожидается: " + getTypeName(fieldType));
                    return;
                }
                break;

            case 2: //по значению в любом поле
                fieldName = "ANY_FIELD";
                String anyValueInput = JOptionPane.showInputDialog(this,
                        "Введите значение для поиска в любом поле:\n" +
                                "(будут удалены все записи, содержащие это значение в любом поле)");
                if (anyValueInput == null || anyValueInput.trim().isEmpty()) return;

                searchValue = anyValueInput;
                partialMatch = true;
                message = "Удалено записей: " + "?placeholder?" + " (по значению '" + anyValueInput + "' в любом поле)";
                break;
        }

        removedCount = database.removeRecords(fieldName, searchValue, partialMatch);

        if (removedCount > 0) {
            saveAfterOperation();
            message = message.replace("?placeholder?", String.valueOf(removedCount));

            if (!isSearchMode) {
                updateTable();
            } else {
                updateStatus(message + " (в режиме поиска - нажмите 'Показать все' чтобы увидеть все записи)");
            }
            JOptionPane.showMessageDialog(this, message);
        } else {
            JOptionPane.showMessageDialog(this, "Записи с указанными параметрами не найдены");
        }
    }

    //очистка бд
    private void clearDatabase(ActionEvent e) {
        if (database.getColumns().isEmpty()) {
            JOptionPane.showMessageDialog(this, "База данных не загружена");
            return;
        }

        int result = JOptionPane.showConfirmDialog(this,
                "Вы уверены, что хотите очистить все записи?", "Очистка базы данных",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            database.clear();
            saveAfterOperation();
            updateTable();
            showAllButton.setVisible(false);
            isSearchMode = false;
            updateStatus("База данных очищена");
        }
    }

    //удаление бд
    private void deleteDatabase(ActionEvent e) {
        if (currentFile == null && database.getColumns().isEmpty()) {
            JOptionPane.showMessageDialog(this, "База данных не загружена");
            return;
        }

        int result = JOptionPane.showConfirmDialog(this,
                "Вы уверены, что хотите удалить базу данных?", "Удаление базы данных",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            if (currentFile != null && currentFile.exists()) {
                if (currentFile.delete()) {
                    updateStatus("Файл базы данных удален: " + currentFile.getName());
                } else {
                    JOptionPane.showMessageDialog(this, "Ошибка удаления файла базы данных");
                }
            }

            database = new Database();
            currentFile = null;
            databaseName = "База не создана";
            updateTable();
            updateSearchFields();
            updateDatabaseNameDisplay();
            showAllButton.setVisible(false);
            isSearchMode = false;
            updateStatus("База данных удалена из памяти");
        }
    }

    //обновления
    private void updateTable() {
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);

        if (database.getColumns().isEmpty()) return;

        for (DatabaseColumn column : database.getColumns()) {
            tableModel.addColumn(column.getName());
        }

        for (Map<String, Object> record : database.getRecords()) {
            Object[] rowData = new Object[database.getColumns().size()];
            for (int i = 0; i < database.getColumns().size(); i++) {
                rowData[i] = record.get(database.getColumns().get(i).getName());
            }
            tableModel.addRow(rowData);
        }

        table.setModel(tableModel);
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    private void updateDatabaseNameDisplay() {
        databaseNameLabel.setText("База данных: " + databaseName);
    }

    private Object convertValue(String value, Class<?> type) {
        if (type == Integer.class) {
            return Integer.parseInt(value.trim());
        } else if (type == Double.class) {
            return Double.parseDouble(value.trim());
        } else if (type == Boolean.class) {
            String lowerValue = value.trim().toLowerCase();
            if (lowerValue.equals("true") || lowerValue.equals("1") || lowerValue.equals("да") || lowerValue.equals("yes")) {
                return true;
            } else if (lowerValue.equals("false") || lowerValue.equals("0") || lowerValue.equals("нет") || lowerValue.equals("no")) {
                return false;
            } else {
                return Boolean.parseBoolean(value);
            }
        } else {
            return value;
        }
    }

    private String getTypeName(Class<?> type) {
        if (type == Integer.class) return "Целое число";
        if (type == Double.class) return "Дробное число";
        if (type == Boolean.class) return "Логическое (true/false/1/0/да/нет)";
        return "Текст";
    }
}