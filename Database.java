import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Database implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<DatabaseColumn> columns;
    private List<Map<String, Object>> records;
    private String primaryKey;

    private transient Map<Object, Integer> primaryKeyIndex;
    private transient Map<String, Map<Object, Set<Integer>>> fieldIndexes;
    private transient Map<String, Map<String, Set<Integer>>> textPartialIndex;

    public Database() {
        this.columns = new ArrayList<>();
        this.records = new ArrayList<>();
        initializeIndexes();
    }

    private void initializeIndexes() {
        this.primaryKeyIndex = new ConcurrentHashMap<>();
        this.fieldIndexes = new ConcurrentHashMap<>();
        this.textPartialIndex = new ConcurrentHashMap<>();
    }

    private void rebuildIndexes() {
        primaryKeyIndex.clear();
        fieldIndexes.clear();
        textPartialIndex.clear();

        for (int i = 0; i < records.size(); i++) {
            Map<String, Object> record = records.get(i);
            indexRecord(record, i);
        }
    }

    //индексируем все поля
    private void indexRecord(Map<String, Object> record, int index) {
        if (primaryKey != null) {
            Object keyValue = record.get(primaryKey);
            if (keyValue != null) {
                primaryKeyIndex.put(keyValue, index);
            }
        }

        for (DatabaseColumn column : columns) {
            String fieldName = column.getName();
            Object value = record.get(fieldName);

            if (value != null) {
                fieldIndexes
                        .computeIfAbsent(fieldName, k -> new ConcurrentHashMap<>())
                        .computeIfAbsent(value, k -> ConcurrentHashMap.newKeySet())
                        .add(index);

                if (value instanceof String) {
                    String text = ((String) value).toLowerCase();
                    String[] words = text.split("\\s+");
                    for (String word : words) {
                        if (word.length() > 2) {
                            textPartialIndex
                                    .computeIfAbsent(fieldName, k -> new ConcurrentHashMap<>())
                                    .computeIfAbsent(word, k -> ConcurrentHashMap.newKeySet())
                                    .add(index);
                        }
                    }
                }
            }
        }
    }

    //удалённую запись удаляем и из индексов
    private void removeRecordFromIndexes(Map<String, Object> record, int index) {
        if (primaryKey != null) {
            Object keyValue = record.get(primaryKey);
            if (keyValue != null) {
                primaryKeyIndex.remove(keyValue);
            }
        }

        for (DatabaseColumn column : columns) {
            String fieldName = column.getName();
            Object value = record.get(fieldName);

            if (value != null) {
                Map<Object, Set<Integer>> fieldIndex = fieldIndexes.get(fieldName);
                if (fieldIndex != null) {
                    Set<Integer> indices = fieldIndex.get(value);
                    if (indices != null) {
                        indices.remove(index);
                        if (indices.isEmpty()) {
                            fieldIndex.remove(value);
                        }
                    }
                }

                if (value instanceof String) {
                    String text = ((String) value).toLowerCase();
                    String[] words = text.split("\\s+");
                    Map<String, Set<Integer>> partialIndex = textPartialIndex.get(fieldName);
                    if (partialIndex != null) {
                        for (String word : words) {
                            if (word.length() > 2) {
                                Set<Integer> wordIndices = partialIndex.get(word);
                                if (wordIndices != null) {
                                    wordIndices.remove(index);
                                    if (wordIndices.isEmpty()) {
                                        partialIndex.remove(word);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    //обновляем после удаления записи
    private void updateIndexesAfterDeletion(List<Integer> removedIndices) {
        if (removedIndices.isEmpty()) return;

        removedIndices.sort(Collections.reverseOrder());

        for (String fieldName : fieldIndexes.keySet()) {
            Map<Object, Set<Integer>> fieldIndex = fieldIndexes.get(fieldName);
            for (Set<Integer> indices : fieldIndex.values()) {
                Set<Integer> updatedIndices = ConcurrentHashMap.newKeySet();
                for (int index : indices) {
                    int updatedIndex = index;
                    for (int removedIndex : removedIndices) {
                        if (updatedIndex > removedIndex) {
                            updatedIndex--;
                        }
                    }
                    updatedIndices.add(updatedIndex);
                }
                indices.clear();
                indices.addAll(updatedIndices);
            }
        }

        for (String fieldName : textPartialIndex.keySet()) {
            Map<String, Set<Integer>> partialIndex = textPartialIndex.get(fieldName);
            for (Set<Integer> indices : partialIndex.values()) {
                Set<Integer> updatedIndices = ConcurrentHashMap.newKeySet();
                for (int index : indices) {
                    int updatedIndex = index;
                    for (int removedIndex : removedIndices) {
                        if (updatedIndex > removedIndex) {
                            updatedIndex--;
                        }
                    }
                    updatedIndices.add(updatedIndex);
                }
                indices.clear();
                indices.addAll(updatedIndices);
            }
        }

        if (primaryKey != null) {
            primaryKeyIndex.clear();
            for (int i = 0; i < records.size(); i++) {
                Map<String, Object> record = records.get(i);
                Object keyValue = record.get(primaryKey);
                if (keyValue != null) {
                    primaryKeyIndex.put(keyValue, i);
                }
            }
        }
    }

    public void addColumn(String name, Class<?> type, boolean isPrimaryKey) {
        if (isPrimaryKey && primaryKey != null) {
            throw new IllegalArgumentException("Первичный ключ уже существует");
        }

        DatabaseColumn column = new DatabaseColumn(name, type);
        columns.add(column);

        if (isPrimaryKey) {
            primaryKey = name;
            rebuildIndexes();
        }
    }

    //добавление записи
    public boolean addRecord(Map<String, Object> record) {
        if (primaryKey != null) {
            Object keyValue = record.get(primaryKey);
            if (keyValue == null) {
                return false;
            }

            //проверка уникальности
            if (primaryKeyIndex.containsKey(keyValue)) {
                return false;
            }
        }

        records.add(new HashMap<>(record));
        int newIndex = records.size() - 1;
        indexRecord(record, newIndex);
        return true;
    }

    //удаление записи (по ключевому и не ключевому значению)
    public int removeRecords(String fieldName, Object value, boolean partialMatch) {
        Set<Integer> indicesToRemove = findRecordIndices(fieldName, value, partialMatch);

        if (indicesToRemove.isEmpty()) {
            return 0;
        }

        List<Integer> removedIndices = new ArrayList<>(indicesToRemove);

        removedIndices.sort(Collections.reverseOrder());

        for (int index : removedIndices) {
            Map<String, Object> record = records.get(index);
            removeRecordFromIndexes(record, index);
            records.remove(index);
        }

        updateIndexesAfterDeletion(removedIndices);

        if (!removedIndices.isEmpty() && shouldReorderKeys(fieldName)) {
            reorderPrimaryKeys();
        }

        return removedIndices.size();
    }

    //поиск записей
    private Set<Integer> findRecordIndices(String fieldName, Object value, boolean partialMatch) {
        Set<Integer> result = ConcurrentHashMap.newKeySet();

        //по всем полям
        if ("ANY_FIELD".equals(fieldName)) {
            for (DatabaseColumn column : columns) {
                result.addAll(findRecordIndices(column.getName(), value, partialMatch));
            }
        }
        //по первичному ключу
        else if (primaryKey != null && fieldName.equals(primaryKey) && !partialMatch) {
            Integer index = primaryKeyIndex.get(value);
            if (index != null && index >= 0 && index < records.size()) {
                result.add(index);
            }
        }
        //по частичному тексту
        else if (partialMatch && value instanceof String) {
            String searchText = ((String) value).toLowerCase();
            Map<String, Set<Integer>> partialIndex = textPartialIndex.get(fieldName);
            if (partialIndex != null) {
                for (Map.Entry<String, Set<Integer>> entry : partialIndex.entrySet()) {
                    if (entry.getKey().contains(searchText)) {
                        for (int index : entry.getValue()) {
                            if (index >= 0 && index < records.size()) {
                                result.add(index);
                            }
                        }
                    }
                }
            }
        }
        //точный поиск по полю
        else {
            Map<Object, Set<Integer>> fieldIndex = fieldIndexes.get(fieldName);
            if (fieldIndex != null) {
                Set<Integer> indices = fieldIndex.get(value);
                if (indices != null) {
                    for (int index : indices) {
                        if (index >= 0 && index < records.size()) {
                            result.add(index);
                        }
                    }
                }
            }
        }

        return result;
    }

    public List<Map<String, Object>> search(String fieldName, Object value, boolean partialMatch) {
        Set<Integer> indices = findRecordIndices(fieldName, value, partialMatch);
        List<Map<String, Object>> results = new ArrayList<>(indices.size());

        for (int index : indices) {
            if (index >= 0 && index < records.size()) {
                results.add(new HashMap<>(records.get(index)));
            }
        }

        return results;
    }

    public List<Map<String, Object>> search(String field, Object value) {
        return search(field, value, false);
    }

    private boolean shouldReorderKeys(String fieldName) {
        return primaryKey != null &&
                (fieldName.equals(primaryKey) || isNumericPrimaryKey());
    }

    private void reorderPrimaryKeys() {
        if (primaryKey == null) return;

        DatabaseColumn pkColumn = getPrimaryKeyColumn();
        if (pkColumn == null || (!pkColumn.getType().equals(Integer.class) &&
                !pkColumn.getType().equals(Double.class))) {
            return;
        }

        records.sort((r1, r2) -> {
            Object v1 = r1.get(primaryKey);
            Object v2 = r2.get(primaryKey);

            if (v1 instanceof Integer && v2 instanceof Integer) {
                return Integer.compare((Integer) v1, (Integer) v2);
            } else if (v1 instanceof Double && v2 instanceof Double) {
                return Double.compare((Double) v1, (Double) v2);
            }
            return 0;
        });

        rebuildIndexes();

        for (int i = 0; i < records.size(); i++) {
            Map<String, Object> record = records.get(i);
            if (pkColumn.getType().equals(Integer.class)) {
                record.put(primaryKey, i + 1);
            } else if (pkColumn.getType().equals(Double.class)) {
                record.put(primaryKey, (double) (i + 1));
            }
        }

        rebuildIndexes();
    }

    private boolean isNumericPrimaryKey() {
        if (primaryKey == null) return false;
        DatabaseColumn pkColumn = getPrimaryKeyColumn();
        return pkColumn != null &&
                (pkColumn.getType().equals(Integer.class) || pkColumn.getType().equals(Double.class));
    }

    private DatabaseColumn getPrimaryKeyColumn() {
        for (DatabaseColumn column : columns) {
            if (column.getName().equals(primaryKey)) {
                return column;
            }
        }
        return null;
    }

    public boolean updateRecord(Object keyValue, Map<String, Object> newData) {
        if (primaryKey == null) return false;

        Integer index = primaryKeyIndex.get(keyValue);
        if (index == null || index < 0 || index >= records.size()) {
            return false;
        }

        Map<String, Object> record = records.get(index);

        removeRecordFromIndexes(record, index);

        record.putAll(newData);

        indexRecord(record, index);

        return true;
    }

    //очистка бд
    public void clear() {
        records.clear();
        initializeIndexes();
    }

    public List<DatabaseColumn> getColumns() { return columns; }
    public List<Map<String, Object>> getRecords() { return records; }
    public String getPrimaryKey() { return primaryKey; }
    public int getRecordCount() { return records.size(); }

    public void saveToFile(File file) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(this);
        }
    }

    public static Database loadFromFile(File file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (Database) ois.readObject();
        }
    }
}