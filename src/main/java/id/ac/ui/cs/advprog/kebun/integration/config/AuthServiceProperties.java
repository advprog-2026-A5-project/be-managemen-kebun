package id.ac.ui.cs.advprog.kebun.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mysawit.services.auth")
public class AuthServiceProperties {

    private String baseUrl;
    private String internalServiceToken = "dev-internal-token";
    
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getInternalServiceToken() {
    return internalServiceToken;
}

    public void setInternalServiceToken(String internalServiceToken) {
        this.internalServiceToken = internalServiceToken;
    }
}
