package org.corfudb.runtime.kv.core;

import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Structure representing the result of an operation executed by any service.
 */
public final class OperationResult<T> extends Result<T, RuntimeException> {

    private static final OperationResult<Void> VOID_RESULT = new OperationResult<>(null, null);

    private OperationResult(T result, RuntimeException error) {
        super(result, error);
    }

    public OperationResult(T result) {
        super(result);
    }

    public OperationResult(RuntimeException error) {
        super(error);
    }

    public static <T> OperationResult<T> of(T result) {
        return new OperationResult<>(result);
    }

    public static <T> OperationResult<T> of(Supplier<T> result) {
        try {
            return OperationResult.of(result.get());
        } catch (RuntimeException ex){
            return OperationResult.of(ex);
        }
    }

    public static <T> OperationResult<T> of(RuntimeException error) {
        return new OperationResult<>(error);
    }

    public static <T, E extends RuntimeException> OperationResult<T> of(Result<T, E> resultOrError){
        // Unwrap the innards of Result<T, E> and recompose into OperationResult.
        if (resultOrError.isPresent()) {
            return OperationResult.of(resultOrError.get());
        } else {
            return OperationResult.of(resultOrError.getError());
        }
    }

    public static <T> OperationResult<T> empty() {
        // Cannot optimize with a static singleton instance because stacktrace need to be preserved.
        // The same is true for not attempting to return a lazy/deferred instance (i.e. via
        // Supplier<> lambda) because the stacktrace of a deferred throwable construction would
        // result in the stacktrace of the invoker of OperationResult#get() rather than the
        // stacktrace of the invoker of OperationResult#empty().
        return OperationResult.of(new NoSuchElementException("No value present"));
    }

    @Override
    public <U> OperationResult<U> map(Function<? super T, ? extends U> function) {
        if (error != null) {
            return new OperationResult<>(error);
        } else if (isPresent()) {
            return new OperationResult<>(function.apply(value));
        } else {
            return empty();
        }
    }

    @Override
    public boolean isPresent() {
        if (this == VOID_RESULT) {
            return (error == null);
        } else {
            return super.isPresent();
        }
    }

    public static OperationResult<Void> voidResult() {
        return VOID_RESULT;
    }
}
