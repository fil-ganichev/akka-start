package juddy.transport.impl.common.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public final class ObjectMapperUtils {

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.S";

    private ObjectMapperUtils() {
    }

    public static ObjectMapper createObjectMapper() {
        return createObjectMapper(null);
    }

    public static ObjectMapper createObjectMapper(Consumer<ObjectMapper> objectMapperCustomizer) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)));
        javaTimeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)));
        objectMapper.registerModule(javaTimeModule);
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new ParameterNamesModule());
        if (objectMapperCustomizer != null) {
            objectMapperCustomizer.accept(objectMapper);
        }
        return objectMapper;
    }
}
