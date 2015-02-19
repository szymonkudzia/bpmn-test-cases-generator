package com.edu.uj.sk.btcg.logic;

import java.util.List;

import com.google.common.collect.Lists;

public class Tokenizer {
	private List<String> operators = Lists.newArrayList(">=", "<=", "==", "&&",
			"||", "!", "<", ">", "&", "|", "=");

	private String expression;

	public Tokenizer(String expression) {
		this.expression = expression;
	}

	public String getNextToken() {
		String token = "";
		String first = getFirstCharacter();

		if (isLeftBracket(first) || isRightBracket(first)) {
			token = first;
			
		} else if (operators.contains(first)) {
			token = first;
			String firstTwo = getFirstTwoCharacters();

			if (operators.contains(firstTwo)) {
				token = firstTwo;
			}
			
		} else {
			int i = 0;
			String currentLetter;
			do {
				if (i >= expression.length()) break;
				
				currentLetter = expression.substring(i, ++i);
				token += currentLetter;

			} while (notTokenEnd(currentLetter));
		}

		expression = expression.substring(token.length()).trim();
		return token.trim();
	}

	public boolean hasNextToken() {
		return !expression.isEmpty();
	}

	public boolean isOperator(String token) {
		return operators.contains(token);
	}
	
	public boolean isLeftBracket(String token) {
		return "(".equals(token);
	}
	
	public boolean isRightBracket(String token) {
		return ")".equals(token);
	}
	
	
	
	

	private String getFirstCharacter() {
		return expression.substring(0, 1);
	}

	private String getFirstTwoCharacters() {
		return expression.substring(0, 2);
	}

	private boolean notTokenEnd(String c) {
		return !(c.equals(" ") || operators.contains(c));
	}
}