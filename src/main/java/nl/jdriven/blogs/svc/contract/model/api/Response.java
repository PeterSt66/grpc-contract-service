package nl.jdriven.blogs.svc.contract.model.api;

import java.util.Collections;
import java.util.List;

public class Response<T> {
    @SuppressWarnings("rawtypes")
    public static final Response NOTFOUND = Response.of(Result.NOTFOUND, "Notfound",(Error)null);
    @SuppressWarnings("rawtypes")
    public static final Response OK = Response.of(Result.OK, "Done", (Error)null);

    private final Result result;
    private final String reason;

    private final List<Error> errors;
    private final T resultObject;

    private Response(Result result, String reason, List<Error> errors, T resultObject) {
        this.result = result;
        this.reason = reason;
        this.errors = errors;
        this.resultObject = resultObject;
    }

    public static <T> Response<T> of(Result result, String reason, List<Error> errors) {
        return new Response<>(result, reason, errors, null);
    }

    public static <T> Response<T> of(Result result, String reason, Error error) {
        return new Response<>(result, reason, Collections.singletonList(error), null);
    }

    public static <R> Response<R> of(Result result, String reason, R resultObject) {
        return new Response<>(result, reason, Collections.emptyList(), resultObject);
    }

    public static <R> Response<R> of(R resultObj) {
        return of(Result.OK, "Ok",  resultObj);
    }


    public Result getResult() {
        return result;
    }

    public String getReason() {
        return reason;
    }

    public List<Error> getErrors() {
        return errors;
    }

    public T getResultObject() {
        return resultObject;
    }

    public enum Result {
        UNKNOWN(0),
        OK(1),
        FAILED(2),
        NOTFOUND(3);

        private final int value;

        Result(int value) {
            this.value = value;
        }

        public static Result forNumber(int value) {
            switch (value) {
                case 0: return UNKNOWN;
                case 1: return OK;
                case 2: return FAILED;
                case 3: return NOTFOUND;
                default: return null;
            }
        }

        public int getNumber() {
            return value;
        }
    }
}
