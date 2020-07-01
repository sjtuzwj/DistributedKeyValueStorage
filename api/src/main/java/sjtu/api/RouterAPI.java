package sjtu.api;

import org.apache.dubbo.config.annotation.DubboReference;

import java.util.concurrent.ConcurrentHashMap;

public interface RouterAPI {

    ConcurrentHashMap<String,String> SYNC();
    Boolean CONTAIN(String key);
    String GET(String key);
    Boolean PUT(String key,String value);
}
