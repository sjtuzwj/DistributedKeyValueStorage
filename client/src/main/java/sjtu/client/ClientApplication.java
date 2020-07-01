package sjtu.client;


import org.apache.dubbo.config.annotation.DubboReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import sjtu.api.MasterAPI;


@EnableAutoConfiguration
public class ClientApplication {

    private final Logger logger = LoggerFactory.getLogger(getClass());


    @DubboReference(version = "${demo.service.version}")
    private MasterAPI master;
    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }


    @Bean
    public ApplicationRunner runner() {
        return args -> {
            for(Integer i = 0;i<20;i++){
                logger.info("READ "+ i + " with "+ master.READ(i.toString()));
            }
        };
    }
}
