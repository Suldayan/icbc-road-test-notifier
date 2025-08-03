package com.example.icbc_road_test_notifier;

import com.example.icbc_road_test_notifier.authentication.internal.IcbcAuthenticationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.modulith.ApplicationModule;

@SpringBootApplication
@EnableConfigurationProperties(IcbcAuthenticationProperties.class)
@ApplicationModule
public class IcbcRoadTestNotifierApplication {

	public static void main(String[] args) {
		SpringApplication.run(IcbcRoadTestNotifierApplication.class, args);
	}

}
