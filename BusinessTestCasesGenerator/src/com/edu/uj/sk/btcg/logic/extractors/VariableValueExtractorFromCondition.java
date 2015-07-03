package com.edu.uj.sk.btcg.logic.extractors;

import java.util.Optional;
import java.util.UUID;

import org.activiti.bpmn.model.SequenceFlow;
import org.apache.commons.lang.StringUtils;

import com.edu.uj.sk.btcg.logic.BooleanExpressionNode;
import com.edu.uj.sk.btcg.logic.BooleanExpressionTree;
import com.edu.uj.sk.btcg.logic.Tokenizer;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class VariableValueExtractorFromCondition extends AbstractVariableValueExtractor<SequenceFlow>{
	private static String random = UUID.randomUUID().toString().replaceAll("-", "");
	private Tokenizer tokenizer = Tokenizer.create();
	
	private VariableValueExtractorFromCondition() {
	}
	
	
	public static VariableValueExtractorFromCondition create() {
		return new VariableValueExtractorFromCondition();
	}
	
	@Override
	protected Multimap<String, Object> doExtraction(SequenceFlow element) {
		String conditionExpression = element.getConditionExpression();
		
		if (StringUtils.isBlank(conditionExpression))
			return HashMultimap.create();
		
		return extractVariableValueMap(conditionExpression);
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
		if (!node.isPresent()) return;
		
		Optional<BooleanExpressionNode> left = node.get().getLeft();
		Optional<BooleanExpressionNode> right = node.get().getRight();
			
		getBoundaryValues(
				possibleValues, 
				left, 
				node.get().getValue(), 
				right);
		
		
		if (left.isPresent() && tokenizer.isOperator(left.get().getValue()))
				determinaBoundaryValues(possibleValues, left);
		
		if (right.isPresent() && tokenizer.isOperator(right.get().getValue()))
			determinaBoundaryValues(possibleValues, right);
		
	}
	
	
	
	
	/**
	 * Get possible values of variables used in connection condition
	 * 
	 * @param possibleValues
	 * @param left
	 * @param current
	 * @param right
	 */
	private void getBoundaryValues(
			HashMultimap<String, Object> possibleValues,
			Optional<BooleanExpressionNode> left, 
			String current,
			Optional<BooleanExpressionNode> right) {

		if (hasNoChilds(left, right) || isBooleanOperator(current)) {
			String variable = current;
			
			if (left.isPresent()) variable = left.get().getValue();
			if (right.isPresent()) variable = right.get().getValue();
			
			possibleValues.put(variable, Boolean.TRUE);
			possibleValues.put(variable, Boolean.FALSE);
			possibleValues.put(variable, null);
			
		}
		
		if (!right.isPresent() || !left.isPresent())
			return;

		
		String variable = left.get().getValue();
		String value = right.get().getValue();
		
		if (tokenizer.isVariable(value)) {
			variable = value;
			value = left.get().getValue();
		}
		
		
		if (tokenizer.isOperator(value)) {
			// TODO evaluate sibling
		} else if (tokenizer.isVariable(value)) {
			// TODO what to do if we encounter variable?
			
		} else if (tokenizer.isNumber(value)) {
			Double dval = Double.parseDouble(value);
			
			possibleValues.put(variable, dval + 1);
			possibleValues.put(variable, dval);
			possibleValues.put(variable, dval - 1);
			possibleValues.put(variable, null);
		} else {
			possibleValues.put(variable, value.replaceAll("\"", ""));
			possibleValues.put(variable, random);
			possibleValues.put(variable, "");
			possibleValues.put(variable, null);
		}
	}


	private boolean isBooleanOperator(String current) {
		return Lists.newArrayList("!", "&&", "||").contains(current);
	}


	private boolean hasNoChilds(
			Optional<BooleanExpressionNode> left,
			Optional<BooleanExpressionNode> sibling) {
		return !(left.isPresent() || sibling.isPresent());
	}

	
	


	public Tokenizer getTokenizer() {
		return tokenizer;
	}



	public void setTokenizer(Tokenizer tokenizer) {
		this.tokenizer = tokenizer;
	}


	
	
}
