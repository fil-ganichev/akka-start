package juddy.transport.impl.kafka;

import juddy.transport.api.engine.ApiEngine;
import juddy.transport.config.kafka.KafkaSourceTestConfiguration;
import juddy.transport.impl.engine.ApiEngineImpl;
import juddy.transport.impl.source.kafka.KafkaSource;
import juddy.transport.test.sink.TestApiMock;
import juddy.transport.test.sink.TestApiSink;
import juddy.transport.test.sink.TestApiSinkServer;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;

@SuppressWarnings("checkstyle:methodName")
@SpringJUnitConfig(KafkaSourceTestConfiguration.class)
@EmbeddedKafka(
        partitions = 1,
        controlledShutdown = true,
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:3333",
                "port=3333"
        })
class KafkaSourceTest {

    @Autowired
    private KafkaTemplate<String, String> stringKafkaTemplate;
    @Autowired
    private ApiEngineImpl apiEngineFromKafkaSource;
    @Autowired
    private ApiEngineImpl apiEngineFromRollbackKafkaSource;
    @Autowired
    private ApiEngineImpl apiEngineFromAutoCommitKafkaSource;
    @Autowired
    private ApiEngine apiEngineFromRecoverableKafkaSource;
    @Autowired
    private TestApiSinkServer testApiSink;
    @Autowired
    private TestApiMock testApiMock;
    @Autowired
    private KafkaSource recoverableKafkaSource;
    @Autowired
    private EmbeddedKafkaBroker kafkaEmbedded;

    @AfterEach
    void clear() {
        Mockito.reset(testApiSink);
        testApiSink.reset();
    }

    /**
     * Кидаем в топик 3 записи и читаем их
     */
    @Test
    protected void when_readKafkaSource_then_ok() {
        TestApiSinkServer testApiSinkServer = (TestApiSinkServer) apiEngineFromKafkaSource
                .findServerBean(TestApiSink.class);
        List<String> messages = Arrays.asList("Москва", "Киев", "Минск");
        messages.forEach(message -> stringKafkaTemplate
                .send("when_readKafkaSource_then_ok_topic", message));
        await().atMost(1, TimeUnit.SECONDS).until(
                testApiSinkServer.processed(messages.size()));
        testApiSinkServer.check(messages.toArray(new String[0]));
    }

    /**
     * Кидаем в топик 4 записи, на 2-й записи происходит Exception и поток прерывается.
     * Повторно запускаем поток (и конзюмер) и убеждаемся, что 1-я запись отсутствует,
     * а остальные 3 записи считываются, так как коммит выполнен был только для 1-й записи
     */
    @Test
    protected void when_processingException_then_rollback() {
        doThrow(new RuntimeException("Error while processing string"))
                .when(testApiSink).process(argThat(value -> value.equals("Второй")));
        List<String> messages = Arrays.asList("Первый", "Второй", "Третий", "Четвертый");
        messages.forEach(message -> stringKafkaTemplate
                .send("when_processingException_then_rollback_topic", message));
        apiEngineFromRollbackKafkaSource.run();
        await().atMost(10, TimeUnit.SECONDS).until(
                testApiSink.processed(1));
        testApiSink.check("Первый");

        Mockito.reset(testApiSink);
        testApiSink.reset();
        apiEngineFromRollbackKafkaSource.run();
        await().atMost(10, TimeUnit.SECONDS).until(
                testApiSink.processed(3));
        testApiSink.check("Второй", "Третий", "Четвертый");
    }

    /**
     * Кидаем в топик 4 записи и дважды запускаем поток, читающий этот топик
     * Проверяем, что при повторном запуске потока ни одной записи не считано
     * Перед повторным запуском потока ждем 10 секунд, так как commit выполняется с задержкой offsets.commit.timeout.ms
     */
    @Test
    protected void when_processingWithoutException_then_allItemsCommited() throws Exception {
        List<String> messages = Arrays.asList("Первый", "Второй", "Третий", "Четвертый");
        messages.forEach(message -> stringKafkaTemplate
                .send("when_processingException_then_rollback_topic", message));
        apiEngineFromRollbackKafkaSource.run();

        // Ждем до 10 секунд, чтобы Consumer успел подключиться к брокеру
        await().atMost(10, TimeUnit.SECONDS).until(
                testApiSink.processed(1));
        testApiSink.check("Первый", "Второй", "Третий", "Четвертый");

        // Дожидаемся commit-а. По-умолчанию, offsets.commit.timeout.ms = 5000.
        assertThrows(ConditionTimeoutException.class,
                () -> await().atMost(10, TimeUnit.SECONDS).until(() -> false));

        testApiSink.reset();
        apiEngineFromRollbackKafkaSource.run();
        assertThrows(ConditionTimeoutException.class,
                () -> await().atMost(10, TimeUnit.SECONDS).until(testApiSink.processed(1)));
        testApiSink.check();
    }

    /**
     * KafkaSource настроен с опцией autoCommit=true
     * Кидаем в топик 4 записи и дважды запускаем поток, читающий этот топик
     * Проверяем, что при повторном запуске потока ни одной записи не считано
     */
    @Test
    protected void when_processingWithAutoCommit_then_allItemsCommited() {
        List<String> messages = Arrays.asList("Первый", "Второй", "Третий", "Четвертый");
        messages.forEach(message -> stringKafkaTemplate
                .send("when_processingWithAutoCommit_then_allItemsCommited_topic", message));
        apiEngineFromAutoCommitKafkaSource.run();
        await().atMost(10, TimeUnit.SECONDS).until(
                testApiSink.processed(1));
        testApiSink.check("Первый", "Второй", "Третий", "Четвертый");

        testApiSink.reset();
        apiEngineFromAutoCommitKafkaSource.run();
        assertThrows(ConditionTimeoutException.class,
                () -> await().atMost(10, TimeUnit.SECONDS).until(testApiSink.processed(1)));
        testApiSink.check();
    }

    /**
     * KafkaSource настроен с опцией autoCommit=true
     * Кидаем в топик 4 записи, на 2-й записи происходит Exception и поток прерывается.
     * Поскольку за один вызов poll все 4 записи считались и указана опция autoCommit=true, то
     * несмотря на то что поток прервался commit произошел еще до того как поток прервался
     * Опция max-batch-size=1 не имеет значения, так как commit происходит автоматически, а не через Commiter
     * Проверяем, что при повторном запуске потока ни одной записи не считано
     */
    @Test
    protected void when_processingExceptionWithAutoCommit_then_allItemsCommited() {
        doThrow(new RuntimeException("Error while processing string"))
                .when(testApiSink).process(argThat(value -> value.equals("Второй")));
        List<String> messages = Arrays.asList("Первый", "Второй", "Третий", "Четвертый");
        messages.forEach(message -> stringKafkaTemplate
                .send("when_processingWithAutoCommit_then_allItemsCommited_topic", message));
        apiEngineFromAutoCommitKafkaSource.run();
        await().atMost(10, TimeUnit.SECONDS).until(
                testApiSink.processed(1));
        testApiSink.check("Первый");

        Mockito.reset(testApiSink);
        testApiSink.reset();
        apiEngineFromAutoCommitKafkaSource.run();
        assertThrows(ConditionTimeoutException.class,
                () -> await().atMost(10, TimeUnit.SECONDS).until(testApiSink.processed(1)));
        testApiSink.check();
    }

    /**
     * KafkaSource настроен с опцией восстановления потока.
     * Дважды эмулируем Exception в потоке, он прерывается и запускается снова
     * В третий раз сообщения успешно обрабатываются
     */
    @Test
    protected void when_recoverableKafkaSource_then_itRecover() {
        doThrow(new RuntimeException("Error while processing string [first step]"))
                .doThrow(new RuntimeException("Error while processing string [second step]"))
                .doCallRealMethod()
                .when(testApiMock).process(anyString());
        List<String> messages = Arrays.asList("Первый", "Второй", "Третий", "Четвертый");
        messages.forEach(message -> stringKafkaTemplate
                .send("when_recoverableKafkaSource_then_itRecover_topic", message));
        apiEngineFromRecoverableKafkaSource.run();
        await().atMost(30, TimeUnit.SECONDS).until(
                testApiSink.processed(4));
        testApiSink.check("Первый", "Второй", "Третий", "Четвертый");

        recoverableKafkaSource.shutDown();
        assertThrows(ConditionTimeoutException.class, () -> await().atMost(5, TimeUnit.SECONDS)
                .until(() -> false));

        testApiSink.reset();
        apiEngineFromRecoverableKafkaSource.run();

        assertThrows(ConditionTimeoutException.class,
                () -> await().atMost(10, TimeUnit.SECONDS).until(testApiSink.processed(1)));
        testApiSink.check();
    }
}
