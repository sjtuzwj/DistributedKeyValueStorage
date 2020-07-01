package sjtu.slave;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

@EnableAutoConfiguration
public class SlaveApplication {

    public static void main(String[] args) {
        SpringApplication.run(SlaveApplication.class, args);
    }

}
