package nl.jdriven.blogs.svc.contract.model.api;

import java.util.Arrays;
import java.util.List;

public class Error {
   private final String location;
   private final String code;
   private final List<String> args;

   public Error( String code, String location, List<String> args) {
      this.location = location;
      this.code = code;
      this.args = args;
   }

   public static Error of(String code, String location, String... args) {
      return new Error(code, location, Arrays.asList(args));
   }

   public String getLocation() {
      return location;
   }

   public String getCode() {
      return code;
   }

   public List<String> getArgs() {
      return args;
   }

}
