package sjtu.api;

import java.util.concurrent.ConcurrentHashMap;

public interface DataAPI {

    ConcurrentHashMap<String,String> SYNC();
    Boolean COMMIT();
    Boolean PUT(String key, String val);
    String  READ(String key);
    Boolean DELETE(String key);
}

