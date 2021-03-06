package br.ufba.dcc.wiser.soft_iot.broker_bottom;
import java.util.List;
import java.util.Random;

import javax.naming.ServiceUnavailableException;
import javax.xml.bind.JAXBException;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Comparator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static java.util.Map.Entry.*;

import br.ufba.dcc.wiser.soft_iot.tatu.TATUWrapper;

import br.ufba.dcc.wiser.soft_iot.entities.Device;

public class Listener implements IMqttMessageListener {
	
	private boolean debugModeValue;
	private ControllerImpl impl;
	private ClientMQTT clienteMQTT;
	Map<String,Map<String,Integer>> topk_k_scoresByIdrequi = new HashMap<String,Map<String,Integer>>(); // Lista de Top-ks de Requis diferentes
	
	

    public Listener(ControllerImpl impl, ClientMQTT clienteMQTT, String topico, int qos) {
        clienteMQTT.subscribe(qos, this, topico);
        this.clienteMQTT = clienteMQTT;
        this.impl = impl;
    }
    
    public Listener(ControllerImpl impl) {       
        this.impl = impl;
    }

    @Override
    public synchronized void messageArrived( final String topic, final MqttMessage message) throws Exception {
    	
    	final String [] params = topic.split("/");
    	final int k = Integer.valueOf(params[2]); // K valores TOP
    	
    	System.out.println("=========================================");
    	
    	this.topk_k_scoresByIdrequi.put(params[1], null);
    	
        new Thread(new Runnable() {
			public void run() {
				String messageContent = new String(message.getPayload());
				printlnDebug("topic: " + topic + "message: " + messageContent);
				
				if(params[0].equals("TOP_K_HEALTH")) {
					System.out.println("REQUI: TOP_K_HEALTH | " + " ID = " + params[1] + " | k = " + k);
//					int qtdDevices = impl.getListDevices().size();
					Map<String,Integer> scores = new HashMap<String,Integer>();
					System.out.println("CALCULATING SCORES FROM DEVICES..");
					
					// Consumindo apiIot para pegar os valores mais atualizados dos dispositivos
					impl.updateValuesSensors();
					
					for(Device d: impl.getListDevices()) {
						// Lógica de usar função personalizável aqui. Porém foi usado uma função aleatória para gerar scores randômicos.
						Random random = new Random();
						int x = random.nextInt(51);
						scores.put(d.getId(), x);
					}
					
					Object[] a = scores.entrySet().toArray();
					
					Arrays.sort(a, new Comparator<Object>() {
					    @SuppressWarnings("unchecked")
						public int compare(Object o1, Object o2) {
					        return ((Map.Entry<String, Integer>) o2).getValue()
					                   .compareTo(((Map.Entry<String, Integer>) o1).getValue());
					    }
					});
					
					for (Object e : a) {
					    System.out.println(((Map.Entry<String, Integer>) e).getKey() + " : "
					            + ((Map.Entry<String, Integer>) e).getValue());
					}
					
					Map<String,Integer> top_k = new HashMap<String,Integer>();
					
					// Pegando os k piores ...
					for (int i = 0; i< k; i++) {
						Map.Entry<String, Integer> e = (Map.Entry<String, Integer>) a[i];
						top_k.put(e.getKey(), e.getValue());
						
					}
					
					topk_k_scoresByIdrequi.put(params[1], top_k);
					
					System.out.println("TOP_K => " + top_k.toString());
					System.out.println("=========================================");
					byte[] b = top_k.toString().getBytes();					
					clienteMQTT.publicar("TOP_K_HEALTH_RES/" + params[1], b, 1);
				}
			}
		}).start();
    }
    
    public void calcScores() {
    	List<Device> listDevices = this.impl.getListDevices();
    	for (int i = 0; i < listDevices.size(); i++) {
			System.out.println(listDevices.get(i));
		}
    }
    
    private void printlnDebug(String str){
		if (debugModeValue)
			System.out.println(str);
	}

}