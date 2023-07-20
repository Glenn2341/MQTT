
package MQTT;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 * @author glennfindlay
 */
public class MQTTBroker {

    public class Broker implements Runnable {

        ServerSocket s_serverSocket;


        TreeMap<String, Topic> s_topics;

        TreeSet<Client> s_clients;

        int s_publishInterval = 4000; // in milliseconds, time to wait between updating subscribers

        ArrayList<Subscribe.MQTTSubscribeRequest> s_subscriptions = new ArrayList<>();

        public Broker(int portNumber) {

            try {

                s_serverSocket = new ServerSocket(portNumber);

                s_topics = new TreeMap<String, Topic>();
                s_clients = new TreeSet<Client>();

            } catch (Exception e) {
                System.out.println("broker failure: " + e);
            }

        }

        // accept incoming connections
        public void run() {

            while (!s_serverSocket.isClosed()) {

                try {
                    s_clients.add(new Client(s_serverSocket.accept(), this));
                } catch (Exception e) {
                    System.out.println("Broker run ERROR: " + e);
                }

            }

        }

        // publish periodically to clients
        public void brokerMain() throws Exception {

            new Thread(this).start();
            System.out.println("broker created, awaiting connections..");

            s_topics.put("Temperature", new Topic("Temperature"));

            while (!s_serverSocket.isClosed()) {

                Thread.sleep(s_publishInterval);
                // update clients
                updateSubscribers();

            }
        }

        public void updateSubscribers() {

            // go through every subscription
            for (Subscribe.MQTTSubscribeRequest subreq : s_subscriptions) {

                String topicName = subreq.getTopicName();
                byte[] tempByteArr = null;

                // if the subscription has # wildcard
                if (subreq.getAllSubTopics()) {

                    tempByteArr = null;

                    for (Topic t : s_topics.values()) {
                        if (t.getName().startsWith(topicName)) {
                            tempByteArr = t.getData();
                            if (tempByteArr != null) {
                                subreq.getClient().sendMessagetoClient(tempByteArr);
                            }
                        }
                    }

                } else {
                    for (Topic t : s_topics.values()) {
                        if (t.getName().equals(topicName)) {
                            tempByteArr = t.getData();
                            if (tempByteArr != null) {
                                subreq.getClient().sendMessagetoClient(tempByteArr);
                            }
                        }
                    }
                }

            }

            // clear data now it's been published
            for (Topic t : s_topics.values()) {
                t.clearData();
            }

        }

        public void processIncomingPublishMessage(Publish pubMessage) {

            String topicName = pubMessage.getTopicName();

            // add the data to the appropriate topic; if it doesn't exist yet, create it
            if (s_topics.containsKey(topicName)) {
                s_topics.get(topicName).addData(pubMessage.getPayload());
            } else {
                Topic newTopic = new Topic(topicName);
                s_topics.put(topicName, newTopic);
                newTopic.addData(pubMessage.getPayload());

            }

        }

        public void processSubscribeMessage(Subscribe subMessage, Client client) {

            ArrayList<Subscribe.MQTTSubscribeRequest> subReqs = subMessage.getSubRequests();

            // for every subscribe request in the message
            for (Subscribe.MQTTSubscribeRequest subreq : subReqs) {

                s_subscriptions.add(subreq);
                String topicName = subreq.getTopicName();

                // if the topic doesn't exist, create it
                if (!s_topics.containsKey(topicName)) {

                    Topic newTopic = new Topic(topicName);
                    s_topics.put(topicName, newTopic);
                }

            }

        }

        

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {

        new MQTTBroker().new Broker(Integer.parseInt(args[0])).brokerMain();

    }

}


/* clear a port
 sudo lsof -i :1883

 sudo kill -9 PID

 */
