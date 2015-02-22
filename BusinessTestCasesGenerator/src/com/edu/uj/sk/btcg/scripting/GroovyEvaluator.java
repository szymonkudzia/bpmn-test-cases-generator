package com.edu.uj.sk.btcg.scripting;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.util.Map;

public class GroovyEvaluator {
	public static Object evaluate(String script, Map<String, Object> context) {
		Binding binding = new Binding();
		context.keySet()
			.forEach(variable -> {
				binding.setVariable(variable, context.get(variable));
			});
		
		GroovyShell shell = new GroovyShell(binding);

		return shell.evaluate(script);
	}
}
