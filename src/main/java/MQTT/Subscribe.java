
package MQTT;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 * @author glennfindlay
 */
public class Subscribe extends MQTTMessage {

    public class MQTTSubscribeRequest {

        private String msr_topicName;
        private int msr_QOSRequested;
        private boolean msr_allSubTopics;
        private Client msr_client;

        private MQTTSubscribeRequest(String topic, int QOS, Client client) {
            msr_topicName = topic;
            msr_QOSRequested = QOS;

            // read & remove wildcard + preceding /
            msr_allSubTopics = (topic.endsWith("#")) ? true : false;
            msr_topicName = (topic.endsWith("#")) ? msr_topicName.substring(0, msr_topicName.length() - 2) : msr_topicName;

            msr_client = client;
        }

        public String getTopicName() {
            return msr_topicName;
        }

        public int getQOSRequested() {
            return msr_QOSRequested;
        }

        public Client getClient() {
            return msr_client;
        }

        public boolean getAllSubTopics() {
            return msr_allSubTopics;
        }
        
        public void printValuesDebug(){
            System.out.println("topic name " + msr_topicName);
            System.out.println("QOS " + msr_QOSRequested);
            System.out.println("allsubstopcs " + msr_allSubTopics);

        }

    }

    private MQTTFixedHeader ms_fixedHeader;
    private ArrayList<MQTTSubscribeRequest> ms_subRequests;
    private int ms_ID = -1;
    private int ms_varHeaderLength = 2;

    // constructor. given an input stream,
    // process an MQTT subscribe request form it
    public Subscribe(ArrayList<Integer> data, Client client) {

        super(data);

        ms_fixedHeader = super.getFixedHeader();

        ms_subRequests = new ArrayList<MQTTSubscribeRequest>();

        readVariableHeader(data);

        readPayload(data, client);

    }

    private void readVariableHeader(ArrayList<Integer> data) {

        int startIndex = ms_fixedHeader.m_fixedHeaderLength;
        int topicLength = 0;
        byte[] tempByteArray;

        Iterator<Integer> dataIterator = data.listIterator(startIndex);

        //read variable header's message ID
        // find the message ID in the next 2 bytes, as subscribe always has QOS level 1
        if (ms_fixedHeader.m_QOS > 0) {

            ms_ID = dataIterator.next().byteValue() << 8; // first byte is the most significant 8 bits
            ms_ID = ms_ID | dataIterator.next();  // add on the less significant 8 bits

        }
    }

    private void readPayload(ArrayList<Integer> data, Client client) {

        int startIndex = ms_varHeaderLength + ms_fixedHeader.m_fixedHeaderLength;
        int payloadSize = ms_fixedHeader.m_varLength - ms_varHeaderLength;

        byte[] tempByteArray;
        int topicLength;
        String topicName;
        int QOSRequested;
        int currentByte = startIndex;

        Queue<Integer> payloadData = new LinkedList(data.subList(startIndex, startIndex + payloadSize));

        while (!payloadData.isEmpty()) {

            // first two lines are the length of the topic name
            topicLength = (payloadData.poll()) << 8; // first byte is the most significant 8 bits
            topicLength = topicLength | payloadData.poll();  // add on the less significant 8 bits

            tempByteArray = new byte[topicLength];

            // next read the topic name itself
            for (int i = 0; i < topicLength; i++) {
                tempByteArray[i] = payloadData.poll().byteValue();
            }
            topicName = (new String(tempByteArray, StandardCharsets.UTF_8));

            // read the QOS
            QOSRequested = payloadData.poll();

            ms_subRequests.add(new MQTTSubscribeRequest(topicName, QOSRequested, client));
        }
    }

    public int getID() {
        return ms_ID;
    }

    public ArrayList getSubRequests() {
        return ms_subRequests;
    }

    public ArrayList<Integer> getQOSRequests() {

        ArrayList<Integer> QOSRequests = new ArrayList<Integer>();

        for (MQTTSubscribeRequest sr : ms_subRequests) {
            QOSRequests.add(sr.getQOSRequested());
        }

        return QOSRequests;
    }

    public void printValuesDebug() {

        System.out.println("MQTTSubscribe debug values:");

        System.out.println("ID " + ms_ID);

        for (MQTTSubscribeRequest sr : ms_subRequests) {
            System.out.print("topic: " + sr.getTopicName());
            System.out.print(" QOS requested: " + sr.getQOSRequested());
        }

    }

}
