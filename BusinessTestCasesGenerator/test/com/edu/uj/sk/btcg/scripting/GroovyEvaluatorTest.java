package com.edu.uj.sk.btcg.scripting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.Test;

import com.google.common.collect.Maps;

public class GroovyEvaluatorTest {

	@Test
	public void addTwoNumbers_sum() {
		Map<String, Object> mappings = Maps.newHashMap();
		mappings.put("a", 1000);
		mappings.put("b", 2000);
		
		Object sum = GroovyEvaluator.evaluate("a + b", mappings);
		
		assertThat(sum).isEqualTo(3000);
	}

}
