package nl.jdriven.blogs.svc.contract.model.api;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class Error {
   private final String location;
   private final String code;
   private final Map<String,String> args;

   public Error( String code, String location,Map<String,String> args) {
      this.location = location;
      this.code = code;
      this.args = args;
   }

   public static Error of(String code, String location, String... args) {
      // very lazy way of getting args across, don't use on production code
      Map<String, String> argsMap = Arrays.stream(args)
              .map(a -> a.split(":"))
              .collect(Collectors.toMap(a -> a[0], a -> a[1]));
      return new Error(code, location, argsMap);
   }

   public String getLocation() {
      return location;
   }

   public String getCode() {
      return code;
   }

   public Map<String,String> getArgs() {
      return args;
   }

}
