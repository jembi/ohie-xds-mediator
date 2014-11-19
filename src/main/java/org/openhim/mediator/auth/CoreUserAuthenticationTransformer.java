package org.openhim.mediator.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.transformer.AbstractMessageTransformer;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * A transformer for setting up the authentication headers required for HIM Core API access.
 * 
 * Setup the user and password via properties.
 */
public class CoreUserAuthenticationTransformer extends AbstractMessageTransformer {
    
    private static final String AUTH_URL_PATTERN = "https://%s:%s/authenticate/%s";

    Logger log = Logger.getLogger(this.getClass());
    
    private String coreHost = "localhost";
    private String coreAPIPort = "8080";
    private String user;
    private String password;

    
    @SuppressWarnings("unused")
    private static class AuthResponse {
        String salt;
        String ts;

        public void setSalt(String salt) {
            this.salt = salt;
        }
        public void setTs(String ts) {
            this.ts = ts;
        }
    }

    private static String hash(String s) throws NoSuchAlgorithmException {
        //thanks to http://www.mkyong.com/java/java-sha-hashing-example/

        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update(s.getBytes());

        byte[] byteData = md.digest();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < byteData.length; i++) {
            sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }
    
    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException {
        try {
            String coreAuthURL = String.format(AUTH_URL_PATTERN, coreHost, coreAPIPort, user);
            log.info("Authenticating to OpenHIM Core: " + coreAuthURL);
            MuleMessage auth = muleContext.getClient().request(coreAuthURL, -1);
            AuthResponse authResponse = new ObjectMapper().readValue(auth.getPayloadAsString(), AuthResponse.class);

            String passHash = hash(authResponse.salt + password);
            String token = hash(passHash + authResponse.salt + authResponse.ts);
            
            message.setProperty("auth-username", user, PropertyScope.OUTBOUND);
            message.setProperty("auth-ts", authResponse.ts, PropertyScope.OUTBOUND);
            message.setProperty("auth-salt", authResponse.salt, PropertyScope.OUTBOUND);
            message.setProperty("auth-token", token, PropertyScope.OUTBOUND);
        } catch (Exception e) {
            throw new TransformerException(this, e);
        }

        return message;
    }

    public String getCoreHost() {
        return coreHost;
    }

    public void setCoreHost(String coreHost) {
        this.coreHost = coreHost;
    }

    public String getCoreAPIPort() {
        return coreAPIPort;
    }

    public void setCoreAPIPort(String coreAPIPort) {
        this.coreAPIPort = coreAPIPort;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
