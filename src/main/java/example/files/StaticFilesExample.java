package example.files;

import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.file.FileService;
import com.linecorp.armeria.server.file.HttpFile;
import kamon.Kamon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;

public final class StaticFilesExample {

    private static final Logger logger = LoggerFactory.getLogger(StaticFilesExample.class);

    public static void main(String[] args) throws Exception {
        Kamon.loadModules();
        final Server server = newServer(8080, 8443);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop().join();
            logger.info("Server has been stopped.");
        }));

        server.start().join();
        logger.info("Server has been started.");
    }

    static Server newServer(int httpPort, int httpsPort) throws Exception {
        return Server.builder()
                     .http(httpPort)
                     .https(httpsPort)
                     .tlsSelfSigned()
                     // Serve an individual file.
                     .service("/favicon.ico",
                              HttpFile.of(StaticFilesExample.class.getClassLoader(), "/Users/lucas.amoroso/os/kamon-armeria-test/src/main/resources/favicon.ico")
                                      .asService())
                     // Serve the files under the current user's home directory.
                     .service("prefix:/",
                              FileService.builder(Paths.get(System.getProperty("user.home")))
                                         .autoIndex(true)
                                         .build())
                     .build();
    }

    private StaticFilesExample() {}
}
