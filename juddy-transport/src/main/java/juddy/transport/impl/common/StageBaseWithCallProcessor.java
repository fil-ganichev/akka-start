package juddy.transport.impl.common;

public abstract class StageBaseWithCallProcessor extends StageBase {

    public abstract <T> T withApiCallProcessor(ApiCallProcessor apiCallProcessor);

    public abstract TransportMode getTransportMode();
}
