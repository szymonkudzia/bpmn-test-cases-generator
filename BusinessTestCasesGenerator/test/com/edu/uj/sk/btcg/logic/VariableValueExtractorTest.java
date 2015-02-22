package com.edu.uj.sk.btcg.logic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class VariableValueExtractorTest {
	private VariableValueExtractor variableValueExtractor;
	
	@Before
	public void setUp() throws Exception {
		variableValueExtractor = VariableValueExtractor.create();
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
	public void booleanComparison_returnedFourValues() {
		Lists.newArrayList(
			"&&", "!", "||"
		).forEach(operator -> assertOneVariableWithThreeBooleanValuesReturned(operator));
	}

	private void assertOneVariableWithThreeBooleanValuesReturned(
			String operator) {
		String expression = String.format("variable %s true", operator);
		
		Multimap<String, Object> variableValueMap =
				variableValueExtractor.extractVariableValueMap(expression);

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
