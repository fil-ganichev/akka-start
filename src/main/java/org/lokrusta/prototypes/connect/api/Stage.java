package org.lokrusta.prototypes.connect.api;


/**
 * Общий интерфейс для всех компонент akka-коннектора
 *
 * @author Филипп Ганичев
 */
public interface Stage {

    ArgsWrapper next(ArgsWrapper argsWrapper);

    void init();
}
