package sjtu.api;

public interface MasterAPI {
    String sayHello(String name);
    Boolean PUT(String key, String val);
    String  READ(String key);
    Boolean DELETE(String key);
}
