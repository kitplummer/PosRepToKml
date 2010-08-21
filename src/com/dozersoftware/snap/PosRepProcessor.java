package com.dozersoftware.snap;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jboss.soa.esb.actions.AbstractActionLifecycle;
import org.jboss.soa.esb.helpers.ConfigTree;
import org.jboss.soa.esb.message.Message;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class PosRepProcessor extends AbstractActionLifecycle {

	public PosRepProcessor(ConfigTree config) {
	}

	public Message process(Message message) {
		
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			String raw = (String)message.getBody().get();
			Document doc = dBuilder.parse(new ByteArrayInputStream(raw.getBytes()));
			
			doc.getDocumentElement().normalize();
			NodeList body = doc.getElementsByTagName("BODY");
			Node e = body.item(0);
			JsonElement jse = new JsonParser().parse(e.getTextContent());
			System.out.println("JSON: " + jse.toString());
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*try {
			new ServiceInvoker("NormOut", "NormProcessor").deliverAsync(message);
		} catch (MessageDeliverException e) {
			e.printStackTrace();
		}*/
		//System.out.println("Kicking a PositionReport!");
		return message;
	}
	
	public void exceptionHandler(Message message, Throwable exception) {
		   System.out.println("!ERROR!");
		   System.out.println(exception.getMessage());
		   System.out.println("For Message: ");
		   System.out.println(message.getBody().get());
	}
}
