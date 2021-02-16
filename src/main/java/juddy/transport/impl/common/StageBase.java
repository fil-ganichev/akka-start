package juddy.transport.impl.common;

import akka.NotUsed;
import akka.stream.javadsl.Flow;
import juddy.transport.api.args.ArgsWrapper;
import juddy.transport.api.common.Stage;
import juddy.transport.impl.context.ApiEngineContext;
import juddy.transport.impl.context.ApiEngineContextProvider;
import juddy.transport.impl.error.ErrorProcessor;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public abstract class StageBase implements Stage, InitializingBean {

    @Getter(AccessLevel.PROTECTED)
    private final ErrorProcessor errorProcessor = new ErrorProcessor();
    @Getter
    private Flow<ArgsWrapper, ArgsWrapper, NotUsed> stageConnector;
    @Getter
    @Setter
    private UnaryOperator<ArgsWrapper> argsConverter;
    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private ApiEngineContextProvider apiEngineContextProvider;

    protected StageBase() {
    }

    protected StageBase(UnaryOperator<ArgsWrapper> argsConverter) {
        this.argsConverter = argsConverter;
    }

    protected StageBase(UnaryOperator<ArgsWrapper> argsConverter, Consumer<Exception> errorListener) {
        this.argsConverter = argsConverter;
        errorProcessor.setErrorListener(errorListener);
    }

    @Override
    public void init() {
    }

    @Override
    @SuppressWarnings("checkstyle:hiddenField")
    public StageBase withArgsConverter(UnaryOperator<ArgsWrapper> argsConverter) {
        this.argsConverter = argsConverter;
        return this;
    }

    @Override
    public StageBase withErrorListener(Consumer<Exception> errorListener) {
        errorProcessor.setErrorListener(errorListener);
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
        return errorProcessor.onError(e);
    }

    protected ArgsWrapper checkError(ArgsWrapper argsWrapper) throws Exception {
        return errorProcessor.checkError(argsWrapper);
    }

    protected ApiEngineContext getApiEngineContext() {
        return apiEngineContextProvider.getApiEngineContext();
    }

    protected abstract Flow<ArgsWrapper, ArgsWrapper, NotUsed> createConnector();

    protected String logTitle(String subTitle) {
        return getClass().getSimpleName()
                .concat(" ")
                .concat(subTitle)
                .concat(": ");
    }
}
