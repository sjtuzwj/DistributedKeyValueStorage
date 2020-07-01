package sjtu.routercoordinator;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;

import org.springframework.beans.factory.annotation.Value;
import sjtu.api.DataAPI;
import sjtu.api.RouterAPI;
import sjtu.api.RouterDataAPI;
import sjtu.api.SlaveAPI;
import sjtu.loadbalance.RouterLoadBalance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@DubboService(version = "${demo.service.version}")
public class RouterCoordinatorService implements RouterAPI {

    /**
     * The default value of ${dubbo.application.name} is ${spring.application.name}
     */
    @Value("${dubbo.application.name}")
    private String serviceName;
    ReadWriteLock lock = new ReentrantReadWriteLock();

    @DubboReference(version = "${demo.service.version}",loadbalance = "RouterLoadBalance")
    private RouterDataAPI data;

    @Override
    public Boolean PUT(String key, String val) {
        lock.writeLock().lock();
        System.out.println("PUT " + key);
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
    public String GET(String key) {
        lock.readLock().lock();
        System.out.println("READ " + key);
        String ret = data.GET(key);
        lock.readLock().unlock();
        return ret;
    }
    @Override
    public ConcurrentHashMap<String,String> SYNC(){
        return data.SYNC();
    }

    @Override
    public Boolean CONTAIN(String key) {
        lock.readLock().lock();
        System.out.println("CONT " + key);
        Boolean ret = data.CONTAIN(key);
        lock.readLock().unlock();
        return ret;
    }
}
