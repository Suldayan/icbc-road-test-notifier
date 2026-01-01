package com.example.icbc_road_test_notifier.notifier.internal;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {"logging.level.com.example=DEBUG"})
public class EmailServiceIntegrationTest {

}
