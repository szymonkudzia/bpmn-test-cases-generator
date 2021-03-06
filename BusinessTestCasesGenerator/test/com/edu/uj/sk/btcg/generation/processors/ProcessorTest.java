package com.edu.uj.sk.btcg.generation.processors;

import java.util.Iterator;

import org.activiti.bpmn.model.BpmnModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.edu.uj.sk.btcg.generation.generators.IGenerator;
import com.edu.uj.sk.btcg.generation.generators.impl.GenerationInfo;
import com.edu.uj.sk.btcg.persistance.TestCasePersister;

@RunWith(MockitoJUnitRunner.class)
public class ProcessorTest {
	@Mock
	private IGenerator generator;
	
	@InjectMocks
	private Processor processor;

	BpmnModel model;
	TestCasePersister persister;

	@Before
	public void before() {
		model = emptyModel();
		persister = Mockito.mock(TestCasePersister.class);
	}
	
	
	@After
	public void after() {
		persister = null;
	}
	
	@Test
	public void generatorReturnsEmptyIterator_processorEndsHisWorkQuietly() throws Exception {
		generatorReturningEmptyIterator();
		
		processor.process(model, persister);
	}

	
	@Test
	public void generatorReturnsIteratorWithOneElement_processorSavesOneTestCaseUsingPersitersMethodPersist() throws Exception {
		generatorReturnsIteratorWithSize(1);
		
		processor.process(model, persister);
		
		verifyThatPersistMethodWasCalledTimes(1);
	}
	
	
	@Test
	public void generatorReturnsIteratorWithTwoElements_processorSavesTwoTestCasesUsingPersitersMethodPersist() throws Exception {
		generatorReturnsIteratorWithSize(2);
		
		processor.process(model, persister);
		
		verifyThatPersistMethodWasCalledTimes(2);
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
	private void verifyThatPersistMethodWasCalledTimes(int i) throws Exception {
		Mockito.verify(persister, Mockito.times(i)).persist(null, model);
	}


	private void generatorReturnsIteratorWithSize(final int size) {
		Iterator<Pair<BpmnModel, GenerationInfo>> iterator = new Iterator<Pair<BpmnModel, GenerationInfo>>() {
			private int i = 0;
			
			@Override
			public boolean hasNext() {
				return i < size;
			}

			@Override
			public Pair<BpmnModel, GenerationInfo> next() {
				if (i++ < size) 				
					return Pair.of(model, null);
				
				return null;
			}
		};
		
		generatorReturns(iterator);
		
	}

	private BpmnModel emptyModel() {
		return new BpmnModel();
	}

	
	
	private void generatorReturningEmptyIterator() {
		Iterator<Pair<BpmnModel, GenerationInfo>> iterator = new Iterator<Pair<BpmnModel, GenerationInfo>>() {

			@Override
			public boolean hasNext() {
				return false;
			}

			@Override
			public Pair<BpmnModel, GenerationInfo> next() {
				return null;
			}
		};
		
		generatorReturns(iterator);
	}


	private void generatorReturns(Iterator<Pair<BpmnModel, GenerationInfo>> iterator) {
		Mockito.when(generator.generate(model)).thenReturn(iterator);
	}
}
