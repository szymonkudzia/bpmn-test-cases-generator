package com.edu.uj.sk.btcg.collections;

import static org.assertj.core.api.Assertions.assertThat;

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

}
