package com.edu.uj.sk.btcg.collections;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;

public class CCollectionsTest {

	@Test
	public void twoListWithTwoElements_fourCorrectPermutations() {
		List<List<Integer>> expectedCombinations = 
			Lists.newArrayList(
				Lists.newArrayList(1,3),
				Lists.newArrayList(1, 4),
				Lists.newArrayList(2, 3),
				Lists.newArrayList(2, 4)
			);
		
		List<Integer> list1 = Lists.newArrayList(1, 2);
		List<Integer> list2 = Lists.newArrayList(3, 4);
		
		List<List<Integer>> allCombinations = CCollections.allCombinations(Lists.newArrayList(list1, list2));
		
		assertThat(allCombinations).containsExactlyElementsOf(expectedCombinations);
	}
	
	
	@Test
	public void iterativelyGeneratedPowerSet() {
		List<List<Integer>> expected = Lists.newArrayList(
			Lists.newArrayList(),
			Lists.newArrayList(1),
			Lists.newArrayList(2),
			Lists.newArrayList(3),
			Lists.newArrayList(4),
			Lists.newArrayList(1, 2),
			Lists.newArrayList(1, 3),
			Lists.newArrayList(1, 4),
			Lists.newArrayList(2, 3),
			Lists.newArrayList(2, 4),
			Lists.newArrayList(3, 4),
			Lists.newArrayList(1, 2, 3),
			Lists.newArrayList(1, 2, 4),
			Lists.newArrayList(1, 3, 4),
			Lists.newArrayList(2, 3, 4),
			Lists.newArrayList(1, 2, 3, 4)
		);
		
		Iterator<List<Integer>> powerSetIterator = 
				CCollections.powerSetIterator(Lists.newArrayList(1, 2, 3, 4));
		
		int iteration = 0;
		while (powerSetIterator.hasNext()) {
			List<Integer> combination = powerSetIterator.next();
			
			assertThat(iteration).isLessThanOrEqualTo(8);
			assertThat(combination)
				.as(String.format("During iteration: %s, permutation returned was different", iteration))
				.containsExactlyElementsOf(expected.get(iteration++));
		}
	}
	
	@Test
	public void assertThatNumberOfSubSetsEqual2PowerOfN() {
		int N = 16;
		
		Iterator<List<Integer>> powerSetIterator = 
				CCollections.powerSetIterator(CCollections.range(N));
		
		int count = 0;
		while (powerSetIterator.hasNext()) {
			++count;
			List<Integer> combination = powerSetIterator.next();
			
			System.out.println(combination);
		}
		
		assertThat(count).isEqualTo(((Double)Math.pow(2.0, N)).intValue());
	}
}
