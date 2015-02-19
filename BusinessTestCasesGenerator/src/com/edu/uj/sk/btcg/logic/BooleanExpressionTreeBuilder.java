package com.edu.uj.sk.btcg.logic;

import java.util.Optional;
import java.util.Stack;

public class BooleanExpressionTreeBuilder {
	
	public BooleanExpressionTree build(String expression) {
		Tokenizer tokenizer = new Tokenizer(expression);
		
		Stack<String> expressionStack = buildExpressionStack(tokenizer);
		Optional<BooleanExpressionNode> root = buildExpressionTree(expressionStack, tokenizer);
		
		if (!root.isPresent()) 
			throw new IllegalStateException(
				"Could not create BooleanExpressionTree"
				+ " for given expression: " 
				+ expression);
		
		return new BooleanExpressionTree(root);
	}




	private Stack<String> buildExpressionStack(Tokenizer tokenizer) {
		Stack<String> expressionStack = new Stack<>();
		Stack<String> operatorsStack = new Stack<>();
		
		while (tokenizer.hasNextToken()) {
			final String token = tokenizer.getNextToken();
			
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
				
				while (tokenizer.isLeftBracket(operator = operatorsStack.pop())) {
					expressionStack.push(operator);
				} 
				
			} else {
				expressionStack.push(token);
			}
		}
		
		if (!operatorsStack.isEmpty()) {
			expressionStack.addAll(operatorsStack);
		}
		
		return expressionStack;
	}

	
	private Optional<BooleanExpressionNode> buildExpressionTree(Stack<String> expressionStack, Tokenizer tokenizer) {
		BooleanExpressionNode node = new BooleanExpressionNode();
		
		String token = expressionStack.pop();
		node.setValue(token);
		
		if (tokenizer.isOperator(token)) {
			node.setRight(buildExpressionTree(expressionStack, tokenizer));
			node.setLeft(buildExpressionTree(expressionStack, tokenizer));
		}
		
		return Optional.of(node);
	}
	
}
