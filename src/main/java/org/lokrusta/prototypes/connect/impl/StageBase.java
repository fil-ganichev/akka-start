package org.lokrusta.prototypes.connect.impl;

import akka.NotUsed;
import akka.stream.javadsl.Flow;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.lokrusta.prototypes.connect.api.ArgsWrapper;
import org.lokrusta.prototypes.connect.api.Stage;
import org.springframework.beans.factory.InitializingBean;

import java.util.function.Consumer;
import java.util.function.Function;

@Getter(AccessLevel.PROTECTED)
@Setter(AccessLevel.PROTECTED)
public abstract class StageBase implements Stage, InitializingBean {

    private Flow<ArgsWrapper, ArgsWrapper, NotUsed> stageConnector;
    private Function<ArgsWrapper, ArgsWrapper> argsConverter;
    private Consumer<Exception> errorListener;

    protected StageBase() {
    }

    protected StageBase(Function<ArgsWrapper, ArgsWrapper> argsConverter) {
        this.argsConverter = argsConverter;
    }

    protected StageBase(Function<ArgsWrapper, ArgsWrapper> argsConverter, Consumer<Exception> errorListener) {
        this.argsConverter = argsConverter;
        this.errorListener = errorListener;
    }

    @Override
    public void init() {
    }

    @Override
    public StageBase withArgsConverter(Function<ArgsWrapper, ArgsWrapper> argsConverter) {
        this.argsConverter = argsConverter;
        return this;
    }

    @Override
    public StageBase withErrorListener(Consumer<Exception> errorListener) {
        this.errorListener = errorListener;
        return this;
    }

    @Override
    public ArgsWrapper next(ArgsWrapper argsWrapper) {
        if (argsConverter != null) {
            return argsConverter.apply(argsWrapper);
        }
        return argsWrapper;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.stageConnector = createConnector();
    }

    protected Exception onError(Exception e) throws Exception {
        if (errorListener != null) {
            errorListener.accept(e);
        }
        return e;
    }

    protected ArgsWrapper checkError(ArgsWrapper argsWrapper) throws Exception {
        if (argsWrapper.getException() != null) {
            throw argsWrapper.getException();
        }
        return argsWrapper;
    }

    protected abstract Flow<ArgsWrapper, ArgsWrapper, NotUsed> createConnector();

    protected String logTitle(String subTitle) {
        return getClass().getSimpleName()
                .concat(" ")
                .concat(subTitle)
                .concat(": ");
    }
}
