package sjtu.zkclient;

/*Dubbo 封装*/
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.remoting.zookeeper.ChildListener;
import org.apache.dubbo.remoting.zookeeper.DataListener;
import org.apache.dubbo.remoting.zookeeper.StateListener;
import org.apache.dubbo.remoting.zookeeper.ZookeeperClient;

public abstract class AbstractZookeeperClient<TargetDataListener, TargetChildListener> implements ZookeeperClient {
    protected static final Logger logger = LoggerFactory.getLogger(org.apache.dubbo.remoting.zookeeper.support.AbstractZookeeperClient.class);
    protected int DEFAULT_CONNECTION_TIMEOUT_MS = 5000;
    protected int DEFAULT_SESSION_TIMEOUT_MS = 60000;
    private final URL url;
    private final Set<StateListener> stateListeners = new CopyOnWriteArraySet();
    private final ConcurrentMap<String, ConcurrentMap<ChildListener, TargetChildListener>> childListeners = new ConcurrentHashMap();
    private final ConcurrentMap<String, ConcurrentMap<DataListener, TargetDataListener>> listeners = new ConcurrentHashMap();
    private volatile boolean closed = false;
    private final Set<String> persistentExistNodePath = new ConcurrentHashSet();

    public AbstractZookeeperClient(URL url) {
        this.url = url;
    }

    public URL getUrl() {
        return this.url;
    }

    public void delete(String path) {
        this.persistentExistNodePath.remove(path);
        this.deletePath(path);
    }

    public void create(String path, boolean ephemeral) {
        if (!ephemeral) {
            if (this.persistentExistNodePath.contains(path)) {
                return;
            }

            if (this.checkExists(path)) {
                this.persistentExistNodePath.add(path);
                return;
            }
        }

        int i = path.lastIndexOf(47);
        if (i > 0) {
            this.create(path.substring(0, i), false);
        }

        if (ephemeral) {
            this.createEphemeral(path);
        } else {
            this.createPersistent(path);
            this.persistentExistNodePath.add(path);
        }

    }

    public void addStateListener(StateListener listener) {
        this.stateListeners.add(listener);
    }

    public void removeStateListener(StateListener listener) {
        this.stateListeners.remove(listener);
    }

    public Set<StateListener> getSessionListeners() {
        return this.stateListeners;
    }

    public List<String> addChildListener(String path, final ChildListener listener) {
        ConcurrentMap<ChildListener, TargetChildListener> listeners = (ConcurrentMap)this.childListeners.computeIfAbsent(path, (k) -> {
            return new ConcurrentHashMap();
        });
        TargetChildListener targetListener = listeners.computeIfAbsent(listener, (k) -> {
            return this.createTargetChildListener(path, k);
        });
        return this.addTargetChildListener(path, targetListener);
    }

    public void addDataListener(String path, DataListener listener) {
        this.addDataListener(path, listener, (Executor)null);
    }

    public void addDataListener(String path, DataListener listener, Executor executor) {
        ConcurrentMap<DataListener, TargetDataListener> dataListenerMap = (ConcurrentMap)this.listeners.computeIfAbsent(path, (k) -> {
            return new ConcurrentHashMap();
        });
        TargetDataListener targetListener = dataListenerMap.computeIfAbsent(listener, (k) -> {
            return this.createTargetDataListener(path, k);
        });
        this.addTargetDataListener(path, targetListener, executor);
    }

    public void removeDataListener(String path, DataListener listener) {
        ConcurrentMap<DataListener, TargetDataListener> dataListenerMap = (ConcurrentMap)this.listeners.get(path);
        if (dataListenerMap != null) {
            TargetDataListener targetListener = dataListenerMap.remove(listener);
            if (targetListener != null) {
                this.removeTargetDataListener(path, targetListener);
            }
        }

    }

    public void removeChildListener(String path, ChildListener listener) {
        ConcurrentMap<ChildListener, TargetChildListener> listeners = (ConcurrentMap)this.childListeners.get(path);
        if (listeners != null) {
            TargetChildListener targetListener = listeners.remove(listener);
            if (targetListener != null) {
                this.removeTargetChildListener(path, targetListener);
            }
        }

    }

    protected void stateChanged(int state) {
        Iterator var2 = this.getSessionListeners().iterator();

        while(var2.hasNext()) {
            StateListener sessionListener = (StateListener)var2.next();
            sessionListener.stateChanged(state);
        }

    }

    public void close() {
        if (!this.closed) {
            this.closed = true;

            try {
                this.doClose();
            } catch (Throwable var2) {
                logger.warn(var2.getMessage(), var2);
            }

        }
    }

    public void create(String path, String content, boolean ephemeral) {
        if (this.checkExists(path)) {
            this.delete(path);
        }

        int i = path.lastIndexOf(47);
        if (i > 0) {
            this.create(path.substring(0, i), false);
        }

        if (ephemeral) {
            this.createEphemeral(path, content);
        } else {
            this.createPersistent(path, content);
        }

    }

    public String getContent(String path) {
        return !this.checkExists(path) ? null : this.doGetContent(path);
    }

    protected abstract void doClose();

    protected abstract void createPersistent(String path);

    public abstract void createEphemeral(String path);

    protected abstract void createPersistent(String path, String data);

    protected abstract void createEphemeral(String path, String data);

    protected abstract boolean checkExists(String path);

    protected abstract TargetChildListener createTargetChildListener(String path, ChildListener listener);

    protected abstract List<String> addTargetChildListener(String path, TargetChildListener listener);

    protected abstract TargetDataListener createTargetDataListener(String path, DataListener listener);

    protected abstract void addTargetDataListener(String path, TargetDataListener listener);

    protected abstract void addTargetDataListener(String path, TargetDataListener listener, Executor executor);

    protected abstract void removeTargetDataListener(String path, TargetDataListener listener);

    protected abstract void removeTargetChildListener(String path, TargetChildListener listener);

    protected abstract String doGetContent(String path);

    protected abstract void deletePath(String path);
}
