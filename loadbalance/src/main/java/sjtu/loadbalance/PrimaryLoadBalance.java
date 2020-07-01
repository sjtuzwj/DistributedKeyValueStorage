package sjtu.loadbalance;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.cluster.loadbalance.AbstractLoadBalance;

public class PrimaryLoadBalance extends AbstractLoadBalance {
    //省略无状态实现，和Data差不多，不想写了
    static String primary = "";

    protected <T> Invoker<T> findInvoker(List<Invoker<T>> invokers, String addr){
        for( Invoker<T> invoke: invokers)
            if(invoke.getUrl().getAddress().equals(addr))
                return invoke;
        return null;
    }
    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        String method = invocation.getMethodName();
        if(primary.equals("")){
            primary = invokers.get(0).getUrl().getAddress();
        }
        Invoker<T> pri= findInvoker(invokers,primary);
        if(pri == null) {
            //primary 挂了，决议新的
            pri = invokers.get(0);
            primary = pri.getUrl().getAddress();
            System.out.println("Now new Primary is "+ primary);
        }
        if (method.equals("READ")||method.equals("SYNC")) {
            return pri;
        } else if (method.equals("PUT") || method.equals("DELETE") || method.equals("COMMIT")) {
            for (int i = 1; i < invokers.size(); i++) {
                String address = RpcContext.getContext().getAttachment("URL");
                    invokers.get(i).invoke(invocation);
                RpcContext.getContext().setAttachment("URL", address);
            }
            return invokers.get(0);
        }
        return invokers.get(0);
    }
}
