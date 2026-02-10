package com.rezami.pdfmanager.util;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public interface TaskRunner {
    <T> void runAsync(Callable<T> task, Consumer<T> onSuccess, Consumer<Throwable> onFailure);
}

