package com.codeu.team34.label;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class WikiMedia {

	private Map<String,String>  mediaUrls;
	private List<String> args;
	
	public WikiMedia(List<String> args) {
		this.args = args;
		this.mediaUrls = new HashMap<String,String>();
	
	}
	
	public void fetchMedia(){
		
		for(String arg: args){
		
			mediaCrawler(arg);
		
		}
	
	}	
	
	public void mediaCrawler(String arg){
	
		String url = "https://commons.wikimedia.org/wiki/" + arg;
		
		Document doc = null;
		
		try{
			doc = Jsoup.connect(url).get();
			Element content = doc.getElementById("mw-content-text");
        	Elements media = doc.select("[src]");
			
			//int idx=1;
			for(Element src: media){
				
				if (src.tagName().equals("img")){
					String newurl = src.attr("abs:src");
					if(!newurl.equals("") && newurl.contains("upload") && !newurl.contains("System-search") && !newurl.contains("-logo")){
						String imageKey = "";
						
						String[] tokens = newurl.split("/");
						if(tokens.length>=1){
							imageKey = tokens[tokens.length-2];
							//String[] tokens_2 = imageKey.split(".");
						}
							//System.out.println(idx+": "+newurl);
							//mediaUrls.put(idx+imageKey,newurl);			
							//idx++;
						mediaUrls.put(imageKey,newurl);
					}						
				}
			}  
		}
		
		catch(IOException e){
			System.out.println("Could not connect to the url");
		}
		
	
	}
	
	public void print(){
	
		for(String imageKey: mediaUrls.keySet()){
		
			System.out.println(imageKey+" ("+mediaUrls.get(imageKey)+")");
		}
	}	
// 
// 	/**
// 	 * Fetches and parses a URL string, returning a list of paragraph elements.
// 	 *
// 	 * @param url
// 	 * @return
// 	 * @throws IOException
// 	 */
// 	public Elements fetchWikipedia(String url) throws IOException {
// 		sleepIfNeeded();
// 
// 		// download and parse the document
// 		Connection conn = Jsoup.connect(url);
// 		Document doc = conn.get();
// 
// 		// select the content text and pull out the paragraphs.
// 		Element content = doc.getElementById("mw-content-text");
// 
// 		// TODO: avoid selecting paragraphs from sidebars and boxouts
// 		Elements paras = content.select("p");
// 		return paras;
// 	}
// 
// 	/**
// 	 * Reads the contents of a Wikipedia page from src/resources.
// 	 *
// 	 * @param url
// 	 * @return
// 	 * @throws IOException
// 	 */
// 	public Elements readWikipedia(String url) throws IOException {
// 		URL realURL = new URL(url);
// 
// 		// assemble the file name
// 		String slash = File.separator;
// 		String filename = "resources" + slash + realURL.getHost() + realURL.getPath();
// 
// 		// read the file
// 		InputStream stream = WikiFetcher.class.getClassLoader().getResourceAsStream(filename);
// 		Document doc = Jsoup.parse(stream, "UTF-8", filename);
// 
// 		// TODO: factor out the following repeated code
// 		Element content = doc.getElementById("mw-content-text");
// 		Elements paras = content.select("p");
// 		return paras;
// 	}
// 
// 	/**
// 	 * Rate limits by waiting at least the minimum interval between requests.
// 	 */
// 	private void sleepIfNeeded() {
// 		if (lastRequestTime != -1) {
// 			long currentTime = System.currentTimeMillis();
// 			long nextRequestTime = lastRequestTime + minInterval;
// 			if (currentTime < nextRequestTime) {
// 				try {
// 					//System.out.println("Sleeping until " + nextRequestTime);
// 					Thread.sleep(nextRequestTime - currentTime);
// 				} catch (InterruptedException e) {
// 					System.err.println("Warning: sleep interrupted in fetchWikipedia.");
// 				}
// 			}
// 		}
// 		lastRequestTime = System.currentTimeMillis();
// 	}
	
	
	
	public static void main(String[] args) throws IOException {
	
		//String url = "https://commons.wikimedia.org/wiki/Belgium";
		List<String> a = new ArrayList<String>();
		a.add("Belgium");
		WikiMedia wm = new WikiMedia(a);
		wm.fetchMedia();
		wm.print();
	
	}

}