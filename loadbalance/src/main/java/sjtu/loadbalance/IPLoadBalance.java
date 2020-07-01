package sjtu.loadbalance;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.cluster.loadbalance.AbstractLoadBalance;

import java.util.List;

public class IPLoadBalance extends AbstractLoadBalance {
    protected <T> Invoker<T> findInvoker(List<Invoker<T>> invokers, String addr){
        for( Invoker<T> invoke: invokers)
            if(invoke.getUrl().getAddress().equals(addr))
                return invoke;
        return null;
    }
    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        String address = RpcContext.getContext().getAttachment("URL");
        System.out.println(address);
        return findInvoker(invokers,address);
    }
}
