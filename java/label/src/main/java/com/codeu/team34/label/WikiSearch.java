package com.codeu.team34.label;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import com.codeu.team34.label.LabelApp;
import redis.clients.jedis.Jedis;


/**
 * Represents the results of a search query.
 *
 */
public class WikiSearch {
	
	// map from URLs that contain the term(s) to relevance score
	private Map<String, Integer> map;

	/**
	 * Constructor.
	 * 
	 * @param map
	 */
	public WikiSearch(Map<String, Integer> map) {
		this.map = map;
	}
	
	/**
	 * Looks up the relevance of a given URL.
	 * 
	 * @param url
	 * @return
	 */
	public Integer getRelevance(String url) {
		Integer relevance = map.get(url);
		return relevance==null ? 0: relevance;
	}
	
	/**
	 * Prints the contents in order of term frequency.
	 * 
	 * @param map
	 */
	private  void print() {
		List<Entry<String, Integer>> entries = sort();
		for (Entry<String, Integer> entry: entries) {
			System.out.println(entry);
		}
	}
	
	/**
	 * Computes the union of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch or(WikiSearch that) {
		Map<String, Integer> union = new HashMap<String, Integer>();
		
		for(Map.Entry<String, Integer> entry1: this.map.entrySet()){
		
			union.put(entry1.getKey(), entry1.getValue());
		}
		
		for(Map.Entry<String, Integer> entry2: that.map.entrySet()){
			
			Integer isDuplicate = union.get(entry2.getKey());
			if(isDuplicate != null){
			
				union.put(entry2.getKey(), totalRelevance(isDuplicate, entry2.getValue()));
			}
			else{
			
				union.put(entry2.getKey(), entry2.getValue());
			}
		}	
		
// 		
// 		for(Map.Entry<String, Integer> entry: union.entrySet()){
// 			
// 			System.out.println(entry.getKey() + "   " + entry.getValue());
// 		}
		
		
		return new WikiSearch(union);
	}
	
	/**
	 * Computes the intersection of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch and(WikiSearch that) {
		Map<String, Integer> intersection = new HashMap<String, Integer>();
	
		for(Map.Entry<String, Integer> entry1: this.map.entrySet()){
 			for(Map.Entry<String, Integer> entry2: that.map.entrySet()){
 					
 					if(entry1.getKey().equals(entry2.getKey())){	
 					
 						intersection.put(entry1.getKey(), totalRelevance(entry1.getValue(),entry2.getValue())); 						
 					}
 			
 			}
 		}
 		
//  		for(Map.Entry<String, Integer> entry: intersection.entrySet()){
// 			
// 			System.out.println(entry.getKey() + "   " + entry.getValue());
// 		}
		return new WikiSearch(intersection);
	}
	
	/**
	 * Computes the intersection of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch minus(WikiSearch that) {
		Map<String, Integer> intersection = new HashMap<String, Integer>();
 		
 		for(Map.Entry<String, Integer> entry1: this.map.entrySet()){
		
			intersection.put(entry1.getKey(), entry1.getValue());
		}
		
		for(Map.Entry<String, Integer> entry2: that.map.entrySet()){
			
			Integer isDuplicate = intersection.get(entry2.getKey());
			if(isDuplicate != null){
				//System.out.println("remove: " + entry2.getKey());
				intersection.remove(entry2.getKey());
			}

		}	
 		
//  		for(Map.Entry<String, Integer> entry: intersection.entrySet()){
//  			
//  			System.out.println(entry.getKey() + "   " + entry.getValue());
//  		}
		return new WikiSearch(intersection);
	}
	
	/**
	 * Computes the relevance of a search with multiple terms.
	 * 
	 * @param rel1: relevance score for the first search
	 * @param rel2: relevance score for the second search
	 * @return
	 */
	protected int totalRelevance(Integer rel1, Integer rel2) {
		// simple starting place: relevance is the sum of the term frequencies.
		return rel1 + rel2;
	}

// 	
// 	public int compareTo(Entry<String, Integer> that){
// 				
// 		return this.Entry<String, Integer>.getValue() > that.getValue() ? 1 : (this.Entry<String, Integer>.getValue() < that.getValue() ? -1 : 0); 
// 	}


	/**
	 * Sort the results by relevance.
	 * 
	 * @return List of entries with URL and relevance.
	 */
	public List<Entry<String, Integer>> sort() {
		
		List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(this.map.entrySet());

		Comparator<Map.Entry<String, Integer>> comparator = new Comparator<Map.Entry<String, Integer>>() {
            
            @Override			
			public int compare(Map.Entry<String, Integer> object1, Map.Entry<String, Integer> object2){
 				
 				return (object1.getValue()).compareTo(object2.getValue());
			}
		};
		
		Collections.sort(list, comparator);
		
		return list;
	}

	/**
	 * Performs a search and makes a WikiSearch object.
	 * 
	 * @param term
	 * @param index
	 * @return
	 */
	public static WikiSearch search(String term, JedisIndex index) {
		Map<String, Integer> map = index.getCounts(term);
		return new WikiSearch(map);
	}

	public static void main(String[] args) throws IOException, GeneralSecurityException {
		
		// make a JedisIndex
// 		Jedis jedis = JedisMaker.make();
// 		JedisIndex index = new JedisIndex(jedis); 
 		
// 		// search for the first term
// 		String term1 = "java";
// 		System.out.println("Query: " + term1);
// 		WikiSearch search1 = search(term1, index);
// 		search1.print();
// 		
// 		// search for the second term
// 		String term2 = "programming";
// 		System.out.println("Query: " + term2);
// 		WikiSearch search2 = search(term2, index);
// 		search2.print();
// 		
// 		// compute the intersection of the searches
// 		System.out.println("Query: " + term1 + " AND " + term2);
// 		WikiSearch intersection = search1.and(search2);
// 		intersection.print();
		
// 		Jedis jedis = JedisMaker.make();
// 		JedisIndex index = new JedisIndex(jedis); 		


 		String image = "demo-image.jpg";
 		Path imagePath = Paths.get(image);
 		
 		LabelApp query = new LabelApp(LabelApp.getVisionService(), imagePath);
 		
 		List<String> labelResults = query.printLabels(imagePath, query.labelImage(imagePath, 3));
		
		for(String la: labelResults){
			System.out.println(la);
		}
		
	}
}
