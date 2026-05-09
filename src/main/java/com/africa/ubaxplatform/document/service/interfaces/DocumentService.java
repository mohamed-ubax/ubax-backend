package com.africa.ubaxplatform.document.service.interfaces;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.document.codeList.RefType;
import com.africa.ubaxplatform.document.entity.Document;
import java.util.List;
import java.util.UUID;

public interface DocumentService {

  Document generateContractPdf(UUID contractId, User generatedBy);

  Document generateInvoice(UUID paymentId, User generatedBy);

  Document generateReceipt(UUID paymentId, User generatedBy);

  List<Document> getDocumentsByRef(UUID refId, RefType refType);
}
