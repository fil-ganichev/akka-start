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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static juddy.transport.common.Constants.KAFKA_CONSUMER_TIMEOUT_MS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;

/**
 * Тесты компонента KafkaSource
 * Проверяется: выполнение commit, отсутствие commit при ошибках, autocommit, восстановление после сбоев
 * Общая структура тестов следующая:
 * 1.Запускается ApiEngine с источником KafkaSource и конечным элементом TestApiSink
 * 2.Кидаем в топик несколько записей
 * 3.Ждем максимум KAFKA_CONSUMER_TIMEOUT_MS времени, чтобы Consumer из KafkaSource гарантированно
 * был обнаружен брокером Кафка
 * 4.Проверяем элементы, которые были получены в TestApiSink
 * 5.Корректно завершаем работу KafkaSource через shutdown(). Это нужно, в первую очередь, для того,
 * чтобы гарантировать commit последних считанных записей
 * 6.Повторяем шаги 1-5
 */
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

    private static final long RECOVERABLE_KAFKA_SOURCE_TIMEOUT_MS = 30000;

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
    private KafkaSource kafkaSource;
    @Autowired
    private KafkaSource rollbackKafkaSource;
    @Autowired
    private KafkaSource recoverableKafkaSource;
    @Autowired
    private KafkaSource autoCommitKafkaSource;
    @Autowired
    private EmbeddedKafkaBroker kafkaEmbedded;

    @AfterEach
    void clear() {
        Mockito.reset(testApiSink);
        testApiSink.reset();
    }

    /**
     * Цель теста проверить чтение записей из KafkaSource
     * Кидаем в топик 3 записи и читаем их
     */
    @Test
    protected void when_readKafkaSource_then_ok() throws ExecutionException, InterruptedException {
        try {
            TestApiSinkServer testApiSinkServer = (TestApiSinkServer) apiEngineFromKafkaSource
                    .findServerBean(TestApiSink.class);
            // Кидаем записи в топик
            List<String> messages = Arrays.asList("Москва", "Киев", "Минск");
            messages.forEach(message -> stringKafkaTemplate
                    .send("when_readKafkaSource_then_ok_topic", message));
            // Ждем максимум KAFKA_CONSUMER_TIMEOUT_MS, давая время брокеру Кафка обнаружить Consumer
            await().atMost(KAFKA_CONSUMER_TIMEOUT_MS, TimeUnit.MILLISECONDS).until(
                    testApiSinkServer.processed(messages.size()));
            // Проверяем, что считаны те же записи, что поместили в топик в нужном порядке
            testApiSinkServer.check(messages.toArray(new String[0]));
        } finally {
            // Корректно завершаем работу KafkaSource, гарантируя выполнение commit
            kafkaSource.shutDown().toCompletableFuture().get();
        }
    }

    /**
     * Цель теста проверить отсутствие commit при возникновении Exception в потоке
     * Кидаем в топик 4 записи, на 2-й записи происходит Exception и поток прерывается.
     * Повторно запускаем поток (и конзюмер) и убеждаемся, что 1-я запись отсутствует,
     * а остальные 3 записи считываются, так как коммит выполнен был только для 1-й записи
     */
    @Test
    protected void when_processingException_then_rollback() throws ExecutionException, InterruptedException {
        try {
            // Имитируем Exception в TestApiSink, при получении 2-го элемента
            doThrow(new RuntimeException("Error while processing string"))
                    .when(testApiSink).process(argThat(value -> value.equals("Второй")));
            // Кидаем записи в топик
            List<String> messages = Arrays.asList("Первый", "Второй", "Третий", "Четвертый");
            messages.forEach(message -> stringKafkaTemplate
                    .send("when_processingException_then_rollback_topic", message));
            // Запускаем ApiEngine
            apiEngineFromRollbackKafkaSource.run();
            // Ждем максимум KAFKA_CONSUMER_TIMEOUT_MS, давая время брокеру Кафка обнаружить Consumer
            await().atMost(KAFKA_CONSUMER_TIMEOUT_MS, TimeUnit.MILLISECONDS).until(
                    testApiSink.processed(1));
            // Проверяем, что считана только 1 запись, так как на 2-й записи поток прервался
            testApiSink.check("Первый");
            // Завершаем работу KafkaSource, проверяя Exception при его закрытии
            ExecutionException executionException = assertThrows(ExecutionException.class,
                    () -> rollbackKafkaSource.shutDown().toCompletableFuture().get());
            assertThat(executionException.getCause().getMessage()).isEqualTo("Error while processing string");

            // Сбрасываем имитацию Exception
            Mockito.reset(testApiSink);
            // Сбрасываем счетчик и данные в TestApiSink
            testApiSink.reset();
            // Запускаем ApiEngine повторно
            apiEngineFromRollbackKafkaSource.run();
            // Ждем максимум KAFKA_CONSUMER_TIMEOUT_MS, давая время брокеру Кафка обнаружить Consumer
            await().atMost(KAFKA_CONSUMER_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .until(testApiSink.processed(3));
            // Проверяем, что считаны остальные записи
            testApiSink.check("Второй", "Третий", "Четвертый");
        } finally {
            // Корректно завершаем работу KafkaSource, гарантируя выполнение commit
            rollbackKafkaSource.shutDown().toCompletableFuture().get();
        }
    }

    /**
     * Цель теста проверить выполнение commit при корректном завершении потока
     * Кидаем в топик 4 записи и дважды запускаем поток, читающий этот топик
     * Проверяем, что при повторном запуске потока ни одной записи не считано
     */
    @Test
    protected void when_processingWithoutException_then_allItemsCommited()
            throws ExecutionException, InterruptedException {
        try {
            // Кидаем записи в топик
            List<String> messages = Arrays.asList("Первый", "Второй", "Третий", "Четвертый");
            messages.forEach(message -> stringKafkaTemplate
                    .send("when_processingException_then_rollback_topic", message));
            // Запускаем ApiEngine
            apiEngineFromRollbackKafkaSource.run();

            // Ждем максимум KAFKA_CONSUMER_TIMEOUT_MS, давая время брокеру Кафка обнаружить Consumer
            await().atMost(KAFKA_CONSUMER_TIMEOUT_MS, TimeUnit.MILLISECONDS).until(
                    testApiSink.processed(4));
            // Проверяем, что считаны все записи, помещенные в топик
            testApiSink.check("Первый", "Второй", "Третий", "Четвертый");

            // Дожидаемся commit-а
            rollbackKafkaSource.shutDown().toCompletableFuture().get();

            // Сбрасываем счетчик и данные в TestApiSink
            testApiSink.reset();
            // Запускаем ApiEngine повторно
            apiEngineFromRollbackKafkaSource.run();
            // Ждем KAFKA_CONSUMER_TIMEOUT_MS времени пока ожидание не прервется по таймауту
            assertThrows(ConditionTimeoutException.class,
                    () -> await().atMost(KAFKA_CONSUMER_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                            .until(testApiSink.processed(1)));
            // Проверяем, что ни одной записи не обработано
            testApiSink.check();
        } finally {
            // Корректно завершаем работу KafkaSource
            rollbackKafkaSource.shutDown().toCompletableFuture().get();
        }
    }

    /**
     * Цель теста проверить выполнение commit при использовании KafkaSource с autoCommit=true
     * Результаты теста полностью идентичны when_processingWithoutException_then_allItemsCommited()
     * Кидаем в топик 4 записи и дважды запускаем поток, читающий этот топик
     * Проверяем, что при повторном запуске потока ни одной записи не считано
     */
    @Test
    protected void when_processingWithAutoCommit_then_allItemsCommited()
            throws ExecutionException, InterruptedException {
        try {
            // Кидаем записи в топик
            List<String> messages = Arrays.asList("Первый", "Второй", "Третий", "Четвертый");
            messages.forEach(message -> stringKafkaTemplate
                    .send("when_processingWithAutoCommit_then_allItemsCommited_topic", message));
            // Запускаем ApiEngine
            apiEngineFromAutoCommitKafkaSource.run();
            // Ждем максимум KAFKA_CONSUMER_TIMEOUT_MS, давая время брокеру Кафка обнаружить Consumer
            await().atMost(KAFKA_CONSUMER_TIMEOUT_MS, TimeUnit.MILLISECONDS).until(
                    testApiSink.processed(4));
            // Проверяем, что считаны все записи, помещенные в топик
            testApiSink.check("Первый", "Второй", "Третий", "Четвертый");
            // Корректно завершаем работу KafkaSource
            autoCommitKafkaSource.shutDown().toCompletableFuture().get();

            // Сбрасываем счетчик и данные в TestApiSink
            testApiSink.reset();
            // Запускаем ApiEngine повторно
            apiEngineFromAutoCommitKafkaSource.run();
            // Ждем KAFKA_CONSUMER_TIMEOUT_MS времени пока ожидание не прервется по таймауту
            assertThrows(ConditionTimeoutException.class,
                    () -> await().atMost(KAFKA_CONSUMER_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                            .until(testApiSink.processed(4)));
            // Проверяем, что ни одной записи не обработано
            testApiSink.check();
        } finally {
            // Корректно завершаем работу KafkaSource
            autoCommitKafkaSource.shutDown().toCompletableFuture().get();
        }
    }

    /**
     * Цель теста проверить выполнение commit даже при возникновении Exception с KafkaSource с autoCommit=true
     * Кидаем в топик 4 записи, на 2-й записи происходит Exception и поток прерывается.
     * Поскольку за один вызов poll все 4 записи считались и указана опция autoCommit=true, то
     * несмотря на то что поток прервался commit произошел еще до того как поток прервался
     * Опция max-batch-size=1 не имеет значения, так как commit происходит автоматически, а не через Commiter
     */
    @Test
    protected void when_processingExceptionWithAutoCommit_then_allItemsCommited()
            throws ExecutionException, InterruptedException {
        try {
            // Имитируем Exception на 2-м элементе потока
            doThrow(new RuntimeException("Error while processing string"))
                    .when(testApiSink).process(argThat(value -> value.equals("Второй")));
            // Кидаем записи в топик
            List<String> messages = Arrays.asList("Первый", "Второй", "Третий", "Четвертый");
            messages.forEach(message -> stringKafkaTemplate
                    .send("when_processingWithAutoCommit_then_allItemsCommited_topic", message));
            // Запускаем ApiEngine
            apiEngineFromAutoCommitKafkaSource.run();
            // Ждем максимум KAFKA_CONSUMER_TIMEOUT_MS, давая время брокеру Кафка обнаружить Consumer
            await().atMost(KAFKA_CONSUMER_TIMEOUT_MS, TimeUnit.MILLISECONDS).until(testApiSink.processed(1));
            // Проверяем, что обработана только первая запись, так как на второй поток прервался
            testApiSink.check("Первый");
            // Закрываем KafkaSource и проверяем Exception
            ExecutionException executionException = assertThrows(ExecutionException.class,
                    () -> autoCommitKafkaSource.shutDown().toCompletableFuture().get());
            assertThat(executionException.getCause().getMessage()).isEqualTo("Error while processing string");

            // Сбрасываем имитацию Exception
            Mockito.reset(testApiSink);
            // Сбрасываем счетчик и данные в TestApiSink
            testApiSink.reset();
            // Запускаем ApiEngine повторно
            apiEngineFromAutoCommitKafkaSource.run();
            // Ждем KAFKA_CONSUMER_TIMEOUT_MS и убеждаемся, что ни одной записи не обработано
            assertThrows(ConditionTimeoutException.class,
                    () -> await().atMost(KAFKA_CONSUMER_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                            .until(testApiSink.processed(1)));
            testApiSink.check();
        } finally {
            // Корректно завершаем работу KafkaSource
            autoCommitKafkaSource.shutDown().toCompletableFuture().get();
        }
    }

    /**
     * Цель теста проверить как работает перезапуск KafkaSource в случае ошибки
     * KafkaSource настроен с опцией восстановления потока.
     * Дважды эмулируем Exception в потоке, он прерывается и запускается снова
     * В третий раз сообщения успешно обрабатываются
     * Первый раз ждем до 30 секунд, чтобы прошло 3 итерации запуска-восстановления потока
     * При каждой итерации нужно время для обнаружения Consumer-а брокером
     * В случае KafkaSource с опцией восстановления потока обязательно вызывать shutdown() для гарантии commit-а!
     */
    @Test
    protected void when_recoverableKafkaSource_then_itRecover()
            throws ExecutionException, InterruptedException {
        try {
            // Имитируем Exception так, что дважды поток прервется, в а 3-й раз ошибки не будет
            doThrow(new RuntimeException("Error while processing string [first step]"))
                    .doThrow(new RuntimeException("Error while processing string [second step]"))
                    .doCallRealMethod()
                    .when(testApiMock).process(anyString());
            // Кидаем записи в топик
            List<String> messages = Arrays.asList("Первый", "Второй", "Третий", "Четвертый");
            messages.forEach(message -> stringKafkaTemplate
                    .send("when_recoverableKafkaSource_then_itRecover_topic", message));
            // Запускаем ApiEngine
            apiEngineFromRecoverableKafkaSource.run();
            // Долго ждем, чтобы трижды поток перезапустился и при каждом перезапуске брокер успел обнаружить Consumer
            await().atMost(RECOVERABLE_KAFKA_SOURCE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .until(testApiSink.processed(4));
            // Проверяем, что в итоге все записи успешно обработаны
            testApiSink.check("Первый", "Второй", "Третий", "Четвертый");
            // Закрываем KakfaSource
            recoverableKafkaSource.shutDown().toCompletableFuture().get();

            // Сбрасываем счетчик и данные в TestApiSink
            testApiSink.reset();
            // Запускаем ApiEngine повторно
            apiEngineFromRecoverableKafkaSource.run();
            // Ждем KAFKA_CONSUMER_TIMEOUT_MS и убеждаемся, что ни одной записи не обработано
            assertThrows(ConditionTimeoutException.class,
                    () -> await().atMost(KAFKA_CONSUMER_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                            .until(testApiSink.processed(1)));
            testApiSink.check();
        } finally {
            recoverableKafkaSource.shutDown().toCompletableFuture().get();
        }
    }
}
