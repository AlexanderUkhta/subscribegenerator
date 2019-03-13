package com.a1s.subscribegeneratorapp.smsc;

import com.a1s.subscribegeneratorapp.service.RequestQueueService;
import com.cloudhopper.commons.gsm.GsmUtil;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.tlv.Tlv;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Service
public class ConcatenationService {
    private static final Log logger = LogFactory.getLog(ConcatenationService.class);

    private static ScheduledExecutorService concatTasksPool = Executors.newScheduledThreadPool(10);

    @Autowired
    private RequestQueueService requestQueueService;

    public void processConcatenationTask(final SubmitSm submitSm) {
        String msisdn = submitSm.getDestAddress().getAddress();

        if(msisdn.equals("already processing in tasks")) {
            //find appropriate task and add this SubmitSm to task's messagePartsMap


        } else if (submitSm.getEsmClass() == SmppConstants.ESM_CLASS_UDHI_MASK) { //if UDH
            byte[] userDataHeader = GsmUtil.getShortMessageUserDataHeader(submitSm.getShortMessage());
            Integer udhPartsQuantity = userDataHeader[4] & 0xff;
            Integer udhCurrentPart = userDataHeader[5] & 0xff;

            concatTasksPool.execute(new UdhConcatenationTask(msisdn, udhPartsQuantity, udhCurrentPart, submitSm));

        } else if (submitSm.getOptionalParameter(SmppConstants.TAG_SAR_TOTAL_SEGMENTS) != null) { //if SAR
            int sarPartsQuantity = 0;
            for (int i : submitSm.getOptionalParameter(SmppConstants.TAG_SAR_TOTAL_SEGMENTS).getValue()) {
                sarPartsQuantity = sarPartsQuantity * 10 + i;
            }

            int sarCurrentPart = 0;
            for (int i : submitSm.getOptionalParameter(SmppConstants.TAG_SAR_SEGMENT_SEQNUM).getValue()) {
                sarCurrentPart = sarCurrentPart * 10 + i;
            }

            concatTasksPool.execute(new SarConcatenationTask(msisdn, sarPartsQuantity, sarCurrentPart, submitSm));

        } else if (submitSm.getOptionalParameter(SmppConstants.TAG_MESSAGE_PAYLOAD) != null) {
            Tlv messagePayload = submitSm.getOptionalParameter(SmppConstants.TAG_MESSAGE_PAYLOAD);
            byte[] shortMessage = messagePayload.getValue();

            //then change msisdn state in map and add it to final document
            requestQueueService.makeMsisdnNotBusy(msisdn);
            logger.info("Got msisdn free again: " + msisdn);
        }
    }

    /* combines parts of UDH/SAR-concatenated request */
    public void concatenateUdhOrSar(ConcurrentHashMap<Integer, SubmitSm> map) {

        /* We are creating a treeMap in order to sort ConcurrentHashMap keys */
        Map<Integer, byte[]> treeMap = new TreeMap<>();
        String msisdn = map.get(1).getDestAddress().getAddress();

        for(Map.Entry<Integer, SubmitSm> entry : map.entrySet()) {
            treeMap.put(entry.getKey(), entry.getValue().getShortMessage());
        }

        ByteArrayOutputStream fullMultipartMessage = new ByteArrayOutputStream();

        for(Map.Entry<Integer, byte[]> entry : treeMap.entrySet()) {
            try {
                fullMultipartMessage.write(entry.getValue());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        byte[] finalConcatenatedMessage = fullMultipartMessage.toByteArray();

        // Now add (String)finalConcatenatedMessage to the final document
        requestQueueService.makeMsisdnNotBusy(msisdn);
        logger.info("Got msisdn free again: " + msisdn);
    }
}
