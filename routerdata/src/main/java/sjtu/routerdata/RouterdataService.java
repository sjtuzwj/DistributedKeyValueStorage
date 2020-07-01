package sjtu.routerdata;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;

import org.springframework.beans.factory.annotation.Value;
import sjtu.api.DataAPI;
import sjtu.api.RouterAPI;
import sjtu.api.RouterDataAPI;
import sjtu.api.SlaveAPI;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@DubboService(version = "${demo.service.version}")
public class RouterdataService implements RouterDataAPI {

    /**
     * The default value of ${dubbo.application.name} is ${spring.application.name}
     */
    @Value("${dubbo.application.name}")
    private String serviceName;
    @Value("${group.hotfix}")
    private String hotfix;

    @DubboReference(version = "${demo.service.version}",check=false)
    private RouterAPI router;

    private  ConcurrentHashMap<String, String> m = new ConcurrentHashMap<>();
    private  ConcurrentHashMap<String, String> cas;

    void Check(){
        if(hotfix.equals("1")) {
            System.out.println("SYNC ");
            m = router.SYNC();
            System.out.println(m.toString());
            System.out.println("SYNCED ");
            hotfix = "0";
        }
    }

    @Override
    public Boolean COMMIT() {
        m = cas;
        return true;
}

    @Override
    public Boolean PUT(String key, String val) {
        Check();
        System.out.println("PUT " + key);
        cas = m;
        cas.put(key, val);
        return true;
    }

    @Override
    public String GET(String key) {
        System.out.println("READ " + key);
        return m.get(key);
    }

    @Override
    public  ConcurrentHashMap<String,String> SYNC(){
        System.out.println("SEND SYNC ");
        return m;
    }

    @Override
    public Boolean CONTAIN(String key) {
        System.out.println("CONTAIN " + key);
        return m.containsKey(key);
    }
}
