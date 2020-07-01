package sjtu.api;

import java.util.concurrent.ConcurrentHashMap;

public interface RouterDataAPI {
    ConcurrentHashMap<String,String> SYNC();
    Boolean COMMIT();
    Boolean CONTAIN(String key);
    String GET(String key);
    Boolean PUT(String key,String value);
}
