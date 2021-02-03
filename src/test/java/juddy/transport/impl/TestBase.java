package juddy.transport.impl;

import akka.NotUsed;
import akka.stream.javadsl.Flow;
import juddy.transport.api.ArgsWrapper;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TestBase {

    public Flow<ArgsWrapper, ArgsWrapper, NotUsed> getConnector(StageBase stageBase) {
        return stageBase.getStageConnector();
    }

    public ApiCallProcessor getApiCallProcessor(ApiClientImpl apiClient) {
        return apiClient.getApiCallProcessor();
    }

    public ApiClientImpl apiClient(List<Class<?>> apiInterfaces) throws Exception {
        ApiClientImpl apiClientImpl = new ApiClientImpl(apiInterfaces
                .stream()
                .collect(Collectors.toMap(clazz -> clazz,
                        clazz -> ApiClientImpl.CallPoint.builder()
                                .api((Class<Object>) clazz)
                                .methods(Arrays.asList(clazz.getMethods()))
                                .build())));
        apiClientImpl.afterPropertiesSet();
        return apiClientImpl;
    }

    public TcpClientTransportImpl tcpClientTransport(ApiCallProcessor apiCallProcessor, String host, int port) throws Exception {
        TcpClientTransportImpl tcpClientTransport = new TcpClientTransportImpl(apiCallProcessor, host, port);
        tcpClientTransport.afterPropertiesSet();
        return tcpClientTransport;
    }

    public TcpServerTransportImpl tcpServerTransport(String host, int port) throws Exception {
        TcpServerTransportImpl tcpServerTransport = TcpServerTransportImpl.of(host, port);
        tcpServerTransport.afterPropertiesSet();
        return tcpServerTransport;
    }

    public FileSource fileSource(Path testFile) throws Exception {
        FileSource fileSource = new FileSource(testFile);
        fileSource.afterPropertiesSet();
        return fileSource;
    }

    public TcpServerTransportImpl runTcpServer(String host, int port, Function<ArgsWrapper, ArgsWrapper> processor) throws Exception {
        return runTcpServer(tcpServerTransport(host, port), processor);
    }

    public TcpServerTransportImpl runTcpServer(TcpServerTransportImpl tcpServerTransport, Function<ArgsWrapper, ArgsWrapper> processor) throws Exception {
        tcpServerTransport.run(Flow.of(ArgsWrapper.class).map(processor::apply));
        return tcpServerTransport;
    }

    public TcpServerTransportImpl runTcpServer(String host, int port) throws Exception {
        return runTcpServer(host, port, argsWrapper -> {
            ArgsWrapperImpl result = ArgsWrapperImpl.of(argsWrapper.getApiCallArguments()).withCorrelationId(argsWrapper.getCorrelationId());
            result.setCallInfo(null);
            return result;
        });
    }
}
