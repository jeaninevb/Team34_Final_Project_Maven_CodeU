package com.codeu.team34.label;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.lang.Math.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Represents a Redis-backed web search index.
 * 
 */
public class JedisIndex {

	private Jedis jedis;
	private long lastRequestTime = -1;
	private long minInterval = 1000;
	private List<String> stopList;

	/**
	 * Constructor.
	 * 
	 * @param jedis
	 */
	public JedisIndex(Jedis jedis) {
		this.jedis = jedis;
		
		stopList = new ArrayList<String>();
		try{
			String  thisLine = "";
			// open input stream test.txt for reading purpose.
			BufferedReader br = new BufferedReader(new FileReader("./en.txt"));
			while ((thisLine = br.readLine()) != null) {
				stopList.add(thisLine);
			}       
	      }catch(Exception e){
	         e.printStackTrace();
	      }
	}

	/**
	 * Returns the Redis key for a given search term.
	 * 
	 * @return Redis key.
	 */
	private String urlSetKey(String term) {
		return "URLSet:" + term;
	}

	/**
	 * Returns the Redis key for a URL's TermCounter.
	 * 
	 * @return Redis key.
	 */
	private String termCounterKey(String url) {
		return "TermCounter:" + url;
	}

	/**
	 * Checks whether we have a TermCounter for a given URL.
	 * 
	 * @param url
	 * @return
	 */
	public boolean isIndexed(String url) {
		String redisKey = termCounterKey(url);
		return jedis.exists(redisKey);
	}

	/**
	 * Adds a URL to the set associated with `term`.
	 * 
	 * @param term
	 * @param tc
	 */
	public void add(String term, TermCounter tc) {
		jedis.sadd(urlSetKey(term), tc.getLabel());
	}

	/**
	 * Looks up a search term and returns a set of URLs.
	 * 
	 * @param term
	 * @return Set of URLs.
	 */
	public Set<String> getURLs(String term) {
		Set<String> set = jedis.smembers(urlSetKey(term));
		return set;
	}

	/**
	 * Looks up a term and returns a map from URL to count.
	 * 
	 * @param term
	 * @return Map from URL to count.
	 */
	public Map<String, Double> getCounts(String term) {
		Map<String, Double> map = new HashMap<String, Double>();
		Set<String> urls = getURLs(term);
		for (String url : urls) {
			Double count = getCount(url, term);
			map.put(url, count);
		}
		return map;
	}

	/**
	 * Looks up a term and returns a map from URL to count.
	 * 
	 * @param term
	 * @return Map from URL to count.
	 */
	public Map<String, Double> getCountsFaster(String term) {
		// convert the set of strings to a list so we get the
		// same traversal order every time
		List<String> urls = new ArrayList<String>();
		urls.addAll(getURLs(term));

		// construct a transaction to perform all lookups
		Transaction t = jedis.multi();
		for (String url : urls) {
			String redisKey = termCounterKey(url);
			t.hget(redisKey, term);
		}
		List<Object> res = t.exec();

		// iterate the results and make the map
		Map<String, Double> map = new HashMap<String, Double>();
		int i = 0;
		for (String url : urls) {
			System.out.println(url);
			Double count = new Double((String) res.get(i++));
			map.put(url, count);
		}
		return map;
	}

	/**
	 * Returns the number of times the given term appears at the given URL.
	 * 
	 * @param url
	 * @param term
	 * @return
	 */
	public Double getCount(String url, String term) {
		double tfidf = 0;
		
		String redisKey = termCounterKey(url);
		String setKey = urlSetKey(term);
		
		String count = jedis.hget(redisKey, term);
		int tf = new Integer(count);
		
		Map<String,String> docMap = jedis.hgetAll(redisKey);
		int docSize = docMap.size();
		
		Set<String> termSet = jedis.keys("TermCounter:*");
		int numDocs = termSet.size();
		
		Set<String> docSet = jedis.smembers(setKey);
		int numDocsContaining = docSet.size();
		
		tfidf = (0.5+0.5*((double)tf/(double)docSize))*Math.log((double)numDocs/(double)numDocsContaining);
		
		return tfidf;
	}

	/**
	 * Add a page to the index.
	 * 
	 * @param url
	 *            URL of the page.
	 * @param paragraphs
	 *            Collection of elements that should be indexed.
	 */
	public void indexPage(String url, Elements paragraphs) {
		// make a TermCounter and count the terms in the paragraphs
		TermCounter tc = new TermCounter(url);
		tc.processElements(paragraphs);

		// push the contents of the TermCounter to Redis
		pushTermCounterToRedis(tc);
	}

	/**
	 * Pushes the contents of the TermCounter to Redis.
	 * 
	 * @param tc
	 * @return List of return values from Redis.
	 */
	public List<Object> pushTermCounterToRedis(TermCounter tc) {
		Transaction t = jedis.multi();
		
		String url = tc.getLabel();
		String hashname = termCounterKey(url);

		// if this page has already been indexed; delete the old hash
		t.del(hashname);

		// for each term, add an entry in the termcounter and a new
		// member of the index
		for (String term : tc.keySet()) {
			if(!stopList.contains(term)) {
				Integer count = tc.get(term);
				t.hset(hashname, term, count.toString());
				t.sadd(urlSetKey(term), url);
			}
		}
		List<Object> res = t.exec();

		return res;
	}

	/**
	 * Prints the contents of the index.
	 * 
	 * Should be used for development and testing, not production.
	 */
	public void printIndex() {
		// loop through the search terms
		for (String term : termSet()) {
			System.out.println(term);

			// for each term, print the pages where it appears
			Set<String> urls = getURLs(term);
			for (String url : urls) {
				Double count = getCount(url, term);
				System.out.println("    " + url + " " + count);
			}
		}
	}

	/**
	 * Returns the set of terms that have been indexed.
	 * 
	 * Should be used for development and testing, not production.
	 * 
	 * @return
	 */
	public Set<String> termSet() {
		Set<String> keys = urlSetKeys();
		Set<String> terms = new HashSet<String>();
		for (String key : keys) {
			String[] array = key.split(":");
			if (array.length < 2) {
				terms.add("");
			} else {
				terms.add(array[1]);
			}
		}
		return terms;
	}

	/**
	 * Returns URLSet keys for the terms that have been indexed.
	 * 
	 * Should be used for development and testing, not production.
	 * 
	 * @return
	 */
	public Set<String> urlSetKeys() {
		return jedis.keys("URLSet:*");
	}

	/**
	 * Returns TermCounter keys for the URLS that have been indexed.
	 * 
	 * Should be used for development and testing, not production.
	 * 
	 * @return
	 */
	public Set<String> termCounterKeys() {
		return jedis.keys("TermCounter:*");
	}

	/**
	 * Deletes all URLSet objects from the database.
	 * 
	 * Should be used for development and testing, not production.
	 * 
	 * @return
	 */
	public void deleteURLSets() {
		Set<String> keys = urlSetKeys();
		Transaction t = jedis.multi();
		for (String key : keys) {
			t.del(key);
		}
		t.exec();
	}

	/**
	 * Deletes all URLSet objects from the database.
	 * 
	 * Should be used for development and testing, not production.
	 * 
	 * @return
	 */
	public void deleteTermCounters() {
		Set<String> keys = termCounterKeys();
		Transaction t = jedis.multi();
		for (String key : keys) {
			t.del(key);
		}
		t.exec();
	}

	/**
	 * Deletes all keys from the database.
	 * 
	 * Should be used for development and testing, not production.
	 * 
	 * @return
	 */
	public void deleteAllKeys() {
		Set<String> keys = jedis.keys("*");
		Transaction t = jedis.multi();
		for (String key : keys) {
			t.del(key);
		}
		t.exec();
	}



	private List<String> loadPortal(String query){
		
		List<String> portalList = new ArrayList<String>();
		String PortalUrl = "https://en.wikipedia.org/wiki/Portal:" + query;
		
		try{
			sleepIfNeeded();
			Document doc = Jsoup.connect(PortalUrl).get();
			Element content = doc.getElementById("mw-content-text");
        	Elements links = doc.select("a[href]");
				
			for(Element link: links){
				
				if(link.attr("href").toLowerCase().indexOf("/wiki/") == 0 &&
						!link.attr("href").contains(":") &&
						!link.attr("href").contains("#")){
					String linkUrl = "https://en.wikipedia.org" + link.attr("href");
					portalList.add(linkUrl);
				}
				if(portalList.size() >= 25) {
					break;
				}
			}
		}
		
		catch(IOException e){
			//System.out.println("Could not connect to the url");
		}
		
		return portalList;
	
	
	}
	
	private List<String> loadWiki(String query){
		
		List<String> wikiList = new ArrayList<String>();
		String wikiUrl = "https://en.wikipedia.org/wiki/" + query;
		
		try {
			sleepIfNeeded();
			Document doc = Jsoup.connect(wikiUrl).get();
			Element content = doc.getElementById("mw-content-text");
        	Elements links = doc.select("a[href]");
				
			for(Element link: links){
				
				if(link.attr("href").toLowerCase().indexOf("/wiki/") == 0){
					
					if(link.attr("href").toLowerCase().indexOf(query.toLowerCase()) != -1 &&
							!link.attr("href").contains("#")) {
						String linkUrl = "https://en.wikipedia.org" + link.attr("href");
						wikiList.add(linkUrl);
					}
				}
				if(wikiList.size() >= 25) {
					break;
				}
			}  
		} catch(IOException e) {
			//System.out.println("Could not connect to the url");
		}

		wikiList.add(wikiUrl);
		return wikiList;
	
	
	}
	
	private boolean portalExists(String term) {
		String PortalUrl = "https://en.wikipedia.org/wiki/Portal:" + term;
		
		try{
			sleepIfNeeded();
			Document doc = Jsoup.connect(PortalUrl).get();
			String html = doc.body().toString();
			if(html.toLowerCase().indexOf("Wikipedia does not have a") != -1 &&
					html.toLowerCase().indexOf("portal") != -1 &&
					html.toLowerCase().indexOf("with this exact name.") != -1) {
				return false;
			} else {
				return true;
			}
		} catch(IOException e){
			return false;
		}
	}
	
	public void loadDB(String[] args) throws IOException{
		List<String> urlList = new ArrayList<String>();

		System.out.println("Building Relevant URLs");
		for(int i=0; i<args.length; i++) {
			String term = args[i];
			if(!(term.toLowerCase().indexOf("--") == 0)) {
				if(portalExists(term)) {
					urlList.addAll(loadPortal(term));
				} else {
					urlList.addAll(loadWiki(term));
				}
			}
			if(urlList.size() >= 125) {
				break;
			}
		}

		if(checkUsage()>0.70){
			deleteTermCounters();
			deleteURLSets();
			deleteAllKeys();
		}
		System.out.println("Indexing URLs");
		loadIndex(urlList);
		System.out.println("Done Indexing URLs");
	}


	public Double checkUsage(){
		String[] lines = jedis.info().split(System.getProperty("line.separator"));
		//System.out.println(jedis.info());
		Double used=0.0;
		Double peak=0.0;
		for(String line:lines){
			if(line.contains("used_memory_rss:")){
				String[] usage = line.split(":");
				//System.out.println(usage[1]);
				used = Double.parseDouble(usage[1]);
			}
			if(line.contains("used_memory_peak:")){
				String[] usage = line.split(":");
				//System.out.println(usage[1]);
				peak = Double.parseDouble(usage[1]);
			}
		}

		Double perct = used/peak;
		System.out.println("Current Database usage:" +Double.toString(perct*100)+"%");
		return perct;
	}


	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
// 		Jedis jedis = JedisMaker.make();
// 		JedisIndex index = new JedisIndex(jedis);
		
		//initiate our redis database
// 		String[] urls = {
// 				"https://en.wikipedia.org/wiki/Java_(programming_language)",
// 				"https://en.wikipedia.org/wiki/Programming_language",
// 				"https://en.wikipedia.org/wiki/Awareness",
// 				"https://en.wikipedia.org/wiki/Computer_science",
// 				"https://en.wikipedia.org/wiki/Concurrent_computing",
// 				"https://en.wikipedia.org/wiki/Consciousness",
// 				"https://en.wikipedia.org/wiki/Knowledge",
// 				"https://en.wikipedia.org/wiki/Mathematics",
// 				"https://en.wikipedia.org/wiki/Modern_philosophy",
// 				"https://en.wikipedia.org/wiki/Philosophy",
// 				"https://en.wikipedia.org/wiki/Property_(philosophy)",
// 				"https://en.wikipedia.org/wiki/Quality_(philosophy)",
// 				"https://en.wikipedia.org/wiki/Science",
// 				"https://en.wikipedia.org/wiki/Dog" };
// 		index.deleteTermCounters();
// 		index.deleteURLSets();
// 		index.deleteAllKeys();
// 		loadIndex(urls);

		//Map<String, Integer> map = index.getCountsFaster("the");
		//for (Entry<String, Integer> entry : map.entrySet()) {
		//	System.out.println(entry);
		//}
		
	}

	/**
	 * Stores two pages in the index for testing purposes.
	 * 
	 * @return
	 * @throws IOException
	 */
	private void loadIndex(List<String> urls)
			throws IOException {
		WikiFetcher wf = new WikiFetcher();

		// String url = "https://en.wikipedia.org/wiki/Java_(programming_language)";
		// Elements paragraphs = wf.readWikipedia(url);
		// index.indexPage(url, paragraphs);

		// url = "https://en.wikipedia.org/wiki/Programming_language";
		// paragraphs = wf.readWikipedia(url);
		// index.indexPage(url, paragraphs);

		for (String url : urls) {
			if(!isIndexed(url)){
				try {
					Elements paragraphs = wf.fetchWikipedia(url);
					indexPage(url, paragraphs);
				} catch(Exception e) {
				
				}
			}
		}

	}
	
	/**
	 * Rate limits by waiting at least the minimum interval between requests.
	 */
	private void sleepIfNeeded() {
		if (lastRequestTime != -1) {
			long currentTime = System.currentTimeMillis();
			long nextRequestTime = lastRequestTime + minInterval;
			if (currentTime < nextRequestTime) {
				try {
					//System.out.println("Sleeping until " + nextRequestTime);
					Thread.sleep(nextRequestTime - currentTime);
				} catch (InterruptedException e) {
					System.err.println("Warning: sleep interrupted in fetchWikipedia.");
				}
			}
		}
		lastRequestTime = System.currentTimeMillis();
	}

}