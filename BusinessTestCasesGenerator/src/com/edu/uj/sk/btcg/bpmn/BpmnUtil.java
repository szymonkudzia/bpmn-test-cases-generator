package com.edu.uj.sk.btcg.bpmn;

import java.io.UnsupportedEncodingException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.apache.commons.io.IOUtils;

import com.edu.uj.sk.btcg.logging.CLogger;

public class BpmnUtil {
	private static final CLogger logger = CLogger.getLogger(BpmnUtil.class);
	
	/**
	 * Make independent copy of given @model
	 * 
	 * @param model
	 * @return copy of @model
	 */
	public static BpmnModel clone(BpmnModel model) {
		String xml = toString(model);
		
		return toBpmnModel(xml);
	}
	
	
	
	/**
	 * Conver given model to xml
	 * 
	 * @param model
	 * @return xml
	 */
	public static String toString(BpmnModel model) {
		BpmnXMLConverter converter = new BpmnXMLConverter();
		String content;
		try {
			content = new String(converter.convertToXML(model), "UTF-8");
			
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e);
		}
		
		return content;
	}
	

	
	/**
	 * Parese xml and return BpmnModel structure
	 * 
	 * @param bpmnModelContent
	 * @return
	 */
	public static BpmnModel toBpmnModel(String bpmnModelContent) {
		BpmnXMLConverter converter = new BpmnXMLConverter();
		XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
		XMLStreamReader xmlStreamReader = null;

		try {
			xmlStreamReader = xmlInputFactory.createXMLStreamReader(IOUtils.toInputStream(bpmnModelContent));
			
		} catch (Exception e) {
			logger.warn("Exception during loading bpmn model", e);
		}

		return converter.convertToBpmnModel(xmlStreamReader);
	}
}
