package sjtu.zkclient;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.CuratorFrameworkFactory.Builder;
import org.apache.curator.framework.api.ACLBackgroundPathAndBytesable;
import org.apache.curator.framework.api.BackgroundPathable;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.RetryNTimes;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.zookeeper.ChildListener;
import org.apache.dubbo.remoting.zookeeper.DataListener;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.Watcher.Event.EventType;

public class CuratorZookeeperClient extends AbstractZookeeperClient<CuratorZookeeperClient.CuratorWatcherImpl, CuratorZookeeperClient.CuratorWatcherImpl> {
    protected static final Logger logger = LoggerFactory.getLogger(CuratorZookeeperClient.class);
    private static final String ZK_SESSION_EXPIRE_KEY = "zk.session.expire";
    static final Charset CHARSET = Charset.forName("UTF-8");
    private final CuratorFramework client;
    private Map<String, TreeCache> treeCacheMap = new ConcurrentHashMap();

    public CuratorZookeeperClient(URL url) {
        super(url);

        try {
            int timeout = url.getParameter("timeout", this.DEFAULT_CONNECTION_TIMEOUT_MS);
            int sessionExpireMs = url.getParameter("zk.session.expire", this.DEFAULT_SESSION_TIMEOUT_MS);
            Builder builder = CuratorFrameworkFactory.builder().connectString(url.getBackupAddress()).retryPolicy(new RetryNTimes(1, 1000)).connectionTimeoutMs(timeout).sessionTimeoutMs(sessionExpireMs);
            String authority = url.getAuthority();
            if (authority != null && authority.length() > 0) {
                builder = builder.authorization("digest", authority.getBytes());
            }

            this.client = builder.build();
            this.client.getConnectionStateListenable().addListener(new CuratorZookeeperClient.CuratorConnectionStateListener(url));
            this.client.start();
            boolean connected = this.client.blockUntilConnected(timeout, TimeUnit.MILLISECONDS);
            if (!connected) {
                throw new IllegalStateException("zookeeper not connected");
            }
        } catch (Exception var7) {
            throw new IllegalStateException(var7.getMessage(), var7);
        }
    }

    public void setContent(String path,String value){
        try {
            if(this.checkExists(path))
                this.client.setData().forPath(path,value.getBytes());
        } catch (Exception var4) {
            throw new IllegalStateException(var4.getMessage(), var4);
        }
    }

    public void createPersistent(String path) {
        try {
            this.client.create().forPath(path);
        } catch (NodeExistsException var3) {
            logger.warn("ZNode " + path + " already exists.", var3);
        } catch (Exception var4) {
            throw new IllegalStateException(var4.getMessage(), var4);
        }

    }

    public void createEmptyPersistent(String path) {
        try {
            this.client.create().forPath(path,"".getBytes());
        } catch (NodeExistsException var3) {
            logger.warn("ZNode " + path + " already exists.", var3);
        } catch (Exception var4) {
            throw new IllegalStateException(var4.getMessage(), var4);
        }

    }
    public String createEphemeralSequential(String path) {
        try {
            return ((ACLBackgroundPathAndBytesable<String>)this.client.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL)).forPath(path);
        } catch (Exception var4) {
            throw new IllegalStateException(var4.getMessage(), var4);
        }
    }
    public void createEphemeral(String path) {
        try {
            ((ACLBackgroundPathAndBytesable<String>)this.client.create().withMode(CreateMode.EPHEMERAL)).forPath(path);
        } catch (NodeExistsException var3) {
            logger.warn("ZNode " + path + " already exists, since we will only try to recreate a node on a session expiration, this duplication might be caused by a delete delay from the zk server, which means the old expired session may still holds this ZNode and the server just hasn't got time to do the deletion. In this case, we can just try to delete and create again.", var3);
            this.deletePath(path);
        } catch (Exception var4) {
            throw new IllegalStateException(var4.getMessage(), var4);
        }

    }

    protected void createPersistent(String path, String data) {
        byte[] dataBytes = data.getBytes(CHARSET);

        try {
            this.client.create().forPath(path, dataBytes);
        } catch (NodeExistsException var7) {
            try {
                this.client.setData().forPath(path, dataBytes);
            } catch (Exception var6) {
                throw new IllegalStateException(var7.getMessage(), var6);
            }
        } catch (Exception var8) {
            throw new IllegalStateException(var8.getMessage(), var8);
        }

    }

    protected void createEphemeral(String path, String data) {
        byte[] dataBytes = data.getBytes(CHARSET);

        try {
            ((ACLBackgroundPathAndBytesable)this.client.create().withMode(CreateMode.EPHEMERAL)).forPath(path, dataBytes);
        } catch (NodeExistsException var5) {
            logger.warn("ZNode " + path + " already exists, since we will only try to recreate a node on a session expiration, this duplication might be caused by a delete delay from the zk server, which means the old expired session may still holds this ZNode and the server just hasn't got time to do the deletion. In this case, we can just try to delete and create again.", var5);
            this.deletePath(path);
            this.createEphemeral(path, data);
        } catch (Exception var6) {
            throw new IllegalStateException(var6.getMessage(), var6);
        }

    }

    protected void deletePath(String path) {
        try {
            this.client.delete().deletingChildrenIfNeeded().forPath(path);
        } catch (NoNodeException var3) {
        } catch (Exception var4) {
            throw new IllegalStateException(var4.getMessage(), var4);
        }

    }

    public List<String> getChildren(String path) {
        try {
            return (List)this.client.getChildren().forPath(path);
        } catch (NoNodeException var3) {
            return null;
        } catch (Exception var4) {
            throw new IllegalStateException(var4.getMessage(), var4);
        }
    }

    public boolean checkExists(String path) {
        try {
            if (this.client.checkExists().forPath(path) != null) {
                return true;
            }
        } catch (Exception var3) {
        }

        return false;
    }

    public boolean isConnected() {
        return this.client.getZookeeperClient().isConnected();
    }

    public String doGetContent(String path) {
        try {
            byte[] dataBytes = (byte[])this.client.getData().forPath(path);
            return dataBytes != null && dataBytes.length != 0 ? new String(dataBytes, CHARSET) : null;
        } catch (NoNodeException var3) {
            return null;
        } catch (Exception var4) {
            throw new IllegalStateException(var4.getMessage(), var4);
        }
    }

    public void doClose() {
        this.client.close();
    }

    public CuratorZookeeperClient.CuratorWatcherImpl createTargetChildListener(String path, ChildListener listener) {
        return new CuratorZookeeperClient.CuratorWatcherImpl(this.client, listener, path);
    }

    public List<String> addTargetChildListener(String path, CuratorZookeeperClient.CuratorWatcherImpl listener) {
        try {
            return (List)((BackgroundPathable)this.client.getChildren().usingWatcher(listener)).forPath(path);
        } catch (NoNodeException var4) {
            return null;
        } catch (Exception var5) {
            throw new IllegalStateException(var5.getMessage(), var5);
        }
    }

    protected CuratorZookeeperClient.CuratorWatcherImpl createTargetDataListener(String path, DataListener listener) {
        return new CuratorZookeeperClient.CuratorWatcherImpl(this.client, listener);
    }

    protected void addTargetDataListener(String path, CuratorZookeeperClient.CuratorWatcherImpl treeCacheListener) {
        this.addTargetDataListener(path, (CuratorZookeeperClient.CuratorWatcherImpl)treeCacheListener, (Executor)null);
    }

    protected void addTargetDataListener(String path, CuratorZookeeperClient.CuratorWatcherImpl treeCacheListener, Executor executor) {
        try {
            TreeCache treeCache = TreeCache.newBuilder(this.client, path).setCacheData(false).build();
            this.treeCacheMap.putIfAbsent(path, treeCache);
            if (executor == null) {
                treeCache.getListenable().addListener(treeCacheListener);
            } else {
                treeCache.getListenable().addListener(treeCacheListener, executor);
            }

            treeCache.start();
        } catch (Exception var5) {
            throw new IllegalStateException("Add treeCache listener for path:" + path, var5);
        }
    }

    protected void removeTargetDataListener(String path, CuratorZookeeperClient.CuratorWatcherImpl treeCacheListener) {
        TreeCache treeCache = (TreeCache)this.treeCacheMap.get(path);
        if (treeCache != null) {
            treeCache.getListenable().removeListener(treeCacheListener);
        }

        treeCacheListener.dataListener = null;
    }

    public void removeTargetChildListener(String path, CuratorZookeeperClient.CuratorWatcherImpl listener) {
        listener.unwatch();
    }

    CuratorFramework getClient() {
        return this.client;
    }

    private class CuratorConnectionStateListener implements ConnectionStateListener {
        private final long UNKNOWN_SESSION_ID = -1L;
        private long lastSessionId;
        private URL url;

        public CuratorConnectionStateListener(URL url) {
            this.url = url;
        }

        public void stateChanged(CuratorFramework client, ConnectionState state) {
            int timeout = this.url.getParameter("timeout", CuratorZookeeperClient.this.DEFAULT_CONNECTION_TIMEOUT_MS);
            int sessionExpireMs = this.url.getParameter("zk.session.expire", CuratorZookeeperClient.this.DEFAULT_SESSION_TIMEOUT_MS);
            long sessionId = -1L;

            try {
                sessionId = client.getZookeeperClient().getZooKeeper().getSessionId();
            } catch (Exception var8) {
                CuratorZookeeperClient.logger.warn("Curator client state changed, but failed to get the related zk session instance.");
            }

            if (state == ConnectionState.LOST) {
                CuratorZookeeperClient.logger.warn("Curator zookeeper session " + Long.toHexString(this.lastSessionId) + " expired.");
                CuratorZookeeperClient.this.stateChanged(0);
            } else if (state == ConnectionState.SUSPENDED) {
                CuratorZookeeperClient.logger.warn("Curator zookeeper connection of session " + Long.toHexString(sessionId) + " timed out. connection timeout value is " + timeout + ", session expire timeout value is " + sessionExpireMs);
                CuratorZookeeperClient.this.stateChanged(3);
            } else if (state == ConnectionState.CONNECTED) {
                this.lastSessionId = sessionId;
                CuratorZookeeperClient.logger.info("Curator zookeeper client instance initiated successfully, session id is " + Long.toHexString(sessionId));
                CuratorZookeeperClient.this.stateChanged(1);
            } else if (state == ConnectionState.RECONNECTED) {
                if (this.lastSessionId == sessionId && sessionId != -1L) {
                    CuratorZookeeperClient.logger.warn("Curator zookeeper connection recovered from connection lose, reuse the old session " + Long.toHexString(sessionId));
                    CuratorZookeeperClient.this.stateChanged(2);
                } else {
                    CuratorZookeeperClient.logger.warn("New session created after old session lost, old session " + Long.toHexString(this.lastSessionId) + ", new session " + Long.toHexString(sessionId));
                    this.lastSessionId = sessionId;
                    CuratorZookeeperClient.this.stateChanged(4);
                }
            }

        }
    }

    static class CuratorWatcherImpl implements CuratorWatcher, TreeCacheListener {
        private CuratorFramework client;
        private volatile ChildListener childListener;
        private volatile DataListener dataListener;
        private String path;

        public CuratorWatcherImpl(CuratorFramework client, ChildListener listener, String path) {
            this.client = client;
            this.childListener = listener;
            this.path = path;
        }

        public CuratorWatcherImpl(CuratorFramework client, DataListener dataListener) {
            this.dataListener = dataListener;
        }

        protected CuratorWatcherImpl() {
        }

        public void unwatch() {
            this.childListener = null;
        }

        public void process(WatchedEvent event) throws Exception {
            if (event.getType() != EventType.None) {
                if (this.childListener != null) {
                    this.childListener.childChanged(this.path, (List)((BackgroundPathable)this.client.getChildren().usingWatcher(this)).forPath(this.path));
                }

            }
        }

        public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
            if (this.dataListener != null) {
                if (CuratorZookeeperClient.logger.isDebugEnabled()) {
                    CuratorZookeeperClient.logger.debug("listen the zookeeper changed. The changed data:" + event.getData());
                }

                Type type = event.getType();
                org.apache.dubbo.remoting.zookeeper.EventType eventType = null;
                String content = null;
                String path = null;
                switch(type) {
                    case NODE_ADDED:
                        eventType = org.apache.dubbo.remoting.zookeeper.EventType.NodeCreated;
                        path = event.getData().getPath();
                        content = event.getData().getData() == null ? "" : new String(event.getData().getData(), CuratorZookeeperClient.CHARSET);
                        break;
                    case NODE_UPDATED:
                        eventType = org.apache.dubbo.remoting.zookeeper.EventType.NodeDataChanged;
                        path = event.getData().getPath();
                        content = event.getData().getData() == null ? "" : new String(event.getData().getData(), CuratorZookeeperClient.CHARSET);
                        break;
                    case NODE_REMOVED:
                        path = event.getData().getPath();
                        eventType = org.apache.dubbo.remoting.zookeeper.EventType.NodeDeleted;
                        break;
                    case INITIALIZED:
                        eventType = org.apache.dubbo.remoting.zookeeper.EventType.INITIALIZED;
                        break;
                    case CONNECTION_LOST:
                        eventType = org.apache.dubbo.remoting.zookeeper.EventType.CONNECTION_LOST;
                        break;
                    case CONNECTION_RECONNECTED:
                        eventType = org.apache.dubbo.remoting.zookeeper.EventType.CONNECTION_RECONNECTED;
                        break;
                    case CONNECTION_SUSPENDED:
                        eventType = org.apache.dubbo.remoting.zookeeper.EventType.CONNECTION_SUSPENDED;
                }

                this.dataListener.dataChanged(path, content, eventType);
            }

        }
    }
}
