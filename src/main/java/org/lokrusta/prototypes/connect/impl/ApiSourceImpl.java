package org.lokrusta.prototypes.connect.impl;

import akka.NotUsed;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;
import org.lokrusta.prototypes.connect.api.ApiSource;
import org.lokrusta.prototypes.connect.api.ArgsWrapper;

public abstract class ApiSourceImpl<In> extends StageBase implements ApiSource<In> {

    protected abstract Source<In, NotUsed> createSource();

    protected abstract Source<In, NotUsed> getSource();
}
