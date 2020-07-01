package sjtu.loadbalance;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.dubbo.config.RegistryConfig;
import io.netty.util.internal.ThreadLocalRandom;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.cluster.loadbalance.AbstractLoadBalance;
import org.apache.dubbo.rpc.cluster.loadbalance.ConsistentHashLoadBalance;
import sjtu.api.RouterAPI;

public class TOWLoadBalance extends AbstractLoadBalance {
    private static Random rng = new Random();
    private static ConsistentHashMap chm = new ConsistentHashMap();
    //判断hash表是否需要变化
    private static Set<String> addrs = new HashSet<String>();
    private RouterAPI router;

    protected <T> Invoker<T> findInvoker(List<Invoker<T>> invokers, String addr){
        for( Invoker<T> invoke: invokers)
            if(invoke.getUrl().getAddress().equals(addr))
                return invoke;
        return null;
    }
    protected <T> List<String>getAddress(List<Invoker<T>> invokers){
        List<String> addrs = new LinkedList<String>();
        for( Invoker<T> invoke: invokers)
            addrs.add(invoke.getUrl().getAddress());
        return addrs;
    }

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {// 当前应用配置
        ApplicationConfig application = new ApplicationConfig();
        application.setName("nmslwsnd");
        RegistryConfig registry = new RegistryConfig();
        registry.setAddress("zookeeper://127.0.0.1:2181");
        ReferenceConfig<RouterAPI> reference = new ReferenceConfig<RouterAPI>();
        reference.setApplication(application);
        reference.setRegistry(registry);
        reference.setInterface(RouterAPI.class);
        reference.setVersion("1.0.0");
        router = reference.get();

        int serviceNum = invokers.size();
        System.out.println("Now node number is " + serviceNum);
        String key = invocation.getAttachments().get("key");
        String method = invocation.getMethodName();

        //Node Change
        List<String> addrlist = getAddress(invokers);
            Set<String> newaddrs = new HashSet<String>();
            newaddrs.addAll(addrlist);
            if(!addrs.equals(newaddrs)) {
                addrs = newaddrs;
                chm = new ConsistentHashMap();
                chm.addServer(addrlist);
            }
        String addr =  chm.getServer(key);
        Invoker<T> invoke = findInvoker(invokers,addr);
        String expectAddr =invoke.getUrl().getAddress();
        Invoker<T> ret;
        if(method.equals("READ")){
            //有数据,沿用之前的服务器
            if(router.CONTAIN(key))
                ret =  findInvoker(invokers, router.GET(key));
                //无数据,直接使用Hash
            else {
                router.PUT(key,invoke.getUrl().getAddress());
                ret= invoke;
            }
        }
        else if(method.equals("PUT")||method.equals("DELETE")){
            //有数据
            if(router.CONTAIN(key)) {
                //节点增加,更新至新的服务器,这个时候delete是个假的delete,但是新服务器没数据所以等效,但是返回false
                if (!router.GET(key).equals( expectAddr))
                    router.PUT(key,  expectAddr);
                ret= invoke;
            }
            //无数据,直接使用hash
            else {
                router.PUT(key, expectAddr);
                ret=  invoke;
            }
        }
        //非法指令,随机分配
        else
            ret= invokers.get(rng.nextInt(serviceNum-1));
        RpcContext.getContext().setAttachment("URL", ret.getUrl().getAddress());
        return ret;
    }
}
