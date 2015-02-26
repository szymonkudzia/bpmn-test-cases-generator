package com.edu.uj.sk.btcg.logic;

import java.util.Optional;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class VariableValueExtractor {
	private Tokenizer tokenizer = Tokenizer.create();
	
	private VariableValueExtractor() {
	}
	
	
	public static VariableValueExtractor create() {
		return new VariableValueExtractor();
	}
	
	
	/**
	 * For given condition (Groovy expression) extract all variables
	 * names with their possible values which will have impact on
	 * how condition will be evaluated
	 * 
	 * @param conditionExpression groovy expression
	 * @return Multimap<Variable name, possible values> NotNull
	 */
	public Multimap<String, Object> extractVariableValueMap(
			String conditionExpression) {
		
		String condition = cleanCondition(conditionExpression);
		BooleanExpressionTree expressionTree = BooleanExpressionTree.create(condition);
		HashMultimap<String, Object> variablesValues = determineVariablesBoundaryValues(expressionTree);
		
		return variablesValues;
	}

	

	private String cleanCondition(String conditionExpression) {
		return conditionExpression
			.replaceAll("^return (.*)", "$1")
			.replaceFirst("(.*);$", "$1");
	}
	
	


	private HashMultimap<String, Object> determineVariablesBoundaryValues(
		BooleanExpressionTree tree) {
		HashMultimap<String, Object> result = HashMultimap.create();
		
		determinaBoundaryValues(result, tree.getRoot());			
		
		return result;
	}
	
	private void determinaBoundaryValues(HashMultimap<String, Object> possibleValues, Optional<BooleanExpressionNode> node) {
		while (node.isPresent()) {
			if (tokenizer.isVarialbe(node.get().getLeft())) {
				getBoundaryValues(
						possibleValues, 
						node.get().getLeft().get().getValue(), 
						node.get().getValue(), 
						node.get().getRight());
				
				node = node.get().getRight();
				
			} else if (tokenizer.isVarialbe(node.get().getRight())) {
				getBoundaryValues(
						possibleValues, 
						node.get().getRight().get().getValue(), 
						node.get().getValue(), 
						node.get().getLeft());
				
				node = node.get().getLeft();
			} else {
				determinaBoundaryValues(possibleValues, node.get().getLeft());
				determinaBoundaryValues(possibleValues, node.get().getRight());
				
				node = Optional.empty();
			}
		}
	}
	
	
	
	
	/**
	 * Get possible values of variables used in connection condition
	 * 
	 * @param possibleValues
	 * @param variable
	 * @param operator
	 * @param sibling
	 */
	private void getBoundaryValues(
			HashMultimap<String, Object> possibleValues,
			String variable, 
			String operator,
			Optional<BooleanExpressionNode> sibling) {

		if (Lists.newArrayList("!").contains(operator)) {
			possibleValues.put(variable, Boolean.TRUE);
			possibleValues.put(variable, Boolean.FALSE);
			possibleValues.put(variable, null);
		} else {
			String value = sibling.get().getValue();
			
			if (tokenizer.isOperator(sibling.get().getValue())) {
				// TODO evaluate sibling
			}
			
			if (tokenizer.isNumber(value)) {
				Double dval = Double.parseDouble(value);
				
				possibleValues.put(variable, dval + 1);
				possibleValues.put(variable, dval);
				possibleValues.put(variable, dval - 1);
				possibleValues.put(variable, null);
			} else {
				possibleValues.put(variable, value.replaceAll("\"", ""));
				possibleValues.put(variable, "");
				possibleValues.put(variable, null);
			}
			
		}
	}

	
	


	public Tokenizer getTokenizer() {
		return tokenizer;
	}



	public void setTokenizer(Tokenizer tokenizer) {
		this.tokenizer = tokenizer;
	}
	
	
	
	
	
}
