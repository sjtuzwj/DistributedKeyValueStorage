package sjtu.lock;

import org.apache.curator.framework.CuratorFramework;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.zookeeper.EventType;
import sjtu.zkclient.CuratorZookeeperClient;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;



public final class Lock
{
    private enum LockType
    {
        READ,
        WRITE
    }
    private CuratorZookeeperClient zkClient;
    private String lockName;
    private String reader;
    private String writer;
    private final String readerUrl;
    private final String writerUrl;

    public static void main(String[] args) {
// Writer
        new Thread(() ->  {
            Lock lock=new Lock( "/lock");
            lock.lockWrite();
            try
            {
                System.out.println("I am Writer");
                System.out.println("开始倒计时10s");
                for (int i = 0; i < 10; i++)
                {
                    System.out.println(10-i);
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            lock.unLockWrite();
        }).start();
// Reader
        new Thread(() ->  {
            try
            {
                Thread.sleep(2000);
            }catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            Lock lock=new Lock( "/lock");
            lock.lockRead();
            System.out.println("I am Reader");
            lock.unLockRead();
        }).start();
    }

    /**
    * 连接zookeeper在lockName目录创建锁
     */
    public Lock(String lockUrl){
        lockName = lockUrl;
        readerUrl = lockName + "/" + LockType.READ + "-";
        writerUrl = lockName + "/" + LockType.WRITE + "-";
        zkClient = new CuratorZookeeperClient(new URL("zookeeper","127.0.0.1",2181));
        if (!zkClient.checkExists(lockName))
            zkClient.createPersistent(lockName);
    }
    /**
     * Wait until no writer in critical area
     */
    public void lockRead()
    {
        CountDownLatch readLatch = new CountDownLatch(1);
        // 入列
        reader = zkClient.createEphemeralSequential(readerUrl);
        // 遍历子节点
        List<String> childNode = zkClient.getChildren(lockName);
        sortNodes(childNode);
        int readerIndex = 0;
        for (int i = childNode.size() - 1; i >= 0; i--) {
            String curNode = lockName + "/" + childNode.get(i);
            //find reader index
            if (reader.equals(curNode))
                readerIndex = i;
            //find last writer index
            else if (i < readerIndex && childNode.get(i).split("-")[0].equals(LockType.WRITE.toString()))
            {
                //等待写锁释放
                zkClient.addDataListener(curNode , (path , value, eventType) -> {
                    if (eventType == EventType.NodeDeleted) readLatch.countDown();
                });
                try {
                    readLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    /**
     * Release Reader Lock
     */
    public void unLockRead()
    {
        if(reader !=null)
            zkClient.delete(reader);
        reader = null;
    }

    /**
     * Wait Until no user in critical area
     */
    public void lockWrite()
    {
        CountDownLatch writeLatch = new CountDownLatch(1);
        writer = zkClient.createEphemeralSequential(writerUrl);

        List<String> childNode = zkClient.getChildren(lockName);
        sortNodes(childNode);
        int writerIndex = 0;
        for (int i = childNode.size() - 1; i >= 0; i--)
        {
            String curNode = lockName + "/" + childNode.get(i);
            //find writer index
            if (writer.equals(curNode))
                writerIndex = i;
            //find last user index
            else if (i == writerIndex - 1)
            {
                zkClient.addDataListener(curNode , (path , value, eventType) ->{
                    if (eventType == EventType.NodeDeleted) writeLatch.countDown();
                });
                try
                {
                    writeLatch.await();
                } catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    /**
     * Release
     */
    public void unLockWrite()
    {
        if(writer !=null)
            zkClient.delete(writer);
        writer = null;
    }

    /**
     * Sequential
     *
     */
    private void sortNodes(List<String> nodes)
    {
        nodes.sort(Comparator.comparing(o -> o.split("-")[1]));
    }

    protected void finalize( )
    {
        zkClient.doClose();
    }
}
