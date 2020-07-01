package sjtu.data;


import com.alibaba.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;

import org.apache.dubbo.rpc.RpcContext;
import org.springframework.beans.factory.annotation.Value;
import sjtu.api.DataAPI;
import sjtu.api.RouterAPI;
import sjtu.api.SlaveAPI;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@DubboService(version = "${demo.service.version}",group = "${group.id}")
public class DataService implements DataAPI {

    /**
     * The default value of ${dubbo.application.name} is ${spring.application.name}
     */
    @Value("${dubbo.application.name}")
    private String serviceName;
    @Value("${group.hotfix}")
    private String hotfix;
    @Value("${group.id}")
    private String groupid;
    @DubboReference(version = "${demo.service.version}",loadbalance = "IPLoadBalance",check=false)
    private SlaveAPI slave;

    private  ConcurrentHashMap<String, String> m = new ConcurrentHashMap<>();
    private  ConcurrentHashMap<String, String> cas;

    void Check(){
        if(hotfix.equals("1")) {
            String address = RpcContext.getContext().getAttachment("URL");
            RpcContext.getContext().setAttachment("URL",address);
            System.out.println("SYNC ");
            System.out.println(address);
            m = slave.SYNC();
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
    public String READ(String key) {
        System.out.println("READ " + key);
        return m.get(key);
    }

    @Override
    public  ConcurrentHashMap<String,String> SYNC(){
        System.out.println("SEND SYNC ");
        return m;
    }

    @Override
    public Boolean DELETE(String key) {
        Check();
        System.out.println("DEL " + key);
        if (!m.containsKey(key))
            return true;
        cas = m;
        cas.remove(key);
        return true;
    }
}
