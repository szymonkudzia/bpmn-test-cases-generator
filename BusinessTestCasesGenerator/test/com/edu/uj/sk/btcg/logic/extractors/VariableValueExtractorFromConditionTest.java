package com.edu.uj.sk.btcg.logic.extractors;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.edu.uj.sk.btcg.logic.extractors.VariableValueExtractorFromCondition;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class VariableValueExtractorFromConditionTest {
	private VariableValueExtractorFromCondition variableValueExtractor;
	
	@Before
	public void setUp() throws Exception {
		variableValueExtractor = VariableValueExtractorFromCondition.create();
	}

	@Test
	public void numericComparison_returnedFourValues() {
		Lists.newArrayList(
			">", ">=", "<", "<=", "==", "!="
		).forEach(operator -> assertOneVariableWithFourNumericValuesReturned(operator));
	}

	private void assertOneVariableWithFourNumericValuesReturned(
			String operator) {
		String expression = String.format("variable %s 1000", operator);
		
		Multimap<String, Object> variableValueMap =
				variableValueExtractor.extractVariableValueMap(expression);

		assertThat(variableValueMap.asMap()).containsOnlyKeys("variable");
		assertThat(variableValueMap.values()).containsOnly(null, 1001.0, 1000.0, 999.0);
	}
	
	
	
	@Test
	public void forExclamationThreeValuesAreReturned() {
		
		Multimap<String, Object> variableValueMap =
				variableValueExtractor.extractVariableValueMap("!variable");

		assertThat(variableValueMap.asMap()).containsOnlyKeys("variable");
		assertThat(variableValueMap.values()).containsOnly(null, true, false);
	}
	
	
	
	
	@Test
	public void stringComparison_returnedThreeValues() {
		Lists.newArrayList(
			"==", "!="
		).forEach(operator -> assertOneVariableWithThreeStringValuesReturned(operator));
	}

	private void assertOneVariableWithThreeStringValuesReturned(
			String operator) {
		String expression = String.format("variable %s \"string\"", operator);
		
		Multimap<String, Object> variableValueMap =
				variableValueExtractor.extractVariableValueMap(expression);

		assertThat(variableValueMap.asMap()).containsOnlyKeys("variable");
		assertThat(variableValueMap.values()).containsOnly(null, "string", "");
	}
}
