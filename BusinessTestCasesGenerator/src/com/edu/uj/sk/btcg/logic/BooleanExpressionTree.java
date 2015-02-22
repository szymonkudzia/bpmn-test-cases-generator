package com.edu.uj.sk.btcg.logic;

import java.util.Optional;


public class BooleanExpressionTree {
	private Optional<BooleanExpressionNode> root = Optional.empty();

	public BooleanExpressionTree() {
	}
	
	public BooleanExpressionTree(Optional<BooleanExpressionNode> root) {
		this.root = root;
	}
	
	public static BooleanExpressionTree create(String expression) {
		return BooleanExpressionTreeBuilder.create().build(expression);
	}
	

	public Optional<BooleanExpressionNode> getRoot() {
		return root;
	}

	public void setRoot(BooleanExpressionNode node) {
		root = Optional.ofNullable(node);
	}
}


