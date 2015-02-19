package com.edu.uj.sk.btcg.logic;

import java.util.Optional;

public class BooleanExpressionNode {
	private String value;
	private Optional<BooleanExpressionNode> left = Optional.empty();
	private Optional<BooleanExpressionNode> right = Optional.empty();

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Optional<BooleanExpressionNode> getLeft() {
		return left;
	}

	public void setLeft(Optional<BooleanExpressionNode> left) {
		this.left = left;
	}

	public Optional<BooleanExpressionNode> getRight() {
		return right;
	}

	public void setRight(Optional<BooleanExpressionNode> right) {
		this.right = right;
	}

}