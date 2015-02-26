package com.edu.uj.sk.btcg.logic;

import java.util.Iterator;
import java.util.Optional;
import java.util.Stack;

import org.apache.commons.lang.StringUtils;

public class BooleanExpressionTreeBuilder {
	Tokenizer tokenizer = Tokenizer.create();
	
	private BooleanExpressionTreeBuilder() {}
	
	public static BooleanExpressionTreeBuilder create() {
		return new BooleanExpressionTreeBuilder();
	}
	
	public BooleanExpressionTree build(String expression) {
		try {
			if (StringUtils.isBlank(expression)) 
				return new BooleanExpressionTree(Optional.empty());
		
			Stack<String> expressionStack = buildExpressionStack(expression);
			Optional<BooleanExpressionNode> root = buildExpressionTree(expressionStack);
			assertThatTreeCanBeBuild(root.isPresent(), expression);
			
			return new BooleanExpressionTree(root);
			
		} catch (Throwable e) {
			assertThatTreeCanBeBuild(false, expression);
			return null; // never called, line above throws exception
		}		
	}

	/**
	 * if canBeBuild is false exception IllegalStateException is thrown
	 * 
	 * @param canBeBuild
	 * @param expression
	 */
	private void assertThatTreeCanBeBuild(boolean canBeBuild, String expression) {
		if (!canBeBuild) 
			throw new IllegalStateException(
				"Could not create BooleanExpressionTree"
				+ " for given expression: " 
				+ expression);
	}


	private Stack<String> buildExpressionStack(String expression) {
		Stack<String> expressionStack = new Stack<>();
		Stack<String> operatorsStack = new Stack<>();
		
		Iterator<String> tokens = tokenizer.tokenize(expression);
		while (tokens.hasNext()) {
			final String token = tokens.next();
			
			if (tokenizer.isOperator(token)) {
				if (operatorsStack.isEmpty()) {
					operatorsStack.push(token);
					continue;
				}
				
				final String operator = operatorsStack.peek();
				
				if (tokenizer.isLeftBracket(operator)) {
					operatorsStack.push(token);
				} else {
					expressionStack.push(operatorsStack.pop());
				}
				
			} else if (tokenizer.isLeftBracket(token)) {
				operatorsStack.push(token);
				
			} else if (tokenizer.isRightBracket(token)) {
				String operator;
				
				while (!tokenizer.isLeftBracket(operator = operatorsStack.pop())) {
					expressionStack.push(operator);
				} 
				
			} else {
				expressionStack.push(token);
			}
		}
		
		if (!operatorsStack.isEmpty()) {
			assertThatTreeCanBeBuild(!operatorsStack.contains("("), expression);
			
			expressionStack.addAll(operatorsStack);
		}
		
		return expressionStack;
	}

	
	private Optional<BooleanExpressionNode> buildExpressionTree(Stack<String> expressionStack) {
		BooleanExpressionNode node = new BooleanExpressionNode();
		
		String token = expressionStack.pop();
		node.setValue(token);
		
		if (tokenizer.isOperator(token)) {
			node.setRight(buildExpressionTree(expressionStack));
			
			if (!token.equals("!"))
				node.setLeft(buildExpressionTree(expressionStack));
		}
		
		return Optional.of(node);
	}
	
}
