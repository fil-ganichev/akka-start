package juddy.transport.impl;

import akka.stream.javadsl.Flow;
import juddy.transport.api.args.ArgsWrapper;
import juddy.transport.impl.args.ArgsWrapperImpl;
import juddy.transport.impl.client.ApiClientImpl;
import juddy.transport.impl.common.ApiCallProcessor;
import juddy.transport.impl.net.TcpClientTransportImpl;
import juddy.transport.impl.net.TcpServerTransportImpl;
import juddy.transport.impl.source.FileSource;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

public class TestBase {

    public ApiClientImpl apiClient(List<Class<?>> apiInterfaces) throws Exception {
        ApiClientImpl apiClientImpl = ApiClientImpl.of(apiInterfaces);
        apiClientImpl.afterPropertiesSet();
        return apiClientImpl;
    }

    public TcpClientTransportImpl tcpClientTransport(ApiCallProcessor apiCallProcessor, String host, int port) throws Exception {
        TcpClientTransportImpl tcpClientTransport = TcpClientTransportImpl.of(apiCallProcessor, host, port);
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
