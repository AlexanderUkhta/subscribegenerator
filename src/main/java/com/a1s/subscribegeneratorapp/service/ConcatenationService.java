package com.a1s.subscribegeneratorapp.service;

import com.a1s.subscribegeneratorapp.model.SubmitSmData;
import com.cloudhopper.commons.gsm.GsmUtil;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.tlv.Tlv;
import com.google.common.collect.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

@Service
public class ConcatenationService {
    private static final Log logger = LogFactory.getLog(ConcatenationService.class);

    private Multimap<String, SubmitSmData> messageParts = ArrayListMultimap.create();

    @Autowired
    private RequestQueueService requestQueueService;

    public void processUdhConcatPart(final SubmitSm submitSm) {
        String msisdn = submitSm.getDestAddress().getAddress();
        String linkId = submitSm.getSourceAddress().getAddress();

        byte[] userDataHeader = GsmUtil.getShortMessageUserDataHeader(submitSm.getShortMessage());
        int udhReferenceId = userDataHeader[3] & 0xff;
        int udhPartsQuantity = userDataHeader[4] & 0xff;
        int udhCurrentPart = userDataHeader[5] & 0xff;

        String messageFullId = msisdn + linkId + udhReferenceId;

        messageParts.put(messageFullId, new SubmitSmData(udhCurrentPart,
                GsmUtil.getShortMessageUserData(submitSm.getShortMessage())));

        if (udhPartsQuantity == messageParts.get(messageFullId).size()) {
            byte[] finalMessage = concatenateUdhOrSar(messageParts.get(messageFullId));
            messageParts.asMap().remove(messageFullId); //todo: and add finalMessage[] to report

            requestQueueService.makeMsisdnNotBusyAndMakeReport(msisdn, finalMessage);
        }

    }

    public void processSarConcatPart(final SubmitSm submitSm) {
        String msisdn = submitSm.getDestAddress().getAddress();
        String linkId = submitSm.getSourceAddress().getAddress();

        int sarTotalSegments = 0;
        for (int i : submitSm.getOptionalParameter(SmppConstants.TAG_SAR_TOTAL_SEGMENTS).getValue()) {
            sarTotalSegments = sarTotalSegments * 10 + i;
        }

        int sarSegmentSeqnum = 0;
        for (int i : submitSm.getOptionalParameter(SmppConstants.TAG_SAR_SEGMENT_SEQNUM).getValue()) {
            sarSegmentSeqnum = sarSegmentSeqnum * 10 + i;
        }

        int sarMsgRefnum = 0;
        for (int i : submitSm.getOptionalParameter(SmppConstants.TAG_SAR_MSG_REF_NUM).getValue()) {
            sarMsgRefnum = sarMsgRefnum * 10 + i; //todo: проверить, нужно ли в цикле умножать на 10 ref_num
        }

        String messageFullId = msisdn + linkId + sarMsgRefnum;

        messageParts.put(messageFullId, new SubmitSmData(sarSegmentSeqnum,
                GsmUtil.getShortMessageUserData(submitSm.getShortMessage())));

        if (sarTotalSegments == messageParts.get(messageFullId).size()) {
            byte[] finalMessage = concatenateUdhOrSar(messageParts.get(messageFullId));
            messageParts.asMap().remove(messageFullId);

            requestQueueService.makeMsisdnNotBusyAndMakeReport(msisdn, finalMessage); //todo: and add finalMessage[] to report
        }

    }

    public void processPayloadConcatMessage(final SubmitSm submitSm) {
        Tlv messagePayload = submitSm.getOptionalParameter(SmppConstants.TAG_MESSAGE_PAYLOAD);
        byte[] shortMessage = messagePayload.getValue();

        requestQueueService.makeMsisdnNotBusyAndMakeReport(submitSm.getDestAddress().getAddress(), shortMessage);
    }

    public void processSimpleMessage(final SubmitSm submitSm) {
        byte[] shortMessage = submitSm.getShortMessage();

        requestQueueService.makeMsisdnNotBusyAndMakeReport(submitSm.getDestAddress().getAddress(), shortMessage);
    }

    /* combines parts of UDH/SAR-concatenated request */
    private byte[] concatenateUdhOrSar(final Collection<SubmitSmData> submitSmDataCollection) {

        /* We are creating a treeMap in order to sort ConcurrentHashMap keys */
        Map<Integer, byte[]> treeMap = new TreeMap<>();

        for(SubmitSmData oneData : submitSmDataCollection) {
            treeMap.put(oneData.getPartId(), oneData.getShortMessage());
        }

        ByteArrayOutputStream fullMultipartMessage = new ByteArrayOutputStream();

        for(Map.Entry<Integer, byte[]> entry : treeMap.entrySet()) {
            try {
                fullMultipartMessage.write(entry.getValue());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

       return fullMultipartMessage.toByteArray();

    }
}
