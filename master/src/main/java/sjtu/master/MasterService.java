package sjtu.master;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;

import org.apache.dubbo.rpc.RpcContext;
import org.springframework.beans.factory.annotation.Value;
import sjtu.api.MasterAPI;
import sjtu.api.SlaveAPI;
import sjtu.loadbalance.TOWLoadBalance;
import sjtu.lock.Lock;

@DubboService(version =  "${demo.service.version}")
public class MasterService implements MasterAPI {

    /**
     * The default value of ${dubbo.application.name} is ${spring.application.name}
     */
    @Value("${dubbo.application.name}")
    private String serviceName;

    @DubboReference(version = "${demo.service.version}",loadbalance = "TOWLoadBalance")
    private SlaveAPI slave;

    @Override
    public String sayHello(String name) {
        return String.format("[%s] : Hello, %s", serviceName, name);
    }


    @Override
    public Boolean PUT(String key, String val){
        Lock lock = new Lock("/rwlock"+key);
        lock.lockWrite();
        RpcContext.getContext().setAttachment("key", key);
        Boolean ret =  slave.PUT(key,val);
        System.out.println("PUT " + key);

        lock.unLockWrite();
        return ret;
    }

    @Override
    public String READ(String key){
        Lock lock = new Lock("/rwlock"+key);
        lock.lockRead();
        RpcContext.getContext().setAttachment("key", key);
        String ret =   slave.READ(key);
        System.out.println("READ " + key);

        lock.unLockRead();
        return ret;
    }

    @Override
    public Boolean DELETE(String key){
        Lock lock = new Lock("/rwlock"+key);
        lock.lockWrite();
        RpcContext.getContext().setAttachment("key", key);
        Boolean ret =  slave.DELETE(key);
        System.out.println("DELETE " + key);

        lock.unLockWrite();
        return ret;
    }
}
