package sjtu.slave;

import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;

import org.apache.dubbo.rpc.RpcContext;
import org.springframework.beans.factory.annotation.Value;
import sjtu.api.DataAPI;
import sjtu.api.SlaveAPI;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@DubboService(version = "${demo.service.version}")
public class SlaveService implements SlaveAPI {

    /**
     * The default value of ${dubbo.application.name} is ${spring.application.name}
     */
    @Value("${dubbo.application.name}")
    private String serviceName;
    @Value("${group.id}")
    private String groupid;
    ReadWriteLock lock = new ReentrantReadWriteLock();

    @DubboReference(version = "${demo.service.version}",group = "${group.id}",loadbalance = "PrimaryLoadBalance")
    private DataAPI data;

    SlaveService(){
    }

    @Override
    public String sayHello(String name) {
        return String.format("[%s] : Hello, %s", serviceName, name);
    }

    @Override
    public Boolean PUT(String key, String val) {
        String address = RpcContext.getContext().getAttachment("URL");
        lock.writeLock().lock();
            System.out.println("PUT " + key);
        RpcContext.getContext().setAttachment("URL", address);
            if(data.PUT(key,val))
                data.COMMIT();
            else{
                lock.writeLock().unlock();
                return false;
        }
        lock.writeLock().unlock();
        return true;
    }

    @Override
    public String READ(String key) {
        String address = RpcContext.getContext().getAttachment("URL");
        lock.readLock().lock();
        String ret = data.READ(key);
            System.out.println("READ " + key);
        lock.readLock().unlock();
            return ret;
    }
    @Override
    public ConcurrentHashMap<String,String> SYNC(){
        //已经加了锁了
        return data.SYNC();
    }

    @Override
    public Boolean DELETE(String key) {
        String address = RpcContext.getContext().getAttachment("URL");
        lock.writeLock().lock();
        RpcContext.getContext().setAttachment("URL", address);
            System.out.println("PUT " + key);
            if(data.DELETE(key))
                data.COMMIT();
            else{
                lock.writeLock().unlock();
                return false;
            }
        lock.writeLock().unlock();
            return true;
    }
}
