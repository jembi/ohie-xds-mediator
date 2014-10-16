package org.openhim.mediator.mule;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoreResponseToken implements Serializable {
    private static final long serialVersionUID = 1699473476486043930L;

    public static class Request implements Serializable {
        private static final long serialVersionUID = 5650843911445158828L;

        private String path;
        private Map<String, String> headers = new HashMap<String, String>();
        private String queryString;
        private String body;
        private String method;
        private Date timestamp;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }
        
        public void putHeader(String name, String value) {
            headers.put(name, value);
        }

        public String getQueryString() {
            return queryString;
        }

        public void setQueryString(String queryString) {
            this.queryString = queryString;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public Date getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }
    }

    public static class Response implements Serializable {
        private static final long serialVersionUID = -2910898384372979044L;

        private String status;
        private Map<String, String> headers = new HashMap<String, String>();
        private String body;
        private Date timestamp;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }
        
        public void putHeader(String name, String value) {
            headers.put(name, value);
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public Date getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }
    }
    
    public static class Orchestration implements Serializable {
        private static final long serialVersionUID = -5774459111752593383L;

        private String name;
        private Request request;
        private Response response;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Request getRequest() {
            return request;
        }

        public void setRequest(Request request) {
            this.request = request;
        }

        public Response getResponse() {
            return response;
        }

        public void setResponse(Response response) {
            this.response = response;
        }
    }
        
    private String status;
    private Response response;
    private List<Orchestration> orchestrations = new ArrayList<Orchestration>();
    private Map<String, String> properties = new HashMap<String, String>();

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public List<Orchestration> getOrchestrations() {
        return orchestrations;
    }

    public void setOrchestrations(List<Orchestration> orchestrations) {
        this.orchestrations = orchestrations;
    }
    
    public void addOrchestration(Orchestration orchestration) {
        orchestrations.add(orchestration);
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
    
    public void putProperty(String name, String value) {
        properties.put(name, value);
    }
}
