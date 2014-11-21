package org.openhim.mediator.mule;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;

public class UDPConnector extends AbstractMessageTransformer {
    
    private String host;
    private String port;
    private boolean async = false;


    @Override
    public Object transformMessage(MuleMessage msg, String outputEncoding) throws TransformerException {
        DatagramSocket clientSocket = null;
        try {
            clientSocket = new DatagramSocket();
        } catch (Exception ex) {
            throw new TransformerException(this, ex);
        }

        try {
            InetAddress addr = InetAddress.getByName(host);
            byte[] payload = (msg.getPayloadAsString() + "\n").getBytes();
            DatagramPacket packet = new DatagramPacket(payload, payload.length, addr, Integer.parseInt(port));
            clientSocket.send(packet);
        
            if (!async) {
                byte[] buffer = new byte[1024*1014];
                DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
                clientSocket.receive(responsePacket);
                String response = new String(buffer);
                msg.setPayload(response);
                System.out.println("Response: " + response);
            } else {
                msg.setPayload(null);
            }
        } catch (Exception ex) {
            throw new TransformerException(this, ex);
        } finally {
            clientSocket.close();
        }

        return msg;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public boolean getAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }
}
