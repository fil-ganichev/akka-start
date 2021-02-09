package juddy.transport.impl.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import juddy.transport.api.args.ApiCallArguments;
import juddy.transport.api.args.ArgsWrapper;
import juddy.transport.api.args.CallInfo;
import juddy.transport.api.dto.ObjectApiCallArguments;
import juddy.transport.impl.args.Message;
import juddy.transport.impl.common.json.ObjectMapperUtils;
import juddy.transport.impl.error.ApiCallSerializationException;
import juddy.transport.impl.error.ApiException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiSerialilizer {

    private final ObjectMapper objectMapper;
    private static final Map<String, List<TypeReference>> apiTypes = new HashMap<>();


    public ApiSerialilizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ApiSerialilizer() {
        this(ObjectMapperUtils.createObjectMapper());
    }

    public String serializeArgs(ArgsWrapper argsWrapper) {
        try {
            return new String(Base64.getEncoder().encode(objectMapper
                    .writeValueAsBytes(ArgsWrapperWrapper.of(argsWrapper))));
        } catch (Exception e) {
            throw new ApiCallSerializationException(e);
        }
    }


    public String toString(Object source) {
        try {
            return objectMapper.writeValueAsString(source);
        } catch (Exception e) {
            throw new ApiException(e);
        }
    }

    public <T> T fromString(String source, Class<T> clazz) {
        try {
            return objectMapper.readValue(source, clazz);
        } catch (Exception e) {
            throw new ApiException(e);
        }
    }

    public String messageToStringDecoded(Message source) {
        Message copy = Message.builder()
                .messageType(source.getMessageType())
                .correlationId(source.getCorrelationId())
                .base64Json(new String(Base64.getDecoder().decode(source.getBase64Json().getBytes())))
                .build();
        return toString(copy);
    }

    public String messageToString(Message source) {
        Message copy = Message.builder()
                .messageType(source.getMessageType())
                .correlationId(source.getCorrelationId())
                .base64Json(source.getBase64Json())
                .build();
        return toString(copy);
    }

    public Message messageFromString(String source) {
        return fromString(source, Message.class);
    }

    public ArgsWrapper parameterFromBase64String(String source) throws NoSuchMethodException, UnsupportedEncodingException {
        ArgsWrapperWrapper argsWrapperWrapper = fromString(new String(Base64.getDecoder().decode(source.getBytes()), "UTF-8"), ArgsWrapperWrapper.class);
        ArgsWrapper argsWrapper = ArgsWrapperWrapper.to(argsWrapperWrapper);
        return convertComplexArguments(argsWrapper);
    }

    public Message messageFromArgs(ArgsWrapper args) {
        Message message = Message.builder()
                .messageType(Message.MessageType.REQUEST)
                .correlationId(args.getCorrelationId())
                .base64Json(serializeArgs(args))
                .build();
        return message;
    }

    public Message welcome() {
        return Message.builder()
                .messageType(Message.MessageType.WELCOME)
                .base64Json(serializeArgs(ArgsWrapper.of("")))
                .build();
    }

    public void registerApiTypes(String apiName, List<TypeReference> types) {
        apiTypes.put(apiName, types);
    }

    private String apiName(Class apiClass, String methodName) {
        return apiClass.getName().concat(".").concat(methodName);
    }

    private ArgsWrapper convertComplexArguments(ArgsWrapper argsWrapper) {
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

    private Object convertValueOfComplexType(Object result, TypeReference type) {
        if (result == null) {
            return null;
        }
        return objectMapper.convertValue(result, type);
    }

    private boolean isResult(ArgsWrapper argsWrapper) {
        return argsWrapper.getCallInfo() == null;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ArgsWrapperWrapper {

        private ApiCallArguments apiCallArguments;
        private Class apiClass;
        private String methodName;
        private Class parameterTypes[];
        private String correlationId;
        private Exception exception;

        @JsonIgnore
        public static ArgsWrapperWrapper of(ArgsWrapper argsWrapper) {
            return ArgsWrapperWrapper.builder()
                    .apiCallArguments(argsWrapper.getApiCallArguments())
                    .correlationId(argsWrapper.getCorrelationId())
                    .exception(argsWrapper.getException())
                    .apiClass(argsWrapper.getCallInfo() == null ? null : argsWrapper.getCallInfo().getApiClass())
                    .methodName(argsWrapper.getCallInfo() == null ? null : argsWrapper.getCallInfo().getApiMethod().getName())
                    .parameterTypes(argsWrapper.getCallInfo() == null ? null : argsWrapper.getCallInfo().getApiMethod().getParameterTypes())
                    .build();
        }

        @JsonIgnore
        public static ArgsWrapper to(ArgsWrapperWrapper argsWrapperWrapper) throws NoSuchMethodException {
            ArgsWrapper argsWrapper = ArgsWrapper.of(argsWrapperWrapper.getApiCallArguments())
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
