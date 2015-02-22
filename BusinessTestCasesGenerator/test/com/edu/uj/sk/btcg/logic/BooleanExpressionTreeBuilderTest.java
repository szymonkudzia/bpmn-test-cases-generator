package com.edu.uj.sk.btcg.logic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

public class BooleanExpressionTreeBuilderTest {
	private BooleanExpressionTreeBuilder booleanExpressionTreeBuilder;
	
	@Before
	public void setUp() throws Exception {
		booleanExpressionTreeBuilder = BooleanExpressionTreeBuilder.create();
	}

	@Test
	public void inputsForWhichEmptyTreeShouldBeCreated() {
		Lists.newArrayList(
			"",
			null,
			"   ", //spaces
			"		", //tabs
			"\n"
		).forEach(expression -> forGivenInputAssertThatEmptyTreeWasCreated(expression));
	}

	
	public void forGivenInputAssertThatEmptyTreeWasCreated(String expression) {
		BooleanExpressionTree tree = booleanExpressionTreeBuilder.build(expression);
		
		assertThat(tree.getRoot().isPresent()).isFalse();		
	}
	
	
	
	@Test
	public void inputsForWhichExceptionShouldBeThrown() {
		Lists.newArrayList(
			"a ==",
			"(a == b",
			"a == (b != c",
			"a == (b != c))"
		).forEach(expression -> forGivenInputAssertThatExceptionWasThrown(expression));
	}

	
	public void forGivenInputAssertThatExceptionWasThrown(String expression) {
		try {
			booleanExpressionTreeBuilder.build(expression);
			
			fail(String.format("Exception was not thrown for wrong expression: <%s>", expression));
		} catch (IllegalStateException e) { /** exception should be thrown **/}
	}
	
	
	
	
	@Test
	public void equationTwoNumbers_treeWithOperatorInRootAndNumbersInLeftAndRightNode() {
		BooleanExpressionTree tree = booleanExpressionTreeBuilder.build("100 == 200");
		
		assertThat(getValue(tree.getRoot())).isEqualTo("==");
		assertThat(getValue(getLeft(tree.getRoot()))).isEqualTo("100");
		assertThat(getValue(getRight(tree.getRoot()))).isEqualTo("200");
		
	}
	
	@Test
	public void twoEquationWithBrackets_treeWithOperatorInRootAndNumbersInLeftAndRightNode() {
		BooleanExpressionTree tree = booleanExpressionTreeBuilder.build("100 == (200 != 300)");
		
		assertThat(getValue(tree.getRoot())).isEqualTo("==");
		assertThat(getValue(getLeft(tree.getRoot()))).isEqualTo("100");
		
		Optional<BooleanExpressionNode> rightChild = getRight(tree.getRoot());
		assertThat(getValue(rightChild)).isEqualTo("!=");
		assertThat(getValue(getLeft(rightChild))).isEqualTo("200");
		assertThat(getValue(getRight(rightChild))).isEqualTo("300");
		
	}
	
	
	
	
	
	
	
	private String getValue(Optional<BooleanExpressionNode> node) {
		assertNodeIsNotEmpty(node);
		return node.get().getValue();
	}
	
	private Optional<BooleanExpressionNode> getLeft(Optional<BooleanExpressionNode> node) {
		assertNodeIsNotEmpty(node);
		return node.get().getLeft();
	}
	
	private Optional<BooleanExpressionNode> getRight(Optional<BooleanExpressionNode> node) {
		assertNodeIsNotEmpty(node);
		return node.get().getRight();
	}

	private void assertNodeIsNotEmpty(Optional<BooleanExpressionNode> node) {
		if (!node.isPresent()) throw new IllegalArgumentException("node is empty !");
	}
}
