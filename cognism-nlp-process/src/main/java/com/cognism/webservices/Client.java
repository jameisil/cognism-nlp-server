package com.cognism.webservices;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONObject;

@ManagedBean(name = "restNeoClient")
@SessionScoped//for testing
public class Client implements Serializable {

    private static final long serialVersionUID = 1L;

    public Client() {

    }
//web service test client example

    public void sendContent() {
        HttpClient httpClient = new DefaultHttpClient();
        try {
            HttpPost request = new HttpPost("http://localhost:1000/cognism-nlp-process/rest/nlp/post/content");
            JSONObject json = new JSONObject();
            json.put("content", "MICROSOFT  CORP  is  showing  strong  Earnings  Quality,  Cash Flow  Quality  and\n"
                    + "Operating Efficiency, and Valuation suggests a lower amount of price risk, but Balance\n"
                    + "Sheet Quality is weak. When combined, MSFT deserves a HOLD rating.\n"
                    + "The  Balance  Sheet  rating  improved  on  the  strength  of  better  liquidity.  Though  this\n"
                    + "dimension and all of the others were either strongeror unchanged at worst, it was not\n"
                    + "sufficient to raise the overall rating.Earnings  quality  has  long  been  analyzed  and  used  by  investors  as  a  measure  of  the  fundamental  quality  of  the  company  and  its\n"
                    + "future  prospects.  Companies  may  be  including  certain items  that  increase  reported  earnings  and  often  the amount  of  cash  flow\n"
                    + "supporting  the  earnings  may  be  weak.   Jefferson  adjusts  for  these  kinds  of  items  and  other  anomalies  to  produce  an  adjusted\n"
                    + "earnings  number  that  more  accurately  reflects  ongoing  business  fundamentals  at  MICROSOFT  CORP.  Reported  earnings  are\n"
                    + "compared to the Jefferson adjusted earnings as a means to gauge earnings quality. Also measured is the amount of cash flow that\n"
                    + "underpins earnings.\n"
                    + "The earnings quality for MSFT remains STRONG.\n"
                    + "With an adjusted net income of $5,253M in the last quarter that was greater than the reported number, MSFT's quality of net income\n"
                    + "earnings is extremely high. However, operating cash flow decreased during the last quarter to $8,354M from$9,514M, and the ratio\n"
                    + "of  operating  cash  flow  to  earnings  has  also  declined. Though  both  Earnings  Quality  measures  declined,  the  changes  were  not\n"
                    + "sufficient to lower the overall rating.");

            StringEntity params = new StringEntity(json.toString());

            request.addHeader("content-type", "application/json");
            request.addHeader("charset", "utf8");
            request.setEntity(params);
            HttpResponse response = (HttpResponse) httpClient.execute(request);
            HttpEntity entity = response.getEntity();

            String jsonOutput = EntityUtils.toString(entity);
            List<Cognitive> list = new ArrayList<Cognitive>();
            Cognitive userData = null;
            ObjectMapper mapper = new ObjectMapper();
            JsonFactory factory = mapper.getJsonFactory();
            JsonParser jp;
            jp = factory.createJsonParser(jsonOutput);
            JsonNode input = mapper.readTree(jp);
            Iterator<JsonNode> retList = input.getElements();
            while (retList.hasNext()) {
                userData = new Cognitive();
                JsonNode node = retList.next();
                String phrase = node.get("phrase").getTextValue();
                String sentiment = node.get("sentiment").getTextValue();
                String sentimentScore = node.get("sentimentScore").getTextValue();

                userData.setPhrase(phrase);
                userData.setSentiment(sentiment);
                userData.setSentimentScore(sentimentScore);
                list.add(userData);
                
            }
              System.out.println("Final list size : " + list.size());
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    static public long convertDateToLong(Date dateValue) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(dateValue);
        long millis = cal.getTimeInMillis();
        return millis;
    }

    public static void main(String args[]) {
        Client test = new Client();
        test.sendContent();

    }
}
