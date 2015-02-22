package com.edu.uj.sk.btcg.logic;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Lists;

public class Tokenizer {
	private static List<String> operators = Lists.newArrayList(">=", "<=", "==", "&&",
			"||", "!", "<", ">", "&", "|", "=", "!=");
	
	private static List<String> incorrectCharacters = Lists.newArrayList(
			".", " ", "<", ">", "(", ")", "&", "%", "@", "#", "$", "^", "*",
			",", "/", "?", ";", "[", "]", "{", "}", "\\", "|", "~", "`", "\"",
			"'", "!", "-"
		);
	
	private static List<String> keywords = Lists.newArrayList(
			"return", "if", "class", "int", "double", "char", "String", "float",
			"Integer", "Double", "Float", "boolean", "Boolean", "switch", "case",
			"throws", "thorw", "for", "do", "volatile", "synchronized", "extends",
			"implements", "private", "public", "protected", "assert", "interface",
			"final", "static", "void", "instanceof"
		); 

	private Tokenizer() {
	}

	public static Tokenizer create() {
		return new Tokenizer();
	}
	
	/**
	 * Tokenize given expression
	 * 
	 * @param expression nullable
	 * @return Iterator of tokens
	 */
	Iterator<String> tokenize(String expression) {
		return new It(expression);
	}

	
	/**
	 * Check if token is one of:
	 * 		">=", "<=", "==", "&&",
	 *		"||", "!", "<", ">", "&", "|", "="
	 *
	 * @param token
	 * @return true if token is an operator
	 */
	public boolean isOperator(String token) {
		return operators.contains(token);
	}
	
	/**
	 * Check if token is left bracket ('(')
	 * 
	 * @param token
	 * @return true if token is a left bracket
	 */
	public boolean isLeftBracket(String token) {
		return "(".equals(token);
	}
	
	/**
	 * Check if token is right bracket (')')
	 * 
	 * @param token nullable
	 * @return true if token is a right bracket
	 */
	public boolean isRightBracket(String token) {
		return ")".equals(token);
	}
	
	
	/** 
	 * Check if token is a number
	 * 
	 * @param token
	 * @return true if token is a number false otherwise, false if token is null
	 */
	public boolean isNumber(String token) {
		if (token == null) return false;
		
		return token.matches("^[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?$");
	}
	
	
	/**
	 * Check if given token is a variable
	 * 
	 * @param token nullable
	 * @return true if token is a variable
	 */
	public boolean isVariable(String token) {
		if (StringUtils.isBlank(token)) return false;
		
		if (token.matches("^[0-9].*")) return false;
		
		
		for (String c : incorrectCharacters) {
			if (token.contains(c)) return false; 
		}
		
		if (isNumber(token)) return false;
		if (isOperator(token)) return false; 
		if ("null".equals(token)) return false;
		
		for (String keyword : keywords) {
			if (keyword.equals(token)) return false;
		}
		
		return true;
	}
	
	/**
	 * Check if BooleanExpressionNode holds a variable
	 * 
	 * @param expressionNode
	 * @return true if BooleanExpressionNode holds a variable
	 */
	public boolean isVarialbe(Optional<BooleanExpressionNode> expressionNode) {
		if (!expressionNode.isPresent()) return false;
		
		String value = expressionNode.get().getValue();
		return isVariable(value);
	}
	
	

	
	
	private class It implements Iterator<String> {
		private String expression;
		
		public It(String expression) {
			if (StringUtils.isBlank(expression))
				this.expression = "";
			else {
				this.expression = expression.trim();
			}
		}
		
		@Override
		public boolean hasNext() {
			return !expression.isEmpty();
		}

		@Override
		public String next() {
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
				String currentLetter = "";
				
				while (true) {
					if (i >= expression.length()) break;
					currentLetter = expression.substring(i, ++i);
					
					if (isTokenEnd(currentLetter)) break;
					
					token += currentLetter;
				}
			}

			expression = expression.substring(token.length()).trim();
			return token.trim();
		}
		
		
		private String getFirstCharacter() {
			return expression.substring(0, 1);
		}

		private String getFirstTwoCharacters() {
			return expression.substring(0, 2);
		}
		
		private boolean isTokenEnd(String c) {
			if (c.equals(" "))         return true;
			if (operators.contains(c)) return true;
			if ("(".equals(c))         return true;
			if (")".equals(c))         return true;
			if (";".equals(c))         return true;
			
			return false;
		}
	}
}