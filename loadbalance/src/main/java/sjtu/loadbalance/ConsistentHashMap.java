package sjtu.loadbalance;


import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.dubbo.common.utils.StringUtils;

public class ConsistentHashMap {
    private List<String> realNodes = new LinkedList<String>();
    private SortedMap<Integer, String> virtualNodes = new TreeMap<Integer, String>();
    private final int VIRTUAL_NODES = 20;

    public void addServer(List<String> servers){
        realNodes = servers;
        //增加后缀作为虚拟节点
        for (String str : realNodes){
            for(int i=0; i<VIRTUAL_NODES; i++){
                String virtualNodeName = str + "&&" + String.valueOf(i);
                int hash = getHash(virtualNodeName);
                virtualNodes.put(hash, virtualNodeName);
            }
        }
    }
    //FNV1_32_HASH
    private static int getHash(String str){
        final int p = 16777619;
        int hash = (int)2166136261L;
        for (int i = 0; i < str.length(); i++)
            hash = (hash ^ str.charAt(i)) * p;
        hash += hash << 13;
        hash ^= hash >> 7;
        hash += hash << 3;
        hash ^= hash >> 17;
        hash += hash << 5;
        if (hash < 0)
            hash = Math.abs(hash);
        return hash;
    }
    //计算
    public String getServer(String key){
        int hash = getHash(key);
        // 大于key的所有节点
        SortedMap<Integer, String> subMap = virtualNodes.tailMap(hash);
        String virtualNode;
        if(subMap.isEmpty()){
            //环首
            Integer i = virtualNodes.firstKey();
            virtualNode = virtualNodes.get(i);
        }else{
            //next fit
            Integer i = subMap.firstKey();
            virtualNode = subMap.get(i);
        }
        //处理后缀获得实际节点
        if(!StringUtils.isBlank(virtualNode)){
            return virtualNode.substring(0, virtualNode.indexOf("&&"));
        }
        return null;
    }
}
