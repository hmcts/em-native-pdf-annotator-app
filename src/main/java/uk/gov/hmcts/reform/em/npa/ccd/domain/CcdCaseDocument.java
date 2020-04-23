package uk.gov.hmcts.reform.em.npa.ccd.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class CcdCaseDocument {
    private String documentName;
    private String documentType;
    private CcdDocument documentLink;
    private LocalDateTime createdDatetime;
    private long size;
    private String createdBy;
//            {
//                "value": {
//                    "documentName": "Prosecution doc 2",
//                    "documentType": "Prosecution",
//                    "documentLink": {
//                        "document_url": "documentUrl",
//                        "document_filename": "prosecution2.pdf",
//                        "document_binary_url": "documentUrl/binary"
//                    },
//                    "createdDatetime": "2019-02-07T12:05:20.000",
//                    "createdBy": "Jeroen"
//                }
//            }
}
