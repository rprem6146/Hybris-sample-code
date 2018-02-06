package com.uk.rnd.ws.order;

import de.hybris.platform.core.model.order.OrderModel;

import com.uk.rnd.ws.exception.WebserviceException;

/**
 * Web service will make call to IKRA system to upload the document.
 * 
 */
public interface IKRAWebService {
    /**
     * Method will make a call to IKRA system through soap ws, to upload the document at
     * IKRA end.
     * 
     * @param order
     * @throws WebserviceException
     */
    void makeUploadDocumentCallToIKRA(OrderModel order) throws WebserviceException;
}