package com.edu.uj.sk.btcg.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class CCollections {
	public static <T> Optional<T> find(Collection<T> collection, Predicate<? super T> predicate) {
		Collection<T> filtered = Collections2.filter(collection, predicate);
		
		if (filtered.size() <= 0) return Optional.absent();
		
		return Optional.of(filtered.iterator().next());
	}
	
	
	
	public static <T> Optional<T> find(T[] array, Predicate<? super T> predicate) {
		return find(Arrays.asList(array), predicate);
	}
	
	
	
	
	/**
	 * Perform given action (@processor) on each item in collection
	 * 
	 * @param collection
	 * @param processor
	 */
	public static <T> void each(Collection<T> collection, Predicate<? super T> processor) {
		for (T element : collection) {
			processor.apply(element);
		}
	}
	
	
	
	
	/**
	 * Perform given action (@processor) on each item in array
	 * 
	 * @param collection
	 * @param processor
	 */
	public static <T> void each(T[] array, Predicate<? super T> processor) {
		each(Arrays.asList(array), processor);
	}
	
	
	
	/**
	 * Return power set of given collection
	 * Example
	 * 	powerSet([1 2 3]) = [[], [1], [2], [3], [1, 2], [1, 3], [2, 3]]
	 * 
	 * @param originalCollection
	 * @return not null list of sets
	 */
	public static <T> List<List<T>> powerSet(Collection<T> originalCollection) {
		Set<Set<T>> powerSet = powerSet(new HashSet<T>(originalCollection));
		
		List<List<T>> powerSetAsList = powerSet.stream().map(x -> new ArrayList<T>(x)).collect(Collectors.toList());
		return powerSetAsList;
	}
	
	
	
	public static <T> Set<Set<T>> powerSet(Set<T> intList){

	    Set<Set<T>> result = new HashSet<>();
	    result.add(new HashSet<>());

	    for (T i : intList){

	        Set<Set<T>> temp = new HashSet<>();

	        for(Set<T> intSet : result){

	            intSet = new HashSet<>(intSet);
	            intSet.add(i);                
	            temp.add(intSet);
	        }
	        result.addAll(temp);
	    }
	    
	    return result;
	}
	
//	public static <T> Set<Set<T>> powerSet(Set<T> originalSet) {
//	    Set<Set<T>> sets = new HashSet<Set<T>>();
//	    if (originalSet.isEmpty()) {
//	    	sets.add(new HashSet<T>());
//	    	return sets;
//	    }
//	    List<T> list = new ArrayList<T>(originalSet);
//	    T head = list.get(0);
//	    Set<T> rest = new HashSet<T>(list.subList(1, list.size())); 
//	    for (Set<T> set : powerSet(rest)) {
//	    	Set<T> newSet = new HashSet<T>();
//	    	newSet.add(head);
//	    	newSet.addAll(set);
//	    	sets.add(newSet);
//	    	sets.add(set);
//	    }		
//	    return sets;
//	}
	
	
	public static <T> Iterator<List<T>> powerSetIterator(final Collection<T> collection) {
		return new Iterator<List<T>>() {
			List<T> source = Lists.newArrayList(collection);
			ArrayList<Integer> indices = Lists.newArrayList();
			
			
			@Override
			public boolean hasNext() {
				int compareResult = indices.size() - source.size();
				
				if (compareResult < 0) 
					return true;
				
				return false;
			}

			@Override
			public List<T> next() {
				if (indices.isEmpty()) {
					indices.add(-1);
					return Lists.newArrayList();
				}
				
				updateIndices();
								
			    return pickSubList();
			}
			
			
			private void updateIndices() {
				boolean addNew = false;
				
				int lastIdx = getLastIndex(source);
				for (int i = getLastIndex(indices), ri = 0; i >= 0; --i, ++ri) {
					int idx = indices.get(i) + 1;
					
					if (idx <= lastIdx - ri) {
						indices.set(i, idx);
						break;
						
					} else if (i == 0) {
						addNew = true;
						break;
					}
				}
				
				if (addNew)
					indices = Lists.newArrayList(range(indices.size() + 1));
			}
			
			private List<T> pickSubList() {
				final List<T> sublist = Lists.newArrayList();
				
				indices.forEach(i -> sublist.add(source.get(i)));
						
				return sublist;
			}
		};
	}
	
	
	
	public static <T> T getLast(List<T> list) {
		Preconditions.checkNotNull(list, "Cannot get last element from null list");
		Preconditions.checkArgument(!list.isEmpty(), "Cannot get last element from empty list");
		
		return list.get(list.size());
	}
	
	public static <T> int getLastIndex(Collection<T> list) {
		Preconditions.checkNotNull(list, "Cannot get last element from null list");
		Preconditions.checkArgument(!list.isEmpty(), "Cannot get last element from empty list");
		
		return list.size() - 1;
	}
	
	
	public static <T> T getFirst(List<T> list) {
		Preconditions.checkNotNull(list, "Cannot get last element from null list");
		Preconditions.checkArgument(!list.isEmpty(), "Cannot get last element from empty list");
		
		return list.get(0);
	}
	
	
	
	
	
	
	/**
	 * Returns all combination of given collection
	 * Example
	 * 	allCombinations([[1, 2], [3, 4]]) = [[1, 3], [1, 4], [2, 3], [2, 4]]
	 * 
	 * @param collections
	 * @return not null list of all combinations
	 */
	public static <T> List<List<T>> allCombinations(List<Collection<T>> collections) {
		if (collections == null) return Lists.newArrayList();
		if (collections.isEmpty()) return Lists.newArrayList();
		
		List<List<T>> result = Lists.newArrayList();
		int[] indexes = new int[collections.size()];
		
		while (indexes[0] < collections.get(0).size()) {
			result.add(newList(collections, indexes));

			for (int i = indexes.length - 1; i >= 0; --i) {
				indexes[i]++;
				
				if (indexes[i] >= collections.get(i).size()) {
					if (i > 0) {
						indexes[i] = 0;
						continue;
					}
				}
				
				break;
			}
		}
		
		return result;
	}
	
	private static <T> List<T> newList(List<Collection<T>> collections, int[] indexes){
		List<T> result = Lists.newArrayList();
		
		for (int i = 0; i < indexes.length; ++i) {
			result.add(new ArrayList<>(collections.get(i)).get(indexes[i]));
		}
		
		return result;
	}
	
	
	
	
	/**
	 * Update map with Multimap values
	 * if @map does not contains value for @key then
	 * pair (@key, @value) is inserted to the @map
	 * otherwise value under @key and new multimap @value
	 * are merged together and stored under @key in @map
	 * 
	 * @param map
	 * @param key
	 * @param value
	 * @return updated @map
	 */
	public static <A, B, C> Map<A, Multimap<B, C>> updateMap
		(Map<A, Multimap<B, C>> map, A key, Multimap<B, C> value) {
		
			Multimap<B, C> v = map.getOrDefault(key, HashMultimap.create());
			v.putAll(value);
			map.put(key, v);
			
			return map;
	}



	public static <T> List<List<T>> permutations(Collection<T> collection) {
		return Lists.newArrayList(Collections2.permutations(collection));
	}
	
	
	
	/**
	 * Generate list of numbers
	 * Starting from @from (inclusive) up till @to parameter (exclusive)
	 * 
	 * @param from integer 
	 * @param to integer
	 * @return list of integer
	 */
	public static List<Integer> range(int from, int to) {
		List<Integer> integers = Lists.newArrayList();
		
		for (int i = from; i < to; i++) {
			integers.add(i);
		}
		
		return integers;
	}
	
	/**
	 * Generate list of numbers
	 * Starting form 0 (inclusive) up till @to parameter (exclusive)
	 * @param to
	 * @return
	 */
	public static List<Integer> range(int to) {
		return range(0, to);
	}
}
