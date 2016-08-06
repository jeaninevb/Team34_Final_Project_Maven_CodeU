package com.codeu.team34.label;

public class WikiSearchResult {
	private WikiSearch ws;
	private String query;
	
	public WikiSearchResult(WikiSearch ws, String query){
		this.ws = ws;
		this.query=query;
	}

	public WikiSearch getWs() {
		return ws;
	}

	public void setWs(WikiSearch ws) {
		this.ws = ws;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}
	
	
}