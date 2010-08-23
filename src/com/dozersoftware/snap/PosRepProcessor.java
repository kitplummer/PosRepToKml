package com.dozersoftware.snap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jboss.internal.soa.esb.persistence.format.MessageStoreFactory;
import org.jboss.soa.esb.Service;
import org.jboss.soa.esb.actions.AbstractActionLifecycle;
import org.jboss.soa.esb.actions.ActionProcessingException;
import org.jboss.soa.esb.helpers.ConfigTree;
import org.jboss.soa.esb.listeners.ListenerTagNames;
import org.jboss.soa.esb.message.Message;
import org.jboss.soa.esb.services.persistence.MessageStore;
import org.jboss.soa.esb.services.persistence.MessageStoreException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import de.micromata.opengis.kml.v_2_2_0.Kml;

public class PosRepProcessor extends AbstractActionLifecycle {

	protected ConfigTree _config;
    private Service service;
    protected Map<String, URI> map;
	protected MessageStore messageStore;
	
    public PosRepProcessor(ConfigTree config) {
		_config = config;
        service = new Service(_config.getParent().getAttribute(ListenerTagNames.SERVICE_CATEGORY_NAME_TAG), _config.getParent().getAttribute(ListenerTagNames.SERVICE_NAME_TAG));
        map = new HashMap<String, URI>();
        
        // Clean out the PosRep store...
        try {
			String messageStoreClass = "org.jboss.internal.soa.esb.persistence.format.db.DBMessageStoreImpl";
			messageStore = MessageStoreFactory.getInstance().getMessageStore(messageStoreClass);
			Map<URI, Message> pre = messageStore.getAllMessages("PosRep");
			Iterator<URI> it = pre.keySet().iterator();
			while(it.hasNext()) {
				messageStore.removeMessage(it.next(), "PosRep");
			}
		} catch (MessageStoreException e) {
			e.printStackTrace();
		}
        //Try to pull the message out
       
	}

	public Message process(Message message) {
		System.out.println("SNAP PosRep: " + message.getBody().get());
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			String raw = (String)message.getBody().get();
			Document doc = dBuilder.parse(new ByteArrayInputStream(raw.getBytes()));
			
			doc.getDocumentElement().normalize();
			String handle = doc.getDocumentElement().getAttribute("sender");
			if(this.map.containsKey(handle)){
				System.out.println("SNAP: Updating Platform PosRep...");
				messageStore.removeMessage(this.map.get(handle), "PosRep");
				URI uri = messageStore.addMessage(message, "PosRep");
				this.map.put(handle, uri);
			} else {
				System.out.println("SNAP: New PosRep...");
				URI uri = messageStore.addMessage(message, "PosRep");
				this.map.put(handle, uri);

			}
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MessageStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return message;
	}
	
	public Message http(Message message) throws ActionProcessingException {

        System.out.println("&&&&&&&&&&&&&&&& PosRepProcessor &&&&&&&&&&&&&&&&&&&&&");
        System.out.println("");
        System.out.println("Service: " + service);
        System.out.println("");
        //System.out.println("------------Http Request Info (XStream Encoded)-------------------");
		//HttpRequest requestInfo = HttpRequest.getRequest(message);
        //String requestInfoXML;

        //XStream xstream = new XStream();
        //requestInfoXML = xstream.toXML(requestInfo);

        //System.out.println(requestInfoXML);

		System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");

		try {
			StringWriter sw = new StringWriter();
			final Kml kml = new Kml();
			Map<URI, Message> msgs = messageStore.getAllMessages("PosRep");
			Iterator<URI> it = msgs.keySet().iterator();
			System.out.println("Tracking for :" + msgs.size() + " records");
			while (it.hasNext()) {
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				String raw = (String)msgs.get(it.next()).getBody().get();
				
				Document doc = dBuilder.parse(new ByteArrayInputStream(raw.getBytes()));
				
				doc.getDocumentElement().normalize();
				NodeList body = doc.getElementsByTagName("BODY");
				String handle = doc.getDocumentElement().getAttribute("sender");
				Node e = body.item(0);
				System.out.println("SNAP: " + handle + "'s PosRep -> " + e.getTextContent());
				JsonElement jse = new JsonParser().parse(e.getTextContent());
				if (jse.isJsonObject()) {
					System.out.println("We got the right thing: " + jse.toString());
					JsonArray ja = jse.getAsJsonObject().getAsJsonArray("POSREP");
					//{"POSREP": [16,"Aug 17, 2010 3:11:00 AM","31.74","-111.11"]}
					Double lat = ja.get(2).getAsDouble();
					Double lng = ja.get(3).getAsDouble();
					
					kml.createAndSetPlacemark()
						.withName(handle).withOpen(Boolean.TRUE)
						.createAndSetPoint()
						.addToCoordinates(lng, lat);
				} else {
					System.out.println("Not an Array!");
				}
				
				
			}

			System.out.println("Processed all positions...");
			kml.marshal(sw);        

			message.getBody().add(sw.toString());
		} catch (DOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MessageStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

        return message;

	}
	
	public void exceptionHandler(Message message, Throwable exception) {
		   System.out.println("!ERROR!");
		   System.out.println(exception.getMessage());
		   System.out.println("For Message: ");
		   System.out.println(message.getBody().get());
	}
}
