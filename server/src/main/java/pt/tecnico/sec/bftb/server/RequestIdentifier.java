package pt.tecnico.sec.bftb.server;

import java.util.HashMap;
import java.util.HashSet;

public class RequestIdentifier {


    private String _replicaId;
    private String _sequenceNumber;
    private boolean echoed;
    private boolean delivered;
    private boolean readied;
    private int numberOfEchoes;
    private int numberOfReadies;
    HashSet<String> receivedEchoes = new HashSet<String>();
    HashSet<String> receivedReadies = new HashSet<String>();

    public RequestIdentifier(String replicaId, String sequenceNumber){
        _replicaId = replicaId;
        _sequenceNumber = sequenceNumber;
        echoed = false;
        delivered = false;
        readied = false;
        numberOfEchoes = 0;
        numberOfReadies = 0;
    }

    public String getReplicaId() {
        return _replicaId;
    }

    public String getSN() {
        return _sequenceNumber;
    }

    public boolean hasEchoed() {
        return echoed;
    }

    public void changeEchoed() {
        echoed = true;
    }

    public int getNumberOfEchoes() {
        return numberOfEchoes;
    }

    public int getNumberOfReadies() {
        return numberOfReadies;
    }

    public boolean receiveEcho(String senderId, int  faultsToTolerate) {
        if (!readied && !receivedEchoes.contains(senderId) ) {
            receivedEchoes.add(senderId);
            numberOfEchoes++;
            if (numberOfEchoes > ( (3 * faultsToTolerate) + 1) /2) {

                readied = true;
                return true;
            }
        }
        return false;
    }

    public int receiveReady(String senderId, int faultsToTolerate) {

        if (!delivered && !receivedEchoes.contains(senderId) ) {
            receivedReadies.add(senderId);
            numberOfReadies++;
            if (numberOfReadies > faultsToTolerate && !readied) {
                readied = true;
                return 1;
            }

            else if (numberOfReadies > 2 * faultsToTolerate  && !delivered) {
                delivered = true;
                return 2;
            }
        }
        return 0;
    }

}
