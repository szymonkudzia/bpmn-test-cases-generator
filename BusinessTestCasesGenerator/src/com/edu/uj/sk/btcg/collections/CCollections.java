package com.edu.uj.sk.btcg.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

public class CCollections {
	public static <T> Optional<T> find(Collection<T> collection, Predicate<? super T> predicate) {
		Collection<T> filtered = Collections2.filter(collection, predicate);
		
		if (filtered.size() <= 0) return Optional.absent();
		
		return Optional.of(filtered.iterator().next());
	}
	
	
	
	public static <T> Optional<T> find(T[] array, Predicate<? super T> predicate) {
		return find(Arrays.asList(array), predicate);
	}
	
	
	
	
	public static <T> void each(Collection<T> collection, Predicate<? super T> processor) {
		for (T element : collection) {
			processor.apply(element);
		}
	}
	
	
	
	public static <T> void each(T[] array, Predicate<? super T> processor) {
		each(Arrays.asList(array), processor);
	}
	
	
	
	
	public static <T> List<List<T>> powerSet(Collection<T> originalCollection) {
		Set<Set<T>> powerSet = powerSet(new HashSet<T>(originalCollection));
		
		List<List<T>> powerSetAsList = powerSet.stream().map(x -> new ArrayList<T>(x)).collect(Collectors.toList());
		return powerSetAsList;
	}
	
	
	public static <T> Set<Set<T>> powerSet(Set<T> originalSet) {
	    Set<Set<T>> sets = new HashSet<Set<T>>();
	    if (originalSet.isEmpty()) {
	    	sets.add(new HashSet<T>());
	    	return sets;
	    }
	    List<T> list = new ArrayList<T>(originalSet);
	    T head = list.get(0);
	    Set<T> rest = new HashSet<T>(list.subList(1, list.size())); 
	    for (Set<T> set : powerSet(rest)) {
	    	Set<T> newSet = new HashSet<T>();
	    	newSet.add(head);
	    	newSet.addAll(set);
	    	sets.add(newSet);
	    	sets.add(set);
	    }		
	    return sets;
	}
}
