package nl.jdriven.blogs.svc.contract.model.api;

import static com.google.common.base.Preconditions.checkNotNull;

public class Response<T> {

    private final DetailedResult result;
    private final T responseObject;

    public Response(DetailedResult result, T responseObject) {
        this.result = result;
        this.responseObject = responseObject;
    }

    public static <T> Response<T> of(DetailedResult result, T reference) {
        return new Response<T>(checkNotNull(result), checkNotNull(reference));
    }

    public static <T> Response<T> of(DetailedResult result) {
        return new Response<T>(result,null);
    }

    public T get() {
        checkNotNull(responseObject);
        return responseObject;
    }

    public DetailedResult getDetailedResult() {
        checkNotNull(result);
        return result;
    }
}
