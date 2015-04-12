package com.edu.uj.sk.btcg.generation.processors;

public class Stats {
	private String name;
	private int total;
	private int unique;

	public Stats(String processorName, int total, int unique) {
		this.name = processorName;
		this.total = total;
		this.unique = unique;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public int getUnique() {
		return unique;
	}

	public void setUnique(int unique) {
		this.unique = unique;
	}

}
