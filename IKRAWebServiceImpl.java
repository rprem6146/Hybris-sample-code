package com.uk.rnd.ws.order.impl;

import java.net.URL;
import java.util.Calendar;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.uk.rnd.core.ws.helper.RNDDocumentUploadHelper;
import com.uk.rnd.ws.constants.RNDSoapWSConstants;
import com.uk.rnd.ws.exception.WebserviceException;
import com.uk.rnd.ws.IKRA.CreateDocument;
import com.uk.rnd.ws.IKRA.Node;
import com.uk.rnd.ws.order.IKRAWebService;
import com.uk.rnd.ws.IKRA.BasicHttpBindingDocumentManagementQSService;
import com.uk.rnd.ws.IKRA.DocumentManagement;

import de.hybris.platform.acceleratorservices.config.SiteConfigService;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.servicelayer.internal.service.AbstractBusinessService;
import de.hybris.platform.servicelayer.model.ModelService;

/**
 * Implementation class of IKRAWebService that will make call to IKRA system to upload the document.
 * 
 */
 
public class IKRAWebServiceImpl extends AbstractBusinessService implements IKRAWebService {

     /** The RNDDocumentUploadHelper */
	@Autowired
    RNDDocumentUploadHelper rndDocumentUploadHelper;
    
	 /** The SiteConfigService */
	@Autowired
    private SiteConfigService siteConfigService;
    
	 /** The ModelService */
	@Autowired
    private ModelService modelService;
    
	/** Static constants */
	private static final Logger LOG = Logger.getLogger(IKRAWebServiceImpl.class);
	private static final String ERROR_CONNECTING_THE_IKRA_SYSTEM = "error connecting the IKRA System";
	
	/**
     * Method will make a call to IKRA system through soap ws, to upload the document at
     * IKRA end.
     * 
     * @param order
     * @throws WebserviceException
     */
    @Override
    public void makeUploadDocumentCallToIKRA(OrderModel order) throws WebserviceException {
	
		LOG.info("makeUploadDocumentCallToIKRA() ::Start for orderID:- " + order.getCode());
		try {

			final String wsdllocation = siteConfigService
				.getProperty(RNDSoapWSConstants.IKRA_WSDL_LOCATION);
			
			java.net.Authenticator.setDefault(new java.net.Authenticator() {

			@Override
			protected java.net.PasswordAuthentication getPasswordAuthentication() {
				return new java.net.PasswordAuthentication(siteConfigService
					.getProperty(RNDSoapWSConstants.IKRA_WS_USER_ID), siteConfigService.getProperty(
					RNDSoapWSConstants.IKRA_WS_PASSWORD).toCharArray());
			}
			});
			
			URL wsdlURL = new URL(wsdllocation);

			final QName SERVICE_NAME = new QName(
				siteConfigService.getProperty(RNDSoapWSConstants.IKRA_NAMESPACE_URI),
				RNDSoapWSConstants.IKRA_WSPROVIDER);
			final QName newportQname = new QName(
				siteConfigService.getProperty(RNDSoapWSConstants.IKRA_NAMESPACE_URI),
				RNDSoapWSConstants.IKRA_WSPROVIDER_PORT);

			final BasicHttpBindingDocumentManagementQSService serviceName = new BasicHttpBindingDocumentManagementQSService(
				wsdlURL, SERVICE_NAME);
			final DocumentManagement port = serviceName.getPort(newportQname, DocumentManagement.class);
			
			int currentYear = Calendar.getInstance().get(Calendar.YEAR);
			
			LOG.info("getNodeByName() ::Start for orderID:- " + order.getCode());
			
			//Getting Folder Id by GetNodeByName
			Node nodeByName = port.getNodeByName(siteConfigService.getInt(RNDSoapWSConstants.IKRA_NODE_PARENTID,0), String.valueOf(currentYear));
			
			int folderId;
			if(nodeByName != null){
			 folderId = nodeByName.getID();
			 LOG.info("Node Id created by IKRA is :- " + folderId);
			}
			else{
			 LOG.error("Node Id could not be created at IKRA");
			 throw new WebserviceException(ERROR_CONNECTING_THE_IKRA_SYSTEM);
			}
			
			CreateDocument documentIKRA = rndDocumentUploadHelper.makeRequestObjectForCreateDocumentIKRACall(order);
			
			//Creating document in IKRA by CreateDocument
			LOG.info("createDocument() ::Start for orderID:- " + order.getCode());
			Node documentNode = port.createDocument(folderId,
				documentIKRA.getName(),
				documentIKRA.getComment(),
				documentIKRA.isAdvancedVersionControl(),
				documentIKRA.getMetadata(),
				documentIKRA.getAttach());
			
			StringBuilder documentUrl = new StringBuilder();
			if(documentNode != null){
				int documentNodeId = documentNode.getID();
				
				//Preparing document url by appending service link and documentNode id
				documentUrl.append(siteConfigService.getProperty(RNDSoapWSConstants.IKRA_DOCUMENT_URL));
				documentUrl.append(documentNodeId);
				
				order.setVatExemptedCerificateDocURL(documentUrl.toString());
				
				//Removing doc from order after setting IKRA doc url
				modelService.remove(order.getVatExemptedCerificateDoc());
				order.setVatExemptedCerificateDoc(null);
				
				//Saving order with updated details
				modelService.save(order);
				modelService.refresh(order);
			
				LOG.info("Document uploaded successfully at IKRA with Document Id :- " + documentNodeId);
			}
			else{
				 LOG.info("Document could not be created at IKRA");
				 throw new WebserviceException(ERROR_CONNECTING_THE_IKRA_SYSTEM);
			}
		   
		} catch (final Exception ex) {
			LOG.error("Getting error for uploading the document at IKRA for order id ::" + order.getCode(), ex );

			throw new WebserviceException(ERROR_CONNECTING_THE_IKRA_SYSTEM, ex);
		}
    }
}