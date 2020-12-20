package org.lokrusta.prototypes.connect.impl;

import akka.NotUsed;
import akka.stream.javadsl.Flow;
import org.lokrusta.prototypes.connect.api.ArgsWrapper;
import org.lokrusta.prototypes.connect.api.Stage;

import java.util.function.Function;

public abstract class StageBase implements Stage {

    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> stageConnector;
    protected Function<ArgsWrapper, ArgsWrapper> argsConverter;

    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> getStageConnector() {
        return stageConnector;
    }

    @Override
    public void init() {
    }

    public StageBase withArgsConverter(Function<ArgsWrapper, ArgsWrapper> argsConverter) {
        this.argsConverter = argsConverter;
        return this;
    }

    @Override
    public ArgsWrapper next(ArgsWrapper argsWrapper) {
        if (argsConverter != null) {
            return argsConverter.apply(argsWrapper);
        }
        return argsWrapper;
    }
}
