package nl.jdriven.blogs.svc.contract.model.api;

public class DetailedResult {
    public static final DetailedResult NOTFOUND = DetailedResult.of(Result.NOTFOUND, "Notfound");
    public static final DetailedResult OK = DetailedResult.of(Result.OK, "Done");

    private final Result result;
    private final String reason;

    private DetailedResult(Result result, String reason) {
        this.result = result;
        this.reason = reason;
    }

    public static DetailedResult of(Result result, String reason) {
        return new DetailedResult(result, reason);
    }

    public Result getResult() {
        return result;
    }

    public String getReason() {
        return reason;
    }

    public enum Result {
        UNKNOWN(0),
        OK(1),
        FAILED(2),
        NOTFOUND(3);

        public static Result valueOf(int value) {
            return forNumber(value);
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
        private final int value;

        private Result(int value) {
            this.value = value;
        }

        public int getNumber() {
            return value;
        }
    }
}
