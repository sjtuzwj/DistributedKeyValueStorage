package sjtu.api;

import java.util.concurrent.ConcurrentHashMap;

public interface SlaveAPI{
    ConcurrentHashMap<String,String> SYNC();
    String sayHello(String name);
    Boolean PUT(String key, String val);
    String  READ(String key);
    Boolean DELETE(String key);
}
