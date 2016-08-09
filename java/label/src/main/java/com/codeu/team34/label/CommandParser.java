package com.codeu.team34.label;

import java.util.ArrayList;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;


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


public class CommandParser {
	private static final String AND = "and";
	private static final String OR = "or";
	private static final String IMAGE_SOURCE = "ImageSource";

	public JedisIndex index;
	public String[] args;
	private OptionParser parser;
	private OptionSpec<String> imageSource;// for later use
	private OptionSpec<String> queryWords;

	public CommandParser(String[] args, JedisIndex index) {
		this.index = index;
		this.parser = new OptionParser();
		this.args = args;
		initiateParser();
		process(args);

	}

	private void process(String[] args) {
		OptionSet options = parser.parse(args);

		if (options.has(IMAGE_SOURCE)) {
			List<String> labelResults=null;
			// read this image's tags ---> Need to modify
		try{
			String image = options.valuesOf(imageSource).get(0);
 			Path imagePath = Paths.get(image);
 			LabelApp query = new LabelApp(LabelApp.getVisionService(), imagePath);
 			labelResults = query.printLabels(imagePath, query.labelImage(imagePath, 3));
		
			System.out.print("image is about ");		
			for(String la: labelResults){
				System.out.print(la+" ");
			}
			System.out.print("\n");
		}catch(Exception e){
			System.out.println("something wrong with your image source..., bye-bye"); 
		}
			// do normal search for the tags and the rest of the query words with OR logic
			if(labelResults!=null){
				labelResults.add("--or");
				parseQueryWords(labelResults.toArray(new String[0]));
			}
		} else {
			parseQueryWords(args);
		}
	}

	private void initiateParser() {
		parser.accepts(AND);
		parser.accepts(OR);
		imageSource = parser.accepts(IMAGE_SOURCE).withRequiredArg()
				.ofType(String.class).defaultsTo("./");
		queryWords = parser.nonOptions().ofType(String.class);

	}

	private void parseQueryWords(String[] args) {
		OptionSet options = parser.parse(args);
		System.out.println("Start Searching...");
		if (options.has(AND) && options.has(OR)) {
			List<String> andSet = new ArrayList<String>();
			List<String> orSet = new ArrayList<String>();
			// note that -or cannot be at the end because we don't know which
			// words it wants to "or" with
			int i = 0;
			while (i < args.length) {
				if (args[i].equals("--or")) {
					orSet.add(args[i + 1]);
					i += 2;
				} else {
					andSet.add(args[i]);
					i++;
				}
			}

			OptionSet andOpstions = parser.parse(andSet.toArray(new String[0]));
			OptionSet orOpstions = parser.parse(orSet.toArray(new String[0]));
			WikiSearchResult intersection = searchMultiterms(
					andOpstions.valuesOf(queryWords), AND);
			WikiSearchResult result = searchMultiterms(intersection.getWs(),
					orOpstions.valuesOf(queryWords), OR,intersection.getQuery());
			System.out.println("=====================");
			System.out.println(result.getQuery());
			result.getWs().print();
			System.out.println("=====================");
		} else if (!options.has(AND) && options.has(OR)) {
			if (args[args.length-1]=="--or") {
				WikiSearchResult result = searchMultiterms(options.valuesOf(queryWords), OR);
				
				System.out.println("I am here!");

				System.out.println("=====================");
				System.out.println(result.getQuery());
				result.getWs().print();
				System.out.println("=====================");
			} else {
				List<String> andSet = new ArrayList<String>();
				List<String> orSet = new ArrayList<String>();
				// note that -or cannot be at the end because we don't know which
				// words it wants to "or" with
				int i = 0;
				while (i < args.length) {
					if (args[i].equals("--or")) {
						orSet.add(args[i + 1]);
						i += 2;
					} else {
						andSet.add(args[i]);
						i++;
					}
				}
				
				OptionSet andOpstions = parser.parse(andSet.toArray(new String[0]));
				OptionSet orOpstions = parser.parse(orSet.toArray(new String[0]));
				WikiSearchResult intersection = searchMultiterms(
						andOpstions.valuesOf(queryWords), AND);
				WikiSearchResult result = searchMultiterms(intersection.getWs(),
						orOpstions.valuesOf(queryWords), OR,intersection.getQuery());
				System.out.println("=====================");
				System.out.println(result.getQuery());
				result.getWs().print();
				System.out.println("=====================");
			}

		} else if (options.has(AND) && !options.has(OR)) {
			WikiSearchResult result = searchMultiterms(options.valuesOf(queryWords), AND);
			System.out.println("=====================");
			System.out.println(result.getQuery());
			result.getWs().print();
			System.out.println("=====================");
		} else {
			// neither AND nor OR
			WikiSearchResult result =searchMultiterms(options.valuesOf(queryWords), AND);
			System.out.println("=====================");
			System.out.println(result.getQuery());
			result.getWs().print();
			System.out.println("=====================");
		}
		System.out.println("End Searching...");
	}

	/*
	 * private void parseInput() { OptionSet options = parser.parse(args);
	 * 
	 * String querywords = options.valueOf(queryWords); String[] terms =
	 * querywords.split("-");
	 * 
	 * if (options.valueOf(text) == true && options.valueOf(image) == false) {
	 * if (options.valueOf(logic).equals("AND")) {
	 * System.out.println("searching... " + querywords + " with logic AND.");
	 * searchMultiterms(terms, "AND"); } else { // logic == or; not; ... } } }
	 */

	private WikiSearchResult searchMultiterms(List<String> list, String logic) {
		String query = "";
		if (list != null) {
			if (logic.equals(AND)) {
				query+=list.get(0) + " ";
				System.out.println("Query: "+list.get(0));
				WikiSearch intersection = WikiSearch.search(list.get(0), index);
				intersection.print();
				for (int i = 1; i < list.size(); i++) {
					query=query+"AND " + list.get(i) + " ";
					WikiSearch curr = WikiSearch.search(list.get(i), index);
					intersection = intersection.and(curr);
					System.out.println("Query: " + list.get(i));
					curr.print();
				}
				return new WikiSearchResult(intersection,query);
			}

			if (logic.equals(OR)) {
				query+=list.get(0) + " ";
				System.out.println("Query: "+list.get(0));
				WikiSearch union = WikiSearch.search(list.get(0), index);
				union.print();
				for (int i = 1; i < list.size(); i++) {
					query=query+"OR " + list.get(i) + " ";
					WikiSearch curr = WikiSearch.search(list.get(i), index);
					union =union.or(curr);
					System.out.println("Query: " + list.get(i));
					curr.print();
				}
				return new WikiSearchResult(union,query);
			}
		}
		return null;
	}

	private WikiSearchResult searchMultiterms(WikiSearch result, List<String> list,
			String logic,String query) {
		if (list != null) {
			if (logic.equals(AND)) {
				for (int i = 0; i < list.size(); i++) {
					query=query+"AND " + list.get(i) + " ";
					WikiSearch curr = WikiSearch.search(list.get(i), index);
					result = result.and(curr);
					System.out.println("Query: " + list.get(i));
					curr.print();
				}
				return new WikiSearchResult(result,query);
			}

			if (logic.equals(OR)) {
				for (int i = 0; i < list.size(); i++) {
					query=query+"OR " + list.get(i) + " ";
					WikiSearch curr = WikiSearch.search(list.get(i), index);
					result =result.or(curr);
					System.out.println("Query: " + list.get(i));
					curr.print();
				}
				return new WikiSearchResult(result,query);
			}
		}
		return null;
	}
}