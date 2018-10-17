package directory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserDirectory {

    private static Map<String, String> users = new ConcurrentHashMap<>();

    static {
        users.put("111", "张三");
        users.put("222", "李四");
        users.put("333", "王五");
    }

    public static Map<String, String> getUsers() {
        return users;
    }
}
