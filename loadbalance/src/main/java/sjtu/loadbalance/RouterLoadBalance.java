package sjtu.loadbalance;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.netty.util.internal.ThreadLocalRandom;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.loadbalance.AbstractLoadBalance;
import org.apache.dubbo.rpc.cluster.loadbalance.ConsistentHashLoadBalance;
import sjtu.zkclient.CuratorZookeeperClient;

public class RouterLoadBalance extends AbstractLoadBalance {
    String primary;
    static String nodeName = "/pri-route";
    static Lock l = new ReentrantLock();

    static CuratorZookeeperClient zkClient = new CuratorZookeeperClient(new URL("zookeeper","127.0.0.1",2181));
    protected <T> Invoker<T> findInvoker(List<Invoker<T>> invokers, String addr){
        for( Invoker<T> invoke: invokers)
            if(invoke.getUrl().getAddress().equals(addr))
                return invoke;
        return null;
    }
    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        if (!zkClient.checkExists(nodeName)) {
            zkClient.createPersistent(nodeName);
        }
        String primary = zkClient.getContent(nodeName);
        String method = invocation.getMethodName();
        if(primary.equals("")) {
            primary = invokers.get(0).getUrl().getAddress();
            l.lock();
            zkClient.setContent(nodeName,primary);
            l.unlock();
        }
        Invoker<T> pri= findInvoker(invokers,primary);
        if(pri == null) {
            //primary 挂了，决议新的
            pri = invokers.get(0);
            primary = invokers.get(0).getUrl().getAddress();
            l.lock();
            zkClient.setContent(nodeName,primary);
            l.unlock();
        }

        if (method.equals("GET")||method.equals("SYNC") || method.equals("CONTAIN")) {
            return pri;
        } else if (method.equals("PUT")) {
            for (int i = 1; i < invokers.size(); i++) {
            invokers.get(i).invoke(invocation);
        }
            return invokers.get(0);
        }
        return invokers.get(0);
    }
}
