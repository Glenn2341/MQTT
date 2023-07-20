
package MQTT;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author glennfindlay
 */
public class Publish extends MQTTMessage {

    private MQTTFixedHeader mp_fixedHeader = null;
    private int mp_varHeaderLength;
    private String mp_topicName = null;
    private int mp_ID = -1;
    private String mp_payload = null;

    public Publish(ArrayList<Integer> data) {

        //create parent
        super(data);
        mp_fixedHeader = super.getFixedHeader();

        // get variable header
        readVariableHeader(data);
        readPayload(data);

    }

    // given an input stream
    // find the length of the next MQTT variable header
    private void readVariableHeader(ArrayList<Integer> data) {

        int startIndex = mp_fixedHeader.m_fixedHeaderLength;
        int topicLength = 0;
        byte[] tempByteArray;

        Iterator<Integer> dataIterator = data.listIterator(startIndex);

        // variable header's first two lines are the length of the topic name
        topicLength = dataIterator.next() << 8; // first byte is the most significant 8 bits
        topicLength = topicLength | dataIterator.next();  // add on the less significant 8 bits

        //find the topic name
        tempByteArray = new byte[topicLength];

        for (int i = 0; i < topicLength; i++) {
            tempByteArray[i] = (dataIterator.next()).byteValue();
        }
        mp_topicName = (new String(tempByteArray, StandardCharsets.UTF_8));

        // find the message ID in the next 2 bytes if QOS 1
        if (mp_fixedHeader.m_QOS > 0) {

            mp_ID = dataIterator.next().byteValue() << 8; // first byte is the most significant 8 bits
            mp_ID = mp_ID | dataIterator.next();  // add on the less significant 8 bits

        }

        // header length is topic plus 2 byte topic length plus 2 byte ID message for QOS >0
        mp_varHeaderLength = (mp_fixedHeader.m_QOS > 0) ? topicLength + 4 : topicLength + 2;

    }

    //given an input stream
    // find the length of the next MQTT payload
    private void readPayload(ArrayList<Integer> data) {

        int startIndex = mp_varHeaderLength + mp_fixedHeader.m_fixedHeaderLength;
        int payloadSize = mp_fixedHeader.m_varLength - mp_varHeaderLength;

        Iterator<Integer> dataIterator = data.listIterator(startIndex);

        byte[] dataBytes = new byte[payloadSize];

        for (int i = 0; i < payloadSize; i++) {
            dataBytes[i] = dataIterator.next().byteValue();

        }
        mp_payload = (new String(dataBytes, StandardCharsets.UTF_8));

    }

    // output function
    // creates a new update message for clients subscribed to a topic
    public static byte[] generateNewBrokerUpdate(int QOS, String topicNameString,
            int ID, ArrayList<String> payload) throws Exception {

        ArrayList<Integer> messageBytesAL = new ArrayList<Integer>();

        int MQTTCode = MQTTCODES.PUBLISH.ordinal();

        // encode topic name to bytes 
        byte[] topicNameBytes = topicNameString.getBytes("UTF-8");

        // encode payload to bytes  
        ArrayList<Integer> payloadBytesAL = new ArrayList<Integer>();

        for (String s : payload) {

            byte[] tempArr = s.getBytes("UTF-8");

            for (int i = 0; i < tempArr.length; i++) {
                payloadBytesAL.add((int) tempArr[i]);
            }
        }

        // length of variable header and payload = topic + 2 'topic length' fields  + payload + 2 ID fields if QOS > 0
        int variableLength = topicNameBytes.length + 2 + payloadBytesAL.size() + ((QOS > 0) ? 2 : 0);

        // create fixed header line 1
        messageBytesAL.add((MQTTCode << 4) | (QOS << 1));

        // create 'remaining length ' field
        {

            int rLenBytes = 0;

            do {
                rLenBytes = variableLength % 128;
                variableLength = variableLength / 128;

                if (variableLength > 0) {
                    rLenBytes = rLenBytes | 128;
                }
                messageBytesAL.add(rLenBytes);

            } while (variableLength > 0);
        }

        // add 2 topic length fields
        messageBytesAL.add((topicNameBytes.length >> 8));      // 00111001 -> 0011

        messageBytesAL.add(topicNameBytes.length & 255);     // 00111001 -> 1001 

        // add topic name
        for (int i = 0; i < topicNameBytes.length; i++) {
            messageBytesAL.add((int) topicNameBytes[i]);
        }

        // add id
        if (QOS > 0) {
            messageBytesAL.add((ID >> 8));      // 00111001 -> 0011

            messageBytesAL.add(ID & 255);     // 00111001 -> 1001 
        }

        // add payload
        for (Integer i : payloadBytesAL) {
            messageBytesAL.add(i);
        }

        byte[] response = new byte[messageBytesAL.size()];

        for (int i = 0; i < response.length; i++) {
            response[i] = messageBytesAL.get(i).byteValue();
        }

        return response;
    }

    public int getQOS() {
        return mp_fixedHeader.m_QOS;
    }

    public int getID() {
        return mp_ID;
    }

    public String getTopicName() {
        return mp_topicName;
    }
    
    public String getPayload(){
        return mp_payload;
    }

    public void printValuesDebug() {
        super.printValuesDebug();
        System.out.println("var header length: " + mp_varHeaderLength);
        System.out.println("topic: " + mp_topicName);
        System.out.println("ID: " + mp_ID);
        System.out.println("payload: " + mp_payload);

    }

    public static void printData(ArrayList<Integer> data) {
        for (Integer I : data) {
            System.out.println(I);
        }
    }

    public static void printData(byte[] data) {
        for (int i = 0; i < data.length; i++) {
            System.out.println(data[i]);
        }
    }
}