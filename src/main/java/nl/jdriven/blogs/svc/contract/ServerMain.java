package nl.jdriven.blogs.svc.contract;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import nl.jdriven.blogs.svc.contract.api.ContractServiceApi;

import java.io.IOException;

public class ServerMain {
    public static void main(String[] args) throws IOException, InterruptedException {
        Server service = ServerBuilder.forPort(53000)
                .addService(new ContractServiceApi())
                .build()
                .start();

        Runtime.getRuntime().addShutdownHook(new Thread(service::shutdownNow));
        System.out.println("Started listening for rpc calls on 53000...");
        service.awaitTermination();
    }
}
