package nl.jdriven.blogs.svc.contract;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.services.HealthStatusManager;
import io.perfmark.PerfMark;
import io.perfmark.traceviewer.TraceEventViewer;
import lombok.extern.slf4j.Slf4j;
import nl.jdriven.blogs.svc.contract.api.ContractServiceApi;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.IOException;

@Slf4j
public class ServerMain {
    public static void main(String[] args) throws IOException, InterruptedException {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        PerfMark.setEnabled(true);

        ContractServiceApi contractServiceApi = new ContractServiceApi();
        //ExceptionsServerInterceptor exceptionsServerInterceptor = new ExceptionsServerInterceptor();

        var hsm = new HealthStatusManager();
        var healthService = hsm.getHealthService();

        Server server = ServerBuilder.forPort(53000)
                //.addService(ServerInterceptors.intercept(contractServiceApi, exceptionsServerInterceptor))
                .addService(contractServiceApi)
                .addService(healthService)
                .build()
                .start();

        Runtime.getRuntime().addShutdownHook(new Thread(new OnShutdown(server)));

        log.info("Started listening for rpc calls on 53000...");
        server.awaitTermination();
        //server.awaitTermination(10, TimeUnit.SECONDS);

    }



    public static class OnShutdown implements Runnable {
        private Server server;

        public OnShutdown(Server server) {
            this.server = server;
        }

        @Override
        public void run() {
            System.err.println("Runtime stopped - writing trace and shutdown server");
            try {
                TraceEventViewer.writeTraceHtml();
            }
            catch (Exception e) {
                System.err.println("Could not write trace data");
            }
            server.shutdownNow();
        }
    }
}
