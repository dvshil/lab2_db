//модель данных
import java.io.Serializable;

class DatabaseColumn implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private Class<?> type;

    public DatabaseColumn(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    public String getName() { return name; }
    public Class<?> getType() { return type; }
}