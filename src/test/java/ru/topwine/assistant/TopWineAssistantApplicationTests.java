package ru.topwine.assistant;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class TopWineAssistantApplicationTests {

    @MockitoBean
    private DSLContext dslContext;

    @Test
    void contextLoads() {
    }

}
