package com.edu.uj.sk.btcg.logic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

public class TokenizerTest {
	private Tokenizer tokenizer;
	
	@Before
	public void setUp() throws Exception {
		tokenizer = Tokenizer.create();
	}
	
	@Test
	public void emptyExpression_noTokens() {
		Iterator<String> tokens = tokenizer.tokenize("");
		
		assertThat(tokens.hasNext()).isFalse();
	}
	
	@Test
	public void nullExpression_noTokens() {
		Iterator<String> tokens = tokenizer.tokenize(null);
		
		assertThat(tokens.hasNext()).isFalse();
	}
	
	@Test
	public void expressionWithOneVariable_variableNameAsSingleTokenReturned() {
		Iterator<String> tokens = tokenizer.tokenize("variable");
		
		assertThat(tokens.next()).isEqualTo("variable");
		assertThat(tokens.hasNext()).isFalse();
	}
	
	
	@Test
	public void expressionWithOneVariableWithLeadingAndTailoringSpaces_variableNameAsSingleTokenReturned() {
		Iterator<String> tokens = tokenizer.tokenize("   variable   ");
		
		assertThat(tokens.next()).isEqualTo("variable");
		assertThat(tokens.hasNext()).isFalse();
	}

	@Test
	public void expressionWithTwoVariablesAndOperatorBetweenThem_twoVariablesAndOperatorAsThreeTokensReturned() {
		Iterator<String> tokens = tokenizer.tokenize("variable1 && variable2");
		
		assertThat(tokens.next()).isEqualTo("variable1");
		assertThat(tokens.next()).isEqualTo("&&");
		assertThat(tokens.next()).isEqualTo("variable2");
		assertThat(tokens.hasNext()).isFalse();
	}
	
	@Test
	public void expressionWithVariableInBrackets_leftBracketVariableRightBracketAsThreeTokensReturned() {
		Iterator<String> tokens = tokenizer.tokenize("(variable)");
		
		assertThat(tokens.next()).isEqualTo("(");
		assertThat(tokens.next()).isEqualTo("variable");
		assertThat(tokens.next()).isEqualTo(")");
		assertThat(tokens.hasNext()).isFalse();
	}
	
	
	@Test
	public void areNotRecognizedAsOperators() {
		Lists.newArrayList(
			null, 
			"", 
			"    ", 
			"a", 
			"<a", 
			" &&", 
			"&& ", 
			" && "
		).forEach(token -> assertThatIsNotAnOperator(token));
	}

	
	
	private void assertThatIsNotAnOperator(String token) {
		boolean isOperator = tokenizer.isOperator(token);
		
		assertThat(isOperator).as("token '%s' was recognized as operator!", token).isFalse();
	}
	
	
	@Test
	public void areRecognizedAsOperators() {
		Lists.newArrayList(
			">", 
			">=",
			"<",
			"<=", 
			"=", 
			"==", 
			"&", 
			"&&", 
			"|",
			"||",
			"!", 
			"!="
		).forEach(token -> assertThatIsAnOperator(token));
	}

	
	
	private void assertThatIsAnOperator(String token) {
		boolean isOperator = tokenizer.isOperator(token);
		
		assertThat(isOperator).as("token '%s' was not recognized as operator!", token).isTrue();
	}
	
	
	
	@Test
	public void areRcognizedAsNumber() {
		Lists.newArrayList(
				"1", 
				"-1",
				"0",
				"0.0", 
				"-1.0", 
				"1.3434", 
				"-1.22324", 
				"3.1E+7", 
				"3.1E-7",
				"3.1e+7",
				"3.1E-7"
			).forEach(token -> assertThatIsRecognizedAsNumber(token));
	}

	private void assertThatIsRecognizedAsNumber(String token) {
		boolean isNumber = tokenizer.isNumber(token);
		
		assertThat(isNumber).as("token '%s' was not recognized as number!", token).isTrue();
	}
	
	
	
	@Test
	public void areNotRcognizedAsNumber() {
		Lists.newArrayList(
				" 1", 
				"",
				null,
				"0. 0", 
				"-1 .0", 
				"1.34.34", 
				"-1.22324 ", 
				"3.1E+7.0", 
				"3000.000.1E-7"
			).forEach(token -> assertThatIsNotRecognizedAsNumber(token));
	}

	private void assertThatIsNotRecognizedAsNumber(String token) {
		boolean isNumber = tokenizer.isNumber(token);
		
		assertThat(isNumber).as("token '%s' was recognized as number!", token).isFalse();
	}
	
	
	
	
	@Test
	public void areRcognizedAsVariable() {
		Lists.newArrayList(
				"a", 
				"abc",
				"abx_asdf",
				"_AAddssAQvdsa__", 
				"daf32323", 
				"ada_3234" 
			).forEach(token -> assertThatIsRecognizedAsVariable(token));
	}

	private void assertThatIsRecognizedAsVariable(String token) {
		boolean isVariable = tokenizer.isVariable(token);
		
		assertThat(isVariable).as("token '%s' was not recognized as variable!", token).isTrue();
	}
	
	
	
	@Test
	public void areNotRcognizedAsVariable() {
		Lists.newArrayList(
				"",
				null,
				"null",
				"1a", 
				"a.bc",
				" abx_asdf",
				"_AAdds sAQvdsa__", 
				"daf32.323", 
				"ada-3234",
				"aaaa ", 
				"return",
				"int",
				"void",
				"double",
				"String",
				"for",
				"instanceof",
				"float",
				"boolean",
				"Boolean",
				"Float",
				"Integer",
				"char"
			).forEach(token -> assertThatIsNotRecognizedAsVariable(token));
	}

	private void assertThatIsNotRecognizedAsVariable(String token) {
		boolean isVariable = tokenizer.isVariable(token);
		
		assertThat(isVariable).as("token '%s' was recognized as variable!", token).isFalse();
	}
}
