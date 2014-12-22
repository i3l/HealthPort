package edu.gatech.i3l.HealthPort;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import com.microsoft.hsg.ConnectionFactory;
import com.microsoft.hsg.HVAccessor;
import com.microsoft.hsg.Request;

public class HealthVaultPort {

	static public String getCCD (String Record_id, String Person_id) {		
		
		if (Person_id == null || Record_id == null) {
			return null;
		}
		StringBuilder requestXml = new StringBuilder();
		requestXml.append("<info><group>");
		requestXml.append("<filter><type-id>9c48a2b8-952c-4f5a-935d-f3292326bf54</type-id></filter>");
		requestXml.append("<format><section>core</section><section>otherdata</section><xml/></format>");
		requestXml.append("</group></info>");

		Request request2 = new Request();
		request2.setMethodName("GetThings");
		request2.setOfflineUserId(Person_id);
		request2.setRecordId(Record_id);
		request2.setInfo(requestXml.toString());

		HVAccessor accessor = new HVAccessor();
		accessor.send(request2, ConnectionFactory.getConnection());
		InputStream is = accessor.getResponse().getInputStream();
		
		int i;
		char c;
		StringBuilder resString = new StringBuilder();
        
		try {
			while((i=is.read())!=-1)
			{
			   // converts integer to character
			   c=(char)i;
			   
			   // prints character
			   resString.append(c);
			   //System.out.print(c);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return  null;
		}
		
		//Remove the HV response tags
		String finalString;
		int intIndex = resString.indexOf("<Clinical");
		finalString = resString.substring(intIndex);
		intIndex = finalString.indexOf("<common>");
		finalString = finalString.substring(0, intIndex);
		//System.out.println(finalString);
		
		return finalString;	
	}
	static public String getThings (String code, String Record_id, String Person_id){
		if (Person_id == null || Record_id == null) {
			return null;
		}
		StringBuilder requestXml = new StringBuilder();
		requestXml.append("<info><group>");
		requestXml.append("<filter><type-id>"+code+"</type-id></filter>");
		requestXml.append("<format><section>core</section><section>otherdata</section><xml/></format>");
		requestXml.append("</group></info>");

		Request request2 = new Request();
		request2.setMethodName("GetThings");
		request2.setOfflineUserId(Person_id);
		request2.setRecordId(Record_id);
		request2.setInfo(requestXml.toString());

		HVAccessor accessor = new HVAccessor();
		accessor.send(request2, ConnectionFactory.getConnection());
		InputStream is = accessor.getResponse().getInputStream();
		
		int i;
		char c;
		StringBuilder resString = new StringBuilder();
        
		try {
			while((i=is.read())!=-1)
			{
			   // converts integer to character
			   c=(char)i;
			   // prints character
			   resString.append(c);
			   //System.out.print(c);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return  null;
		}
		
		return resString.toString();
	}
	
	static public ArrayList<String> getWeight (String Record_id, String Person_id){
		String responseStr = null;
		String Value = null;
		String Units = null;
		//String finalString = "";
		ArrayList<String> finalList = new ArrayList<String>();
		responseStr= getThings("3d34d87e-7fc1-4153-800f-f56592cb0d17",Record_id,Person_id);
		
		   try {
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(new InputSource(new StringReader(responseStr)));
				doc.getDocumentElement().normalize();
				NodeList nList = doc.getElementsByTagName("weight");
				
				for (int temp = 0; temp < nList.getLength(); temp++) {
					Node nNode = nList.item(temp);
					
					if (nNode.getNodeType() == Node.ELEMENT_NODE) {
						Element eElement = (Element) nNode;
						Value = eElement.getElementsByTagName("value").item(0).getTextContent();
						Units = null;
						if (eElement.getElementsByTagName("kg")!= null){
							Units = "kg";
						}
						else if (eElement.getElementsByTagName("lb")!= null){
							Units = "lbs";
						}
						else{
							Units = "N/A";
						}
					}
					finalList.add(Value);
					finalList.add(Units);
				}
			    } catch (Exception e) {
				e.printStackTrace();
			    }
		return finalList;
		
	}
	static public ArrayList<String> getHeight (String Record_id, String Person_id){
		String responseStr = null;
		String Value = null;
		String Units = null;
		ArrayList<String> finalList = new ArrayList<String>();
		responseStr= getThings("40750a6a-89b2-455c-bd8d-b420a4cb500b",Record_id,Person_id);
		
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new InputSource(new StringReader(responseStr)));
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("height");
			
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					Value = eElement.getElementsByTagName("value").item(0).getTextContent();
					Units = null;
					if (eElement.getElementsByTagName("m")!= null){
						Units = "m";
					}
					else if (eElement.getElementsByTagName("ft")!= null){
						Units = "ft";
					}
					else{
						Units = "N/A";
					}
				}
				finalList.add(Value);
				finalList.add(Units);
			}
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		return finalList ;	
	}
	static public ArrayList<String> getBloodPressure(String Record_id, String Person_id){
		String responseStr = null;
		String systolic = null;
		String diastolic = null;
		ArrayList<String> finalList = new ArrayList<String>();
		responseStr= getThings("ca3c57f4-f4c1-4e15-be67-0a3caf5414ed",Record_id,Person_id);
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new InputSource(new StringReader(responseStr)));
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("blood-pressure");
			
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					systolic = eElement.getElementsByTagName("systolic").item(0).getTextContent();
					diastolic= eElement.getElementsByTagName("diastolic").item(0).getTextContent();
				}
				finalList.add("Systolic Blood Pressure");
				finalList.add(systolic);
				finalList.add("mm[Hg]");
				finalList.add("Diastolic Blood Pressure");
				finalList.add(diastolic);
				finalList.add("mm[Hg]");
			}
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		return finalList;	
	}
	static public ArrayList<String> getLabResults(String Record_id, String Person_id){
		String responseStr = null;
		ArrayList<String> finalList = new ArrayList<String>();
		responseStr= getThings("5800eab5-a8c2-482a-a4d6-f1db25ae08c3",Record_id,Person_id);
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new InputSource(new StringReader(responseStr)));
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("results");
			
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					finalList.add(eElement.getElementsByTagName("date").item(0).getTextContent());
					nNode = nNode.getFirstChild().getNextSibling();
					finalList.add(nNode.getTextContent());
					nNode = nNode.getNextSibling();
					eElement = (Element) nNode;
					String[] split = (eElement.getElementsByTagName("display").item(0).getTextContent()).split("\\s+");
					if (split.length == 2){
						finalList.add(split[0]);
						finalList.add(split[1]);
					}
					else{
						finalList.add(split[0]);
						finalList.add("N/A");
					}
				}
			}
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		
		return finalList;	
	}
	/*
	static public String getVitals(String Record_id, String Person_id){
		String responseStr = null;
		responseStr= getThings("73822612-c15f-4b49-9e65-6af369e55c65",Record_id,Person_id);
		return responseStr;	
	}*/
	
	static public ArrayList<String> getCondition(String Record_id, String Person_id){
		String responseStr = null;
		ArrayList<String> finalList = new ArrayList<String>();
		responseStr= getThings("7ea7a1f9-880b-4bd4-b593-f5660f20eda8",Record_id,Person_id);
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new InputSource(new StringReader(responseStr)));
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("condition");
			
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					finalList.add(eElement.getElementsByTagName("name").item(0).getTextContent());
					nNode = nNode.getFirstChild().getNextSibling().getFirstChild().getNextSibling();
					eElement = (Element) nNode;
					finalList.add(eElement.getElementsByTagName("value").item(0).getTextContent());
				}
				
			}
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		
		return finalList;
		
	}
	
	static public ArrayList<String> getMedication(String Record_id, String Person_id){
		String responseStr = null;
	
		ArrayList<String> finalList = new ArrayList<String>();
		responseStr= getThings("30cafccc-047d-4288-94ef-643571f7919d",Record_id,Person_id);
		System.out.println(responseStr);
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new InputSource(new StringReader(responseStr)));
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("medication");
			
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					finalList.add(eElement.getElementsByTagName("name").item(0).getTextContent());
					nNode = nNode.getFirstChild().getNextSibling().getFirstChild();
					String[] split = (nNode.getTextContent().split("\\s+"));
					finalList.add(split[0]);
					finalList.add(split[1]);
					nNode = nNode.getParentNode().getNextSibling().getFirstChild();
					split = (nNode.getTextContent().split("\\s+"));
					finalList.add(split[0]);
					finalList.add(split[1]);
				}
				
			}
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		
		System.out.println(finalList);
		return finalList;
	}

}
