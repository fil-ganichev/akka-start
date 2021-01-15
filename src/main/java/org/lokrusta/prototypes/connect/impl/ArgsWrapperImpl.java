package org.lokrusta.prototypes.connect.impl;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.lokrusta.prototypes.connect.api.ApiCallArguments;
import org.lokrusta.prototypes.connect.api.ArgsWrapper;
import org.lokrusta.prototypes.connect.api.CallInfo;
import org.lokrusta.prototypes.connect.api.dto.ArrayApiCallArguments;
import org.lokrusta.prototypes.connect.api.dto.ObjectApiCallArguments;
import org.lokrusta.prototypes.connect.api.dto.StringApiCallArguments;

//todo убрать StringApiCallArguments
@Data
@EqualsAndHashCode
public class ArgsWrapperImpl implements ArgsWrapper {

    private ApiCallArguments apiCallArguments;
    private CallInfo callInfo;
    private String correlationId;
    private Exception exception;

    public static ArgsWrapperImpl of(String arg) {
        return new ArgsWrapperImpl(new StringApiCallArguments(arg), null);
    }

    public static ArgsWrapperImpl of(ApiCallArguments apiCallArguments) {
        return new ArgsWrapperImpl(apiCallArguments);
    }

    public static ArgsWrapperImpl of(Exception e) {
        return new ArgsWrapperImpl(e);
    }

    public static <T> ArgsWrapperImpl of(T value) {
        return new ArgsWrapperImpl(new ObjectApiCallArguments<T>(value));
    }

    public static <T> ArgsWrapperImpl of(T[] value) {
        return new ArgsWrapperImpl(new ArrayApiCallArguments(value));
    }

    public ArgsWrapperImpl withCorrelationId(String correlationId) {
        setCorrelationId(correlationId);
        return this;
    }

    private ArgsWrapperImpl(ApiCallArguments apiCallArguments, CallInfo callInfo) {
        this.apiCallArguments = apiCallArguments;
        this.callInfo = callInfo;
    }

    private ArgsWrapperImpl(ApiCallArguments apiCallArguments) {
        this.apiCallArguments = apiCallArguments;
    }

    private ArgsWrapperImpl(Exception e) {
        this.exception = e;
    }

    @Override
    public ApiCallArguments getApiCallArguments() {
        return apiCallArguments;
    }

    @Override
    public CallInfo getCallInfo() {
        return callInfo;
    }

    @Override
    public String getCorrelationId() {
        return correlationId;
    }

}
