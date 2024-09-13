package hr.performancemanagement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class StartupCheck {

    @Value("${server.address:}")
    private String expectedAddress;

    @PostConstruct
    public void init() {
        try {
            InetAddress localAddress = InetAddress.getLocalHost();
            String actualAddress = localAddress.getHostAddress();

            if (!actualAddress.equals(expectedAddress)) {
                System.err.println("ERROR: Application is running on unexpected address but got: " + actualAddress);
                System.exit(1); // Exit with error code
            }
        } catch (UnknownHostException e) {
            System.err.println("ERROR: Unable to determine the local address.");
            System.exit(1); // Exit with error code
        }
    }
}
