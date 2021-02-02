package org.lokrusta.prototypes.connect.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.lokrusta.prototypes.connect.api.*;
import org.lokrusta.prototypes.connect.api.dto.ObjectApiCallArguments;
import org.lokrusta.prototypes.connect.impl.error.ApiCallSerializationException;
import org.lokrusta.prototypes.connect.impl.error.ApiException;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiHelper {

    private static final ObjectMapper objectMapper = createMapper();
    private static final Map<String, List<TypeReference>> apiTypes = new HashMap<>();

    public static boolean isSource(Stage stage) {
        return stage instanceof ApiSource;
    }

    public static String serialize(ArgsWrapper argsWrapper) {
        try {
            return new String(Base64.getEncoder().encode(objectMapper.writeValueAsBytes(ArgsWrapperWrapperImpl.of(argsWrapper))));
        } catch (Exception e) {
            throw new ApiCallSerializationException(e);
        }
    }

    public static <T> T fromString(String source, Class<T> clazz) {
        try {
            return objectMapper.readValue(source, clazz);
        } catch (Exception e) {
            throw new ApiException(e);
        }
    }

    public static String toString(Object source) {
        try {
            return objectMapper.writeValueAsString(source);
        } catch (Exception e) {
            throw new ApiException(e);
        }
    }

    public static String messageToStringDecoded(Message source) {
        Message copy = Message.builder()
                .messageType(source.getMessageType())
                .correlationId(source.getCorrelationId())
                .base64Json(new String(Base64.getDecoder().decode(source.getBase64Json().getBytes())))
                .build();
        return toString(copy);
    }

    public static String messageToString(Message source) {
        Message copy = Message.builder()
                .messageType(source.getMessageType())
                .correlationId(source.getCorrelationId())
                .base64Json(source.getBase64Json())
                .build();
        return toString(copy);
    }

    public static Message messageFromString(String source) {
        return fromString(source, Message.class);
    }

    public static ArgsWrapper parameterFromBase64String(String source) throws NoSuchMethodException, UnsupportedEncodingException {
        ArgsWrapperWrapperImpl argsWrapperWrapper = fromString(new String(Base64.getDecoder().decode(source.getBytes()), "UTF-8"), ArgsWrapperWrapperImpl.class);
        ArgsWrapperImpl argsWrapper = ArgsWrapperWrapperImpl.to(argsWrapperWrapper);
        return convertComplexArguments(argsWrapper);
    }

    public static Message messageFromArgs(ArgsWrapper args) {
        Message message = Message.builder()
                .messageType(Message.MessageType.REQUEST)
                .correlationId(args.getCorrelationId())
                .base64Json(serialize(args))
                .build();
        return message;
    }

    public static Message welcome() {
        return Message.builder()
                .messageType(Message.MessageType.WELCOME)
                .base64Json(serialize(ArgsWrapperImpl.of("")))
                .build();
    }

    public static void registerApiTypes(String apiName, List<TypeReference> types) {
        apiTypes.put(apiName, types);
    }

    private static String apiName(Class apiClass, String methodName) {
        return apiClass.getName().concat(".").concat(methodName);
    }

    private static ArgsWrapper convertComplexArguments(ArgsWrapperImpl argsWrapper) {
        if (!isResult(argsWrapper)) {
            String apiName = apiName(argsWrapper.getCallInfo().getApiClass(), argsWrapper.getCallInfo().getApiMethod().getName());
            List<TypeReference> types = apiTypes.get(apiName);
            if (types != null) {
                ApiCallArguments apiCallArguments = argsWrapper.getApiCallArguments();
                if (apiCallArguments.getArgsType() == ApiCallArguments.Type.OBJECT) {
                    ((ObjectApiCallArguments) apiCallArguments).setValue(
                            convertValueOfComplexType(apiCallArguments.getResult(), types.get(0)));
                } else if (apiCallArguments.getArgsType() == ApiCallArguments.Type.ARRAY) {
                    Object[] arrayValues = (Object[]) apiCallArguments.getResult();
                    for (int i = 0; arrayValues != null && i < arrayValues.length; i++) {
                        arrayValues[i] = convertValueOfComplexType(arrayValues[i], types.get(i));
                    }
                }
            }
        }
        return argsWrapper;
    }

    private static Object convertValueOfComplexType(Object result, TypeReference typeReference) {
        if (result == null) {
            return null;
        }
        return objectMapper.convertValue(result, typeReference);
    }


    private static ObjectMapper createMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper;
    }

    private static boolean isResult(ArgsWrapperImpl argsWrapper) {
        return argsWrapper.getCallInfo() == null;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ArgsWrapperWrapperImpl {

        private ApiCallArguments apiCallArguments;
        private Class apiClass;
        private String methodName;
        private Class parameterTypes[];
        private String correlationId;
        private Exception exception;

        @JsonIgnore
        public static ArgsWrapperWrapperImpl of(ArgsWrapper argsWrapper) {
            return ArgsWrapperWrapperImpl.builder()
                    .apiCallArguments(argsWrapper.getApiCallArguments())
                    .correlationId(argsWrapper.getCorrelationId())
                    .exception(argsWrapper.getException())
                    .apiClass(argsWrapper.getCallInfo() == null ? null : argsWrapper.getCallInfo().getApiClass())
                    .methodName(argsWrapper.getCallInfo() == null ? null : argsWrapper.getCallInfo().getApiMethod().getName())
                    .parameterTypes(argsWrapper.getCallInfo() == null ? null : argsWrapper.getCallInfo().getApiMethod().getParameterTypes())
                    .build();
        }

        @JsonIgnore
        public static ArgsWrapperImpl to(ArgsWrapperWrapperImpl argsWrapperWrapper) throws NoSuchMethodException {
            ArgsWrapperImpl argsWrapper = ArgsWrapperImpl.of(argsWrapperWrapper.getApiCallArguments())
                    .withCorrelationId(argsWrapperWrapper.getCorrelationId());
            argsWrapper.setException(argsWrapperWrapper.getException());
            if (argsWrapperWrapper.getApiClass() != null) {
                argsWrapper.setCallInfo(CallInfo.builder()
                        .apiClass(argsWrapperWrapper.getApiClass())
                        .apiMethod(argsWrapperWrapper.getApiClass().getMethod(argsWrapperWrapper.getMethodName(), argsWrapperWrapper.getParameterTypes()))
                        .build());
            }
            return argsWrapper;
        }
    }
}
