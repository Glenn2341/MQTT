
package MQTT;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 *
 * @author glennfindlay
 */
public class Client implements Runnable, Comparable<Client>{

    private Socket c_mySocket = null;
    private OutputStream c_outputStream;

    private InputStream c_inputStream;
    private InputStreamReader c_inputStreamReader;
    private BufferedReader c_bufferedReader;

    private MQTTBroker.Broker c_broker;
    
    private Client c_thisClient;
    
    @Override
    public int compareTo(Client otherClient){
        
        String myIP = getIP().toString();
        String otherIP = otherClient.getIP().toString();
       
        
        return myIP.compareTo(otherIP);
    }

    public Client(Socket socket, MQTTBroker.Broker broker) throws Exception {
        c_mySocket = socket;
        c_outputStream = c_mySocket.getOutputStream();

        c_inputStream = c_mySocket.getInputStream();
        c_inputStreamReader = new InputStreamReader(c_inputStream);
        c_bufferedReader = new BufferedReader(c_inputStreamReader);

        c_broker = broker;
        
        c_thisClient = this;

        new Thread(this).start();
      
    }

    public void run() {

        ArrayList<Integer> data = new ArrayList<Integer>();
        ArrayList<Integer> currentMessage = new ArrayList<Integer>();

        StringBuilder inputString = null;

        while (!c_mySocket.isClosed()) {

            try {

                // read input
                if (c_inputStream.available() > 0) {

                    do {
                        data.add(c_inputStream.read());
                    } while (c_inputStream.available() > 0);

               
                    
                    // process each message in the pipe, and remove it
                    while (!data.isEmpty()) {
                        int nextMessageLength = MQTTMessage.getNextMessageLength(data);
                   
                        
                        
                        try{
                        currentMessage = new ArrayList(data.subList(0, nextMessageLength));                   
                        data = new ArrayList(data.subList(nextMessageLength, data.size()));
                        }
                        catch(Exception e){
                            System.out.println("error in sublisting data: " + e);
                        }
                        
                        
                        try{
                        readMessage(currentMessage);
                        }
                        catch(Exception e){
                            System.out.println("error in reading message: " + e);
                        }
                        
                        
                        
                        
                    }

                }

            } catch (Exception e) {
                System.out.println("client socket read failed: " + e);
            }

        }

    }

    // reads the next MQTT message from the data stream and removes it
    // also updates the broker on the new messages
    private void readMessage(ArrayList<Integer> data) {

        int type;
        int totalLength;

        type = MQTTMessage.getNextMessageType(data);
        
     

        // identify the message, pass it to the broker and send back an acknowledgement
        if (type == MQTTCODES.PUBLISH.ordinal()) {
            Publish message = (new Publish(data));
            c_broker.processIncomingPublishMessage(message);
            // send PUBACK if QOS > 0
            if(message.getQOS() > 0){
                sendMessagetoClient(MQTTMessage.generatePUBACK(message.getID()));
            }
            
        } else if (type == MQTTCODES.SUBSCRIBE.ordinal()) {
            Subscribe message = new Subscribe(data, c_thisClient);

            c_broker.processSubscribeMessage(message, c_thisClient);
            
            
            // send SUBACK
            sendMessagetoClient(MQTTMessage.generateSUBACK(message.getID(), message.getQOSRequests() ));
            
        }
        else if (type == MQTTCODES.CONNECT.ordinal()){
            // generate connack send connack back to client           
            sendMessagetoClient(MQTTMessage.generateCONNACK());
            
        }
        else if (type == MQTTCODES.PINGREQ.ordinal()){
            sendMessagetoClient(MQTTMessage.generatePINGRESP());
        }
        

    }

    private void clearInput() {
        try {
            do {
                c_inputStream.read();
            } while (c_inputStream.available() > 0);
        } catch (Exception e) {
            System.out.println("error in Client clearInput " + e);
        }

    }

    public InetAddress getIP() {
        return c_mySocket.getInetAddress();
    }
    
    public void printData(ArrayList<Integer> data){
        for(Integer I: data){
            System.out.println(I);
        }
    }
    
    public void printData(byte[] data){
        for (int i = 0; i < data.length; i++) {
            System.out.println(data[i]);
        }
    }

    public boolean sendMessagetoClient(byte[] message) {
        
       // System.out.println("sendint client message: ");
       // printData(message);
        
         try {

         c_outputStream.write(message);
         c_outputStream.flush();
         } catch (Exception e) {
         System.out.println("error in sending message to client " + getIP() + " " + e);
         }
         
        return true;

    }

}
