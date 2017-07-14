package posTagger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;

import com.sun.org.apache.xerces.internal.util.URI;
import com.sun.org.apache.xerces.internal.util.URI.MalformedURIException;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class tagger {
	
	static LinkedHashMap<String, Long> resultMap = new LinkedHashMap<>();
    static MaxentTagger tagger = new MaxentTagger("tagger/spanish-distsim.tagger");
    
	public static void main (String Args[]) throws IOException, URISyntaxException, InterruptedException, ExecutionException{
		String csvFile = "/Users/javier/Downloads/Per_a_Python/tweets_training.csv";
        BufferedReader br = null;
        String line = "";
        resultMap.put("", (long) 0);
    	resultMap.put("adjective", (long) 0);
    	resultMap.put("conjunction", (long) 0);
    	resultMap.put("determinier", (long) 0);
    	resultMap.put("punctuation", (long) 0);
    	resultMap.put("interjection", (long) 0);
    	resultMap.put("noun", (long) 0);
    	resultMap.put("pronoun", (long) 0);
    	resultMap.put("adverb", (long) 0);
    	resultMap.put("preposition", (long) 0);
    	resultMap.put("verb", (long) 0);
    	resultMap.put("date", (long) 0);
    	resultMap.put("numeral", (long) 0);
    	resultMap.put("other", (long) 0);
        try {
            br = new BufferedReader(new FileReader(csvFile));
            while ((line = br.readLine()) != null) {
                processLine(line);
            }
        	Iterator<Entry<String, Long>> it = resultMap.entrySet().iterator();
        	String lineOut="";
            while (it.hasNext()) {
                Entry<String, Long> pair = it.next();
                lineOut = lineOut + pair.getKey() +"\t";
            }
            FileWriter pw = new FileWriter("D:/output.csv",true /* append = true */); 
            pw.write(lineOut);
            pw.close();
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } 
	}
	public static LinkedHashMap<String, Long> processLine(String line) throws IOException, URISyntaxException, InterruptedException, ExecutionException{
        String cvsSplitBy = "\t";
        LinkedHashMap<String, Long> result = fillMap();
        String[] tweets = line.split(cvsSplitBy);

        
        ExecutorService service = Executors.newFixedThreadPool(70);
        List<Future<LinkedHashMap<String, Long>>> futures = new ArrayList<Future<LinkedHashMap<String, Long>>>();
        
        for (int i=0; i<tweets.length;i++) {
        	String auxTweet = tweets[i];
            Callable<LinkedHashMap<String, Long>> callable = new Callable<LinkedHashMap<String, Long>>() {
                public LinkedHashMap<String, Long> call() throws Exception {
                	LinkedHashMap<String, Long> output = processTweet(auxTweet);
                    // process your input here and compute the output
                    return output;
                }
            };
            futures.add(service.submit(callable));
        }
        service.shutdown();
        List<LinkedHashMap<String, Long>> outputs = new ArrayList<LinkedHashMap<String, Long>>();
        for (Future<LinkedHashMap<String, Long>> future : futures) {
            outputs.add(future.get());
        }
        for(LinkedHashMap<String, Long> output : outputs){
        	output.forEach((k, v) -> result.merge(k, v, (v1, v2) -> v1 + v2));
        }
        /**for(int i=0; i<tweets.length;i++){
        	aux = processTweet(tweets[i]);
        	aux.forEach((k, v) -> result.merge(k, v, (v1, v2) -> v1 + v2));
        }*/

    	Iterator<Entry<String, Long>> it = result.entrySet().iterator();
    	String lineOut="";
        while (it.hasNext()) {
            Entry<String, Long> pair = it.next();
            lineOut = lineOut + pair.getValue() +"\t";
        }
        FileWriter pw = new FileWriter("D:/output.csv",true /* append = true */); 
        pw.write(lineOut+"\r\n");
        pw.close();
	    System.out.println(result);
		return result;
	}
	
	Runnable task = () -> {
	    String threadName = Thread.currentThread().getName();
	    System.out.println("Hello " + threadName);
	};
	
	public static LinkedHashMap<String, Long> processTweet(String tweet) throws IOException, URISyntaxException{
	       String tagged = tagger.tagString(tweet);
	       String[] list = tagged.split(" ");
	       List<String> tagger;
	       for(int i=0; i<list.length;i++){
	    	   if(list[i].startsWith("http")){
	    		   list[i] = getUrl(list[i].substring(0, list[i].lastIndexOf('_')));
	    		   if(!resultMap.containsValue(list[i]) && list[i] != ""){
	    			   resultMap.put(list[i], (long) 0);
	    		   }
	    	   }
	    	   else{
	    		   list[i] = list[i].substring(list[i].lastIndexOf('_')+1);
	    		   switch(list[i].substring(0, 1)){
	    		   case "a": list[i]= "adjective"; break;
	    		   case "c": list[i]= "conjunction"; break;
	    		   case "d": list[i]= "determinier"; break;
	    		   case "f": list[i]= "punctuation"; break;
	    		   case "i": list[i]= "interjection"; break;
	    		   case "n": list[i]= "noun"; break;
	    		   case "p": list[i]= "pronoun"; break;
	    		   case "r": list[i]= "adverb"; break;
	    		   case "s": list[i]= "preposition"; break;
	    		   case "v": list[i]= "verb"; break;
	    		   case "w": list[i]= "date"; break;
	    		   case "z": list[i]= "numeral"; break;
	    		   default: list[i]= "other"; 
	    		   }
	    	   }
	       }
	       tagger = Arrays.asList(list);
	       Map<String, Long> result = tagger.stream()
	    		   .collect(
	    				   Collectors.groupingBy(Function.identity(), Collectors.counting()));
	       return new LinkedHashMap<String, Long>(result);
	}
	
	public static String getUrl (String url) throws IOException{// expande las url y devuelve su region
        String shortenedUrl = url;
        System.out.println(shortenedUrl);
        String expandedURL = expandSingleLevel(shortenedUrl);
        int i = 0;
        try{
	        while(expandedURL != null && !shortenedUrl.equals(expandedURL)){ //mientras se pueda expandir mas el bucle sigue (devolvera null cuando acabe)
	        	shortenedUrl = expandedURL;
	        	expandedURL = expandSingleLevel(shortenedUrl);
	        	System.out.println(++i + ": " + shortenedUrl + " -> " + expandedURL);
	        }
	        expandedURL=shortenedUrl;
	        String result = getClassification(expandedURL);
	        return result;
        }
        catch (URISyntaxException e){
		    return "";
	   }
        catch (MalformedURLException e){
		    return "";
	   }
	}
	public static String expandSingleLevel(String shortenedUrl) throws IOException { //resuelve la llamada
		try{
			URL url = new URL(shortenedUrl);    
			HttpURLConnection httpURLConnection = (HttpURLConnection) 
			url.openConnection(Proxy.NO_PROXY); 
			httpURLConnection.setInstanceFollowRedirects(true);
			httpURLConnection.setConnectTimeout(10000);
			String expandedURL = httpURLConnection.getHeaderField("Location");
			httpURLConnection.disconnect();	        	
			return expandedURL;
		}
		catch (MalformedURLException e){
		    return "";
	   }
	}
	public static String getClassification(String url) throws URISyntaxException {
	    try{
	    	URI uri = new URI(url);
	    	String domain = uri.getHost();
	    	domain = domain.startsWith("www.") ? domain.substring(4) : domain;
	    	domain = "www."+domain;
	    	return domain.toLowerCase();//devuelve la region a la que pertenece el dominio
	    	//return domain.substring(domain.lastIndexOf('.')+1);
	    }
	    catch (MalformedURIException e){
	    	return "";
	    }
	}
	public static LinkedHashMap<String, Long> fillMap(){
		LinkedHashMap<String, Long> aux = (LinkedHashMap<String, Long>) resultMap.clone();
		return aux;
	}
}
