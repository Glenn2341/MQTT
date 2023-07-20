

package MQTT;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TreeMap;

enum MQTTCODES {

    RESERVED, CONNECT, CONNACK, PUBLISH, PUBACK, PUBREC, PUBREL, PUBCOMP,
    SUBSCRIBE, SUBACK, UNSUBSCRIBE, UNSUBACK, PINGREQ, PINGRESP, DISCONNECT;
    // MQTTCODES.PUBLISH.ordinal = 3
}

public abstract class MQTTMessage {

    private MQTTFixedHeader m_MQTTFixedHeader;

    public class MQTTFixedHeader {

        public int m_type;
        private String m_typeName;
        public int m_QOS;
        public int m_varLength = 0; // variable header and payload varLength
        public int m_fixedHeaderLength;

        // given a data stream
        // create a fied header form the next message
        public MQTTFixedHeader(ArrayList<Integer> data) {
            m_type = getNextMessageType(data);
            m_typeName = MQTTCODES.values()[m_type].toString();

            m_QOS = (byte) (data.get(0) >> 1 & 3);      // 00000010 -> 00000001. 0001 & 3 = 0001 & 0011 = 0001

            m_fixedHeaderLength = getNextFixedHeaderLength(data);
            m_varLength = getNextMessageLength(data) - m_fixedHeaderLength;

        }

    }

    public MQTTMessage(ArrayList<Integer> data) {

        m_MQTTFixedHeader = new MQTTFixedHeader(data);

    }

    public MQTTFixedHeader getFixedHeader() {
        return m_MQTTFixedHeader;
    }

    // take a data stream data
    // returns the type of the next message
    public static int getNextMessageType(ArrayList<Integer> data) {
        return (byte) (data.get(0) >> 4);
    }

    // take a data stream data
    // returns the length of the fixed header of the next message in bytes
    public static int getNextFixedHeaderLength(ArrayList<Integer> data) {

        // get remaining varLength
        int multiplier = 1;
        int val = 0;
        int currentByte = 0;
        int fixedHeaderLength = 1;
        // start one bye in because that's where the fixed header remaining length field is
        Iterator<Integer> dataIterator = data.listIterator(1);

        do {
            currentByte = (Integer) dataIterator.next();
            val += (currentByte & 127) * multiplier;
            multiplier *= 128;
            fixedHeaderLength++;
        } while (((currentByte & 128) != 0));

        return (fixedHeaderLength);

    }

    // take a data stream data
    // returns the length of the next message in bytes
    public static int getNextMessageLength(ArrayList<Integer> data) {

        // get remaining varLength
        int multiplier = 1;
        int value = 0;
        int digit = 0;
        int fixedHeaderLength = 1;
        // start one bye in because that's where the fixed header remaining length field is
        Iterator<Integer> dataIterator = data.listIterator(1);

        do {
            digit = (Integer) dataIterator.next();
            value += (digit & 127) * multiplier;
            multiplier *= 128;
            fixedHeaderLength++;
        } while (((digit & 128) != 0));

        return value + fixedHeaderLength;
    }

    public static byte[] generatePINGRESP() {

        byte[] response = new byte[2];

        byte pingRESPCode = (byte) MQTTCODES.PINGRESP.ordinal();

        response[0] = (byte) (pingRESPCode << 4);
        //line 2 is 0

        return response;
    }

    public static byte[] generateCONNACK() {

        byte CONNACKCode = (byte) MQTTCODES.CONNACK.ordinal();

        byte[] response = new byte[4];

        response[0] = (byte) (CONNACKCode << 4);     // shift the bits over

        response[1] = ((byte) 2);    // remaining length of 2

        // next byte unused
        // next byte is 0 for connection accepted code
        return response;
    }

    public static byte[] generatePUBACK(int messageID) {

        byte PUBACKCODE = (byte) MQTTCODES.PUBACK.ordinal();

        byte[] response = new byte[4];

        response[0] = (byte) (PUBACKCODE << 4);     // shift the bits over

        response[1] = ((byte) 2);    // remaining length of 2

        //next 2 bytes are message ID
        // PUBACK is send for QOS > 0, which will have an ID # 
        response[2] = (byte) (messageID >> 8);      // 00111001 -> 0011

        response[3] = (byte) (messageID & 255);     // 00111001 -> 1001              


        return response;
    }

    public static byte[] generateSUBACK(int messageID, ArrayList<Integer> QOSRequests) {

        byte SUBACKCODE = (byte) MQTTCODES.SUBACK.ordinal();

        // one byte for each QOS request, 2 for ID
        int variableSize = QOSRequests.size() + 2;

        ArrayList<Integer> tempAL = new ArrayList<Integer>();

        //fixed header type field
        tempAL.add((SUBACKCODE << 4));

        //fixed header remaing length field
        int rLenBytes = 0;

        do {
            rLenBytes = variableSize % 128;
            variableSize = variableSize / 128;

            if (variableSize > 0) {
                rLenBytes = rLenBytes | 128;
            }
            tempAL.add(rLenBytes);

        } while (variableSize > 0);
             
        
        tempAL.add((messageID >> 8));      // 00111001 -> 0011

        tempAL.add(messageID & 255);     // 00111001 -> 1001 

        
        // payload
         for (int j = 0; j < QOSRequests.size(); j++) {
         tempAL.add(QOSRequests.get(j));
         }
              
        
        byte[] response = new byte[tempAL.size()];

        for (int i = 0; i < tempAL.size(); i++) {
            response[i] = tempAL.get(i).byteValue();
        }  

        
        return response;
    }
    

    public void printValuesDebug() {
        System.out.println("type: " + m_MQTTFixedHeader.m_type);
        System.out.println(m_MQTTFixedHeader.m_typeName);
        System.out.println("QOS: " + m_MQTTFixedHeader.m_QOS);
        System.out.println("variable length: " + m_MQTTFixedHeader.m_varLength);
        System.out.println("fixed header length: " + m_MQTTFixedHeader.m_fixedHeaderLength);
    }

    public static void printData(byte[] data) {
        for (int i = 0; i < data.length; i++) {
            System.out.println(data[i]);
        }
    }

    public static void printData(ArrayList<Integer> data) {
        for (Integer I : data) {
            System.out.println(I);
        }
    }

}